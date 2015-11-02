/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Stack;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeDrawerNode;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * Subclass of ExportPanel which outputs a postscript version of a DendroView.
 *
 */
public class PostscriptExportPanel extends ExportPanel implements SettingsPanel {

	// I wish I could just inherit this...
	public PostscriptExportPanel(final HeaderInfo arrayHeaderInfo,
			final HeaderInfo geneHeaderInfo,
			final TreeSelectionI geneSelection,
			final TreeSelectionI arraySelection,
			final TreePainter arrayTreeDrawer,
			final TreePainter geneTreeDrawer, final ArrayDrawer arrayDrawer,
			final MapContainer arrayMap, final MapContainer geneMap) {
		super(arrayHeaderInfo, geneHeaderInfo, geneSelection, arraySelection,
				arrayTreeDrawer, geneTreeDrawer, arrayDrawer, arrayMap,
				geneMap, false);
	}

	@Override
	protected Font getGeneFont() {
		return new Font("Courier", 0, 8);
	}

	@Override
	protected Font getArrayFont() {
		return new Font("Courier", 0, 8);
	}

	@Override
	public void synchronizeTo() {
		save();
	}

	@Override
	public void synchronizeFrom() {
		// do nothing...
	}

	@Override
	public void save() {
		try {
			final PrintStream output = new PrintStream(
					new BufferedOutputStream(new FileOutputStream(getFile())));

			final DendroPSWriter psw = new DendroPSWriter();
			psw.write(output);

			output.close();
		} catch (final Exception e) {
			LogBuffer.println("PostscriptExportPanel.save() caught exception "
					+ e);
			e.printStackTrace();
		}
	}

	/**
	 * Inner class which outputs a postscript version of Dendroview like things
	 *
	 * It is loosely coupled in that it only calls protected methods in the
	 * ExporPanel superclass.
	 */
	class DendroPSWriter {

		/**
		 * Writes out postscript header, much of it stolen directly from eisen.
		 */
		private void writeHeader(final PrintStream ps) {
			final int totalWidth = estimateWidth();
			;
			final int totalHeight = estimateHeight();

			ps.println("%!PS-Adobe-3.0");
			if (includeBbox()) {
				ps.println("%%BoundingBox: 0 0 " + totalWidth + " "
						+ totalHeight);
			}
			ps.println("%%Creator: DendroPSWriter (a TreeView Component)");
			ps.println("%%CreationDate: " + (new java.util.Date()).toString());
			ps.println("%%Pages: (atend)");
			ps.println("%%EndComments");
			ps.println("%%BeginSetup");

			ps.println("/ln { newpath moveto lineto stroke closepath } bind def");
			ps.println("/tx { newpath moveto show closepath } bind def");
			ps.println("/sl { setlinewidth } def");
			ps.println("/sc { setlinecap } def");
			ps.println("/sr { setrgbcolor } def");
			ps.println("/sf { exch findfont exch scalefont setfont } def");
			ps.println("/tr { translate } def");
			ps.println("/sp { 1 sc 1 sl 0.0 0.0 0.0 sr 18.00000 13.00000 tr 0.96000 0.98205 scale tr } def");
			ps.println("/fb {exch dup 0 rlineto exch 0 exch rlineto neg 0 rlineto closepath fill } bind def");
			// consLineTo duplicates the point on the stack, lineto and strokes,
			// and then moves to it.
			ps.println("/consLineTo {1 index 1 index lineto stroke moveto} bind def");
			// the following expects rx, ry, tx, ly, lx as arguments, and draws
			// a line connecting, for GTR
			ps.println("/snGTR {1 index moveto 1 index exch consLineTo 1 index consLineTo lineto stroke} bind def");
			ps.println("/snATR {1 index exch moveto 1 index consLineTo 1 index exch consLineTo exch lineto stroke } bind def");

			// old eisen fillbox: '/fillbox {newpath moveto 8 0 rlineto 0 8
			// rlineto -8 0 rlineto closepath fill} def
			ps.println("%%EndSetup");
			ps.println("%%Page: tree 1");
			ps.println("%%PageResources: (atend)");
			ps.println("%%BeginPageSetup");
			ps.println("/pgsave save def");
			ps.println("%%EndPageSetup");

		}

