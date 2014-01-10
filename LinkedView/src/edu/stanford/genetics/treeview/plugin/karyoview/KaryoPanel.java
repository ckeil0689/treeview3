/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: KaryoPanel.java,v $
 * $Revision: 1.7 $
 * $Date: 2009-09-08 11:24:24 $
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
package edu.stanford.genetics.treeview.plugin.karyoview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;

import edu.stanford.genetics.treeview.CancelableSettingsDialog;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DragGridPanel;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.MessagePanel;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.ModelessSettingsDialog;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.SwingWorker;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.UrlSettingsPanel;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.XmlConfig;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class encapsulates a dendrogram view, which is the classic Eisen
 * treeview. It uses a drag grid panel to lay out a bunch of linked
 * visualizations of the data, a la Eisen. In addition to laying out components,
 * it also manages the GlobalZoomMap. This is necessary since both the GTRView
 * (gene tree) and GlobalView need to know where to lay out genes using the same
 * map. The zoom map is managed by the ViewFrame- it represents the selected
 * genes, and potentially forms a link between different views, only one of
 * which is the DendroView.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.7 $ $Date: 2009-09-08 11:24:24 $
 */
public class KaryoPanel extends DragGridPanel implements MainPanel {

	private static final long serialVersionUID = 1L;

	private final Color BLUE2 = new Color(235, 240, 255, 255);

	/**
	 * Constructor for the KaryoPanel object
	 * 
	 * @param tvmodel
	 *            data model to represent
	 * @param geneSelection
	 *            shared selection model
	 * @param vFrame
	 *            parent ViewFrame of DendroView
	 * @param configNode
	 *            config node to take state from and store state in.
	 */
	public KaryoPanel(final DataModel tvmodel,
			final TreeSelectionI geneSelection, final ViewFrame vFrame,
			final ConfigNode configNode) {

		super(2, 3);
		viewFrame = vFrame;

		startingGenome = new Genome(tvmodel);
		genome = startingGenome;

		bindConfig(configNode);
		// requires that genome be set...
		karyoDrawer = new KaryoDrawer(genome, geneSelection, DataModel.NODATA);
		karyoDrawer.bindConfig(getFirst("KaryoDrawer"));
		karyoView = new KaryoView(karyoDrawer, tvmodel);
		karyoView.bindConfig(getFirst("KaryoView"));
		parameterPanel = new KaryoViewParameterPanel(karyoDrawer, karyoView,
				this);
		statusPanel = new MessagePanel("Status", BLUE2);

		windowActive = true;
		setBorderWidth(2);
		setBorderHeight(2);
		setMinimumWidth(1);
		setMinimumHeight(1);
		setFocusWidth(1);
		setFocusHeight(1);

		setupViews();

		coordinatesTimer = new javax.swing.Timer(1000, new TimerListener());
		coordinatesTimer.stop();

		try {
			final CoordinatesPresets coordinatesPresets = KaryoscopeFactory
					.getCoordinatesPresets();
			if (configNode.getAttribute("coordinates", "").length() > 0) {
				final FileSet source = new FileSet(configNode.getAttribute(
						"coordinates", ""), vFrame.getApp().getCodeBase()
						.toString()
						+ "coordinates/");
				getGenome(source);
			} else if (coordinatesPresets.getDefaultIndex() != -1) {
				// requires that karyoView be set, so we can get header info for
				// matching.
				final int index = coordinatesPresets.getDefaultIndex();
				final FileSet def = coordinatesPresets.getFileSet(index);
				// System.out.println("default index is " + index +", fileset "
				// + def);
				getGenome(def);
			} else {
				useOriginal();
			}
		} catch (final LoadException e) {
			LogBuffer.println("Error loading coordinates " + e);
			e.printStackTrace();
			useOriginal();
		}

		final KaryoColorPresets colorPresets = KaryoscopeFactory
				.getColorPresets();

		if (colorPresets.getDefaultIndex() != -1) {
			karyoDrawer.getKaryoColorSet().copyStateFrom(
					colorPresets.getDefaultColorSet());
		}

	}

