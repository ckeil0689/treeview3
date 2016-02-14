package edu.stanford.genetics.treeview;

public enum PAPER_TYPE {
	US_LETTER("US Letter"), A5("A5");
	
	private final String toString;
	
	private PAPER_TYPE(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}
}
