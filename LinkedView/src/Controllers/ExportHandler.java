/**
 * 
 */
package Controllers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import edu.stanford.genetics.treeview.ExportAspect;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PaperType;
import edu.stanford.genetics.treeview.PpmWriter;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This class provides export functionality of the matrix and trees to a file.
 * @author rleach
 */
public class ExportHandler {

	final protected DendroView dendroView;
	final protected MapContainer interactiveXmap;
	final protected MapContainer interactiveYmap;
	final protected TreeSelectionI colSelection;
	final protected TreeSelectionI rowSelection;

	protected String defPageSize = "A5"; //See freehep manual for options

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
	protected int minTileDim = 11; //Min number of "points" for a tile's edge
	protected int tileHeight = 11; //Number of "points" for a tile's height
	protected int tileWidth  = 11; //Number of "points" for a tile's width

	protected int treeMatrixGapMin = 5; //Min number of "points" bet tree/matrix
	protected int treeMatrixGapSize = 20; //Number of "points" bet tree/matrix

	/**
	 * Constructor.  All the parameters are necessary unless you are not
	 * exporting a selected area, in which case the last 2 parameters are
	 * unnecessary and you may use the other constructor.
	 * @author rleach
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
	 * Constructor.  All parameters are required.
	 * @author rleach
	 * @param dendroView
	 * @param interactiveXmap
	 * @param interactiveYmap
	 */
	public ExportHandler(final DendroView dendroView,
		final MapContainer interactiveXmap,final MapContainer interactiveYmap) {
	
		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
		this.colSelection = null;
		this.rowSelection = null;
	}

	/**
	 * Allows one to set the page size that is given to freehep.
	 * @author rleach
	 * @param dps - default page size
	 */
	public void setDefaultPageSize(String dps) {
		this.defPageSize = dps;
	}

	public void setDefaultPageSize(PaperType pT) {
		this.defPageSize = pT.toString();
	}

	/**
	 * Uses stored tileWidth, treesHeight, & treeMatrixGapSize to determine the
	 * width of the output image.
	 * @author rleach
	 * @param region
	 * @return int
	 */
	public int getXDim(final Region region) {
		return(getNumXExportIndexes(region) * tileWidth +
			(dendroView.getRowTreeView().treeExists() ? treesHeight +
				treeMatrixGapSize : 0));
	}

	/**
	 * Uses stored tileHeight, treesHeight, & treeMatrixGapSize to determine the
	 * height of the output image.
	 * @author rleach
	 * @param region
	 * @return int
	 */
	public int getYDim(final Region region) {
		return(getNumYExportIndexes(region) * tileHeight +
			(dendroView.getColumnTreeView().treeExists() ? treesHeight +
				treeMatrixGapSize : 0));
	}

