// Nicole Chan (nc83@students.waikato.ac.nz)
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Handler to print out raw values and convert to a csv.
 * Calculates modes of values too. (Per line: width, height, number of strings, number of chars, space above to previous line)
 * 
 * Could potentially include font case (upper/lower/mixed), currently doesn't process advertisements
 */
class altoMetsHandler extends DefaultHandler{
	//Used when printing values to the console
	int cBCount = 0;
	int tBCount = 0;
	int tLCount = 0;
	
	String outputFile = "";
	boolean isAdvert = false;
	int verbLevel = 1; //set default verbosity level to 1
	int charsInLine = 0;
	int strInLine = 0;
	int heightAbove = 0;
	int currLine = 0;
	int tempLine = 0;
	boolean firstLine = true;
	private boolean isFirstPage = true;
	
	//Used in Visualiser()
	private ArrayList<Rectangle> rectList = new ArrayList<Rectangle>();
	private int pageWidth = 0;
	private int pageHeight = 0;
	
	ArrayList<Integer> modeLWidth = new ArrayList<Integer>();
	ArrayList<Integer> modeLHeight = new ArrayList<Integer>();
	ArrayList<Integer> modeLChars = new ArrayList<Integer>();
	ArrayList<Integer> modeLStrings = new ArrayList<Integer>();
	ArrayList<Integer> modeLSpace = new ArrayList<Integer>();
	
	private int[] modes = new int[5]; //Array to store the mode values
	
	//Set the maximum and minimum space above a line to -500 and 500
	int spaceCapMax = 500; 
	int spaceCapMin = -500;
	
	StringBuilder strOutput = new StringBuilder(); //Stores the string to print to the csv file
	
	HashMap<String, String> tBTags; //Textblock tags e.g. TITLE_SECTION_HEADLINE
	String currTag = ""; //Tag for the current line
	String currTB = ""; //ID of the current textblock being processed
	
	/*
	 * Store the most freq # & most freq count, compare and update if necessary
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
		if(isAdvert == false) {
			if(qName.equalsIgnoreCase("Page")) {
				if(verbLevel > 3) {
					System.out.println(outputFile + ": Page " + attributes.getValue("ID")); //prints the current page number being processed to the console
				}
				//Get the page width and height for Visualiser drawing
				pageWidth = Integer.parseInt(attributes.getValue("WIDTH"));
				pageHeight = Integer.parseInt(attributes.getValue("HEIGHT"));
			} else if(qName.equalsIgnoreCase("ComposedBlock")) {
				String type = attributes.getValue("TYPE");
				if(type.equals("Advertisement")) { isAdvert = true; } //Set the flag for advertisements
				cBCount++;
				if(verbLevel > 3) {
					System.out.print("\nComposed Block: " + Integer.toString(cBCount) + "; Advert? " + isAdvert); //Print block information
				}
			} else if(qName.equalsIgnoreCase("TextBlock")) {
				tBCount++;
				if(verbLevel > 3) {
					System.out.print("\nText Block: " + Integer.toString(tBCount));
				}
				//Set the position of the tempLine if it's the first line of the TextBlock
				if(firstLine) { 
					tempLine = Integer.parseInt(attributes.getValue("VPOS"));
					//If it's the first page, print the header
					if(isFirstPage)	{ strOutput.append("HPOS,VPOS,width,height,space,strings,chars,textBlockNum,tag\n"); }
				}
				currTB = attributes.getValue("ID");
				if(tBTags != null) { //If the tag list isn't empty, get the current tag for the block
					currTag = tBTags.get(currTB);
				}
			} else if(qName.equalsIgnoreCase("TextLine")) {
				if(currTag != null && currTag != "") { //If the line has a tag, process the line, else skip it
					tLCount++;
					if(verbLevel > 3) {
						System.out.print("\n\tText Line: " + Integer.toString(tLCount) + " ");
					}
					analyseLine(attributes); //process the line
				}
			} else if(qName.equalsIgnoreCase("String")) {
				String content = attributes.getValue("CONTENT");
				if(verbLevel > 3) {
					System.out.print(content + " "); //print the line content
				}
				strInLine++;
				charsInLine+=content.length();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException{
		if(isAdvert == false) {
			if(qName.equalsIgnoreCase("TextLine")) {
				//Checks if the line has a tag associated with it, if it does, process, else skip the line
				if(currTag != null && currTag != "") {
					//Get the textBlock number
					String tBNum = (currTB.substring(currTB.lastIndexOf("TB"))).replaceFirst("^(0|[a-zA-Z])+(?!$)", "");
					//Add values to the mode lists for future calculations
					modeLStrings.add(strInLine);
					modeLChars.add(charsInLine);					
					//Add values to the .csv output
					strOutput.append(strInLine);
					strOutput.append(',');
					strOutput.append(charsInLine);
					strOutput.append(',');
					strOutput.append(tBNum);
					strOutput.append(',');
					strOutput.append(currTag);
					strOutput.append('\n'); //End of the .csv output for the line
				}
				//Reset the number of strings and chars in a line, for mode calculations
				strInLine = 0;
				charsInLine = 0;
			} else if(qName.equalsIgnoreCase("Page")) {
				//End of page, calculate the modes
				int mWidth = getModes(modeLWidth);
				int mHeight = getModes(modeLHeight);
				int mStrings = getModes(modeLStrings);
				int mChars = getModes(modeLChars);
				int mSpace = getModes(modeLSpace);
				
				if(verbLevel > 3) {
					//Prints the mode values for the page
					System.out.println("\nLWidth Mode: " + Integer.toString(mWidth));
					System.out.println("LHeight Mode: " + Integer.toString(mHeight));
					System.out.println("Num Strings Mode: " + Integer.toString(mStrings));
					System.out.println("Num Chars Mode: " + Integer.toString(mChars));
					System.out.println("Above Space Mode: " + Integer.toString(mSpace));
				}
				
				//Set the modes array to the appropriate values
				modes[0] = mWidth; modes[1] = mHeight; modes[2] = mStrings; modes[3] = mChars; modes[4] = mSpace;
				//Write the page to the output file
				outputToCSV();
			}
		}
		if(qName.equalsIgnoreCase("ComposedBlock")) {
			isAdvert = false;
		}
	}
	
	/*
	 * Processes the line.
	 * Gets the (x, y, width, height) attributes of the line.
	 * Adds the line as a rectangle to the list.
	 * Adds the properties to the output string and the lists for mode calculations.
	 */
	public void analyseLine(Attributes attributes) {
		int hPos = Integer.parseInt(attributes.getValue("HPOS"));
		int vPos = Integer.parseInt(attributes.getValue("VPOS"));
		int width = Integer.parseInt(attributes.getValue("WIDTH"));
		int height = Integer.parseInt(attributes.getValue("HEIGHT"));
		
		Rectangle r1 = new Rectangle(hPos, vPos, width, height);
		rectList.add(r1);
		
		//Add values to the .csv output string
		strOutput.append(hPos);
		strOutput.append(',');
		strOutput.append(vPos);
		strOutput.append(',');
		strOutput.append(width);
		strOutput.append(',');
		strOutput.append(height);
		strOutput.append(',');
		
		//Add values to the raw lists for mode calculations
		modeLWidth.add(width);
		modeLHeight.add(height);
		
		//Checks if it's the first line, set the height above line to the current line, and the boolean to false
		if(firstLine) {
			heightAbove = tempLine;
			firstLine = false;
		}
		currLine = vPos + height;
		int space = currLine - heightAbove;
		//Calculate the space above the current line to the previous line
		space = (space > spaceCapMax) ? spaceCapMax : (space < spaceCapMin) ? spaceCapMin : space;
		modeLSpace.add(space);
		strOutput.append(space);
		strOutput.append(',');
		heightAbove = currLine; //Set the previous line height to the current line
	}
	
	
	/*
	 * https://stackoverflow.com/questions/15725370/write-a-mode-method-in-java-to-find-the-most-frequently-occurring-element-in-an
	 * Given an array list, calculate and return the mode
	 */
	public int getModes(ArrayList<Integer> intList) {
		HashMap<Integer,Integer> modes = new HashMap<Integer,Integer>();
		int max = 1;
		int temp = 0;
		for(int i = 0; i < intList.size(); i++) {
			if(modes.get(intList.get(i)) != null) {
				int count = modes.get(intList.get(i));
				count++;
				modes.put(intList.get(i), count);
				if(count > max) {
					max = count;
					temp = intList.get(i);
				}
			} else
				modes.put(intList.get(i), 1);
		}
		return temp;
	}
	
