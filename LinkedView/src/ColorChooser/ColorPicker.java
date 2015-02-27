package ColorChooser;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JColorChooser;

import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;

public class ColorPicker {

	/* Adjust this to MigLayout variables of mainPanel! */
	private static final int WIDTH = 450;
	
	/* The currently active set of colors */
	private ColorSet activeColorSet;
	
	/* Responsible for active data-to-color mapping */
	private final ColorExtractor colorExtractor;
	
	/* >>>>> Important shared state variables <<<<<< */
	/* Fractions for LinearGradientPaint (depend on thumb positions) */
	private float[] fractions;
	
	/* List of all active thumbs (one per color) */
	private final List<Thumb> thumbList;
	
	/* List of all active colors (depends on active ColorSet) */
	private final List<Color> colorList;
	
	/* Inflexible array of colors for LinearGradientPaint */
	private Color[] colors;
	
	/* Data boundaries */
	private final double minVal;
	private final double maxVal;
	
	public ColorPicker(final ColorExtractor drawer, final double minVal,
			final double maxVal) {
		
		this.colorExtractor = drawer;
		this.thumbList = new ArrayList<Thumb>();
		this.colorList = new ArrayList<Color>();
		
		/* data range */
		this.minVal = minVal;
		this.maxVal = maxVal;
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
		fractions = resetFractions();

		final String[] colors = colorSet.getColors();

		for (final String color : colors) {

			colorList.add(Color.decode(color));
		}

		updateColorArray();

		fractions = colorSet.getFractions();
	}
	
	/**
	 * Serves to store the currently chosen custom color setup in a configNode.
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
	
	private void positionRects(final double width) {

		final int start = ((int) width - WIDTH) / 2;

		thumbRect.setRect(start, 0, WIDTH, 30);
		gradientRect.setRect(start, 30, WIDTH, 70);
		rulerRect.setRect(start, 100, WIDTH, 10);
		numRect.setRect(start, 110, WIDTH, 40);
	}
	
	private void refresh() {
		
		setGradientColors();
		updateColorArray();
		repaint();
	}
	
	/**
	 * Adds a color to the gradient.
	 *
	 * @param newCol
	 */
	protected void addColor(final Color newCol) {

		int selectedIndex = 0;
		if (selectedThumb != null) {
			selectedIndex = thumbList.indexOf(selectedThumb);
		}

		if (thumbList.get(selectedIndex).getX() == thumbList.get(
				thumbList.size() - 1).getX()) {
			selectedIndex--;
		}

		colorList.add(selectedIndex + 1, newCol);

		final double halfRange = (fractions[selectedIndex + 1] - fractions[selectedIndex]) / 2;

		final double newFraction = halfRange + fractions[selectedIndex];

		final int x = (int) (newFraction * getSize().getWidth());

		insertThumbAt(x, newCol);
		fractions = updateFractions();

		if (thumbList.size() != fractions.length) {
			System.out.println("ThumbList size (" + thumbList.size()
					+ ") and fractions size (" + fractions.length
					+ ") are different in drawNumbBox!");
		}

		refresh();
	}
	
	/**
	 * Removes a color from the gradient.
	 */
	protected void removeColor() {

		int index = 0;
		for (final Thumb t : thumbList) {

			if (t.isSelected()) {
				thumbList.remove(index);
				colorList.remove(index);
				selectedThumb = null;
				break;
			}
			index++;
		}

		fractions = updateFractions();

		if (thumbList.size() != fractions.length) {
			System.out.println("ThumbList size (" + thumbList.size()
					+ ") and fractions size (" + fractions.length
					+ ") are different in drawNumbBox!");
		}

		refresh();
	}
	
	/**
	 * Changes the gradient color in the area the mouse was clicked in.
	 *
	 * @param newCol
	 * @param point
	 */
	protected void changeColor(final Point point) {

		Color newCol = null;

		final int clickPos = (int) point.getX();
		int index = 0;
		int distance = WIDTH;

		for (final Thumb t : thumbList) {

			if (Math.abs(t.getX() - clickPos) < distance) {
				distance = Math.abs(t.getX() - clickPos);
				index = thumbList.indexOf(t);
			}
		}

		newCol = JColorChooser.showDialog(this, "Pick a Color",
				thumbList.get(index).getColor());

		if (newCol != null) {
			colorList.set(index, newCol);
			thumbList.get(index).setColor(newCol);
			
			refresh();
		}
	}
	
	/**
	 * Calculates the fractions needed for the LinearGradient object to
	 * determine where the center of each color is displayed.
	 *
	 * @return A float array containing the thumb positions as fractions of the
	 *         width of gradientRect.
	 */
	private float[] updateFractions() {

		final float[] fractions = new float[colorList.size()];

		int i = 0;
		for (final Thumb t : thumbList) {

			fractions[i++] = getThumbFraction(t);
		}

		return fractions;
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
	
	protected List<Color> getColorList() {
		
		return colorList;
	}
	
	protected float[] getFractions() {
		
		return fractions;
	}
}
