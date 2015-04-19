package ColorChooser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;

public class ColorPicker {

	/* Adjust this to MigLayout variables of mainPanel! */
	protected static final int WIDTH = 450;
	
	private final JPanel containerPanel;
	
	/* The different components which make up the ColorPicker */
	private GradientBox gradientBox;
	private ThumbBox thumbBox;
	private InfoBox infoBox;
	
	/* The currently active set of colors */
	private ColorSet activeColorSet;
	
	/* Responsible for active data-to-color mapping */
	private final ColorExtractor colorExtractor;
	
	/* >>>>> Important shared state variables <<<<<< */
	/* Fractions for LinearGradientPaint (depend on thumb positions) */
	private float[] fractions;
	
	/* List of all active thumbs (one per color) */
	private List<Thumb> thumbList;
	
	/* List of all active colors (depends on active ColorSet) */
	private final List<Color> colorList;
	
	/* Inflexible array of colors for LinearGradientPaint */
	private Color[] colors;
	
	/* Data boundaries */
	private final double minVal;
	private final double maxVal;
	private double range;
	
	public ColorPicker(final ColorExtractor drawer, final double minVal,
			final double maxVal) {
		
		this.colorExtractor = drawer;
		this.thumbList = new ArrayList<Thumb>();
		this.colorList = new ArrayList<Color>();
		
		this.gradientBox = new GradientBox(this);
		this.thumbBox = new ThumbBox(this);
		this.infoBox = new InfoBox(this);
		
		/* data range */
		this.minVal = minVal;
		this.maxVal = maxVal;
		this.range = maxVal - minVal;
		
		this.containerPanel = new ContainerPanel();
		
	}
	
	public JPanel getContainerPanel() {
		
		return containerPanel;
	}
	
	private class ContainerPanel extends JPanel {
		
		@Override
		public void paintComponent(final Graphics g) {
		 
			super.paintComponent(g);

			final Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			positionRects(getWidth());
			
			thumbBox.drawThumbBox(g2);
			gradientBox.drawGradientBox(g2);
			infoBox.drawRulerBox(g2);
			infoBox.drawNumBox(g2);
		}
		
		/**
		 * Gives the boxes which make up ColorPicker a start x-position
		 * and a size which is based on the size on a set size for the 
		 * ColorPicker as a whole.
		 * @param width
		 */
		private void positionRects(final double width) {

			final int start = ((int) width - WIDTH) / 2;

			thumbBox.setRect(start, 0, WIDTH, 30);
			gradientBox.setRect(start, 30, WIDTH, 70);
			infoBox.setRect(start, 100, WIDTH, 10);
		}
	}
	
	/**
	 * Loads the values from a given ColorSet into colorList and fractionList.
	 *
	 * @param colorSet
	 */
	protected void loadPresets(final ColorSet colorSet) {

		/* clearing all data */
		colorList.clear();
		thumbList.clear();
		resetFractions();

		final String[] colors = colorSet.getColors();

		for (final String color : colors) {

			colorList.add(Color.decode(color));
		}

		updateColorArray();

		fractions = colorSet.getFractions();
	}
	
	/**
	 * Serves to store the currently chosen custom color setup in a configNode.
	 * @return The new ColorSet object created from the current custom colors
	 * and the relative positions of the associated thumbs.
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

		return new ColorSet(name, colorList, fractionList, missing, empty);
	}
	
	/**
	 * Sets the color in colors[] at the specified index.
	 *
	 * @param newCol
	 * @param index
	 */
	protected void setGradientColors() {

		colorExtractor.setNewParams(fractions, colorList);
		colorExtractor.setMissingColor(activeColorSet.getMissing());
		colorExtractor.notifyObservers();
	}
	
	/* Updates the missing color object in active ColorExtractor */
	protected void setMissing(final Color color) {

		colorExtractor.setMissingColor(color);
		colorExtractor.notifyObservers();
	}
	
