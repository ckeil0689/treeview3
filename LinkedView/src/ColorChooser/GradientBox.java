package ColorChooser;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;
import Utilities.Helper;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;

/**
 * A special JPanel that represents a gradient colored box. It has a
 * MouseListener attached (via the controller class) which handles user input
 * and allows for change of color in the clicked area.
 */
public class GradientBox { //extends JPanel {

//	private static final long serialVersionUID = 1L;

//	/* Responsible for active data-to-color mapping */
//	private final ColorExtractor colorExtractor;
//
//	/* List of all active colors (depends on active ColorSet) */
//	private final List<Color> colorList;
//
//	/* Inflexible array of colors for LinearGradientPaint */
//	private Color[] colors;
	private ColorPicker colorPicker;
//
//	/* List of all active thumbs (one per color) */
//	private final List<Thumb> thumbList;
////
//	/* Fractions for LinearGradientPaint (depend on thumb positions) */
//	private float[] fractions;

	/* The currently active set of colors */
//	private ColorSet activeColorSet;

//	/* Data boundaries */
//	private final double minVal;
//	private final double maxVal;

	/* Holds the currently selected thumb */
//	private Thumb selectedThumb = null;

	private final Rectangle2D gradientRect = new Rectangle2D.Float();
//	private final Rectangle2D thumbRect = new Rectangle2D.Float();
//	private final Rectangle2D rulerRect = new Rectangle2D.Float();
//	private final Rectangle2D numRect = new Rectangle2D.Float();

//	private final FontMetrics fm;

	/* Adjust this to MigLayout variables of mainPanel! */
//	private static final int WIDTH = 450;

	/**
	 * Constructs a GradientBox object.
	 */
	public GradientBox(ColorPicker colorPicker) {

		this.colorPicker = colorPicker;
//		this.fractions = fractions;
//		this.colors = colors;
//		this.colorExtractor = drawer;

		/* Font details for text-alignment in numBox */
//		this.fm = getFontMetrics(GUIFactory.FONTS);

		/* data range */
//		this.minVal = minVal;
//		this.maxVal = maxVal;

//		/* active colors and thumbs */
//		this.colorList = new ArrayList<Color>();
//		this.thumbList = new ArrayList<Thumb>();

//		setToolTipText("This Turns Tooltips On");
//		setFocusable(true);
	}
	
	protected void drawGradientBox(final Graphics2D g2) {

		// Dimensions
		final float startX = (float) gradientRect.getMinX();
		final float startY = (float) gradientRect.getMinY();

		final float endX = (float) gradientRect.getMaxX();

		final int height = (int) gradientRect.getHeight();
		final int width = (int) gradientRect.getWidth();
		
		float[] fractions = colorPicker.getFractions();
		Color[] colors = colorPicker.getColors();

		// Generating Gradient to fill the rectangle with
		final LinearGradientPaint gradient = new LinearGradientPaint(startX,
				startY, endX, startY, fractions, colors, CycleMethod.NO_CYCLE);

		/* Outline of gradient box */
		g2.setColor(Color.black);
		g2.drawRect((int) startX, (int) startY, width, height);

		/* fill gradient box with gradient */
		g2.setPaint(gradient);
		g2.fillRect((int) startX, (int) startY, width, height);
	}
	
	/**
	 * Checks if a passed Point is contained in the area of the gradient
	 * rectangle.
	 *
	 * @param point
	 * @return
	 */
	protected boolean isGradientArea(final Point point) {

		return gradientRect.contains(point);
	}
	
	protected void setRect(int start, int left, int width, int height) {
		
		gradientRect.setRect(start, left, width, height);
	}
	
	/**
	 * Adds a color to the gradient.
	 *
	 * @param newCol
	 */
	protected void addColor(final Color newCol) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		int newColorIndex = (thumbs.size() - 1) / 2;

		colorPicker.getColorList().add(newColorIndex + 1, newCol);
		
