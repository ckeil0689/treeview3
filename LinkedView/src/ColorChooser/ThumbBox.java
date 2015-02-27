package ColorChooser;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import Utilities.GUIFactory;
import Utilities.Helper;

public class ThumbBox extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ColorPicker colorPicker;
	
	private final Rectangle2D thumbRect = new Rectangle2D.Float();
	
	/* Holds the currently selected thumb */
	private Thumb selectedThumb = null;
	
	public ThumbBox(ColorPicker colorPicker) {
		
		this.colorPicker = colorPicker;
		
		setToolTipText("This Turns Tooltips On");
		setFocusable(true);
	}
	
	@Override
	public void paintComponent(final Graphics g) {

		super.paintComponent(g);
		
		verifyThumbs();

		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		// Fill thumbRect with background color
		g2.setColor(GUIFactory.DEFAULT_BG);
		g2.fill(thumbRect);

		// Paint the thumbs
		for (final Thumb t : thumbList) {

			t.paint(g2);
		}
	}
	
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

				if (t.isSelected()) {
					selectedThumb = t;
				} else {
					selectedThumb = null;
				}
				break;

			} else {
				t.setSelected(false);
			}
		}

		repaint();
	}
	
	/**
	 * First checks if fraction array is in ascending order, then verifies thumb
	 * positions to match fractions. Returns a boolean so that the boxes won't
	 * be drawn in the paintComponent method, if the fractions aren't ascending.
	 * This would cause an exception with LinearGradientPaint.
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
		}
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

		/* stay within boundaries */
		if (selectedThumb == null
				|| (inputX < (int) thumbRect.getMinX() || inputX > (int) thumbRect
						.getMaxX()))
			return;

		/* get position of previous thumb */
		final int selectedIndex = thumbList.indexOf(selectedThumb);
		int previousPos = 0;
		int nextPos = (int) thumbRect.getMaxX();

		/* set positions around active thumb */
		if (selectedIndex == 0) {
			nextPos = thumbList.get(selectedIndex + 1).getX();

		} else if (selectedIndex == thumbList.size() - 1) {
			previousPos = thumbList.get(selectedIndex - 1).getX();

		} else {
			previousPos = thumbList.get(selectedIndex - 1).getX();
			nextPos = thumbList.get(selectedIndex + 1).getX();
		}

		/* get updated x-values */
		final int deltaX = inputX - selectedThumb.getX();
		final int newX = selectedThumb.getX() + deltaX;

		/* set new thumb position and check for boundaries/ other thumbs */
		if (previousPos < newX && newX < nextPos) {
			selectedThumb.setCoords(newX, selectedThumb.getY());
			fractions = updateFractions();

		} else if (newX < previousPos && previousPos != thumbRect.getMinX()) {
			Collections.swap(thumbList, selectedIndex, selectedIndex - 1);
			Collections.swap(colorList, selectedIndex, selectedIndex - 1);
			selectedThumb.setCoords(newX, selectedThumb.getY());
			fractions = updateFractions();
			updateColorArray();

		} else if (newX > nextPos && nextPos < thumbRect.getMaxX()) {
			Collections.swap(thumbList, selectedIndex, selectedIndex + 1);
			Collections.swap(colorList, selectedIndex, selectedIndex + 1);
			selectedThumb.setCoords(newX, selectedThumb.getY());
			fractions = updateFractions();
			updateColorArray();
		}

		setGradientColors();
		repaint();
	}

	protected void setThumbPosition(final Point point) {

		for (final Thumb t : thumbList) {

			if (t.contains((int) point.getX(), (int) point.getY())) {

				final JDialog positionInputDialog = new JDialog();
				positionInputDialog
				.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
				positionInputDialog
				.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				positionInputDialog.setTitle("New Position");

				final JLabel enterPrompt = GUIFactory.createLabel(
						"Enter data value: ", GUIFactory.FONTS);

				final JTextField inputField = new JTextField();
				inputField.setEditable(true);

				/* Initially display thumb position */
				inputField.setText(Double.toString(getThumbPosition(t)));

				final JButton okButton = GUIFactory.createBtn("OK");
				okButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent arg0) {

						try {
							final double inputDataValue = Double
									.parseDouble(inputField.getText());

							if (inputDataValue >= minVal
									&& inputDataValue <= maxVal) {

								final double fraction = 
										Math.abs(inputDataValue- minVal)
										/ (maxVal - minVal);

								final int inputXValue = (int) Math
										.round((fraction * WIDTH));

								updateThumbPos(inputXValue);
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
						GUIFactory.DEFAULT, null);

				panel.add(enterPrompt, "push, span, wrap");
				panel.add(inputField, "push, growx, span, wrap");
				panel.add(okButton, "pushx, alignx 50%");

				positionInputDialog.getContentPane().add(panel);

				positionInputDialog.pack();
				positionInputDialog.setLocationRelativeTo(Frame.getFrames()[0]);
				positionInputDialog.setVisible(true);
			}
		}

		setGradientColors();
		repaint();
	}
	
	/**
	 * 
	 * @return The Rectangle object used to display the thumbs.
	 */
	protected Rectangle2D getThumbRect() {
		
		return thumbRect;
	}
	
	/**
	 * Finds out which thumb in thumbList is currently selected and returns it.
	 * @return The index of the selected thumb in thumbList.
	 */
	protected int getSelectedThumbIndex() {
		
		int selectedIndex = 0;
		if (selectedThumb != null) {
			selectedIndex = thumbList.indexOf(selectedThumb);
		}

		if (thumbList.get(selectedIndex).getX() == thumbList.get(
				thumbList.size() - 1).getX()) {
			selectedIndex--;
		}
		
		return selectedIndex;
	}
}


