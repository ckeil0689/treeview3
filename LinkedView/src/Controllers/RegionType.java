/**
 * 
 */
package Controllers;

import java.util.List;

/**
 * The region of the matrix to export to a file
 * @author rleach
 */
public enum RegionType {
	ALL("All"),VISIBLE("Visible"),SELECTION("Selection");

	private final String toString;
	
	private RegionType(String toString) {
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
	public static RegionType getDefault() {
		return(RegionType.VISIBLE);
	}

	/**
	 * Get the default when only selections may be invalid
	 * 
	 * @param selectionsExist
	 * @return
	 */
	public static RegionType getDefault(final boolean selectionsExist) {
		return(getDefault() == RegionType.SELECTION && !selectionsExist ?
			RegionType.VISIBLE : RegionType.SELECTION);
	}

	/**
	 * Returns the first region not in a list of regions that are too big, based
	 * on a list ordered by priority and whether or not selections exist
	 * 
	 * @param bigRegs
	 * @return
	 */
	public static RegionType getDefault(final List<RegionType> bigRegs,
		final boolean selectionsExist) {

		RegionType[] priority = {RegionType.VISIBLE,RegionType.ALL,RegionType.SELECTION};
		for(RegionType reg : priority) {
			if(!bigRegs.contains(reg) &&
				(reg != RegionType.SELECTION || selectionsExist)) {

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
	public static RegionType getRegion(String regName) {
		for(RegionType reg : RegionType.values()) {
			if(reg.toString().equals(regName)) {
				return(reg);
			}
		}
		return(null);
	}
}
