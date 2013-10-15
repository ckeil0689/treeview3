/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: avsegal
 * $RCSfile: AtrTVModel.java
 * $Revision: 
 * $Date: Jun 25, 2004
 * $Name:  
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

/**
 * @author avsegal
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */


import java.awt.Frame;
import java.awt.MenuItem;

import edu.stanford.genetics.treeview.*;


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
	public double getValue(int x, int y) {
		return -1;
	}
	@Override
	public void setExprData(double [] newData) {
	}
	ConfigNode documentConfig = new DummyConfigNode("AtrTVModel");
	@Override
	public ConfigNode getDocumentConfigRoot() {
		return documentConfig;
	}
	
	@Override
	public void setDocumentConfig(XmlConfig newVal) {
	}
	
	@Override
	public void setFrame(Frame f) {
	}
	
	@Override
	public Frame getFrame() {
			return null;
	}
	
	public MenuItem getStatMenuItem() {
		return null;
	}
	
	@Override
	public void loadNew(FileSet fileSet) throws LoadException {
		resetState();
		setSource(fileSet);
		final AtrTVModelLoader loader = new AtrTVModelLoader(this);
		loader.loadInto(); 
	}
	
}