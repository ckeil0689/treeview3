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

	/**
	 * Get the default when all options are valid
	 * 
	 * @return
	 */
	public static Region getDefault() {
		return(Region.VISIBLE);
	}

	/**
	 * Get the default when only selections may be invalid
	 * 
	 * @param selectionsExist
	 * @return
	 */
	public static Region getDefault(final boolean selectionsExist) {
		return(getDefault() == Region.SELECTION && !selectionsExist ?
			Region.VISIBLE : Region.SELECTION);
	}

	/**
	 * Returns the first region not in a list of regions that are too big, based
	 * on a list ordered by priority and whether or not selections exist
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

	/**
	 * Convert a region string to Region type.
	 * 
	 * @param regName
	 * @return
	 */
	public static Region getRegion(String regName) {
		for(Region reg : Region.values()) {
			if(reg.toString().equals(regName)) {
				return(reg);
			}
		}
		return(null);
	}
}
