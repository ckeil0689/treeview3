package edu.stanford.genetics.treeview;

import java.awt.Dimension;

public enum PaperType {
	INTERNATIONAL("International"),A4("A4"),LETTER("Letter"),A3("A3"),
	LEGAL("Legal"),A5("A5"),A6("A6"),EXECUTIVE("Executive"),LEDGER("Ledger");
	
	private final String toString;
	public final static int LONGSIDE = 450;
	
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
	 * @param typeName
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
	
	/**
	 * Get a dimension for a PaperType which can be used to set sizes for
	 * the export dialog preview components.
	 * Info taken from https://en.wikipedia.org/wiki/Paper_size#ANSI_paper_sizes
	 * @param type - The PaperType for which to get a Dimension.
	 * @return A Dimension representative of the selected PaperType with the
	 * longer side always being length 400. Orientation is always portrait.
	 */
	public static Dimension getDimension(PaperType type) {
		
		/* US paper aspect ratios (long side / short side) */
		double letterRatio = 11.0 / 8.5;
		double interntlRatio = 1.0;
		double legalRatio = 14.0 / 8.5;
		double ledgerRatio = 17.0 / 11.0;
		double executiveRatio = 10.5 / 7.25;
		
		/* The aspect ratio of ISO A series paper is sqrt(2). */
		int shortSideASeries = (int)(LONGSIDE / Math.sqrt(2.0));
		
		/* Long side is always 400 (max), short side adjusted accordingly. */
		/* Not certain for international, default until it's cleared up */
		int shortSideInterntl = (int)(LONGSIDE / interntlRatio);
		int shortSideLegal = (int)(LONGSIDE / legalRatio);
		int shortSideLetter = (int)(LONGSIDE / letterRatio);
		int shortSideLedger = (int)(LONGSIDE / ledgerRatio);
		int shortSideExecutive = (int)(LONGSIDE / executiveRatio);
		
		Dimension typeDim;
		switch(type) {
		/* Fall through, ISO A series paper size have the same aspect ratio */
		case A3:
		case A4:
		case A5:
		case A6:
			typeDim = new Dimension(shortSideASeries, LONGSIDE);
			break;
		case INTERNATIONAL:
			typeDim = new Dimension(shortSideInterntl, LONGSIDE);
			break;
		case LETTER:
			typeDim = new Dimension(shortSideLetter, LONGSIDE);
			break;
		case LEGAL:
			typeDim = new Dimension(shortSideLegal, LONGSIDE);
			break;
		case EXECUTIVE:
			typeDim = new Dimension(shortSideExecutive, LONGSIDE);
			break;
		case LEDGER:
			typeDim = new Dimension(shortSideLedger, LONGSIDE);
			break;
		default:
			typeDim = new Dimension(LONGSIDE, LONGSIDE);
			break;
		}
		return typeDim;
	}
}
