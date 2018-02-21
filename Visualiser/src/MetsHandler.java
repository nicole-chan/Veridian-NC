// Nicole Chan (nc83@students.waikato.ac.nz)
import java.util.HashMap;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Handler to convert mets values and convert to a csv
 * 
 * Could potentially include font and case (upper/lower/mixed), doesn't process advertisements currently
 */
class metsHandler extends DefaultHandler{
	String filename = "";
	boolean isLogical = false;
	boolean isAdvert = false;
	
	boolean isTitle_Section = false;
	boolean isHeadline = false;
	boolean isTextBlock = false;
	boolean isContent = false;
	boolean isArticle = false;
	boolean isHeading = false;
	boolean isTitle = false;
	boolean isBody = false;
	boolean isBody_Content = false;
	boolean isParagraph = false;
	boolean isText = false;
	boolean isArea = false;
	
	int verbLevel = 1;
	
	//Stores a stack of the tags currently associated with the data being processed
	Stack<String> tags = new Stack<String>(); 
	
	//stores the hashmap of tags for the text blocks
	private HashMap<String, String> tBTags = new HashMap<String, String>();
	
	/*
	 * Looks at the various types of the div inside the logical structure of the METS file, sets the appropriate flags.
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
		if(isAdvert == false) { //TODO need to add isAdvert = true somewhere, if we need to process advertisements
			if(qName.equalsIgnoreCase("structMap")) {
				String type = attributes.getValue("TYPE");
				if(type.equalsIgnoreCase("LOGICAL")) {
					isLogical = true;
				}
			}
			if(isLogical == true) {
				if(qName.equalsIgnoreCase("div")) {
					String type = attributes.getValue("TYPE");
					//Check if what type of div is being processed, set flags, and push the appropriate tag onto the stack
					if(type.equalsIgnoreCase("TITLE_SECTION")) {
						isTitle_Section = true;
						tags.push("TITLE_SECTION");
					} else if(type.equalsIgnoreCase("HEADLINE")) {
						isHeadline = true;
						tags.push("HEADLINE");
					} else if(type.equalsIgnoreCase("TEXTBLOCK")) {
						isTextBlock = true;
						tags.push("TEXTBLOCK");
					} else if(type.equalsIgnoreCase("CONTENT")) {
						isContent = true;
						tags.push("CONTENT");
					} else if(type.equalsIgnoreCase("ARTICLE")) {
						isArticle = true;
						tags.push("ARTICLE");
					} else if(type.equalsIgnoreCase("HEADING")) {
						isHeading = true;
						tags.push("HEADING");
					} else if(type.equalsIgnoreCase("TITLE")) {
						isTitle = true;
						tags.push("TITLE");
					} else if(type.equalsIgnoreCase("BODY")) {
						isBody = true;
						tags.push("BODY");
					} else if(type.equalsIgnoreCase("BODY_CONTENT")) {
						isBody_Content = true;
						tags.push("BODY_CONTENT");
					} else if(type.equalsIgnoreCase("PARAGRAPH")) {
						isParagraph = true;
						tags.push("PARAGRAPH");
					} else if(type.equalsIgnoreCase("TEXT")) {
						isText = true;
						tags.push("TEXT");
					} else {
						tags.push("irrelevantDiv");
					}
				} else if(qName.equalsIgnoreCase("area")) { //Text blocks (or Compound Blocks)
					isArea = true;
					String begin = attributes.getValue("BEGIN"); //get the name of the Text Block
					if(verbLevel > 3) {
						System.out.println("area:" + getTag()); //begin.toString());
					}
					tBTags.put(begin.toString(), getTag());
				}
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException{
		if(qName.equalsIgnoreCase("structMap")) {
			isLogical = false;
		}
		if(isAdvert == false) {
			if(isLogical == true) {
				if(qName.equalsIgnoreCase("div")) {
					if(tags != null) {
						String currTag = tags.pop().toString();
						//Set the flags for the sections
						if(currTag.equalsIgnoreCase("TITLE_SECTION")) {
							isTitle_Section = false;
						} else if(currTag.equalsIgnoreCase("HEADLINE")) {
							isHeadline = false;
						} else if(currTag.equalsIgnoreCase("TEXTBLOCK")) {
							isTextBlock = false;
						} else if(currTag.equalsIgnoreCase("CONTENT")) {
							isContent = false;
						} else if(currTag.equalsIgnoreCase("ARTICLE")) {
							isArticle = false;
						} else if(currTag.equalsIgnoreCase("HEADING")) {
							isHeading = false;
						} else if(currTag.equalsIgnoreCase("TITLE")) {
							isTitle = false;
						} else if(currTag.equalsIgnoreCase("BODY")) {
							isBody = false;
						} else if(currTag.equalsIgnoreCase("BODY_CONTENT")) {
							isBody_Content = false;
						} else if(currTag.equalsIgnoreCase("PARAGRAPH")) {
							isParagraph = false;
						} else if(currTag.equalsIgnoreCase("TEXT")) {
							isText = false;
						}
					}
				}
			}
		}
	}
	
	/*
	 * Get the tag for the line. i.e. position the textblock is in.
	 * e.g. TITLE_SECTION_HEADLINE
	 * Currently only 5 options:
	 * 		TITLE_SECTION_HEADLINE
	 * 		TITLE_SECTION_TEXTBLOCK
	 * 		CONTENT_ARTICLE_HEADING_TITLE
	 * 		CONTENT_ARTICLE_BODY_BODY_CONTENT_PARAGRAPH
	 * 		CONTENT_SECTION_BODY_BODY_CONTENT_ADVERTISEMENT
	 */
	public String getTag(){    	
    	String tag = "";
		if (isTitle_Section == true) {
			tag += "TITLE_SECTION";
			if (isHeadline == true) {
				tag += "_HEADLINE";
			} else {
				tag += "_TEXTBLOCK";
			}
		} else {
			tag += "CONTENT";
			if (isArticle == true) {
				tag += "_ARTICLE";
				if (isHeading == true) {
					tag += "_HEADING_TITLE";
				} else {
					tag += "_BODY_BODY_CONTENT_PARAGRAPH";
				}
			} else {
				tag += "_SECTION_BODY_BODY_CONTENT_ADVERTISEMENT";
			}
		}
    	return tag;
	}

	/*
	 * Constructor for the mets Handler
	 * _filename stores the name of the file being used
	 */
	public metsHandler(String _filename, int _verbLevel) {
		filename = _filename;
		verbLevel = _verbLevel;
	}
	
	/*
	 * Returns the textblock tags hashmap
	 */
	public HashMap<String, String> getTBTags() { return tBTags; }
}