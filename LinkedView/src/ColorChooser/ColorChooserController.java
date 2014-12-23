package ColorChooser;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JColorChooser;
import javax.swing.Timer;

public class ColorChooserController {

	public static final Integer DEFAULT_MULTI_CLICK_INTERVAL = 300;
	private final ColorChooser gradientPick;
	private final GradientBox gradientBox;

	public ColorChooserController(final ColorChooser gradientPick) {

		this.gradientPick = gradientPick;
		this.gradientBox = gradientPick.getGradientBox();

		addAllListeners();
	}

    /**
     * Returns the system multi-click interval.
     */
    public static int getMultiClickInterval() {
        Integer multiClickInterval = (Integer) Toolkit.getDefaultToolkit()
        		.getDesktopProperty("awt.multiClickInterval");

        if (multiClickInterval == null) {
            multiClickInterval = DEFAULT_MULTI_CLICK_INTERVAL;
        }

        return multiClickInterval;
    }
    
	/**
	 * Adds all defined listeners to the ColorGradientChooser object.
	 */
	private void addAllListeners() {

		if (gradientPick != null) {
			gradientPick.addThumbSelectListener(new ThumbSelectListener());
			gradientPick.addThumbMotionListener(new ThumbMotionListener());
			gradientPick.addAddListener(new AddButtonListener());
			gradientPick.addRemoveListener(new RemoveButtonListener());
			gradientPick.addColorSetListener(new ColorSetListener());
			gradientPick.addMissingListener(new MissingBtnListener());
		}
	}

	/**
	 * This listener specifically defines what happens when a user clicks on a
	 * thumb.
	 * 
	 * @author CKeil
	 * 
	 */
	private class ThumbSelectListener implements MouseListener,
			ActionListener {

		private final Timer timer;
		private MouseEvent lastEvent;

		protected ThumbSelectListener() {
			timer = new Timer(ColorChooserController.getMultiClickInterval(), 
					this);
		}

		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void clickOrPress() {

			if (gradientPick.isCustomSelected()) {
				if (gradientBox.isGradientArea(lastEvent.getPoint())) {
					gradientBox.changeColor(lastEvent.getPoint());
					gradientPick.setActiveColorSet("Custom");

				} else {
					gradientBox.deselectAllThumbs();
					gradientBox.selectThumb(lastEvent.getPoint());
				}
			}
		}

		/**
		 * Specifies what happens when a double click is performed by the user.
		 */
		private void doubleClick() {

			if (gradientPick.isCustomSelected()) {
				gradientBox.setThumbPos(lastEvent.getPoint());
			}
		}

		@Override
		public void mouseClicked(final MouseEvent e) {

			lastEvent = e;

			if (timer.isRunning()) {
				timer.stop();

				if (e.getClickCount() >= 2) {
					doubleClick();
				}
			} else {
				timer.restart();
			}
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
		}

		@Override
		public void mouseExited(final MouseEvent e) {
		}

		@Override
		public void mousePressed(final MouseEvent e) {

			lastEvent = e;
			clickOrPress();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			timer.stop();
			clickOrPress();
		}
	}

	/**
	 * This listener defines the behavior of ColorGradientChooser when the user
	 * moves the mouse on top a thumb or drags it in a certain direction.
	 * 
	 * @author CKeil
	 * 
	 */
	private class ThumbMotionListener implements MouseMotionListener {

		@Override
		public void mouseDragged(final MouseEvent e) {

			if (gradientPick.isCustomSelected()) {
				gradientBox.updateThumbPos(e.getX());
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {

			if (gradientPick.isCustomSelected()) {
				if (gradientBox.containsThumb(e.getX(), e.getY())) {
					gradientBox.setCursor(new Cursor(Cursor.HAND_CURSOR));

				} else {
					gradientBox.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}
	}

	/**
	 * Adds a user-selected color to the colorList in the gradientBox.
	 * @author chris0689
	 *
	 */
	private class AddButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final Color newCol = JColorChooser.showDialog(
					gradientBox, "Pick a Color", Color.black);
			
			if(newCol != null) {
				gradientBox.addColor(newCol);
			}
		}
	}

	/**
	 * Removes a color from colorList in the gradientBox.
	 * @author chris0689
	 *
	 */
	private class RemoveButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if (gradientBox.getColorListSize() > 2) {
				gradientBox.removeColor();
			}
		}
	}

	/**
	 * Radio-button controls over which ColorSet is active.
	 * @author chris0689
	 *
	 */
	private class ColorSetListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			boolean isCustom = gradientPick.isCustomSelected();
			String colorSetName = "";
			
			/* Save if switching from 'Custom' */
			if (isCustom) gradientPick.saveStatus();
			
			if (arg0.getSource() == gradientPick.getRGButton()) {
				/* Switch to RedGreen */
				colorSetName = "RedGreen";
				isCustom = false;

			} else if (arg0.getSource() == gradientPick.getYBButton()) {
				/* Switch to YellowBlue */
				colorSetName = "YellowBlue";
				isCustom = false;

			} else {
				/* Switch to Custom */
				colorSetName = "Custom";
				isCustom = true;
			}
			
			gradientPick.switchColorSet(colorSetName);
			gradientPick.setCustomSelected(isCustom);
		}
	}
	
	private class MissingBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (gradientPick.isCustomSelected()) {
				final Color missing = JColorChooser.showDialog(
						gradientPick.getMainPanel(), "Pick Color for Missing", 
						gradientBox.getMissing());
				
				if (missing != null) {
					gradientBox.setMissing(missing);
				}
			}
		}
	}
}
