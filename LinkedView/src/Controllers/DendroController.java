package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.stanford.genetics.treeview.CdtFilter;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DummyConfigNode;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.ReorderedTreeSelection;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.model.AtrTVModel;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.AtrAligner;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroException;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.InvertedTreeDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.LeftTreeDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import edu.stanford.genetics.treeview.plugin.dendroview.TreeColorer;

public class DendroController implements ConfigNodePersistent {

	private DendroView2 dendroView;
	private TreeViewFrame tvFrame;
	private TVModel tvModel;
	
	protected ConfigNode root;
	
	private int[] arrayIndex = null;
	private int[] geneIndex = null;
	
	// Drawers
	protected ArrayDrawer arrayDrawer;
	protected InvertedTreeDrawer invertedTreeDrawer;
	protected LeftTreeDrawer leftTreeDrawer;
	
	// MapContainers
	protected MapContainer globalXmap;
	protected MapContainer globalYmap;
	
	// Selections
	private TreeSelectionI geneSelection = null;
	private TreeSelectionI arraySelection = null;
	
	// Color Extractor
	private ColorExtractor colorExtractor;
	
	public DendroController(DendroView2 dendroView, TreeViewFrame tvFrame, 
			TVModel tvModel, ConfigNode root) {
		
		this.dendroView = dendroView;
		this.tvFrame = tvFrame;
		this.tvModel = tvModel;
		this.root = root;
		
		checkConfig();
		bindComponentFunctions();
		dendroView.setupLayout();
		
		// add listeners
		dendroView.addScaleListener(new ScaleListener());
		dendroView.addZoomListener(new ZoomListener());
		dendroView.addCompListener(new ResizeListener());
	}
	
