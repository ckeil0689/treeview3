package edu.stanford.genetics.treeview.model;

import java.util.List;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * Parses supplied tree data (node-node-value) and 
 */
public class ModelTreeAdder {
	private final TVModel targetModel;
	
	private String[] gtrLabelTypes = new String[] {};
	private String[][] gtrLabels = new String[][] {};
	private String[] atrLabelTypes = new String[] {};
	private String[][] atrLabels = new String[][] {};
	
	public ModelTreeAdder(final TVModel targetModel) {
		
		this.targetModel = targetModel;
	}
	
	/**
	 * Parses column tree data obtained from clustering.
	 * @param atrData - A list of String arrays in which each entry 
	 * describes a node-node-value pair.
	 * @return whether trees were successfully parsed
	 */
	public boolean parseATR(final List<String[]> atrData) {
		
		if(targetModel == null) {
			LogBuffer.println("Cannot parse column tree data because no model was " +
			                  "specified.");
			return false;
		}
		
		// in case an atr file exists but is empty */
		if(atrData == null || atrData.isEmpty()) {
			LogBuffer.println("ATR file empty.");
			return false;
		}

		final String[] firstRow = atrData.get(0);
		if( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {

			// okay, need to assign label types...
			setAtrLabelTypes(new String[] {	"NODEID", "LEFT", "RIGHT",
																									"CORRELATION"});

			final String[][] atrLabels = new String[atrData.size()][];
			for(int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i);
			}
			setAtrLabels(atrLabels);
		}
		else {// first row of tempVector is actual label type names...
			setAtrLabelTypes(firstRow);

			final String[][] atrLabels = new String[atrData.size() - 1][];
			for(int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i + 1);
			}
			setAtrLabels(atrLabels);
		}
		
		return true;
	}
	
	/**
	 * Parses row tree data obtained from clustering.
	 * @param gtrData - A list of String arrays in which each entry 
	 * describes a node-node-value pair.
	 * @return whether trees were successfully parsed
	 */
	public boolean parseGTR(final List<String[]> gtrData) {

		if(targetModel == null) {
			LogBuffer.println("Cannot parse row tree data because no model was " +
			                  "specified.");
			return false;
		}
		
		// in case an gtr file exists but is empty
		if(gtrData == null || gtrData.isEmpty()) {
			LogBuffer.println("GTR file empty.");
			return false;
		}

		final String[] firstRow = gtrData.get(0);
		if( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {
			// okay, need to assign label types...
			setGtrLabelTypes(new String[] {	"NODEID", "LEFT", "RIGHT",
																									"CORRELATION"});

			final String[][] gtrLabels = new String[gtrData.size()][];
			for(int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i);
			}
			setGtrLabels(gtrLabels);
		}
		else {// first row of tempVector is actual label type names...
			setGtrLabelTypes(firstRow);

			final String[][] gtrLabels = new String[gtrData.size() - 1][];
			for(int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i + 1);
			}
			setGtrLabels(gtrLabels);
		}

		return true;
	}

	
	public String[] getGtrLabelTypes() {
		return gtrLabelTypes;
	}

	
	public void setGtrLabelTypes(String[] gtrLabelTypes) {
		this.gtrLabelTypes = gtrLabelTypes;
	}

	
	public String[][] getGtrLabels() {
		return gtrLabels;
	}

	
	public void setGtrLabels(String[][] gtrLabels) {
		this.gtrLabels = gtrLabels;
	}

	
	public String[] getAtrLabelTypes() {
		return atrLabelTypes;
	}

	
	public void setAtrLabelTypes(String[] atrLabelTypes) {
		this.atrLabelTypes = atrLabelTypes;
	}

	
	public String[][] getAtrLabels() {
		return atrLabels;
	}

	
	public void setAtrLabels(String[][] atrLabels) {
		this.atrLabels = atrLabels;
	}
}
