package ColorChooser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import Utilities.Helper;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;

public class ColorPicker {

	/* Adjust this to MigLayout variables of mainPanel! */
	protected static final int WIDTH = 510;
	protected static final int OFFSET = 70;
	protected static final int THUMB_HEIGHT = 40;

	private final JPanel containerPanel;

	/* The different components which make up the ColorPicker */
	private GradientBox gradientBox;
	private NumBox numBox;
	private ThumbBox thumbBox;
	private RulerBox rulerBox;
	private BoundaryBox minBox;
	private BoundaryBox maxBox;

	/* The currently active set of colors */
	private ColorSet activeColorSet;

	/* Responsible for active data-to-color mapping */
	private final ColorExtractor colorExtractor;

	/* >>>>> Important shared state variables <<<<<< */
	/* Fractions for LinearGradientPaint (depend on thumb positions) */
	private float[] fractions;

	/* Inflexible array of colors for LinearGradientPaint */
	private Color[] colors;

	/* List of all active thumbs (one per color) */
	protected Thumb minThumb;
	protected Thumb maxThumb;
	private List<Thumb> thumbList;

	/* List of all active colors (depends on active ColorSet) */
	private final List<Color> colorList;

	/* Data boundaries */
	private final double mean;
	private final double median;
	
	private double minVal;
	private double maxVal;
	private double range;

	public ColorPicker(final ColorExtractor drawer, final double minVal,
			final double maxVal, final double mean, final double median) {

		this.colorExtractor = drawer;
		
		this.mean = mean;
		this.median = median;
		
		this.thumbList = new ArrayList<Thumb>();
		this.colorList = new ArrayList<Color>();

		this.minThumb = new BoundaryThumb(true);
		this.maxThumb = new BoundaryThumb(false);

		this.containerPanel = new ContainerPanel();

		this.gradientBox = new GradientBox(this);
		this.numBox = new NumBox(this);
		this.thumbBox = new ThumbBox(this);
		this.rulerBox = new RulerBox();
		this.minBox = new BoundaryBox(this, minThumb, true);
		this.maxBox = new BoundaryBox(this, maxThumb, false);

		setMinVal(minVal);
		setMaxVal(maxVal);
		updateRange();
	}

	public JPanel getContainerPanel() {

		return containerPanel;
	}

	private class ContainerPanel extends JPanel {

		/**
		 * Default serial version ID to keep Eclipse happy...
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void paintComponent(final Graphics g) {

			super.paintComponent(g);

			final Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			positionRects();

			minBox.drawBoundaryBox(g2);
			numBox.drawNumBox(g2);
			thumbBox.drawThumbBox(g2);
			gradientBox.drawGradientBox(g2);
			rulerBox.drawRulerBox(g2);
			maxBox.drawBoundaryBox(g2);
		}

		/**
		 * Gives the boxes which make up ColorPicker a start x-position and a
		 * size which is based on the size on a set size for the ColorPicker as
		 * a whole.
		 * 
		 * @param width
		 */
		private void positionRects() {

			final int start = OFFSET;

			minBox.setRect(start - 80, 40, 80, 100);
			numBox.setRect(start, 0, ColorPicker.WIDTH, 40);
			thumbBox.setRect(start, 40, ColorPicker.WIDTH, 40);
			gradientBox.setRect(start, 40, ColorPicker.WIDTH, 100);
			rulerBox.setRect(start, 140, ColorPicker.WIDTH, 10);
			maxBox.setRect(start + ColorPicker.WIDTH, 40, 80, 100);
		}
	}

	/**
	 * Loads the values from a given ColorSet into colorList and fractionList.
	 *
	 * @param colorSet
	 */
	protected void loadPresets() {

		/* clearing all data */
		colorList.clear();
		thumbList.clear();

		thumbList.add(minThumb);
		thumbList.add(maxThumb);

		colorExtractor.setMissingColor(activeColorSet.getMissing());

		/* Only load non-dataset min/max if custom colorset is loaded */
		if (!("RedGreen".equalsIgnoreCase(activeColorSet.getName()) || "YellowBlue"
				.equalsIgnoreCase(activeColorSet.getName()))) {
			setMinVal(activeColorSet.getMin());
			setMaxVal(activeColorSet.getMax());
		}

		final String[] colors = activeColorSet.getColors();

		for (final String color : colors) {

			colorList.add(Color.decode(color));
		}

		setFractions(activeColorSet.getFractions());
		updateColors();
	}

	/**
	 * Serves to store the currently chosen custom color setup in a configNode.
	 * 
	 * @return The new ColorSet object created from the current custom colors
	 *         and the relative positions of the associated thumbs.
	 */
	protected ColorSet saveCustomPresets() {

		final List<Double> fractionList = new ArrayList<Double>();

		for (final float f : fractions) {

			fractionList.add((double) f);
		}

		final String name = "Custom";
		String missing = Integer.toHexString(colorExtractor.getMissing()
				.getRGB());
		missing = "#" + missing.substring(2, missing.length());
		String empty = Integer.toHexString(colorExtractor.getEmpty().getRGB());
		empty = "#" + empty.substring(2, empty.length());

		return new ColorSet(name, colorList, fractionList, minVal, maxVal,
				missing, empty);
	}

	/**
	 * Sets the color in colors[] at the specified index.
	 *
	 * @param newCol
	 * @param index
	 */
	protected void setGradientColors() {

		colorExtractor.setNewParams(fractions, colorList);
		colorExtractor.notifyObservers();
	}

	/**
	 * Updates the missing color object in active ColorExtractor.
	 */
	protected void setMissing(final Color color) {

		colorExtractor.setMissingColor(color);
		colorExtractor.notifyObservers();
	}