		float[] fractions = colorPicker.getFractions();
		final double halfRange = (fractions[newColorIndex + 1] 
				- fractions[newColorIndex]) / 2;
		final double newFraction = halfRange + fractions[newColorIndex];

		
		final int x = (int) (newFraction * gradientRect.getWidth());

		colorPicker.getThumbBox().insertThumbAt(x, newCol);
		colorPicker.updateFractions();

		if (thumbs.size() != fractions.length) {
			System.out.println("ThumbList size (" + thumbs.size()
					+ ") and fractions size (" + fractions.length
					+ ") are different in drawNumbBox!");
		}

		colorPicker.refreshColors();
	}
	
	/**
	 * Removes a color and its matching thumb from the gradient.
	 */
	protected void removeColor() {

		float[] fractions = colorPicker.getFractions();
		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colorList = colorPicker.getColorList();
		
		int index = 0;
		for (final Thumb t : thumbs) {

			if (t.isSelected()) {
				thumbs.remove(index);
				colorList.remove(index);
				colorPicker.getThumbBox().setSelectedThumb(null);
				break;
			}
			index++;
		}

		colorPicker.updateFractions();

		if (thumbs.size() != fractions.length) {
			System.out.println("ThumbList size (" + thumbs.size()
					+ ") and fractions size (" + fractions.length
					+ ") are different in drawNumbBox!");
		}

		colorPicker.refreshColors();
	}
	
	/**
	 * Changes the gradient color in the area the mouse was clicked on.
	 *
	 * @param newCol
	 * @param point
	 */
	protected void changeColor(final Point point) {

		List<Thumb> thumbs = colorPicker.getThumbList();
		List<Color> colorList = colorPicker.getColorList();
		
		Color newCol = null;

		final int clickPos = (int) point.getX();
		int index = 0;
		int distance = ColorPicker.WIDTH;

		for (final Thumb t : thumbs) {

			if (Math.abs(t.getX() - clickPos) < distance) {
				distance = Math.abs(t.getX() - clickPos);
				index = thumbs.indexOf(t);
			}
		}

		JPanel panel = colorPicker.getContainerPanel();
		newCol = JColorChooser.showDialog(panel, "Pick a Color",
				thumbs.get(index).getColor());

		if (newCol != null) {
			colorList.set(index, newCol);
			thumbs.get(index).setColor(newCol);
			
			colorPicker.refreshColors();
		}
	}

//	/**
//	 * Sets the activeColors key in configNode. This represents the currently
//	 * selected ColorSet choice by the user.
//	 *
//	 * @param name
//	 *            ConfigNode name of the active ColorSet
//	 */
//	public void setActiveColorSet(final ColorSet set) {
//
//		this.activeColorSet = set;
//	}

//	@Override
//	public void paintComponent(final Graphics g) {
//
//		super.paintComponent(g);
//
//		final Graphics2D g2 = (Graphics2D) g;
//		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);

//		positionRects(getWidth());
//
//		verifyThumbs();
//
//		drawThumbBox(g2);
//		drawGradientBox(g2);
//		drawRulerBox(g2);
//		drawNumBox(g2);

//		g2.dispose();
//	}

//	private void positionRects(final double width) {
//
//		final int start = ((int) width - WIDTH) / 2;
//
//		thumbRect.setRect(start, 0, WIDTH, 30);
//		gradientRect.setRect(start, 30, WIDTH, 70);
//		rulerRect.setRect(start, 100, WIDTH, 10);
//		numRect.setRect(start, 110, WIDTH, 40);
//	}


//	private void drawThumbBox(final Graphics2D g2) {
//
//		// Fill thumbRect with background color
//		g2.setColor(GUIFactory.DEFAULT_BG);
//		g2.fill(thumbRect);
//
//		// Paint the thumbs
//		for (final Thumb t : thumbList) {
//
//			t.paint(g2);
//		}
//	}