	public void getGenome(final FileSet fileSet) throws LoadException {
		final TVModel model = new TVModel();
		model.setFrame(viewFrame);
		model.loadNew(fileSet);
		getGenome(model);
	}

	ProgressMonitor coordinatesMonitor;
	javax.swing.Timer coordinatesTimer;
	CoordinatesTask coordinatesTask;
	CoordinatesSettingsPanel coordinatesPanel = null;

	class TimerListener implements ActionListener { // manages the
													// averagermonitor
		@Override
		public void actionPerformed(final ActionEvent evt) {
			if (coordinatesMonitor.isCanceled() || coordinatesTask.done()) {
				coordinatesMonitor.close();
				coordinatesTask.stop();
				// Toolkit.getDefaultToolkit().beep();
				coordinatesTimer.stop();
				if (coordinatesTask.done()) {
					coordinatesMonitor.setNote("Matching complete");
				}
				if (coordinatesPanel != null) {
					coordinatesPanel.setEnabled(true);
				}
			} else {
				coordinatesMonitor.setNote(coordinatesTask.getMessage());
				coordinatesMonitor.setProgress(coordinatesTask.getCurrent());
			}
			repaint();
		}
	}

	class CoordinatesTask {
		private int current = 0;
		private String statMessage;

		/**
		 * Called to start the task. I don't know why we bother with the
		 * ActualTask class, so don't ask.
		 */
		void go(final DataModel tvmodel) {
			final DataModel model = tvmodel;
			setCurrent(0);
			final SwingWorker worker = new SwingWorker() {
				@Override
				public Object construct() {
					return new ActualTask(model);
				}
			};
			worker.start();
		}

		/**
		 * Called from ProgressBarDemo to find out how much work needs to be
		 * done.
		 */
		int getLengthOfTask() {
			final HeaderInfo existingHeaders = karyoView.getGeneInfo();
			return existingHeaders.getNumHeaders();
		}

		/**
		 * Called from ProgressBarDemo to find out how much has been done.
		 */
		int getCurrent() {
			return current;
		}

		void setCurrent(final int i) {
			current = i;
		}

		public void incrCurrent() {
			current++;
		}

		/**
		 * called to stop the averaging on a cancel...
		 */
		void stop() {
			current = getLengthOfTask();
		}

		/**
		 * Called from ProgressBarDemo to find out if the task has completed.
		 */
		boolean done() {
			if (current >= getLengthOfTask()) {
				return true;
			} else {
				return false;
			}
		}

		String getMessage() {
			return statMessage;
		}

		class ActualTask {
			ActualTask(final DataModel newModel) {
				final Genome newGenome = new Genome(newModel);

				// set indexes to -1...
				final int n = newGenome.getNumLoci();
				for (int i = 0; i < n; i++) {
					newGenome.getLocus(i).setCdtIndex(-1);
				}

				statMessage = "Hashing new keys ";
				final HeaderInfo newHeaders = newModel.getGeneHeaderInfo();
				final int newN = newHeaders.getNumHeaders();
				final Hashtable tempTable = new Hashtable((newN * 4) / 3, .75f);
				for (int j = 0; j < newN; j++) {
					tempTable.put(newHeaders.getHeader(j, "YORF"), new Integer(
							j));
				}

				statMessage = "Performing lookups";
				// match up indexes with using headerinfo...
				final HeaderInfo existingHeaders = karyoView.getGeneInfo();
				final int existingN = existingHeaders.getNumHeaders();
				for (int i = 0; i < existingN; i++) {
					incrCurrent();
					final String thisID = existingHeaders.getHeader(i, "YORF");
					if (thisID == null)
						continue;
					if (thisID.equals(""))
						continue;
					final Integer j = (Integer) tempTable.get(thisID);
					if (j != null) {
						newGenome.getLocus(j.intValue()).setCdtIndex(i);
					} else {
						LogBuffer.println("Missing locus for " + thisID);
					}
					if (done())
						break;
				}
				KaryoPanel.this.setGenome(newGenome);
				karyoDrawer.setGenome(newGenome);
				karyoView.recalculateAverages();
				karyoView.redoScale();
				stop();
			}
		}
	}

