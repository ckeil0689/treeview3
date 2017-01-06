/**
 *
 */
package Controllers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.PageConstants;
import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import edu.stanford.genetics.treeview.AspectType;
import edu.stanford.genetics.treeview.ExportDialog;
import edu.stanford.genetics.treeview.ExportDialog.ExportBarDialog;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PaperType;
import edu.stanford.genetics.treeview.PpmWriter;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.model.ModelLoader;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This class provides export functionality of the matrix and trees to a file.
 */
public class ExportHandler {

	//This is the maximum size for an exported image format imposed by the fact
	//that the ImageIO class for writing PNG/JPG/PPM uses BufferedImage which
	//uses this maximum
	static final Integer MAX_IMAGE_SIZE = Integer.MAX_VALUE;

	final protected DendroView dendroView;
	final protected MapContainer interactiveXmap;
	final protected MapContainer interactiveYmap;
	final protected TreeSelectionI colSelection;
	final protected TreeSelectionI rowSelection;

	protected PaperType defPageSize = PaperType.getDefault();
	protected static String defPageOrientation = PageConstants.LANDSCAPE;

	protected double aspectRatio = 1.0; //x / y
	protected double treeRatio = 0.2; //fraction of the long content dimension
	protected double treeMatrixGapRatio = 0.005; //Gap bet tree & matrix

	/* Note: The height in "points" a tree is determines whether should provide
	 * enough space to separate a node's shoulder line from its parent's and
	 * childrens' shoulders.  Note, this package always outputs trees that are
	 * the same "height" for both column and row trees. */
	protected int minTreeHeight = 100; //Min number of "points" for tree height
	protected int treesHeight = 100; //Number of "points" for a tree's height

	/* Note: The line width of the tree is 1, so the more points thicker the
	 * tile is, the relatively more narrow the tree lines are */
	protected int minTileDim = 3; //Min number of "points" for a tile's edge
	protected int tileHeight = 3; //Number of "points" for a tile's height
	protected int tileWidth = 3; //Number of "points" for a tile's width

	protected int treeMatrixGapMin = 5; //Min number of "points" bet tree/matrix
	protected int treeMatrixGapSize = 20; //Number of "points" bet tree/matrix

	/**
	 * Constructor. All the parameters are necessary unless you are not
	 * exporting a selected area, in which case the last 2 parameters are
	 * unnecessary and you may use the other constructor.
	 * 
	 * @param dendroView
	 * @param interactiveXmap
	 * @param interactiveYmap
	 * @param colSelection
	 * @param rowSelection
	 */
	public ExportHandler(final DendroView dendroView,
		final MapContainer interactiveXmap,final MapContainer interactiveYmap,
		final TreeSelectionI colSelection,final TreeSelectionI rowSelection) {

		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
		this.colSelection = colSelection;
		this.rowSelection = rowSelection;
	}

	/**
	 * Constructor. All parameters are required.
	 * 
	 * @param dendroView
	 * @param interactiveXmap
	 * @param interactiveYmap
	 */
	public ExportHandler(final DendroView dendroView,
		final MapContainer interactiveXmap,final MapContainer interactiveYmap) {

		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
		colSelection = null;
		rowSelection = null;
	}

	/**
	 * Allows one to set the page size that is given to freehep.
	 * 
	 * @param dps - default page size
	 */
	public void setDefaultPageSize(String dps) {
		defPageSize = PaperType.getPaperType(dps);
	}

	public void setDefaultPageSize(PaperType pT) {
		defPageSize = pT;
	}

	/**
	 * @return the defPageOrientation
	 */
	public static String getDefaultPageOrientation() {
		return(defPageOrientation);
	}

	/**
	 * @param defPageOrientation the defPageOrientation to set
	 */
	public void setDefaultPageOrientation(String defPageOrientation) {
		ExportHandler.defPageOrientation = defPageOrientation;
	}

