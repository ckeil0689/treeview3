package GradientColorChoice;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets;

import net.miginfocom.swing.MigLayout;

public class ColorGradientChooser {

	private JPanel mainPanel;
	
	private GradientBox gradientBox;
	
	private Color[] colors;
	private Insets insets = new Insets(10, 10, 10, 10);
	private float[] fractions = {0.0f, 0.5f, 1.0f};
	
	private ArrayList<Color> colorList;
	private ArrayList<Thumb> thumbList;
	
	private final Color RG_DEFAULT_PRIMARY = Color.red;
	private final Color YB_DEFAULT_PRIMARY = Color.yellow;
	
	private final Color DEFAULT_SECONDARY = Color.black;
	
	private final Color RG_DEFAULT_TERTIARY = Color.green;
	private final Color YB_DEFAULT_TERTIARY = Color.blue;
	
	private JButton addButton;
	private JButton redGreenButton;
	private JButton yellowBlueButton;
	
	private ColorExtractor colorExtractor;
	private ColorPresets colorPresets;
	
	private Thumb selectedThumb = null;
	
	public ColorGradientChooser(final ColorExtractor drawer,
			final ColorPresets colorPresets, JPanel parent) {
		
		this.colorExtractor = drawer;
		this.colorPresets = colorPresets;
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.LIGHTGRAY);
		
		colorList = new ArrayList<Color>();
		thumbList = new ArrayList<Thumb>();
		
		colorList.add(RG_DEFAULT_PRIMARY);
		colorList.add(DEFAULT_SECONDARY);
		colorList.add(RG_DEFAULT_TERTIARY);
		
		gradientBox = new GradientBox();
		
		addButton = GUIParams.setButtonLayout("Add Color", null);
		
		redGreenButton = GUIParams.setButtonLayout("Default", null);
		yellowBlueButton = GUIParams.setButtonLayout("Color Blind", null);
		
