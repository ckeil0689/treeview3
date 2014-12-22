package ColorChooser;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;
import Utilities.Helper;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;

public class ColorChooser implements ConfigNodePersistent {
	
	/* Saved data */
	private Preferences configNode;
	
	/* GUI components */
	private JPanel mainPanel;
	private GradientBox gradientBox;

	/* Color data storage */
	private List<Color> colorList;
	private float[] fractions;

	/* Data boundaries */
	private final double minVal;
	private final double maxVal;

	/* Flexible list of thumbs for gradient manipulation */
	private List<Thumb> thumbList;

	/* For custom ColorSet manipulation */
	private JButton addBtn;
	private JButton removeBtn;
	private JButton missingBtn;
	
	/* Collection of ColorSet choice buttons */
	private ButtonGroup colorBtnGroup;

	/* ColorSet choices */
	private JRadioButton redGreenBtn;
	private JRadioButton yellowBlueBtn;
	private JRadioButton customColorBtn;

	/* Responsible for active data-to-color mapping */
	private final ColorExtractor colorExtractor;
	
	/* Holds all preset color data */
	private final ColorPresets colorPresets;
	
	/* The currently active set of colors */
	private ColorSet activeColorSet;
	
	/* Stores whether custom ColorSet is selected or not */
	private boolean isCustomSelected;

	/* Holds the currently selected thumb */
	private Thumb selectedThumb = null;

	/**
	 * Constructs a ColorChooser object.
	 * @param drawer The CoorExtractor which defines how colors are mapped
	 * to data.
	 * @param minVal Minimum boundary of the data.
	 * @param maxVal Maximum boundary of the data.
	 */
	public ColorChooser(final ColorExtractor drawer, Double minVal, Double maxVal) {

		this.colorExtractor = drawer;
		this.colorPresets = DendrogramFactory.getColorPresets();
		this.minVal = minVal;
		this.maxVal = maxVal;
		
		setLayout();
	}
	
	/**
	 * Sets up the GUI layout of the ColorChooser object. 
	 */
	private void setLayout() {
		
		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
		mainPanel.setBorder(BorderFactory.createEtchedBorder());

		final JLabel hint = GUIFactory.createLabel("Move or add sliders "
				+ "to adjust color scheme.", GUIFactory.FONTS);

		this.colorList = new ArrayList<Color>();
		thumbList = new ArrayList<Thumb>();

		addBtn = GUIFactory.createBtn("Add Color");
		removeBtn = GUIFactory.createBtn("Remove Selected");

		colorBtnGroup = new ButtonGroup();

		redGreenBtn = GUIFactory.createRadioBtn("Red-Green");
		yellowBlueBtn = GUIFactory.createRadioBtn("Yellow-Blue");
		customColorBtn = GUIFactory.createRadioBtn("Custom Colors");
		missingBtn = GUIFactory.createBtn("Missing Data");

		colorBtnGroup.add(redGreenBtn);
		colorBtnGroup.add(yellowBlueBtn);
		colorBtnGroup.add(customColorBtn);

		final JPanel radioButtonPanel = 
				GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);

		final JLabel colorHint = GUIFactory.createLabel("Choose a Color "
				+ "Scheme: ", GUIFactory.FONTS);

		radioButtonPanel.add(colorHint, "span, wrap");
		radioButtonPanel.add(redGreenBtn, "span, wrap");
		radioButtonPanel.add(yellowBlueBtn, "span, wrap");
		radioButtonPanel.add(customColorBtn, "span");
		
		gradientBox = this.new GradientBox();
		gradientBox.setPresets();
		
		mainPanel.add(hint, "span, wrap");
		mainPanel.add(gradientBox, "h 150:150:, w 500:500:, pushx, alignx 50%, "
				+ "span, wrap");
		mainPanel.add(addBtn, "pushx, split 3, alignx 50%");
		mainPanel.add(removeBtn, "pushx");
		mainPanel.add(missingBtn, "wrap");
		mainPanel.add(radioButtonPanel, "pushx, wrap");
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("GradientChooser");
			