	/**
	 * Uses stored tileWidth, treesHeight, & treeMatrixGapSize to determine the
	 * width of the output image.
	 * 
	 * @param region
	 * @return int
	 */
	public int getXDim(final RegionType region) {
		int xDim = (getNumXExportIndexes(region) * tileWidth) +
			(dendroView.getRowTreeView().treeExists() ? treesHeight +
				treeMatrixGapSize : 0);
		return(xDim);
	}

	/**
	 * Gets the minimum possible x dimension size (bases on a 1:1 aspect ratio).
	 * All other aspect ratios increase the overall size.
	 *
	 * @param region
	 * @return
	 */
	public int getMinXDim(final RegionType region) {
		int minXDim = (getNumXExportIndexes(region) * minTileDim) +
			(dendroView.getRowTreeView().treeExists() ? treesHeight +
				treeMatrixGapSize : 0);
		return(minXDim);
	}

	/**
	 * Uses stored tileHeight, treesHeight, & treeMatrixGapSize to determine the
	 * height of the output image.
	 * 
	 * @param region
	 * @return int
	 */
	public int getYDim(final RegionType region) {
		int yDim = (getNumYExportIndexes(region) * tileHeight) +
			(dendroView.getColumnTreeView().treeExists() ? treesHeight +
				treeMatrixGapSize : 0);
		return(yDim);
	}

	/**
	 * Gets the minimum possible y dimension size (bases on a 1:1 aspect ratio).
	 * All other aspect ratios increase the overall size.
	 *
	 * @param region
	 * @return
	 */
	public int getMinYDim(final RegionType region) {
		int minYDim = (getNumYExportIndexes(region) * minTileDim) +
			(dendroView.getColumnTreeView().treeExists() ? treesHeight +
				treeMatrixGapSize : 0);
		return(minYDim);
	}

	/**
	 * Determines the number of x axis data indexes that will be exported
	 * 
	 * @param region
	 * @return int
	 */
	public int getNumXExportIndexes(final RegionType region) {
		if(region == RegionType.ALL) {
			return(interactiveXmap.getTotalTileNum());
		} else if(region == RegionType.VISIBLE) {
			return(interactiveXmap.getNumVisible());
		} else if(region == RegionType.SELECTION) {
			if((colSelection == null) ||
				(colSelection.getNSelectedIndexes() == 0)) {
				LogBuffer.println("ERROR: Selection uninitialized or empty.");
				return(0);
			}
			return(colSelection.getFullSelectionRange());
		} else {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return(0);
		}
	}

	/**
	 * Determines the number of y axis data indexes that will be exported
	 * 
	 * @param region
	 * @return int
	 */
	public int getNumYExportIndexes(final RegionType region) {
		if(region == RegionType.ALL) {
			return(interactiveYmap.getTotalTileNum());
		} else if(region == RegionType.VISIBLE) {
			return(interactiveYmap.getNumVisible());
		} else if(region == RegionType.SELECTION) {
			if((rowSelection == null) ||
				(rowSelection.getNSelectedIndexes() == 0)) {
				LogBuffer.println("ERROR: Selection uninitialized or empty.");
				return(0);
			}
			return(rowSelection.getFullSelectionRange());
		} else {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return(0);
		}
	}

	/**
	 * Sets the aspectRatio of a tile
	 * 
	 * @param aR - aspect ratio
	 */
	public void setTileAspectRatio(final double aR) {
		aspectRatio = aR;
	}

	/**
	 * Sets the aspectRatio of a tile
	 * 
	 * @param eAsp - aspect ratio
	 */
	public void setTileAspectRatio(final AspectType eAsp) {
		if(eAsp == AspectType.ASSEEN) {
			setTileAspectRatioToScreen(RegionType.VISIBLE);
		} else {
			setTileAspectRatio(1.0);
			if(eAsp != AspectType.ONETOONE) {
				LogBuffer.println("Warning: Invalid or unselected aspect " +
					"ratio type submitted.  Defaulting to 1:1.");
			}
		}
	}

