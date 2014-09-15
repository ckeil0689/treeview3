/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ViewFrame.java,v $
 * $Revision: 1.37 $
 * $Date: 2009-08-26 11:48:27 $
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
 * 
 * http://www.gnu.org/licenses/gpl.txt *
 * END_HEADER
 */

package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import Cluster.ClusterFileFilter;
import edu.stanford.genetics.treeview.core.FileMru;

/**
 * Any frame that wants to contain MainPanels must extend this.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.37 $ $Date: 2009-08-26 11:48:27 $
 */
public abstract class ViewFrame implements Observer {
	// extends JFrame implements Observer {

	// Main application frame
	protected JFrame applicationFrame;

	protected Preferences configNode;

	// The global most recently used object.
	protected FileMru fileMru;

	// allows opening of urls in external browser
	protected BrowserControl browserControl = null;

	// url extractor for genes
	private UrlExtractor urlExtractor;

	// url extractor for arrays
	private UrlExtractor arrayUrlExtractor;

	// menubar for the application
	protected TreeviewMenuBarI menubar;

	/**
	 * Constructor for the ViewFrame object Sets title and window listeners
	 * 
	 * @param title
	 *            Title for the ViewFrame.
	 */
	public ViewFrame(final String title) {

		this.applicationFrame = new JFrame(title);

		setupWindowListener();
	}

	/**
	 * constructs an untitled <code>ViewFrame</code>
	 */
	public ViewFrame() {

		this.applicationFrame = new JFrame();

		setupWindowListener();
	}

	/**
	 * Returns the applications main JFrame.
	 * 
	 * @return
	 */
	public JFrame getAppFrame() {

		return applicationFrame;
	}

	// must override in subclass...
	/**
	 * This is to ensure that we can observe the MainPanels when they change.
	 * 
	 * @param observable
	 *            The MainPanel or other thing which changed.
	 * @param object
	 *            Generally null.
	 */
	@Override
	public abstract void update(Observable observable, Object object);

	// /**
	// * This routine should return any instances of the plugin of the indicated
	// * name (i.e. it will loop over all instantiated MainPanel calling their
	// * getName() properties, find all that are equal to the indicated string,
	// * and return all matching ones
	// */
	// public abstract MainPanel[] getMainPanelsByName(String name);

	// /**
	// *
	// * @return all mainPanels managed by this viewFrame
	// */
	// public abstract MainPanel[] getMainPanels();

	/**
	 * This method sets up an object of the FileMRU class, which is used to
	 * build a node ('FileMRU') with Java's Preferences API. This node contains
	 * information about the Most Recently Used files and can be used to
	 * conveniently load or edit them.
	 * 
	 * @param fileMruNode
	 *            Node which will be bound to the FileMru
	 */
	protected void setupFileMru() {

		fileMru = new FileMru();
		fileMru.setConfigNode(configNode);

		try {
			fileMru.removeMoved();

		} catch (final Exception e) {
			LogBuffer.println("problem checking MRU in ViewFrame constructor: "
					+ e.toString());
			e.printStackTrace();
		}

		fileMru.addObserver(this);
		fileMru.notifyObservers();// sends us message
	}

	/**
	 * Centers the frame on screen.
	 * 
	 * @param rectangle
	 *            A rectangle describing the outlines of the screen.
	 */
	private void center(final Rectangle rectangle) {

		final Dimension dimension = applicationFrame.getSize();
		applicationFrame.setLocation((rectangle.width - dimension.width) / 3
				+ rectangle.x, (rectangle.height - dimension.height) / 3
				+ rectangle.y);
	}

	/**
	 * Determines dimension of screen and centers frame on screen.
	 */
	public void centerOnscreen() {

		// trying this for mac...
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension dimension = toolkit.getScreenSize();
		final Rectangle rectangle = new Rectangle(dimension);

		// should drag out of global config
		center(rectangle);
	}

	/**
	 * To maximize the application on startup, considering taskbar.
	 */
	public void setupFrameSize() {

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final Insets screenInsets = toolkit.getScreenInsets(GraphicsEnvironment
				.getLocalGraphicsEnvironment().getScreenDevices()[0]
				.getDefaultConfiguration());
		final int taskbarHeight = screenInsets.bottom;

		applicationFrame.setSize(new Dimension(screenSize.width,
				screenSize.height - taskbarHeight));
		applicationFrame.setMinimumSize(new Dimension(screenSize.width * 2 / 3,
				screenSize.height * 2 / 3));
	}

