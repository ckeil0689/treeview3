package GradientColorChoice;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.GUIParams;

import net.miginfocom.swing.MigLayout;

public class ColorGradientChooser {

	private JPanel mainPanel;
	
	private GradientBox gradientBox;
	
	private Color[] colors;
	private float[] fractions;
	private ArrayList<Color> colorList;
	
	private final Color RG_DEFAULT_PRIMARY = Color.red;
	private final Color YB_DEFAULT_PRIMARY = Color.yellow;
	
	private final Color DEFAULT_SECONDARY = Color.black;
	
	private final Color RG_DEFAULT_TERTIARY = Color.green;
	private final Color YB_DEFAULT_TERTIARY = Color.blue;
	
	private JButton addButton;
	private JButton removeButton;
	private JButton redGreenButton;
	private JButton yellowBlueButton;
	
	public ColorGradientChooser() {
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.BG_COLOR);
		
		colorList = new ArrayList<Color>();
		
		colorList.add(RG_DEFAULT_PRIMARY);
		colorList.add(DEFAULT_SECONDARY);
		colorList.add(RG_DEFAULT_TERTIARY);
		
		gradientBox = new GradientBox();
		
		addButton = GUIParams.setButtonLayout("Add Color", null);
		removeButton = GUIParams.setButtonLayout("Remove Color", null);
		
		redGreenButton = GUIParams.setButtonLayout("Set RedGreen", null);
		yellowBlueButton = GUIParams.setButtonLayout("Set YellowBlue", null);
		
		mainPanel.add(gradientBox, "h 10%, growx, pushx, alignx 50%, " +
				"span, wrap");
		mainPanel.add(addButton);
		mainPanel.add(removeButton, "wrap");
		mainPanel.add(redGreenButton, "span, wrap");
		mainPanel.add(yellowBlueButton, "span, wrap");
	}
	
	protected class GradientBox extends JPanel {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void paintComponent(Graphics g) {
			
			super.paintComponent(g);
			
			float size = (float)(getPreferredSize().getWidth()
					/ colorList.size());
			float distance = (float)(size/ getPreferredSize().getWidth());
			
			fractions = new float[colorList.size()];
			float addFloat = 0;
			// Equal size of all colors
			for(int i = 0; i < colorList.size(); i++) {
				
				fractions[i] = addFloat;
				addFloat += distance;
			}
			
			System.out.println("Fractions: " + Arrays.toString(fractions));
			
			int width = (int)getSize().getWidth();
			int height = (int)getSize().getHeight();
			
			colors = new Color[colorList.size()];
			
			for(int i = 0; i < colors.length; i++) {
				
				colors[i] = colorList.get(i);
			}
			
			Graphics2D g2 = (Graphics2D) g;
			
			double endXPoint = getSize().getWidth();
			Point2D start = new Point2D.Float(0, 0);
			Point2D end = new Point2D.Float((float)endXPoint, 0);
			
			LinearGradientPaint gradient = new LinearGradientPaint(start, end, 
					fractions, colors, CycleMethod.NO_CYCLE);
				
			g2.setPaint(gradient);
			g2.fillRect(0, 0, width, height);
			
			g2.dispose();
		}
		
		/**
		 * Adds a color to the gradient.
		 * @param newCol
		 */
		public void addColor(Color newCol) {
			
			if(colorList.size() == 2) {
				colorList.add(1, newCol);
			}
			repaint();
		}
		
		/** 
		 * Removes a color from the gradient.
		 */
		public void removeColor() {
			
			if(colorList.size() == 3) {
				colorList.remove(1);
			}
			
			repaint();
		}
		
		public void setGradientColor(Color newCol, Point point) {
			
			double xCoord = point.getX();
			double width = getSize().getWidth();
			double part = xCoord/ width;
			
			int index = -1;
			for(int i = 0; i < fractions.length; i++) {
				
				if(part > fractions[i]) {
					index++;
				}
			}
			
			if(index > -1) {
				colorList.set(index, newCol);
			}
			
			mainPanel.repaint();
		}
	}
	
	public JPanel makeGradientPanel() {
		
		return mainPanel;
	}
	
	public GradientBox getGradientBox() {
		
		return gradientBox;
	}
	
	/**
	 * Resets the color values to default.
	 * @param redGreen
	 */
	public void setDefaultColors(boolean redGreen) {
		
		colorList.clear();
		
		if(redGreen) {
			colorList.add(RG_DEFAULT_PRIMARY);
			colorList.add(DEFAULT_SECONDARY);
			colorList.add(RG_DEFAULT_TERTIARY);
			
		} else {
			colorList.add(YB_DEFAULT_PRIMARY);
			colorList.add(DEFAULT_SECONDARY);
			colorList.add(YB_DEFAULT_TERTIARY);
		}
		
		mainPanel.repaint();
	}
	
	// Button Getters
	public JButton getRGButtton() {
		
		return redGreenButton;
	}
	
	public JButton getYBButton() {
		
		return yellowBlueButton;
	}
	
	// Listeners
	public void addColorListener(MouseListener l) {
		
		gradientBox.addMouseListener(l);
	}
	
	public void addAddListener(ActionListener l) {
		
		addButton.addActionListener(l);
	}
	
	public void addRemoveListener(ActionListener l) {
		
		removeButton.addActionListener(l);
	}
	
	public void addDefaultListener(ActionListener l) {
		
		redGreenButton.addActionListener(l);
		yellowBlueButton.addActionListener(l);
	}
}
