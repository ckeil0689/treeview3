/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: TreeViewFrame.java,v $w
 * $Revision: 1.76 $
 * $Date: 2010-05-02 13:34:53 $
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
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import Cluster.ClusterFileSet;
import Cluster.ClusterFrame;
import Cluster.ClusterModel;
import Cluster.ClusterFrameWindow;
import Cluster.ClusterView;

import edu.stanford.genetics.treeview.core.ArrayFinder;
import edu.stanford.genetics.treeview.core.FileMruEditor;
import edu.stanford.genetics.treeview.core.GeneFinder;
import edu.stanford.genetics.treeview.core.GlobalPrefInfo;
import edu.stanford.genetics.treeview.core.HeaderFinder;
import edu.stanford.genetics.treeview.core.LogMessagesPanel;
import edu.stanford.genetics.treeview.core.LogSettingsPanel;
import edu.stanford.genetics.treeview.core.MemMonitor;
import edu.stanford.genetics.treeview.core.MenuHelpPluginsFrame;
import edu.stanford.genetics.treeview.core.PluginManager;
import edu.stanford.genetics.treeview.core.TreeViewJMenuBar;
import edu.stanford.genetics.treeview.core.TreeViewMenuBar;
import edu.stanford.genetics.treeview.model.DataModelWriter;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

/**
 * This class is the main window of Java TreeView.
 * In practice, it serves as the base class for the LinkedViewFrame and
 * the AppletViewFrame.
 * 
 * @author aloksaldanha
 *
 */
