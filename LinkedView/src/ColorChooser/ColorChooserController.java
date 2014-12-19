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

import edu.stanford.genetics.treeview.LogBuffer;

public class ColorChooserController {

	public static final Integer DEFAULT_MULTI_CLICK_INTERVAL = 300;
	private final ColorChooser gradientPick;

	public ColorChooserController(final ColorChooser gradientPick) {

		this.gradientPick = gradientPick;

		// Add listeners
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
	 * Adds all listeners to the ColorGradientChooser object.
	 */
	public void addAllListeners() {

		if (gradientPick != null) {
			gradientPick
					.addThumbSelectionListener(new ThumbSelectionListener());
			gradientPick.addThumbMotionListener(new ThumbMotionListener());
			gradientPick.addAddListener(new AddButtonListener());
			gradientPick.addRemoveListener(new RemoveButtonListener());
			gradientPick.addDefaultListener(new DefaultListener());
		}
	}

	/**
	 * This listener specifically defines what happens when a user clicks on a
	 * thumb.
	 * 
	 * @author CKeil
	 * 
	 */
	protected class ThumbSelectionListener implements MouseListener,
			ActionListener {

		private final Timer timer;
		private MouseEvent lastEvent;

		protected ThumbSelectionListener() {
			timer = new Timer(ColorChooserController.getMultiClickInterval(), this);
		}

		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void clickOrPress() {

			if (gradientPick.isCustomSelected()) {
				if (gradientPick.getGradientBox().isGradientArea(
						lastEvent.getPoint())) {
					gradientPick.getGradientBox().setGradientColor(
							lastEvent.getPoint());
					gradientPick.setActiveColorSet("Custom");

				} else {
					gradientPick.getGradientBox().deselectAllThumbs();
					gradientPick.getGradientBox().selectThumb(
							lastEvent.getPoint());
				}
			}
		}

		/**
		 * Specifies what happens when a double click is performed by the user.
		 */
		private void doubleClick() {

			if (gradientPick.isCustomSelected()) {
				gradientPick.getGradientBox().specifyThumbPos(
						lastEvent.getPoint());
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
	class ThumbMotionListener implements MouseMotionListener {

		@Override
		public void mouseDragged(final MouseEvent e) {

			if (gradientPick.isCustomSelected()) {
				gradientPick.getGradientBox().updateThumbPos(e.getX());
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {

			if (gradientPick.isCustomSelected()) {
				if (gradientPick.getGradientBox().containsThumb(e.getX(),
						e.getY())) {
					gradientPick.getGradientBox().setCursor(
							new Cursor(Cursor.HAND_CURSOR));

				} else {
					gradientPick.getGradientBox().setCursor(
							new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}
	}

	class AddButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			final Color newCol = JColorChooser.showDialog(
					gradientPick.getGradientBox(), "Pick a Color", Color.black);

			gradientPick.getGradientBox().addColor(newCol);
		}
	}

	class RemoveButtonListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if (gradientPick.getColorListSize() > 2) {
				gradientPick.getGradientBox().removeColor();
			}
		}
	}

	class DefaultListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if (arg0.getSource() == gradientPick.getRGButton()) {
				if (gradientPick.getConfigNode()
						.get("activeColors", "RedGreen")
						.equalsIgnoreCase("Custom")) {
					gradientPick.saveStatus();
				}
				gradientPick.switchColorSet("RedGreen");
				gradientPick.setCustomSelected(false);

			} else if (arg0.getSource() == gradientPick.getYBButton()) {
				if (gradientPick.getConfigNode()
						.get("activeColors", "YellowBlue")
						.equalsIgnoreCase("Custom")) {
					gradientPick.saveStatus();
				}
				gradientPick.switchColorSet("YellowBlue");
				gradientPick.setCustomSelected(false);

			} else if (arg0.getSource() == gradientPick.getCustomColorButton()) {
				gradientPick.switchColorSet("Custom");
				gradientPick.setCustomSelected(true);

			} else {
				LogBuffer.println("No source found for ActionEvent in "
						+ "DefaultListener in ColorGradientController");
			}
		}
	}
	
	class MissingBtnListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final Color trial = JColorChooser.showDialog(
					gradientPick.getMainPanel(), "Pick Color for Missing", 
					gradientPick.getColorExtractor().getMissing());
			
			if (trial != null) {
				gradientPick.getColorExtractor().setMissingColor(trial);
				gradientPick.getColorExtractor().notifyObservers();
			}
		}
	}

	class SaveColorPresetListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			//final ColorSet temp = new ColorSet();
			// colorExtractorEditor.copyStateTo(temp);
			//temp.setName("UserDefined");
			//gradientPick.addColorSet(temp);
		}

	}
}
