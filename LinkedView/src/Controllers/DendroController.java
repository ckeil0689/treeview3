package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import Utilities.Helper;
import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ReorderedTreeSelection;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.AtrTVModel;
import edu.stanford.genetics.treeview.model.ReorderedDataModel;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.AtrAligner;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroException;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.GlobalView2;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreeColorer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreePainter;

public class DendroController implements ConfigNodePersistent {

	private DendroView dendroView;
	private final TreeViewFrame tvFrame;
	private DataModel tvModel;

	protected Preferences configNode;

	private int[] arrayIndex = null;
	private int[] geneIndex = null;

	// Drawers
	protected ArrayDrawer arrayDrawer;
	protected TreePainter invertedTreeDrawer;
	protected TreePainter leftTreeDrawer;

	// MapContainers
	protected final MapContainer globalXmap;
	protected final MapContainer globalYmap;

	// Selections
	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;

	// Color Extractor
	private ColorExtractor colorExtractor;

	public DendroController(final TreeViewFrame tvFrame) {

		this.tvFrame = tvFrame;
		
		globalXmap = new MapContainer("Fixed", "GlobalXMap");
		globalYmap = new MapContainer("Fixed", "GlobalYMap");
	}
	
	public void setNew(final DendroView dendroView, final DataModel tvModel) {
		
		this.dendroView = dendroView;
		this.tvModel = tvModel;

		setConfigNode(tvFrame.getConfigNode());
		updateHeaderInfo();
		bindComponentFunctions();
		
		/* Get saved tree visibility status, default to false */
		setTreesVis(configNode.getBoolean("treesVisible", false));

		dendroView.setupLayout();
		setSavedScale();

		// add listeners
		addViewListeners();
		addMenuBtnListeners();
	}

	/**
	 * Recalculates proportions for the MapContainers, when the layout was
	 * changed by removing or adding components, or resizing the TVFrame. Only
	 * works if GlobalView is already resized (has availablePixels set to new
	 * value!).
	 */
	public void resetMapContainers() {

		dendroView.getGlobalView().resetHome(true);
	}
	
	private void addViewListeners() {

		dendroView.addScaleListeners(new ScaleListener());
		dendroView.addZoomListener(new ZoomListener());
		dendroView.addCompListener(new ResizeListener());
	}

	/**
	 * TODO Make sure only one listener is on each button, not multiple 
	 * instances.
	 */
	private void addMenuBtnListeners() {

		dendroView.addSearchBtnListener(new SearchButtonListener());
		dendroView.addTreeBtnListener(new TreeBtnListener());
	}
	