	/**
	 * Listener for the setScale-buttons in DendroView.
	 * Changes the scale in xMap and yMap MapContainers, allowing the user
	 * to zoom in or out of each individual axis in GlobalView.
	 * @author CKeil
	 *
	 */
	class ScaleListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource() == dendroView.getXPlusButton()) {
				globalXmap.zoomIn();
				
			} else if(e.getSource() == dendroView.getXMinusButton()) {
				globalXmap.zoomOut();
				
			} else if(e.getSource() == dendroView.getYPlusButton()) {
				globalYmap.zoomIn();
				
			} else if(e.getSource() == dendroView.getYMinusButton()) {
				globalYmap.zoomOut();
				
			} else if(e.getSource() == dendroView.getHomeButton()) {
				globalXmap.setHome();
				globalYmap.setHome();
			}
		}
	}
	
	/**
	 * The Zoom listener which allows the user to zoom into a selection.
	 * @author CKeil
	 *
	 */
	class ZoomListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			dendroView.zoomSelection();
		}
		
	}
	
	/**
	 * Listens to the resizing of DendroView2 and makes 
	 * changes to MapContainers as a result.
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
			
			if (globalXmap.getAvailablePixels() > globalXmap.getUsedPixels()
					&& globalXmap.getScale() == globalXmap.getMinScale()) {
				globalXmap.setHome();
			}

			if (globalYmap.getAvailablePixels() > globalYmap.getUsedPixels()
					&& globalYmap.getScale() == globalYmap.getMinScale()) {
				globalYmap.setHome();
			}

			dendroView.getDendroPane().revalidate();
			dendroView.getDendroPane().repaint();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {
		}
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
	 * binds this dendroView to a particular confignode, resizing the panel
	 * sizes appropriately.
	 * 
	 * @param configNode
	 *            ConfigNode to bind to
	 */

	@Override
	public void bindConfig(final ConfigNode configNode) {
		
		root = configNode;
		/*
		 * ConfigNode heightNodes[] = root.fetch("Height"); ConfigNode
		 * widthNodes[] = root.fetch("Width");
		 * 
		 * float heights[]; float widths[]; if (heightNodes.length != 0) {
		 * heights = new float[heightNodes.length]; widths = new
		 * float[widthNodes.length]; for (int i = 0; i < heights.length; i++) {
		 * heights[i] = (float) heightNodes[i].getAttribute( "value", 1.0 /
		 * heights.length); } for (int j = 0; j < widths.length; j++) {
		 * widths[j] = (float) widthNodes[j].getAttribute( "value", 1.0 /
		 * widths.length); } } else { widths = new float[]{2 / 11f, 3 / 11f, 3 /
		 * 11f, 3 / 11f}; heights = new float[]{3 / 16f, 1 / 16f, 3 / 4f}; }
		 * setHeights(heights); setWidths(widths);
		 */
	}
	
	/**
	 * Checks whether there is a configuration node for the current model
	 * and DendroView. If not it creates one.
	 */
	public void checkConfig() {
		
		if (root == null) {
			if (tvModel.getDocumentConfigRoot() != null) {
				bindConfig(tvModel.getDocumentConfigRoot().fetchOrCreate(
						"MainView"));

			} else {
				bindConfig(new DummyConfigNode("MainView"));
			}
		} else {
			bindConfig(root);
		}
	}
	
	/**
	 * Getter for root
	 */
	public ConfigNode getConfigNode() {

		return root;
	}
	
	/**
	 * Sets arrayIndex and geneIndex if a K-Means clustered file 
	 * has been loaded.
	 * These indexes are used to set gaps to visualize the distinct groups
	 * in a K-Means clustered file.
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
//			tvModel = new ReorderedDataModel(tvModel, geneIndex, 
//					arrayIndex);
			System.out.println("DataModel issue in DendroController.");
		}
	}
	
	/**
	 * Returns an array of indexes of K-Means groups.
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
			if (cur.equals(test) == false) {
				cur = test;
				ngroup++;
			}
		}

		final int[] groupVector = new int[ngroup + headerInfo.getNumHeaders()];
		ngroup = 0;
		cur = headerInfo.getHeader(0, groupIndex);

		for (int i = 0; i < headerInfo.getNumHeaders(); i++) {

			final String test = headerInfo.getHeader(i, groupIndex);
			if (cur.equals(test) == false) {
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
		dendroView.getArraynameview().setDataModel(tvModel);
		
		final ColorPresets colorPresets = DendrogramFactory.getColorPresets();
		colorExtractor = new ColorExtractor();
		colorExtractor.setDefaultColorSet(colorPresets.getDefaultColorSet());
		colorExtractor.setMissing(DataModel.NODATA, DataModel.EMPTY);

		final DoubleArrayDrawer dArrayDrawer = new DoubleArrayDrawer();
		dArrayDrawer.setColorExtractor(colorExtractor);
		arrayDrawer = dArrayDrawer;
		((Observable) tvModel).addObserver(arrayDrawer);
		
		// set data first to avoid adding auto-generated
		// contrast to documentConfig.
		dArrayDrawer.setDataMatrix(tvModel.getDataMatrix());
		dArrayDrawer.recalculateContrast();
		dArrayDrawer.bindConfig(getFirst("ArrayDrawer"));
		
		// Headers for GlobalView
		dendroView.getGlobalView().setHeaders(tvModel.getGeneHeaderInfo(),
				tvModel.getArrayHeaderInfo());
		
		// globalmaps tell globalview, atrview, and gtrview
		// where to draw each data point.
		// the scrollbars "scroll" by communicating with the maps.
		globalXmap = new MapContainer("Fixed");
		globalYmap = new MapContainer("Fixed");
		
		setMapContainers();
		
		globalXmap.setScrollbar(dendroView.getXScroll());
		globalYmap.setScrollbar(dendroView.getYScroll());
		
		globalXmap.bindConfig(getFirst("GlobalXMap"));
		globalYmap.bindConfig(getFirst("GlobalYMap"));
		
		dendroView.getTextview().bindConfig(getFirst("TextView"));
		dendroView.getArraynameview().bindConfig(getFirst("ArrayNameView"));
		dendroView.getAtrview().getHeaderSummary().bindConfig(
				getFirst("AtrSummary"));
		dendroView.getAtrview().getHeaderSummary().bindConfig(
				getFirst("GtrSummary"));
		
		// Drawers
		dendroView.getGlobalView().setArrayDrawer(arrayDrawer);
		
		leftTreeDrawer = new LeftTreeDrawer();
		dendroView.getGtrview().setLeftTreeDrawer(leftTreeDrawer);
		
		invertedTreeDrawer = new InvertedTreeDrawer();
		dendroView.getAtrview().setInvertedTreeDrawer(invertedTreeDrawer);
		
		// URLs
		colorExtractor.bindConfig(getFirst("ColorExtractor"));
		
		// this is here because my only subclass shares this code.
		bindTrees();
		
		// perhaps I could remember this stuff in the MapContainer...
		globalXmap.setIndexRange(0, tvModel.getDataMatrix().getNumCol() - 1);
		globalYmap.setIndexRange(0, tvModel.getDataMatrix().getNumRow() - 1);
		
		// Ensuring window resizing works with GlobalView
		globalXmap.setHome();
		globalYmap.setHome();
		
		globalXmap.notifyObservers();
		globalYmap.notifyObservers();
	}
	
	/**
	 * Sets up the views with the MapContainers.
	 */
	public void setMapContainers() {
		
		dendroView.getAtrview().setMap(globalXmap);
		dendroView.getGtrview().setMap(globalYmap);
		dendroView.getTextview().setMap(globalYmap);
		dendroView.getArraynameview().setMap(globalXmap);
		
		dendroView.getGlobalView().setXMap(globalXmap);
		dendroView.getGlobalView().setYMap(globalYmap);
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

		globalXmap.setIndexRange(0,
				tvModel.getDataMatrix().getNumCol() - 1);
		globalXmap.notifyObservers();

		((Observable) tvModel).notifyObservers();
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

			}
		}

		final TreeDrawerNode arrayNode = invertedTreeDrawer.getRootNode()
				.findNode(selectedID);

		arraySelection.setSelectedNode(arrayNode.getId());
		dendroView.getAtrview().setSelectedNode(arrayNode);

		arraySelection.notifyObservers();
		invertedTreeDrawer.notifyObservers();
	}
	
	/**
	 * Loads a TVModel from a provided FileSet and then returns the new
	 * TVModel.
	 * @param fileSet
	 * @return
	 * @throws LoadException
	 */
	protected TVModel makeCdtModel(final FileSet fileSet) throws LoadException {

		final TVModel tvModel = new TVModel();

		try {
			tvModel.loadNew(fileSet);

		} catch (final LoadException e) {
			JOptionPane.showMessageDialog(dendroView.getDendroPane(), e);
			throw e;
		} catch (InterruptedException e) {
			e.printStackTrace();
			
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return tvModel;
	}
	
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

		final int retVal = 
				fileDialog.showOpenDialog(dendroView.getDendroPane());

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
					tvModel.getArrayHeaderInfo(),
					model.getAtrHeaderInfo(), model.getArrayHeaderInfo());

			/*
			 * System.out.print("New ordering: "); for(int i = 0; i <
			 * ordering.length; i++) { System.out.print(ordering[i] + " "); }
			 * System.out.println();
			 */

			tvModel.reorderArrays(ordering);
			tvModel.notifyObservers();

			if (selectedID != null) {

				updateATRDrawer(selectedID);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}
	
	/**
	 * Creates an AtrTVModel for use in tree alignment.
	 * 
	 * @param fileSet
	 * @return a new AtrTVModel with the file set loaded into it.
	 * @throws LoadException
	 */
	protected AtrTVModel makeAtrModel(final FileSet fileSet)
			throws LoadException {

		final AtrTVModel atrTVModel = new AtrTVModel();

		try {
			atrTVModel.loadNew(fileSet);

		} catch (final LoadException e) {
			JOptionPane.showMessageDialog(dendroView.getDendroPane(), e);
			throw e;
		}

		return atrTVModel;
	}
	
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
//	/**
//	 * Finds the currently selected genes, mirror image flips them, and then
//	 * rebuilds all necessary trees and saved data to the .jtv file.
//	 * 
//	 */
//	private void flipSelectedGTRNode() {
//
//		int leftIndex, rightIndex;
//		String selectedID;
//		final TreeDrawerNode geneNode = leftTreeDrawer
//				.getNodeById(getGeneSelection().getSelectedNode());
//
//		if (geneNode == null || geneNode.isLeaf()) {
//
//			return;
//		}
//
//		selectedID = geneNode.getId();
//
//		// find the starting index of the left array tree, the ending
//		// index of the right array tree
//		leftIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
//				geneNode.getLeft().getLeftLeaf().getId());
//		rightIndex = getDataModel().getGeneHeaderInfo().getHeaderIndex(
//				geneNode.getRight().getRightLeaf().getId());
//
//		final int num = getDataModel().getDataMatrix().getNumRow();
//
//		final int[] newOrder = SetupInvertedArray(num, leftIndex, rightIndex);
//
//		/*
//		 * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
//		 * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
//		 */
//
//		((TVModel) getDataModel()).reorderGenes(newOrder);
//		// ((TVModel)getDataModel()).saveGeneOrder(newOrder);
//		((Observable) getDataModel()).notifyObservers();
//
//		updateGTRDrawer(selectedID);
//	}