	/**
	 * Calculates the aspect ratio in the user's window and sets the result
	 * 
	 * @param region
	 */
	public void setTileAspectRatioToScreen(final RegionType region) {
		int matrixPxWidth = dendroView.getInteractiveMatrixView().getWidth();
		int matrixPxHeight = dendroView.getInteractiveMatrixView().getHeight();
		int numCols = getNumXExportIndexes(region);
		int numRows = getNumYExportIndexes(region);
		aspectRatio = ((double) matrixPxWidth / (double) numCols) /
			((double) matrixPxHeight / (double) numRows);
	}

	/**
	 * @return the treeRatio
	 */
	public double getTreeRatio() {
		return(treeRatio);
	}

	/* TODO: I should look for the smallest branch height, so that I can set the
	 * min tree height to 1/(smallest branch height/3) */
	/**
	 * Sets the treeRatio, which is the fraction of the longest final edge of
	 * the image (when the tree(s) are included)
	 * 
	 * @param treeRatio the treeRatio to set
	 */
	public void setTreeRatio(double treeRatio) {
		this.treeRatio = treeRatio;
	}

	/**
	 * Returns the fraction of the longest edge of the image that is reserved
	 * for the gap between the tree and the matrix
	 * 
	 * @return the treeMatrixGapRatio
	 */
	public double getTreeMatrixGapRatio() {
		return(treeMatrixGapRatio);
	}

	/**
	 * Sets the fraction of the longest edge of the image that is reserved
	 * for the gap between the tree and the matrix
	 * 
	 * @param treeMatrixGapRatio the treeMatrixGapRatio to set
	 */
	public void setTreeMatrixGapRatio(double treeMatrixGapRatio) {
		this.treeMatrixGapRatio = treeMatrixGapRatio;
	}

	public void setCalculatedDimensions(final RegionType region,
		final AspectType aspect) {

		setTileAspectRatio(aspect);
		setCalculatedDimensions(region,aspectRatio);
	}

	/**
	 * Uses the currently set aspect ratio to calculate and set the dimensions/
	 *
	 * @param region
	 */
	public void setCalculatedDimensions(final RegionType region) {
		setCalculatedDimensions(region,aspectRatio);
	}

	/**
	 * Calculates the points in size of the tile dimensions and trees height
	 * that will meet all the minimum dimension requirements. E.g. If there are
	 * very few or small tiles in the larger image dimension and the treeRatio
	 * dictates that the treesHeight works out to be smaller than the
	 * minTreesHeight, then the tile dimensions are scaled up to meet the
	 * minimum tree height requirement or vice versa.
	 * 
	 * @param region
	 */
	public void setCalculatedDimensions(final RegionType region,
		final double aspectRatio) {

		//Calculates: treesHeight,treeMatrixGapSize,tileHeight, and tileWidth
		//Using: treeMatrixGapRatio,aspectRatio,treeRatio
		//And adjusting using: treeMatrixGapMin,minTileDim,minTreeHeight

		//First, we'll calculate the tile dimensions
		if(aspectRatio > 1.0) {
			tileHeight = minTileDim;
			tileWidth = (int) Math.round(aspectRatio * (double) minTileDim);
		} else {
			tileWidth = minTileDim;
			tileHeight = (int) Math.round((double) minTileDim / aspectRatio);
		}

		//Now we can calculate the trees height based on the shorter matrix
		//dimension
		int maxImageDim = calculateMaxFinalImageDim(region);
		treeMatrixGapSize =
			(int) Math.round(treeMatrixGapRatio * (double) maxImageDim);
		if(treeMatrixGapSize < treeMatrixGapMin) {
			treeMatrixGapSize = treeMatrixGapMin;
		}
		treesHeight = (int) Math.round(treeRatio * (double) maxImageDim) -
			treeMatrixGapSize;
		if(treesHeight < minTreeHeight) {
			double scaleUpFactor =
				(double) minTreeHeight / (double) treesHeight;
			tileWidth = (int) Math.ceil(scaleUpFactor * (double) tileWidth);
			tileHeight = (int) Math.round(tileWidth / aspectRatio);
			maxImageDim = calculateMaxFinalImageDim(region);
			treeMatrixGapSize =
				(int) Math.round(treeMatrixGapRatio * (double) maxImageDim);
			if(treeMatrixGapSize < treeMatrixGapMin) {
				treeMatrixGapSize = treeMatrixGapMin;
			}
			treesHeight = (int) Math.round(treeRatio * (double) maxImageDim) -
				treeMatrixGapSize;
			if(treesHeight < minTreeHeight) {
				LogBuffer.println("ERROR: Something is wrong with the " +
					"calculation of the export dimensions using " +
					"scaleupFactor: [" + scaleUpFactor +
					"] to create tiledims: [" + tileWidth + "/" + tileHeight +
					"].  treesHeight: [" + treesHeight + "] turned out " +
					"smaller than the minimum: [" + minTreeHeight + "].");
				treesHeight = minTreeHeight;
			}
		}
	}

