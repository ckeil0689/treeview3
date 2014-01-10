/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TVModel.java,v $f
 * $Revision: 1.37 $
 * $Date: 2008-04-23 23:29:19 $
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

import java.awt.Frame;
import java.util.Hashtable;
import java.util.Observable;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.FileSetListener;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.XmlConfig;

public class TVModel extends Observable implements DataModel {

	protected Frame frame;
	protected FileSet source = null;
	protected String dir = null;
	protected String root;

	protected TVDataMatrix dataMatrix;

	protected IntHeaderInfo arrayHeaderInfo;
	protected GeneHeaderInfo geneHeaderInfo;
	protected IntHeaderInfo atrHeaderInfo;
	protected IntHeaderInfo gtrHeaderInfo;

	protected boolean aidFound = false;
	protected boolean gidFound = false;

	protected boolean eweightFound = false;
	protected boolean gweightFound = false;
	protected XmlConfig documentConfig; // holds document config

	/** has model been successfully loaded? */
	private boolean loaded = false;
	private int appendIndex = -1;

	/*
	 * For cases where we are comparing two models (this needs to be changed).
	 */
	private TVModel compareModel = null;
	private int extraCompareExpr = 0;

	public TVModel() {

		super();
		/* build TVModel, initially empty... */
		geneHeaderInfo = new GeneHeaderInfo();
		arrayHeaderInfo = new IntHeaderInfo();
		atrHeaderInfo = new IntHeaderInfo();
		gtrHeaderInfo = new IntHeaderInfo();
		dataMatrix = new TVDataMatrix();
	}

	/*
	 * This not-so-object-oriented hack is in those rare instances where it is
	 * not enough to know that we've got a DataModel.
	 */
	@Override
	public String getType() {

		return "TVModel";
	}

	@Override
	public void setModelForCompare(final DataModel m) {
		if (m == null) {
			compareModel = null;
			extraCompareExpr = 0;

		} else {
			compareModel = (TVModel) m;
			extraCompareExpr = compareModel.nExpr() + 2;
		}
		hasChanged();
	}

	// accessor methods
	@Override
	public IntHeaderInfo getGeneHeaderInfo() {

		return geneHeaderInfo;
	}

	@Override
	public IntHeaderInfo getArrayHeaderInfo() {

		return arrayHeaderInfo;
	}

	@Override
	public DataMatrix getDataMatrix() {

		if (compareModel != null) {

		}
		return dataMatrix;
	}

	@Override
	public HeaderInfo getAtrHeaderInfo() {

		return atrHeaderInfo;
	}

	@Override
	public HeaderInfo getGtrHeaderInfo() {

		return gtrHeaderInfo;
	}

	public boolean gweightFound() {

		return gweightFound;
	}

	public int nGene() {

		return geneHeaderInfo.getNumHeaders();
	}

	public int nExpr() {

		return arrayHeaderInfo.getNumHeaders() + extraCompareExpr;
	}

	public void setExprData(final double[] newData) {

		dataMatrix.setExprData(newData);
	}

	public double getValue(final int x, final int y) {

		final int nexpr = nExpr();
		final int ngene = nGene();
		if (x >= nexpr + 2) {
			if (compareModel != null) {
				return compareModel.getValue(x - (nexpr + 2), y); // check
																	// offsets
			}

		} else if (x >= nexpr && y < ngene) {
			return 0; // gray border
		}

		if ((x < nexpr && y < ngene) && (x >= 0 && y >= 0)) {
			return dataMatrix.getValue(x, y);
		}

		return NODATA;
	}

	@Override
	public boolean aidFound() {

		return aidFound;
	}

	public void aidFound(final boolean newVal) {

		aidFound = newVal;
	}

	@Override
	public boolean gidFound() {

		return gidFound;
	}

	public void gidFound(final boolean newVal) {

		gidFound = newVal;
	}

	public void setSource(final FileSet source) {
		this.source = source;
		setChanged();
	}

	@Override
	public String getSource() {
		if (source == null) {
			return "No Data Loaded";

		} else {
			return source.getCdt();
		}
	}

	@Override
	public String getName() {

		return getFileSet().getRoot();
	}

	@Override
	public FileSet getFileSet() {

		return source;
	}

	@Override
	public void clearFileSetListeners() {

		source.clearFileSetListeners();
	}

	@Override
	public void addFileSetListener(final FileSetListener listener) {

		source.addFileSetListener(listener);
	}

	public XmlConfig getDocumentConfig() {

		return documentConfig;
	}

	@Override
	public ConfigNode getDocumentConfigRoot() {

		return documentConfig.getRoot();
	}