//	private int[] SetupInvertedArray(final int num, final int leftIndex,
//			final int rightIndex) {
//
//		final int[] newOrder = new int[num];
//
//		for (int i = 0; i < num; i++) {
//
//			newOrder[i] = i;
//		}
//
//		for (int i = 0; i <= (rightIndex - leftIndex); i++) {
//
//			newOrder[leftIndex + i] = rightIndex - i;
//		}
//
//		return newOrder;
//	}

//	/**
//	 * Finds the currently selected arrays, mirror image flips them, and then
//	 * rebuilds all necessary trees and saved data to the .jtv file.
//	 */
//	private void flipSelectedATRNode() {
//
//		int leftIndex, rightIndex;
//		String selectedID;
//		final TreeDrawerNode arrayNode = invertedTreeDrawer
//				.getNodeById(getArraySelection().getSelectedNode());
//
//		if (arrayNode == null || arrayNode.isLeaf()) {
//
//			return;
//		}
//
//		selectedID = arrayNode.getId();
//
//		// find the starting index of the left array tree,
//		// the ending index of the right array tree
//		leftIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
//				arrayNode.getLeft().getLeftLeaf().getId());
//		rightIndex = getDataModel().getArrayHeaderInfo().getHeaderIndex(
//				arrayNode.getRight().getRightLeaf().getId());
//
//		final int num = getDataModel().getDataMatrix().getNumUnappendedCol();
//
//		final int[] newOrder = new int[num];
//
//		for (int i = 0; i < num; i++) {
//
//			newOrder[i] = i;
//		}
//
//		for (int i = 0; i <= (rightIndex - leftIndex); i++) {
//
//			newOrder[leftIndex + i] = rightIndex - i;
//		}
//
//		/*
//		 * System.out.print("Fliping to: "); for(int i = 0; i < newOrder.length;
//		 * i++) { System.out.print(newOrder[i] + " "); } System.out.println("");
//		 */
//
//		((TVModel) getDataModel()).reorderArrays(newOrder);
//		((Observable) getDataModel()).notifyObservers();
//
//		updateATRDrawer(selectedID);
//	}
	
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
				// LogPanel.println("Had problem setting up the array tree : "
				// + e.getMessage());
				// e.printStackTrace();
				final Box mismatch = new Box(BoxLayout.Y_AXIS);
				mismatch.add(new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch "
						+ "between your ATR and CDT files?"));
				mismatch.add(new JLabel("Ditching Array Tree, "
						+ "since it's lame."));

				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), mismatch,
						"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

				dendroView.getAtrview().setEnabled(false);

				try {
					invertedTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {

				}
			}
		} else {
			dendroView.getAtrview().setEnabled(false);

			try {
				invertedTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {

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
				// LogPanel.println("Had problem setting up the gene tree :
				// " + e.getMessage());
				// e.printStackTrace();
				final Box mismatch = new Box(BoxLayout.Y_AXIS);
				mismatch.add(new JLabel(e.getMessage()));
				mismatch.add(new JLabel("Perhaps there is a mismatch "
						+ "between your GTR and CDT files?"));
				mismatch.add(new JLabel("Ditching Gene Tree, "
						+ "since it's lame."));

				JOptionPane.showMessageDialog(tvFrame.getAppFrame(), mismatch,
						"Tree Construction Error", JOptionPane.ERROR_MESSAGE);

				dendroView.getGtrview().setEnabled(false);

				try {
					leftTreeDrawer.setData(null, null);

				} catch (final DendroException ex) {

				}
			}
		} else {
			dendroView.getGtrview().setEnabled(false);

			try {
				leftTreeDrawer.setData(null, null);

			} catch (final DendroException ex) {

			}
		}

		leftTreeDrawer.notifyObservers();
	}
	
	/**
	 * Scrolls to index i in the Y-MapContainer
	 * @param i
	 */
	public void scrollToGene(final int i) {

		globalYmap.scrollToIndex(i);
		globalYmap.notifyObservers();
	}

	/**
	 * Scrolls to index i in the X-MapContainer.
	 * @param i
	 */
	public void scrollToArray(final int i) {

		globalXmap.scrollToIndex(i);
		globalXmap.notifyObservers();
	}
	
	/**
	 * show summary of the specified indexes
	 */
	public void showSubDataModel(final int[] indexes) {

		tvFrame.showSubDataModel(indexes, null, null);
	}
	
	// Getters
	/**
	 * Always returns an instance of the node, even if it has to create it.
	 */
	protected ConfigNode getFirst(final String name) {

		return getConfigNode().fetchOrCreate(name);
	}
	
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
	
	// Getters
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