	protected void updateColors() {

		updateBoundaryColors();
		updateColorArray();
		
//		setGradientColors();
		
		containerPanel.repaint();
	}

	protected void updateBoundaryColors() {

		minThumb.setColor(colorList.get(0));
		maxThumb.setColor(colorList.get(getColorNumber() - 1));
	}

	/**
	 * Adjusts the fractions to new data values.
	 * 
	 * @param dataValues
	 */
	protected void updateFractions() {

		float[] newFractions = new float[thumbList.size()];

		for (int i = 0; i < newFractions.length; i++) {

			double dataVal = thumbList.get(i).getDataValue();
			double diff = Math.abs(dataVal - minVal);
			final float frac;

			if (i == 0) {
				frac = 0.0f;

			} else if (i == newFractions.length - 1) {
				frac = 1.0f;

			} else {
				frac = (float) (diff / (range));
			}

			newFractions[i] = frac;
		}

		setFractions(newFractions);
	}

	/**
	 * Updates the state of the fractions. Also updates the thumb positions as a
	 * result.
	 * 
	 * @param newFracs
	 *            The new set of fractions.
	 */
	protected void setFractions(float[] newFracs) {

		this.fractions = newFracs;

		thumbBox.verifyThumbs();
		thumbBox.adjustThumbsToFractions();
	}

	/*
	 * Updates the color array when needed so LinearGradientPaint can use it to
	 * generate the gradientRect.
	 */
	protected void updateColorArray() {

		/* TODO simplify */
		colors = new Color[colorList.size()];

		for (int i = 0; i < colors.length; i++) {
			colors[i] = colorList.get(i);
		}
	}

	/**
	 * Sets the activeColors key in configNode. This represents the currently
	 * selected ColorSet choice by the user.
	 *
	 * @param name
	 *            ConfigNode name of the active ColorSet
	 */
	public void setActiveColorSet(final ColorSet set) {

		this.activeColorSet = set;
	}

	/**
	 * Swaps positions of thumbs and colors in their specific lists.
	 * 
	 * @param oldIndex
	 *            Previous position of color/ thumb in their respective lists.
	 * @param newIndex
	 *            New position of color/ thumb in their respective lists.
	 */
	protected void swapPositions(int oldIndex, int newIndex) {

		Collections.swap(thumbList, oldIndex, newIndex);
		Collections.swap(colorList, oldIndex, newIndex);
	}

	/**
	 * Defines a new minimum value for the color range.
	 * 
	 * @param minVal
	 */
	protected void setMinVal(double minVal) {

		this.minVal = minVal;
		colorExtractor.setMin(minVal);
		minThumb.setDataValue(minVal);
		updateRange();

		updateFractions();
	}

	/**
	 * Defines a new maximum value for the color range.
	 * 
	 * @param maxVal
	 */
	protected void setMaxVal(double maxVal) {

		this.maxVal = maxVal;
		colorExtractor.setMax(maxVal);
		maxThumb.setDataValue(maxVal);
		updateRange();

		updateFractions();
	}

	/**
	 * Updates the range.
	 */
	private void updateRange() {

		this.range = maxVal - minVal;
	}

	/**
	 * 
	 * @return The currently defined minimum data value.
	 */
	protected double getMinVal() {

		return minVal;
	}

	/**
	 * 
	 * @return The currently defined maximum data value.
	 */
	protected double getMaxVal() {

		return maxVal;
	}
	
	/**
	 * 
	 * @return The data's mean.
	 */
	protected double getMean() {

		return mean;
	}
	
	/**
	 * 
	 * @return The data's median.
	 */
	protected double getMedian() {

		return median;
	}

	/**
	 * 
	 * @return Returns the currently defined range of the dataset (maxVal -
	 *         minVal).
	 */
	protected double getRange() {

		double testRange = maxVal - minVal;

		/* make sure the range is always defined correctly */
		if (!Helper.nearlyEqual(range, testRange)) {
			LogBuffer.println("Range was not defined properly!");
			this.range = testRange;
		}

		return range;
	}

	/**
	 * Turns a fraction into a data value.
	 * 
	 * @param frac
	 * @return
	 */
	protected double getDataFromFraction(float frac) {

		double dataVal;

		dataVal = Math.abs((range) * frac) + minVal;
		dataVal = (double) Math.round(dataVal * 1000) / 1000;

		return dataVal;
	}

	/**
	 * Returns the Color set for missing data.
	 *
	 * @return Color
	 */
	protected Color getMissing() {

		return colorExtractor.getMissing();
	}

	protected List<Thumb> getThumbList() {

		return thumbList;
	}

	protected void setThumbList(List<Thumb> thumbs) {

		this.thumbList = thumbs;
	}

	protected ThumbBox getThumbBox() {

		return thumbBox;
	}

	protected GradientBox getGradientBox() {

		return gradientBox;
	}

	/**
	 * 
	 * @return The number of thumbs in the thumbList.
	 */
	protected int getThumbNumber() {

		return thumbList.size();
	}

	protected Thumb getThumb(int index) {

		return thumbList.get(index);
	}

	protected List<Color> getColorList() {

		return colorList;
	}

	protected int getColorNumber() {

		return colorList.size();
	}

	protected float[] getFractions() {

		return fractions;
	}

	protected Color[] getColors() {

		return colors;
	}

	/**
	 * The color, fraction, and thumb list should always be the same size. This
	 * method gives information whether this condition is currently fulfilled.
	 * 
	 * @return boolean
	 */
	protected boolean isSynced() {

		return (fractions.length == colorList.size())
				&& (colorList.size() == thumbList.size());
	}
}
