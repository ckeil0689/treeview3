package GradientColorChoice;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JColorChooser;

public class ColorGradientController {

	private ColorGradientChooser gradientPick;
	
	public ColorGradientController(ColorGradientChooser gradientPick) {
		
		this.gradientPick = gradientPick;
		
		// Add listeners
		gradientPick.addColorListener(new ColorListener());
		gradientPick.addAddListener(new AddButtonListener());
		gradientPick.addRemoveListener(new RemoveButtonListener());
		gradientPick.addDefaultListener(new DefaultListener());
	}
	
	class ColorListener implements MouseListener {
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
			
			Color newCol = JColorChooser.showDialog(
					gradientPick.getGradientBox(), "Pick a Color", Color.black);
			gradientPick.getGradientBox().setGradientColor(newCol, 
					arg0.getPoint());
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			
			gradientPick.getGradientBox().setCursor(
					new Cursor(Cursor.HAND_CURSOR));
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			
			gradientPick.getGradientBox().setCursor(
					new Cursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		public void mousePressed(MouseEvent arg0) {}

		@Override
		public void mouseReleased(MouseEvent arg0) {}
	}
	
	class AddButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			gradientPick.getGradientBox().addColor(
					JColorChooser.showDialog(gradientPick.getGradientBox(), 
							"Pick a Color", Color.black));
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
				gradientPick.setDefaultColors(true);
				
			} else {
				gradientPick.setDefaultColors(false);
			}
		}
		
	}
}