	/*
	 * Put data stored in strOutput to the end of the output .csv file
	 * If it's the first page, write to the file, else append to end
	 */
	public void outputToCSV(){
	    try {
	    	FileWriter fw = new FileWriter(outputFile, !isFirstPage);
	    	BufferedWriter bw = new BufferedWriter(fw);
	    	PrintWriter pw = new PrintWriter(bw);
	    	pw.write(strOutput.toString());
	    	pw.flush();
	    	pw.close();
	    	if(verbLevel > 1) {
	    		System.out.print("Output data to: " + outputFile);
	    	}
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (Exception e) {
	         e.printStackTrace();
	    }
	}
	
	/*
	 * Returns the list of rectangle objects for drawing the Visualiser
	 */
	public ArrayList<Rectangle> getRectList(){ return rectList;	}
	
	/*
	 * Returns the page height
	 */
	public int getPageHeight(){ return pageHeight; }
	
	/*
	 * Returns the page width
	 */
	public int getPageWidth(){ return pageWidth; }
	/*
	 * Returns a list of the mode values
	 */
	public int[] getModeValues() { return modes; }
	
	/*
	 * Constructor for the altoMetsHandler Handler
	 * _outputFile stores the name of the file to be output to
	 * _tBTags stores the textBlock IDs and its corresponding position tag
	 * _isFirstPage is used when printing the header
	 * _verbLevel is the verbosity level
	 */
	public altoMetsHandler(String _outputFile, HashMap<String, String> _tBTags, boolean _isFirstPage, int _verbLevel) {
		outputFile = _outputFile;
		tBTags = _tBTags;
		strOutput.setLength(0); //reset the string builder
		isFirstPage = _isFirstPage;
		verbLevel = _verbLevel;
	}
	
	/*
	 * Constructor for the altoMetsHandler Handler, should only be used when there is no mets file
	 * _outputFile stores the name of the file to be output to
	 * _isFirstPage is used when printing the header
	 * _verbLevel is the verbosity level
	 */
	/*public altoMetsHandler(String _outputFile, boolean _isFirstPage, int _verbLevel) {
		outputFile = _outputFile;
		strOutput.setLength(0); //reset the string builder
		isFirstPage = _isFirstPage;
		verbLevel = _verbLevel;
	}*/
}