	/**
	 * Calculates the larger final image dimension without using treesHeight,
	 * but rather treeRatio so that the actual treesHeight can be calculated
	 * using the longer final dimension. It uses the current tileWidth and
	 * tileHeight values to do the calculation. This differs from comparing
	 * getXDim and getYDim which use the already calculated treeHeight.
	 *
	 * The purpose of the existence of this method is so that we do not generate
	 * images with trees that appear too big. What we want is the tree height
	 * to be 20 percent of the longest image dimension. This enables that,
	 * especially when only 1 dimension has a tree, by returning the length of
	 * the longest image dimension. The size of the trees can then be
	 * calculated, especially in the case where they need to be calculated based
	 * on the longer edge when the longer edge does not have a tree - the
	 * shorter edge does.
	 *
	 * @param region
	 * @return int
	 */
	public int calculateMaxFinalImageDim(final RegionType region) {
		int matrixWidth = getNumXExportIndexes(region) * tileWidth;
		int matrixHeight = getNumYExportIndexes(region) * tileHeight;
		int maxMatrixDim =
			(matrixWidth > matrixHeight ? matrixWidth : matrixHeight);
		if(dendroView.getRowTreeView().treeExists() &&
			dendroView.getColumnTreeView().treeExists()) {
			return((int) Math.round((double) maxMatrixDim /
				(1.0 - treeRatio)));
		} else if(dendroView.getRowTreeView().treeExists()) {
			if((int) Math.round(matrixWidth / (1.0 - treeRatio)) >
				matrixHeight) {

				return((int) Math.round((double) matrixWidth /
					(1.0 - treeRatio)));
			} else {
				return(matrixHeight);
			}
		} else if(dendroView.getColumnTreeView().treeExists()) {
			if((int) Math.round(matrixHeight / (1.0 - treeRatio)) >
				matrixWidth) {

				return((int) Math.round((double) matrixHeight /
					(1.0 - treeRatio)));
			} else {
				return(matrixWidth);
			}
		} else {
			return(maxMatrixDim);
		}
	}

	/**
	 * Wrapper export function for all file types.
	 * 
	 * @param format - String. Recognized values: pdf, svg, ps (and implied
	 *            recognized values: png, jpg, and ppm)
	 * @param fileName - String
	 * @param region - String. Implied recognized values: all, visible,
	 *            selection
	 */
	public boolean export(final FormatType format,final String fileName,
		final RegionType region,final boolean showSelections) throws Exception {

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return false;
		}

