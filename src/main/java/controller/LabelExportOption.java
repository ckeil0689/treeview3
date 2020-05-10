/**
 * 
 */
package controller;

/**
 * The export file formats supported.
 */
public enum LabelExportOption {
	YES("Yes"),NO("No"),SELECTION("Selection");

	private final String toString;
	
	private LabelExportOption(String toString) {
		this.toString = toString;
	}
	 
	@Override
	public String toString() {
		return toString;
	}

	public static LabelExportOption getDefault() {
		return(LabelExportOption.YES);
	}

	/**
	 * Returns the option that will result in the smallest resolution exported
	 * image
	 * 
	 * @return
	 */
	public static LabelExportOption getMinDefault() {
		return(LabelExportOption.NO);
	}

	public static LabelExportOption getLabelExportOption(String optName) {
		for(LabelExportOption leo : LabelExportOption.values()) {
			if(leo.toString().equals(optName)) {
				return(leo);
			}
		}
		return(null);
	}
}