//	private void drawRulerBox(final Graphics2D g2) {
//
//		g2.setColor(Color.black);
//
//		final int minY = (int) rulerRect.getMinY();
//		final int minX = (int) rulerRect.getMinX();
//		final int maxX = (int) rulerRect.getMaxX();
//
//		for (int x = minX; x < maxX + 1; x++) {
//
//			if (x == minX || x == maxX) {
//				g2.drawLine(x, minY, x, minY + 10);
//
//			} else if ((x - minX) % 50 == 0) {
//				g2.drawLine(x, minY, x, minY + 5);
//			}
//		}
//	}

//	private void drawNumBox(final Graphics2D g2) {
//
//		g2.setColor(GUIFactory.DEFAULT_BG);
//		g2.fill(numRect);
//
//		g2.setColor(Color.black);
//		g2.setFont(GUIFactory.FONTS);
//
//		// Paint the thumb values
//		// if (thumbList.size() == fractions.length) {
//		// int i = 0;
//		// for (final Thumb t : thumbList) {
//		//
//		// // Rounding to 3 decimals
//		// final float fraction = fractions[i];
//		// Double value = Math.abs((maxVal - minVal) * fraction)
//		// + minVal;
//		// value = (double) Math.round(value * 1000) / 1000;
//		//
//		// g2.drawString(Double.toString(value), t.getX(),
//		// (int) ((numRect.getHeight() / 2) + numRect
//		// .getMinY()));
//		// i++;
//		// }
//		// } else {
//		// LogBuffer.println("ThumbList size (" + thumbList.size()
//		// + ") and fractions size (" + fractions.length
//		// + ") are different in drawNumbBox!");
//		// }
//
//		/* Draw first number */
//		final double first = minVal;
//		int x = (int) numRect.getMinX();
//
//		g2.drawString(Double.toString(first), x,
//				(int) ((numRect.getHeight() / 2) + numRect.getMinY()));
//
//		final double last = maxVal;
//		x = (int) numRect.getMaxX();
//		final int stringWidth = fm.stringWidth(Double.toString(last));
//
//		g2.drawString(Double.toString(last), x - stringWidth,
//				(int) ((numRect.getHeight() / 2) + numRect.getMinY()));
//	}

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
//		insertThumbAt(x, newCol);
//		fractions = updateFractions();
//
//		if (thumbList.size() != fractions.length) {
//			System.out.println("ThumbList size (" + thumbList.size()
//					+ ") and fractions size (" + fractions.length
//					+ ") are different in drawNumbBox!");
//		}
//
//		setGradientColors();
//		updateColorArray();
//		repaint();
//	}

//	/**
//	 * Removes a color from the gradient.
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
//		fractions = updateFractions();
//
//		if (thumbList.size() != fractions.length) {
//			System.out.println("ThumbList size (" + thumbList.size()
//					+ ") and fractions size (" + fractions.length
//					+ ") are different in drawNumbBox!");
//		}
//
//		setGradientColors();
//		updateColorArray();
//		repaint();
//	}

//	/**
//	 * Changes the gradient color in the area the mouse was clicked in.
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
//			setGradientColors();
//			updateColorArray();
//			repaint();
//		}
//	}

//	/*
//	 * Updates the color array when needed so LinearGradientPaint can use it to
//	 * generate the gradientRect.
//	 */
//	private void updateColorArray() {
//
//		colors = new Color[colorList.size()];
//
//		for (int i = 0; i < colors.length; i++) {
//
//			colors[i] = colorList.get(i);
//		}
//	}

//	/**
//	 * Loads the values from a given ColorSet into colorList and fractionList.
//	 *
//	 * @param colorSet
//	 */
//	protected void loadPresets(final ColorSet colorSet) {
//
//		/* clearing all data */
//		colorList.clear();
//		thumbList.clear();
//		fractions = resetFractions();
//
//		final String[] colors = colorSet.getColors();
//
//		for (final String color : colors) {
//
//			colorList.add(Color.decode(color));
//		}
//
//		updateColorArray();
//
//		fractions = colorSet.getFractions();
//	}