	/**
	 * Determines the number of x axis data indexes that will be exported
	 * @author rleach
	 * @param region
	 * @return int
	 */
	public int getNumXExportIndexes(final Region region) {
		if(region == Region.ALL) {
			return(interactiveXmap.getTotalTileNum());
		} else if(region == Region.VISIBLE) {
			return(interactiveXmap.getNumVisible());
		} else if(region == Region.SELECTION) {
			if(colSelection == null ||
				colSelection.getNSelectedIndexes() == 0) {
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
	 * @author rleach
	 * @param region
	 * @return int
	 */
	public int getNumYExportIndexes(final Region region) {
		if(region == Region.ALL) {
			return(interactiveYmap.getTotalTileNum());
		} else if(region == Region.VISIBLE) {
			return(interactiveYmap.getNumVisible());
		} else if(region == Region.SELECTION) {
			if(rowSelection == null ||
				rowSelection.getNSelectedIndexes() == 0) {
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
	 * @author rleach
	 * @param aR - aspect ratio
	 */
	public void setTileAspectRatio(final double aR) {
		this.aspectRatio = aR;
	}

	/**
	 * Sets the aspectRatio of a tile
	 * @author rleach
	 * @param aR - aspect ratio
	 */
	public void setTileAspectRatio(final ExportAspect eAsp) {
		if(eAsp == ExportAspect.ASSEEN) {
			setTileAspectRatioToScreen(Region.VISIBLE);
		} else {
			setTileAspectRatio(1.0);
			if(eAsp != ExportAspect.ONETOONE) {
				LogBuffer.println("Warning: Invalid or unselected aspect " +
					"ratio type submitted.  Defaulting to 1:1.");
			}
		}
	}

	/**
	 * Calculates the aspect ratio in the user's window and sets the result
	 * @author rleach
	 * @param region
	 */
	public void setTileAspectRatioToScreen(final Region region) {
		int matrixPxWidth = dendroView.getInteractiveMatrixView().getWidth();
		int matrixPxHeight = dendroView.getInteractiveMatrixView().getHeight();
		int numCols = getNumXExportIndexes(region);
		int numRows = getNumYExportIndexes(region);
		this.aspectRatio = ((double) matrixPxWidth / (double) numCols) /
			((double) matrixPxHeight / (double) numRows);
	}

	/**
	 * @author rleach
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
	 * @author rleach
	 * @param treeRatio the treeRatio to set
	 */
	public void setTreeRatio(double treeRatio) {
		this.treeRatio = treeRatio;
	}

	/**
	 * Returns the fraction of the longest edge of the image that is reserved
	 * for the gap between the tree and the matrix
	 * @author rleach
	 * @return the treeMatrixGapRatio
	 */
	public double getTreeMatrixGapRatio() {
		return(treeMatrixGapRatio);
	}

	/**
	 * Sets the fraction of the longest edge of the image that is reserved
	 * for the gap between the tree and the matrix
	 * @author rleach
	 * @param treeMatrixGapRatio the treeMatrixGapRatio to set
	 */
	public void setTreeMatrixGapRatio(double treeMatrixGapRatio) {
		this.treeMatrixGapRatio = treeMatrixGapRatio;
	}

	/**
	 * Calculates the points in size of the tile dimensions and trees height
	 * that will meet all the minimum dimension requirements.  E.g. If there are
	 * very few or small tiles in the larger image dimension and the treeRatio
	 * dictates that the treesHeight works out to be smaller than the
	 * minTreesHeight, then the tile dimensions are scaled up to meet the
	 * minimum tree height requirement or vice versa.
	 * @author rleach
	 * @param region
	 */
	public void calculateDimensions(final Region region) {
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
	 * using the longer final dimension.  It uses the current tileWidth and
	 * tileHeight values to do the calculation.  This differs from comparing
	 * getXDim and getYDim which use the already calculated treeHeight.
	 * 
	 * The purpose of the existence of this method is so that we do not generate
	 * images with trees that appear too big.  What we want is the tree height
	 * to be 20 percent of the longest image dimension.  This enables that,
	 * especially when only 1 dimension has a tree, by returning the length of
	 * the longest image dimension.  The size of the trees can then be
	 * calculated, especially in the case where they need to be calculated based
	 * on the longer edge when the longer edge does not have a tree - the
	 * shorter edge does.
	 * 
	 * @author rleach
	 * @param region
	 * @return int
	 */
	public int calculateMaxFinalImageDim(final Region region) {
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
	 * @author rleach
	 * @param format - String.  Recognized values: pdf, svg, ps (and implied
	 *                 recognized values: png, jpg, and ppm)
	 * @param fileName - String
	 * @param region - String.  Implied recognized values: all, visible,
	 *                 selection
	 */
	public void export(final Format format,final String fileName,
		final Region region,final boolean showSelections) {

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return;
		}

		if(format == Format.PDF || format == Format.SVG ||
			format == Format.PS) {

			exportDocument(format,defPageSize,fileName,region,showSelections);
		} else {
			exportImage(format,fileName,region,showSelections);
		}
	}

	/**
	 * Export an image file (not on a "page")
	 * @author rleach
	 * @param format
	 * @param fileName
	 * @param region
	 */
	public void exportImage(final Format format,final String fileName,
		final Region region,final boolean showSelections) {

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return;
		}

		try {
			calculateDimensions(region);

			BufferedImage im;
			//JPG is the only format that doesn't support an alpha channel, so
			//we must create a buffered image object without the ARGB type
			if(format == Format.JPG) {
				im = new BufferedImage(
					getXDim(region),getYDim(region),BufferedImage.TYPE_INT_RGB);
			} else {
				im = new BufferedImage(getXDim(region),getYDim(region),
					BufferedImage.TYPE_INT_ARGB);
			}

			Graphics2D g2d = (Graphics2D) im.getGraphics();
			createContent(g2d,region,showSelections);

			File exportFile = new File(fileName);
			if(format == Format.PNG) {
				ImageIO.write(im,"png",exportFile);
			} else if(format == Format.JPG) {
				ImageIO.write(im,"jpg",exportFile);
			} else if(format == Format.PPM) { //ppm = bitmat
				final OutputStream os = new BufferedOutputStream(
					new FileOutputStream(exportFile));
				PpmWriter.writePpm(im,os);
			} else {
				LogBuffer.println("Unrecognized export format: [" + format +
					"].");
				return;
			}
		}
		catch(Exception exc) {
			LogBuffer.println("Unable to export image.");
			LogBuffer.logException(exc);
		}
	}

	/**
	 * Determines whether the exported region selection should work or not
	 * @author rleach
	 * @param region
	 * @return boolean
	 */
	public boolean isExportValid(final Region region) {
		if(region == Region.ALL) {
			return(interactiveXmap.getTotalTileNum() > 0);
		} else if(region == Region.VISIBLE) {
			return(interactiveXmap.getNumVisible() > 0);
		} else if(region == Region.SELECTION) {
			return(!(colSelection == null ||
				colSelection.getNSelectedIndexes() == 0));
		}

		return(false);
	}

	/**
	 * Export an image file as a part of a document in a vector format
	 * @author rleach
	 * @param format
	 * @param pageSize
	 * @param fileName
	 * @param region
	 */
	public void exportDocument(final Format format,final String pageSize,
		String fileName,final Region region,final boolean showSelections) {

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region +
				"].");
			return;
		}

		try {
			calculateDimensions(region);

			Dimension dims =
				new Dimension(getXDim(region),getYDim(region));
			File exportFile = new File(fileName);

			VectorGraphics g;
			/* TODO: This needs to supply a size of an export region */
			if(format == Format.PDF) {
				if (!fileName.endsWith(".pdf")) {
					fileName += ".pdf";
				}
				g = new PDFGraphics2D(exportFile,dims);
			} else if(format == Format.PS) {
				if (!fileName.endsWith(".ps")) {
					fileName += ".ps";
				}
				g = new PSGraphics2D(exportFile,dims);
			} else if(format == Format.SVG) {
				if (!fileName.endsWith(".svg")) {
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
			p.setProperty("PageSize",pageSize);
			g.setProperties(p); 

			g.startExport();
			createContent(g,region,showSelections);
			g.endExport();
		}
		catch(FileNotFoundException exc) {
			LogBuffer.println("File [" + fileName + "] could not be written " +
				"to.");
			LogBuffer.logException(exc);
		}
		catch(IOException exc) {
			LogBuffer.println("File [" + fileName + "] could not be written " +
				"to.");
			LogBuffer.logException(exc);
		}
	}

	/**
	 * This calls the export functions of the various components of the total
	 * image, arranged in an aligned fashion together
	 * @author rleach
	 * @param g2d
	 * @param region
	 */
	public void createContent(Graphics2D g2d,final Region region,
		final boolean showSelections) {

		dendroView.getInteractiveMatrixView().export(g2d,
			(dendroView.getRowTreeView().treeExists() ?
				treesHeight + treeMatrixGapSize : 0),
			(dendroView.getColumnTreeView().treeExists() ?
				treesHeight + treeMatrixGapSize : 0),
			tileWidth,tileHeight,region,showSelections);

		if(dendroView.getColumnTreeView().treeExists()) {
			dendroView.getColumnTreeView().export(g2d,
				(dendroView.getRowTreeView().treeExists() ?
					treesHeight + treeMatrixGapSize : 0),treesHeight,tileWidth,
					region,showSelections);
		}

		if(dendroView.getRowTreeView().treeExists()) {
			dendroView.getRowTreeView().export(g2d,treesHeight,
				(dendroView.getColumnTreeView().treeExists() ?
					treesHeight + treeMatrixGapSize : 0),tileHeight,region,
					showSelections);
		}
	}
}
