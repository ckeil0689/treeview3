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
	protected double treeMatrixGapRatio = 0.005;
	protected int treeMatrixGapMin = 1;
	protected String defPageSize = "A5";

	public ExportHandler(final DendroView dendroView,
		final MapContainer interactiveXmap,final MapContainer interactiveYmap) {

		this.dendroView = dendroView;
		this.interactiveXmap = interactiveXmap;
		this.interactiveYmap = interactiveYmap;
	}

	public void setDefaultPageSize(String dps) {
		this.defPageSize = dps;
	}

	public void exportDocument(String format,String pageSize,String fileName) { //E.g. "pdf","A5","Output.pdf"
		try {
			int indent  = 500; //This is the height for the trees on both axes
			int tileDim = 20;
			int gap     = (int) (treeMatrixGapRatio *
				((interactiveXmap.getTotalTileNum() >
				interactiveYmap.getTotalTileNum() ?
					interactiveXmap.getTotalTileNum() :
						interactiveYmap.getTotalTileNum()) * tileDim +
						indent));
			if(gap < treeMatrixGapMin) {
				gap = treeMatrixGapMin;
			}

			Dimension dims =
				new Dimension(interactiveXmap.getTotalTileNum() * tileDim +
					indent + gap,
				interactiveYmap.getTotalTileNum() * tileDim + indent + gap);
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
			/* TODO: This needs to supply a start end end index for each
			 * dimension fining the export region */
			createContent(g,indent,gap,tileDim);
			g.endExport();
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	public void exportImage(String format,String fileName) { //E.g. "png","A5","Output.pdf"
		try {
			int indent  = 500; //This is the height for the trees on both axes
			int tileDim = 20;
			int gap     = (int) (treeMatrixGapRatio *
				((interactiveXmap.getTotalTileNum() >
				interactiveYmap.getTotalTileNum() ?
					interactiveXmap.getTotalTileNum() :
						interactiveYmap.getTotalTileNum()) * tileDim +
						indent));
			if(gap < treeMatrixGapMin) {
				gap = treeMatrixGapMin;
			}
	
			final BufferedImage im = new BufferedImage(
				interactiveXmap.getTotalTileNum() * tileDim + indent + gap,
				interactiveYmap.getTotalTileNum() * tileDim + indent + gap,
				BufferedImage.TYPE_INT_ARGB);
	
			Graphics2D g2d = (Graphics2D) im.getGraphics();
			createContent(g2d,indent,gap,tileDim);

			File exportFile = new File(fileName);
			if(format == "png") {
				ImageIO.write(im,"png",exportFile);
			}
			else if(format == "jpg") {
				ImageIO.write(im,"jpg",exportFile);
			}
			else if(format == "ppm") { //ppm = bitmat
				final OutputStream os = new BufferedOutputStream(
					new FileOutputStream(exportFile));
				PpmWriter.writePpm(im,os);
			}
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	public void export(String format,String fileName) { //E.g. "png","Output.pdf"
		if(format == "pdf" || format == "svg" || format == "ps") {
			exportDocument(format,defPageSize,fileName);
		} else {
			exportImage(format,fileName);
		}
	}

	public void createContent(Graphics2D g2d,int indent,int gap,int tileDim) {
		dendroView.getInteractiveMatrixView().exportPixels(g2d,indent + gap,indent + gap,tileDim);
		dendroView.getColumnTreeView().exportTree(g2d,indent + gap,indent,tileDim);
		dendroView.getRowTreeView().exportTree(g2d,indent,indent + gap,tileDim);
	}
}