//	/**
//	 * Sets the color in colors[] at the specified index.
//	 *
//	 * @param newCol
//	 * @param index
//	 */
//	protected void setGradientColors() {
//
//		colorExtractor.setNewParams(fractions, colorList);
//		colorExtractor.setMissingColor(activeColorSet.getMissing());
//		colorExtractor.notifyObservers();
//	}
//
//	/* Updates the missing color object in active ColorExtractor */
//	protected void setMissing(final Color color) {
//
//		colorExtractor.setMissingColor(color);
//		colorExtractor.notifyObservers();
//	}

//	/**
//	 * Serves to store the currently chosen custom color setup in a configNode.
//	 */
//	protected ColorSet saveCustomPresets() {
//
//		final List<Double> fractionList = new ArrayList<Double>();
//
//		for (final float f : fractions) {
//
//			fractionList.add((double) f);
//		}
//
//		final String name = "Custom";
//		String missing = Integer.toHexString(colorExtractor.getMissing()
//				.getRGB());
//		missing = "#" + missing.substring(2, missing.length());
//		String empty = Integer.toHexString(colorExtractor.getEmpty().getRGB());
//		empty = "#" + empty.substring(2, empty.length());
//
//		return new ColorSet(name, colorList, fractionList, missing, empty);
//	}

//	// Thumb related methods
//	/**
//	 * Checks if a thumb is located at the x,y-coordinates.
//	 *
//	 * @param x
//	 * @param y
//	 * @return
//	 */
//	protected boolean containsThumb(final int x, final int y) {
//
//		boolean containsThumb = false;
//
//		for (final Thumb t : thumbList) {
//
//			if (t.contains(x, y)) {
//				containsThumb = true;
//				break;
//			}
//		}
//
//		return containsThumb;
//	}

//	/**
//	 * Selects a thumb if it is not yet selected, deselects it otherwise.
//	 *
//	 * @param point
//	 */
//	protected void selectThumb(final Point point) {
//
//		for (final Thumb t : thumbList) {
//
//			if (t.contains((int) point.getX(), (int) point.getY())) {
//				t.setSelected(!t.isSelected());
//
//				if (t.isSelected()) {
//					selectedThumb = t;
//				} else {
//					selectedThumb = null;
//				}
//				break;
//
//			} else {
//				t.setSelected(false);
//			}
//		}
//
//		repaint();
//	}

//	/**
//	 * First checks if fraction array is in ascending order, then verifies thumb
//	 * positions to match fractions. Returns a boolean so that the boxes won't
//	 * be drawn in the paintComponent method, if the fractions aren't ascending.
//	 * This would cause an exception with LinearGradientPaint.
//	 */
//	private void verifyThumbs() {
//
//		final int x = (int) thumbRect.getMinX();
//		final int w = (int) thumbRect.getWidth();
//
//		for (int i = 0; i < fractions.length; i++) {
//
//			// Avoid rounding errors when casting to int
//			final double widthFactor = Math.round(w * fractions[i]);
//			final int pos = x + (int) (widthFactor);
//
//			if (!checkThumbPresence(i)
//					&& !((thumbList.size() == colorList.size()) && thumbList
//							.size() == fractions.length)) {
//				insertThumbAt(pos, colorList.get(i));
//
//			}
//		}
//	}

//	/**
//	 * Checks if a thumb is present at the given x-position.
//	 *
//	 * @param pos
//	 * @return
//	 */
//	private boolean checkThumbPresence(final int thumbIndex) {
//
//		boolean isPresent = false;
//
//		if (thumbList.size() > thumbIndex) {
//			double fraction = thumbList.get(thumbIndex).getX()
//					/ thumbRect.getWidth();
//			fraction = (double) Math.round(fraction * 10000) / 10000;
//
//			final double fraction2 = (double) Math
//					.round(fractions[thumbIndex] * 10000) / 10000;
//
//			if (Helper.nearlyEqual(fraction, fraction2)
//					|| thumbList.get(thumbIndex).isSelected()) {
//				isPresent = true;
//			}
//		}
//
//		return isPresent;
//	}

