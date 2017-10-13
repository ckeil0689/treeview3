/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import Cluster.ClusterFileFilter;
import edu.stanford.genetics.treeview.core.FileMru;
<<<<<<< HEAD
=======
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.Timer;
>>>>>>> master

/* BEGIN_HEADER                                                   TreeView 3
*
* Please refer to our LICENSE file if you wish to make changes to this software
*
* END_HEADER 
*/
// TODO Lots of methods here and in TreeViewFrame which really belong in the
// controller class
public abstract class ViewFrame extends Observable implements Observer,
		ConfigNodePersistent {

	// Main application frame
	protected JFrame appFrame;

	protected Preferences configNode;

	// The global most recently used object.
	protected FileMru fileMru;

	// allows opening of urls in external browser
	protected BrowserControl browserControl = null;

	// url extractor for genes
	private UrlExtractor urlExtractor;

	// url extractor for arrays
	private UrlExtractor arrayUrlExtractor;
	
	/*
	 * The shared selection objects
	 */
	TreeSelectionI rowSelection = null;
	TreeSelectionI colSelection = null;
	
	/*
	 * Keep track of when active, so that clicks don't get passed through too
	 * much.
	 */
	private boolean windowActive;

	// menubar for the application
	// protected TreeviewMenuBarI menubar;

	/**
	 * Constructor for the ViewFrame object Sets title and window listeners
	 *
	 * @param title
	 *            Title for the ViewFrame.
	 */
	public ViewFrame(String title, final Preferences mainConfigNode) {

		// TODO replace with static method when PR is merged
		final String os = System.getProperty("os.name").toLowerCase();
		final boolean isMac = os.startsWith("mac os x");
		if(isMac) {
			// no app name in frame title
			title = "";
		}

		this.appFrame = new JFrame(title);
		this.configNode = mainConfigNode;
		
		// maximize frame first
		setupFrameSize();

		final int init_width = appFrame.getWidth();
		final int init_height = appFrame.getHeight();

		final int left = mainConfigNode.getInt("frame_left", 0);
		final int top = mainConfigNode.getInt("frame_top", 0);
		final int width = mainConfigNode.getInt("frame_width", init_width);
		final int height = mainConfigNode.getInt("frame_height", init_height);

		appFrame.setBounds(left, top, width, height);
		
		//Handle app quit via a confirmation box, so set the default close
		//operation to do nothing. Closing will be handled by an explicit call
		//to dispose.
		appFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setupWindowPosListener();
	}

	/**
	 * constructs an untitled <code>ViewFrame</code>
	 */
	public ViewFrame() {

		this.appFrame = new JFrame();
	}

	/**
	 * Returns the applications main JFrame.
	 *
	 * @return
	 */
	public JFrame getAppFrame() {

		return appFrame;
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
			LogBuffer.logException(e);
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

		final Dimension dimension = appFrame.getSize();
		appFrame.setLocation((rectangle.width - dimension.width) / 3
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
	 * Maximizes the application on startup, considers taskbar for 
	 * Windows and OSX.
	 */
	public void setupFrameSize() {

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final Insets screenInsets = toolkit.getScreenInsets(GraphicsEnvironment
				.getLocalGraphicsEnvironment().getScreenDevices()[0]
				.getDefaultConfiguration());
		final int taskbarHeight = screenInsets.bottom;

		//Account for the border between the 4 quadrants
		int border_thickness = DendroView.getBORDER_THICKNESS();
		//Make the min size 3 times the default min grid cell size or starting
		//label area static size (whichever is bigger)
		int content_height1 = DendroView.getMIN_GRID_CELL_SIZE() * 3;
		int content_height2 = DendroView.getLABEL_AREA_HEIGHT() * 3;
		int min_size = border_thickness + (content_height1 > content_height2 ?
			content_height1 : content_height2);

		//Don't make the min size larger than the monitor
		int min_width = (screenSize.width / 2 > min_size ?
			min_size : screenSize.width / 2);
		int min_height = (screenSize.height / 2 > min_size ?
			min_size : screenSize.height / 2);

		appFrame.setSize(new Dimension(screenSize.width,
			screenSize.height - taskbarHeight));
		appFrame.setMinimumSize(new Dimension(
			min_width + screenInsets.left + screenInsets.right,
			min_height + screenInsets.bottom + screenInsets.top));
	}
	
	public void addWindowListener(final WindowAdapter wa) {
		
		appFrame.addWindowListener(wa);
	}

	/**
	 * Keep track of when active, so that clicks don't get passed through too
	 * much.
	 *
	 * @param flag
	 *            The new windowActive value
	 */
	public void setWindowActive(final boolean flag) {

		windowActive = flag;
	}

	/**
	 * Keep track of when active, so that clicks don't get passed through too
	 * much.
	 *
	 * @return True if window is active.
	 */
	public boolean isWindowActive() {

		return windowActive;
	}

	/**
	 * Listens to the repositioning of the window and stores the state once
	 * movement has stopped (as determined by a timer)
	 */
	private class AppWindowPosListener extends ComponentAdapter {

		//Timer to prevent repeatedly saving window dimensions upon resize
		private final int saveResizeDelay = 1000;
		private javax.swing.Timer saveResizeTimer;
		ActionListener saveWindowAttrs = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if (evt.getSource() == saveResizeTimer) {
					/* Stop timer */
					saveResizeTimer.stop();
					saveResizeTimer = null;
					LogBuffer.println("Saving window dimensions & position.");

					storeState();
				}
			}
		};

		public void componentMoved(ComponentEvent e) {
			//Save the new dimensions/position if it's done changing
			if (this.saveResizeTimer == null) {
				/* Start waiting for saveResizeDelay millis to elapse and then
				 * call actionPerformed of the ActionListener
				 * "saveWindowAttrs". */
				this.saveResizeTimer = new Timer(this.saveResizeDelay,
					saveWindowAttrs);
				this.saveResizeTimer.start();
			} else {
				/* Event came too soon, swallow it by resetting the timer.. */
				this.saveResizeTimer.restart();
			}
		}
	}

	public void addAppWindowPosListener() {

		appFrame.addComponentListener(new AppWindowPosListener());
	}

	private void setupWindowPosListener() {
		addAppWindowPosListener();
	}

	/**
	 * required by all <code>ModelPanel</code>s
	 *
	 * @return The shared TreeSelection object for genes.
	 */
	public TreeSelectionI getRowSelection() {

		return rowSelection;
	}

	protected void setGeneSelection(final TreeSelectionI newSelection) {

		this.rowSelection = newSelection;
	}

	/**
	 * required by all <code>ModelPanel</code>s
	 *
	 * @return The shared TreeSelection object for arrays.
	 */
	public TreeSelectionI getColSelection() {

		return colSelection;
	}

	protected void setColSelection(final TreeSelectionI newSelection) {

		this.colSelection = newSelection;
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

	// /**
	// * Gets the shared <code>DataModel</code>
	// *
	// * @return Gets the shared <code>DataModel</code>
	// */
	// public abstract DataModel getDataModel();

	// /**
	// * Sets the shared <code>DataModel</code>
	// *
	// * @return Sets the shared <code>DataModel</code>
	// * @throws LoadException
	// */
	// public abstract void setDataModel(DataModel model);
	// /**
	// * Should scroll all MainPanels in this view frame to the specified gene.
	// * The index provided is respect to the TreeSelection object.
	// *
	// * @param i
	// * gene index in model to scroll the MainPanel to.
	// */
	// public abstract void scrollToGene(int i);
	//
	// public abstract void scrollToArray(int i);

	public void deselectAll() {

		rowSelection.deselectAllIndexes();
		colSelection.deselectAllIndexes();
	}

	/**
	 * This routine causes all data views to select and scroll to a particular
	 * gene.
	 */
	public void seekGene(final int i) {

		rowSelection.deselectAllIndexes();
		rowSelection.setIndexSelection(i, true);
		rowSelection.notifyObservers();
		// scrollToGene(i);
	}

	/**
	 * This routine causes all data views to select and scroll to a particular
	 * array.
	 */
	public void seekArray(final int i) {

		colSelection.deselectAllIndexes();
		colSelection.setIndexSelection(i, true);
		colSelection.notifyObservers();
	}

	/**
	 * This routine extends the selected range to include the index i.
	 */
	public void extendRange(final int i) {

		if (rowSelection.getMinIndex() == -1) {
			seekGene(i);
		}

		rowSelection.setIndexSelection(i, true);
		rowSelection.notifyObservers();
	}

	public boolean geneIsSelected(final int i) {

		return getRowSelection().isIndexSelected(i);
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

		} catch (final IOException e) {
			LogBuffer.logException(e);
			final String message = new StringBuffer("Problem loading url: ")
					.append(e).toString();
			JOptionPane.showMessageDialog(appFrame, message);
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

	abstract public void generateView(final ViewType view);

	/**
	 * Decides which dialog option to use for opening files, depending on the
	 * operating system of the user. This is meant to ensure a more native feel
	 * of the application on the user's system although using FileDialog isn't
	 * preferred because it's AWT while the rest of the GUI uses Swing.
	 *
	 * @return File file
	 * @throws LoadException
	 */
	public File selectFile() throws LoadException {

		final boolean isMacOrUnix = System.getProperty("os.name").contains(
				"Mac")
				|| System.getProperty("os.name").contains("nix")
				|| System.getProperty("os.name").contains("nux")
				|| System.getProperty("os.name").contains("aix");

		return isMacOrUnix ? selectFileNix() : selectFileWin();
	}

	/**
	 * Method opens a file chooser dialog for Windows systems
	 *
	 * @return File file
	 * @throws LoadException
	 */
	public File selectFileWin() throws LoadException {

		File chosen = null;

		final JFileChooser fileDialog = new JFileChooser();

		setupFileDialog(fileDialog);

		final int retVal = fileDialog.showOpenDialog(appFrame);

		if (retVal == JFileChooser.APPROVE_OPTION) {
			chosen = fileDialog.getSelectedFile();

		} else {
			LogBuffer.println("File Dialog closed without selection...");
		}

		return chosen;
	}

	/**
	 * Method opens a file chooser dialog for Unix based systems. This uses AWT
	 * FileDialog rather than JFileChooser because it provides the more 'native'
	 * way of selecting files for Linux and OSX systems. It's ugly to deal with
	 * though (centering on screen, steals focus under Ubuntu...)
	 *
	 * @return File file
	 */
	public File selectFileNix() {

		File chosen = null;

		final FileDialog fileDialog = new FileDialog(appFrame, "Choose a file",
				FileDialog.LOAD);

		String string = fileMru.getMostRecentDir();
		if (string == null) {
			string = configNode.get("lastDir", "/");
		}
		fileDialog.setDirectory(string);

		/* Lots of code to be able to center an awt.FileDialog on screen... */
		final Rectangle rect = appFrame.getContentPane().getBounds();

		/*
		 * Making sure FileDialog has a size before setVisible, otherwise center
		 * cannot be found.
		 */
		fileDialog.pack();
		fileDialog.setSize(800, 600);
		fileDialog.validate();

		final double width = fileDialog.getBounds().getWidth();
		final double height = fileDialog.getBounds().getHeight();

		final double x = rect.getCenterX() - (width / 2);
		final double y = rect.getCenterY() - (height / 2);

		final Point newPoint = new Point();
		newPoint.setLocation(x, y);

		fileDialog.setLocation(newPoint);
		fileDialog.setVisible(true);

		final String dir = fileDialog.getDirectory();
		final String filename = fileDialog.getFile();
		if (dir != null && filename != null) {
			chosen = new File(dir + filename);
			configNode.put("lastdir", fileDialog.getDirectory());
		}

		/* AWT FileDialog steals focus... this is necessary */
		appFrame.requestFocus();

		return chosen;
	}

	/**
	 * Open a dialog which allows the user to select a new data file
	 *
	 * @return The fileset corresponding to the dataset.
	 */
	public static FileSet getFileSet(final File file) {

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
		} 
		catch (final Exception e) {
			// hmm... I'll just assume that there's no accept all.
			fileDialog.addChoosableFileFilter(
					new javax.swing.filechooser.FileFilter() {

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
	protected static int getKey(final int i) {
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
		default:
			break;
		}
		return 0;
	}

	// public void showSubDataModel(final int[] indexes, final String source,
	// final String name) {
	//
	// if (indexes.length == 0) {
	// JOptionPane.showMessageDialog(applicationFrame,
	// "No Genes to show summary of!");
	// return;
	// }
	//
	// showSubDataModel(indexes, null, source, name);
	// }
	//
	// public void showSubDataModel(final int[] geneIndexes,
	// final int[] arrayIndexes, final String source, final String name) {
	//
	// final ReorderedDataModel dataModel = new ReorderedDataModel(
	// getDataModel(), geneIndexes, arrayIndexes);
	// if (source != null) {
	// dataModel.setSource(source);
	// }
	//
	// if (name != null) {
	// dataModel.setName(name);
	// }
	//
	// final ViewFrame window = getApp().openNew();
	// window.setDataModel(dataModel);
	// window.setLoaded(true);
	// window.getAppFrame().setVisible(true);
	// }
}
