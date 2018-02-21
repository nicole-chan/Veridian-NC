// Nicole Chan (nc83@students.waikato.ac.nz)
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author nc83
 */
public class Visualiser extends JFrame{
	private static final long serialVersionUID = 7702298459121313397L; //version control for Serializable - Eclipse generated
	private static ArrayList<Rectangle> rectList = new ArrayList<Rectangle>(); //used in Visualiser() to draw store (text as) rectangles of the page
	private static int SCALE = 20; //scale for the page to draw at in Visualiser()
	private static Random rand = new Random(); //used in the paint for Visualiser() to determine the colour of the rectangles
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {		
			int verbLevel = 1; //set the default verbosity (verb) level to 1
			ArrayList<String> inputDirsList = new ArrayList<String>(); //Directories to get input from
			String outputFile = ""; //file to store output, must be .csv
			int index = 0;
			int indexVerb = args.length; //get the index of the verbosity level
			for(String runArgs : args) {			
				//if the -v or -verbose argument is seen, the next argument has to be a number i.e. verbosity level
				if(runArgs.equalsIgnoreCase("-v")||runArgs.equalsIgnoreCase("-verbose")) {
					indexVerb = index;
				} else if(index == indexVerb+1) {
					try{
						verbLevel = Integer.parseInt(runArgs);
					} catch(Exception e) {
						System.out.println(e);
					}
				} else if(args[index].contains(".csv") && outputFile=="") { //output file
					outputFile = args[index];
				} else if(new File(args[index]).isDirectory()){ //check if the argument is a directory, add it to inputDir
					inputDirsList.add(args[index]);
				}
				index++;
			}
			//no file or directory provided or nonexistent directory - print error message and stop running
			if(inputDirsList.size() == 0) {
				System.out.println("No compatible input files or directories provided.");
				return;
			}
			//Remove directories that don't exist
			for(String dir : inputDirsList) {
				if(!((new File(dir)).exists())) {
					inputDirsList.remove(dir);
				}
			}
			//no file or directory provided or nonexistent directory - print error message and stop running
			if(inputDirsList.size() == 0) {
				System.out.println("No compatible input files or directories provided.");
					return;
			}
			//Directory list should be compatible, process each of the directories in the list
			for(String inputDir : inputDirsList) {
				if(inputDirsList.size() == 1 || inputDir.equals(inputDirsList.get(0))) {
					processDir(inputDir, verbLevel, outputFile, false);
				} else {
					processDir(inputDir, verbLevel, outputFile, true);
				}
			}
		} catch (Exception e) {
	         e.printStackTrace();
		}
	}
	
	/*
	 * Processes the input directory, the pages and the mets and puts tags on them.
	 * Appends to end of file so there is one file for raw data, one file for normalised data.
	 */
	public static void processDir(String inputDir, int verbLevel, String outputFile, boolean append) {
		File inputFile;
		HashMap<String, String> tBTags; //For each line, store the textblock number as the key, the tag as the value
		//boolean append = false;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			String fileDir = "";
			
			File[] files = new File(inputDir).listFiles(); // Get a list of the all files in the directory
			if(files != null) { // Input was a directory
				// Get a hashmap of the mets files and its corresponding pages 
				HashMap<String, ArrayList<String>> metsToPages = getFilesMap(files, "", new ArrayList<String>(), new HashMap<String, ArrayList<String>>());
				// If there were mets pages found
				if(!metsToPages.isEmpty()) {
					TreeMap<String, ArrayList<String>> mapSorted = new TreeMap<>(metsToPages); //sort the hash map
				    // FULL APPEND for the output file with all files
					try {
						// Get the file directory and check if it exists, else, make a file
				    	File dir = new File(fileDir);
				    	if (!dir.exists()) {
				    		dir.mkdir();
				    	}
				    	String filename = getFilename(outputFile, fileDir, mapSorted.firstKey(), "full", verbLevel);
				    	// If the file exists, append, else rewrite
				    	if(append == true && new File(filename).exists()) {
				    		append = true;
				    	}
				    	FileWriter fw = new FileWriter(filename, append);
				    	System.out.println(fileDir + outputFile);
				    	BufferedWriter bw = new BufferedWriter(fw);
				    	PrintWriter pw = new PrintWriter(bw);
				    	if(append == false) { // If it's a new file, write the header to the file
				    		pw.write("HPOS,VPOS,width,height,space,strings,chars,textBlockNum,tag\n");
				    	}
				    	pw.flush();
				    	pw.close();
				    } catch (FileNotFoundException e) {
				        e.printStackTrace();
				    } catch (Exception e) {
				         e.printStackTrace();
				    }
					
					//check if there are mets files, indicates how many issues there are
					for(Map.Entry<String,ArrayList<String>> page : mapSorted.entrySet()) {
						inputFile = new File(page.getKey());
						
						// Process the mets file, and get the mets tags
						metsHandler metsHandler = new metsHandler(inputFile.getName(), verbLevel);
						saxParser.parse(inputFile, metsHandler);
						tBTags = metsHandler.getTBTags();
						
						boolean isFirstPage = true;
						inputFile = new File(page.getValue().get(0).toString());
						if(verbLevel >= 1) { System.out.println("Processing: " + inputFile); }
						
						// Process the raw values, and the normalised values for the first page
						altoMetsHandler aMHandler = new altoMetsHandler(getFilename(outputFile, fileDir, page.getKey(), "raw", verbLevel), tBTags, isFirstPage, verbLevel);
						saxParser.parse(inputFile, aMHandler);
						NormaliseAMHandler nHandler = new NormaliseAMHandler(aMHandler.getModeValues(), getFilename(outputFile, fileDir, page.getKey(), "normal", verbLevel), tBTags, isFirstPage, verbLevel);
						saxParser.parse(inputFile, nHandler);
						
						isFirstPage = false;
						// Process the full append first page
						nHandler = new NormaliseAMHandler(aMHandler.getModeValues(), getFilename(outputFile, fileDir, page.getKey(), "full", verbLevel), tBTags, isFirstPage, 0); //FULL APPEND
						saxParser.parse(inputFile, nHandler); //FULL APPEND
						
						// Process the list of pages
						for(String p : page.getValue().subList(1, page.getValue().size())) {
							inputFile = new File(p);
							if(verbLevel >= 1) {	System.out.println("\nProcessing: " + inputFile); }
							
							// Process the raw values, and the normalised values
							aMHandler = new altoMetsHandler(getFilename(outputFile, fileDir, page.getKey(), "raw", verbLevel), tBTags, isFirstPage, verbLevel);
							saxParser.parse(inputFile, aMHandler);
							nHandler = new NormaliseAMHandler(aMHandler.getModeValues(), getFilename(outputFile, fileDir, page.getKey(), "normal", verbLevel), tBTags, isFirstPage, verbLevel);
							saxParser.parse(inputFile, nHandler);
							nHandler = new NormaliseAMHandler(aMHandler.getModeValues(), getFilename(outputFile, fileDir, page.getKey(), "full", verbLevel), tBTags, isFirstPage, 0); //FULL APPEND
							saxParser.parse(inputFile, nHandler); //FULL APPEND
				        
							/*//Draw the newspaper text to a jpg file
							rectList = aMHandler.getRectList();
							int width = aMHandler.getPageWidth();
							int height = aMHandler.getPageHeight();
							String name = filename.substring(0, filename.lastIndexOf(".")) + "-img";
							new Visualiser(name, width, height);*/
						}
					}
				} else { // No mets files exist in the directory
			    	System.out.println("No METS xml files found in this directory.");
			    	return;
			    }
			} else { // Directory was invalid
				System.out.println("Please provide a valid directory of input files.");
				return;
			}
		} catch (FileNotFoundException e) {
	         e.printStackTrace();
	    } catch (IOException e) {
	         e.printStackTrace();
	    } catch (Exception e) {
	         e.printStackTrace();
	    }
	}
	
	
	/*
	 * Given the parameters, it creates the filename and returns it.
	 * If given an output filename for a full file, return that.
	 * Else returns the directory with the name of the publication
	 * output is the output filename if given, else ""
	 * fileDir is the directory to store the file
	 * pagekey is the key of the current page being processed
	 * fileType is the file to be output, i.e. "full", "normal" or "raw". (Could potentially change to bools - isNormal, isFull)
	 */
	public static String getFilename(String output, String fileDir, String pageKey, String fileType, int verbLevel) {
		String filename = "";
		if(fileType.equalsIgnoreCase("full") && output != "") { //an output filename was given, just return the filename
			//filename = fileDir + output; //sets the filename to the fileDir with the output file
			return output; //file is output to project folder if only name of file was given in program arguments
		} else {
			if(fileDir.equals("")) { //sets the file directory to a a sub-folder (Testing) inside the project folder
				fileDir = System.getProperty("user.dir") + "/Testing/";
				if(verbLevel > 1) {
					System.out.println("\nOutput directory was not provided, redirected to: " + fileDir);
				}
			}
			filename = pageKey;
			filename = fileDir + filename.substring(filename.toLowerCase().lastIndexOf("/")+1, filename.toLowerCase().lastIndexOf("mets")-1);
			if(fileType.equalsIgnoreCase("normal")) {
				filename += "-N";
			} else if (fileType.equalsIgnoreCase("full")) {
				filename += "-FULL";
			}
			filename += ".csv";
		}
		return filename;
	}
	
	/*
	 * https://stackoverflow.com/questions/3154488/how-do-i-iterate-through-the-files-in-a-directory-in-java
	 * Given an array of Files, recursively look through the directories and
	 * Adds all the directories to hashmap - metsToPages
	 */
	public static HashMap<String, ArrayList<String>> getFilesMap(File[] files, String metsKey, ArrayList<String> pages, HashMap<String, ArrayList<String>> metsToPages) {
		for (File file : files) {
	        if (file.isDirectory()) {
	        	if(metsKey != "" && !metsToPages.containsKey(metsKey)) { //if the key isn't null, and the HashMap doesn't already contain the key
	        		Collections.sort(pages);
	        		metsToPages.put(metsKey, pages); //add the current mets page and the associated pages - sorted to the HashMap
	        	}
	        	pages = new ArrayList<String>(); //initialise a new pages ArrayList
	            getFilesMap(file.listFiles(), metsKey, pages, metsToPages); //recurse through the directory
	        } else { //not a directory
	        	String fPath = file.getPath();
	        	Pattern pMets = Pattern.compile("(.*)(METS|mets)(.*)[.](xml|XML)"); //check if it's a mets .xml file
	        	Matcher mMets = pMets.matcher(fPath);
	        	Pattern pNotMets = Pattern.compile("(.*)[^(mets|METS)](.*)[.](xml|XML)"); //checks if it's not a mets .xml
	        	Matcher mNotMets = pNotMets.matcher(fPath);
	        	if(mMets.matches()) { //if the current path is a mets .xml file, set the metsKey variable
	        		metsKey = fPath;
	        	} else if(mNotMets.matches()) { //if the current path is regular .xml file, add the page to the list
	        		pages.add(fPath);
	        	}
	        }
	    }
		//Add the last item to the HashMap
		if(metsKey != "" && !metsToPages.containsKey(metsKey)) { //if the key isn't null, and the HashMap doesn't already contain the key
    		Collections.sort(pages); //sort the pages
    		metsToPages.put(metsKey, pages); //add the current mets page and the associated pages to the HashMap
    	}
		return metsToPages;
	}

	/*
	 * Draws the page to an image file where lines are now rectangles
	 * Scales image by const.
	 * int width and int height is the raw size of the image 
	 */
	public Visualiser(String name, int width, int height) {
		setSize(width/SCALE, height/SCALE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setVisible(true);

	    //Write image to .jpg file
	    BufferedImage img = new BufferedImage(this.getSize().width, this.getSize().height, BufferedImage.TYPE_INT_ARGB);
	    Graphics g = img.createGraphics();
	    this.paint(g);
	    g.dispose();
	    try {
	    	ImageIO.write(img, "jpg", new File(new String(name + ".jpg")));
	    } catch (Exception e) {
	         e.printStackTrace();
	    }
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.awt.Window#paint(java.awt.Graphics)
	 * Paint the rectangles in rectList.
	 */
	public void paint(Graphics g) {
		float f = rand.nextFloat();
		float f1 = rand.nextFloat();
		float f2 = rand.nextFloat();
		Color c = new Color(f, f1, f2);
		g.setColor(c);
	    for (Rectangle r : rectList) {
	    	g.fillRect(r.x/SCALE, r.y/SCALE, r.width/SCALE, r.height/SCALE);
	    }
	}
}


