//	/**
//	 * Inserts a thumb at a specific x-value and passes a color to the Thumb
//	 * constructor.
//	 *
//	 * @param x
//	 * @param color
//	 */
//	private void insertThumbAt(final int x, final Color color) {
//
//		int index = 0;
//		for (final Thumb t : thumbList) {
//
//			if (x > t.getX()) {
//				index++;
//			}
//		}
//
//		final int y = (int) (thumbRect.getHeight());
//		thumbList.add(index, new Thumb(x, y, color));
//	}

//	/**
//	 * Sets all thumbs' selection status to 'false'.
//	 */
//	protected void deselectAllThumbs() {
//
//		for (final Thumb t : thumbList) {
//
//			t.setSelected(false);
//			selectedThumb = null;
//		}
//	}

//	protected void updateThumbPos(final int inputX) {
//
//		/* stay within boundaries */
//		if (selectedThumb == null
//				|| (inputX < (int) thumbRect.getMinX() || inputX > (int) thumbRect
//						.getMaxX()))
//			return;
//
//		/* get position of previous thumb */
//		final int selectedIndex = thumbList.indexOf(selectedThumb);
//		int previousPos = 0;
//		int nextPos = (int) thumbRect.getMaxX();
//
//		/* set positions around active thumb */
//		if (selectedIndex == 0) {
//			nextPos = thumbList.get(selectedIndex + 1).getX();
//
//		} else if (selectedIndex == thumbList.size() - 1) {
//			previousPos = thumbList.get(selectedIndex - 1).getX();
//
//		} else {
//			previousPos = thumbList.get(selectedIndex - 1).getX();
//			nextPos = thumbList.get(selectedIndex + 1).getX();
//		}
//
//		/* get updated x-values */
//		final int deltaX = inputX - selectedThumb.getX();
//		final int newX = selectedThumb.getX() + deltaX;
//
//		/* set new thumb position and check for boundaries/ other thumbs */
//		if (previousPos < newX && newX < nextPos) {
//			selectedThumb.setCoords(newX, selectedThumb.getY());
//			fractions = updateFractions();
//
//		} else if (newX < previousPos && previousPos != thumbRect.getMinX()) {
//			Collections.swap(thumbList, selectedIndex, selectedIndex - 1);
//			Collections.swap(colorList, selectedIndex, selectedIndex - 1);
//			selectedThumb.setCoords(newX, selectedThumb.getY());
//			fractions = updateFractions();
//			updateColorArray();
//
//		} else if (newX > nextPos && nextPos < thumbRect.getMaxX()) {
//			Collections.swap(thumbList, selectedIndex, selectedIndex + 1);
//			Collections.swap(colorList, selectedIndex, selectedIndex + 1);
//			selectedThumb.setCoords(newX, selectedThumb.getY());
//			fractions = updateFractions();
//			updateColorArray();
//		}
//
//		setGradientColors();
//		repaint();
//	}