	public void getGenome(final DataModel newModel) {
		coordinatesTask = new CoordinatesTask();
		coordinatesMonitor = new ProgressMonitor(this,
				"Finding matching coordinates", "Note", 0,
				coordinatesTask.getLengthOfTask());
		coordinatesMonitor.setProgress(0);
		coordinatesTask.go(newModel);
		coordinatesTimer.start();
	}

	public void useOriginal() {
		karyoDrawer.setGenome(startingGenome);
		karyoView.recalculateAverages();
		karyoView.redoScale();
	}

	// accessors
	/**
	 * This method should be called only during initial setup of the modelview
	 * 
	 * It sets up the views, which are reinitialized if the model changes.
	 * 
	 */
	private void setupViews() {

		final Rectangle rectangle = new Rectangle(0, 0, 1, 1);

		addComponent(new JScrollPane(parameterPanel), rectangle);
		rectangle.translate(1, 0);

		addComponent(statusPanel, rectangle);

		rectangle.setSize(2, 2);
		rectangle.translate(-1, 1);
		addComponent(karyoView.getComponent(), rectangle);
		karyoView.setParameterPanel(parameterPanel);
		karyoView.setStatusPanel(statusPanel);
		karyoView.setViewFrame(viewFrame);

		rectangle.translate(0, 1);
		// addView(KaryoZoomView);
	}

	/**
	 * Adds a component to the DendroView
	 * 
	 * @param component
	 *            The component to be added
	 * @param rectangle
	 *            The location to add it in
	 */
	@Override
	public void addComponent(final Component component,
			final Rectangle rectangle) {
		if (component != null) {
			addComponent(component, rectangle.x, rectangle.y, rectangle.width,
					rectangle.height);
		}
	}

	/**
	 * Adds a ModelView to the KaryoPanel
	 * 
	 * @param modelView
	 *            The ModelView to be added
	 * @param rectangle
	 *            The location to add it in
	 */
	public void addView(final ModelView modelView, final Rectangle rectangle) {
		addComponent(modelView.getComponent(), rectangle);
		modelView.setStatusPanel(statusPanel);
		modelView.setViewFrame(viewFrame);
	}

	/**
	 * Determines if window is currently active
	 * 
	 * @return returns true if active
	 */
	public boolean windowActive() {
		return windowActive;
	}

	// MainPanel
	/**
	 * This makes the persistent storage resemble the compnents, if it doesn't
	 * already.
	 */
	@Override
	public void syncConfig() {
	}

	public void showDisplayPopup() {
		final SettingsPanel avePanel = new DisplaySettingsPanel(this,
				KaryoscopeFactory.getColorPresets(), viewFrame);
		final JDialog popup = new ModelessSettingsDialog(viewFrame, "Display",
				avePanel);
		popup.addWindowListener(XmlConfig.getStoreOnWindowClose(viewFrame
				.getDataModel().getDocumentConfigRoot()));
		popup.pack();
		popup.setVisible(true);
	}

	public void showCoordinatesPopup() {
		coordinatesPanel = new CoordinatesSettingsPanel(KaryoPanel.this,
				KaryoscopeFactory.getCoordinatesPresets(), viewFrame);
		final JDialog popup = new ModelessSettingsDialog(viewFrame,
				"Coordinates", coordinatesPanel);
		popup.addWindowListener(XmlConfig.getStoreOnWindowClose(viewFrame
				.getDataModel().getDocumentConfigRoot()));
		popup.pack();
		popup.setVisible(true);
	}

	public void showAveragingPopup() {
		final SettingsPanel avePanel = karyoView.getAveragerSettingsPanel();
		final JDialog popup = new ModelessSettingsDialog(viewFrame,
				"Averaging", avePanel);
		popup.addWindowListener(XmlConfig.getStoreOnWindowClose(viewFrame
				.getDataModel().getDocumentConfigRoot()));
		popup.pack();
		popup.setVisible(true);
	}

