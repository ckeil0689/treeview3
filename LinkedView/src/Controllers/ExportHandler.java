/**
 * 
 */
package Controllers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.PpmWriter;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * @author rleach
 *
 */
public class ExportHandler {

	protected DendroView dendroView = null;
	protected MapContainer interactiveXmap = null;
	protected MapContainer interactiveYmap = null;
	protected TreeSelectionI colSelection = null;
	protected TreeSelectionI rowSelection = null;
	protected double treeMatrixGapRatio = 0.005;
	protected int treeMatrixGapMin = 5;
	protected int treeMatrixGapSize = 20;
	protected String defPageSize = "A5";
	protected double aspectRatio = 1.0;
	protected int minTileDim = 11;
	protected int minTreeHeight = 100;
	protected int treesHeight = 100;
	protected int tileHeight = 11;
	protected int tileWidth = 11;
	protected double treeRatio = 0.2; //fraction of the long content dimension

	public ExportHandler(final DendroView dendroView,
		final MapContainer interactiveXmap,final MapContainer interactiveYmap,
		final TreeSelectionI colSelection,final TreeSelectionI rowSelection) {

		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
		this.colSelection = colSelection;
		this.rowSelection = rowSelection;
	}

	public ExportHandler(final DendroView dendroView,
		final MapContainer interactiveXmap,final MapContainer interactiveYmap) {
	
		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
	}

	public void setDefaultPageSize(String dps) {
		this.defPageSize = dps;
	}

	public int getXDim(final String region) {
		LogBuffer.println("getXDim calculation: getNumXExportIndexes(region) * tileWidth + (dendroView.getRowTreeView().TreeExists() ? treesHeight + treeMatrixGapSize : 0) -> " +
			getNumXExportIndexes(region) + " * " + tileWidth + " + (" + dendroView.getRowTreeView().TreeExists() + " ? " + treesHeight + " + " + treeMatrixGapSize + " : 0)");
		LogBuffer.println("Row trees " + (dendroView.getRowTreeView().TreeExists() ? "exist" : "") + ".  So XDim is [" + (getNumXExportIndexes(region) * tileWidth +
		      			(dendroView.getRowTreeView().TreeExists() ? treesHeight +
		      				treeMatrixGapSize : 0)) + "]");
		return(getNumXExportIndexes(region) * tileWidth +
			(dendroView.getRowTreeView().TreeExists() ? treesHeight +
				treeMatrixGapSize : 0));
	}

	public int getYDim(final String region) {
		LogBuffer.println("Column trees " + (dendroView.getColumnTreeView().TreeExists() ? "exist" : "") + ".  So YDim is [" + (getNumYExportIndexes(region) * tileHeight +
			(dendroView.getColumnTreeView().TreeExists() ? treesHeight +
				treeMatrixGapSize : 0)) + "]");
		return(getNumYExportIndexes(region) * tileHeight +
			(dendroView.getColumnTreeView().TreeExists() ? treesHeight +
				treeMatrixGapSize : 0));
	}

	public int getNumXExportIndexes(final String region) {
		if(region == "all") {
			return(interactiveXmap.getTotalTileNum());
		} else if(region == "visible") {
			return(interactiveXmap.getNumVisible());
		} else if(region == "selection") {
			if(colSelection == null ||
				colSelection.getNSelectedIndexes() == 0) {
				LogBuffer.println("ERROR: Selection uninitialized or empty.");
				return(0);
			}
			return(colSelection.getFullSelectionRange());
		} else {
			LogBuffer.println("ERROR: Invalid export region: [" + region + "].");
			return(0);
		}
	}

	public int getNumYExportIndexes(final String region) {
		if(region == "all") {
			return(interactiveYmap.getTotalTileNum());
		} else if(region == "visible") {
			return(interactiveYmap.getNumVisible());
		} else if(region == "selection") {
			if(rowSelection == null ||
				rowSelection.getNSelectedIndexes() == 0) {
				LogBuffer.println("ERROR: Selection uninitialized or empty.");
				return(0);
			}
			return(rowSelection.getFullSelectionRange());
		} else {
			LogBuffer.println("ERROR: Invalid export region: [" + region + "].");
			return(0);
		}
	}