//	protected void setThumbPosition(final Point point) {
//
//		for (final Thumb t : thumbList) {
//
//			if (t.contains((int) point.getX(), (int) point.getY())) {
//
//				final JDialog positionInputDialog = new JDialog();
//				positionInputDialog
//				.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
//				positionInputDialog
//				.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//				positionInputDialog.setTitle("New Position");
//
//				final JLabel enterPrompt = GUIFactory.createLabel(
//						"Enter data value: ", GUIFactory.FONTS);
//
//				final JTextField inputField = new JTextField();
//				inputField.setEditable(true);
//
//				/* Initially display thumb position */
//				inputField.setText(Double.toString(getThumbPosition(t)));
//
//				final JButton okButton = GUIFactory.createBtn("OK");
//				okButton.addActionListener(new ActionListener() {
//
//					@Override
//					public void actionPerformed(final ActionEvent arg0) {
//
//						try {
//							final double inputDataValue = Double
//									.parseDouble(inputField.getText());
//
//							if (inputDataValue >= minVal
//									&& inputDataValue <= maxVal) {
//
//								final double fraction = Math.abs(inputDataValue
//										- minVal)
//										/ (maxVal - minVal);
//
//								final int inputXValue = (int) Math
//										.round((fraction * WIDTH));
//
//								updateThumbPos(inputXValue);
//								positionInputDialog.dispose();
//
//							} else {
//								inputField.setText("Number out of range!");
//							}
//
//						} catch (final NumberFormatException e) {
//							inputField.setText("Enter a number!");
//						}
//					}
//				});
//
//				final JPanel panel = GUIFactory.createJPanel(false,
//						GUIFactory.DEFAULT, null);
//
//				panel.add(enterPrompt, "push, span, wrap");
//				panel.add(inputField, "push, growx, span, wrap");
//				panel.add(okButton, "pushx, alignx 50%");
//
//				positionInputDialog.getContentPane().add(panel);
//
//				positionInputDialog.pack();
//				positionInputDialog.setLocationRelativeTo(Frame.getFrames()[0]);
//				positionInputDialog.setVisible(true);
//			}
//		}
//
//		setGradientColors();
//		repaint();
//	}

//	/**
//	 * Calculates the fractions needed for the LinearGradient object to
//	 * determine where the center of each color is displayed.
//	 *
//	 * @return A float array containing the thumb positions as fractions of the
//	 *         width of gradientRect.
//	 */
//	private float[] updateFractions() {
//
//		final float[] fractions = new float[colorList.size()];
//
//		int i = 0;
//		for (final Thumb t : thumbList) {
//
//			fractions[i++] = getThumbFraction(t);
//		}
//
//		return fractions;
//	}

//	/**
//	 * Returns the fraction of the width of the gradientRect where a thumb is
//	 * currently positioned.
//	 *
//	 * @param t
//	 * @return a float value between 0.0 and 1.0
//	 */
//	private float getThumbFraction(final Thumb t) {
//
//		final double x = t.getX() - gradientRect.getMinX();
//		return (float) (x / gradientRect.getWidth());
//	}
//
//	/**
//	 * Gets a thumb's position in terms of the data range.
//	 *
//	 * @param t
//	 * @return A double value between minimum and maximum of the currently
//	 *         relevant data range for coloring.
//	 */
//	private double getThumbPosition(final Thumb t) {
//
//		final float fraction = getThumbFraction(t);
//		final double value = Math.abs((maxVal - minVal) * fraction) + minVal;
//
//		return (double) Math.round(value * 1000) / 1000;
//	}

//	/**
//	 * Resets the fractions float[] to a default value with 3 colors.
//	 *
//	 * @return
//	 */
//	private float[] resetFractions() {
//
//		return new float[] { 0.0f, 0.5f, 1.0f };
//	}

//	@Override
//	public String getToolTipText(final MouseEvent e) {
//
//		String ret = "";
//		for (final Thumb t : thumbList) {
//
//			if (t.contains(e.getX(), e.getY())) {
//				final double value = getThumbPosition(t);
//				ret = Double.toString(value);
//			}
//		}
//		return ret;
//	}
//	
//	protected float[] getFractions() {
//		
//		return fractions;
//	}
//	
//	protected List<Color> getColorList() {
//		
//		return colorList;
//	}

//	/**
//	 * Returns the current size of colorList in ColorGradientChooser.
//	 *
//	 * @return int
//	 */
//	protected int getColorListSize() {
//
//		return colorList.size();
//	}

//	/**
//	 * Returns the Color set for missing data.
//	 *
//	 * @return Color
//	 */
//	protected Color getMissing() {
//
//		return colorExtractor.getMissing();
//	}
}