		/**
		 * draws boxes using maps with the lower left corner at the current
		 * origin.
		 *
		 */
		private void writeBoxes(final PrintStream ps) {
			final int height = (int) getYmapHeight();

			if (includeGtr()) { // make room for Gtr...
				ps.println("% make room for gtrview");
				ps.println(getGtrWidth() + " 0 translate");
			}

			final int yoff = getYmapPixel(minGene() - 0.5);
			final int xoff = -getXmapPixel(minArray() - 0.5);
			ps.println("% account for offset into data matrix");
			ps.println(xoff + " " + yoff + " translate");
			// HACK doesn't account for discontinuous selection...
			// for each row...
			for (int i = minGene(); i <= maxGene(); i++) {
				final int maxArray = maxArray(); // for efficiency...
				for (int j = minArray(); j <= maxArray; j++) {
					final Color color = getArrayDrawer().getColor(j, i);
					// setcolor
					ps.println(convertColor(color) + " sr");
					// move to lower left corner...
					final int lx = getXmapPixel(j - 0.5);
					final int ly = getYmapPixel(i - 0.5);
					final int ux = getXmapPixel(j + 0.5);
					final int uy = getYmapPixel(i + 0.5);

					ps.println((lx) + " " + (height - uy) + " moveto");
					// draw filled box
					final int w = ux - lx;
					final int h = uy - ly;
					ps.println(w + " " + h + " fb");
				}
			}
			ps.println((-xoff) + " " + (-yoff) + " translate");

			if (includeGtr()) {
				ps.println((-getGtrWidth()) + " 0 translate");
			}
		}

		private void writeGeneNames(final PrintStream ps) {
			// translate over
			if (getGeneAnnoLength() <= 0)
				return;
			if (includeArrayMap()) {
				ps.println(getXmapWidth() + " 0 translate");
			}
			if (includeGtr()) {
				ps.println(getGtrWidth() + " 0 translate");
				// if (includeAtr()) ps.println("0 " + getAtrHeight() +
				// " translate");
			}

			ps.println(" /Courier findfont");
			ps.println("8 scalefont");
			ps.println("setfont");

			final int yoff = getYmapPixel(minGene() - 0.5);
			final int xoff = 0;
			ps.println("% account for offset into data matrix");
			ps.println(xoff + " " + yoff + " translate");
			final int height = (int) getYmapHeight();
			final int maxGene = maxGene();

			for (int j = minGene(); j <= maxGene; j++) {
				final Color bgColor = getGeneBgColor(j);
				if (bgColor != null) {
					final int lx = 0;
					final int ly = getYmapPixel(j - 0.5);
					final int ux = getGeneAnnoLength();
					final int uy = getYmapPixel(j + 0.5);
					ps.println(convertColor(bgColor) + " sr");
					// ps.println("0 " + (height - uy) + " moveto");

					ps.println((lx) + " " + (height - uy) + " moveto");
					// draw filled box
					final int w = ux - lx;
					final int h = uy - ly;
					ps.println(w + " " + h + " fb");
				}
			}

			for (int j = minGene(); j <= maxGene; j++) {
				final int uy = getYmapPixel(j + 0.25);
				final String out = getGeneAnno(j);
				final Color fgColor = getGeneFgColor(j);
				if (out != null) {
					if (fgColor != null) {
						ps.println(convertColor(fgColor) + " sr");
					}
					ps.println("0 " + (height - uy) + " moveto");
					ps.println("( " + psEscape(out) + " ) show");
				}
			}
			ps.println((-xoff) + " " + (-yoff) + " translate");

			// translate back
			// if (includeAtr()) ps.println("0 " + - getAtrHeight() +
			// " translate");
			if (includeGtr()) {
				ps.println(-getGtrWidth() + " 0 translate");
			}
			if (includeArrayMap()) {
				ps.println(-getXmapWidth() + " 0 translate");
			}
		}

