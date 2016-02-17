package edu.stanford.genetics.treeview;

public enum PaperType {
	A3("A3"), A$("A4"), A5("A5"), A6("A6"), SMALL("Small"), 
	MEDIUM("Medium"), LARGE("LARGE"), BEST_FIT("Best fit");
	
	private final String toString;
	
	private PaperType(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}
}
