/**
 * 
 */
package Controllers;

import edu.stanford.genetics.treeview.AspectType;

/**
 * The export file formats supported.
 * @author rleach
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