public class TreeViewFrame extends ViewFrame implements FileSetListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** override in subclasses? */
	private static String appName = "TreeView 3.0";

	private ProgramMenu programMenu;
	public String getAppName() {
		return appName;
	}

	public TreeViewFrame(TreeViewApp treeview) {
		this(treeview, appName);
	}

	public TreeViewFrame(TreeViewApp treeview, String appName) {
		super(appName);
		treeView = treeview;
		loaded = false;
		setWindowActive(true);
		waiting = new JPanel();
		waiting.setLayout(new MigLayout());//new BoxLayout(waiting, BoxLayout.Y_AXIS));
//		waiting.setAlignmentX((float) 0.5);
//		waiting.setAlignmentY((float) 0.5);
		JLabel jl = new JLabel("Hello! How are you Gentlepeople?");
		jl.setFont(new Font("Sans Serif", Font.PLAIN, 30));
		jl.setForeground(new Color(60, 180, 220, 255));
		jl.setAlignmentX((float) 0.5);
		jl.setAlignmentY((float) 0.5);
		waiting.add(jl, "wrap, pushx, alignx 50%");
		jl = new JLabel("Welcome to " + getAppName());
		jl.setFont(new Font("Sans Serif", Font.PLAIN, 40));
		jl.setForeground(new Color(60, 180, 220, 255));
		jl.setAlignmentX((float) 0.5);
		jl.setAlignmentY((float) 0.5);
		waiting.add(jl, "span, pushx, alignx 50%");

		waiting.setBackground(Color.WHITE);

		setupPresets();
		setupMenuBar();
		setupFileMru(treeView.getGlobalConfig().getNode("FileMru"));

		centerOnscreen();
		setLoaded(false);
	}

	protected void setupMenuBar() {

		if (true) {
		menubar = new TreeViewJMenuBar();
		setJMenuBar(new JMenuBar());
		
		((TreeViewJMenuBar) menubar).setUnderlyingMenuBar(getJMenuBar());
		} else {
			menubar = new TreeViewMenuBar();
			setMenuBar(new MenuBar());
			
			((TreeViewMenuBar) menubar).setUnderlyingMenuBar(getMenuBar());
		}
		synchronized(menubar) {
			menubar.addMenu(TreeviewMenuBarI.programMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_F);
			menubar.addMenu(TreeviewMenuBarI.clusterMenu);
			populateClusterMenu(menubar);
			menubar.setMenuMnemonic(KeyEvent.VK_C);
			programMenu = new ProgramMenu(); // rebuilt when fileMru notifies
			menubar.addMenu(TreeviewMenuBarI.documentMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_S);
			menubar.addSubMenu(TreeviewMenuBarI.presetsSubMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_P);
			populateSettingsMenu(menubar);
			menubar.addMenu(TreeviewMenuBarI.analysisMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_A);
			menubar.addMenu(TreeviewMenuBarI.exportMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_E);
			menubar.addMenu(TreeviewMenuBarI.windowMenu);			
			menubar.setMenuMnemonic(KeyEvent.VK_W);
			menubar.addMenu(TreeviewMenuBarI.helpMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_H);
			populateHelpMenu(menubar);
		}		
	}

	protected void setupPresets() {
	}

	public UrlPresets getGeneUrlPresets() {
		return treeView.getGeneUrlPresets();
	}

	public UrlPresets getArrayUrlPresets() {
		return treeView.getArrayUrlPresets();
	}

	public void closeWindow() {
		if (running != null) {
			running.syncConfig();
		}
		super.closeWindow();
	}

	protected FileSet offerUrlSelection() throws LoadException {
		FileSet fileSet1;
		/*
		 * JTextField textField = new JTextField(); JPanel prompt = new
		 * JPanel(); prompt.setLayout(new BorderLayout()); prompt.add(new
		 * JLabel("Enter a Url"), BorderLayout.NORTH); prompt.add(textField,
		 * BorderLayout.CENTER);
		 */
		// get string from user...
		String urlString = JOptionPane.showInputDialog(this, "Enter a Url");
		if (urlString != null) {
			// must parse out name, parent + sep...
			int postfix = urlString.lastIndexOf("/") + 1;
			String name = urlString.substring(postfix);
			String parent = urlString.substring(0, postfix);
			fileSet1 = new FileSet(name, parent);
		} else {
			throw new LoadException("Input Dialog closed without selection...",
					LoadException.NOFILE);
		}
		return fileSet1;
	}
	

	public void scrollToGene(int i) {
		running.scrollToGene(i);
	}

	public void scrollToArray(int i) {
		running.scrollToArray(i);
	}

	public void load(FileSet fileSet) throws LoadException {
		loadFileSet(fileSet);
		fileSet = fileMru.addUnique(fileSet);
		fileMru.setLast(fileSet);
		fileMru.notifyObservers();
		setLoaded(true);
	}

	/**
	 * To load any fileset without using the event queue thread
	 */
	public void loadNW(FileSet fileSet) throws LoadException {
		loadFileSetNW(fileSet);
		fileSet = fileMru.addUnique(fileSet);
		fileMru.setLast(fileSet);
		fileMru.notifyObservers();
		setLoaded(true);
	}
	

	/**
	 * r * This is the workhorse. It creates a new DataModel of the file, and
	 * then sets the Datamodel. A side effect of setting the datamodel is to
	 * update the running window.
	 */
	public void loadFileSet(FileSet fileSet) throws LoadException {
		TVModel tvModel = new TVModel();
		tvModel.setFrame(this);
		try {
			tvModel.loadNew(fileSet);
			setDataModel(tvModel);
		} catch (LoadException e) {
			if (e.getType() != LoadException.INTPARSE)
				JOptionPane.showMessageDialog(this, e);
			throw e;
		}
	}
	
	/**
	 * r * This is the workhorse. It creates a new DataModel of the file, and
	 * then sets the Datamodel. A side effect of setting the datamodel is to
	 * update the running window.
	 */
	public void loadClusterFileSet(ClusterFileSet fileSet) throws LoadException {
		ClusterModel clusterModel = new ClusterModel();
		clusterModel.setFrame(this);
		try {
			clusterModel.loadNew(fileSet);     //gives the clusterModel the info from the fileSet
			setClusterDataModel(clusterModel);	//makes datamodel the clusterModel for the getDataModel function											//setdataModel is used to eventually create the DendroView
		} catch (LoadException e) {
			if (e.getType() != LoadException.INTPARSE)
				JOptionPane.showMessageDialog(this, e);
			throw e;
		}
	}
	

	/**
	 * To load any FileSet without using the event queue thread
	 */
	public void loadFileSetNW(FileSet fileSet) throws LoadException {
		TVModel tvModel = new TVModel();
		tvModel.setFrame(this);
		try {
			tvModel.loadNewNW(fileSet);
			setDataModel(tvModel);
		} catch (LoadException e) {
			if (e.getType() != LoadException.INTPARSE)
				JOptionPane.showMessageDialog(this, e);
			throw e;
		}
	}
	

	/**
	 * Sets up the following: 1) urlExtractor, an object that generates urls
	 * from gene indexes 2) arrayUrlExtractor, similarly 3) geneSelection and 4)
	 * arraySelection, the two selection objects. It is important that these are
	 * set up before any plugins are instantiated. This is called before
	 * setupRunning by setDataModel.
	 */
	protected void setupExtractors() {
		ConfigNode documentConfig = getDataModel().getDocumentConfigRoot();
		// extractors...
		UrlPresets genePresets = getGeneUrlPresets();
		UrlExtractor urlExtractor = new UrlExtractor(getDataModel() //is the clusterModel!
				.getGeneHeaderInfo(), genePresets);
		urlExtractor.bindConfig(documentConfig.fetchOrCreate("UrlExtractor"));
		setUrlExtractor(urlExtractor);
		UrlPresets arrayPresets = getArrayUrlPresets();
		UrlExtractor arrayUrlExtractor = new UrlExtractor(getDataModel()
				.getArrayHeaderInfo(), arrayPresets);
		arrayUrlExtractor.bindConfig(documentConfig
				.fetchOrCreate("ArrayUrlExtractor"));
		setArrayUrlExtractor(arrayUrlExtractor);
		DataMatrix matrix = getDataModel().getDataMatrix();
		int ngene = matrix.getNumRow(); //the number of genes etc. stored here??
		int nexpr = matrix.getNumCol();
		geneSelection = new TreeSelection(ngene);
		arraySelection = new TreeSelection(nexpr);
	}
	
	//starting dendrogram interface
	protected void setupRunning() {
		DendroView dv = new DendroView(getDataModel(), this);						//where used? Create a similar function with ClusterView! used in setDataModel
		running = dv;
	}
	
	//starting the cluster interface
	protected void setupClusterRunning() {
		ClusterView cv = new ClusterView(getDataModel(), this);						//where used? Create a similar function with ClusterView! used in setDataModel
		running = cv;
	}

	// Observer
	public void update(Observable observable, Object object) {
		if (observable == fileMru) {
			// System.out.println("Rebuilding file menu");
			programMenu.rebuild();
		} else {
			System.out.println("Got weird update");
		}
	}

	/**
	 * This should be called whenever the loaded status changes It's
	 * responsibility is to change the look of the main view only
	 */

	public void setLoaded(boolean flag) {
		// reset persistent popups
		setGeneFinder(null);
		loaded = flag;
		getContentPane().removeAll();
		if (loaded) {
			if (running == null) {													//currently running = null...why? coming from setupRunning() which creates a DendroView
				JOptionPane.showMessageDialog(this, "TreeViewFrame 253: " +
						"No plugins to display");
			} else {
				getContentPane().add((JComponent) running);
				setLoadedTitle();
				treeView.getGlobalConfig().store();
			}
		} else {
			getContentPane().add(waiting);
			setTitle(getAppName());
		}

		// menubar.rebuild...
		rebuildMainPanelMenu();
		treeView.rebuildWindowMenus();
		
		validate();
		repaint();
	}
	

	private void setLoadedTitle() {
		setTitle(getAppName() + " : " + dataModel.getSource());
	}

	public void rebuildMainPanelMenu() {
		synchronized(menubar) {
		menubar.setMenu(TreeviewMenuBarI.documentMenu);
		menubar.removeMenuItems();
		menubar.setMenu(TreeviewMenuBarI.analysisMenu);
		menubar.removeAll();
		menubar.setEnabled(true);
		menubar.setMenu(TreeviewMenuBarI.exportMenu);
		menubar.removeAll();
		
		}
		
		if (getLoaded()) {
			menubar.setMenu(TreeviewMenuBarI.analysisMenu);
			populateAnalysisMenu(menubar);
			menubar.setMenu(TreeviewMenuBarI.exportMenu);
			populateExportMenu(menubar);
			if (running != null) {
				menubar.setMenu(TreeviewMenuBarI.documentMenu);
				running.populateSettingsMenu(menubar);
				menubar.setMenu(TreeviewMenuBarI.analysisMenu);
				running.populateAnalysisMenu(menubar);
				menubar.setMenu(TreeviewMenuBarI.exportMenu);
				if (menubar.getItemCount() > 0)
					menubar.addSeparator();
				running.populateExportMenu(menubar);
			}
			menubar.setMenu(TreeviewMenuBarI.analysisMenu);
			if (menubar.getItemCount() > 0)
				menubar.addSeparator();
		}

		menubar.setMenu(TreeviewMenuBarI.documentMenu);
		menubar.setEnabled(true);
	 }

	public boolean getLoaded() {
		return loaded;
	}

	// Menus

	protected JDialog presetsFrame = null; // persistent popup

	protected TabbedSettingsPanel presetsPanel = null;

	protected void setupPresetsPanel() {
		presetsFrame = new JDialog(TreeViewFrame.this, "Presets", true);
		presetsPanel = new TabbedSettingsPanel();

		UrlPresetsEditor presetEditor = new UrlPresetsEditor(
				getGeneUrlPresets());
		presetEditor.setTitle("Gene Url Presets");
		presetsPanel.addSettingsPanel("Gene", presetEditor);

		presetEditor = new UrlPresetsEditor(getArrayUrlPresets());
		presetEditor.setTitle("Array Url Presets");
		presetsPanel.addSettingsPanel("Array", presetEditor);

		SettingsPanelHolder innerPanel = new SettingsPanelHolder(presetsFrame,
				getApp().getGlobalConfig().getRoot());
		innerPanel.addSettingsPanel(presetsPanel);
		presetsFrame.getContentPane().add(innerPanel);
		presetsFrame.pack();
	}

	protected void populateSettingsMenu(TreeviewMenuBarI menubar2) {
		menubar2.addMenuItem("Gene Url Presets...",new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (presetsPanel == null)
					setupPresetsPanel();
				presetsPanel.synchronizeFrom();
				presetsPanel.setSelectedIndex(0);
				presetsFrame.setVisible(true);
			}
		});
		menubar2.setAccelerator(KeyEvent.VK_P);
		menubar2.setMnemonic(KeyEvent.VK_G);
		
		menubar2.addMenuItem("Array Url Presets...", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				if (presetsPanel == null)
					setupPresetsPanel();
				presetsPanel.synchronizeFrom();
				presetsPanel.setSelectedIndex(1);
				presetsFrame.setVisible(true);
			}
		});
		menubar2.setMnemonic(KeyEvent.VK_A);

		PluginFactory[] plugins = PluginManager.getPluginManager().getPluginFactories();
		if (plugins.length == 0) {
			menubar2.addMenuItem("Color Presets...",new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					if (presetsPanel == null)
						setupPresetsPanel();
					presetsPanel.synchronizeFrom();
					presetsPanel.setSelectedIndex(2);
					presetsFrame.setVisible(true);
				}
			});
			menubar2.setMnemonic(KeyEvent.VK_C);
		} else {
			for (int i = 0; i < plugins.length; i++) {
				plugins[i].addPluginConfig(menubar2, this);
			}
		}

	}

	private HeaderFinder geneFinder = null;

	/** Setter for geneFinder */
	public void setGeneFinder(HeaderFinder geneFinder) {
		this.geneFinder = geneFinder;
	}

	/** Getter for geneFinder */
	@Override
	public HeaderFinder getGeneFinder() {
		if (geneFinder == null) {
			geneFinder = new GeneFinder(TreeViewFrame.this, getDataModel()
					.getGeneHeaderInfo(), getGeneSelection());
		}
		return geneFinder;
	}

	private ClusterFrame clusterDialog = null;
	
	/** Getter for ClusterDialog */
	@Override
	public ClusterFrame getClusterDialogWindow(DataModel dataModel) {
		//if (clusterDialog == null) {
			clusterDialog = new ClusterFrameWindow(TreeViewFrame.this, dataModel); //just a ClusterFilter subclass
		//}
		return clusterDialog;
	}	
	
	private HeaderFinder arrayFinder = null;

	/** Setter for geneFinder */
	public void setArrayFinder(HeaderFinder geneFinder) {
		this.geneFinder = geneFinder;
	}

	/** Getter for geneFinder */
	public HeaderFinder getArrayFinder() {
		if (arrayFinder == null) {
			arrayFinder = new ArrayFinder(TreeViewFrame.this, getDataModel()
					.getArrayHeaderInfo(), getArraySelection());
		}
		return arrayFinder;
	}

	protected void populateAnalysisMenu(TreeviewMenuBarI menubar2) {
		menubar.addMenuItem("Find Genes...", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				getGeneFinder().setVisible(true);
			}
		});
		menubar.setAccelerator(KeyEvent.VK_G);
		
		menubar.addMenuItem("Find Arrays...",new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				getArrayFinder().setVisible(true);
			}
		});
		menubar.setAccelerator(KeyEvent.VK_A);
		
		menubar.addMenuItem("Stats...", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(TreeViewFrame.this,
						new JTextArea(getDataModel().toString()));
			}
		});
		menubar.setAccelerator(KeyEvent.VK_S);
	}

	protected void populateExportMenu(TreeviewMenuBarI menubar2) {
		/*
		 * MenuItem menuItem2 = new MenuItem("Export to Text File... ");
		 * menuItem2.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent actiosnEvent) { ViewFrame viewFrame =
		 * TreeViewFrame.this; FileSet source = getDataModel().getFileSet();
		 * GeneListMaker t = new GeneListMaker(viewFrame, getGeneSelection(),
		 * getDataModel().getGeneHeaderInfo(), source.getDir()+source.getRoot() +
		 * ".txt"); t.setDataMatrix(getDataModel().getDataMatrix(),
		 * getDataModel().getArrayHeaderInfo(), DataModel.NODATA);
		 * t.bindConfig(getDataModel().getDocumentConfig().getNode("GeneListMaker"));
		 * t.makeList(); } }); exportMenu.add(menuItem2);
		 */

		menubar.addMenuItem("Save List", new ActionListener() {
			public void actionPerformed(ActionEvent actiosnEvent) {
				if (warnSelectionEmpty()) {
					ViewFrame viewFrame = TreeViewFrame.this;
					FileSet source = getDataModel().getFileSet();
					String def = getDataModel().getName() + "_list.txt";
					if (source != null) {
						def = source.getDir() + source.getRoot() + "_list.txt";
					}
					GeneListMaker t = new GeneListMaker(viewFrame,
							getGeneSelection(), getDataModel()
									.getGeneHeaderInfo(), def);
					t.setDataMatrix(getDataModel().getDataMatrix(),
							getDataModel().getArrayHeaderInfo(),
							DataModel.NODATA);
					t.bindConfig(getDataModel().getDocumentConfigRoot()
							.fetchOrCreate("GeneListMaker"));
					t.pack();
					t.setVisible(true);
				}
			}
		});
		menubar.setMnemonic(KeyEvent.VK_L);

		menubar.addMenuItem("Save Data", new ActionListener() {
			public void actionPerformed(ActionEvent actiosnEvent) {
				if (warnSelectionEmpty()) {
					ViewFrame viewFrame = TreeViewFrame.this;
					FileSet source = getDataModel().getFileSet();
					GeneListMaker t = new GeneListMaker(viewFrame,
							getGeneSelection(), getDataModel()
									.getGeneHeaderInfo(), source.getDir()
									+ source.getRoot() + "_data.cdt");
					t.setDataMatrix(getDataModel().getDataMatrix(),
							getDataModel().getArrayHeaderInfo(),
							DataModel.NODATA);
					t.bindConfig(getDataModel().getDocumentConfigRoot()
							.fetchOrCreate("GeneListMaker"));
					t.includeAll();
					t.pack();
					t.setVisible(true);
				}
			}
		});
		menubar.setMnemonic(KeyEvent.VK_D);

	}

	public boolean warnSelectionEmpty() {
		TreeSelectionI treeSelection = getGeneSelection();
		if ((treeSelection == null)
				|| (treeSelection.getNSelectedIndexes() <= 0)) {

			JOptionPane.showMessageDialog(this,
					"Cannot generate gene list, no gene selected");
			return false;
		}
		return true;
	}

	public TreeViewApp getApp() {
		return treeView;
	}
	private boolean doModelSave(boolean incremental) {
		DataModelWriter writer = new DataModelWriter(getDataModel());
		final Set<DataModelFileType> written;
		if (incremental) {
			written =  writer.writeIncremental(getDataModel().getFileSet());
		} else {
			written =  writer.writeAll(getDataModel().getFileSet());
		}
		if (written.isEmpty()) {
			JOptionPane.showMessageDialog(TreeViewFrame.this, "No Model changes were written\nOnly the following changes require explicit saving:\n\n"+
					" - Tree Node flips (Analysis->Flip Array/Gene Tree Node)\n" +
					" - Tree Node Annotations (Analysis->Array/Gene TreeAnno)\n");
			return false;
		} else {
			String msg = "Model changes were written to ";
			int i = 0;
			for (DataModelFileType type : written) {
				msg += type.name();
				i++;
				if (i == written.size()) {
					// nothing after last one.
				} else if (i+1 == written.size()) {
					msg += " and ";
				} else {
					msg += ",";
				}
			}
			JOptionPane.showMessageDialog(TreeViewFrame.this, msg);
			return true;
		}
	}

	private class ProgramMenu {
		ProgramMenu() {
			synchronized(menubar) {
				menubar.setMenu(TreeviewMenuBarI.programMenu);
			menubar.addMenuItem("Open...", new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					try {
						FileSet fileSet = offerSelection();
						loadFileSet(fileSet);
						fileSet = fileMru.addUnique(fileSet);
						fileMru.setLast(fileSet);
						fileMru.notifyObservers();
						setLoaded(true);
					} catch (LoadException e) {
						if ((e.getType() != LoadException.INTPARSE)
								&& (e.getType() != LoadException.NOFILE)) {
							LogBuffer.println("Could not open file: "
									+ e.getMessage());
							e.printStackTrace();
						}
						// setLoaded(false);
					}
				}
			});
			menubar.setAccelerator(KeyEvent.VK_O);
			menubar.setMnemonic(KeyEvent.VK_O);

			menubar.addMenuItem("Open Url...", new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					try {
						FileSet fileSet = offerUrlSelection();
						loadFileSet(fileSet);
						fileSet = fileMru.addUnique(fileSet);
						fileMru.setLast(fileSet);
						fileMru.notifyObservers();
						setLoaded(true);
					} catch (LoadException e) {
						LogBuffer.println("could not load url: "
								+ e.getMessage());
						// setLoaded(false);
					}
				}
			});
			menubar.setMnemonic(KeyEvent.VK_U);
			
			menubar.addMenuItem("Save",new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					doModelSave(true);
				}
			});
			menubar.setMnemonic(KeyEvent.VK_S);
			menubar.setAccelerator(KeyEvent.VK_S);

			menubar.addMenuItem("Save as..",new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					if (getDataModel().getFileSet() == null) {
						JOptionPane.showMessageDialog(TreeViewFrame.this, "Saving of datamodels not backed by files is not yet supported.");
					} else {
						JFileChooser fileDialog = new JFileChooser();
						CdtFilter ff = new CdtFilter();
						fileDialog.setFileFilter(ff);
	
						String string = getDataModel().getFileSet().getDir();
						if (string != null) {
							fileDialog.setCurrentDirectory(new File(string));
						}
						int retVal = fileDialog.showSaveDialog(TreeViewFrame.this);
						if (retVal == JFileChooser.APPROVE_OPTION) {
							File chosen = fileDialog.getSelectedFile();
							String name = chosen.getName();
							if (!name.toLowerCase().endsWith(".cdt") && !name.toLowerCase().endsWith(".pcl"))
								name += ".cdt";
							FileSet fileSet2 = new FileSet(name, chosen.getParent()+File.separator);
							fileSet2.copyState(getDataModel().getFileSet());
							FileSet fileSet1 = new FileSet(name, chosen.getParent()+File.separator);
							fileSet1.setName(getDataModel().getFileSet().getName());
							getDataModel().getFileSet().copyState(fileSet1);
							doModelSave(false);
							getDataModel().getFileSet().notifyMoved();
							fileMru.removeDuplicates(getDataModel().getFileSet());
							fileSet2 = fileMru.addUnique(fileSet2);
							fileMru.setLast(getDataModel().getFileSet());
							rebuild();
							if (getDataModel() instanceof TVModel) {
								((TVModel) getDataModel()).getDocumentConfig().setFile(getDataModel().getFileSet().getJtv());
							}
						}
					}
				}
			});
				

			menubar.addSubMenu(TreeviewMenuBarI.mruSubMenu);
			menubar.setMenuMnemonic(KeyEvent.VK_R);
			menubar.setMenu(TreeviewMenuBarI.programMenu);
			menubar.addMenuItem("Edit Recent Files...", new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					FileMruEditor fme = new FileMruEditor(fileMru);
					fme.showDialog(TreeViewFrame.this);
				}
			});
			menubar.setMnemonic(KeyEvent.VK_E);
			
			menubar.addMenuItem("Edit Preferences...", new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					getApp().getPrefs().showEditor();
					getApp().getGlobalConfig().store();
				}
			});
			menubar.setMnemonic(KeyEvent.VK_P);

			menubar.addSeparator();
			menubar.addMenuItem("Quit Program", new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					try {
						treeView.closeAllWindows();
					} catch (Exception e) {
						System.out.println("While trying to exit, got error "
								+ e);
						System.exit(1);
					}
				}
			});
			menubar.setMnemonic(KeyEvent.VK_Q);
			menubar.setAccelerator(KeyEvent.VK_Q);
			}
		}

		public void rebuild() {
			/*
			removeAll();
			add(openItem);
			add(urlItem);
			add(saveItem);
*/
			synchronized(menubar) {
			menubar.setMenu(TreeviewMenuBarI.programMenu);
			menubar.setSubMenu(TreeviewMenuBarI.mruSubMenu);
			menubar.removeAll();
			
			ConfigNode aconfigNode[] = fileMru.getConfigs();
			String astring[] = fileMru.getFileNames();
			for (int j = aconfigNode.length; j > 0; j--) {
				FileMenuListener fileMenuListener = new FileMenuListener(
						new FileSet(aconfigNode[j - 1]));
				menubar.addMenuItem(astring[j - 1], fileMenuListener);
			}
			}
			/*
			add(mruItem);
			
			add(fmeItem);
			addSeparator();

			add(exitItem);
*/
		}
	}

	class FileMenuListener implements ActionListener {
		private FileSet fileSet;

		FileMenuListener(FileSet set) {
			fileSet = set;
		}

		public void actionPerformed(ActionEvent argActionEvent) {
			final ActionEvent actionEvent = argActionEvent;
			Runnable update = new Runnable() {
				public void run() {
					try {
						fileMru.setLast(fileSet);
						fileMru.notifyObservers();
						if (running != null)
							running.syncConfig();
						loadFileSet(fileSet);
						setLoaded(getDataModel().isLoaded());
					} catch (LoadException e) {
						if (e.getType() == LoadException.INTPARSE) {
							// System.out.println("Parsing cancelled...");
						} else {
							// System.out.println("Could not load: " + e);
							int result = FileMruEditor.offerSearch(fileSet,
									TreeViewFrame.this, "Could not Load "
											+ fileSet.getCdt());
							if (result == FileMruEditor.FIND) {
								fileMru.notifyFileSetModified();
								fileMru.notifyObservers();
								actionPerformed(actionEvent); // REPROCESS...
								return; // EARLY RETURN
							} else if (result == FileMruEditor.REMOVE) {
								fileMru.removeFileSet(fileSet);
								fileMru.notifyObservers();
							}
						}
						setLoaded(false);
					}
					// dataModel.notifyObservers();
				}
			};
			SwingUtilities.invokeLater(update);
		}
	}

	private void populateHelpMenu(TreeviewMenuBarI menubar) {
		menubar.addMenuItem("About...",  new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				/*
				 * Popup popup = new Popup(TreeViewFrame.this,getAppName(), new
				 * String [] { "Java TreeView was created by Alok
				 * (alok@genome).", "It is an extensible, crossplatform port of
				 * Eisen's TreeView.", "Version: " +
				 * TreeViewApp.getVersionTag(), "Homepage:
				 * http://genetics.stanford.edu/~alok/TreeView/" });
				 */
				JPanel message = new JPanel();
				// message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
				message.setLayout(new GridLayout(0, 1));
				message
						.add(new JLabel(
								getAppName()
										+ " was created by Alok (alokito@users.sourceforge.net)."));
				message.add(new JLabel("Version: "
						+ TreeViewApp.getVersionTag()));

				JPanel home = new JPanel();
				home.add(new JLabel("Homepage"));
				home.add(new JTextField(TreeViewApp.getUpdateUrl()));
				JButton yesB = new JButton("Open");
				yesB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						displayURL(TreeViewApp.getUpdateUrl());
					}

				});
				home.add(yesB);
				message.add(home);

				home = new JPanel();
				home.add(new JLabel("Announcements"));
				home.add(new JTextField(TreeViewApp.getAnnouncementUrl()));
				yesB = new JButton("Sign Up");
				yesB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						displayURL(TreeViewApp.getAnnouncementUrl());
					}

				});
				home.add(yesB);
				message.add(home);

				JOptionPane.showMessageDialog(TreeViewFrame.this, message, 
						"About...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_A);

		
		menubar.addMenuItem("Messages...", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JPanel inner = new JPanel();
				inner.setLayout(new BorderLayout());
				inner.add(new JLabel("JTV Messages"), BorderLayout.NORTH);
				inner.add(new JScrollPane(new LogMessagesPanel(LogBuffer
						.getSingleton())), BorderLayout.CENTER);
				LogBuffer buffer = LogBuffer.getSingleton();
				buffer.setLog(true);
				inner.add(new LogSettingsPanel(buffer),
						BorderLayout.SOUTH);
				final JDialog top = new JDialog(TreeViewFrame.this,
						"JTV Messages", false);
				top.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				top.setContentPane(inner);
				top.pack();
				top.setLocationRelativeTo(TreeViewFrame.this);
				top.setVisible(true);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_M);

		menubar.addMenuItem("Documentation...", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				// Popup popup = new Popup(TreeViewFrame.this, "Java TreeView:
				// Color", new String [] { "I'm going to add better help
				// later.", "For now, point a web browser at index.html in the
				// doc subdirectory of the Java TreeView folder.", "(that is, if
				// it doesn't open automatically...)" });
				// String classPath = System.getProperty("java.class.path");
				JPanel message = new JPanel();
				message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
				message.add(new JLabel(getAppName()
						+ " documentation is available from the website."));
				final String docUrl = TreeViewApp.getUpdateUrl()
						+ "/manual.html";
				message.add(new JTextField(docUrl));
				JButton lButton = new JButton("Launch Browser");
				lButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						displayURL(docUrl);
					}
				});
				message.add(lButton);
				JOptionPane.showMessageDialog(TreeViewFrame.this, message);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_D);

		menubar.addMenuItem("Plugins...",new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				displayPluginInfo();
			}
		});		
		menubar.setMnemonic(KeyEvent.VK_P);

		menubar.addMenuItem("Registration...",new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				ConfigNode node = treeView.getGlobalConfig().getNode(
						"Registration");
				if (node != null) {
					try {
						edu.stanford.genetics.treeview.reg.RegEngine
								.reverify(node);
					} catch (Exception e) {
						LogBuffer.println("registration error " + e);
						e.printStackTrace();
					}
				}
			}
		});
		menubar.setMnemonic(KeyEvent.VK_R);

		menubar.addMenuItem("Feedback...",new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				JPanel feedback = new JPanel();
				feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
				JComponent tmp = new JLabel("Please report bugs at ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				final String bugsURL = "http://sourceforge.net/tracker/?group_id=84593&atid=573298";
				tmp = new JTextField(bugsURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				JButton yesB = new JButton("Report Bug");
				yesB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						displayURL(bugsURL);
					}
				});
				feedback.add(yesB);

				tmp = new JLabel("Please request features at ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				final String featureURL = "https://sourceforge.net/tracker/?group_id=84593&atid=573301";
				tmp = new JTextField(featureURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				yesB = new JButton("Request Feature");
				yesB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						displayURL(featureURL);
					}
				});
				feedback.add(yesB);

				tmp = new JLabel("For support, send email to ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				final String supportURL = "jtreeview-users@lists.sourceforge.net";
				tmp = new JTextField(supportURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				yesB = new JButton("Email Support");
				yesB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						displayURL("mailto:"+supportURL);
					}
				});
				feedback.add(yesB);

				tmp = new JLabel("You may also search the list archives at ");
				tmp.setAlignmentX((float) 0.0);
				feedback.add(tmp);
				final String archiveURL = "http://sourceforge.net/mailarchive/forum.php?forum_id=36027";
				tmp = new JTextField(archiveURL);
				// tmp.setAlignmentX((float) 1.0);
				feedback.add(tmp);
				yesB = new JButton("Browse Archive");
				yesB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						displayURL(archiveURL);
					}
				});
				feedback.add(yesB);
				JOptionPane.showMessageDialog(TreeViewFrame.this, feedback, 
						"Feedback...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menubar.setMnemonic(KeyEvent.VK_F);
		menubar.addSeparator();

		
		menubar.addMenuItem("Memory...", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MemMonitor m = new MemMonitor();
				m.start();
			}
		});
		menubar.setMnemonic(KeyEvent.VK_M);

		menubar.addMenuItem("Threads...",new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ThreadListener t = new ThreadListener();
				t.start();
			}
		});
		menubar.setMnemonic(KeyEvent.VK_T);
		
		menubar.addMenuItem("Global Pref Info...", new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				GlobalPrefInfo gpi = new GlobalPrefInfo(getApp());
				JOptionPane.showMessageDialog(null, 
						gpi, 
						"Global Pref Info...", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		/* This is to help debug plugin instance naming.
		MenuItem pluginSearch = new MenuItem("Search for instances");
		pluginSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextField name = new JTextField(20);
				JOptionPane.showMessageDialog(TreeViewFrame.this, name);
				MainPanel [] list = getPluginsByName(name.getText());
				JPanel res = new JPanel();
				for (int i = 0; i < list.length; i++)
					res.add(new JLabel(list[i].getName()));
				JOptionPane.showMessageDialog(TreeViewFrame.this,res);
			}
		});
		menu.add(pluginSearch);
		*/
	}
	
	/**EDIT 
	 * @author: Chris Keil
	 * 
	 * Adding Menu for Clustering Option
	 */
	protected void populateClusterMenu(TreeviewMenuBarI menubar2) {
		menubar.addMenuItem("Load File for Clustering", new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					ClusterFileSet fileSet = clusterSelection();
					loadClusterFileSet(fileSet); 
					setLoaded(true);
				} catch (LoadException e) {
					if ((e.getType() != LoadException.INTPARSE)
							&& (e.getType() != LoadException.NOFILE)) {
						LogBuffer.println("Could not open file: "
								+ e.getMessage());
						e.printStackTrace();
						}
				}
				//-------------------------------------

				//-------------------------------------
			}
		});
		menubar.setAccelerator(KeyEvent.VK_L);
	}
	
	/**
	 * EDIT END
	 */

	public double noData() {
		return DataModel.NODATA;
	}

	TreeViewApp treeView;



	private boolean loaded;

	protected JPanel waiting;

	protected MainPanel running;

	protected DataModel dataModel;
	


	/**
	 * Setter for dataModel, also sets extractors, running.
	 * 
	 * @throws LoadException
	 */
	public void setClusterDataModel(DataModel newModel) {									//used to create the dendroview with setupRunning
		if (dataModel != null)
			dataModel.clearFileSetListeners();
		dataModel = newModel;
		if (dataModel != null)
			dataModel.addFileSetListener(this);
		setupExtractors();
		setupClusterRunning();
	}
	
	/**
	 * Setter for dataModel, also sets extractors, running.
	 * 
	 * @throws LoadException
	 */
	public void setDataModel(DataModel newModel) {									//used to create the dendroview with setupRunning
		if (dataModel != null)
			dataModel.clearFileSetListeners();
		dataModel = newModel;
		if (dataModel != null)
			dataModel.addFileSetListener(this);
		setupExtractors();
		setupRunning();
	}
	
	
	/** Getter for dataModel */
	public DataModel getDataModel() {
		return dataModel;
	}
	

	/**
	 * This method displays the current plugin info. 
	 * I set it up as a method so that it can be overridden
	 * by AppletViewFrame
	 */
	protected void displayPluginInfo() {
		MenuHelpPluginsFrame frame = new MenuHelpPluginsFrame(
				"Current Plugins", this);
		File f_currdir = new File(".");
		try {
			frame.setSourceText(f_currdir.getCanonicalPath() + 
					File.separator +"plugins" + File.separator);
		} catch (IOException e) {
			frame.setSourceText("Unable to read default plugins directory.");
		}
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	public MainPanel[] getMainPanelsByName(String name) {
		if (running != null) {
			// is the current running a match?
			if (name.equals(running.getName())) {
				MainPanel [] list = new MainPanel[1];
				list[0] = running;
				return list;
			}
			// okay, is the current running a linkedPanel?
			try {
				LinkedPanel linked = (LinkedPanel) running;
				return linked.getMainPanelsByName(name);
			} catch (ClassCastException e) {
				// fall through to end
			}
		} else {
			// fall through to end
		}
		MainPanel [] list = new MainPanel[0];
		return list;
	}

	public MainPanel[] getMainPanels() {
		if (running == null) {
			MainPanel [] list = new MainPanel[0];
			return list;			
		}
		// okay, is the current running a linkedPanel?
		try {
			LinkedPanel linked = (LinkedPanel) running;
			return linked.getMainPanels();
		} catch (ClassCastException e) {
			// fall through to end
		}

		MainPanel [] list = new MainPanel[1];
		list[0] = running;
		return list;
	}

	@Override
	public void onFileSetMoved(FileSet fileset) {
		setLoadedTitle();
	}

	@Override
	public void onFileSetMoved(ClusterFileSet fileset) {
		setLoadedTitle();
		
	}

}
