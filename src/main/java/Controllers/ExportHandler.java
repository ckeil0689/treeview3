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
import org.freehep.graphicsio.AbstractVectorGraphicsIO;
import org.freehep.graphicsio.FontConstants;
import org.freehep.graphicsio.PageConstants;
import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import edu.stanford.genetics.treeview.AspectType;
import edu.stanford.genetics.treeview.ExportDialog;
import edu.stanford.genetics.treeview.ExportDialog.ExportBarDialog;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.ExportOptions;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PaperType;
import edu.stanford.genetics.treeview.PpmWriter;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.model.ModelLoader;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.LabelView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This class provides export functionality of the matrix and trees to a file.
 */
public class ExportHandler {

	//This is the maximum size for an exported image format imposed by the fact
	//that the ImageIO class for writing PNG/PPM uses BufferedImage which
	//uses this maximum
	static final Integer MAX_IMAGE_SIZE = Integer.MAX_VALUE;

	final protected DendroView dendroView;
	final protected MapContainer interactiveXmap;
	final protected MapContainer interactiveYmap;
	final protected TreeSelectionI colSelection;
	final protected TreeSelectionI rowSelection;

	protected PaperType pageSize = PaperType.getDefault();
	protected static String pageOrientation = PageConstants.LANDSCAPE;
	protected boolean showSelecions = true; //Regardless of selec. existence
	protected FormatType format; //Only important to know doc or image


	protected double aspectRatio = 1.0; //x / y
	protected double treeRatio = 0.2; //fraction of the long content dimension
	protected double gapRatio = 0.005; //Gap bet tree, labels, & matrix

	/* Note: The height in "points" a tree is determines whether should provide
	 * enough space to separate a node's shoulder line from its parent's and
	 * childrens' shoulders.  Note, this package always outputs trees that are
	 * the same "height" for both column and row trees. */
	protected int minTreesHeight = 100; //Min number of "points" for tree height
	protected int treesHeight = 100; //Number of "points" for a tree's height

	/* Note: The line width of the tree is 1, so the more points thicker the
	 * tile is, the relatively more narrow the tree lines are */
	protected int minTileDim = 3; //Min number of "points" for a tile's edge
	protected int curMinTileDim = minTileDim; //Changed by label height
	protected int tileHeight = curMinTileDim; //Number of "points" for a tile's height
	protected int tileWidth = curMinTileDim; //Number of "points" for a tile's width

	protected int minGapSize = 5; //Min number of "points" bet tree/matrix
	protected int gapSize = 20; //Number of "points" bet tree/matrix

	//Label Options
	protected LabelExportOption rowLabelsIncluded = LabelExportOption.NO;
	protected LabelExportOption colLabelsIncluded = LabelExportOption.NO;
	protected TreeExportOption rowTreeIncluded = TreeExportOption.AUTO;
	protected TreeExportOption colTreeIncluded = TreeExportOption.AUTO;
	protected int minFontPoints = 1;
	public final static int SQUEEZE = LabelView.getSqueeze(); //static
	protected int labelAreaHeight = minFontPoints + SQUEEZE; //Hght for 1 label
	protected int rowLabelPaneWidth = 0;
	protected int colLabelPaneWidth = 0;

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
	 * @param pT - paper type
	 */
	public void setPaperType(PaperType pT) {
		pageSize = pT;
	}

	/**
	 * @return the pageOrientation
	 */
	public static String getPageOrientation() {
		return(pageOrientation);
	}

	/**
	 * @param pageOrientation the pageOrientation to set
	 */
	public void setPageOrientation(String pageOrientation) {
		ExportHandler.pageOrientation = pageOrientation;
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
			getRowTreeAndGapLen() + getRowLabelAndGapLen();
		return(xDim);
	}

	/**
	 * This determines the pre-calculated matrix-only width.  Used for
	 * determining whether the matrix is too big for document format because it
	 * uses an embedded PNG that is limited by BufferedImage
	 * 
	 * @param region
	 * @return
	 */
	public int getMatrixXDim(final RegionType region) {
		int xDim = (getNumXExportIndexes(region) * tileWidth);
		return(xDim);
	}

	/**
	 * Gets the minimum possible x dimension size (based on a 1:1 aspect ratio).
	 * All other aspect ratios increase the overall size.
	 *
	 * @param region
	 * @return
	 */
	public int getMinXDim(final RegionType region) {
		int minXDim = (getNumXExportIndexes(region) * curMinTileDim) +
			getRowTreeAndGapLen() + getRowLabelAndGapLen();
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
			getColTreeAndGapLen() + getColLabelAndGapLen();
		return(yDim);
	}