	public void setDocumentConfig(final XmlConfig newVal) {

		documentConfig = newVal;
	}

	public void setFrame(final Frame f) {

		frame = f;
	}

	public Frame getFrame() {

		return frame;
	}

	protected void hashAIDs() {

		arrayHeaderInfo.hashIDs("AID");
	}

	protected void hashGIDs() {

		geneHeaderInfo.hashIDs("GID");
	}

	protected void hashATRs() {

		atrHeaderInfo.hashIDs("NODEID");
	}

	protected void hashGTRs() {

		gtrHeaderInfo.hashIDs("NODEID");
	}

	protected static Hashtable<String, Integer> populateHash(
			final HeaderInfo source, final String headerName,
			final Hashtable<String, Integer> target) {

		final int indexCol = source.getIndex(headerName);

		return populateHash(source, indexCol, target);
	}

	protected static Hashtable<String, Integer> populateHash(
			final HeaderInfo source, int indexCol,
			Hashtable<String, Integer> target) {

		if (target == null) {
			target = new Hashtable<String, Integer>(
					(source.getNumHeaders() * 4) / 3, .75f);

		} else {
			target.clear();
		}

		if (indexCol < 0) {
			indexCol = 0;
		}

		for (int i = 0; i < source.getNumHeaders(); i++) {
			target.put(source.getHeader(i)[indexCol], new Integer(i));
		}

		return target;
	}

	/**
	 * Reorders all the arrays in the new ordering.
	 * 
	 * @param ordering
	 *            the new ordering of arrays, must have size equal to number of
	 *            arrays.
	 */
	public void reorderArrays(final int[] ordering) {
		if (ordering == null
				|| ordering.length != dataMatrix.getNumUnappendedCol()) {

			return;
		}

		final DataMatrix data = getDataMatrix();

		final double[] temp = new double[data.getNumUnappendedCol()];
		for (int j = 0; j < data.getNumRow(); j++) {
			for (int i = 0; i < ordering.length; i++) {
				temp[i] = data.getValue(ordering[i], j);
			}

			for (int i = 0; i < ordering.length; i++) {
				data.setValue(temp[i], i, j);
			}
		}

		final String[][] aHeaders = arrayHeaderInfo.getHeaderArray();
		final String[][] temp2 = new String[aHeaders.length][];

		for (int i = 0; i < aHeaders.length; i++) {
			if (i < ordering.length) {
				temp2[i] = aHeaders[ordering[i]];

			} else {
				temp2[i] = aHeaders[i];
			}
		}

		setArrayHeaders(temp2);
		hashAIDs();
		setChanged();
	}

	/**
	 * Reorders all the arrays in the new ordering.
	 * 
	 * @param ordering
	 *            the new ordering of arrays, must have size equal to number of
	 *            arrays
	 */
	public void reorderGenes(final int[] ordering) {

		if (ordering == null || ordering.length != dataMatrix.getNumRow()) { // make
																				// sure
																				// input
																				// to
																				// function
																				// makes
																				// sense

			return;
		}

		final DataMatrix data = getDataMatrix();
		final double[] temp = new double[data.getNumRow()];
		for (int j = 0; j < data.getNumUnappendedCol(); j++) {
			for (int i = 0; i < ordering.length; i++) {
				temp[i] = data.getValue(j, ordering[i]);
			}

			for (int i = 0; i < ordering.length; i++) {
				data.setValue(temp[i], j, i);
			}
		}
		geneHeaderInfo.reorderHeaders(ordering);
		hashGIDs();
		setChanged();
	}

	public void resetState() {

		// reset some state stuff.
		// if (documentConfig != null)
		// documentConfig.store();
		documentConfig = null;
		setLoaded(false);
		aidFound = false;
		gidFound = false;
		source = null;

		eweightFound = false;
		gweightFound = false;

		geneHeaderInfo.clear();
		arrayHeaderInfo.clear();
		atrHeaderInfo.clear();
		gtrHeaderInfo.clear();
		dataMatrix.clear();
	}

	@Override
	public String toString() {

		final String[] strings = toStrings();
		String msg = "";
		for (int i = 0; i < strings.length; i++) {
			msg += strings[i] + "\n";
		}
		return msg;
	}

