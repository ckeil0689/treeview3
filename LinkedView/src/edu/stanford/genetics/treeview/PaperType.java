package edu.stanford.genetics.treeview;

import Controllers.Region;

public enum PaperType {
	INTERNATIONAL("International"),A4("A4"),LETTER("Letter"),A3("A3"),
	LEGAL("Legal"),A5("A5"),A6("A6"),EXECUTIVE("Executive"),LEDGER("Ledger");
	
	private final String toString;
	
	private PaperType(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static PaperType getDefault() {
		return(PaperType.LETTER);
	}

	/**
	 * Convert a paper type string to PaperType type.
	 * 
	 * @param regName
	 * @return
	 */
	public static PaperType getPaperType(String typeName) {
		for(PaperType type : PaperType.values()) {
			if(type.toString().equals(typeName)) {
				return(type);
			}
		}
		return(null);
	}
}
