/**
 * 
 */
package Controllers;

/**
 * The export file formats supported.
 * @author rleach
 */
public enum FormatType {
	PDF("PDF"),SVG("SVG"),PS("PS"),PNG("PNG"),JPG("JPG"),PPM("PPM");

	private final String toString;
	
	private FormatType(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static FormatType getDefault() {
		return(FormatType.PNG);
	}

	public static FormatType getDefaultDocumentFormat() {
		return(FormatType.PDF);
	}

	public static FormatType[] getHiResFormats() {
		FormatType[] hiResFormats = {PDF,SVG,PS,PNG,PPM};
		return(hiResFormats);
	}

	public static FormatType[] getDocumentFormats() {
		FormatType[] docFormats = {PDF,SVG,PS};
		return(docFormats);
	}

	public static FormatType[] getImageFormats() {
		FormatType[] imgFormats = {PNG,JPG,PPM};
		return(imgFormats);
	}

	public boolean isDocumentFormat() {
		FormatType[] docFormats = FormatType.getDocumentFormats();
		for(int i = 0;i < docFormats.length;i++) {
			if(this == docFormats[i]) {
				return(true);
			}
		}
		return(false);
	}
}
