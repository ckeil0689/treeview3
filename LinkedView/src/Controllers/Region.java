/**
 * 
 */
package Controllers;

import java.util.List;

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

	/**
	 * Returns the first region not in a list of regions that are too big, based on a list ordered by priority
	 * 
	 * @param bigRegs
	 * @return
	 */
	public static Region getDefault(final List<Region> bigRegs,
		final boolean selectionsExist) {

		Region[] priority = {Region.VISIBLE,Region.ALL,Region.SELECTION};
		for(Region reg : priority) {
			if(!bigRegs.contains(reg) &&
				(reg != Region.SELECTION || selectionsExist)) {

				return(reg);
			}
		}

		return(null);
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