	// MOVE to TVFrame controller
	/**
	 * Sets a listener on self, so that we can grab focus when activated, and
	 * close ourselves when closed.
	 */
	private void setupWindowListener() {

		applicationFrame
				.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		applicationFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(final WindowEvent windowEvent) {

				setWindowActive(true);
			}

			@Override
			public void windowClosing(final WindowEvent windowEvent) {

				closeWindow();
			}

			@Override
			public void windowDeactivated(final WindowEvent windowEvent) {

				setWindowActive(false);
			}
		});
	}

	/**
	 * Keep track of when active, so that clicks don't get passed through too
	 * much.
	 * 
	 * @param flag
	 *            The new windowActive value
	 */
	protected void setWindowActive(final boolean flag) {

		windowActive = flag;
	}

	/**
	 * Keep track of when active, so that clicks don't get passed through too
	 * much.
	 * 
	 * @return True if window is active.
	 */
	public boolean windowActive() {

		return windowActive;
	}

	/**
	 * Keep track of when active, so that clicks don't get passed through too
	 * much.
	 */
	private boolean windowActive;

	/**
	 * checks if DataModel has been modified. Stores a configFile when closing
	 * the window.
	 */
	public void closeWindow() {

//		try {
////			final DataModel dataModel = getDataModel();
//
//			if (dataModel != null) {
//				if (dataModel.getModified()) {
//					final int option = JOptionPane.showConfirmDialog(
//							applicationFrame,
//							"DataModel is modified. Do you wish to save?");
//
//					switch (option) {
//
//					case JOptionPane.YES_OPTION:
//						final DataModelWriter writer = new DataModelWriter(
//								getDataModel());
//						writer.writeIncremental(getDataModel().getFileSet());
//						break;
//
//					case JOptionPane.CANCEL_OPTION:
//						return;
//
//					case JOptionPane.NO_OPTION:
//						break;
//					}
//				}
//
//				final Preferences documentConfig = dataModel
//						.getDocumentConfigRoot();
//				if (documentConfig != null) {
//					// documentConfig.store();
//				}
//			}
//		} catch (final Exception e) {
//			LogBuffer.println("ViewFrame.closeWindow() Got exception: " + e);
//		}

		final ConfirmDialog confirm = new ConfirmDialog(this, "Exit TreeView");
		confirm.setVisible(true);

		if (confirm.getConfirmed()) {
			saveSettings();
			applicationFrame.dispose();
		}
	}

	public abstract void saveSettings();

	/**
	 * required by all <code>ModelPanel</code>s
	 * 
	 * @return The shared TreeSelection object for genes.
	 */
	public TreeSelectionI getGeneSelection() {

		return geneSelection;
	}

	protected void setGeneSelection(final TreeSelectionI newSelection) {

		geneSelection = newSelection;
	}

	/**
	 * required by all <code>ModelPanel</code>s
	 * 
	 * @return The shared TreeSelection object for arrays.
	 */
	public TreeSelectionI getArraySelection() {

		return arraySelection;
	}

	protected void setArraySelection(final TreeSelectionI newSelection) {

		arraySelection = newSelection;
	}

	/**
	 * used by data model to signal completion of loading. The
	 * <code>ViewFrame</code> will react by reconfiguring it's widgets.
	 * 
	 * @param b
	 *            The new loaded value
	 */
	public abstract void setLoaded(boolean b);

	/**
	 * returns special no data value. generally, just cribs from the
	 * <code>DataModel</code>
	 * 
	 * @return A special double which means no data available.
	 */

	public abstract double noData();

	/**
	 * returns the UrlPresets for the views to make use of when configuring
	 * linking for genes
	 * 
	 * @return The shared <code>UrlPresets</code> object for genes
	 */
	public abstract UrlPresets getGeneUrlPresets();

	/**
	 * returns the UrlPresets for the views to make use of when configuring
	 * linking for arrays
	 * 
	 * @return The shared <code>UrlPresets</code> object for arrays
	 */
	public abstract UrlPresets getArrayUrlPresets();

	/**
	 * Gets the loaded attribute of the ViewFrame object
	 * 
	 * @return True if there is currently a model loaded.
	 */
	public abstract boolean isLoaded();