		mainPanel.add(gradientBox, "h 20%, growx, pushx, alignx 50%, " +
				"span, wrap");
		mainPanel.add(addButton, "wrap");
		mainPanel.add(redGreenButton);
		mainPanel.add(yellowBlueButton);
	}
	
	/**
	 * A special JPanel that represents a gradient colored box.
	 * It has a MouseListener attached (via the controller class) which
	 * handles user input and allows for the change of the color in the 
	 * clicked area.
	 * @author CKeil
	 *
	 */
	protected class GradientBox extends JPanel {

		private static final long serialVersionUID = 1L;
		
		private Rectangle2D gradientRect = new Rectangle2D.Float();
		private Rectangle2D thumbRect = new Rectangle2D.Float();
		
		public GradientBox() {
			
			setFocusable(true);
		}
		
		@Override
		public void paintComponent(Graphics g) {
			
			super.paintComponent(g);
			
			int width = (int)getSize().getWidth();
			int height = (int)getSize().getHeight();
			
			setupRects(width, height);
			
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			updateThumbs();
			
			drawThumbBox(g2);
			drawGradientBox(g2);
			
			g2.dispose();
		}
		
		public void setupRects(int width, int height) {
			
			gradientRect.setRect(0, 0, width, height * 3/4);
			thumbRect.setRect(0, 0, width, height * 1/4);
		}
		
		public void drawGradientBox(Graphics2D g2) {

			// Dimensions 
			float startX = (float)gradientRect.getX();
			float startY = (float)gradientRect.getY();
			
			float endX = (float)gradientRect.getWidth() - startX;
			float endY = (float)gradientRect.getHeight() - startY;
			
			colors = new Color[colorList.size()];
			
			for(int i = 0; i < colors.length; i++) {
				
				colors[i] = colorList.get(i);
			}
			
			// Generating Gradient to fill the rectangle with
			LinearGradientPaint gradient = new LinearGradientPaint(startX, 
					startY, endX, endY, fractions, colors, 
					CycleMethod.NO_CYCLE);
				
			g2.setPaint(gradient);
			g2.fillRect(0, (int)thumbRect.getHeight(), 
					(int)gradientRect.getWidth(), 
					(int)gradientRect.getHeight());
		}
		
		public void drawThumbBox(Graphics2D g2) {
			
			// Fill thumbRect with background color
			g2.setColor(GUIParams.LIGHTGRAY);
			g2.fill(thumbRect);
			
			// Paint the thumbs
			for(Thumb t : thumbList) {
				
				t.paint(g2);
			}
		}
		
		/**
		 * Adds a color to the gradient.
		 * @param newCol
		 */
		public void addColor(Color newCol, int index) {
			
			colorList.add(index, newCol);
			fractions = updateFractions();
			
//			setColor(newCol, index);
			repaint();
		}
		
		/** 
		 * Removes a color from the gradient.
		 */
		public void removeColor() {
			
			fractions = updateFractions();
			repaint();
		}
		
		/**
		 * Changes the gradient color in the area the mouse was clicked in.
		 * @param newCol
		 * @param point
		 */
		public void setGradientColor(Point point) {
			
			Color newCol = null;
			int index = 0;
			for(Thumb t : thumbList) {
				
				if(t.contains((int)point.getX(), (int)point.getY())) {
					newCol = JColorChooser.showDialog(this, "Pick a Color", 
							t.getColor());
					t.setColor(newCol);
					break;
				}
				index++;
			}
		
			if(newCol != null) {
				colorList.set(index, newCol);
//				setColor(newCol, index);
				mainPanel.repaint();	
			}
		}
		
		/**
		 * Sets the color in colors[] at the specified index.
		 * @param newCol
		 * @param index
		 */
		public void setColor(Color newCol, int index) {
			
			switch(index) {
			
			case 0: colorExtractor.setUpColor(newCol);
					break;
					
			case 1: colorExtractor.setZeroColor(newCol);
					break;
					
			case 2: colorExtractor.setDownColor(newCol);
					break;
					
			default: colorExtractor.setMissingColor(Color.gray);
			}
			
			colorExtractor.notifyObservers();
		}
		
		/**
		 * Resets the color values to default.
		 * @param redGreen
		 */
		public void setDefaults(boolean redGreen) {
			
			colorList.clear();
			thumbList.clear();
			
			if(redGreen) {
				colorList.add(RG_DEFAULT_PRIMARY);
				colorList.add(DEFAULT_SECONDARY);
				colorList.add(RG_DEFAULT_TERTIARY);
				
			} else {
				colorList.add(YB_DEFAULT_PRIMARY);
				colorList.add(DEFAULT_SECONDARY);
				colorList.add(YB_DEFAULT_TERTIARY);
			}
			
			for(int i = 0; i < colorList.size(); i++) {
				
				setColor(colorList.get(i), i);
			}
			
			fractions = resetFractions();
			
			mainPanel.repaint();
		}
		
		/**
		 * Sets thumbs evenly depending on the amounts of colors currently
		 * added to the gradient.
		 */
		public void updateThumbs() {
			
			int x = (int)thumbRect.getX();
			int w = (int)thumbRect.getWidth(); 
			
			for (int i = 0; i < fractions.length; i++) {  
	            
				int pos = x + (int) (w * fractions[i]);
				
				if(!checkThumbPos(pos)) {
					insertThumbAt(pos, colorList.get(i));  
				}
	        }
			
			System.out.println("thumblist size: " + thumbList.size());
		}
		
		/**
		 * Checks if a thumb is present at the given x-position.
		 * @param pos
		 * @return
		 */
		public boolean checkThumbPos(int pos) {
			
			boolean isPresent = false;
			
			for(Thumb t : thumbList) {
				
				if(t.getX() == pos || t.isSelected()) {
					isPresent = true;
					break;
				}
			}
			return isPresent;
		}
		
		/**
		 * Inserts a thumb at a specific x-value and passes a color to
		 * the Thumb constructor.
		 * @param x
		 * @param color
		 */
		public void insertThumbAt(int x, Color color) {  
		      
			int index = 0;  
			for (Thumb t : thumbList) {  
			
				if (x > t.getX()) {  
					index++;  
				}  
			}  
			
			int y = (int) (thumbRect.getHeight());  
			thumbList.add(index, new Thumb(x, y, color));
		}
		
		/**
		 * Removes a thumb if its GeneralPath contains the specified x- 
		 * and y-coordinates. Also removes the color at the same 
		 * index in colorList so that getFractions() can properly calculate 
		 * the new fractions. 
		 * @param x
		 * @param y
		 */
		public void removeThumbAt(int x, int y) {
			
			int index = 0;
			for(Thumb t : thumbList) {
				
				if(t.contains(x, y)) {
					thumbList.remove(t);
					colorList.remove(index);
				}
				index++;
			}
		}
		
		
		public void selectThumb(int x, int y) {
			
			for(Thumb t : thumbList) {
				
				if(t.contains(x, y)) {
					t.setSelected(true);
					selectedThumb = t;
					break;
					
				} else if(!t.contains(x, y) && t.isSelected() == true) {
					t.setSelected(false);
				}
			}
			repaint();
		}
		
		/**
		 * Sets all thumbs' selection status to 'false'.
		 */
		public void deselectAllThumbs() {
			
			for(Thumb t : thumbList) {
				
				t.setSelected(false);
				selectedThumb = null;
			}
		}
		
		public void updateThumbPos(int mouseX) {
			
			// adjust fractions for colors
			
			
			if(selectedThumb != null) {
				int deltaX = mouseX - selectedThumb.getX();
				int newX = selectedThumb.getX() + deltaX;
				
				selectedThumb.setCoords(newX, selectedThumb.getY());
				fractions = updateFractions();
				repaint();
			}
		}
		
		/**
		 * Calculates the fractions needed for the LinearGradient object to
		 * determine where the center of each color is displayed.
		 * @return
		 */
		public float[] updateFractions() {
			
			float[] fractions = new float[colorList.size()];
			
			int i = 0;
			for(Thumb t : thumbList) {
				
				fractions[i] = (float)(t.getX()/ gradientRect.getWidth());
				i++;
			}
			
			System.out.println("Fractions: " + Arrays.toString(fractions));
			
			return fractions;
		}
		
		/**
		 * Resets the fractions float[] to its initial value with 3 colors.
		 * @return
		 */
		public float[] resetFractions() {
			
			return new float[]{0.0f, 0.5f, 1.0f};
		}
	}

	/**
	 * Returns the mainPanel which contains all the GUI components for
	 * the ColorGradientChooser.
	 * @return
	 */
	public JPanel makeGradientPanel() {
		
		return mainPanel;
	}
	
	/**
	 * Gives access to the GradientBox object which is the JPanel containing
	 * the actual color gradient and thumbs.
	 * @return
	 */
	protected GradientBox getGradientBox() {
		
		return gradientBox;
	}
	
	/**
	 * Returns the current size of colorList in ColorGradientChooser.
	 * @return
	 */
	protected int getColorListSize() {
		
		return colorList.size();
	}
	
	/**
	 * A class which describes a small triangular object used to define colors
	 * and color positions along the gradient box. This is what the user 
	 * interacts with to define the color scheme for DendroView.
	 * @author CKeil
	 *
	 */
	private class Thumb {
		
		private int x;
		private int y;
		
		private int width = 10;
		private int height = 15;
		
		private GeneralPath innerthumbPath;
		private GeneralPath outerthumbPath;
		private Color thumbColor;
		private boolean selected = false;
		
		/**
		 * Constructs a thumb object if given the x/y-coordinates and a color.
		 * @param x
		 * @param y
		 * @param color
		 */
		public Thumb(int x, int y, Color color) {
			
			this.thumbColor = color;
	
			setCoords(x, y);
		}
		
		/**
		 * Sets the base x/y-coordinates for the thumb object. This is
		 * where it touches the gradientBox.
		 * @param x
		 * @param y
		 */
		public void setCoords(int x, int y) {
			
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
			innerthumbPath.moveTo(x, y + height/2);
			innerthumbPath.lineTo(x + width / 4, y - height);
			innerthumbPath.lineTo(x - width / 4, y - height);
			innerthumbPath.closePath();
			
			outerthumbPath = new GeneralPath();
			outerthumbPath.moveTo(x, y);
			outerthumbPath.lineTo(x + width / 2, y - height);
			outerthumbPath.lineTo(x - width / 2, y - height);
			outerthumbPath.closePath();
		}
		
		public void setSelected(boolean selected) {
			
			this.selected = selected;
		}
		
		/**
		 * Paints the GeneralPath object with the set color and makes the
		 * thumb visible to the user.
		 * @param g2d
		 */
		public void paint(Graphics2D g2d) {  
			
			if(isSelected()) {
	        	g2d.setColor(Color.red);  
	        	
	        } else {
	        	g2d.setColor(Color.black);
	        }
	        
	        g2d.fill(outerthumbPath);
	        
	        g2d.setColor(thumbColor);  
	        g2d.fill(innerthumbPath);  
	    } 
		
		/**
		 * Returns the base x-coordinate for the thumb where it 
		 * contacts the gradientBox.
		 * @return int
		 */
		public int getX() {
			
			return x;
		}
		
		public void setX(int x) {
			
			this.x = x;
		}
		
		/**
		 * Returns the base y-coordinate for the thumb where it contacts the
		 * gradientBox. Should equal the height of thumbBox because it sits
		 * on top of gradientBox and they directly touch.
		 * @return
		 */
		public int getY() {
			
			return y;
		}
		
		/**
		 * Shows if the current thumb's selected status is true or not.
		 * @return boolean
		 */
		public boolean isSelected() {
			
			return selected;
		}
		
		/**
		 * Provides the currently set color for its thumb object.
		 * @return
		 */
		public Color getColor() {
			
			return thumbColor;
		}
		
		/**
		 * Specifies the color of the Thumb object.
		 * @param color
		 */
		public void setColor(Color color) {
			
			thumbColor = color;
		}
		
		/**
		 * Checks if this Thumb's GeneralPath object contains the 
		 * specified x- and y-variable.
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean contains(int x, int y) {  
	        
			return outerthumbPath.contains(x, y);  
	    } 
	}
	
	// Button Getters
	protected JButton getRGButtton() {
		
		return redGreenButton;
	}
	
	protected JButton getYBButton() {
		
		return yellowBlueButton;
	}
	
	// Listeners
	protected void addColorListener(MouseListener l) {
		
		gradientBox.addMouseListener(l);
	}
	
	protected void addThumbMotionListener(MouseMotionListener l) {
		
		gradientBox.addMouseMotionListener(l);
	}
	
	protected void addAddListener(ActionListener l) {
		
		addButton.addActionListener(l);
	}
	
	protected void addDefaultListener(ActionListener l) {
		
		redGreenButton.addActionListener(l);
		yellowBlueButton.addActionListener(l);
	}
}