		final ExportWorker worker = new ExportWorker(format,fileName,region,
			showSelections);
		// start the process of exporting
		worker.execute();
		worker.ebd.setVisible(true);
		return worker.isExportSuccessful();
	}

	/**
	 * Determines whether the given region is properly defined or not. It does
	 * not check whether a region is too big for export. Refer to these methods
	 * for that: getOversizedRegions, getOversizedAspects, and isOversized.
	 *
	 * @param region
	 * @return boolean
	 */
	public boolean isExportValid(final RegionType region) {
		if(region == RegionType.ALL) {
			return(interactiveXmap.getTotalTileNum() > 0);
		} else if(region == RegionType.VISIBLE) {
			return(interactiveXmap.getNumVisible() > 0);
		} else if(region == RegionType.SELECTION) {
			return(!((colSelection == null) ||
				(colSelection.getNSelectedIndexes() == 0)));
		}

		return(false);
	}

	/**
	 * Returns a list of regions that are too big to be exported in an image
	 * format (png/jpg/ppm) at the smallest (1:1) aspect ratio, regardless of
	 * current the aspect ratio datamember's value
	 *
	 * @param minimum - whether to determine if the minimum size is too big or
	 *            if the current size is too big
	 * @return
	 */
	public List<RegionType> getOversizedMinimumRegions() {
		return(getOversizedRegions(true));
	}

	/**
	 * Returns a list of regions that are at least possible to be exported at
	 * the minimum size (i.e. aspect ratio is 1:1)
	 *
	 * @return
	 */
	public List<RegionType> getMinimumAvailableRegions() {
		List<RegionType> regs = new ArrayList<RegionType>();
		for(int i = 0;i < RegionType.values().length;i++) {
			//If this region is valid for export and it is not too big
			if(isExportValid(RegionType.values()[i]) &&
				((((double) getMinXDim(RegionType.values()[i]) /
				(double) MAX_IMAGE_SIZE) *
				(double) getMinYDim(RegionType.values()[i])) <= 1.0)) {

				regs.add(RegionType.values()[i]);
			}
		}
		return(regs);
	}

	/**
	 * Determines whether an region can be exported as a PNG/JPG/PPM
	 *
	 * @return
	 */
	public boolean isImageExportPossible() {
		return(getMinimumAvailableRegions().size() > 0);
	}

	/**
	 * Returns a list of regions that are too big to be exported in an image
	 * format (png/jpg/ppm) at either the minimum or the current aspect ratio,
	 * given the minimum boolean.
	 *
	 * @param minimum - whether to determine if the minimum size is too big or
	 *            if the current size is too big
	 * @return
	 */
	public List<RegionType> getOversizedRegions(final boolean minimum) {
		List<RegionType> regs = new ArrayList<RegionType>();
		for(int i = 0;i < RegionType.values().length;i++) {
			//If this region is valid for export and it is too big
			if(isExportValid(RegionType.values()[i]) &&
				((((double) (minimum ? getMinXDim(RegionType.values()[i]) :
					getXDim(RegionType.values()[i])) /
					(double) MAX_IMAGE_SIZE) *
				(double) (minimum ? getMinYDim(RegionType.values()[i]) :
					getYDim(RegionType.values()[i]))) > 1.0)) {

				regs.add(RegionType.values()[i]);
			}
		}
		return(regs);
	}


	/**
	 * Returns a list of Aspects that are too big to be exported in an image
	 * format (png/jpg/ppm) for the given region.
	 *
	 * @param selectedRegion - The region to apply the aspect to, to get the
	 *            predicted size
	 * @return
	 */
	public List<AspectType> getOversizedAspects(
		final RegionType selectedRegion) {

		//Save the current dimension values
		double saveAspect = aspectRatio;
		int saveTreesHeight = treesHeight;
		int saveTileHeight = tileHeight;
		int saveTileWidth = tileWidth;
		int saveGapSize = treeMatrixGapSize;

		List<AspectType> asps = new ArrayList<AspectType>();
		for(int i = 0;i < AspectType.values().length;i++) {
			AspectType aspect = AspectType.values()[i];
			setCalculatedDimensions(selectedRegion,aspect);
			//If this aspect results in an image that is too big
			if((((double) getXDim(selectedRegion) / (double) MAX_IMAGE_SIZE) *
				(double) getYDim(selectedRegion)) > 1.0) {

				asps.add(aspect);
			}
		}

		//Restore the dimensions to what they were previously so that there are
		//no side-effects to running this method
		aspectRatio = saveAspect;
		treesHeight = saveTreesHeight;
		tileHeight = saveTileHeight;
		tileWidth = saveTileWidth;
		treeMatrixGapSize = saveGapSize;

		return(asps);
	}

	public int getTreesHeight() {

		return treesHeight;
	}

	public int getTreeMatrixGapSize() {

		return treeMatrixGapSize;
	}

	/**
	 * Given current settings for aspect ratio, will the supplied region end up
	 * too big?
	 *
	 * @param reg
	 * @return
	 */
	public boolean isOversized(RegionType reg) {
		//If this region is too big
		if((((double) getXDim(reg) / (double) MAX_IMAGE_SIZE) *
			(double) getYDim(reg)) > 1.0) {

		return(true); }
		return(false);
	}

	public class ExportWorker extends SwingWorker<Void,ModelLoader.LoadStatus>
		implements Observer {
		// Progress Bar used to show export process
		public ExportDialog.ExportBarDialog ebd;
		FormatType format;
		String fileName;
		final RegionType region;
		final boolean showSelections;
		ModelLoader.LoadStatus ls;
		private boolean exportSuccessful = true;

		public ExportWorker(FormatType format,String fileName,
			final RegionType region,
			final boolean showSelections) {
			ebd = new ExportBarDialog("Exporting ...",this);
			ebd.setupDialogComponents();
			ebd.setExportFileName("File Name - " + fileName);
			this.format = format;
			this.fileName = fileName;
			this.region = region;
			this.showSelections = showSelections;
			dendroView.getInteractiveMatrixView().getArrayDrawer().addObserver(
				this);
			ls = new ModelLoader.LoadStatus();
			ls.setMaxProgress(getMaxProgress());
			ebd.getProgressBar().setMaximum(ls.getMaxProgress());
		}

		@Override
		protected void process(List<ModelLoader.LoadStatus> chunks) {
			ebd.setProgressBarValue(ls.getProgress());
			ebd.setText(ls.getStatus());
			ebd.repaint();
		}

		@Override
		protected Void doInBackground() throws Exception {
			if((format == FormatType.PDF) || (format == FormatType.SVG) ||
				(format == FormatType.PS)) {

				exportDocument(format,defPageSize,fileName,region,
					showSelections);
			} else {
				try {
					exportImage(format,fileName,region,showSelections);
				}
				catch(OutOfMemoryError oome) {
					showWarning("ERROR: Out of memory.  Note, you may be " +
						"able to export a smaller portion of the matrix.");
				}
				catch(Exception e) {
					showWarning(e.getLocalizedMessage());
				}
			}
			return null;
		}

		@Override
		protected void done() {
			try {
				get();
				ebd.dispose();
			}
			catch(ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void update(Observable o,Object arg) {
			if((arg != null) && Integer.class.isInstance(arg)) {
				if(o ==
					dendroView.getInteractiveMatrixView().getArrayDrawer()) {
					int i = (int) arg;
					ls.setProgress(i);
					publish(ls);
				}
			}
		}

		private int getMaxProgress() {
			int max = 0;
			if(region == RegionType.ALL) {
				max += dendroView.getInteractiveMatrixView().getXMap().getMaxIndex() *
					dendroView.getInteractiveMatrixView().getYMap().getMaxIndex();
			} else if(region == RegionType.VISIBLE) {
				max += (dendroView.getInteractiveMatrixView().getXMap().getLastVisible() -
					dendroView.getInteractiveMatrixView().getXMap().getFirstVisible()) *
					(dendroView.getInteractiveMatrixView().getYMap().getLastVisible() -
					dendroView.getInteractiveMatrixView().getYMap().getFirstVisible());
			} else if(region == RegionType.SELECTION) {
				max += (dendroView.getInteractiveMatrixView().getColSelection().getMaxIndex() -
					dendroView.getInteractiveMatrixView().getColSelection().getMinIndex()) *
					(dendroView.getInteractiveMatrixView().getRowSelection().getMaxIndex() -
						dendroView.getInteractiveMatrixView().getRowSelection().getMinIndex());

			} else {
				LogBuffer.println("ERROR: Invalid export region: [" + region +
					"].");
			}
			return max;
		}

		/**
		 * This calls the export functions of the various components of the
		 * total
		 * image, arranged in an aligned fashion together
		 *
		 * @param g2d
		 * @param region
		 */
		private void createContent(final Graphics2D g2d,
			final RegionType region,
			final boolean showSelections) {

			ls.setStatus("Exporting interactive matrix view ...");
			ls.setProgress(0);
			publish(ls);
			dendroView.getInteractiveMatrixView().export(this,g2d,
				(dendroView.getRowTreeView().treeExists() ?
					treesHeight + treeMatrixGapSize : 0),
				(dendroView.getColumnTreeView().treeExists() ?
					treesHeight + treeMatrixGapSize : 0),
				tileWidth,tileHeight,region,showSelections);
			// Checks if the worker has been cancelled
			if(isCancelled()) {
				setExportSuccessful(false);
				return;
			}
			ls.setStatus("Exporting column tree view ...");
			publish(ls);
			if(dendroView.getColumnTreeView().treeExists()) {
				dendroView.getColumnTreeView().export(g2d,
					(dendroView.getRowTreeView().treeExists() ?
						treesHeight + treeMatrixGapSize : 0),treesHeight,
					tileWidth,
					region,showSelections);
			}

			if(isCancelled()) {
				setExportSuccessful(false);
				return;
			}
			ls.setStatus("Exporting row tree view ...");
			publish(ls);
			if(dendroView.getRowTreeView().treeExists()) {
				dendroView.getRowTreeView().export(g2d,treesHeight,
					(dendroView.getColumnTreeView().treeExists() ?
						treesHeight + treeMatrixGapSize : 0),tileHeight,region,
					showSelections);
			}

			ls.setStatus("Preparing to open the file in the default system " +
				"app");
			publish(ls);
		}

		/**
		 * Export an image file (not on a "page")
		 * 
		 * @param format
		 * @param fileName
		 * @param region
		 */
		private void exportImage(final FormatType format,final String fileName,
			final RegionType region,final boolean showSelections)
			throws Exception {

			try {
				int colorProfile;
				//JPG is the only format that doesn't support an alpha channel,
				//so we must create a buffered image object without the ARGB
				//type
				if(format == FormatType.JPG) {
					colorProfile = BufferedImage.TYPE_INT_RGB;
					LogBuffer.println("Exporting withOUT an alpha channel");
				} else {
					colorProfile = BufferedImage.TYPE_INT_ARGB;
					LogBuffer.println("Exporting with an alpha channel");
				}

				setCalculatedDimensions(region);

				BufferedImage im = new BufferedImage(getXDim(region),
					getYDim(region),colorProfile);
				Graphics2D g2d = (Graphics2D) im.getGraphics();

				//Formats JPG and PPM default to a black background, so we need
				//to draw a white canvas.  Note, setting the background color
				//did not work
				if((format == FormatType.JPG) || (format == FormatType.PPM)) {
					g2d.setBackground(Color.WHITE);
					g2d.setColor(Color.WHITE);
					g2d.fillRect(0,0,getXDim(region),getYDim(region));
				}

				createContent(g2d,region,showSelections);

				File exportFile = new File(fileName);
				if(format == FormatType.PNG) {
					ImageIO.write(im,"png",exportFile);
				} else if(format == FormatType.JPG) {
					//Code from http://stackoverflow.com/questions/17108234/
					//setting-jpg-compression-level-with-imageio-in-java
					JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(
						null);
					jpegParams.setCompressionMode(
						ImageWriteParam.MODE_EXPLICIT);
					jpegParams.setCompressionQuality(1f);
					final ImageWriter writer =
						ImageIO.getImageWritersByFormatName("jpg").next();
					writer.setOutput(new FileImageOutputStream(exportFile));
					writer.write(null,new IIOImage(im,null,null),jpegParams);
				} else if(format == FormatType.PPM) { //ppm = bitmat
					final OutputStream os = new BufferedOutputStream(
						new FileOutputStream(exportFile));
					PpmWriter.writePpm(im,os);
				} else {
					LogBuffer.println("Unrecognized export format: [" + format +
						"].");
					return;
				}
			}
			catch(IllegalArgumentException iae) {
				double tooBig = ((double) getXDim(region) /
					(double) MAX_IMAGE_SIZE) * (double) getYDim(region);
				BigDecimal bd = new BigDecimal(tooBig);
				bd = bd.round(new MathContext(4));
				double rounded = bd.doubleValue();
				throw new Exception("Error: Unable to export image.\n\n" +
					"Exported region [" + region.toString() + ": " +
					getNumXExportIndexes(region) + "cols x " +
					getNumYExportIndexes(region) + "rows] is about [" +
					rounded +
					"] times too big to export.\n\nPlease zoom to a smaller " +
					"area and try exporting only that visible portion.",iae);
			}
			catch(Exception exc) {
				double tooBig = ((double) getXDim(region) /
					(double) MAX_IMAGE_SIZE) * (double) getYDim(region);
				if(tooBig > 1.0) {
					BigDecimal bd = new BigDecimal(tooBig);
					bd = bd.round(new MathContext(4));
					double rounded = bd.doubleValue();
					throw new Exception("Error: Unable to export image.\n\n" +
						"Exported region [" + region.toString() + ": " +
						getNumXExportIndexes(region) + "cols x " +
						getNumYExportIndexes(region) + "rows] is about [" +
						rounded +
						"] times too big to export.\n\nPlease zoom to " +
						"a smaller area and try exporting only that visible " +
						"portion.",exc);
				} else {
					exc.printStackTrace();
					throw new Exception(
						"Unknown Error: Unable to export image.\n" +
							"Try exporting a smaller area.",exc);
				}
			}
		}

		/**
		 * Export an image file as a part of a document in a vector format
		 *
		 * @param format
		 * @param pageSize
		 * @param fileName
		 * @param region
		 */
		private void
			exportDocument(final FormatType format,final PaperType pageSize,
				String fileName,final RegionType region,
				final boolean showSelections) {

			try {
				setCalculatedDimensions(region);

				Dimension dims =
					new Dimension(getXDim(region),getYDim(region));
				File exportFile = new File(fileName);

				VectorGraphics g;
				if(format == FormatType.PDF) {
					if(!fileName.endsWith(".pdf")) {
						fileName += ".pdf";
					}
					g = new PDFGraphics2D(exportFile,dims);
				} else if(format == FormatType.PS) {
					if(!fileName.endsWith(".ps")) {
						fileName += ".ps";
					}
					g = new PSGraphics2D(exportFile,dims);
				} else if(format == FormatType.SVG) {
					if(!fileName.endsWith(".svg")) {
						fileName += ".svg";
					}
					g = new SVGGraphics2D(exportFile,dims);
				} else {
					g = null;
					LogBuffer.println("Unrecognized export format: [" + format +
						"].");
					return;
				}

				Properties p = new Properties();
				p.setProperty(PDFGraphics2D.PAGE_SIZE,pageSize.toString());
				p.setProperty(PDFGraphics2D.ORIENTATION,defPageOrientation);
				g.setProperties(p);

				g.startExport();
				createContent(g,region,showSelections);
				g.endExport();
			}
			catch(FileNotFoundException exc) {
				LogBuffer.println("File [" + fileName +
					"] could not be written " +
					"to.");
				LogBuffer.logException(exc);
			}
			catch(IOException exc) {
				LogBuffer.println("File [" + fileName +
					"] could not be written " +
					"to.");
				LogBuffer.logException(exc);
			}
		}

		public boolean isExportSuccessful() {
			return exportSuccessful;
		}

		public void setExportSuccessful(boolean exportSuccessful) {
			this.exportSuccessful = exportSuccessful;
		}

		private void showWarning(final String message) {

			JOptionPane.showMessageDialog(ebd,
				message,"Warning",JOptionPane.WARNING_MESSAGE);
			LogBuffer.println(message);
		}
	}

}