	/* --------------  Listeners --------------------- */
	/**
	 * Listener for the search button. Opens a dialog when the button
	 * is clicked.
	 * @author CKeil
	 *
	 */
	private class SearchButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			//Putting the mapContainer objects in DendroView so that I can control zoom-out of the found genes/arrays are outside the visible area
			dendroView.setGlobalXMap(globalXmap);
			dendroView.setGlobalYMap(globalYmap);
			//Adding the mapContainer objects here so that the search dialog can determine if results are visible in order to be able to determine whether to zoom out
			dendroView.openSearchDialog(tvModel.getGeneHeaderInfo(), 
					tvModel.getArrayHeaderInfo());
		}
	}

	/**
	 * Listener for the button that is responsible to show/ hide Dendrograms. It
	 * switches between JSplitPanes and JPanels in DendroView and causes
	 * DendroView to reset its layout, re-add the listeners, and reset the
	 * MapContainers to adjust to the different size of GlobalView.
	 * 
	 * @author CKeil
	 * 
	 */
	private class TreeBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			setTreesVis(dendroView.getTreeButton().isSelected());
			
			dendroView.setupLayout();
			addViewListeners();
		}
	}

	/**
	 * Listener for the setScale-buttons in DendroView. Changes the scale in
	 * xMap and yMap MapContainers, allowing the user to zoom in or out of each
	 * individual axis in GlobalView.
	 * 
	 * @author CKeil
	 * 
	 */
	class ScaleListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (e.getSource() == dendroView.getXPlusButton()) {
				getGlobalXMap().zoomIn();

			} else if (e.getSource() == dendroView.getXMinusButton()) {
				getGlobalXMap().zoomOut();

			} else if (e.getSource() == dendroView.getYPlusButton()) {
				getGlobalYMap().zoomIn();
				

			} else if (e.getSource() == dendroView.getYMinusButton()) {
				getGlobalYMap().zoomOut();
				

			} else if (e.getSource() == dendroView.getHomeButton()) {
				resetMapContainers();

			} else {
				LogBuffer.println("Got weird source for actionPerformed() "
						+ "in DendroController ScaleListener.");
			}
			
			getGlobalXMap().notifyObservers();
			getGlobalYMap().notifyObservers();
		}
	}
	
	/**
	 * Sets the dimensions of the GlobalView axes. There are three options, 
	 * passed from the MenuBar when the user selects it.
	 * Fill: This fills all of the available space on the screen 
	 * with the matrix.
	 * Equal: Both axes are equally sized, forming a square matrix.
	 * Proportional: Axes are sized in proportion to how many elements they
	 * show. 
	 * @param mode
	 */
	public void setMatrixSize(int mode) {
		
		switch(mode) {
		
		case GlobalView2.FILL:
			setMatrixFill();
			break;
			
		case GlobalView2.EQUAL:
			setMatrixAxesEqual();
			break;
			
		case GlobalView2.PROPORT:
			setMatrixPropotional();
			break;
			
		default:
			setMatrixFill();
			break;
		}
		
		dendroView.setupLayout();
		addViewListeners();
		resetMapContainers();
	}
	
	/** 
	 * Sets axis dimensions of heat map to their maximum values.
	 */
	private void setMatrixFill() {
		
		dendroView.setGVWidth(DendroView.MAX_GV_WIDTH);
		dendroView.setGVHeight(DendroView.MAX_GV_HEIGHT);
	}
	
	/**
	 * Sets the size of each GlobalView axes to the smallest of both to
	 * form a square.
	 */
	private void setMatrixAxesEqual() {
		
		/* Get GlobalView dimensions */
		double absGVWidth = dendroView.getGlobalView().getWidth();
		double absGVHeight = dendroView.getGlobalView().getHeight();
		
		if(!Helper.nearlyEqual(absGVWidth, absGVHeight)) {
			
			/* 
			 * Depends on app frame size (what if app used on a screen 
			 * with larger height than width etc...)
			 */
			int screen_width = tvFrame.getAppFrame().getWidth();
			int screen_height = tvFrame.getAppFrame().getHeight();
			
			/* Make sure the axis with the smallest screen side is maximized */
			if(screen_height < screen_width) {		
				double newWidth = calcAxisDimension(absGVWidth, absGVHeight, 
						DendroView.MAX_GV_WIDTH);
				
				dendroView.setGVWidth(newWidth);
				
			} else {
				double newHeight = calcAxisDimension(absGVHeight, absGVWidth, 
						DendroView.MAX_GV_HEIGHT);
				
				dendroView.setGVHeight(newHeight);
			}
		}
	}
	
	private double calcAxisDimension(double big, double small, double max) {
		
		double percentDiff = small / big;
		
		/* new abs-size for axis */
		double newAbsSize = big * percentDiff;
		
		double newAxis = percentDiff * max;

		LogBuffer.println("Adjusted other axis: " + newAbsSize);
		
		// rounding
		newAxis *= 1000;
		newAxis = (double) Math.round(newAxis);
		newAxis /= 1000;
		
		LogBuffer.println("PercentDiff: " + percentDiff);
		LogBuffer.println("New percent: " + newAxis);
		
		return newAxis;
	}
	
	/**
	 * Resizes the matrix such that it fits all pixels as squares.
	 * If gvWidth or gvHeight would go below a certain size, the matrix is
	 * adjusted such that the content remains viewable in a meaningful manner.
	 */
	private void setMatrixPropotional() {
		
		// Condition: All pixels must be shown, but must have same scale.
				
		double xScale = globalXmap.getScale();
		double yScale = globalYmap.getScale();
		
		if(xScale >= yScale) {
			globalXmap.setScale(yScale);
			
			double used = globalXmap.getUsedPixels();
			double avail = globalXmap.getAvailablePixels();
			
			double percentDiff = used/ avail;
			
			double newWidth = DendroView.MAX_GV_WIDTH * percentDiff;
			// rounding
			newWidth = newWidth * 1000;
			newWidth = (double)Math.round(newWidth);
			newWidth = newWidth/ 1000;
			
			dendroView.setGVWidth(newWidth);
			
		} else {
			globalYmap.setScale(xScale);
			
			double used = globalYmap.getUsedPixels();
			double avail = globalYmap.getAvailablePixels();
			
			double percentDiff = used/ avail;
			
			double newHeight = DendroView.MAX_GV_HEIGHT * percentDiff;
			// rounding
			newHeight = newHeight * 1000;
			newHeight = (double)Math.round(newHeight);
			newHeight = newHeight/ 1000;
			
			dendroView.setGVHeight(newHeight);
		}
	}

	public void saveSettings() {

		try {
			if (configNode.nodeExists("GlobalXMap") && globalXmap != null) {
				configNode.node("GlobalXMap").putDouble("scale",
						globalXmap.getScale());
				configNode.node("GlobalXMap").putInt("XScrollValue",
						dendroView.getXScroll().getValue());
			}

			if (configNode.nodeExists("GlobalYMap") && globalYmap != null) {
				configNode.node("GlobalYMap").putDouble("scale",
						globalYmap.getScale());
				configNode.node("GlobalYMap").putInt("YScrollValue",
						dendroView.getYScroll().getValue());
			}
		} catch (final BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public void setSavedScale() {

		try {
			if (configNode.nodeExists("GlobalXMap") && globalXmap != null) {
				globalXmap.setLastScale();
				dendroView.setXScroll(configNode.node("GlobalXMap").getInt(
						"XScrollValue", 0));
			}

			if (configNode.nodeExists("GlobalYMap") && globalYmap != null) {
				globalYmap.setLastScale();
				dendroView.setYScroll(configNode.node("GlobalYMap").getInt(
						"YScrollValue", 0));
			}

		} catch (final BackingStoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The Zoom listener which allows the user to zoom into a selection.
	 * 
	 * @author CKeil
	 * 
	 */
	class ZoomListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			zoomSelection();
			centerSelection();
		}
	}

	/**
	 * Uses the array- and geneSelection and currently available pixels on
	 * screen retrieved from the MapContainer objects to calculate a new scale
	 * and zoom in on it by working in conjunction with centerSelection().
	 */
	public void zoomSelection() {

		double newScale  = 0.0;
		double newScale2 = 0.0;
		
		//Declare the min number of spots to zoom in on for each dimension.
		//We should set this as a preference in the future. -Rob
		double minZoomIndex      = 20;
		double minArrayZoomIndex = minZoomIndex;
		double minGeneZoomIndex  = minZoomIndex;
		
		//Determine the boundaries of the data (so that we do not exceed them)
		final double maxArrayIndex = globalXmap.getMaxIndex();
		final double maxGeneIndex  = globalYmap.getMaxIndex();
		
		//Make sure our zoom limits have not exceeded the boundaries of the data (if the data matrix is really small)
		if(maxArrayIndex < minArrayZoomIndex) {
			minArrayZoomIndex = maxArrayIndex;
		}
		if(maxGeneIndex < minGeneZoomIndex) {
			minGeneZoomIndex = maxGeneIndex;
		}

		//We'll allow the user to surpass the min zoom index when they are near the edge, so that
		//their selection is centered on the screen, so let's get the edges of the selection
		double minSelectedArrayIndex = arraySelection.getMinIndex();
		double minSelectedGeneIndex  = geneSelection.getMinIndex();
		
		//Obtain the selection size of each dimension
		double arrayIndexes = arraySelection.getMaxIndex() - minSelectedArrayIndex + 1;
		double geneIndexes  = geneSelection.getMaxIndex() - minSelectedGeneIndex + 1;
		
		//If the array selection is smaller than the minimum zoom level
		if(arrayIndexes < minArrayZoomIndex) {
			
			//If the center of the selection is less than half the distance to the near edge
			if((minSelectedArrayIndex + arrayIndexes / 2) < (minArrayZoomIndex / 2)) {
				arrayIndexes = (minSelectedArrayIndex + arrayIndexes / 2) * 2;
			}
			//Else if the center of the selection is less than half the distance to the far edge
			else if((minSelectedArrayIndex + arrayIndexes / 2) >
					(maxArrayIndex - (minArrayZoomIndex / 2))) {
				arrayIndexes = (maxArrayIndex - (minSelectedArrayIndex + arrayIndexes / 2 - 1)) * 2;
			}
			//Otherwise, set the standard minimum zoom
			else {
				arrayIndexes = minArrayZoomIndex;
			}
		}
		
		//If the gene selection is smaller than the minimum zoom level
		if(geneIndexes < minGeneZoomIndex) {
			
			//If the center of the selection is less than half the distance to the near edge
			if((minSelectedGeneIndex + geneIndexes / 2) < (minGeneZoomIndex / 2)) {
				geneIndexes = (minSelectedGeneIndex + geneIndexes / 2) * 2;
			}
			//Else if the center of the selection is less than half the distance to the far edge
			else if((minSelectedGeneIndex + geneIndexes / 2) >
					(maxGeneIndex - (minGeneZoomIndex / 2))) {
				geneIndexes = (maxGeneIndex - (minSelectedGeneIndex + geneIndexes / 2 - 1)) * 2;
			}
			//Otherwise, set the standard minimum zoom
			else {
				geneIndexes = minGeneZoomIndex;
			}
		}

		if (arrayIndexes > 0 && geneIndexes > 0) {
			newScale = (globalXmap.getAvailablePixels()) / arrayIndexes;

			//if (newScale < globalXmap.getMinScale()) {
			//	newScale = globalXmap.getMinScale();
			//}
			globalXmap.setScale(newScale);
			
			//LogBuffer.println("Setting numVisible for arrays to round of double [" + arrayIndexes + "].");

			//Track explicitly manipulated visible area (instead of the visible area) as
			//is manipulated via indirect actions (such as resizing the window)
			int numArrayIndexes = (int) Math.round(arrayIndexes);
			globalXmap.setNumVisible(numArrayIndexes);

			newScale2 = (globalYmap.getAvailablePixels()) / geneIndexes;
			
//			LogBuffer.println("Zooming. MinSelectedArrayIndex: [" + minSelectedArrayIndex + "] " +
//							  "MinSelectedGeneIndex: [" + minSelectedGeneIndex + "] " +
//							  "ArrayIndexesSelected: [" + arrayIndexes + "] " +
//							  "GeneIndexesSelected: [" + geneIndexes + "] " +
//							  "xscale: [" + newScale + "] " +
//							  "yscale: [" + newScale2 + "].");

			//if (newScale2 < globalYmap.getMinScale()) {
			//	newScale2 = globalYmap.getMinScale();
			//}
			globalYmap.setScale(newScale2);

			//LogBuffer.println("Setting numVisible for genes to round of double [" + geneIndexes + "].");

			//Track explicitly manipulated visible area (instead of the visible area) as
			//is manipulated via indirect actions (such as resizing the window)
			int numGeneIndexes = (int) Math.round(geneIndexes);
			globalYmap.setNumVisible(numGeneIndexes);
		}

		saveSettings();
	}

	/**
	 * Scrolls to the center of the selected rectangle
	 */
	public void centerSelection() {

		int scrollX;
		int scrollY;

		final int[] selectedGenes = geneSelection.getSelectedIndexes();
		final int[] selectedArrays = arraySelection.getSelectedIndexes();

		if (selectedGenes.length > 0 && selectedArrays.length > 0) {

			final double endX = selectedArrays[selectedArrays.length - 1];
			final double endY = selectedGenes[selectedGenes.length - 1];

			final double startX = selectedArrays[0];
			final double startY = selectedGenes[0];

			scrollX = (int) Math.round((endX + startX) / 2);
			scrollY = (int) Math.round((endY + startY) / 2);

			globalXmap.scrollToIndex(scrollX);
			globalYmap.scrollToIndex(scrollY);
			
			//Calculate the first visible data index in both dimensions
			int firstX = 0;
			while(firstX < globalXmap.getMaxIndex() && !globalXmap.isVisible(firstX)) {
				firstX++;
			}
				
			int firstY = 0;
			while(firstY < globalYmap.getMaxIndex() && !globalYmap.isVisible(firstY)) {
				firstY++;
			}
			
			//Track explicitly manipulated visible area (instead of the visible area) as
			//is manipulated via indirect actions (such as resizing the window)
			globalXmap.setFirstVisible(firstX);
			globalYmap.setFirstVisible(firstY);
		}
	}

	/**
	 * Uses the currently visible data indexes on the screen to update the scale
	 * and zoom in conjunction with centerSelection() to handle window resize events.
	 * Based on zoomSelection().  Does not change the number of visible data indexes.
	 */
	public void reZoomVisible() {

		double newScale  = 0.0;
		double newScale2 = 0.0;
		
		//Obtain the selection size of each dimension
		//double arrayIndexes = globalXmap.getTileNumVisible();
		//double geneIndexes  = globalYmap.getTileNumVisible();
		double arrayIndexes = globalXmap.getNumVisible();
		double geneIndexes  = globalYmap.getNumVisible();
		
		if (arrayIndexes == 0 || geneIndexes == 0 
				|| (arrayIndexes == globalXmap.getMaxIndex() 
				&& geneIndexes == globalYmap.getMaxIndex())) {
			//LogBuffer.println("No spots are visible. Resetting view.");
			arrayIndexes = globalXmap.getMaxIndex() + 1;
			geneIndexes  = globalYmap.getMaxIndex() + 1;
			resetMapContainers();
		}
		else {
			//LogBuffer.println("pixels / array indexes visible: [" + globalXmap.getAvailablePixels() + "/" + arrayIndexes + "] gene indexes visible: [" + globalYmap.getAvailablePixels() + "/" + geneIndexes + "].");
			newScale = (globalXmap.getAvailablePixels()) / arrayIndexes;
			//LogBuffer.println("reZoomVisible: numVisible: [" + arrayIndexes + "] is being used in calculations for new scale values: [" + newScale + "].  They cannot be less than the minscale: [" + globalXmap.getMinScale() + "]");

			//if (newScale < globalXmap.getMinScale()) {
			//	newScale = globalXmap.getMinScale();
			//}
			globalXmap.setScale(newScale);
			globalXmap.notifyObservers();

			newScale2 = (globalYmap.getAvailablePixels()) / geneIndexes;
			//LogBuffer.println("reZoomVisible: numVisible: [" + geneIndexes + "] is being used in calculations for new scale values: [" + newScale2 + "].  They cannot be less than the minscale: [" + globalYmap.getMinScale() + "]");
			
			//if (newScale2 < globalYmap.getMinScale()) {
			//	newScale2 = globalYmap.getMinScale();
			//}
			globalYmap.setScale(newScale2);
			globalYmap.notifyObservers();
			dendroView.getGlobalView().repaint();
		}

		saveSettings();
	}

	/**
	 * Scrolls to the center of the visible rectangle.  Used when the window
	 * or the image area is resized in order to keep the same data displayed.
	 */
	public void reCenterVisible() {

		//final int visibleGenes  = globalYmap.getTileNumVisible();
		//final int visibleArrays = globalXmap.getTileNumVisible();
		final int visibleGenes  = globalYmap.getNumVisible();
		final int visibleArrays = globalXmap.getNumVisible();

		if (visibleGenes > 0 && visibleArrays > 0) {

			int startX = globalXmap.getFirstVisible();
			int startY = globalYmap.getFirstVisible();
			
			//LogBuffer.println("Firstx visible: [" + startX + "] Firsty visible: [" + startY + "].");

			globalXmap.scrollToFirstIndex(startX);
			globalXmap.notifyObservers();

			globalYmap.scrollToFirstIndex(startY);
			globalYmap.notifyObservers();
		}
		
		saveSettings();
	}

	/**
	 * Listens to the resizing of DendroView2 and makes changes to MapContainers
	 * as a result.
	 * 
	 * @author CKeil
	 * 
	 */
	class ResizeListener implements ComponentListener {

		// Component Listeners
		@Override
		public void componentHidden(final ComponentEvent arg0) {
		}

		@Override
		public void componentMoved(final ComponentEvent arg0) {
		}

		@Override
		public void componentResized(final ComponentEvent arg0) {
			//LogBuffer.println("componentResized: globalYmap.getTileNumVisible: [" + globalYmap.getTileNumVisible() +
			//					"] globalXmap.getTileNumVisible: [" + globalXmap.getTileNumVisible() +
			//					"] dendroView.getXScroll().getValue(): [" + dendroView.getXScroll().getValue() +
			//					"] dendroView.getYScroll().getValue(): [" + dendroView.getYScroll().getValue() + "].");

			//Previously, resetMapContainers was called here, but that caused
			//the zoom level to change when the user resized the window, so I
			//added a way to track the currently visible area in mapContainer
			//and implemented these functions to make the necessary
			//adjustments to the image when that happens
			reZoomVisible();
			reCenterVisible();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {
		}
	}

	public void setTreesVis(final boolean vis) {

		dendroView.setTreesVisible(vis);
		configNode.putBoolean("treesVisible", vis);
	}

	public void saveImage(final JPanel panel) throws IOException {

		File saveFile = new File("savedImage.png");

		final JFileChooser fc = new JFileChooser();

		fc.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
		fc.setSelectedFile(saveFile);
		final int returnVal = fc.showSaveDialog(dendroView.getDendroPane());

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			saveFile = fc.getSelectedFile();

			String fileName = saveFile.toString();

			if (!fileName.endsWith(".png")) {
				fileName += ".png";
				saveFile = new File(fileName);
			}

			final BufferedImage im = new BufferedImage(panel.getWidth(),
					panel.getHeight(), BufferedImage.TYPE_INT_ARGB);

			panel.paint(im.getGraphics());
			ImageIO.write(im, "PNG", saveFile);
		}
	}

	/**
	 * Checks whether there is a configuration node for the current model and
	 * DendroView. If not it creates one.
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			if (tvModel.getDocumentConfigRoot() != null) {
				configNode = ((TVModel)tvModel).getDocumentConfig();

			} else {
				configNode = Preferences.userRoot().node("DendroView");
			}
		}
	}

	/**
	 * Getter for root
	 */
	public Preferences getConfigNode() {

		return configNode;
	}

	/**
	 * Sets arrayIndex and geneIndex if a K-Means clustered file has been
	 * loaded. These indexes are used to set gaps to visualize the distinct
	 * groups in a K-Means clustered file.
	 */
	public void setKMeansIndexes() {

		if (tvModel.getArrayHeaderInfo().getIndex("GROUP") != -1) {
			final HeaderInfo headerInfo = tvModel.getArrayHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");

			arrayIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			arrayIndex = null;
		}

		if (tvModel.getGeneHeaderInfo().getIndex("GROUP") != -1) {
			System.err.println("got gene group header");
			final HeaderInfo headerInfo = tvModel.getGeneHeaderInfo();
			final int groupIndex = headerInfo.getIndex("GROUP");
			geneIndex = getGroupVector(headerInfo, groupIndex);

		} else {
			geneIndex = null;
		}

		// ISSUE: Needs DataModel, not TVModel. Should dataModel be used
		// in this class rather than TVModel?
		if ((arrayIndex != null) || (geneIndex != null)) {
			 tvModel = new ReorderedDataModel(tvModel, geneIndex,
			 arrayIndex);
			LogBuffer.println("DataModel issue in DendroController.");
		}
	}

	/**
	 * Returns an array of indexes of K-Means groups.
	 * 
	 * @param headerInfo
	 * @param groupIndex
	 * @return
	 */
	private int[] getGroupVector(final HeaderInfo headerInfo,
			final int groupIndex) {

		int ngroup = 0;
		String cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
			if (!cur.equals(test)) {
				cur = test;
				ngroup++;
			}
		}

		final int[] groupVector = new int[ngroup + headerInfo.getNumHeaders()];
		ngroup = 0;
		cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
			if (!cur.equals(test)) {
				groupVector[i + ngroup] = -1;
				cur = test;
				ngroup++;
			}
			groupVector[i + ngroup] = i;
		}

		return groupVector;
	}

	/**
	 * Binds functionality to Swing components in DendroView.
	 */
	public void bindComponentFunctions() {

		// Handle selection
		if (geneIndex != null) {
			setGeneSelection(new ReorderedTreeSelection(
					tvFrame.getGeneSelection(), geneIndex));

		} else {
			setGeneSelection(tvFrame.getGeneSelection());
		}

		if (arrayIndex != null) {
			setArraySelection(new ReorderedTreeSelection(
					tvFrame.getArraySelection(), arrayIndex));

		} else {
			setArraySelection(tvFrame.getArraySelection());
		}

		// Give components access to TVModel
		// dendroView.getArraynameview().setDataModel(tvModel);

		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorPresets.setConfigNode(configNode);
		colorExtractor = new ColorExtractor();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);

		final DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((TVModel)tvModel).addObserver(arrayDrawer);

		// set data first to avoid adding auto-generated
		// contrast to documentConfig.
		dArrayDrawer.setDataMatrix(tvModel.getDataMatrix());
		dArrayDrawer.recalculateContrast();
		dArrayDrawer.setConfigNode("ArrayDrawer1");

		// globalmaps tell globalview, atrview, and gtrview
		// where to draw each data point.
		// the scrollbars "scroll" by communicating with the maps.
		setMapContainers();

		globalXmap.setScrollbar(dendroView.getXScroll());
		globalYmap.setScrollbar(dendroView.getYScroll());

		// Drawers
		dendroView.getGlobalView().setArrayDrawer(arrayDrawer);

		// leftTreeDrawer = new LeftTreeDrawer();
		leftTreeDrawer = new TreePainter();
		dendroView.getGtrview().setLeftTreeDrawer(leftTreeDrawer);

		invertedTreeDrawer = new TreePainter();
		dendroView.getAtrview().setInvertedTreeDrawer(invertedTreeDrawer);

		setPresets();

		// this is here because my only subclass shares this code.
		bindTrees();

		// perhaps I could remember this stuff in the MapContainer...
		globalXmap.setIndexRange(0, tvModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, tvModel.getDataMatrix().getNumRow() - 1);
	}

	/**
	 * Connects all sub components with DendroView's configuration node, so that
	 * the hierarchical structure of Java's Preferences API can be followed.
	 */
	public void setPresets() {

		globalXmap.setConfigNode(configNode);// getFirst("GlobalXMap"));
		globalYmap.setConfigNode(configNode);// getFirst("GlobalYMap"));

		// URLs
		colorExtractor.setConfigNode(configNode);// getFirst("ColorExtractor"));

		dendroView.getTextview().setConfigNode(configNode);// getFirst("TextView"));
		dendroView.getArraynameview().setConfigNode(configNode);// getFirst("ArrayNameView"));
		dendroView.getAtrview().getHeaderSummary().setConfigNode(configNode);
		dendroView.getGtrview().getHeaderSummary().setConfigNode(configNode);
	}

	/**
	 * Sets up the views with the MapContainers.
	 */
	public void setMapContainers() {

		dendroView.getAtrview().setMap(globalXmap);
		dendroView.getArraynameview().setMap(globalXmap);
		dendroView.getGtrview().setMap(globalYmap);
		dendroView.getTextview().setMap(globalYmap);

		dendroView.getGlobalView().setXMap(globalXmap);
		dendroView.getGlobalView().setYMap(globalYmap);
	}
	
	/**
	 * Updates all headerInfo instances for all the view components.
	 */
	public void updateHeaderInfo() {
		
		dendroView.getAtrview().setATRHeaderInfo(tvModel.getAtrHeaderInfo());
		dendroView.getGtrview().setGTRHeaderInfo(tvModel.getGtrHeaderInfo());
		dendroView.getArraynameview().setHeaderInfo(
				tvModel.getArrayHeaderInfo());
		dendroView.getTextview().setHeaderInfo(tvModel.getGeneHeaderInfo());
		dendroView.getGlobalView().setHeaders(tvModel.getGeneHeaderInfo(),
				tvModel.getArrayHeaderInfo());
	}

	/**
	 * Displays a data set alongside the primary one for comparison.
	 * 
	 * @param model
	 *            - the model containing cdt data being added to the display.
	 */
	public void compareToModel(final TVModel model) {

		tvModel.removeAppended();
		tvModel.append(model);

		arraySelection.resize(tvModel.getDataMatrix().getNumCol());
		arraySelection.notifyObservers();

		globalXmap.setIndexRange(0, tvModel.getDataMatrix().getNumCol() - 1);
		globalXmap.notifyObservers();

		((TVModel)tvModel).notifyObservers();
	}

	/**
	 * Updates the ATRDrawer to reflect changes in the DataMode array order;
	 * rebuilds the TreeDrawerNode tree.
	 * 
	 * @param selectedID
	 *            ID of the node selected before a change in tree structure was
	 *            made. This node is then found and reselected after the ATR
	 *            tree is rebuilt.
	 */
	private void updateATRDrawer(final String selectedID) {

		try {
			invertedTreeDrawer.setData(tvModel.getAtrHeaderInfo(),
					tvModel.getArrayHeaderInfo());
			final HeaderInfo trHeaderInfo = tvModel.getAtrHeaderInfo();

			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {

				TreeColorer.colorUsingHeader(invertedTreeDrawer.getRootNode(),
						trHeaderInfo, trHeaderInfo.getIndex("NODECOLOR"));
			}
		} catch (final DendroException e) {

			// LogPanel.println("Had problem setting up the array tree : "
			// + e.getMessage());
			// e.printStackTrace();
			final Box mismatch = new Box(BoxLayout.Y_AXIS);
			mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch "
					+ "between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Array Tree, since it's lame."));

			JOptionPane.showMessageDialog(tvFrame.getAppFrame(), mismatch,
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

			dendroView.getAtrview().setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {
				LogBuffer.println("Got DendroException when trying to "
						+ "setData() on invertedTreeDrawer: " + e.getMessage());
			}
		}

		final TreeDrawerNode arrayNode = invertedTreeDrawer.getRootNode()
				.findNode(selectedID);

		arraySelection.setSelectedNode(arrayNode.getId());
		dendroView.getAtrview().setSelectedNode(arrayNode);

		arraySelection.notifyObservers();
		invertedTreeDrawer.notifyObservers();
	}

