package GradientColorChoice;

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
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet2;

public class ColorGradientController {

	private ColorGradientChooser gradientPick;
	
	public ColorGradientController(ColorGradientChooser gradientPick) {
		
		this.gradientPick = gradientPick;
		
		// Add listeners
		addAllListeners();
	}
	
	/**
	 * Adds all listeners to the ColorGradientChooser object.
	 */
	public void addAllListeners() {
		
		if(gradientPick != null) {
			gradientPick.addThumbSelectionListener(
					new ThumbSelectionListener());
			gradientPick.addThumbMotionListener(new ThumbMotionListener());
			gradientPick.addAddListener(new AddButtonListener());
			gradientPick.addRemoveListener(new RemoveButtonListener());
			gradientPick.addDefaultListener(new DefaultListener());
		}
	}
	
	/**
	 * This listener specifically defines what happens when a user clicks
	 * on a thumb.
	 * @author CKeil
	 *
	 */
	protected class ThumbSelectionListener implements MouseListener, 
	ActionListener {
		
		private final Integer clickInterval = 
				(Integer) Toolkit.getDefaultToolkit().getDesktopProperty(
						"awt.multiClickInterval");
		
		private Timer timer;
		private MouseEvent lastEvent;
		
		protected ThumbSelectionListener() {
			
			timer = new Timer(clickInterval, this);
		}
		
		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void clickOrPress() {
	
			if(gradientPick.isCustomSelected()) {
				if(gradientPick.getGradientBox().isGradientArea(
						lastEvent.getPoint())) {
					gradientPick.getGradientBox().setGradientColor(
							lastEvent.getPoint());
					gradientPick.setActiveColorSet("Custom");
					
				} else {
					gradientPick.getGradientBox().deselectAllThumbs();
					gradientPick.getGradientBox().selectThumb(lastEvent.getPoint());
				}
			}
		}
		
		/**
		 * Specifies what happens when a double click is performed by the user.
		 */
		private void doubleClick() {
			
			if(gradientPick.isCustomSelected()) {
				gradientPick.getGradientBox().specifyThumbPos(
						lastEvent.getPoint());
			}
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
			lastEvent = e;
			
			if(timer.isRunning()) {
				timer.stop();
				
				if(e.getClickCount() >= 2) {
					doubleClick();
				}
			} else {
				timer.restart();	
			} 
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {
			
			lastEvent = e;
			clickOrPress();
		}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			timer.stop();
			clickOrPress();
		}
	}
	
	/**
	 * This listener defines the behavior of ColorGradientChooser when
	 * the user moves the mouse on top a thumb or 
	 * drags it in a certain direction.
	 * @author CKeil
	 *
	 */
	class ThumbMotionListener implements MouseMotionListener {
		
		@Override
		public void mouseDragged(MouseEvent e) {
			
			if(gradientPick.isCustomSelected()) {
				gradientPick.getGradientBox().updateThumbPos(e.getX());
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			
			if(gradientPick.isCustomSelected()) {
				if(gradientPick.getGradientBox().containsThumb(e.getX(), 
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
		public void actionPerformed(ActionEvent arg0) {
			
			Color newCol = JColorChooser.showDialog(
					gradientPick.getGradientBox(), "Pick a Color", 
					Color.black);
			
			gradientPick.getGradientBox().addColor(newCol);
		}
	}
	
	class RemoveButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if(gradientPick.getColorListSize() > 2) {
				gradientPick.getGradientBox().removeColor();
			}
		}
	}
	
	class DefaultListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if(arg0.getSource() == gradientPick.getRGButton()) {
				gradientPick.switchColorSet("RedGreen");
				gradientPick.setCustomSelected(false);
				
			} else if(arg0.getSource() == gradientPick.getYBButton()){
				gradientPick.switchColorSet("YellowBlue");
				gradientPick.setCustomSelected(false);
				
			} else if(arg0.getSource() == gradientPick.getCustomColorButton()){
				gradientPick.switchColorSet("Custom");
				gradientPick.setCustomSelected(true);
				
			} else {
				LogBuffer.println("No source found for ActionEvent in " +
						"DefaultListener in ColorGradientController");
			}
		}
	}
	
	class SaveColorPresetListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			final ColorSet2 temp = new ColorSet2();
//			colorExtractorEditor.copyStateTo(temp);
			temp.setName("UserDefined");
			gradientPick.addColorSet(temp);	
		}
		
	}
}
