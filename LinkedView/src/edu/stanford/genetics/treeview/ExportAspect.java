/**
 * 
 */
package edu.stanford.genetics.treeview;

import java.util.List;

/**
 * The region of the matrix to export to a file
 * @author rleach
 */
public enum ExportAspect {
	ONETOONE("1:1"),ASSEEN("As seen on screen");

	private final String toString;
	
	private ExportAspect(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static ExportAspect getDefault() {
		return(ExportAspect.ONETOONE);
	}

	public static ExportAspect getDefault(final List<ExportAspect> bigAsps) {
		ExportAspect[] priority = {ExportAspect.ONETOONE,ExportAspect.ASSEEN};
		for(ExportAspect asp : priority) {
			if(!bigAsps.contains(asp)) {
				return(asp);
			}
		}
		return(null);
	}

	public static ExportAspect getAspect(String aspName) {
		for(ExportAspect asp : ExportAspect.values()) {
			if(asp.toString().equals(aspName)) {
				return(asp);
			}
		}
		return(null);
	}
}