	public void setTileAspectRatio(final double aR) {
		this.aspectRatio = aR;
	}

	public void setTileAspectRatioToScreen(final String region) {
		int screenWidth = dendroView.getInteractiveMatrixView().getWidth();
		int screenHeight = dendroView.getInteractiveMatrixView().getHeight();
		int numCols = getNumXExportIndexes(region);
		int numRows = getNumYExportIndexes(region);
		this.aspectRatio = ((double) screenWidth / (double) numCols) / ((double) screenHeight / (double) numRows);
		LogBuffer.println("Set aspect ratio to: [" + aspectRatio + "] using: [screenWidth / numCols / screenHeight / numRows -> " + screenWidth + " / " + numCols + " / " + screenHeight + " / " + numRows + "]");
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
	 * @author rleach
	 * @param treeRatio the treeRatio to set
	 */
	public void setTreeRatio(double treeRatio) {
		this.treeRatio = treeRatio;
	}

	/**
	 * @author rleach
	 * @return the treeMatrixGapRatio
	 */
	public double getTreeMatrixGapRatio() {
		return(treeMatrixGapRatio);
	}

	/**
	 * @author rleach
	 * @param treeMatrixGapRatio the treeMatrixGapRatio to set
	 */
	public void setTreeMatrixGapRatio(double treeMatrixGapRatio) {
		this.treeMatrixGapRatio = treeMatrixGapRatio;
	}

	public void calculateDimensions(final String region) {
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
		LogBuffer.println("Calculating region dimensions. Set tile width to: [" + tileWidth + "] and tileHeight to: [" + tileHeight + "]");

		//Now we can calculate the trees height based on the shorter matrix dimension
		int maxImageDim = calculateMaxFinalImageDim(region);
		treeMatrixGapSize = (int) Math.round(treeMatrixGapRatio * (double) maxImageDim);
		if(treeMatrixGapSize < treeMatrixGapMin) {
			treeMatrixGapSize = treeMatrixGapMin;
		}
		treesHeight = (int) Math.round(treeRatio * (double) maxImageDim) - treeMatrixGapSize;
		if(treesHeight < minTreeHeight) {
			double scaleUpFactor = (double) minTreeHeight / (double) treesHeight;
			tileWidth = (int) Math.ceil(scaleUpFactor * (double) tileWidth);
			tileHeight = (int) Math.round(tileWidth / aspectRatio); //Stay close to AR
			maxImageDim = calculateMaxFinalImageDim(region);
			treeMatrixGapSize = (int) Math.round(treeMatrixGapRatio * (double) maxImageDim);
			if(treeMatrixGapSize < treeMatrixGapMin) {
				treeMatrixGapSize = treeMatrixGapMin;
			}
			treesHeight = (int) Math.round(treeRatio * (double) maxImageDim) - treeMatrixGapSize;
			if(treesHeight < minTreeHeight) {
				LogBuffer.println("ERROR: Something is wrong with the calculation of the export dimensions using scaleupFactor: [" + scaleUpFactor + "] to create tiledims: [" + tileWidth + "/" + tileHeight + "].  treesHeight: [" + treesHeight + "] turned out smaller than the minimum: [" + minTreeHeight + "].");
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
	public int calculateMaxFinalImageDim(final String region) {
		int matrixWidth = getNumXExportIndexes(region) * tileWidth;
		int matrixHeight = getNumYExportIndexes(region) * tileHeight;
		int maxMatrixDim = (matrixWidth > matrixHeight ? matrixWidth : matrixHeight);
		if(dendroView.getRowTreeView().TreeExists() &&
			dendroView.getColumnTreeView().TreeExists()) {
			return((int) Math.round((double) maxMatrixDim / (1.0 - treeRatio)));
		} else if(dendroView.getRowTreeView().TreeExists()) {
			if((int) Math.round(matrixWidth / (1.0 - treeRatio)) > matrixHeight) {
				return((int) Math.round((double) matrixWidth / (1.0 - treeRatio)));
			} else {
				return(matrixHeight);
			}
		} else if(dendroView.getColumnTreeView().TreeExists()) {
			if((int) Math.round(matrixHeight / (1.0 - treeRatio)) > matrixWidth) {
				return((int) Math.round((double) matrixHeight / (1.0 - treeRatio)));
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
	public void export(final String format,final String fileName,
		final String region) { //E.g. "png","Output.pdf","visible"

		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region + "].");
			return;
		}

		if(format == "pdf" || format == "svg" || format == "ps") {
			exportDocument(format,defPageSize,fileName,region);
		} else {
			exportImage(format,fileName,region);
		}
	}

	public void exportImage(String format,String fileName,final String region) { //E.g. "png","A5","Output.pdf"
		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region + "].");
			return;
		}

		try {
			calculateDimensions(region);

			final BufferedImage im = new BufferedImage(
				getXDim(region),getYDim(region),BufferedImage.TYPE_INT_ARGB);
	
			Graphics2D g2d = (Graphics2D) im.getGraphics();
			createContent(g2d,region);

			File exportFile = new File(fileName);
			if(format == "png") {
				ImageIO.write(im,"png",exportFile);
			} else if(format == "jpg") {
				ImageIO.write(im,"jpg",exportFile);
			} else if(format == "ppm") { //ppm = bitmat
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
			exc.printStackTrace();
		}
	}

	public boolean isExportValid(final String region) {
		if(region == "all") {
			return(interactiveXmap.getTotalTileNum() > 0);
		} else if(region == "visible") {
			return(interactiveXmap.getNumVisible() > 0);
		} else if(region == "selection") {
			return(!(colSelection == null ||
				colSelection.getNSelectedIndexes() == 0));
		}

		return(false);
	}

	public void exportDocument(String format,String pageSize,String fileName,final String region) { //E.g. "pdf","A5","Output.pdf"
		if(!isExportValid(region)) {
			LogBuffer.println("ERROR: Invalid export region: [" + region + "].");
			return;
		}

		try {
			calculateDimensions(region);

			Dimension dims =
				new Dimension(getXDim(region),getYDim(region));
			File exportFile = new File(fileName);

			VectorGraphics g;
			/* TODO: This needs to supply a size of an export region */
			if(format == "pdf") {
				if (!fileName.endsWith(".pdf")) {
					fileName += ".pdf";
				}
				g = new PDFGraphics2D(exportFile,dims);
			} else if(format == "ps") {
				if (!fileName.endsWith(".ps")) {
					fileName += ".ps";
				}
				g = new PSGraphics2D(exportFile,dims);
			} else if(format == "svg") {
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
			createContent(g,region);
			g.endExport();
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	public void createContent(Graphics2D g2d,final String region) {
		dendroView.getInteractiveMatrixView().export(g2d,
			(dendroView.getRowTreeView().TreeExists() ?
				treesHeight + treeMatrixGapSize : 0),
			(dendroView.getColumnTreeView().TreeExists() ?
				treesHeight + treeMatrixGapSize : 0),
			tileWidth,tileHeight,region);
		if(dendroView.getColumnTreeView().TreeExists()) {
			dendroView.getColumnTreeView().export(g2d,
				(dendroView.getRowTreeView().TreeExists() ? treesHeight + treeMatrixGapSize : 0),treesHeight,tileWidth,region);
		}
		if(dendroView.getRowTreeView().TreeExists()) {
			dendroView.getRowTreeView().export(g2d,treesHeight,
				(dendroView.getColumnTreeView().TreeExists() ? treesHeight + treeMatrixGapSize : 0),tileHeight,region);
		}
	}
}