//	/**
//	 * Loads a TVModel from a provided FileSet and then returns the new TVModel.
//	 * 
//	 * @param fileSet
//	 * @return DataModel
//	 * @throws LoadException
//	 */
//	protected DataModel makeCdtModel(final FileSet fileSet) 
//			throws LoadException {
//
//		final DataModel tvModel = new TVModel();
//
//		try {
//			((TVModel)tvModel).loadNew(fileSet);
//
//		} catch (LoadException | InterruptedException | ExecutionException e) {
//			String message = "Clustering was interrupted.";
//			JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, 
//					"Error", JOptionPane.ERROR_MESSAGE);
//			LogBuffer.logException(e);
//		}
//
//		return tvModel;
//	}

	// ATR Methods
	/**
	 * Open a dialog which allows the user to select a new CDT data file for
	 * tree alignment.
	 * 
	 * @return The fileset corresponding to the dataset.
	 */
	/*
	 * Unknow what actually happens if the file CDT does not have an associated
	 * ATR.
	 */
	protected FileSet offerATRFileSelection() throws LoadException {

		FileSet fileSet1; // will be chosen...

		final JFileChooser fileDialog = new JFileChooser();
		setupATRFileDialog(fileDialog);

		final int retVal = fileDialog
				.showOpenDialog(dendroView.getDendroPane());

		if (retVal == JFileChooser.APPROVE_OPTION) {
			final File chosen = fileDialog.getSelectedFile();
			fileSet1 = new FileSet(chosen.getName(), chosen.getParent()
					+ File.separator);

		} else {
			throw new LoadException("File Dialog closed without selection...",
					LoadException.NOFILE);
		}

		return fileSet1;
	}

	/**
	 * Sets up a dialog for loading ATR files for tree alignment.
	 * 
	 * @param fileDialog
	 *            the dialog to setup
	 */
	protected void setupATRFileDialog(final JFileChooser fileDialog) {

		final CdtFilter ff = new CdtFilter();
		try {
			fileDialog.addChoosableFileFilter(ff);
			// will fail on pre-1.3 swings
			fileDialog.setAcceptAllFileFilterUsed(true);

		} catch (final Exception e) {
			LogBuffer.println("Exception when adding a ChoosableFileFilter "
					+ "and setAcceptAllFileFilterUsed(): " + e.getMessage());
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
	}

	/**
	 * Aligns the current ATR to the passed model as best as possible, saves the
	 * new ordering to the .jtv file.
	 * 
	 * @param model
	 *            - AtrTVModel with which to align.
	 */
	public void alignAtrToModel(final AtrTVModel model) {

		try {
			String selectedID = null;

			try {
				selectedID = arraySelection.getSelectedNode();

			} catch (final NullPointerException npe) {
				npe.printStackTrace();
			}

			int[] ordering;
			ordering = AtrAligner.align(tvModel.getAtrHeaderInfo(),
					tvModel.getArrayHeaderInfo(), model.getAtrHeaderInfo(),
					model.getArrayHeaderInfo());

			/*
			 * System.out.print("New ordering: "); for(int i = 0; i <
			 * ordering.length; i++) { System.out.print(ordering[i] + " "); }
			 * System.out.println();
			 */

			((TVModel)tvModel).reorderArrays(ordering);
			((TVModel)tvModel).notifyObservers();

			if (selectedID != null) {

				updateATRDrawer(selectedID);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}

//	/**
//	 * Creates an AtrTVModel for use in tree alignment.
//	 * 
//	 * @param fileSet
//	 * @return a new AtrTVModel with the file set loaded into it.
//	 * @throws LoadException
//	 */
//	protected AtrTVModel makeAtrModel(final FileSet fileSet)
//			throws LoadException {
//
//		final AtrTVModel atrTVModel = new AtrTVModel();
//
//		try {
//			atrTVModel.loadNew(fileSet);
//
//		} catch (final LoadException e) {
//			String message = "Loading Atr model was interrupted.";
//			JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, 
//					"Error", JOptionPane.ERROR_MESSAGE);
//			LogBuffer.logException(e);
//		}
//
//		return atrTVModel;
//	}

	// GTR methods
	/**
	 * Updates the GTRDrawer to reflect changes in the DataModel gene order;
	 * rebuilds the TreeDrawerNode tree.
	 * 
	 * @param selectedID
	 *            ID of the node selected before a change in tree structure was
	 *            made. This node is then found and reselected after the ATR
	 *            tree is rebuilt.
	 */
	private void updateGTRDrawer(final String selectedID) {

		try {
			leftTreeDrawer.setData(tvModel.getGtrHeaderInfo(),
					tvModel.getGeneHeaderInfo());

			final HeaderInfo trHeaderInfo = tvModel.getGtrHeaderInfo();

			if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
				TreeColorer.colorUsingHeader(leftTreeDrawer.getRootNode(),
						trHeaderInfo, trHeaderInfo.getIndex("NODECOLOR"));
			}
		} catch (final DendroException e) {

			// LogPanel.println("Had problem setting up the array tree : "
			// + e.getMessage());
			// e.printStackTrace();
			final Box mismatch = new Box(BoxLayout.Y_AXIS);
			mismatch.add(new JLabel(e.getMessage()));
			mismatch.add(new JLabel("Perhaps there is a mismatch "
					+ "between your ATR and CDT files?"));
			mismatch.add(new JLabel("Ditching Gene Tree, since it's lame."));

			JOptionPane.showMessageDialog(tvFrame.getAppFrame(), mismatch,
					"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

			dendroView.getGtrview().setEnabled(false);

			try {
				leftTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {
				LogBuffer.println("Got exception in setData() "
						+ "for leftTreeDrawer in updateGTRDrawer(): "
						+ e.getMessage());
			}
		}

		final TreeDrawerNode arrayNode = leftTreeDrawer.getRootNode().findNode(
				selectedID);

		geneSelection.setSelectedNode(arrayNode.getId());
		dendroView.getGtrview().setSelectedNode(arrayNode);

		geneSelection.notifyObservers();
		leftTreeDrawer.notifyObservers();
	}

	// Flipping the trees
	// /**
	// * Finds the currently selected genes, mirror image flips them, and then
	// * rebuilds all necessary trees and saved data to the .jtv file.
	// *
	// */
	// private void flipSelectedGTRNode() {
	//
	// int leftIndex, rightIndex;
	// String selectedID;
	// final TreeDrawerNode geneNode = leftTreeDrawer
	// .getNodeById(getGeneSelection().getSelectedNode());
	//
	// if (geneNode == null || geneNode.isLeaf()) {
	//
	// return;
	// }
	//
	// selectedID = geneNode.getId();
	//
	// // find the starting index of the left array tree, the ending
	// // index of the right array tree
	// leftIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
	// geneNode.getLeft().getLeftLeaf().getId());
	// rightIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
	// geneNode.getRight().getRightLeaf().getId());
	//
	// final int num = getDataModel().getDataMatrix().getNumRow();
	//
	// final int[] newOrder = SetupInvertedArray(num, leftIndex, rightIndex);
	//
	// /*
	// * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
	// * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
	// */
	//
	// ((TVModel) getDataModel()).reorderGenes(newOrder);
	// // ((TVModel)getDataModel()).saveGeneOrder(newOrder);
	// ((Observable) getDataModel()).notifyObservers();
	//
	// updateGTRDrawer(selectedID);
	// }

	// private int[] SetupInvertedArray(final int num, final int leftIndex,
	// final int rightIndex) {
	//
	// final int[] newOrder = new int[num];
	//
	// for (int i = 0; i < num; i++) {
	//
	// newOrder[i] = i;
	// }
	//
	// for (int i = 0; i <= (rightIndex - leftIndex); i++) {
	//
	// newOrder[leftIndex + i] = rightIndex - i;
	// }
	//
	// return newOrder;
	// }

	// /**
	// * Finds the currently selected arrays, mirror image flips them, and then
	// * rebuilds all necessary trees and saved data to the .jtv file.
	// */
	// private void flipSelectedATRNode() {
	//
	// int leftIndex, rightIndex;
	// String selectedID;
	// final TreeDrawerNode arrayNode = invertedTreeDrawer
	// .getNodeById(getArraySelection().getSelectedNode());
	//
	// if (arrayNode == null || arrayNode.isLeaf()) {
	//
	// return;
	// }
	//
	// selectedID = arrayNode.getId();
	//
	// // find the starting index of the left array tree,
	// // the ending index of the right array tree
	// leftIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
	// arrayNode.getLeft().getLeftLeaf().getId());
	// rightIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
	// arrayNode.getRight().getRightLeaf().getId());
	//
	// final int num = getDataModel().getDataMatrix().getNumUnappendedCol();
	//
	// final int[] newOrder = new int[num];
	//
	// for (int i = 0; i < num; i++) {
	//
	// newOrder[i] = i;
	// }
	//
	// for (int i = 0; i <= (rightIndex - leftIndex); i++) {
	//
	// newOrder[leftIndex + i] = rightIndex - i;
	// }
	//
	// /*
	// * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
	// * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
	// */
	//
	// ((TVModel) getDataModel()).reorderArrays(newOrder);
	// ((Observable) getDataModel()).notifyObservers();
	//
	// updateATRDrawer(selectedID);
	// }

	/**
	 * this is meant to be called from setupViews. It make sure that the trees
	 * are generated from the current model, and enables/disables them as
	 * required.
	 * 
	 * I factored it out because it is common betwen DendroView and
	 * KnnDendroView.
	 */
	protected void bindTrees() {

		if ((tvModel != null) && tvModel.aidFound()) {
			try {
				dendroView.getAtrview().setEnabled(true);

				invertedTreeDrawer.setData(tvModel.getAtrHeaderInfo(),
						tvModel.getArrayHeaderInfo());
				final HeaderInfo trHeaderInfo = tvModel.getAtrHeaderInfo();

				if (trHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(
							invertedTreeDrawer.getRootNode(), trHeaderInfo,
							trHeaderInfo.getIndex("NODECOLOR"));
				}

			} catch (final DendroException e) {
				String message = "Seems like there is a mismatch between your "
						+ "ATR and CDT files. Ditching Array Tree.";
				
				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, 
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);

				dendroView.getAtrview().setEnabled(false);

				try {
					invertedTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {
					message = "Got DendroException in setData() for "
							+ "invertedTreeDrawer in bindTrees(): " 
							+ e.getMessage();
					
					JOptionPane.showMessageDialog(tvFrame.getAppFrame(), 
							message, "Error", JOptionPane.ERROR_MESSAGE);
					LogBuffer.logException(ex);
				}
			}
		} else {
			dendroView.getAtrview().setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException e) {
				String message = "Got DendroException in setData() "
						+ "for invertedTreeDrawer in bindTrees(): "
						+ e.getMessage();
				
				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, 
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);
			}
		}

		invertedTreeDrawer.notifyObservers();

		if ((tvModel != null) && tvModel.gidFound()) {
			try {
				dendroView.getGtrview().setEnabled(true);

				leftTreeDrawer.setData(tvModel.getGtrHeaderInfo(),
						tvModel.getGeneHeaderInfo());
				final HeaderInfo gtrHeaderInfo = tvModel.getGtrHeaderInfo();

				if (gtrHeaderInfo.getIndex("NODECOLOR") >= 0) {
					TreeColorer.colorUsingHeader(leftTreeDrawer.getRootNode(),
							tvModel.getGtrHeaderInfo(),
							gtrHeaderInfo.getIndex("NODECOLOR"));

				} else {
					TreeColorer.colorUsingLeaf(leftTreeDrawer.getRootNode(),
							tvModel.getGeneHeaderInfo(), tvModel
									.getGeneHeaderInfo().getIndex("FGCOLOR"));
				}

			} catch (final DendroException e) {
				String message = "There seems to be a mismatch between your "
						+ "GTR and CDT files. Ditching Gene Tree, "
						+ "since it's lame.";
				
				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, 
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);
				
				dendroView.getGtrview().setEnabled(false);

				try {
					leftTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {
					message = "Got DendroException in setData() "
							+ "for leftTreeDrawer in bindTrees(): "
							+ ex.getMessage();
					
					JOptionPane.showMessageDialog(tvFrame.getAppFrame(), 
							message, "Error", JOptionPane.ERROR_MESSAGE);
					LogBuffer.logException(ex);
				}
			}
		} else {
			dendroView.getGtrview().setEnabled(false);

			try {
				leftTreeDrawer.setData(null, null);

			} catch (final DendroException e) {
				String message = "Got DendroException in setData() "
						+ "for leftTreeDrawer in bindTrees(): "
						+ e.getMessage();
				
				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), message, 
						"Error", JOptionPane.ERROR_MESSAGE);
				LogBuffer.logException(e);
			}
		}

		leftTreeDrawer.notifyObservers();
	}

	/**
	 * Scrolls to index i in the Y-MapContainer
	 * 
	 * @param i
	 */
	public void scrollToGene(final int i) {

		getGlobalYMap().scrollToIndex(i);
		getGlobalYMap().notifyObservers();
	}

	/**
	 * Scrolls to index i in the X-MapContainer.
	 * 
	 * @param i
	 */
	public void scrollToArray(final int i) {

		getGlobalXMap().scrollToIndex(i);
		getGlobalXMap().notifyObservers();
	}