		private void writeArrayNames(final PrintStream ps) {
			if (getArrayAnnoLength() <= 0)
				return;
			int tHeight = 0;
			int tWidth = 0;
			if (includeGeneMap()) {
				tHeight += getYmapHeight();
			}
			if (includeAtr() && (getArrayAnnoInside() == false)) {
				tHeight += getAtrHeight();
			}
			if (includeGtr()) {
				tWidth += getGtrWidth();
			}

			ps.println(tWidth + " " + tHeight + " translate");

			final int xoff = -getXmapPixel(minArray() - 0.5);
			final int yoff = 0;
			ps.println("% account for offset into data matrix");
			ps.println(xoff + " " + yoff + " translate");

			ps.println("0 0 0 sr");
			ps.println(" /Courier findfont");
			ps.println("8 scalefont");
			ps.println("setfont");
			ps.println("90 rotate");
			final int max = maxArray();

			for (int j = minArray(); j <= max; j++) {
				final Color bgColor = getArrayBgColor(j);
				if (bgColor != null) {
					final int lx = 0;
					final int ly = getXmapPixel(j - 0.5);
					final int ux = getArrayAnnoLength();
					final int uy = getXmapPixel(j + 0.5);
					ps.println(convertColor(bgColor) + " sr");
					// ps.println("0 " + (-uy) + " moveto");

					ps.println((lx) + " " + (-uy) + " moveto");
					// draw filled box
					final int w = ux - lx;
					final int h = uy - ly;
					ps.println(w + " " + h + " fb");
				}
			}

			for (int i = minArray(); i <= max; i++) {
				final int ux = getXmapPixel(i + 0.25);
				final String out = getArrayAnno(i);
				final Color color = getArrayFgColor(i);
				if (out != null) {
					if (color != null) {
						ps.println(convertColor(color) + " sr");
					}
					ps.println("0 " + (-ux) + " moveto");
					ps.println("( " + psEscape(out) + " ) show");
				}

			}

			ps.println("-90 rotate");

			ps.println((-xoff) + " " + (-yoff) + " translate");
			ps.println(-tWidth + " " + (-tHeight) + " translate");

		}

		private String psEscape(final String inString) {
			final String convicts = "()"; // escape the convicts!!!
			final StringBuffer outString = new StringBuffer(inString.length());
			for (int i = 0; i < inString.length(); i++) {
				final char thisChar = inString.charAt(i);
				if (convicts.indexOf(thisChar) >= 0) {
					outString.append('\\');
				}
				outString.append(thisChar);
			}
			return outString.toString();
		}

		private String convertColor(final Color c) {
			// God Damn java 1.0!!!
			// float comp[] = new float [3];
			// c.getRGBColorComponents(comp);
			return convertRGB(c.getRed()) + " " + convertRGB(c.getGreen())
					+ " " + convertRGB(c.getBlue());
		}

		private float convertRGB(final int r) {
			return ((float) r) / 255;
		}

		private void writeFooter(final PrintStream ps) {
			ps.println("showpage");
		}

		private double scaleGTR, corrGTR;
		private final int offsetGTR = 5;

		private void writeGTR(final PrintStream ps) {
			if (includeGtr() == false)
				return;
			corrGTR = getMinGeneCorr();
			scaleGTR = (getGtrWidth() - offsetGTR) / (1.0 - corrGTR);

			ps.println((offsetGTR / 2) + " 0 translate");

			final int yoff = getYmapPixel(minGene() - 0.5);
			final int xoff = 0;
			ps.println("% account for offset into data matrix");
			ps.println(xoff + " " + yoff + " translate");
			interateGTR(ps, getGeneNode());
			ps.println(convertColor(Color.black) + " sr");
			ps.println((-xoff) + " " + (-yoff) + " translate");

			ps.println((-offsetGTR / 2) + " 0 translate");

		}

