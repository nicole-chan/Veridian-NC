// Nicole Chan (nc83@students.waikato.ac.nz)
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Handler to normalise raw values and convert to a csv
 * 
 * Could potentially include font case (upper/lower/mixed), also doesn't process advertisements currently.
 */
class NormaliseAMHandler extends DefaultHandler{
	boolean isAdvert = false;
	int[] modes = new int[5]; //the list of modes to divide the values by
	String outputFile = "";
	int tBHeight = 0;
	boolean firstLine = true;
	int heightAbove = 0;
	int tempLine = 0;
	int charsInLine = 0;
	int strInLine = 0;
	int verbLevel = 1; //set default verbosity level to 1
	int numNullTags = 0;
	private boolean isFirstPage = true;
	
	//Set the maximum and minimum space above a line to 10 and -10
	int spaceCap = 10;
	int spaceCapMin = -10;
	
	StringBuilder strOutput = new StringBuilder(); //Stores the string to print to the .csv file
	
	HashMap<String, String> tBTags; //Textblock tags e.g. TITLE_SECTION_HEADLINE
	
	String currTB, currTag = "";

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
		if(isAdvert == false) {
			if(qName.equalsIgnoreCase("ComposedBlock")) {
				String type = attributes.getValue("TYPE");
				//Check if the current block is an advertisement, set flag
				if(type.equalsIgnoreCase(("Advertisement"))) {
					isAdvert = true;
				}
			} else if(qName.equalsIgnoreCase("TextBlock")) {
				tBHeight = Integer.parseInt(attributes.getValue("HEIGHT")); //Get height of current textblock
				if(firstLine) { //Set the vertical position to the current line if it's the first line of the page
					tempLine = Integer.parseInt(attributes.getValue("VPOS"));
				}
				currTB = attributes.getValue("ID");
				if(tBTags != null) { //If the tag list isn't empty, get the current tag for the block
					currTag = tBTags.get(currTB);
				}
			} else if(qName.equalsIgnoreCase("TextLine")) {
				if(currTag != null && currTag != "") { //If the line has a tag, process the line
					if(firstLine) { //set the line position values if it's the first line of the page
						heightAbove = tempLine;
						firstLine = false;
						if(isFirstPage) { //if it's also the first page, add the header to the page
							strOutput.append("HPOS,VPOS,width,height,space,strings,chars,textBlockNum,tag\n");
						}
					}
					analyseLine(attributes); //process the line
				} else { //No tag for the line
					numNullTags++; //Increase the number of null tags count for the page
					if (verbLevel == 2) { //Print error to console
						System.out.print("\nError: Null tag on " + attributes.getValue("ID").toString() + " ");
					}
				}
			} else if(qName.equalsIgnoreCase("String")) {
				String content = attributes.getValue("CONTENT");
				charsInLine+=content.length();
				strInLine++;
				if((currTag == null && verbLevel == 2) || (currTag == null && verbLevel == 2)) { //Print error - line content, to the console
					System.out.print(content + " ");
				}
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException{
		if(isAdvert == false) {
			if(qName.equalsIgnoreCase("TextLine")) {
				//Checks if the line has a tag associated with it, if it does, process, else skip the line
				if(currTag != null && currTag != "") {
					float nStrings = (float)strInLine/modes[2]; //Normalised strings value = num strings in current line / num strings mode
					float nChars = (float)charsInLine/modes[3]; //Normalised strings value = num strings in current line / num strings mode
					//Get the textBlock number
					String tBNum = (currTB.substring(currTB.lastIndexOf("TB"))).replaceFirst("^(0|[a-zA-Z])+(?!$)", "");
					//Add values to the .csv output
					strOutput.append(String.format("%.2f", nStrings));
					strOutput.append(',');
					strOutput.append(String.format("%.2f", nChars));
					strOutput.append(',');
					strOutput.append(tBNum);
					strOutput.append(',');
					strOutput.append(currTag);
					strOutput.append('\n'); //End of the .csv output for the line
				}
				//Reset the number of strings and chars in a line, for calculations
				strInLine = 0;
				charsInLine = 0;
			} else if(qName.equalsIgnoreCase("Page")) {
				outputToCSV(); //Write the page to the output file
				//If verb level is low and there were null tags on the page, print the number of null tags
				if(verbLevel == 1 && numNullTags > 0) {
					System.out.println(numNullTags + " null tags on Page.");
					System.out.println("Run again with a higher verbosity level (2 or higher), to see detailed null tag entries");
				}
			}
		}
		if(qName.equalsIgnoreCase("ComposedBlock")) {
			isAdvert = false;
		}
	}
	
	/*
	 * Put data stored in strOutput to the end of the output .csv file
	 * Should be normalised values of vertical pos, horizontal pos, width, height, space above, num strings in a line, num chars in a line, textblock number, tag
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
	 * Processes the line.
	 * Gets the (x, y, width, height) attributes of the line.
	 * Calculates the normalised values
	 * Adds the properties to the output string and the lists for mode calculations.
	 */
	public void analyseLine(Attributes attributes) {
		float hPos = Float.parseFloat(attributes.getValue("HPOS"));
		float vPos = Float.parseFloat(attributes.getValue("VPOS"));
		float width = Float.parseFloat(attributes.getValue("WIDTH"));
		float height = Float.parseFloat(attributes.getValue("HEIGHT"));
		
		try {
			float nHPos = hPos/modes[0]; 	 //Normalised horizontal position = h position/width mode
			float nVPos = vPos/tBHeight; 	 //Normalised vertical position = v position/height of textblock
			float nWidth = width/modes[0]; 	 //Normalised width = line width/width mode
			float nHeight = height/modes[1]; //Normalised height = line width/height mode 
			
			int currLine = (int)(vPos + height);
			float nSpace = (float)(currLine - heightAbove)/modes[4]; //Normalised space = (v position + height of line - v position of line above) / space
			nSpace = (nSpace > spaceCap) ? spaceCap : (nSpace < spaceCapMin) ? spaceCapMin : nSpace; //checks if the normalised value is within the range
			heightAbove = currLine;
			
			//Add values to the .csv output string
			strOutput.append(String.format("%.2f", nHPos));
			strOutput.append(',');
			strOutput.append(String.format("%.2f", nVPos));
			strOutput.append(',');
			strOutput.append(String.format("%.2f", nWidth));
			strOutput.append(',');
			strOutput.append(String.format("%.2f", nHeight));
			strOutput.append(',');
			strOutput.append(String.format("%.2f", nSpace));
			strOutput.append(',');
		} catch(ArithmeticException ae) {
			ae.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Constructor for the normalise Handler
	 * _modes stores the mode values from the altoMetsHandler
	 * _filename stores the name of the file being used
	 * _tBTags stores the textBlock ids and its corresponding position tag
	 */
	public NormaliseAMHandler(int[] _modes, String _outputFile, HashMap<String, String> _tBTags, boolean _isFirstPage, int _verbLevel) {
		modes = _modes;
		outputFile = _outputFile;
		tBTags = _tBTags;
		strOutput.setLength(0);
		isFirstPage = _isFirstPage;
		verbLevel = _verbLevel;
	}
	
	/*
	 * Constructor for the normalise Handler, should only be used when there is no mets file
	 * _modes stores the mode values from the altoMetsHandler
	 * _filename stores the name of the file being used
	 * _tBTags stores the textBlock ids and its corresponding position tag
	 */
	/*public NormaliseAMHandler(int[] _modes, String _outputFile, boolean _isFirstPage, int _verbLevel) {
		modes = _modes;
		outputFile = _outputFile;
		strOutput.setLength(0);
		isFirstPage = _isFirstPage;
		verbLevel = _verbLevel;
	}*/
}