//	/**
//	 * show summary of the specified indexes
//	 */
//	public void showSubDataModel(final int[] indexes) {
//
//		tvFrame.showSubDataModel(indexes, null, null);
//	}

	// Setters
	/**
	 * This should be called after setDataModel has been set to the appropriate
	 * model
	 * 
	 * @param arraySelection
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(dendroView);
		}

		this.arraySelection = arraySelection;
		arraySelection.addObserver(dendroView);

		dendroView.getGlobalView().setArraySelection(arraySelection);
		dendroView.getAtrview().setArraySelection(arraySelection);
		dendroView.getTextview().setArraySelection(arraySelection);
		dendroView.getArraynameview().setArraySelection(arraySelection);
	}

	/**
	 * This should be called after setDataModel has been set to the appropriate
	 * model
	 * 
	 * @param geneSelection
	 */
	public void setGeneSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(dendroView);
		}

		this.geneSelection = geneSelection;
		geneSelection.addObserver(dendroView);

		dendroView.getGlobalView().setGeneSelection(geneSelection);
		dendroView.getGtrview().setGeneSelection(geneSelection);
		dendroView.getTextview().setGeneSelection(geneSelection);
		dendroView.getArraynameview().setGeneSelection(geneSelection);
	}

	// Getters for fields
	public ArrayDrawer getArrayDrawer() {

		return arrayDrawer;
	}

	public MapContainer getGlobalXMap() {

		return globalXmap;
	}

	public MapContainer getGlobalYMap() {

		return globalYmap;
	}
}
