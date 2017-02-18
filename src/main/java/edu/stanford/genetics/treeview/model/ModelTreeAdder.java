package edu.stanford.genetics.treeview.model;

import java.util.List;

import edu.stanford.genetics.treeview.LogBuffer;

public class ModelTreeAdder {
	private final TVModel targetModel;
	
	public ModelTreeAdder(final TVModel targetModel) {
		
		this.targetModel = targetModel;
	}
	
	public void parseATR(final List<String[]> atrData, final boolean hasAID) {
		
		/* In case an atr file exists but is empty */
		if(atrData == null || atrData.isEmpty()) {
			LogBuffer.println("ATR file empty.");
			targetModel.aidFound(false);
			return;
		}

		final String[] firstRow = atrData.get(0);
		if( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {

			// okay, need to assign label types...
			targetModel.setAtrLabelTypes(new String[] {	"NODEID", "LEFT", "RIGHT",
																									"CORRELATION"});

			final String[][] atrLabels = new String[atrData.size()][];
			for(int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i);
			}
			targetModel.setAtrLabels(atrLabels);
		}
		else {// first row of tempVector is actual label type names...
			targetModel.setAtrLabelTypes(firstRow);

			final String[][] atrLabels = new String[atrData.size() - 1][];
			for(int i = 0; i < atrLabels.length; i++) {
				atrLabels[i] = atrData.get(i + 1);
			}
			targetModel.setAtrLabels(atrLabels);
		}

		targetModel.hashAIDs();
		targetModel.hashATRs();
		targetModel.aidFound(hasAID);
	}
	
	public void parseGTR(final List<String[]> gtrData, final boolean hasGID) {

		// In case an gtr file exists but is empty
		if(gtrData == null || gtrData.isEmpty()) {
			LogBuffer.println("GTR file empty.");
			targetModel.gidFound(false);
			return;
		}

		final String[] firstRow = gtrData.get(0);
		if( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& !(firstRow[0].equalsIgnoreCase("NODEID"))) {
			// okay, need to assign label types...
			targetModel.setGtrLabelTypes(new String[] {	"NODEID", "LEFT", "RIGHT",
																									"CORRELATION"});

			final String[][] gtrLabels = new String[gtrData.size()][];
			for(int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i);
			}
			targetModel.setGtrLabels(gtrLabels);

		}
		else {// first row of tempVector is actual label type names...
			targetModel.setGtrLabelTypes(firstRow);

			final String[][] gtrLabels = new String[gtrData.size() - 1][];
			for(int i = 0; i < gtrLabels.length; i++) {
				gtrLabels[i] = gtrData.get(i + 1);
			}
			targetModel.setGtrLabels(gtrLabels);
		}

		targetModel.hashGIDs();
		targetModel.hashGTRs();
		targetModel.gidFound(hasGID);
	}
}
