/**
 * 
 */
package edu.stanford.genetics.treeview;

import Controllers.Region;

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
}
