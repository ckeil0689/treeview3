/**
 * 
 */
package Controllers;

/**
 * The export file formats supported.
 * @author rleach
 */
public enum Format {
	PDF("PDF"),SVG("SVG"),PS("PS"),PNG("PNG"),JPG("JPG"),PPM("PPM");

	private final String toString;
	
	private Format(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static Format getDefault() {
		return(Format.PNG);
	}

	public static Format getDefaultDocumentFormat() {
		return(Format.PDF);
	}

	public static Format[] getHiResFormats() {
		Format[] hiResFormats = {PDF,SVG,PS,PNG,PPM};
		return(hiResFormats);
	}

	public static Format[] getDocumentFormats() {
		Format[] docFormats = {PDF,SVG,PS};
		return(docFormats);
	}

	public static Format[] getImageFormats() {
		Format[] imgFormats = {PNG,JPG,PPM};
		return(imgFormats);
	}

	public boolean isDocumentFormat() {
		Format[] docFormats = Format.getDocumentFormats();
		for(int i = 0;i < docFormats.length;i++) {
			if(this == docFormats[i]) {
				return(true);
			}
		}
		return(false);
	}
}
