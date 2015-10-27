/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */


package edu.stanford.genetics.treeview.model;

/**
 * @author avsegal
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.awt.MenuItem;
import java.util.prefs.Preferences;

public class AtrTVModel extends TVModel {

	/**
	 *
	 */
	public AtrTVModel() {
		super();
	}

	@Override
	public String getType() {
		return "AtrTVModel";
	}

	@Override
	public double getValue(final int x, final int y) {
		return -1;
	}

	@Override
	public void setExprData(final double[][] newData) {
	}

	// @Override
	// public void setExprData(final double[][] newData) {
	// }

	Preferences documentConfig = Preferences.userRoot().node(
			this.getClass().getName());

	@Override
	public Preferences getDocumentConfigRoot() {

		return documentConfig;
	}

	@Override
	public void setDocumentConfig(final Preferences newVal) {
	}

	public MenuItem getStatMenuItem() {
		return null;
	}

	// @Override
	// public void loadNew(final FileSet fileSet) throws LoadException {
	// resetState();
	// setSource(fileSet);
	// final AtrTVModelLoader loader = new AtrTVModelLoader(this);
	// // loader.loadInto();
	// loader.load();
	// }

}