			String colorSet = configNode.get("activeColors", "RedGreen");
			this.activeColorSet = colorPresets.getColorSet(colorSet);

		} else {
			LogBuffer.println("Could not find or create GradientChooser "
					+ "node because parentNode was null.");
		}
	}

	/**
	 * A special JPanel that represents a gradient colored box. It has a
	 * MouseListener attached (via the controller class) which handles user
	 * input and allows for change of color in the clicked area.
	 */
	protected class GradientBox extends JPanel {

		private static final long serialVersionUID = 1L;

		private final Rectangle2D gradientRect = new Rectangle2D.Float();
		private final Rectangle2D thumbRect = new Rectangle2D.Float();
		private final Rectangle2D rulerRect = new Rectangle2D.Float();
		private final Rectangle2D numRect = new Rectangle2D.Float();
		
		private final FontMetrics fm;
		
		/* Adjust this to MigLayout variables of mainPanel! */
		private static final int WIDTH = 450; 

		/**
		 * Constructs a GradientBox object.
		 */
		public GradientBox() {
			
			this.fm = getFontMetrics(GUIFactory.FONTS);

			setFocusable(true);
			setPresets();
		}

		@Override
		public void paintComponent(final Graphics g) {

			super.paintComponent(g);

			final double width = getSize().getWidth();
			final double height = getSize().getHeight();

			setupRects(width, height);

			final Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			verifyThumbs();
			drawThumbBox(g2);

			try {
				drawGradientBox(g2);

			} catch (final IllegalArgumentException e) {
				LogBuffer.println("IllegalArgumentException in "
						+ "drawGradientBox in ColorChooser: "
						+ e.getMessage());
				LogBuffer.println("Fraction Keyframe not increasing "
						+ "for LinearGradientPaint.");
				LogBuffer.println("Fractions: " + Arrays.toString(fractions));
				LogBuffer.println("Colors: " + colorList.toString());
			}

			drawRulerBox(g2);
			drawNumBox(g2);

			g2.dispose();
		}

		private void setupRects(final double width, final double height) {
			
			int start = ((int)width - WIDTH)/ 2;
			
			thumbRect.setRect(start, 0, WIDTH, 30);
			gradientRect.setRect(start, 30, WIDTH, 70);
			rulerRect.setRect(start, 100, WIDTH, 10);
			numRect.setRect(start, 110, WIDTH, 40);
		}

		private void drawGradientBox(final Graphics2D g2)
				throws IllegalArgumentException {

			// Dimensions
			final float startX = (float) gradientRect.getMinX();
			final float startY = (float) gradientRect.getMinY();

			final float endX = (float) gradientRect.getMaxX();
			
			final int height = (int) gradientRect.getHeight();
			final int width = (int) gradientRect.getWidth();

			Color[] colors = new Color[colorList.size()];

			for (int i = 0; i < colors.length; i++) {

				colors[i] = colorList.get(i);
			}

			// Generating Gradient to fill the rectangle with
			final LinearGradientPaint gradient = new LinearGradientPaint(
					startX, startY, endX, startY, fractions, colors, 
					CycleMethod.NO_CYCLE);

			g2.setPaint(gradient);
			g2.fillRect((int) startX, (int) startY, width, height);
		}

		private void drawThumbBox(final Graphics2D g2) {

			// Fill thumbRect with background color
			g2.setColor(GUIFactory.DEFAULT_BG);
			g2.fill(thumbRect);

			// Paint the thumbs
			for (final Thumb t : thumbList) {

				t.paint(g2);
			}
		}
		
		private void drawRulerBox(final Graphics2D g2) {
			
			g2.setColor(Color.black);
		    
			final int minY = (int) rulerRect.getMinY();
			final int minX = (int) rulerRect.getMinX();
			final int maxX = (int) rulerRect.getMaxX();
		    
		    for (int x = minX; x < maxX + 1; x++) {
		    	
		    	if(x == minX || x == maxX) {
		    		g2.drawLine(x, minY, x, minY + 10);
		    		
		    	} else if ((x - minX) % 50 == 0) {
		            g2.drawLine(x, minY, x, minY + 5);
		     	}
//		        else if (x % 10 == 0) {
//		            g2.drawLine(x, maxY - 10, x, maxY);
//		        }
//		        else if (x % 2 == 0) {
//		            g2.drawLine(x, maxY - 5, x, maxY);
//		        }
		    }
		}

		private void drawNumBox(final Graphics2D g2) {

			g2.setColor(GUIFactory.DEFAULT_BG);
			g2.fill(numRect);

			g2.setColor(Color.black);
			g2.setFont(GUIFactory.FONTS);

			// Paint the thumb values
//			if (thumbList.size() == fractions.length) {
//				int i = 0;
//				for (final Thumb t : thumbList) {
//
//					// Rounding to 3 decimals
//					final float fraction = fractions[i];
//					Double value = Math.abs((maxVal - minVal) * fraction)
//							+ minVal;
//					value = (double) Math.round(value * 1000) / 1000;
//
//					g2.drawString(Double.toString(value), t.getX(),
//							(int) ((numRect.getHeight() / 2) + numRect
//									.getMinY()));
//					i++;
//				}
//			} else {
//				LogBuffer.println("ThumbList size (" + thumbList.size()
//						+ ") and fractions size (" + fractions.length
//						+ ") are different in drawNumbBox!");
//			}
			
			/* Draw first number */
			double first = minVal;
			int x = (int) numRect.getMinX();
			
			g2.drawString(Double.toString(first), x,
					(int) ((numRect.getHeight() / 2) + numRect
							.getMinY()));
			
			double last = minVal;
			x = (int) numRect.getMaxX();
			int stringWidth = fm.stringWidth(Double.toString(last));
			
			g2.drawString(Double.toString(last), x - stringWidth,
					(int) ((numRect.getHeight() / 2) + numRect
							.getMinY()));
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

			final double halfRange = (fractions[selectedIndex + 1] 
					- fractions[selectedIndex]) / 2;

			final double newFraction = halfRange + fractions[selectedIndex];

			final int x = (int) (newFraction * getGradientBox().getSize()
					.getWidth());

			insertThumbAt(x, newCol);
			fractions = updateFractions();

			if (thumbList.size() != fractions.length) {
				System.out.println("ThumbList size (" + thumbList.size()
						+ ") and fractions size (" + fractions.length
						+ ") are different in drawNumbBox!");
			}

			setGradientColors();
			repaint();
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

			setGradientColors();
			repaint();
		}

		/**
		 * Changes the gradient color in the area the mouse was clicked in.
		 * 
		 * @param newCol
		 * @param point
		 */
		protected void setGradientColor(final Point point) {

			Color newCol = null;

			final int clickPos = (int) point.getX();
			int index = 0;
			int distance = gradientBox.getWidth();

			for (final Thumb t : thumbList) {

				if (Math.abs(t.getX() - clickPos) < distance) {
					distance = Math.abs(t.getX() - clickPos);
					index = thumbList.indexOf(t);
				}
			}

			newCol = JColorChooser.showDialog(this, "Pick a Color", thumbList
					.get(index).getColor());

			if (newCol != null) {
				colorList.set(index, newCol);
				thumbList.get(index).setColor(newCol);
				setGradientColors();
			}
		}

		/**
		 * Set all default color values.
		 */
		private void setPresets() {

			/* clearing all data */
			colorList.clear();
			thumbList.clear();
			fractions = resetFractions();

			final String defaultColors = "RedGreen";
			
			/* Get the active ColorSet name */
			String colorScheme = defaultColors;
			if (configNode != null) {
				colorScheme = configNode.get("activeColors",
					defaultColors);
			}

			/* Choose ColorSet according to name */
			final ColorSet selectedColorSet;
			if (colorScheme.equalsIgnoreCase("Custom")) {
				customColorBtn.setSelected(true);
				setCustomSelected(true);
				selectedColorSet = colorPresets.getColorSet(colorScheme);

			} else if (colorScheme.equalsIgnoreCase(defaultColors)) {
				redGreenBtn.setSelected(true);
				setCustomSelected(false);
				selectedColorSet = colorPresets.getColorSet(colorScheme);

			} else if (colorScheme.equalsIgnoreCase("YellowBlue")) {
				yellowBlueBtn.setSelected(true);
				setCustomSelected(false);
				selectedColorSet = colorPresets.getColorSet("YellowBlue");

			} else {
				/* Should never get here */
				selectedColorSet = null;
				LogBuffer.println("No matching ColorSet found in "
						+ "ColorGradientChooser.setPresets()");
			}
			
			loadPresets(selectedColorSet);
		}

		/**
		 * Loads the values from a given ColorSet into colorList and
		 * fractionList.
		 * 
		 * @param colorSet
		 */
		private void loadPresets(final ColorSet colorSet) {

			final String[] colors = colorSet.getColors();

			for (final String color : colors) {

				colorList.add(Color.decode(color));
			}

			final float[] fracs = colorSet.getFractions();
			fractions = fracs;
		}
		
		/**
		 * Sets the color in colors[] at the specified index.
		 * 
		 * @param newCol
		 * @param index
		 */
		private void setGradientColors() {

			colorExtractor.setNewParams(fractions, colorList);
			colorExtractor.setMissingColor(activeColorSet.getMissing());
			colorExtractor.notifyObservers();
			mainPanel.repaint();
		}

		/**
		 * Serves to store the currently chosen custom color setup in a
		 * configNode.
		 */
		protected void saveCustomPresets() {

			final List<Double> fractionList = new ArrayList<Double>();

			for (final float f : fractions) {

				fractionList.add((double) f);
			}

			final String colorSetName = "Custom";
			String missing = Integer.toHexString(colorExtractor.getMissing()
					.getRGB());
			missing = "#" + missing.substring(2, missing.length());
			String empty = Integer.toHexString(colorExtractor.getEmpty()
					.getRGB());
			empty = "#" + empty.substring(2, empty.length());
			colorPresets.addColorSet(colorSetName, colorList, fractionList,
					 missing, empty);
			
			LogBuffer.println("Custom colors saved.");
		}

		/**
		 * Checks if a passed Point is contained in the area of the gradient
		 * rectangle.
		 * 
		 * @param point
		 * @return
		 */
		protected boolean isGradientArea(final Point point) {

			boolean inArea = false;

			if (gradientRect.contains(point)) {
				inArea = true;
			}

			return inArea;
		}

		// Thumb related methods
		/**
		 * Checks if a thumb is located at the x,y-coordinates.
		 * 
		 * @param x
		 * @param y
		 * @return
		 */
		protected boolean containsThumb(final int x, final int y) {

			boolean containsThumb = false;

			for (final Thumb t : thumbList) {

				if (t.contains(x, y)) {
					containsThumb = true;
					break;
				}
			}

			return containsThumb;
		}

		/**
		 * Selects a thumb if it is not yet selected, deselects it otherwise.
		 * 
		 * @param point
		 */
		protected void selectThumb(final Point point) {

			for (final Thumb t : thumbList) {

				if (t.contains((int) point.getX(), (int) point.getY())) {
					t.setSelected(!t.isSelected());
					break;
					
				} else {
					t.setSelected(false);
				}
			}

			repaint();
		}

		/**
		 * First checks if fraction array is in ascending order, then verifies
		 * thumb positions to match fractions. Returns a boolean so that the
		 * boxes won't be drawn in the paintComponent method, if the fractions
		 * aren't ascending. This would cause an exception with
		 * LinearGradientPaint.
		 */
		private void verifyThumbs() {

			final int x = (int) thumbRect.getMinX();
			final int w = (int) thumbRect.getWidth();

			for (int i = 0; i < fractions.length; i++) {

				// Avoid rounding errors when casting to int
				final double widthFactor = Math.round(w * fractions[i]);
				final int pos = x + (int) (widthFactor);

				if (!checkThumbPresence(i)
						&& !((thumbList.size() == colorList.size()) && thumbList
								.size() == fractions.length)) {
					insertThumbAt(pos, colorList.get(i));

				} 
//				else {
//					adjustThumbPos(i);
//				}
			}
		}

		private boolean verifyFractions() {

			boolean ascending = true;
			for (int i = 0; i < fractions.length - 1; i++) {

				// Ascending
				if (fractions[i] > fractions[i + 1]) {
					ascending = false;
					break;
				}

				// Out of range
				if (fractions[i] > 1.0 || fractions[i + 1] > 1.0) {
					ascending = false;
					break;

				} else if (fractions[i] < 0.0 || fractions[i + 1] < 0.0) {
					ascending = false;
					break;

				} else if (Helper.nearlyEqual(fractions[i], fractions[i + 1])) {
					ascending = false;
					break;
				}
			}

			return ascending;
		}

		/**
		 * Checks if a thumb is present at the given x-position.
		 * 
		 * @param pos
		 * @return
		 */
		private boolean checkThumbPresence(final int thumbIndex) {

			boolean isPresent = false;

			if (thumbList.size() > thumbIndex) {
				double fraction = thumbList.get(thumbIndex).getX()
						/ thumbRect.getWidth();
				fraction = (double) Math.round(fraction * 10000) / 10000;

				final double fraction2 = (double) Math
						.round(fractions[thumbIndex] * 10000) / 10000;

				if (Helper.nearlyEqual(fraction, fraction2)
						|| thumbList.get(thumbIndex).isSelected()) {
					isPresent = true;
				}
			}

			return isPresent;
		}

		/**
		 * Inserts a thumb at a specific x-value and passes a color to the Thumb
		 * constructor.
		 * 
		 * @param x
		 * @param color
		 */
		private void insertThumbAt(final int x, final Color color) {

			int index = 0;
			for (final Thumb t : thumbList) {

				if (x > t.getX()) {
					index++;
				}
			}

			final int y = (int) (thumbRect.getHeight());
			thumbList.add(index, new Thumb(x, y, color));
		}

		/**
		 * Sets all thumbs' selection status to 'false'.
		 */
		protected void deselectAllThumbs() {

			for (final Thumb t : thumbList) {

				t.setSelected(false);
				selectedThumb = null;
			}
		}

		protected void updateThumbPos(final int inputX) {

			final float[] checkFracs = fractions.clone();

			if (selectedThumb != null
					&& inputX >= (int)thumbRect.getMinX() 
					&& inputX <= (int)thumbRect.getMaxX()) {
				// get position of previous thumb
				final int selectedIndex = thumbList.indexOf(selectedThumb);
				int previousPos = 0;
				int nextPos = (int)thumbRect.getMaxX();

				if (selectedIndex == 0) {
					nextPos = thumbList.get(selectedIndex + 1).getX();

				} else if (selectedIndex == thumbList.size() - 1) {
					previousPos = thumbList.get(selectedIndex - 1).getX();

				} else {
					previousPos = thumbList.get(selectedIndex - 1).getX();
					nextPos = thumbList.get(selectedIndex + 1).getX();
				}

				final int deltaX = inputX - selectedThumb.getX();
				final int newX = selectedThumb.getX() + deltaX;

				if (previousPos < newX && newX < nextPos) {
					selectedThumb.setCoords(newX, selectedThumb.getY());
					fractions = updateFractions();

				} else if (newX < previousPos 
						&& previousPos != thumbRect.getMinX()) {
					Collections.swap(thumbList, selectedIndex,
							selectedIndex - 1);
					Collections.swap(colorList, selectedIndex,
							selectedIndex - 1);
					selectedThumb.setCoords(newX, selectedThumb.getY());
					fractions = updateFractions();

				} else if (newX > nextPos && nextPos < thumbRect.getMaxX()) {
					Collections.swap(thumbList, selectedIndex,
							selectedIndex + 1);
					Collections.swap(colorList, selectedIndex,
							selectedIndex + 1);
					selectedThumb.setCoords(newX, selectedThumb.getY());
					fractions = updateFractions();
				}

				final boolean fracsOK = verifyFractions();

				if (!fracsOK) {
					LogBuffer.println("Fractions not ok. Original: "
							+ Arrays.toString(checkFracs) + "\n" + "New: "
							+ Arrays.toString(fractions));
				}

				setGradientColors();
				repaint();
			}
		}

		protected void specifyThumbPos(final Point point) {

			for (final Thumb t : thumbList) {

				if (t.contains((int) point.getX(), (int) point.getY())) {

					final JDialog positionInputDialog = new JDialog();
					positionInputDialog
							.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
					positionInputDialog
							.setDefaultCloseOperation(
									WindowConstants.DISPOSE_ON_CLOSE);
					positionInputDialog.setTitle("New Position");

					final JLabel enterPrompt = GUIFactory.createLabel(
							"Enter data value: ", GUIFactory.FONTS);

					final JTextField inputField = new JTextField();
					inputField.setEditable(true);

					final JButton okButton = GUIFactory.createBtn("OK");
					okButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent arg0) {

							try {
								final double inputDataValue = Double
										.parseDouble(inputField.getText());

								if (inputDataValue >= minVal
										&& inputDataValue <= maxVal) {

									final double fraction = Math
											.abs(inputDataValue - minVal)
											/ (maxVal - minVal);

									final int width = gradientBox.getWidth();
									final int inputXValue = (int) Math
											.round((fraction * width));

									gradientBox.updateThumbPos(inputXValue);
									positionInputDialog.dispose();

								} else {
									inputField.setText("Number out of range!");
								}

							} catch (final NumberFormatException e) {
								inputField.setText("Enter a number!");
							}
						}
					});

					final JPanel panel = GUIFactory.createJPanel(false, 
							GUIFactory.NO_PADDING, null);

					panel.add(enterPrompt, "push, span, wrap");
					panel.add(inputField, "push, growx, span, wrap");
					panel.add(okButton, "pushx, alignx 50%");

					positionInputDialog.getContentPane().add(panel);

					positionInputDialog.pack();
					positionInputDialog.setLocationRelativeTo(
							JFrame.getFrames()[0]);
					positionInputDialog.setVisible(true);
				}
			}

			setGradientColors();
			repaint();
		}

		// Fraction list Methods
		/**
		 * Calculates the fractions needed for the LinearGradient object to
		 * determine where the center of each color is displayed.
		 * 
		 * @return
		 */
		private float[] updateFractions() {

			final float[] fractions = new float[colorList.size()];

			int i = 0;
			for (final Thumb t : thumbList) {

				fractions[i] = (float) (t.getX() / gradientRect.getMaxX());
				i++;
			}

			return fractions;
		}

		/**
		 * Resets the fractions float[] to a default value with 3 colors.
		 * 
		 * @return
		 */
		private float[] resetFractions() {

			return new float[] { 0.0f, 0.5f, 1.0f };
		}
	}

	/**
	 * Returns the mainPanel which contains all the GUI components for the
	 * ColorGradientChooser.
	 * 
	 * @return
	 */
	public JPanel makeGradientPanel() {

		gradientBox.setPresets();
		return mainPanel;
	}

	/**
	 * Gives access to the GradientBox object which is the JPanel containing the
	 * actual color gradient and thumbs.
	 * 
	 * @return
	 */
	protected GradientBox getGradientBox() {

		return gradientBox;
	}

	/**
	 * Returns the current size of colorList in ColorGradientChooser.
	 * 
	 * @return
	 */
	protected int getColorListSize() {

		return colorList.size();
	}

	/**
	 * A class which describes a small triangular object used to define colors
	 * and color positions along the gradient box. This is what the user
	 * interacts with to define the color scheme for DendroView.
	 * 
	 * @author CKeil
	 * 
	 */
	private class Thumb {

		private int x;
		private int y;

		private final int width = 10;
		private final int height = 15;

		private GeneralPath innerthumbPath;
		private GeneralPath outerthumbPath;
		private Color thumbColor;
		private boolean selected = false;

		/**
		 * Constructs a thumb object if given the x/y-coordinates and a color.
		 * 
		 * @param x
		 * @param y
		 * @param color
		 */
		public Thumb(final int x, final int y, final Color color) {

			this.thumbColor = color;

			setCoords(x, y);
		}

		/**
		 * Sets the base x/y-coordinates for the thumb object. This is where it
		 * touches the gradientBox.
		 * 
		 * @param x
		 * @param y
		 */
		public void setCoords(final int x, final int y) {

			this.x = x;
			this.y = y;

			createThumbPath();
		}

		/**
		 * Uses the GeneralPath class and x/y-coordinates to generate a small
		 * triangular object which will represent an interactive 'thumb'.
		 */
		public void createThumbPath() {

			innerthumbPath = new GeneralPath();
			innerthumbPath.moveTo(x, y + height / 2);
			innerthumbPath.lineTo(x + width / 4, y - height);
			innerthumbPath.lineTo(x - width / 4, y - height);
			innerthumbPath.closePath();

			outerthumbPath = new GeneralPath();
			outerthumbPath.moveTo(x, y);
			outerthumbPath.lineTo(x + width / 2, y - height);
			outerthumbPath.lineTo(x - width / 2, y - height);
			outerthumbPath.closePath();
		}

		public void setSelected(final boolean selected) {

			this.selected = selected;

			if (selected) {
				selectedThumb = this;
			}
		}

		/**
		 * Paints the GeneralPath object with the set color and makes the thumb
		 * visible to the user.
		 * 
		 * @param g2d
		 */
		public void paint(final Graphics2D g2d) {

			if (isSelected()) {
				g2d.setColor(Color.red);

			} else {
				g2d.setColor(Color.black);
			}

			g2d.fill(outerthumbPath);

			g2d.setColor(thumbColor);
			g2d.fill(innerthumbPath);
		}

		/**
		 * Returns the base x-coordinate for the thumb where it contacts the
		 * gradientBox.
		 * 
		 * @return int
		 */
		public int getX() {

			return x;
		}

		/**
		 * Returns the base y-coordinate for the thumb where it contacts the
		 * gradientBox. Should equal the height of thumbBox because it sits on
		 * top of gradientBox and they directly touch.
		 * 
		 * @return
		 */
		public int getY() {

			return y;
		}

		/**
		 * Shows if the current thumb's selected status is true or not.
		 * 
		 * @return boolean
		 */
		public boolean isSelected() {

			return selected;
		}

		/**
		 * Provides the currently set color for its thumb object.
		 * 
		 * @return
		 */
		public Color getColor() {

			return thumbColor;
		}

		/**
		 * Provides the currently set color for its thumb object.
		 * 
		 * @return
		 */
		public void setColor(final Color newCol) {

			thumbColor = newCol;
		}

		/**
		 * Checks if this Thumb's GeneralPath object contains the specified x-
		 * and y-variable.
		 * 
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean contains(final int x, final int y) {

			return outerthumbPath.contains(x, y);
		}
	}

	// Accessors
	protected Preferences getConfigNode() {

		return configNode;
	}
	
	protected ColorExtractor getColorExtractor() {
		
		return colorExtractor;
	}
	
	protected JPanel getMainPanel() {
		
		return mainPanel;
	}

	protected ButtonGroup getButtonGroup() {

		return colorBtnGroup;
	}

	protected JRadioButton getRGButton() {

		return redGreenBtn;
	}

	protected JRadioButton getYBButton() {

		return yellowBlueBtn;
	}

	protected JRadioButton getCustomColorButton() {

		return customColorBtn;
	}

	public boolean isCustomSelected() {

		return isCustomSelected;
	}

	// Mutators
	/**
	 * Saves the current colors and fractions as a ColorSet to the configNode.
	 */
	public void saveStatus() {

		if (gradientBox != null) {
			gradientBox.saveCustomPresets();
		}
	}

	/**
	 * Sets button status and remembers if custom ColorSet is selected or not.
	 * @param selected Whether the custom ColorSet was selected or not.
	 */
	public void setCustomSelected(final boolean selected) {

		this.isCustomSelected = selected;

		addBtn.setEnabled(selected);
		removeBtn.setEnabled(selected);
		missingBtn.setEnabled(selected);
	}
	
	/**
	 * Switched the currently used ColorSet to the one that matches the
	 * specified entered name key in its 'ColorSet' configNode.
	 */
	public void switchColorSet(final String name) {

		setActiveColorSet(name);
		
		/* Load and set data accordingly */
		gradientBox.setPresets();
		gradientBox.setGradientColors();
	}

	/**
	 * Sets the activeColors key in configNode  
	 * @param name
	 */
	public void setActiveColorSet(final String name) {

		this.activeColorSet = colorPresets.getColorSet(name);
		configNode.put("activeColors", name);
	}

	protected void addColorSet(final ColorSet temp) {

		colorPresets.addColorSet(temp);
	}

	// Listeners
	protected void addThumbSelectListener(final MouseListener l) {

		gradientBox.addMouseListener(l);
	}

	protected void addThumbMotionListener(final MouseMotionListener l) {

		gradientBox.addMouseMotionListener(l);
	}

	protected void addAddListener(final ActionListener l) {

		addBtn.addActionListener(l);
	}

	protected void addRemoveListener(final ActionListener l) {

		removeBtn.addActionListener(l);
	}

	protected void addDefaultListener(final ActionListener l) {

		redGreenBtn.addActionListener(l);
		yellowBlueBtn.addActionListener(l);
		customColorBtn.addActionListener(l);
	}
	
	protected void addMissingListener(final ActionListener l) {
		
		missingBtn.addActionListener(l);
	}
}
