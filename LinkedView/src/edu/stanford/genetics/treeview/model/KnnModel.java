/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: KnnModel.java,v $
 * $Revision: 1.16 $
 * $Date: 2008-06-11 01:58:58 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.model;

import java.util.Vector;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
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
				"Source = " + source.getCdt(), "Nexpr   = " + nExpr(),
				"NGeneHeader = " + getRowHeaderInfo().getNumNames(),
				"Ngene   = " + nGene(), "eweight  = " + eweightFound,
				"gweight  = " + gweightFound, "aid  = " + aidFound,
				"gid  = " + gidFound };

		/*
		 * Enumeration e = genePrefix.elements(); msg += "GPREFIX: " +
		 * e.nextElement(); for (; e.hasMoreElements() ;) { msg += " " +
		 * e.nextElement(); }
		 *
		 * e = aHeaders.elements(); msg += "\naHeaders: " + e.nextElement(); for
		 * (; e.hasMoreElements() ;) { msg += ":" + e.nextElement(); }
		 */

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

		final HeaderInfo geneHeader = getRowHeaderInfo();
		// final boolean result = checkCorrespondence(tempTable, geneHeader,
		// ptype);
		final boolean result = checkCorrespondence(labels, geneHeader, ptype);

		if (result) {
			geneHeader.addName("GROUP", geneHeader.getNumNames() - 1);

			for (int row = 0; row < geneHeader.getNumHeaders(); row++) {

				// geneHeader.setHeader(row, "GROUP", tempTable.getString(row,
				// 1));
				geneHeader.setHeader(row, "GROUP", labels[row][1]);
			}
		}
	}

	public void setAClusters(final String[][] labels, final int kagparse) {

		final HeaderInfo arrayHeader = getColumnHeaderInfo();
		// final boolean result = checkCorrespondence(tempTable, arrayHeader,
		// kagparse);
		final boolean result = checkCorrespondence(labels, arrayHeader,
				kagparse);

		if (result) {
			arrayHeader.addName("GROUP", arrayHeader.getNumNames() - 1);

			for (int row = 0; row < arrayHeader.getNumHeaders(); row++) {

				// arrayHeader.setHeader(row, "GROUP",
				// tempTable.getString(row, 1));
				arrayHeader.setHeader(row, "GROUP", labels[row][1]);
			}
		}
	}

	public void parseClusters() throws LoadException {

		gClusterMembers = calculateMembership(getRowHeaderInfo(), "GROUP");
		aClusterMembers = calculateMembership(getColumnHeaderInfo(), "GROUP");
	}

	public int[][] calculateMembership(final HeaderInfo headerInfo,
			final String column) {

		final int groupIndex = headerInfo.getIndex(column);
		if (groupIndex < 0)
			return null;
		final int[] counts = getCountVector(headerInfo, groupIndex);
		final int[][] members = new int[counts.length][];
		for (int i = 0; i < counts.length; i++) {
			members[i] = new int[counts[i]];
		}
		populateMembers(members, headerInfo, groupIndex);
		return members;
	}

	private void populateMembers(final int[][] members,
			final HeaderInfo headerInfo, final int index) {

		final int[] counts = new int[members.length];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {
			final Integer group = new Integer(headerInfo.getHeader(i, index));
			final int g = group.intValue();
			members[g][counts[g]] = i;
			counts[g]++;
		}
	}

	/**
	 * For a column of ints, returns the number of occurrences of each int in
	 * the column.
	 *
	 * @param headerInfo
	 * @param columnIndex
	 * @return
	 */
	private int[] getCountVector(final HeaderInfo headerInfo,
			final int columnIndex) {

		final Vector<Integer> counts = new Vector<Integer>();
		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {
			final Integer group = new Integer(headerInfo.getHeader(i,
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
	 * @param headerInfo
	 * @param ptype
	 * @return true if it matches
	 */
	// private boolean checkCorrespondence(final RectData tempTable,
	// final HeaderInfo headerInfo, final int ptype) {
	//
	// return true;
	// }

	private boolean checkCorrespondence(final String[][] labels,
			final HeaderInfo headerInfo, final int ptype) {

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
