/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ScatterPanel.java,v $
 * $Revision: 1.6 $
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
package edu.stanford.genetics.treeview.plugin.scatterview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Observable;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.CancelableSettingsDialog;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.ModelessSettingsDialog;
import edu.stanford.genetics.treeview.NoValueException;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.XmlConfig;

/**
 * ScatterPanel make scatterplots from an SPDatasource which are linked to other
 * views by a TreeSelection object.
 * 
 */

public class ScatterPanel extends JPanel implements MainPanel,
		java.util.Observer {
	public String[] getHints() {
		final String[] hints = { "Click to select points", };
		return hints;
	}

	private ConfigNode configNode;

	/** Setter for configNode */
	public void setConfigNode(final ConfigNode configNode) {
		this.configNode = configNode;
	}

	/** Getter for configNode */
	@Override
	public ConfigNode getConfigNode() {
		return configNode;
	}

	ScatterView scatterPane;

	/** Setter for scatterPane */
	public void setScatterPane(final ScatterView scatterPane) {
		this.scatterPane = scatterPane;
	}

	/** Getter for scatterPane */
	public ScatterView getScatterPane() {
		return scatterPane;
	}

	ScatterParameterPanel scatterParameterPanel;
	private TreeViewFrame viewFrame;

	/** Setter for viewFrame */
	public void setViewFrame(final TreeViewFrame viewFrame) {
		this.viewFrame = viewFrame;
	}

	/** Getter for viewFrame */
	public TreeViewFrame getViewFrame() {
		return viewFrame;
	}

	public void scaleScatterPane() {
		System.out.println("scatterPane resized");
	}

	public ScatterPanel(final TreeViewFrame viewFrame,
			final ConfigNode configNode) {
		setViewFrame(viewFrame);
		setLayout(new BorderLayout());
		setConfigNode(configNode);
		final int xType = configNode.getAttribute("xtype", 0);
		final int yType = configNode.getAttribute("ytype", 0);
		final int xIndex = configNode.getAttribute("xindex", 0);
		final int yIndex = configNode.getAttribute("yindex", 0);

		final SPDataSource dataSource = new DataModelSource(xType, yType,
				xIndex, yIndex);
		scatterPane = new ScatterView(dataSource);
		final ScatterColorPresets colorPresets = ScatterplotFactory
				.getColorPresets();
		scatterPane.setDefaultColorSet(colorPresets.getDefaultColorSet());
		scatterPane.setConfigNode(getFirst("ScatterView"));

		/*
		 * scrollPane = new JScrollPane(scatterPane); verticalAxisPane = new
		 * VerticalAxisPane(scatterPane.getYAxisInfo(),
		 * scatterPane.getColorSet());
		 * scrollPane.setRowHeaderView(verticalAxisPane); horizontalAxisPane =
		 * new HorizontalAxisPane(scatterPane.getXAxisInfo(),
		 * scatterPane.getColorSet());
		 * scrollPane.setColumnHeaderView(horizontalAxisPane); add(scrollPane,
		 * BorderLayout.CENTER);
		 */

		add(scatterPane.getComponent(), BorderLayout.CENTER);
		scatterParameterPanel = new ScatterParameterPanel(scatterPane, this);
		add(scatterParameterPanel, BorderLayout.NORTH);

	}

	public void showDisplayPopup() {
		final SettingsPanel displayPanel = new DisplaySettingsPanel(
				scatterPane, ScatterplotFactory.getColorPresets(), viewFrame);
		final JDialog popup = new ModelessSettingsDialog(viewFrame, "Display",
				displayPanel);
		popup.addWindowListener(XmlConfig.getStoreOnWindowClose(getViewFrame()
				.getDataModel().getDocumentConfigRoot()));
		popup.pack();
		popup.setVisible(true);
	}

	// Observer
	@Override
	public void update(final Observable o, final Object arg) {
		if (o == selection) {
			scatterPane.selectionChanged();
		} else {
			System.out.println("Scatterview got funny update!");
		}
	}

	TreeSelectionI selection;

	public void setSelection(final TreeSelectionI selection) {
		if (this.selection != null) {
			this.selection.deleteObserver(this);
		}
		this.selection = selection;
		this.selection.addObserver(this);
	}

	// main Panel
	/**
	 * This syncronizes the sub compnents with their persistent storage.
	 */
	@Override
	public void syncConfig() {
	}

	/**
	 * Add items related to settings
	 * 
	 * @param menu
	 *            A menu to add items to.
	 */
	@Override
	public void populateSettingsMenu(final TreeviewMenuBarI menu) {
		menu.addMenuItem("Display...");
//		, new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				showDisplayPopup();
//			}
//		}, 0);
		menu.setMnemonic(KeyEvent.VK_D);
	}

	/**
	 * Add items which do some kind of analysis
	 * 
	 * @param menu
	 *            A menu to add items to.
	 */
	@Override
	public void populateAnalysisMenu(final TreeviewMenuBarI menu) {
	}

	/**
	 * Add items which allow for export, if any.
	 * 
	 * @param menu
	 *            A menu to add items to.
	 */
	@Override
	public void populateExportMenu(final TreeviewMenuBarI menu) {
		menu.addMenuItem("Export to Image...");
//		, new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent) {
//
//				final BitmapScatterViewExportPanel bitmapPanel = new BitmapScatterViewExportPanel(
//						scatterPane);
//				bitmapPanel.setSourceSet(viewFrame.getDataModel().getFileSet());
//
//				final JDialog popup = new CancelableSettingsDialog(viewFrame,
//						"Export to Image...", bitmapPanel);
//				popup.setSize(500, 300);
//				popup.setVisible(true);
//			}
//		});
		menu.setMnemonic(KeyEvent.VK_I);
	}

	/**
	 * ensure a particular index is visible. Used by Find.
	 * 
	 * @param i
	 *            Index of gene in cdt to make visible
	 */
	@Override
	public void scrollToGene(final int i) {
		LogBuffer.println("ScatterPanel.scrollToGene not implemented");
	}

	@Override
	public void scrollToArray(final int i) {
		LogBuffer.println("ScatterPanel.scrollToArray not implemented");
	}

	/*
	 * this class encapsulates the possible ways to extract per-gene stats.
	 */

	public static final int INDEX = 0; // stat is simple gene index
	public static final int RATIO = 1; // stat is an array ratio
	public static final int PREFIX = 2; // stat is a prefix column

	/**
	 * always returns an instance of the node, even if it has to create it.
	 */
	private ConfigNode getFirst(final String name) {
		final ConfigNode cand = getConfigNode().fetchFirst(name);
		return (cand == null) ? getConfigNode().create(name) : cand;
	}

	/**
	 * This class probably belongs in the scatterview package. Oh well.
	 */
	class DataModelSource implements SPDataSource {
		private final int xIndex; // meaningful for RATIO and PREFIX
		private final int xType;

		private final int yIndex; // meaningful for RATIO and PREFIX
		private final int yType;

		@Override
		public int getNumPoints() {
			return getViewFrame().getDataModel().getDataMatrix().getNumRow();
		}

		@Override
		public double getX(final int i) throws NoValueException {
			if (xVals == null)
				setupVals();
			if (xVals[i] == DataModel.NODATA)
				throw new NoValueException("NODATA");
			if (xVals[i] == DataModel.EMPTY)
				throw new NoValueException("EMPTY");
			return xVals[i];
			// return getValue(xType, xIndex, i);
		}

		@Override
		public double getY(final int i) throws NoValueException {
			if (yVals == null)
				setupVals();
			if (yVals[i] == DataModel.NODATA)
				throw new NoValueException("NODATA");
			return yVals[i];
			// return getValue(yType, yIndex, i);
		}

		@Override
		public String getLabel(final int geneIndex) {
			final DataModel tvmodel = getViewFrame().getDataModel();
			final HeaderInfo info = tvmodel.getGeneHeaderInfo();
			return info.getHeader(geneIndex)[info.getIndex("YORF")];
		}

		@Override
		public Color getColor(final int i) {
			if (getViewFrame().geneIsSelected(i)) {
				return scatterPane.getColorSet().getColor("Selected");
			} else {
				return scatterPane.getColorSet().getColor("Data");
			}
		}

		@Override
		public String getTitle() {
			return getXLabel() + " vs. " + getYLabel();
		}

		@Override
		public String getXLabel() {
			return getName(xType, xIndex);
		}

		@Override
		public String getYLabel() {
			return getName(yType, yIndex);
		}

		@Override
		public void select(final int i) {
			getViewFrame().extendRange(i);
		}

		double[] xVals = null;
		double[] yVals = null;

		private void setupVals() {
			final int n = getNumPoints();
			xVals = new double[n];
			yVals = new double[n];
			for (int i = 0; i < n; i++) {
				xVals[i] = getSimpleValue(xType, xIndex, i);
				yVals[i] = getSimpleValue(yType, yIndex, i);
			}
		}

		@Override
		public void select(final double xL, final double yL, final double xU,
				final double yU) {
			if (xVals == null)
				setupVals();
			final int n = getNumPoints();
			int first = -1;
			int last = -1;
			// TreeSelection treeSelection = getViewFrame().getGeneSelection();
			final TreeSelectionI treeSelection = selection;
			for (int i = 0; i < n; i++) {
				final double x = xVals[i];
				if (x == DataModel.NODATA)
					continue;
				final double y = yVals[i];
				if (y == DataModel.NODATA)
					continue;

				if ((x > xL) && (x < xU) && (y > yL) && (y < yU)) {
					// System.out.println("selecting (" +x+ ", " + y +")");
					treeSelection.setIndex(i, true);
					last = i;
					if (first == -1)
						first = i;
					// select(i);
				}
			}
			if (last != -1) {
				if (treeSelection.getMinIndex() == -1) {
					getViewFrame().seekGene(first);
				}
				treeSelection.notifyObservers();
				getViewFrame().scrollToGene(first);
			}
		}

		@Override
		public void deselectAll() {
			getViewFrame().deselectAll();
		}

		@Override
		public boolean isSelected(final int i) {
			return getViewFrame().geneIsSelected(i);
		}

		public DataModelSource(final int xT, final int yT, final int xI,
				final int yI) {
			xType = xT;
			yType = yT;

			xIndex = xI;
			yIndex = yI;
		}

		/**
		 * throws exception on nodata.
		 */
		public double getValue(final int type, final int index,
				final int geneIndex) throws NoValueException {
			if (type == ScatterPanel.INDEX)
				return geneIndex;
			final DataModel tvmodel = getViewFrame().getDataModel();
			if (type == ScatterPanel.RATIO) {
				final DataMatrix dataMatrix = tvmodel.getDataMatrix();
				final double val = dataMatrix.getValue(index, geneIndex);
				if (val == DataModel.NODATA) {
					throw new NoValueException("NODATA");
				} else {
					return val;
				}
			}
			if (type == ScatterPanel.PREFIX) {
				final HeaderInfo info = tvmodel.getGeneHeaderInfo();
				final String sval = info.getHeader(geneIndex)[index];
				if (sval == null) {
					throw new NoValueException("NODATA");
				} else {
					final Double d = new Double(sval);
					return d.doubleValue();
				}
			}
			System.out.println("Illegal Type Specified");
			throw new NoValueException("Illegal Type Specified");
		}

		/**
		 * just returns the value, even if it's no data.
		 */
		public double getSimpleValue(final int type, final int index,
				final int geneIndex) {
			final DataModel tvmodel = getViewFrame().getDataModel();
			switch (type) {
			case ScatterPanel.INDEX:
				return geneIndex;
			case ScatterPanel.RATIO:
				final DataMatrix dataMatrix = tvmodel.getDataMatrix();
				return dataMatrix.getValue(index, geneIndex);
			case ScatterPanel.PREFIX:
				final HeaderInfo info = tvmodel.getGeneHeaderInfo();
				final String sval = info.getHeader(geneIndex)[index];
				if (sval == null) {
					return DataModel.NODATA;
				} else {
					final Double d = new Double(sval);
					return d.doubleValue();

				}
			}
			System.out.println("Illegal Type Specified");
			return DataModel.NODATA;
		}

		public String getName(final int type, final int index) {
			if (type == ScatterPanel.INDEX) {
				return "INDEX";
			}
			final DataModel tvmodel = getViewFrame().getDataModel();
			if (type == ScatterPanel.RATIO) {
				final HeaderInfo info = tvmodel.getArrayHeaderInfo();
				return info.getHeader(index)[0];
			}
			if (type == ScatterPanel.PREFIX) {
				final HeaderInfo info = tvmodel.getGeneHeaderInfo();
				return info.getNames()[index];
			}
			return null;
		}

	}

	private static ImageIcon scatterIcon = null;

	/**
	 * icon for display in tabbed panel
	 */
	@Override
	public ImageIcon getIcon() {
		if (scatterIcon == null) {
			try {
				scatterIcon = new ImageIcon("images/plot.gif", "Plot Icon");
			} catch (final java.security.AccessControlException e) {
				// need form relative URL somehow...
			}
		}
		return scatterIcon;
	}

	@Override
	public void export(final MainProgramArgs args) throws ExportException {
		throw new ExportException("Export not implemented for plugin "
				+ getName());
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub

	}

}
