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
}