	protected void refreshColors() {
		
		updateColorArray(); // swapped
		setGradientColors();
		repaint();
	}
	
//	/**
//	 * Adds a color to the gradient.
//	 *
//	 * @param newCol
//	 */
//	protected void addColor(final Color newCol) {
//
//		int selectedIndex = 0;
//		if (selectedThumb != null) {
//			selectedIndex = thumbList.indexOf(selectedThumb);
//		}
//
//		if (thumbList.get(selectedIndex).getX() == thumbList.get(
//				thumbList.size() - 1).getX()) {
//			selectedIndex--;
//		}
//
//		colorList.add(selectedIndex + 1, newCol);
//
//		final double halfRange = (fractions[selectedIndex + 1] - fractions[selectedIndex]) / 2;
//
//		final double newFraction = halfRange + fractions[selectedIndex];
//
//		final int x = (int) (newFraction * getSize().getWidth());
//
//		thumbBox.insertThumbAt(x, newCol);
//		updateFractions();
//
//		if (thumbList.size() != fractions.length) {
//			System.out.println("ThumbList size (" + thumbList.size()
//					+ ") and fractions size (" + fractions.length
//					+ ") are different in drawNumbBox!");
//		}
//
//		refreshColors();
//	}
//	
//	/**
//	 * Removes a color and its matching thumb from the gradient.
//	 */
//	protected void removeColor() {
//
//		int index = 0;
//		for (final Thumb t : thumbList) {
//
//			if (t.isSelected()) {
//				thumbList.remove(index);
//				colorList.remove(index);
//				selectedThumb = null;
//				break;
//			}
//			index++;
//		}
//
//		updateFractions();
//
//		if (thumbList.size() != fractions.length) {
//			System.out.println("ThumbList size (" + thumbList.size()
//					+ ") and fractions size (" + fractions.length
//					+ ") are different in drawNumbBox!");
//		}
//
//		refreshColors();
//	}
//	
//	/**
//	 * Changes the gradient color in the area the mouse was clicked on.
//	 *
//	 * @param newCol
//	 * @param point
//	 */
//	protected void changeColor(final Point point) {
//
//		Color newCol = null;
//
//		final int clickPos = (int) point.getX();
//		int index = 0;
//		int distance = WIDTH;
//
//		for (final Thumb t : thumbList) {
//
//			if (Math.abs(t.getX() - clickPos) < distance) {
//				distance = Math.abs(t.getX() - clickPos);
//				index = thumbList.indexOf(t);
//			}
//		}
//
//		newCol = JColorChooser.showDialog(this, "Pick a Color",
//				thumbList.get(index).getColor());
//
//		if (newCol != null) {
//			colorList.set(index, newCol);
//			thumbList.get(index).setColor(newCol);
//			
//			refreshColors();
//		}
//	}
	
	/**
	 * Resets the fractions float[] to a default value with 3 colors.
	 */
	protected void resetFractions() {

		this.fractions = new float[] { 0.0f, 0.5f, 1.0f };
	}
	
	/**
	 * Calculates the fractions needed for the LinearGradient object to
	 * determine where the center of each color is displayed.
	 *
	 * @return A float array containing the thumb positions as fractions of the
	 *         width of gradientRect.
	 */
	protected void updateFractions() {

		final float[] newFractions = new float[colorList.size()];

		int i = 0;
		for (final Thumb t : thumbList) {

			newFractions[i++] = thumbBox.getThumbFraction(t);
		}
		
		this.fractions = newFractions;
	}
	
	/*
	 * Updates the color array when needed so LinearGradientPaint can use it to
	 * generate the gradientRect.
	 */
	protected void updateColorArray() {

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
	 * Repaints all related UI elements.
	 */
	protected void repaint() {
		
		gradientBox.repaint();
		thumbBox.repaint();
		infoBox.repaint();
	}
	
	/**
	 * Swaps positions of thumbs and colors in their specific lists.
	 * @param oldIndex
	 * @param newIndex
	 */
	protected void swapPositions(int oldIndex, int newIndex) {
		
		Collections.swap(thumbList, oldIndex, newIndex);
		Collections.swap(colorList, oldIndex, newIndex);
		
		updateColorArray();
		updateFractions();
	}
	
	protected double getMinVal() {
		
		return minVal;
	}
	
	protected double getMaxVal() {
		
		return maxVal;
	}
	
	protected double getRange() { 
		
		return range;
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
}
