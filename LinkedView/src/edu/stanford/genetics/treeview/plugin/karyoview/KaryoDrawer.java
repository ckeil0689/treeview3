/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: KaryoDrawer.java,v $i
 * $Revision: 1.2 $
 * $Date: 2007-02-03 07:28:14 $
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
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview.plugin.karyoview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Observable;
import java.util.Observer;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DummyConfigNode;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeSelectionI;

/**
 * This is observable because it signals changes to the drawer it is an observer
 * of the selection.
 */

public class KaryoDrawer extends Observable implements Observer {
	/*
	 * begin state variables and accessors
	 */
	/**
	 * The genome to render
	 */
	private Genome genome;

	/** Setter for genome */
	public void setGenome(final Genome genome) {
		this.genome = genome;
		setChanged();
	}

	/** Getter for genome */
	public Genome getGenome() {
		return genome;
	}

	/**
	 * The colors to render with.
	 */
	private final KaryoColorSet karyoColorSet = new KaryoColorSet(
			"KaryoDrawerColorSet");

	/** Getter for karyoColorSet */
	public KaryoColorSet getKaryoColorSet() {
		return karyoColorSet;
	}

	/**
	 * The selection model to indicate.
	 */
	private TreeSelectionI geneSelection;

	/** Setter for geneSelection */
	public void setGeneSelection(final TreeSelectionI geneSelection) {
		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}
		setChanged();
		this.geneSelection = geneSelection;
		if (this.geneSelection != null) {
			this.geneSelection.addObserver(this);
		}
	}

	/** Getter for geneSelection */
	public TreeSelectionI getGeneSelection() {
		return geneSelection;
	}

	/**
	 * value to skip and not render, since the loci doesn't know it's own
	 * expression.
	 */
	private double nodata;

	/** Setter for nodata */
	public void setNodata(final double nodata) {
		this.nodata = nodata;
	}

	/** Getter for nodata */
	public double getNodata() {
		return nodata;
	}

	/**
	 * height and width are the actual width and height of the target area to
	 * draw to.
	 */
	private int height = 0;

	/** Setter for height */
	public void setHeight(final int height) {
		if (this.height == height)
			return;
		this.height = height;
		setChanged();
	}

	/** Getter for height */
	public int getHeight() {
		return height;
	}

	/**
	 * height and width are the actual width and height of the target area to
	 * draw to.
	 */
	private int width = 0;

	/** Setter for width */
	public void setWidth(final int width) {
		if (this.width == width)
			return;
		this.width = width;
		setChanged();
	}

	/** Getter for width */
	public int getWidth() {
		return width;
	}

	/**
	 * pixelPerMap is horizontal pixels per arbitrary unit along chromosome
	 */
	private double pixelPerMap;

	/** Setter for pixelPerMap */
	public void setPixelPerMap(final double pixelPerMap) {
		this.pixelPerMap = pixelPerMap;
		setChanged();
	}

	/** Getter for pixelPerMap */
	public double getPixelPerMap() {
		return pixelPerMap;
	}

	/**
	 * pixelPerVal is vertical pixels per expression unit.
	 */
	private double pixelPerVal;

	/** Setter for pixelPerVal */
	public void setPixelPerVal(final double pixelPerVal) {
		this.pixelPerVal = pixelPerVal;
		// System.out.println("setting ppv to " + pixelPerVal + " stack:" );
		// Exception e = new Exception(); e.printStackTrace();
		setChanged();
	}

	/** Getter for pixelPerVal */
	public double getPixelPerVal() {
		return pixelPerVal;
	}

	private final int defaultIconSize = 2;

	/** Setter for iconSize */
	public void setIconSize(final int iconSize) {
		configNode.setAttribute("iconSize", iconSize, defaultIconSize);
		// System.out.println("iconsize changed to " + iconSize);
		setChanged();

	}

	/** Getter for iconSize */
	public int getIconSize() {
		// System.out.println("iconsize gotten " + iconSize);
		return configNode.getAttribute("iconSize", defaultIconSize);
	}

	private final int[] iconSizes = new int[] { 1, 3, 5, 7 };

	/**
	 * Setter for iconSizes public void setIconSizes(int [] iconSizes) {
	 * this.iconSizes = iconSizes; }
	 */
	/** Getter for iconSizes */
	public int[] getIconSizes() {
		return iconSizes;
	}

	private final int defaultIconType = 2;

	/** Setter for iconType */
	public void setIconType(final int iconType) {
		configNode.setAttribute("iconType", iconType, defaultIconType);
		setChanged();
	}

	/** Getter for iconType */
	public int getIconType() {
		return configNode.getAttribute("iconType", defaultIconType);
	}

	private String[] iconTypes = new String[] { "None", "Circle", "Disc" };

	/** Setter for iconTypes */
	public void setIconTypes(final String[] iconTypes) {
		this.iconTypes = iconTypes;
	}

	/** Getter for iconTypes */
	public String[] getIconTypes() {
		return iconTypes;
	}

	/* XXX stuff I should support at some point ... */
	/**
	 * base of log for scale lines...
	 */
	private final double defaultLinesBase = 2.0;

	/** Setter for linesBase */
	public void setLinesBase(final double linesBase) {
		if (getLinesBase() == linesBase)
			return;
		configNode.setAttribute("linesBase", linesBase, defaultLinesBase);
		setChanged();
	}

	/** Getter for linesBase */
	public double getLinesBase() {
		return configNode.getAttribute("linesBase", defaultLinesBase);
	}

	/**
	 * include scale lines above?
	 */
	private final int defaultLinesAbove = 0;

	/** Setter for linesAbove */
	public void setLinesAbove(final boolean linesAbove) {
		if (getLinesAbove() == linesAbove)
			return;
		final int val = (linesAbove == true) ? 1 : 0;
		configNode.setAttribute("linesAbove", val, defaultLinesAbove);
		setChanged();
	}

	/** Getter for linesAbove */
	public boolean getLinesAbove() {
		return (configNode.getAttribute("linesAbove", defaultLinesAbove) == 1);
	}

	private final int defaultLinesMax = 5;

	/** Setter for linesMax */
	public void setLinesMax(final int linesMax) {
		if (getLinesMax() == linesMax)
			return;
		configNode.setAttribute("linesMax", linesMax, defaultLinesMax);
		setChanged();
	}

	/** Getter for linesMax */
	public int getLinesMax() {
		return configNode.getAttribute("linesMax", defaultLinesMax);
	}

	/**
	 * include scale lines below?
	 */
	private final int defaultLinesBelow = 0;

	/** Setter for linesBelow */
	public void setLinesBelow(final boolean linesBelow) {
		if (linesBelow == getLinesBelow())
			return;
		final int val = (linesBelow == true) ? 1 : 0;
		configNode.setAttribute("linesBelow", val, defaultLinesBelow);
		setChanged();
	}

	/** Getter for linesBelow */
	public boolean getLinesBelow() {
		return (configNode.getAttribute("linesBelow", defaultLinesBelow) == 1);
	}

	/**
	 * include bar chart?
	 */
	private final int defaultDrawBars = 1;

	public boolean getBarChart() {
		return (configNode.getAttribute("drawBars", defaultDrawBars) == 1);
	}

	public void setBarChart(final boolean drawBars) {
		final int val = (drawBars == true) ? 1 : 0;
		configNode.setAttribute("drawBars", val, defaultDrawBars);
		setChanged();
	}

	/**
	 * include line chart?
	 */
	private final int defaultDrawLines = 0;

	public boolean getLineChart() {
		return (configNode.getAttribute("drawLines", defaultDrawLines) == 1);
	}

	public void setLineChart(final boolean drawLines) {
		final int val = (drawLines == true) ? 1 : 0;
		configNode.setAttribute("drawLines", val, defaultDrawLines);
		setChanged();
	}

	/* end stuff I should support at some point ... */

	/*
	 * end state variables and accessors
	 */

	/* Some private state variables... */
	private double[] mapValues;

	public void setMapValue(final int i, final double val) {
		if (i < 0)
			return;
		setChanged();
		mapValues[i] = val;
	}

	public void setMapValue(final ChromosomeLocus locus, final double val) {
		setMapValue(locus.getCdtIndex(), val);
	}

	public double getMapValue(final int i) {
		return mapValues[i];
	}

	/**
	 * removes any pointers to exteral objects to aid GC
	 */
	public void cleanup() {
		geneSelection = null;
		mapValues = null;
	}

	@Override
	public void update(final Observable o, final Object arg) {
		if (o == geneSelection) {
			setChanged();
			notifyObservers();
		} else {
			LogBuffer.println("KaryoView got weird update from " + o);
		}
	}

	public KaryoDrawer(final Genome genome, final TreeSelectionI selection,
			final double nodata) {
		super();
		// set all locals to default values....
		mapValues = new double[selection.getNumIndexes()];
		setGenome(genome);
		setGeneSelection(selection);
		setNodata(nodata);
		setHeight(100);
		setWidth(100);
		autoScale(); // sets pixelPerMap, pixelPerVal...
	}

	/**
	 * Adjusts pixels per map, pixels per value so that the karyoview will fit
	 * nicely in the current width/height combination.
	 */
	public void autoScale() {
		final Genome loci = getGenome();
		final int nChr = loci.getMaxChromosome();
		if (nChr == 0)
			return;
		final Dimension d = new Dimension(width, height / nChr);
		// System.out.println("Each chromosome gets the follow dimension: " +
		// d);

		int minNonempty = -1;
		for (int i = 1; i <= nChr; i++) {
			if (loci.getChromosome(i).isEmpty() == false) {
				if (hasData(loci.getChromosome(i))) {
					minNonempty = i;
					break;
				}
			}
		}
		if (minNonempty == -1) {
			// System.out.println("using min ppv of 1");
			setPixelPerMap(1);
			setPixelPerVal(1);
		} else {
			double minPpm = getOptPpm(loci.getChromosome(minNonempty), d);
			double minPpv = getOptPpv(loci.getChromosome(minNonempty), d);
			for (int i = minNonempty + 1; i <= nChr; i++) {
				// System.out.println("testing " + i + " best so far is ppm " +
				// minPpm + ", ppv " + minPpv);
				final double ppm = getOptPpm(loci.getChromosome(i), d);
				final double ppv = getOptPpv(loci.getChromosome(i), d);
				if ((ppv != 0) && (ppm != 0)) {
					if (ppm < minPpm)
						minPpm = ppm;
					if (ppv < minPpv)
						minPpv = ppv;
				}
			}
			// System.out.println("using min ppv of " + minPpv);
			setPixelPerMap(minPpm);
			setPixelPerVal(minPpv);
		}
	}

	private final int border = 5;
	private final int yborder = 0;

	/**
	 * determine the optimal pixels per map for drawing this chromosome on the
	 * specified dimension
	 */
	private double getOptPpv(final Chromosome chr, final Dimension d) {
		double opt = 0.0;
		if (chr.getMaxPosition(ChromosomeLocus.CIRCULAR) == 0.0) {
			// linear chromosome...
			final double maxVal = getMaxAbsVal(chr.getLeftEnd());
			if (maxVal != 0.0) {
				opt = (d.height - 2 * yborder) / (1.5 * maxVal);
			}
			/*
			 * double sum = getSumAbsVal(chr.getLeftEnd()); int count =
			 * getCount(chr.getLeftEnd()); if (count != 0) { double maxVal = sum
			 * / (double)count * (double) 4; opt = (d.height - 2 * yborder) / (2
			 * * maxVal); }
			 */
		}
		return opt;
	}

	/**
	 * determine the optimal pixels per map for drawing this chromosome on the
	 * specified dimension
	 */
	private boolean hasData(final Chromosome chr) {
		if (chr.getMaxPosition(ChromosomeLocus.CIRCULAR) == 0.0) {
			// linear chromosome...
			final ChromosomeLocus start = chr.getLeftEnd();
			ChromosomeLocus locus = start;
			do {
				final int cdtIndex = locus.getCdtIndex();
				if (cdtIndex >= 0) {
					final double abs = mapValues[cdtIndex];
					if (abs == nodata) {
						// ignore nodata...
					} else {
						return true;
					}
				}
				locus = locus.getRight();
			} while ((locus != start) && (locus != null));
		}
		return false;
	}

	/**
	 * determine the optimal pixels per val for drawing this chromosome on the
	 * specified dimension
	 */
	private double getOptPpm(final Chromosome chr, final Dimension d) {
		double opt = 0.0;
		if (chr.getMaxPosition(ChromosomeLocus.CIRCULAR) == 0.0) {
			// linear chromosome...
			final double maxPos = chr.getMaxPosition();
			opt = (d.width - 2 * border) / (2 * maxPos);
		}
		return opt;
	}

	/**
	 * This function traverses the chromosomes, starting at start and calling
	 * getRight() until it reaches end. It keeps track of and returns the
	 * maximum abs value it finds.
	 */
	private double getMaxAbsVal(final ChromosomeLocus start) {
		double maxVal = 0.0;
		if (start == null)
			return maxVal;
		ChromosomeLocus locus = start;
		do {
			final int cdtIndex = locus.getCdtIndex();
			if (cdtIndex >= 0) {
				double abs = mapValues[cdtIndex];
				if (abs == nodata) {
					// ignore nodata...
				} else {
					abs = Math.abs(abs);
					if (abs > maxVal)
						maxVal = abs;
				}
			}
			locus = locus.getRight();
		} while ((locus != start) && (locus != null));

		return maxVal;
	}

	/**
	 * This function traverses the chromosomes, starting at start and calling
	 * getRight() until it reaches end. It keeps track of and returns the
	 * running total abs of the values it finds.
	 */
	/*
	 * private double getSumAbsVal(ChromosomeLocus start) { double sum = 0.0; if
	 * (start == null) return sum; ChromosomeLocus locus = start; do { int
	 * cdtIndex = locus.getCdtIndex(); if (cdtIndex >= 0) { double abs =
	 * mapValues[cdtIndex]; if (abs == nodata) { // ignore nodata... } else {
	 * abs = Math.abs(abs); sum += abs; } } locus = locus.getRight(); } while
	 * ((locus != start) && (locus != null));
	 * 
	 * return sum; }
	 */

	/**
	 * This function traverses the chromosomes, starting at start and calling
	 * getRight() until it reaches end. It keeps track of and returns the
	 * running total non-missing loci it finds.
	 */
	/*
	 * private int getCount(ChromosomeLocus start) { int count = 0; if (start ==
	 * null) return count; ChromosomeLocus locus = start; do { int cdtIndex =
	 * locus.getCdtIndex(); if (cdtIndex >= 0) { double abs =
	 * mapValues[cdtIndex]; if (abs == nodata) { // ignore nodata... } else {
	 * count++; } } locus = locus.getRight(); } while ((locus != start) &&
	 * (locus != null));
	 * 
	 * return count; }
	 */

	/**
	 * This not-so-object-oriented code is necessary since I don't want the
	 * Chromosome to know about graphics. I could define a ChromosomeDrawer
	 * class with appropriate subclasses, and have to manage those, but I'm lazy
	 * about it.
	 * 
	 * @return the position of the locus in pixels, if the centromere is at
	 *         xcenter.
	 */
	private int getLinearPosition(final ChromosomeLocus locus, final int xcenter) {
		if (locus == null)
			return 0;
		final int arm = locus.getArm();
		final double pos = locus.getPosition();
		int x;
		if (arm == ChromosomeLocus.LEFT) {
			x = xcenter - (int) (pos * getPixelPerMap());
		} else {
			x = xcenter + (int) (pos * getPixelPerMap());
		}
		return x;
	}

	/**
	 * @return the distance of the farthest end from the centromere in pixels
	 */
	public int getFarthestEndDistance(final Chromosome chr) {
		int max = 0;
		if (chr.getMaxPosition(ChromosomeLocus.CIRCULAR) == 0.0) {
			// linear chromosome...
			final double maxPos = chr.getMaxPosition();
			max = (int) (maxPos * getPixelPerMap());
		}
		return max;
	}

	public void paintChromosome(final Graphics g,
			final LinearChromosome linear, final Rectangle destination) {
		/*
		 * don't do this! it prevents the important case of lines overlapping...
		 * g.setColor(getKaryoColorSet().getColor("Background"));
		 * g.fillRect(destination
		 * .x,destination.y,destination.width,destination.height);
		 */
		final int xcenter = destination.width / 2 + destination.x;
		final int ycenter = destination.height / 2 + destination.y;
		final ChromosomeLocus leftMost = linear.getLeftEnd();
		final ChromosomeLocus rightMost = linear.getRightEnd();

		final int leftEnd = getLinearPosition(leftMost, xcenter);
		final int rightEnd = getLinearPosition(rightMost, xcenter);

		// actual line representing chromosome...
		g.setColor(getKaryoColorSet().getColor("Genome"));
		g.drawLine(leftEnd, ycenter, rightEnd, ycenter);

		if (getLinesAbove() || getLinesBelow()) {
			int i = 2; // log(0) is undefined, log(1) is always zero.
			final int maxInt = i + getLinesMax();
			final double base = getLinesBase();
			final double logE = Math.log(base);
			final double ppv = getPixelPerVal();
			double thisval = Math.log(i++) / logE;
			int thisy = (int) (ppv * thisval);
			int lasty = 0;
			g.setColor(getKaryoColorSet().getColor("Line"));
			// while (thisy < destination.height / 2) {
			while (i <= maxInt && ((lasty + 1) < thisy)) {
				if (getLinesAbove())
					g.drawLine(leftEnd, ycenter - thisy, rightEnd, ycenter
							- thisy);
				if (getLinesBelow())
					g.drawLine(leftEnd, ycenter + thisy, rightEnd, ycenter
							+ thisy);
				lasty = thisy;
				thisval = Math.log(i++) / logE;
				thisy = (int) (ppv * thisval);
			}
		}

		// could use clipRect to speed up...
		// int minX = clipRect.x;
		// int maxX = minX + clipRect.width;

		ChromosomeLocus locus = linear.getLeftEnd();
		int lastX = 0;
		int lastY = 0;
		ChromosomeLocus lastLocus = null;
		while (locus != null) {
			final int cdtIndex = locus.getCdtIndex();
			if (cdtIndex >= 0) {
				final int x = getLinearPosition(locus, xcenter);

				// XXX need to do a better job here...
				if ((geneSelection != null)
						&& geneSelection.isIndexSelected(cdtIndex)) {
					g.setColor(getKaryoColorSet().getColor("Highlight"));
					final int diameter = iconSizes[getIconSize()];
					final int radius = (diameter - 1) / 2;
					switch (getIconType()) {
					case 0:
						break;
					case 1:
						g.fillOval(x - radius, ycenter - radius, diameter,
								diameter);
						break;
					case 2:
						g.drawOval(x - radius, ycenter - radius, diameter,
								diameter);
						break;
					}
				}

				// could use clipRect to speed up...
				// if (x < minX) continue;
				// if (x > maxX) continue;

				final double val = mapValues[cdtIndex];
				int yend = ycenter;
				if (val != nodata) {
					yend -= (int) (val * getPixelPerVal());
					// System.out.println("chr " + locus.getChromosome() +
					// " arm " + arm + " pos " + pos +" val " + val + " length "
					// + val * getPixelPerVal());
					if (getBarChart()) {
						// need to abstract to ColorConverter...
						if (val > 0) {
							g.setColor(getKaryoColorSet().getColor("Up"));
						} else {
							g.setColor(getKaryoColorSet().getColor("Down"));
						}
						g.drawLine(x, ycenter, x, yend);
					}
					if (getLineChart()) {
						if (lastLocus != null) {
							g.setColor(getKaryoColorSet().getColor("Line"));
							g.drawLine(lastX, lastY, x, yend);
						}
						lastLocus = locus;
						lastX = x;
						lastY = yend;

					}
				}
			}
			locus = locus.getRight();
		}

		g.setColor(Color.blue);
		g.drawOval(xcenter - 3, ycenter - 3, 5, 5);
	}

	/**
	 * This gets the starting pixel for chromosome chr, where chr is a number
	 * from 0 to nchr-1. If chr == nchr, will return the max pixel
	 */
	public int getStartingY(final int chr) {
		final int nChromosomes = genome.getMaxChromosome();
		if (chr == nChromosomes) {
			return getHeight();
		}
		if (nChromosomes == 0) {
			return 0;
		}
		return (chr * getHeight()) / nChromosomes;
	}

	public int minVisibleChromosome(final Rectangle clipRect) {
		int minChr = 0;
		final int nChromosomes = genome.getMaxChromosome();
		while (getStartingY(minChr) < clipRect.y) {
			minChr++;
			if (minChr == nChromosomes)
				break;
		}
		if (minChr < 2)
			return 1;
		return minChr;
	}

	public int maxVisibleChromosome(final Rectangle clipRect) {
		int maxChr = 1;
		final int nChromosomes = genome.getMaxChromosome();
		while (getStartingY(maxChr) < clipRect.y + clipRect.height) {
			maxChr++;
			if (maxChr == nChromosomes)
				break;
		}
		if (maxChr > nChromosomes)
			maxChr = nChromosomes;
		return maxChr;
	}

	public void paintBackground(final Graphics g, final Rectangle clipRect) {
		if (clipRect != null) {
			g.setColor(getKaryoColorSet().getColor("Background"));
			g.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
		}
	}

	public void paint(final Graphics g, final Rectangle clipRect) {

		// background...
		paintBackground(g, clipRect);
		// System.out.println("drawing to clip " + clipRect);
		final int nChromosomes = genome.getMaxChromosome();
		if (nChromosomes == 0) {
			return;
		}
		// use clipRect to find the min and max visible chromosomes...
		int minChr = 0;
		while (getStartingY(minChr) < clipRect.y) {
			minChr++;
			if (minChr == nChromosomes)
				break;
		}
		int maxChr = minChr;
		while (getStartingY(maxChr) < clipRect.y + clipRect.height) {
			maxChr++;
			if (maxChr == nChromosomes)
				break;
		}
		if (minChr != 0)
			minChr--;
		maxChr++;
		if (maxChr > nChromosomes)
			maxChr = nChromosomes;
		final Rectangle dest = new Rectangle();
		for (int chr = minChr; chr < maxChr; chr++) {
			dest.x = 0;
			dest.y = getStartingY(chr);
			dest.width = getWidth();
			dest.height = getStartingY(chr + 1) - dest.y;
			paintChromosome(g, genome.getChromosome(chr + 1), dest);
		}
	}

	public void paintChromosome(final Graphics g, final Chromosome c,
			final Rectangle dest) {
		if (c.getType() == Chromosome.LINEAR) {
			paintChromosome(g, (LinearChromosome) c, dest);
		}
	}

	public ChromosomeLocus getClosest(final Point pos) {
		// figure out which chromosome...
		final int nChromosomes = genome.getMaxChromosome();
		for (int chr = 1; chr <= nChromosomes; chr++) {
			if (pos.y < getStartingY(chr)) {
				// getStartingY() actually gets start of chr+1, i.e.
				// getStartingY(1) is the start of chr 2.
				// so, if pos.y < getStartingY(1), it's on chromosome 1.
				return getClosest(genome.getChromosome(chr), pos);
			}
		}
		return null;
	}

	public ChromosomeLocus getClosest(final Chromosome chromosome,
			final Point pos) {
		if (chromosome == null)
			return null;
		final ChromosomeLocus start = chromosome.getLeftEnd();
		ChromosomeLocus closest = start;
		ChromosomeLocus current = closest;
		double dist = 100000 * getDistance(closest, pos);
		if (current == null)
			return null;
		do {
			final int cdtIndex = current.getCdtIndex();
			if (cdtIndex >= 0) {
				final double tdist = getDistance(current, pos);
				// System.out.println("got dist " + tdist + "for " + current +
				// " to " + pos);
				if (tdist < dist) {
					dist = tdist;
					closest = current;
				}
			}
			current = current.getRight();
		} while ((current != null) && (current != start));
		return closest;
	}

	private double getDistance(final ChromosomeLocus locus, final Point pos) {

		// Point end = getEnd(locus);
		final Point end = getBase(locus);

		if (end == null) {
			return 10.0;
		} else {
			return (square(end.x - pos.x) + square(end.y - pos.y));
		}
	}

	private double square(final double in) {
		return in * in;
	}

	/**
	 * returns the pixel at the base of the given locus.
	 */
	public Point getBase(final ChromosomeLocus locus) {
		if ((genome == null) || (locus == null)) {
			return null;
		}
		final int type = genome.getChromosome(locus.getChromosome()).getType();
		final int ycenter = (getStartingY(locus.getChromosome() - 1) + getStartingY(locus
				.getChromosome())) / 2;
		final int xcenter = getWidth() / 2;
		int x = xcenter;
		final int yend = ycenter;
		if (type == Chromosome.LINEAR) {
			final int arm = locus.getArm();
			final double pos = locus.getPosition();
			if (arm == ChromosomeLocus.LEFT) {
				x = xcenter - (int) (pos * getPixelPerMap());
			} else {
				x = xcenter + (int) (pos * getPixelPerMap());
			}
		}
		return new Point(x, yend);
	}

	public Point getEnd(final ChromosomeLocus locus) {
		final int type = genome.getChromosome(locus.getChromosome()).getType();
		if (type == Chromosome.LINEAR) {
			final Point ret = getBase(locus);
			final int cdtIndex = locus.getCdtIndex();
			if (cdtIndex >= 0) {
				final double val = mapValues[cdtIndex];
				if (val != nodata) {
					ret.y -= (int) (val * getPixelPerVal());
				}
			}
			return ret;
		}
		return new Point(0, 0);
	}

	private ConfigNode configNode = new DummyConfigNode("KaryoDrawer");

	/** Setter for configNode */
	public void bindConfig(final ConfigNode configNode) {
		this.configNode = configNode;
		getKaryoColorSet().bindConfig(getFirst("KaryoColorSet"));
	}

	/** Getter for configNode */
	public ConfigNode getConfigNode() {
		return configNode;
	}

	/**
	 * always returns an instance of the node, even if it has to create it.
	 */
	private ConfigNode getFirst(final String name) {
		final ConfigNode cand = getConfigNode().fetchFirst(name);
		return (cand == null) ? getConfigNode().create(name) : cand;
	}

}
