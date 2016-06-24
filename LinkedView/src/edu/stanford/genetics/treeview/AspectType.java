/**
 * 
 */
package edu.stanford.genetics.treeview;

import java.util.List;

/**
 * The region of the matrix to export to a file
 * @author rleach
 */
public enum AspectType {
	ONETOONE("1:1"), ASSEEN("As seen on screen");

	private final String toString;
	
	private AspectType(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static AspectType getDefault() {
		return(AspectType.ONETOONE);
	}

	public static AspectType getDefault(final List<AspectType> bigAsps) {
		AspectType[] priority = {AspectType.ONETOONE,AspectType.ASSEEN};
		for(AspectType asp : priority) {
			if(!bigAsps.contains(asp)) {
				return(asp);
			}
		}
		return(null);
	}

	public static AspectType getAspect(String aspName) {
		for(AspectType asp : AspectType.values()) {
			if(asp.toString().equals(aspName)) {
				return(asp);
			}
		}
		return(null);
	}
}
