/**
 * 
 */
package Controllers;

/**
 * The region of the matrix to export to a file
 * @author rleach
 */
public enum Region {
	ALL("All"),VISIBLE("Visible"),SELECTION("Selection");

	private final String toString;
	
	private Region(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static Region getDefault() {
		return(Region.VISIBLE);
	}

	public static Region getRegion(String regName) {
		for(Region reg : Region.values()) {
			if(reg.toString().equals(regName)) {
				return(reg);
			}
		}
		return(null);
	}
}