	/**
	 * This determines the pre-calculated matrix-only height.  Used for
	 * determining whether the matrix is too big for document format because it
	 * uses an embedded PNG that is limited by BufferedImage
	 * 
	 * @param region
	 * @return
	 */
	public int getMatrixYDim(final RegionType region) {
		int yDim = (getNumYExportIndexes(region) * tileHeight);
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
		int minYDim = (getNumYExportIndexes(region) * curMinTileDim) +
			getColTreeAndGapLen() + getColLabelAndGapLen();
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
	 * @return the gapRatio
	 */
	public double getGapRatio() {
		return(gapRatio);
	}

	/**
	 * Sets the fraction of the longest edge of the image that is reserved
	 * for the gap between the tree and the matrix
	 * 
	 * @param gapRatio the treeMatrixGapRatio to set
	 */
	public void setGapRatio(double gapRatio) {
		this.gapRatio = gapRatio;
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

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return;
		}

		//Calculates: treesHeight,treeMatrixGapSize,tileHeight, and tileWidth
		//Using: treeMatrixGapRatio,aspectRatio,treeRatio
		//And adjusting using: treeMatrixGapMin,minTileDim,minTreeHeight

		//Update various dimensions to account for labels (if they're included)
		updateForLabelSize(region);

		//First, we'll calculate the tile dimensions
		if(aspectRatio > 1.0) {
			tileHeight = curMinTileDim;
			tileWidth = (int) Math.round(aspectRatio * (double) curMinTileDim);
		} else {
			tileWidth = curMinTileDim;
			tileHeight = (int) Math.round((double) curMinTileDim / aspectRatio);
		}

		//Now we can calculate the trees height based on the shorter matrix
		//dimension
		int maxImageDim = calculateMaxFinalImageDim(region);
		gapSize =
			(int) Math.round(gapRatio * (double) maxImageDim);
		if(gapSize < minGapSize) {
			gapSize = minGapSize;
		}
		treesHeight = (int) Math.round(treeRatio * (double) maxImageDim);

		//If the trees are smaller than the minimum, scale up the whole image
		//(resolution) until they are large enough
		if(treesHeight < minTreesHeight) {
			double scaleUpFactor =
				(double) minTreesHeight / (double) treesHeight;
			tileWidth = (int) Math.ceil(scaleUpFactor * (double) tileWidth);
			tileHeight = (int) Math.round(tileWidth / aspectRatio);
			maxImageDim = calculateMaxFinalImageDim(region);

			//Note, we do not need to recalculate the label area dimensions or
			//the font size because we're defaulting to the min (or fixed, if
			//set) font and the dimensions of the area are based on the longest
			//string and the tile dimensions

			gapSize =
				(int) Math.round(gapRatio * (double) maxImageDim);
			if(gapSize < minGapSize) {
				gapSize = minGapSize;
			}
			treesHeight = (int) Math.round(treeRatio * (double) maxImageDim);

			//Just in case there's a math or precision error above...
			if(treesHeight < minTreesHeight) {
				LogBuffer.println("ERROR: Something is wrong with the " +
					"calculation of the export dimensions using " +
					"scaleupFactor: [" + scaleUpFactor +
					"] to create tiledims: [" + tileWidth + "/" + tileHeight +
					"].  treesHeight: [" + treesHeight + "] turned out " +
					"smaller than the minimum: [" + minTreesHeight + "].");
				treesHeight = minTreesHeight;
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
		int matrixLabelWidth = getNumXExportIndexes(region) * tileWidth;
		if(areRowLabelsIncluded()) {
			matrixLabelWidth += rowLabelPaneWidth;
		}
		int matrixLabelHeight = getNumYExportIndexes(region) * tileHeight;
		if(areColLabelsIncluded()) {
			matrixLabelHeight += colLabelPaneWidth;
		}
		//This is the size component that is determined statically (not by
		//proportion, like trees height or gap sizes)
		int maxPartialDimSize =
			(matrixLabelWidth > matrixLabelHeight ?
				matrixLabelWidth : matrixLabelHeight);
		boolean maxDimIncludesLabels = (matrixLabelWidth > matrixLabelHeight ?
			areRowLabelsIncluded() : areColLabelsIncluded());

		if(isRowTreeIncluded() && isColTreeIncluded()) {
			return((int) Math.round((double) maxPartialDimSize /
				(1.0 - treeRatio -
					gapRatio * (maxDimIncludesLabels ? 2.0 : 1.0))));
		} else if(isRowTreeIncluded()) {
			int finalWidth = (int) Math.round(matrixLabelWidth / (1.0 -
				treeRatio - gapRatio * (areRowLabelsIncluded() ? 2.0 : 1.0)));
			int finalHeight = (int) Math.round(matrixLabelHeight /
				(1.0 - (areColLabelsIncluded() ? gapRatio : 0.0)));
			//If the final width is larger than the final height in the case of
			//only the row tree being present
			return(Math.max(finalHeight,finalWidth));
		} else if(isColTreeIncluded()) {
			int finalWidth = (int) Math.round(matrixLabelWidth / (1.0 -
				(areRowLabelsIncluded() ? gapRatio : 0.0)));
			int finalHeight = (int) Math.round(matrixLabelHeight / (1.0 -
				treeRatio - gapRatio * (areColLabelsIncluded() ? 2.0 : 1.0)));
			//If the final width is larger than the final height in the case of
			//only the row tree being present
			return(Math.max(finalHeight,finalWidth));
		} else {
			return((int) Math.round((double) maxPartialDimSize /
				(1.0 - (maxDimIncludesLabels ? gapRatio : 0.0))));
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
		final RegionType region,final boolean showSelections,
		final LabelExportOption rowLabelOption,
		final LabelExportOption colLabelOption) throws Exception {

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return false;
		}

		final ExportWorker worker = new ExportWorker(format,fileName,region,
			showSelections,rowLabelOption,colLabelOption);
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
		return(getOversizedRegions(true,false));
	}

	/**
	 * Returns a list of regions that are at least possible to be exported at
	 * the minimum size (i.e. aspect ratio is 1:1)
	 *
	 * @param matrixOnly - Whether or not to base available options on the
	 *                     entire image (for image formats) or just on the
	 *                     matrix (for document formats that embed a PNG)
	 * @return
	 */
	public List<RegionType> getMinimumAvailableRegions(
		final boolean matrixOnly) {

		List<RegionType> regs = new ArrayList<RegionType>();
		for(int i = 0;i < RegionType.values().length;i++) {
			RegionType rt = RegionType.values()[i];

			//Skip regions like selection when no selection exists
			if(!isExportValid(rt)) {
				continue;
			}

			int xdim,ydim;
			if(matrixOnly) {
				xdim = getNumXExportIndexes(rt) * curMinTileDim;
				ydim = getNumYExportIndexes(rt) * curMinTileDim;
			} else {
				xdim = getMinXDim(rt);
				ydim = getMinYDim(rt);
			}

			//If this region is not too big
			if((double) xdim / (double) MAX_IMAGE_SIZE * (double) ydim <= 1.0) {
				regs.add(rt);
			}
		}
		return(regs);
	}

	/**
	 * Determines whether a region can be exported as a PNG/PPM
	 *
	 * @return
	 */
	public boolean isImageExportPossible() {
		return(getMinimumAvailableRegions(false).size() > 0);
	}

	/**
	 * Returns a list of regions that are too big to be exported in an image
	 * format (png/jpg/ppm) at either the minimum or the current aspect ratio,
	 * given the minimum boolean.
	 *
	 * @param minimum - whether to determine if the minimum size is too big or
	 *            if the current size is too big
	 * @param matrixOnly - Whether or not to base exportability on the
	 *                     entire image (for image formats) or just on the
	 *                     matrix (for document formats that embed a PNG)
	 * @return
	 */
	public List<RegionType> getOversizedRegions(final boolean minimum,
		final boolean matrixOnly) {

		List<RegionType> regs = new ArrayList<RegionType>();
		for(int i = 0;i < RegionType.values().length;i++) {
			RegionType rt = RegionType.values()[i];

			//Skip regions like selection when no selection exists
			if(!isExportValid(rt)) {
				continue;
			}

			int xdim,ydim;
			if(matrixOnly) {
				xdim = getNumXExportIndexes(rt) *
					(minimum ? curMinTileDim : tileWidth);
				ydim = getNumYExportIndexes(rt) *
					(minimum ? curMinTileDim : tileHeight);
			} else {
				xdim = minimum ? getMinXDim(rt) : getXDim(rt);
				ydim = minimum ? getMinYDim(rt) : getYDim(rt);
			}

			//If this region is too big
			if((((double) xdim / (double) MAX_IMAGE_SIZE) * (double) ydim) >
				1.0) {

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
		final RegionType selectedRegion, final boolean matrixOnly) {

		//Save the current dimension values
		double saveAspect = aspectRatio;
		int saveTreesHeight = treesHeight;
		int saveTileHeight = tileHeight;
		int saveTileWidth = tileWidth;
		int saveGapSize = gapSize;

		int xdim,ydim;
		if(matrixOnly) {
			xdim = getNumXExportIndexes(selectedRegion) * tileWidth;
			ydim = getNumYExportIndexes(selectedRegion) * tileHeight;
		} else {
			xdim = getXDim(selectedRegion);
			ydim = getYDim(selectedRegion);
		}

		List<AspectType> asps = new ArrayList<AspectType>();
		for(int i = 0;i < AspectType.values().length;i++) {
			AspectType aspect = AspectType.values()[i];
			setCalculatedDimensions(selectedRegion,aspect);
			//If this aspect results in an image that is too big
			//The logic below is weird to avoid generating a number larger than
			//Integer.MAX_VALUE.  It's more easily understood as:
			//if(w*h > Integer.MAX_VALUE)
			//NOTE: if we are doing matrixOnly, we do not include the gaps,
			//labels, or trees.  This is intended to service the case where we
			//embed a PNG of the matrix inside a PDF
			if(((double) xdim / (double) MAX_IMAGE_SIZE * (double) ydim) >
				1.0) {

				asps.add(aspect);
			}
		}

		//Restore the dimensions to what they were previously so that there are
		//no side-effects to running this method
		aspectRatio = saveAspect;
		treesHeight = saveTreesHeight;
		tileHeight = saveTileHeight;
		tileWidth = saveTileWidth;
		gapSize = saveGapSize;

		return(asps);
	}

	public int getTreesHeight() {
		return treesHeight;
	}

	public int getGapSize() {
		return gapSize;
	}

	/**
	 * Given current settings for aspect ratio, will the supplied region end up
	 * too big?
	 *
	 * @param reg
	 * @return
	 */
	public boolean isOversized(RegionType reg) {
		if(format == null || !format.isDocumentFormat()) {
			if((((double) getXDim(reg) / (double) MAX_IMAGE_SIZE) *
				(double) getYDim(reg)) > 1.0) {

				return(true);
			}
		} else {
			if((((double) getMatrixXDim(reg) / (double) MAX_IMAGE_SIZE) *
				(double) getMatrixYDim(reg)) > 1.0) {

				return(true);
			}
		}

		//If this region is too big
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
		final LabelExportOption rowLabelOption;
		final LabelExportOption colLabelOption;
		ModelLoader.LoadStatus ls;
		private boolean exportSuccessful = true;

		public ExportWorker(FormatType format,String fileName,
			final RegionType region,
			final boolean showSelections,
			final LabelExportOption rowLabelOption,
			final LabelExportOption colLabelOption) {
			ebd = new ExportBarDialog("Exporting ...",this);
			ebd.setupDialogComponents();
			ebd.setExportFileName("File Name - " + fileName);
			this.format = format;
			this.fileName = fileName;
			this.region = region;
			this.showSelections = showSelections;
			this.rowLabelOption = rowLabelOption;
			this.colLabelOption = colLabelOption;
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
			try {
				export();
			}
			catch(OutOfMemoryError oome) {
				LogBuffer.println(oome.getLocalizedMessage());
				int x = (format.isDocumentFormat() ?
					getMatrixXDim(region) : getXDim(region));
				int y = (format.isDocumentFormat() ?
					getMatrixYDim(region) : getYDim(region));
				double tooBig =
					((double) x / (double) MAX_IMAGE_SIZE) * (double) y;
				if(tooBig > 1.0) {
					throw new ExportException(ExportHandler.this,region);
				} else {
					LogBuffer.println("Export NOT too big.  [x" + x + " * y" +
						y + "] <= [" + MAX_IMAGE_SIZE + "].");
					oome.printStackTrace();
					showWarning("Out of memory.\n\nNote, you may be able to " +
						"export a smaller portion of the matrix or select " +
						"fewer\noptions which increase image size or " +
						"resolution (such as the inclusion of labels\nor " +
						"selecting the as-seen-on-screen aspect ratio).");
				}
				setExportSuccessful(false);
			}
			catch(Exception e) {
				showWarning(e.getLocalizedMessage());
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
		 * This calls the export functions of the trees
		 *
		 * @param g2d
		 * @param region
		 * @param showSelections
		 */
		private void createContentForTrees(final Graphics2D g2d,final RegionType region,
			final boolean showSelections) {

			if(isColTreeIncluded()) {
				ls.setProgress(0);
				ls.setStatus("Exporting column tree ...");
				publish(ls);

				dendroView.getColumnTreeView().export(g2d,
					getRowTreeAndGapLen() + getRowLabelAndGapLen(),
					treesHeight,
					tileWidth,
					region,showSelections);
			}

			if(isRowTreeIncluded()) {
				ls.setProgress(0);
				ls.setStatus("Exporting row tree ...");
				publish(ls);

				dendroView.getRowTreeView().export(g2d,treesHeight,
					getColTreeAndGapLen() + getColLabelAndGapLen(),
					tileHeight,region,
					showSelections);
			} 
		}
		
		/**
		 * This calls the export functions of the various components of the total
		 * image, arranged in an aligned fashion together
		 * 
		 * @param g2d
		 * @param region
		 */
		private void createContent(final Graphics2D g2d,
			final RegionType region,final boolean showSelections,
			final LabelExportOption rowLabelOption,
			final LabelExportOption colLabelOption,final boolean isDocFormat) {

			ls.setProgress(0);
			ls.setStatus("Exporting matrix ...");
			publish(ls);
			//If this is a document format, embed a PNG of the matrix in the
			//document so that the size of the exported file is not enormous
			if(isDocFormat) {
				BufferedImage im =
					new BufferedImage(getNumXExportIndexes(region) * tileWidth,
						getNumYExportIndexes(region) * tileHeight,
						BufferedImage.TYPE_INT_ARGB);
				Graphics2D imGraphics = (Graphics2D) im.getGraphics();
				// create contents of the image
				createContentForIMVAlone(imGraphics,region,showSelections);
				// draw the image to the vector graphics g
				g2d.drawImage(im,
					getRowTreeAndGapLen() + getRowLabelAndGapLen(),
					getColTreeAndGapLen() + getColLabelAndGapLen(),null);
			} else {
				dendroView.getInteractiveMatrixView().export(this,g2d,
					getRowTreeAndGapLen() + getRowLabelAndGapLen(),
					getColTreeAndGapLen() + getColLabelAndGapLen(),
					tileWidth,tileHeight,region,showSelections);
				// Checks if the worker has been cancelled
				if(isCancelled()) {
					setExportSuccessful(false);
					return;
				}
			}

			createContentForTrees(g2d, region, showSelections);

			if(rowLabelOption != LabelExportOption.NO) {
				ls.setProgress(0);
				ls.setStatus("Exporting row labels ...");
				publish(ls);

				//Determine how long the labels are
				int labelLength =
					dendroView.getColLabelView().getMaxExportStringLength(region,
						colLabelOption == LabelExportOption.SELECTION,labelAreaHeight - SQUEEZE);

				dendroView.getRowLabelView().export(g2d,
					getRowTreeAndGapLen(),
					getColTreeAndGapLen() +
					(colLabelOption != LabelExportOption.NO ?
						labelLength + gapSize : 0),
					tileHeight,region,showSelections,
					rowLabelOption == LabelExportOption.SELECTION,
					labelAreaHeight - SQUEEZE);
			}

			//Doing the column labels last because it rotates the coordinate system and I'm not certain how to unrotate it
			if(colLabelOption != LabelExportOption.NO) {
				ls.setProgress(0);
				ls.setStatus("Exporting column labels ...");
				publish(ls);

				//Determine how long the labels are
				int labelLength =
					dendroView.getRowLabelView().getMaxExportStringLength(region,
						rowLabelOption == LabelExportOption.SELECTION,labelAreaHeight - SQUEEZE);

				dendroView.getColLabelView().export(g2d,
					getRowTreeAndGapLen() +
						(rowLabelOption != LabelExportOption.NO ?
								labelLength + gapSize : 0),
					getColTreeAndGapLen(),tileWidth,region,showSelections,
					colLabelOption == LabelExportOption.SELECTION,
					labelAreaHeight - SQUEEZE);
			}
			if(isCancelled()) {
				setExportSuccessful(false);
				return;
			}

			ls.setStatus("Preparing to open the file in the default system " +
				"app");
			publish(ls);
		}
		
		/**
		 * @param g2d
		 * @param region
		 * @param showSelections
		 */
		private void createContentForIMVAlone(final Graphics2D g2d,
			final RegionType region, final boolean showSelections) {
			ls.setStatus("Exporting matrix ...");
			publish(ls);
			dendroView.getInteractiveMatrixView().export(this,g2d,0,0,
				tileWidth,tileHeight,region,showSelections);
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

		/**
		 * Export a file
		 */
		private void export() throws Exception {
			try {
				fileName = format.appendExtension(fileName);
				File exportFile = new File(fileName);

				setCalculatedDimensions(region);
				Dimension dims =
					new Dimension(getXDim(region),getYDim(region));

				if(format.isDocumentFormat()) {
					VectorGraphics g;
					if(format == FormatType.PDF) {
						g = new PDFGraphics2D(exportFile,dims);
					} else if(format == FormatType.PS) {
						g = new PSGraphics2D(exportFile,dims);
					} else if(format == FormatType.SVG) {
						g = new SVGGraphics2D(exportFile,dims);
					} else {
						LogBuffer.println("Unrecognized export format: [" +
							format + "].");
						return;
					}

					Properties p = new Properties();
					p.setProperty(PDFGraphics2D.PAGE_SIZE,pageSize.toString());
					p.setProperty(PDFGraphics2D.ORIENTATION,pageOrientation);

					//These 3 commands are entirely what makes the labels
					//selectable/editable in PDF and other vector formats
					p.setProperty(PDFGraphics2D.EMBED_FONTS,"true");
					p.setProperty(PDFGraphics2D.EMBED_FONTS_AS,
						FontConstants.EMBED_FONTS_TYPE3);
					p.setProperty(AbstractVectorGraphicsIO.TEXT_AS_SHAPES,
						"false");

					g.setProperties(p);

					g.startExport();

					createContent(g,region,showSelections,rowLabelOption,
						colLabelOption,true);

					g.endExport();
				} else {
					int colorProfile;
					if(format.hasAlpha()) {
						colorProfile = BufferedImage.TYPE_INT_ARGB;
						LogBuffer.println("Exporting with an alpha channel");
					} else {
						colorProfile = BufferedImage.TYPE_INT_RGB;
						LogBuffer.println("Exporting withOUT an alpha channel");
					}

					BufferedImage im = new BufferedImage(getXDim(region),
						getYDim(region),colorProfile);
					Graphics2D g2d = (Graphics2D) im.getGraphics();

					//Formats JPG and PPM default to a black background, so we
					//need to draw a white canvas.  Note, setting the background
					//color did not work
					if(format.hasDefaultBackground()) {
						g2d.setBackground(Color.WHITE);
						g2d.setColor(Color.WHITE);
						g2d.fillRect(0,0,getXDim(region),getYDim(region));
					}

					createContent(g2d,region,showSelections,rowLabelOption,
						colLabelOption,false);

					if(format == FormatType.PNG) {
						ImageIO.write(im,"png",exportFile);
					} else if(format == FormatType.JPG) {
						//Code from http://stackoverflow.com/questions/17108234/
						//setting-jpg-compression-level-with-imageio-in-java
						JPEGImageWriteParam jpegParams =
							new JPEGImageWriteParam(null);
						jpegParams.setCompressionMode(
							ImageWriteParam.MODE_EXPLICIT);
						jpegParams.setCompressionQuality(1f);
						final ImageWriter writer =
							ImageIO.getImageWritersByFormatName("jpg").next();
						writer.setOutput(new FileImageOutputStream(exportFile));
						writer.write(null,new IIOImage(im,null,null),
							jpegParams);
					} else if(format == FormatType.PPM) { //ppm = bitmat
						final OutputStream os = new BufferedOutputStream(
							new FileOutputStream(exportFile));
						PpmWriter.writePpm(im,os);
					} else {
						LogBuffer.println("Unrecognized export format: [" +
							format + "].");
						return;
					}
				}
			}
			catch(FileNotFoundException fnfe) {
				LogBuffer.println("File [" + fileName + "] not found.");
				LogBuffer.logException(fnfe);
			}
			catch(IOException ioe) {
				LogBuffer.println("File [" + fileName +
					"] could not be written to.");
				LogBuffer.logException(ioe);
			}
			catch(Exception exc) {
				int x = (format.isDocumentFormat() ?
					getMatrixXDim(region) : getXDim(region));
				int y = (format.isDocumentFormat() ?
					getMatrixYDim(region) : getYDim(region));
				double tooBig =
					((double) x / (double) MAX_IMAGE_SIZE) * (double) y;
				if(tooBig > 1.0) {
					BigDecimal bd = new BigDecimal(tooBig);
					bd = bd.round(new MathContext(4));
					double rounded = bd.doubleValue();
					int overflow = 0;
					if(tooBig < 2.0) {
						overflow = (int) Math.round((double) MAX_IMAGE_SIZE *
							(tooBig - 1.0));
					}
					LogBuffer.println("Export too big.  [x" + x + " * y" + y +
						"] > [" + MAX_IMAGE_SIZE + "].");
					setExportSuccessful(false);
					throw new Exception("Error: Unable to export image.\n\n" +
						"Exported region [" + region.toString() + ": " +
						getNumXExportIndexes(region) + "cols x " +
						getNumYExportIndexes(region) + "rows] is about [" +
						(overflow == 0 ?
							rounded + "] times" : overflow + "] points") +
						" too big for image export.\n\nPlease select a " +
						"smaller area, fewer options to include (e.g. " +
						"labels), or reduce the minimum font size and try " +
						"again.",exc);
				} else {
					LogBuffer.println("Export NOT too big.  [x" + x + " * y" +
						y + "] <= [" + MAX_IMAGE_SIZE + "].");
					exc.printStackTrace();
					setExportSuccessful(false);
					throw new Exception(
						"Unknown Error: Unable to export image.\n" +
							"Try exporting a smaller area.",exc);
				}
			}
		}
	}

	/**
	 * Determines whether adding a row labels panel to the exported image will
	 * make the image size exceed the dimensions allowable for image export
	 * (limited by BufferedImage's MAX_INTEGER requirement).
	 * @param reg
	 * @param matrixOnly - Only calculate dimensions of the matrix necessary to
	 *                     accommodate the labels
	 * @return
	 */
	public boolean areRowLabelsTooBig(final RegionType reg,
		final boolean matrixOnly) {

		/* TODO: This needs to be redone differently. This strategy of saving
		 * the label option, changing, calculating, then restoring &
		 * recalculating is problematic. What if the calling code had not set
		 * calculated dimensions yet? What should be done is setting options &
		 * calculating in the caller and then asking if it's too big. */
		LabelExportOption save_rleo = rowLabelsIncluded;
		setRowLabelsIncluded(true);
		setCalculatedDimensions(reg);

		int xdim,ydim;
		if(matrixOnly) {
			xdim = getNumXExportIndexes(reg) * tileWidth;
			ydim = getNumYExportIndexes(reg) * tileHeight;
		} else {
			xdim = getXDim(reg);
			ydim = getYDim(reg);
		}

		//If this region is valid for export and it is not too big
		if(isExportValid(reg) &&
			((double) xdim / (double) MAX_IMAGE_SIZE * (double) ydim) < 1.0) {

			setRowLabelsIncluded(save_rleo);
			setCalculatedDimensions(reg);
			return(false);
		}

		setRowLabelsIncluded(save_rleo);
		setCalculatedDimensions(reg);
		return(true);
	}

	/**
	 * Determines whether adding a column labels panel to the exported image
	 * will make the image size exceed the dimensions allowable for image export
	 * (limited by BufferedImage's MAX_INTEGER requirement).
	 * @param reg
	 * @param matrixOnly - Only calculate dimensions of the matrix necessary to
	 *                     accommodate the labels
	 * @return
	 */
	public boolean areColLabelsTooBig(final RegionType reg,
		final boolean matrixOnly) {

		/* TODO: This needs to be redone differently. This strategy of saving
		 * the label option, changing, calculating, then restoring &
		 * recalculating is problematic. What if the calling code had not set
		 * calculated dimensions yet? What should be done is setting options &
		 * calculating in the caller and then asking if it's too big. */
		LabelExportOption save_cleo = colLabelsIncluded;
		setColLabelsIncluded(true);
		setCalculatedDimensions(reg);

		int xdim,ydim;
		if(matrixOnly) {
			xdim = getNumXExportIndexes(reg) * tileWidth;
			ydim = getNumYExportIndexes(reg) * tileHeight;
		} else {
			xdim = getXDim(reg);
			ydim = getYDim(reg);
		}

		//If this region is valid for export and it is not too big
		if(isExportValid(reg) &&
			((double) xdim / (double) MAX_IMAGE_SIZE * (double) ydim) < 1.0) {

			setColLabelsIncluded(save_cleo);
			setCalculatedDimensions(reg);
			return(false);
		}

		setColLabelsIncluded(save_cleo);
		setCalculatedDimensions(reg);
		return(true);
	}

	/**
	 * Getter for rowLabelsIncluded
	 * @return the rowLabelsIncluded
	 */
	public LabelExportOption getRowLabelsIncluded() {
		return(rowLabelsIncluded);
	}

	/**
	 * Check whether or not any row labels are included
	 * @return boolean whether any labels are included or not
	 */
	public boolean areRowLabelsIncluded() {
		return(rowLabelsIncluded != LabelExportOption.NO);
	}

	/**
	 * Calculate and return the space needed for the labels and gap
	 * 
	 * @return
	 */
	public int getRowLabelAndGapLen() {
		return(areRowLabelsIncluded() ? rowLabelPaneWidth + gapSize : 0);
	}

	/**
	 * Calculate and return the space needed for the labels and gap
	 * 
	 * @return
	 */
	public int getColLabelAndGapLen() {
		return(areColLabelsIncluded() ? colLabelPaneWidth + gapSize : 0);
	}

	/**
	 * Calculate and return the space needed for the tree and gap
	 * 
	 * @return
	 */
	public int getRowTreeAndGapLen() {
		return(isRowTreeIncluded() ? treesHeight + gapSize : 0);
	}

	/**
	 * Calculate and return the space needed for the tree and gap
	 * 
	 * @return
	 */
	public int getColTreeAndGapLen() {
		return(isColTreeIncluded() ? treesHeight + gapSize : 0);
	}

	/**
	 * 
	 * @param rowLabelsIncluded the rowLabelsIncluded to set
	 */
	public void setRowLabelsIncluded(LabelExportOption rowLabelsIncluded) {
		this.rowLabelsIncluded = rowLabelsIncluded;
	}

	/**
	 * 
	 * @param rowLabelsIncluded the rowLabelsIncluded to set
	 */
	public void setRowLabelsIncluded(final boolean rowLabelsIncluded) {
		if(rowLabelsIncluded) {
			this.rowLabelsIncluded = LabelExportOption.YES;
		} else {
			this.rowLabelsIncluded = LabelExportOption.NO;
		}
	}

	/**
	 * 
	 * @return the colLabelsIncluded
	 */
	public LabelExportOption getColLabelsIncluded() {
		return(colLabelsIncluded);
	}

	/**
	 * 
	 * @return boolean wehter any labels are included or not
	 */
	public boolean areColLabelsIncluded() {
		return(colLabelsIncluded != LabelExportOption.NO);
	}

	/**
	 * 
	 * @param colLabelsIncluded the colLabelsIncluded to set
	 */
	public void setColLabelsIncluded(LabelExportOption colLabelsIncluded) {
		this.colLabelsIncluded = colLabelsIncluded;
	}

	/**
	 * 
	 * @param colLabelsIncluded the colLabelsIncluded to set
	 */
	public void setColLabelsIncluded(final boolean colLabelsIncluded) {
		if(colLabelsIncluded) {
			this.colLabelsIncluded = LabelExportOption.YES;
		} else {
			this.colLabelsIncluded = LabelExportOption.NO;
		}
	}

	public TreeExportOption getRowTreeIncluded() {
		return rowTreeIncluded;
	}

	public boolean isRowTreeIncluded() {
		if(rowTreeIncluded != TreeExportOption.NO) {
			return(dendroView.getRowTreeView().treeExists());
		} else {
			return(false);
		}
	}

	public void setRowTreeIncluded(TreeExportOption rowTreeIncluded) {
		this.rowTreeIncluded = rowTreeIncluded;
	}

	public TreeExportOption getColTreeIncluded() {
		return colTreeIncluded;
	}

	public void setColTreeIncluded(TreeExportOption colTreeIncluded) {
		this.colTreeIncluded = colTreeIncluded;
	}

	public boolean isColTreeIncluded() {
		if(colTreeIncluded != TreeExportOption.NO) {
			return(dendroView.getColumnTreeView().treeExists());
		} else {
			return(false);
		}
	}

	/**
	 * 
	 * @return the fontPoints
	 */
	public int getMinFontPoints() {
		return(minFontPoints);
	}

	/**
	 * 
	 * @param fontPoints the fontPoints to set
	 */
	public void setMinFontPoints(int fontPoints) {
		this.minFontPoints = fontPoints;
	}

	/**
	 * 
	 * @return the labelAreaHeight
	 */
	public int getLabelAreaHeight() {
		return(labelAreaHeight);
	}

	/**
	 * 
	 * @param labelAreaHeight the labelAreaHeight to set
	 */
	public void setLabelAreaHeight(int labelAreaHeight) {
		this.labelAreaHeight = labelAreaHeight;
	}

	/**
	 * Grabs the minimum (or fixed) font height (plus SQUEEZE) and updates
	 * labelAreaHeight and curMinTileDim (if labels are included).
	 * Adds 1 to curMinTileDim if the height is less than 10 and even, so that
	 * the tree branch and label will look centered on the tile
	 * @param region the region whose labels are being exported
	 */
	public void updateForLabelSize(RegionType rt) {
		//Label height is the same for both row and column labels, so we're arbitrarily
		//grabbing the row view's label height
		try {
			setLabelAreaHeight(
				dendroView.getRowLabelView().getMinLabelTileHeight());
		} catch(Exception e) {
			setLabelAreaHeight(minFontPoints + SQUEEZE);
		}
		if(areColLabelsIncluded() || areRowLabelsIncluded()) {
			curMinTileDim = labelAreaHeight;
		} else {
			curMinTileDim = minTileDim;
		}
		if(curMinTileDim < 10 && curMinTileDim % 2 == 0) {
			curMinTileDim++;
		}

		//Update the height of the col label area
		if(getColLabelsIncluded() == LabelExportOption.YES) {
			colLabelPaneWidth =
				dendroView.getColLabelView().getMaxExportStringLength(rt,false,
					labelAreaHeight - SQUEEZE);
		} else if(getColLabelsIncluded() == LabelExportOption.SELECTION) {
			colLabelPaneWidth =
				dendroView.getColLabelView().getMaxExportStringLength(rt,true,
					labelAreaHeight - SQUEEZE);
		} else {
			colLabelPaneWidth = 0;
		}

		//Update the length of the row label area
		if(getRowLabelsIncluded() == LabelExportOption.YES) {
			rowLabelPaneWidth =
				dendroView.getRowLabelView().getMaxExportStringLength(rt,false,
					labelAreaHeight - SQUEEZE);
		} else if(getRowLabelsIncluded() == LabelExportOption.SELECTION) {
			rowLabelPaneWidth =
				dendroView.getRowLabelView().getMaxExportStringLength(rt,true,
					labelAreaHeight - SQUEEZE);
		} else {
			rowLabelPaneWidth = 0;
		}
	}

	public int getRowLabelPanelWidth(final RegionType region,
		LabelExportOption rowLabelOption) {

		if(rowLabelOption == LabelExportOption.NO) {
			return(0);
		}
		return(dendroView.getRowLabelView().getMaxExportStringLength(region,
			rowLabelOption == LabelExportOption.SELECTION,
			labelAreaHeight - SQUEEZE));
	}

	public int getColLabelPanelHeight(final RegionType region,
		LabelExportOption colLabelOption) {

		if(colLabelOption == LabelExportOption.NO) {
			return(0);
		}
		return(dendroView.getColLabelView().getMaxExportStringLength(region,
			colLabelOption == LabelExportOption.SELECTION,
			labelAreaHeight - SQUEEZE));
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public void setTileHeight(int tileHeight) {
		this.tileHeight = tileHeight;
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public void setTileWidth(int tileWidth) {
		this.tileWidth = tileWidth;
	}

	/**
	 * 
	 * @return the defShowSelecions
	 */
	public boolean isShowSelecions() {
		return(showSelecions);
	}

	/**
	 * 
	 * @param showSelecions the defShowSelecions to set
	 */
	public void setShowSelecions(boolean showSelecions) {
		this.showSelecions = showSelecions;
	}

	/**
	 * 
	 * @return the format
	 */
	public FormatType getFormat() {
		return(format);
	}

	/**
	 * 
	 * @param format the format to set
	 */
	public void setFormat(FormatType format) {
		this.format = format;
	}

	public void setOptions(ExportOptions eo) {
		setRowLabelsIncluded(eo.getRowLabelOption());
		setColLabelsIncluded(eo.getColLabelOption());
		setRowTreeIncluded(eo.getRowTreeOption());
		setColTreeIncluded(eo.getColTreeOption());
		setTileAspectRatio(eo.getAspectType());
		setPageOrientation(eo.getOrientation());
		setPaperType(eo.getPaperType());
		setShowSelecions(eo.isShowSelections());
		setFormat(eo.getFormatType());
	}

	/**
	 * This creates and returns an ExportOptions object, set with valid default
	 * values based on the data, but regardless of whether the sizes are too
	 * big.  Use getSetBestOptions for that.
	 * 
	 * @return
	 */
	public ExportOptions getDefaultOptions() {
		ExportOptions eo = new ExportOptions();

		//We're not going to worry about whether the settings are too big - just
		//retrieve otherwise valid defaults
		eo.setAspectType(AspectType.getDefault());

		//We can assume labels exist
		eo.setRowLabelOption(LabelExportOption.getDefault());
		eo.setColLabelOption(LabelExportOption.getDefault());

		//Make sure there are trees before including them as a default
		eo.setColTreeOption(isColTreeIncluded() ?
			TreeExportOption.getDefault() : TreeExportOption.NO);
		eo.setRowTreeOption(isRowTreeIncluded() ?
			TreeExportOption.getDefault() : TreeExportOption.NO);

		eo.setFormatType(FormatType.getDefault());
		eo.setOrientation(pageOrientation);
		eo.setPaperType(PaperType.getDefault());

		//Make sure the region exists, otherwise default to ALL
		eo.setRegionType(isExportValid(RegionType.getDefault()) ?
			RegionType.getDefault() : RegionType.ALL);

		//Arbitrarily don't show selections by default
		eo.setShowSelections(isExportValid(RegionType.SELECTION) ?
			showSelecions : false);

		return(eo);
	}

	/**
	 * Returns default option settings for size of the exported image.  Tries to
	 * adhere to the default settings where possible, but reduces options to the
	 * smallest possible if the resulting export is too big for the
	 * BufferedImage class.  Catches an exception from its helper method
	 * (getSetBestOptionsHelper) if no size will work, shows a warning dialog,
	 * and returns null.
	 * 
	 * @return
	 */
	public ExportOptions getSetBestOptions() throws ExportException {
		ExportOptions eo = getDefaultOptions();
		ExportOptions beo = eo;
		try {
			beo = getSetBestOptionsHelper(eo);
		} catch(ExportException e) {
			throw new ExportException(this,eo.getRegionType());
		}
		return(beo);
	}

	/**
	 * This is an internal helper method to getSetBestOptions().  It takes a set
	 * of options and explores options that would result in a smaller image to
	 * see if the image is small enough for export.  It checks lesser options in
	 * reverse hierarchical order, starting with labels, aspect ratio, region,
	 * and format type.  If no other modifications can be made to the options to
	 * result in a small enough export, an exception is thrown.
	 * 
	 * @param eo
	 * @return
	 * @throws Exception
	 */
	protected ExportOptions getSetBestOptionsHelper(ExportOptions eo) throws
		ExportException {

		setOptions(eo);
		setCalculatedDimensions(eo.getRegionType());

		//First, try eliminating one or both sets of labels
		if(isOversized(eo.getRegionType())) {
			if(areColLabelsIncluded()) {
				setColLabelsIncluded(false);
				eo.setColLabelOption(LabelExportOption.NO);
				setCalculatedDimensions(eo.getRegionType());
				if(isOversized(eo.getRegionType())) {
					setColLabelsIncluded(true);
					eo.setColLabelOption(LabelExportOption.YES);
					setCalculatedDimensions(eo.getRegionType());
					if(areRowLabelsIncluded()) {
						setRowLabelsIncluded(false);
						eo.setRowLabelOption(LabelExportOption.NO);
						setCalculatedDimensions(eo.getRegionType());
						if(isOversized(eo.getRegionType())) {
							setColLabelsIncluded(false);
							eo.setColLabelOption(LabelExportOption.NO);
							setCalculatedDimensions(eo.getRegionType());
						} else {
							return(eo);
						}
					}
				} else {
					return(eo);
				}
			} else if(areRowLabelsIncluded()) {
				setRowLabelsIncluded(false);
				eo.setRowLabelOption(LabelExportOption.NO);
				setCalculatedDimensions(eo.getRegionType());
			}
		} else {
			return(eo);
		}

		//If that wasn't enough, try minimizing the tile aspect
		if(isOversized(eo.getRegionType())) {
			if(eo.getAspectType() == AspectType.ASSEEN) {
				setTileAspectRatio(AspectType.ONETOONE);
				eo.setAspectType(AspectType.ONETOONE);
				setCalculatedDimensions(eo.getRegionType());
			}
		} else {
			return(eo);
		}

		//If that wasn't enough, try minimizing the region
		if(isOversized(eo.getRegionType())) {
			if(eo.getRegionType() == RegionType.ALL) {
				eo.setRegionType(RegionType.VISIBLE);
				setCalculatedDimensions(RegionType.VISIBLE);
				if(isOversized(RegionType.VISIBLE)) {
					if(isExportValid(RegionType.SELECTION)) {
						eo.setRegionType(RegionType.SELECTION);
						setCalculatedDimensions(RegionType.SELECTION);
					}
				} else {
					return(eo);
				}
			} else if(eo.getRegionType() == RegionType.VISIBLE) {
				if(isExportValid(RegionType.SELECTION)) {
					eo.setRegionType(RegionType.SELECTION);
					setCalculatedDimensions(RegionType.SELECTION);
				}
			} else if(eo.getRegionType() == RegionType.SELECTION) {
				if(isOversized(RegionType.VISIBLE)) {
					eo.setRegionType(RegionType.VISIBLE);
					setCalculatedDimensions(RegionType.VISIBLE);
				} else {
					return(eo);
				}
			}
		} else {
			return(eo);
		}

		//Currently, it's not possible to export without trees if they exist, so
		//the last resort is trying to switch from an image format to document
		//We'll use recursion so that we can re-use the above code
		if(!eo.getFormatType().isDocumentFormat() &&
			isOversized(eo.getRegionType())) {

			setFormat(FormatType.getDefaultDocumentFormat());
			eo.setFormatType(FormatType.getDefaultDocumentFormat());
			return(getSetBestOptionsHelper(eo));
		} else if(isOversized(eo.getRegionType())) {
			throw new ExportException(this,eo.getRegionType());
		}

		return(eo);
	}

	/**
	 * 
	 * @return the maxImageSize
	 */
	public static Integer getMaxImageSize() {
		return(MAX_IMAGE_SIZE);
	}
}