		private double scaleATR, corrATR;
		private final int offsetATR = 5;

		private void writeATR(final PrintStream ps) {
			if (includeAtr() == false)
				return;
			corrATR = getMinArrayCorr();
			scaleATR = (getAtrHeight() - offsetATR) / (1.0 - corrATR);
			int widthOffset = 0;
			int heightOffset = 0;
			if (includeGtr()) {
				widthOffset += getGtrWidth();
			}
			if (includeGeneMap()) {
				heightOffset += getYmapHeight();
			}
			if (getArrayAnnoInside()) {
				heightOffset += getArrayAnnoLength();
			}

			ps.println(widthOffset + " " + (heightOffset - offsetATR / 2)
					+ " translate");

			final int xoff = -getXmapPixel(minArray() - 0.5);
			final int yoff = 0;
			ps.println("% account for offset into data matrix");
			ps.println(xoff + " " + yoff + " translate");
			recurseATR(ps, getArrayNode());
			ps.println(convertColor(Color.black) + " sr");

			ps.println((-xoff) + " " + (-yoff) + " translate");

			ps.println((-widthOffset) + " " + (-heightOffset + offsetATR / 2)
					+ " translate");

		}

		private void interateGTR(final PrintStream ps,
				final TreeDrawerNode startNode) {
			final int height = (int) getYmapHeight();
			final Stack remaining = new Stack();
			remaining.push(startNode);
			while (remaining.empty() == false) {
				final TreeDrawerNode node = (TreeDrawerNode) remaining.pop();
				final TreeDrawerNode left = node.getLeft();
				final TreeDrawerNode right = node.getRight();

				final int rx = (int) (scaleGTR * (right.getCorr() - corrGTR));
				final int lx = (int) (scaleGTR * (left.getCorr() - corrGTR));
				final int tx = (int) (scaleGTR * (node.getCorr() - corrGTR));

				final int ry = getYmapPixel(right.getIndex());
				final int ly = getYmapPixel(left.getIndex());
				final Color color = node.getColor();
				// setcolor
				ps.println(convertColor(color) + " sr");

				ps.println(rx + " " + (height - ry) + " " + tx + " "
						+ (height - ly) + " " + lx + " snGTR");

				if (left.isLeaf() == false) {
					remaining.push(left);
				}
				if (right.isLeaf() == false) {
					remaining.push(right);
				}
			}
		}

		private void recurseATR(final PrintStream ps, final TreeDrawerNode node) {
			final int height = (int) getAtrHeight();

			final TreeDrawerNode left = node.getLeft();
			final TreeDrawerNode right = node.getRight();

			final int ry = (int) (scaleATR * (right.getCorr() - corrATR));
			final int ly = (int) (scaleATR * (left.getCorr() - corrATR));
			final int ty = (int) (scaleATR * (node.getCorr() - corrATR));

			final int rx = getXmapPixel(right.getIndex());
			final int lx = getXmapPixel(left.getIndex());
			final Color color = node.getColor();
			// setcolor
			ps.println(convertColor(color) + " sr");
			ps.println((height - ry) + " " + rx + " " + (height - ty) + " "
					+ lx + " " + (height - ly) + " snATR");

			if (left.isLeaf() == false) {
				recurseATR(ps, left);
			}
			if (right.isLeaf() == false) {
				recurseATR(ps, right);
			}
		}

		public void write(final PrintStream ps) {

			// calculateDimensions();
			writeHeader(ps);

			// write gtr?
			if (includeGtr()) {
				writeGTR(ps);
			}
			if (includeAtr()) {
				writeATR(ps);
			}

			writeArrayNames(ps);

			writeGeneNames(ps);

			if (includeData()) {
				writeBoxes(ps);
			}
			writeFooter(ps);

			if (ps.checkError()) {
				LogBuffer
						.println("Some error occured during PostScript export");
			}
		}
	}
}