//	/**
//	 * Gets the shared <code>DataModel</code>
//	 * 
//	 * @return Gets the shared <code>DataModel</code>
//	 */
//	public abstract DataModel getDataModel();

//	/**
//	 * Sets the shared <code>DataModel</code>
//	 * 
//	 * @return Sets the shared <code>DataModel</code>
//	 * @throws LoadException
//	 */
//	public abstract void setDataModel(DataModel model);
//	/**
//	 * Should scroll all MainPanels in this view frame to the specified gene.
//	 * The index provided is respect to the TreeSelection object.
//	 * 
//	 * @param i
//	 *            gene index in model to scroll the MainPanel to.
//	 */
//	public abstract void scrollToGene(int i);
//
//	public abstract void scrollToArray(int i);

	/**
	 * The shared selection objects
	 */
	TreeSelectionI geneSelection = null;
	TreeSelectionI arraySelection = null;

	public void deselectAll() {

		geneSelection.deselectAllIndexes();
		arraySelection.deselectAllIndexes();
	}

	/**
	 * This routine causes all data views to select and scroll to a particular
	 * gene.
	 */
	public void seekGene(final int i) {

		geneSelection.deselectAllIndexes();
		geneSelection.setIndexSelection(i, true);
		geneSelection.notifyObservers();
//		scrollToGene(i);
	}

	/**
	 * This routine causes all data views to select and scroll to a particular
	 * array.
	 */
	public void seekArray(final int i) {

		arraySelection.deselectAllIndexes();
		arraySelection.setIndexSelection(i, true);
		arraySelection.notifyObservers();
//		scrollToGene(i);
	}

	/**
	 * This routine extends the selected range to include the index i.
	 */
	public void extendRange(final int i) {

		if (geneSelection.getMinIndex() == -1) {
			seekGene(i);
		}

		geneSelection.setIndexSelection(i, true);
		geneSelection.notifyObservers();

//		scrollToGene(i);
	}

	public boolean geneIsSelected(final int i) {

		return getGeneSelection().isIndexSelected(i);
	}

	/**
	 * url linking support
	 * 
	 * @param i
	 *            index of gene who's url you would like to display.
	 */
	public void displayURL(final int i) {

		displayURL(getUrl(i));
	}

	/**
	 * Gets the url for a particular gene.
	 * 
	 * @param i
	 *            index of the gene, for the gene's <code>UrlExtractor</code>
	 * @return A string representation of the url
	 */
	public String getUrl(final int i) {

		if (urlExtractor == null) {
			return null;
		}
		return urlExtractor.getUrl(i);
	}

	/**
	 * Gets the url for a particular array.
	 * 
	 * @param i
	 *            index of the array, for the array's <code>UrlExtractor</code>
	 * @return A string representation of the url
	 */
	public String getArrayUrl(final int i) {

		if (arrayUrlExtractor == null) {
			return null;
		}
		return arrayUrlExtractor.getUrl(i);
	}

	/**
	 * Pops up a browser window with the specified url
	 * 
	 * @param string
	 *            String representation of the url.
	 */
	public void displayURL(final String string) {

		if (string == null) {
			return;
		}
		try {
			if (browserControl == null) {
				browserControl = BrowserControl.getBrowserControl();
			}
			browserControl.displayURL(string);

		} catch (final MalformedURLException e) {
			final String message = new StringBuffer("Problem loading url: ")
					.append(e).toString();
			LogBuffer.println(message);
			e.printStackTrace();
			JOptionPane.showMessageDialog(applicationFrame, message);

		} catch (final IOException e) {
			final String message = new StringBuffer("Could not load url: ")
					.append(e).toString();
			LogBuffer.println(message);
			e.printStackTrace();
			JOptionPane.showMessageDialog(applicationFrame, message);
		}
	}

	/**
	 * Gets the UrlExtractor for the arrays.
	 * 
	 * This object is used to convert a given array index into a url string. It
	 * can be configured to do this in multiple ways.
	 * 
	 * @return The UrlExtractor for the arrays
	 */
	public UrlExtractor getArrayUrlExtractor() {

		return arrayUrlExtractor;
	}

	/**
	 * Gets the UrlExtractor for the genes.
	 * 
	 * This object is used to convert a given gene index into a url string. It
	 * can be configured to do this in multiple ways.
	 * 
	 * @return The UrlExtractor for the genes
	 */
	public UrlExtractor getUrlExtractor() {

		return urlExtractor;
	}

	/**
	 * Sets the arrayUrlExtractor attribute of the ViewFrame object
	 * 
	 * @param ue
	 *            The new arrayUrlExtractor value
	 */
	public void setArrayUrlExtractor(final UrlExtractor ue) {

		arrayUrlExtractor = ue;
	}

	/**
	 * Sets the urlExtractor attribute of the ViewFrame object
	 * 
	 * @param ue
	 *            The new urlExtractor value
	 */
	public void setUrlExtractor(final UrlExtractor ue) {

		urlExtractor = ue;
	}

	// abstract public HeaderFinder getGeneFinder();

	abstract public void setView(String name);

	/**
	 * Method opens a file chooser dialog
	 * 
	 * @return File file
	 * @throws LoadException
	 */
	public File selectFile() throws LoadException {

		File chosen = null;

		final JFileChooser fileDialog = new JFileChooser();

		setupFileDialog(fileDialog);

		final int retVal = fileDialog.showOpenDialog(applicationFrame);

		if (retVal == JFileChooser.APPROVE_OPTION) {
			chosen = fileDialog.getSelectedFile();

		} else {
			System.out.println("File Dialog closed without selection...");
		}

		return chosen;
	}

	/**
	 * Open a dialog which allows the user to select a new data file
	 * 
	 * @return The fileset corresponding to the dataset.
	 */
	public FileSet getFileSet(final File file) {

		FileSet fileSet1;

		fileSet1 = new FileSet(file.getName(), file.getParent()
				+ File.separator);
		/*
		 * // check existing file nodes... ConfigNode aconfigNode[] =
		 * fileMru.getConfigs(); for (int i = 0; i < aconfigNode.length; i++) {
		 * FileSet fileSet2 = new FileSet(aconfigNode[i]); if
		 * (fileSet2.equals(fileSet1)) {
		 * LogPanel.println("Found Existing node in MRU list for " + fileSet1);
		 * return fileSet2; } }
		 */

		/*
		 * Don't enforce suffixes... // see if we match at all... try { if
		 * (!ff.accept(null, fileSet1.getCdt())) throw new
		 * LoadException(fileSet1.getCdt() + " did not end in .cdt or .pcl",
		 * LoadException.EXT); } catch (NullPointerException e) { throw new
		 * LoadException(e + ",most likely, no file selected so cdt is null",
		 * LoadException.NOFILE); }
		 */

		/*
		 * ConfigNode configNode = fileMru.createSubNode();
		 * fileMru.setLast(configNode); FileSet fileSet3 = new
		 * FileSet(configNode); fileSet3.copyState(fileSet1);
		 * fileMru.notifyObservers();
		 */
		return fileSet1;
	}

	// /**
	// * Setting up a file dialog without file filters
	// * @param fileDialog
	// */
	// protected void setupFileDialog(JFileChooser fileDialog) {
	//
	// try {
	//
	// // will fail on pre-1.3 swings
	// fileDialog.setAcceptAllFileFilterUsed(true);
	// } catch (Exception e) {
	//
	// // hmm... I'll just assume that there's no accept all.
	// fileDialog.addChoosableFileFilter(new javax.swing.filechooser
	// .FileFilter() {
	// public boolean accept (File f) {
	// return true;
	// }
	// public String getDescription () {
	// return "All Files";
	// }
	// });
	// }
	// fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
	// String string = fileMru.getMostRecentDir();
	// if (string != null) {
	// fileDialog.setCurrentDirectory(new File(string));
	// }
	// }

	/**
	 * Overloaded setupFileDialog Setting up a file choosing dialog with
	 * appropriate file filter
	 * 
	 * @param fileDialog
	 * @param viz
	 */
	protected void setupFileDialog(final JFileChooser fileDialog) {

		final ClusterFileFilter ff = new ClusterFileFilter();

		try {

			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);

		} catch (final Exception e) {

			// hmm... I'll just assume that there's no accept all.
			fileDialog
					.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

						@Override
						public boolean accept(final File f) {

							return true;
						}

						@Override
						public String getDescription() {

							return "All Files";
						}
					});
		}

		fileDialog.setFileFilter(ff);
		fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final String string = fileMru.getMostRecentDir();

		if (string != null) {

			fileDialog.setCurrentDirectory(new File(string));
		}
	}

	// /**
	// * Rebuild a particular window menu.
	// *
	// * @param windows
	// * the list of windows to add elements to.
	// *
	// * Add a menu item for each window which grants that window the
	// * foreground when selected.
	// */
	// public void rebuildWindowMenu(final Vector<Window> windows) {
	//
	// synchronized (menubar) {
	//
	// menubar.setMenu(TreeviewMenuBarI.windowMenu);
	// menubar.removeAll();
	//
	// final int max = windows.size();
	// for (int i = 0; i < max; i++) {
	//
	// if (i > 8) {
	//
	// break;
	// }// just want first 9 windows...
	// addFocusItem(windows, i);
	// }
	// menubar.addSeparator();
	//
	// menubar.addMenuItem("New Window");
	// // , new ActionListener() {
	// //
	// // @Override
	// // public void actionPerformed(final ActionEvent actionEvent) {
	// //
	// // createNewFrame().setVisible(true);
	// // }
	// // });
	// menubar.setAccelerator(KeyEvent.VK_N);
	// menubar.setMnemonic(KeyEvent.VK_N);
	//
	// menubar.addMenuItem("Close Window");
	// // , new ActionListener() {
	// //
	// // @Override
	// // public void actionPerformed(final ActionEvent actionEvent) {
	// //
	// // closeWindow();
	// // }
	// // });
	// menubar.setAccelerator(KeyEvent.VK_W);
	// menubar.setMnemonic(KeyEvent.VK_W);
	// }
	// }

	/**
	 * currenlty, only the concrete subclass has a reference to the application,
	 * and hence can create new frames. perhaps this will change if I add an
	 * interface for the App classes.
	 */
	public ViewFrame createNewFrame() {

		return getApp().openNew();
	}

	public abstract TreeViewApp getApp();

	// /**
	// * Constructs a MenuItem which causes the i'th window to be moved to the
	// * front.
	// *
	// * @param windows
	// * a list of windows
	// * @param i
	// * which window to move to the front.
	// * @return a menuItem which focuses the i'th window, or null if more than
	// 9
	// * windows.
	// */
	// private void addFocusItem(final Vector<Window> windows, final int i) {
	//
	// final int p1 = i + 1;
	//
	// if (p1 > 9) {
	//
	// return;
	// }
	//
	// final ViewFrame source = (ViewFrame) windows.elementAt(i);
	// String name;
	//
	// if (source.getLoaded()) {
	//
	// name = source.getDataModel().getName();
	// } else {
	//
	// name = "Not Loaded";
	// }
	// menubar.addMenuItem(name);
	// // , new ActionListener() {
	// //
	// // @Override
	// // public void actionPerformed(final ActionEvent e) {
	// //
	// // source.toFront();
	// // }
	// // });
	// menubar.setAccelerator(getKey(p1));
	// }

	/**
	 * Gets the key corresponding to a particular number.
	 * 
	 * @param i
	 *            The number
	 * @return The VK_blah key value
	 */
	protected int getKey(final int i) {
		switch (i) {
		case 0:
			return KeyEvent.VK_0;
		case 1:
			return KeyEvent.VK_1;
		case 2:
			return KeyEvent.VK_2;
		case 3:
			return KeyEvent.VK_3;
		case 4:
			return KeyEvent.VK_4;
		case 5:
			return KeyEvent.VK_5;
		case 6:
			return KeyEvent.VK_6;
		case 7:
			return KeyEvent.VK_7;
		case 8:
			return KeyEvent.VK_8;
		case 9:
			return KeyEvent.VK_9;
		}
		return 0;
	}

//	public void showSubDataModel(final int[] indexes, final String source,
//			final String name) {
//
//		if (indexes.length == 0) {
//			JOptionPane.showMessageDialog(applicationFrame,
//					"No Genes to show summary of!");
//			return;
//		}
//
//		showSubDataModel(indexes, null, source, name);
//	}
//
//	public void showSubDataModel(final int[] geneIndexes,
//			final int[] arrayIndexes, final String source, final String name) {
//
//		final ReorderedDataModel dataModel = new ReorderedDataModel(
//				getDataModel(), geneIndexes, arrayIndexes);
//		if (source != null) {
//			dataModel.setSource(source);
//		}
//
//		if (name != null) {
//			dataModel.setName(name);
//		}
//
//		final ViewFrame window = getApp().openNew();
//		window.setDataModel(dataModel);
//		window.setLoaded(true);
//		window.getAppFrame().setVisible(true);
//	}
}
