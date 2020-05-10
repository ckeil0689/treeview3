/**
 * 
 */
package model.export.trees;

/**
 * The model.export file formats supported.
 */
public enum TreeExportOption {
	YES("Yes"),NO("No"),AUTO("Auto");

	private final String toString;
	
	private TreeExportOption(String toString) {
		this.toString = toString;
	}
	 
	@Override
	public String toString() {
		return toString;
	}

	public static TreeExportOption getDefault() {
		return(TreeExportOption.AUTO);
	}

	public static TreeExportOption getLabelExportOption(String optName) {
		for(TreeExportOption leo : TreeExportOption.values()) {
			if(leo.toString().equals(optName)) {
				return(leo);
			}
		}
		return(null);
	}
}
