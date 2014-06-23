/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: KnnDendroView.java,v $
 * $Revision: 1.2 $
 * $Date: 2006-09-21 17:18:55 $
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
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DendroPanel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.GUIUtils;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.MessagePanel;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.model.KnnModel;

/**
 * This class encapsulates a dendrogram view, which is the classic Eisen
 * treeview. It uses a drag grid panel to lay out a bunch of linked
 * visualizations of the data, a la Eisen. In addition to laying out components,
 * it also manages the GlobalZoomMap. This is necessary since both the GTRView
 * (gene tree) and KnnGlobalView need to know where to lay out genes using the
 * same map.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.2 $ $Date: 2006-09-21 17:18:55 $
 */
public class KnnDendroView2 extends DendroView2 implements
		ConfigNodePersistent, DendroPanel, Observer {

	/**
	 * Constructor for the KnnDendroView object
	 * 
	 * @param tVModel
	 *            model this KnnDendroView is to represent
	 * @param vFrame
	 *            parent ViewFrame of KnnDendroView
	 */
	public KnnDendroView2(final TreeViewFrame vFrame) {

		super(null, vFrame);
	}

	public KnnDendroView2(final Preferences root, final TreeViewFrame vFrame) {

		super(root, vFrame);//, "KnnDendrogram");
	}

	/**
	 * This method should be called only during initial setup of the ModelView
	 * It sets up the views and binds them all to Config nodes.
	 */
	@Override
	protected void setupViews() {

		final DataModel knnModel = getDataModel();
//		statuspanel = new MessagePanel();

		final ColorPresets2 colorPresets = DendrogramFactory.getColorPresets();
		final ColorExtractor2 colorExtractor = new ColorExtractor2();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);

		final KnnArrayDrawer kArrayDrawer = new KnnArrayDrawer();
		kArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = kArrayDrawer;
		// XXX shouldn't need to observer, should be immuable?
		((Observable) getDataModel()).addObserver(arrayDrawer);

		globalview = new GlobalView();

		arraynameview = new ArrayNameView(getDataModel().getArrayHeaderInfo());

		leftTreeDrawer = new LeftTreeDrawer();
		gtrview = new GTRView();
		gtrview.setMap(globalYmap);
		gtrview.setLeftTreeDrawer(leftTreeDrawer);

		invertedTreeDrawer = new TreePainter();
		atrview = new ATRView();


//		arraynameview.setUrlExtractor(viewFrame.getArrayUrlExtractor());

		textview = new TextViewManager(getDataModel().getGeneHeaderInfo(),
				tvFrame.getUrlExtractor(), getDataModel());

		doDoubleLayout();

		// reset persistent popups
		settingsFrame = null;
		settingsPanel = null;

		// set data first to avoid adding auto-genereated contrast to
		// documentConfig.
		kArrayDrawer.setDataMatrix(knnModel.getDataMatrix());

	}

	@Override
	public void setConfigNode(Preferences configNode) {
		
	}
}