	/**
	 * Add items related to settings
	 * 
	 * @param menu
	 *            A menu to add items to.
	 */
	@Override
	public void populateSettingsMenu(final TreeviewMenuBarI menu) {
		menu.addMenuItem("Display...", new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showDisplayPopup();
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_D);

		menu.addMenuItem("Averaging...", new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showAveragingPopup();
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_A);

		menu.addMenuItem("Coordinates...", new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showCoordinatesPopup();
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_C);

		menu.addMenuItem("Url Links...", new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final SettingsPanel urlPanel = new UrlSettingsPanel(viewFrame
						.getUrlExtractor(), viewFrame.getGeneUrlPresets());
				final JDialog popup = new ModelessSettingsDialog(viewFrame,
						"Url Linking", urlPanel);
				popup.pack();
				popup.setVisible(true);
			}
		}, 0);
		menu.setMnemonic(KeyEvent.VK_U);
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
		menu.addMenuItem("Export to Image...", new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent actionEvent) {
				double origPPM, origPPV; // save since export currently changes
											// these.
				origPPM = getKaryoDrawer().getPixelPerMap();
				origPPV = getKaryoDrawer().getPixelPerVal();

				final BitmapKaryoViewExportPanel bitmapPanel = new BitmapKaryoViewExportPanel(
						karyoView);
				bitmapPanel.setSourceSet(viewFrame.getDataModel().getFileSet());

				final JDialog popup = new CancelableSettingsDialog(viewFrame,
						"Export to Image", bitmapPanel);
				popup.pack();
				int width = popup.getWidth();
				int height = popup.getHeight();
				if (width < 500)
					width = 500;
				if (height < 300)
					height = 300;
				popup.setSize(width, height);
				popup.setVisible(true);
				getKaryoDrawer().setPixelPerMap(origPPM);
				getKaryoDrawer().setPixelPerVal(origPPV);
			}
		});
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
		// LogPanel.println("KaryoPanel.scrollToGene not implemented");
	}

	@Override
	public void scrollToArray(final int i) {
		// LogPanel.println("KaryoPanel.scrollToArray not implemented");
	}

	private ConfigNode configNode;

	/** Setter for configNode */
	public void bindConfig(final ConfigNode configNode) {
		this.configNode = configNode;
	}

	/** Getter for configNode */
	@Override
	public ConfigNode getConfigNode() {
		return configNode;
	}

	private final ViewFrame viewFrame;
	private final boolean windowActive;
	/** store original coordinates here... */
	private final Genome startingGenome;
	private Genome genome;

	/** Setter for genome */
	public void setGenome(final Genome genome) {
		final FileSet fileSet = genome.getFileSet();
		configNode.setAttribute("coordinates",
				fileSet.getRoot() + fileSet.getExt(), "");
		this.genome = genome;
	}

	/** Getter for genome */
	public Genome getGenome() {
		return genome;
	}

	private KaryoDrawer karyoDrawer;

	/** Setter for karyoDrawer */
	public void setKaryoDrawer(final KaryoDrawer karyoDrawer) {
		this.karyoDrawer = karyoDrawer;
	}

	/** Getter for karyoDrawer */
	public KaryoDrawer getKaryoDrawer() {
		return karyoDrawer;
	}

	private KaryoView karyoView;

	/** Setter for karyoView */
	public void setKaryoView(final KaryoView karyoView) {
		this.karyoView = karyoView;
	}

	/** Getter for karyoView */
	public KaryoView getKaryoView() {
		return karyoView;
	}

	private final KaryoViewParameterPanel parameterPanel;

	private final MessagePanel statusPanel;

	/**
	 * always returns an instance of the node, even if it has to create it.
	 */
	private ConfigNode getFirst(final String name) {
		final ConfigNode cand = getConfigNode().fetchFirst(name);
		return (cand == null) ? getConfigNode().create(name) : cand;
	}

	private static ImageIcon karyoIcon = null;

	/**
	 * icon for display in tabbed panel
	 */
	@Override
	public ImageIcon getIcon() {
		if (karyoIcon == null) {
			try {
				karyoIcon = new ImageIcon("images/karyoscope.gif",
						"Karyoscope Icon");
			} catch (final java.security.AccessControlException e) {
				// need form relative URL somehow...
			}
		}
		return karyoIcon;
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