	public String[] toStrings() {

		final String[] msg = { "Selected TVModel Stats",
				"Source = " + getSource(), "Nexpr   = " + nExpr(),
				"NGeneHeader = " + getGeneHeaderInfo().getNumNames(),
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

	/*
	 * // debug functions private String commonEscapes() { String err =
	 * "Common escapes\n"; err += "ttype TT_EOL = " +
	 * FlatFileStreamTokenizer.TT_EOL; err += " ttype TT_EOF = " +
	 * FlatFileStreamTokenizer.TT_EOF; err += " ttype TT_NUMBER = " +
	 * FlatFileStreamTokenizer.TT_NUMBER; err += " ttype TT_WORD = " +
	 * FlatFileStreamTokenizer.TT_WORD; err += " '\t' = " + '\t'; err +=
	 * " '\n' = " + '\n'; err += " '\r' = " + '\r';
	 * 
	 * return err; }
	 * 
	 * private void printStream(FlatFileStreamTokenizer st) throws IOException {
	 * int tt = st.nextToken(); while (tt != st.TT_EOF) { String msg; switch(tt)
	 * { case FlatFileStreamTokenizer.TT_WORD: msg = "Word: " + st.sval; break;
	 * case FlatFileStreamTokenizer.TT_NUMBER: msg = "Number: " + st.nval;
	 * break; case FlatFileStreamTokenizer.TT_EOL: msg = "EOL:"; break; case
	 * FlatFileStreamTokenizer.TT_NULL: msg = "NULL:"; break; default: msg =
	 * "INVALID TOKEN, tt=" + tt; break; } System.out.println(msg); tt =
	 * st.nextToken(); } }
	 */

	@Override
	public void removeAppended() {

		if (appendIndex == -1) {

			return;
		}

		final int ngene = nGene();
		int nexpr = nExpr();
		final double[] temp = new double[ngene * appendIndex];

		int i = 0;

		for (int g = 0; g < this.dataMatrix.getNumRow(); g++) {
			for (int e = 0; e < nexpr; e++) {
				if (e < appendIndex) {
					temp[i++] = getValue(e, g);
				}
			}
		}
		dataMatrix.setExprData(temp);

		final String[][] tempS = new String[appendIndex][];

		for (int j = 0; j < appendIndex; j++) {
			tempS[j] = arrayHeaderInfo.getHeader(j);
		}

		arrayHeaderInfo.setHeaderArray(tempS);
		nexpr = appendIndex;
		appendIndex = -1;
		setChanged();
	}

	/**
	 * Appends a second matrix to this one provided they have the same height.
	 * Used for comparison of two data sets where the data is displayed side by
	 * side.
	 * 
	 */
	@Override
	public void append(final DataModel m) {

		final int ngene = nGene();
		int nexpr = nExpr();
		if (m == null || m.getDataMatrix().getNumRow() != ngene
				|| appendIndex != -1) {
			System.out.println("Could not compare.");
			return;
		}

		final double[] temp = new double[getDataMatrix().getNumRow()
				* getDataMatrix().getNumCol() + m.getDataMatrix().getNumRow()
				* (m.getDataMatrix().getNumCol() + 1)];

		int i = 0;

		for (int g = 0; g < m.getDataMatrix().getNumRow(); g++) {
			for (int e = 0; e < nexpr + m.getDataMatrix().getNumCol() + 1; e++) {

				if (e < nexpr) {
					temp[i++] = getValue(e, g);

				} else if (e < nexpr + 1) {
					temp[i++] = DataModel.NODATA;

				} else {
					temp[i++] = m.getDataMatrix().getValue(e - nexpr - 1, g);
				}
			}
		}

		final String[][] tempS = new String[getArrayHeaderInfo()
				.getNumHeaders() + m.getArrayHeaderInfo().getNumHeaders() + 1][];

		i = 0;
		for (int j = 0; j < getArrayHeaderInfo().getNumHeaders(); j++) {
			tempS[i++] = getArrayHeaderInfo().getHeader(j);
		}

		tempS[i] = new String[getArrayHeaderInfo().getNumNames()];

		for (int j = 0; j < tempS[i].length; j++) {
			tempS[i][j] = "-----------------------";
		}

		i++;

		for (int j = 0; j < getArrayHeaderInfo().getNumHeaders(); j++) {
			tempS[i++] = getArrayHeaderInfo().getHeader(j);
		}

		arrayHeaderInfo.setHeaderArray(tempS);
		appendIndex = nexpr;
		nexpr += m.getDataMatrix().getNumCol() + 1;
		dataMatrix.setExprData(temp);
		setChanged();
	}

	/**
	 * Really just a thin wrapper around exprData array.
	 * 
	 * @author aloksaldanha
	 * 
	 */
	public class TVDataMatrix implements DataMatrix {

		private boolean modified = false;
		private double[] exprData = null;

		public void clear() {

			exprData = null;
		}

		@Override
		public double getValue(final int x, final int y) {

			final int nexpr = nExpr();
			final int ngene = nGene();
			if ((x < nexpr) && (y < ngene) && (x >= 0) && (y >= 0)) {
				return exprData[x + y * nexpr];

			} else {
				return DataModel.NODATA;
			}
		}

		public void setExprData(final double[] newData) {

			exprData = newData;
		}

		public double[] getExprData() {

			return exprData;
		}

		@Override
		public void setValue(final double value, final int x, final int y) {

			exprData[x + y * getNumCol()] = value;
			setModified(true);
			setChanged();
		}

		@Override
		public int getNumRow() {

			return nGene();
		}

		@Override
		public int getNumCol() {

			return nExpr();
		}

		@Override
		public int getNumUnappendedCol() {

			return appendIndex == -1 ? getNumCol() : appendIndex;
		}

		@Override
		public int getNumUnappendedRow() {

			return appendIndex == -1 ? getNumRow() : appendIndex;
		}

		@Override
		public void setModified(final boolean modified) {

			this.modified = modified;
		}

		@Override
		public boolean getModified() {

			return modified;
		}
	}

	/** holds actual node information for array tree */
	public void setAtrHeaders(final String[][] atrHeaders) {

		atrHeaderInfo.setHeaderArray(atrHeaders);
	}

	/** holds header row from atr file */
	public void setAtrPrefix(final String[] atrPrefix) {

		atrHeaderInfo.setPrefixArray(atrPrefix);
	}

	/** holds actual node information for gene tree */
	public void setGtrHeaders(final String[][] gtrHeaders) {

		gtrHeaderInfo.setHeaderArray(gtrHeaders);
	}

	public void setGtrPrefix(final String[] gtrPrefix) {

		gtrHeaderInfo.setPrefixArray(gtrPrefix);
	}

	public void setArrayHeaders(final String[][] newHeaders) {

		arrayHeaderInfo.setHeaderArray(newHeaders);
	}

	public void setArrayPrefix(final String[] newPrefix) {

		arrayHeaderInfo.setPrefixArray(newPrefix);
	}

	class GeneHeaderInfo extends IntHeaderInfo {

		public int getAIDIndex() {

			return 1;
		}

		public int getGIDIndex() {

			return 0;
		}

		public int getYorfIndex() {

			if (getIndex("GID") == -1) {

				return 0;

			} else {
				return 1;
			}
		}

		public int getNameIndex() {

			if (getIndex("GID") == -1) {
				return 1;

			} else {
				return 2;
			}
		}

		/**
		 * There are two special indexes, YORF and NAME.
		 */
		@Override
		public int getIndex(final String header) {

			final int retval = super.getIndex(header);

			if (retval != -1) {

				return retval;
			}

			if (header.equals("YORF")) {

				return getYorfIndex();
			}

			if (header.equals("NAME")) {

				return getNameIndex();
			}

			return -1;
		}

	}

	public void setGenePrefix(final String[] newVal) {

		geneHeaderInfo.setPrefixArray(newVal);
	}

	public void setGeneHeaders(final String[][] newVal) {

		geneHeaderInfo.setHeaderArray(newVal);
	}

	// loading stuff follows...
	/**
	 * 
	 * 
	 * @param fileSet
	 *            fileset to load
	 * 
	 */
	public void loadNew(final FileSet fileSet) throws LoadException {

		resetState();
		setSource(fileSet);
		final TVModelLoader2 loader = new TVModelLoader2(this);
		loader.loadInto();

		if (!isLoaded()) {
			throw new LoadException("Loading Cancelled", LoadException.INTPARSE);
		}
	}

	/**
	 * Don't open a loading window...
	 */
	public void loadNewNW(final FileSet fileSet) throws LoadException {

		resetState();
		setSource(fileSet);
		final TVModelLoader2 loader = new TVModelLoader2(this);
		loader.loadIntoNW();

		if (!isLoaded()) {
			throw new LoadException("Loading Cancelled", LoadException.INTPARSE);
		}
	}

	/**
	 * @param b
	 */
	public void setEweightFound(final boolean b) {

		eweightFound = b;
	}

	/**
	 * @param b
	 */
	public void setGweightFound(final boolean b) {

		gweightFound = b;
	}

	@Override
	public boolean getModified() {

		return getGtrHeaderInfo().getModified() ||
		// getGeneHeaderInfo().getModified() ||
		// getArrayHeaderInfo().getModified() ||
				getAtrHeaderInfo().getModified();
	}

	@Override
	public boolean isLoaded() {

		return loaded;
	}

	public void setLoaded(final boolean loaded) {

		this.loaded = loaded;
	}

	@Override
	public String getFileName() {
		if (source == null) {
			return "No Data Loaded";

		} else {
			return source.getRoot() + source.getExt();
		}
	}
}
