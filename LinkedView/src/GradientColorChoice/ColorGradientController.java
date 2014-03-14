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

public class ColorGradientController {

	private ColorGradientChooser gradientPick;
	
	public ColorGradientController(ColorGradientChooser gradientPick) {
		
		this.gradientPick = gradientPick;
		
		// Add listeners
		gradientPick.addColorListener(new ColorListener());
		gradientPick.addThumbMotionListener(new ThumbMotionListener());
		gradientPick.addAddListener(new AddButtonListener());
		gradientPick.addDefaultListener(new DefaultListener());
	}
	
	protected class ColorListener implements MouseListener, ActionListener {
		
		private final Integer clickInterval = 
				(Integer) Toolkit.getDefaultToolkit().getDesktopProperty(
						"awt.multiClickInterval");
		
		private Timer timer;
		private MouseEvent lastEvent;
		
		protected ColorListener() {
			
			timer = new Timer(clickInterval, this);
		}
		
		/**
		 * Specifies what happens when a single click is performed by the user.
		 */
		private void singleClick() {
	
			gradientPick.getGradientBox().setGradientColor(
					lastEvent.getPoint());
		}
		
		/**
		 * Specifies what happens when a double click is performed by the user.
		 */
		private void doubleClick() {
			
			if(gradientPick.getColorListSize() > 2) {
				gradientPick.getGradientBox().removeThumbAt(lastEvent.getX(), 
						lastEvent.getY());
				gradientPick.getGradientBox().removeColor();
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
			
			gradientPick.getGradientBox().selectThumb(e.getX(), e.getY());
			gradientPick.getGradientBox().repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			
			gradientPick.getGradientBox().deselectAllThumbs();
			gradientPick.getGradientBox().repaint();
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			timer.stop();
			singleClick();
		}
	}
	
	class ThumbMotionListener implements MouseMotionListener {
		
		@Override
		public void mouseDragged(MouseEvent e) {
			
			gradientPick.getGradientBox().updateThumbPos(e.getX());
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			
			if((e.getX() > 0 && e.getX() 
					< gradientPick.getGradientBox().getWidth())
					&& (e.getY() > 0 && e.getY() 
							< gradientPick.getGradientBox().getHeight() 
							* 1/4)) {
				gradientPick.getGradientBox().setCursor(
						new Cursor(Cursor.HAND_CURSOR));
				
			} else {
				gradientPick.getGradientBox().setCursor(
						new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}
	
	class AddButtonListener implements ActionListener {

		private int addIndex = -1;
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			Color newCol = JColorChooser.showDialog(
					gradientPick.getGradientBox(), "Pick a Color", 
					Color.black);
			
			int x = findBestX();
			
			gradientPick.getGradientBox().insertThumbAt(x, newCol);
			
			if(addIndex > -1) {
				gradientPick.getGradientBox().addColor(newCol, addIndex);
				
			} else {
				System.out.println("No useful index set to add a color" +
						"to color list. Location: ColorGradientController" +
						" AddButtonListener.");
			}
		}
		
		public int findBestX() {
			
			float[] fractions = gradientPick.getGradientBox().updateFractions();
			
			int x = -1;
			float bigDiff = 0.0f;

			for(int i = 0; i < fractions.length - 1; i++) {
				
				float diff = fractions[i + 1] - fractions[i];
				if(diff > bigDiff) {
					bigDiff = diff;
					x = (int)((bigDiff/2 + fractions[i]) 
							* gradientPick.getGradientBox().getSize()
							.getWidth());
					addIndex = i + 1;
				}
			}
			
			return x;
		}
	}
	
	class RemoveButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			gradientPick.getGradientBox().removeColor();
		}
	}
	
	class DefaultListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			if(arg0.getSource() == gradientPick.getRGButtton()) {
				gradientPick.getGradientBox().setDefaults(true);
				
			} else {
				gradientPick.getGradientBox().setDefaults(false);
			}
		}
		
	}
}
