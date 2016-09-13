/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.model;

import java.util.Vector;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.LabelInfo;
import edu.stanford.genetics.treeview.LoadException;

public class KnnModel extends TVModel implements DataModel {
	/**
	 * This not-so-object-oriented hack is in those rare instances where it is
	 * not enough to know that we've got a DataModel.
	 */
	@Override
	public String getType() {

		return "KnnModel";
	}

	// accessor methods
	public int getNumArrayClusters() {

		return aClusterMembers.length;
	}

	public int getNumGeneClusters() {

		return gClusterMembers.length;
	}

	public int[] getArrayClusters() {

		if (aClusterMembers == null)
			return null;
		final int n = aClusterMembers.length;
		final int[] clusters = new int[n];
		for (int i = 0; i < n; i++) {
			clusters[i] = aClusterMembers[i].length;
		}
		return clusters;
	};

	public int[] getGeneClusters() {

		if (gClusterMembers == null)
			return null;
		final int n = gClusterMembers.length;
		final int[] clusters = new int[n];
		for (int i = 0; i < n; i++) {
			clusters[i] = gClusterMembers[i].length;
		}
		return clusters;
	};

	public KnnModel() {

		super();
		/* build KnnModel, initially empty... */
	}

	// /**
	// *
	// *
	// * @param fileSet
	// * fileset to load
	// *
	// */
	// @Override
	// public void loadNew(final FileSet fileSet) throws LoadException {
	//
	// resetState();
	// setSource(fileSet);
	// // final KnnModelLoader loader = new KnnModelLoader(this);
	// // loader.loadInto();
	// final NewKnnModelLoader loader = new NewKnnModelLoader(this);
	// loader.load();
	// }

	// /**
	// * Don't open a loading window...
	// */
	// @Override
	// public void loadNewNW(final FileSet fileSet) throws LoadException {
	//
	// resetState();
	// setSource(fileSet);
	// // final KnnModelLoader loader = new KnnModelLoader(this);
	// // loader.loadIntoNW();
	// final NewKnnModelLoader loader = new NewKnnModelLoader(this);
	// loader.loadIntoNW();
	// }

	@Override
	public String[] toStrings() {

		final String[] msg = { "Selected KnnModel Stats",
				"Source = " + source.getCdt(), "NCols   = " + nCols(),
				"NrowLabelType = " + getRowLabelInfo().getNumLabelTypes(),
				"NRows   = " + nRows(), "eweight  = " + eweightFound,
				"gweight  = " + gweightFound, "aid  = " + aidFound,
				"gid  = " + gidFound };

		return msg;
	}

	static final int gap = 1;

	/**
	 * This method adds a GROUP column to the CDT
	 *
	 * @param tempTable
	 *            - RectData object with two columns, the first of gene names
	 *            and the second of group membership
	 * @param ptype
	 *            the parse type for error reporting.
	 */
	// public void setGClusters(final RectData tempTable, final int ptype) {
	//
	// final HeaderInfo geneHeader = getGeneHeaderInfo();
	// final boolean result = checkCorrespondence(tempTable, geneHeader, ptype);
	//
	// if (result) {
	// geneHeader.addName("GROUP", geneHeader.getNumNames() - 1);
	//
	// for (int row = 0; row < geneHeader.getNumHeaders(); row++) {
	//
	// geneHeader.setHeader(row, "GROUP", tempTable.getString(row, 1));
	// }
	// }
	// }
	//
	// public void setAClusters(final RectData tempTable, final int kagparse) {
	//
	// final HeaderInfo arrayHeader = getArrayHeaderInfo();
	// final boolean result = checkCorrespondence(tempTable, arrayHeader,
	// kagparse);
	//
	// if (result) {
	// arrayHeader.addName("GROUP", arrayHeader.getNumNames() - 1);
	//
	// for (int row = 0; row < arrayHeader.getNumHeaders(); row++) {
	//
	// arrayHeader.setHeader(row, "GROUP",
	// tempTable.getString(row, 1));
	// }
	// }
	// }

	public void setGClusters(final String[][] labels, final int ptype) {

		final LabelInfo rowLabelI = getRowLabelInfo();
		// final boolean result = checkCorrespondence(tempTable, geneHeader,
		// ptype);
		final boolean result = checkCorrespondence(labels, rowLabelI, ptype);

		if (result) {
			rowLabelI.addLabelType("GROUP", rowLabelI.getNumLabelTypes() - 1);

			for (int row = 0; row < rowLabelI.getNumLabels(); row++) {

				// geneHeader.setHeader(row, "GROUP", tempTable.getString(row,
				// 1));
				rowLabelI.setLabel(row, "GROUP", labels[row][1]);
			}
		}
	}

	public void setAClusters(final String[][] labels, final int kagparse) {

		final LabelInfo colLabelI = getColLabelInfo();
		// final boolean result = checkCorrespondence(tempTable, arrayHeader,
		// kagparse);
		final boolean result = checkCorrespondence(labels, colLabelI,
				kagparse);

		if (result) {
			colLabelI.addLabelType("GROUP", colLabelI.getNumLabelTypes() - 1);

			for (int row = 0; row < colLabelI.getNumLabels(); row++) {

				// arrayHeader.setHeader(row, "GROUP",
				// tempTable.getString(row, 1));
				colLabelI.setLabel(row, "GROUP", labels[row][1]);
			}
		}
	}

	public void parseClusters() throws LoadException {

		gClusterMembers = calculateMembership(getRowLabelInfo(), "GROUP");
		aClusterMembers = calculateMembership(getColLabelInfo(), "GROUP");
	}

	public int[][] calculateMembership(final LabelInfo labelInfo,
			final String column) {

		final int groupIndex = labelInfo.getIndex(column);
		if (groupIndex < 0)
			return null;
		final int[] counts = getCountVector(labelInfo, groupIndex);
		final int[][] members = new int[counts.length][];
		for (int i = 0; i < counts.length; i++) {
			members[i] = new int[counts[i]];
		}
		populateMembers(members, labelInfo, groupIndex);
		return members;
	}

	private void populateMembers(final int[][] members,
			final LabelInfo labelInfo, final int index) {

		final int[] counts = new int[members.length];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		for (int i = 0; i < labelInfo.getNumLabels(); i++) {
			final Integer group = new Integer(labelInfo.getLabel(i, index));
			final int g = group.intValue();
			members[g][counts[g]] = i;
			counts[g]++;
		}
	}

	/**
	 * For a column of ints, returns the number of occurrences of each int in
	 * the column.
	 *
	 * @param labelInfo
	 * @param columnIndex
	 * @return
	 */
	private int[] getCountVector(final LabelInfo labelInfo,
			final int columnIndex) {

		final Vector<Integer> counts = new Vector<Integer>();
		for (int i = 0; i < labelInfo.getNumLabels(); i++) {
			final Integer group = new Integer(labelInfo.getLabel(i,
					columnIndex));
			final Integer current = counts.elementAt(group.intValue());
			Integer insertElement = new Integer(1);
			if (current != null) {
				insertElement = new Integer(current.intValue() + 1);
			}
			counts.insertElementAt(insertElement, group.intValue());
		}
		final int[] cv = new int[counts.size()];
		for (int i = 0; i < cv.length; i++) {
			cv[i] = counts.elementAt(i).intValue();
		}
		return cv;
	}

	/**
	 * check to see that the order of names in the first column of the temptable
	 * matches the headerinfo.
	 *
	 * @param tempTable
	 * @param labelInfo
	 * @param ptype
	 * @return true if it matches
	 */
	// private boolean checkCorrespondence(final RectData tempTable,
	// final LabelInfo labelInfo, final int ptype) {
	//
	// return true;
	// }

	private boolean checkCorrespondence(final String[][] labels,
			final LabelInfo labelInfo, final int ptype) {

		return true;
	}

	/**
	 * holds membership of the gene clusters
	 */
	private int[] gClusterMembers[];
	/**
	 * holds membership of the array clusters
	 */
	private int[] aClusterMembers[];

}
