package GradientColorChoice;

import java.awt.Color;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor2;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractorEditor2;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorPresets2;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorSet2;

import net.miginfocom.swing.MigLayout;

public class ColorGradientChooser implements ConfigNodePersistent {

	private JFrame applicationFrame;
	private JPanel mainPanel;
	private Preferences configNode;
	
	private GradientBox gradientBox;
	
	private Color[] colors;
	private float[] fractions;
	
	private double minVal;
	private double maxVal;
	
	private ArrayList<Color> colorList;
	private ArrayList<Thumb> thumbList;
	
	private JButton addButton;
	private JButton removeButton;
	private JButton saveButton;
	
	private JRadioButton redGreenButton;
	private JRadioButton yellowBlueButton;
	private JRadioButton customColorButton;
	
	private ButtonGroup colorButtonGroup;
	
	private ColorExtractor2 colorExtractor;
	private ColorPresets2 colorPresets;
	
	private ColorExtractorEditor2 colorExtractorEditor;
	private ColorPresetsPanel colorPresetsPanel;
	
	private Thumb selectedThumb = null;
	
	public ColorGradientChooser(final ColorExtractor2 drawer,
			final ColorPresets2 colorPresets, double minVal, double maxVal, 
			JFrame applicationFrame) {
		
		this.colorExtractor = drawer;
		this.colorPresets = colorPresets;
		this.applicationFrame = applicationFrame;
		this.minVal = minVal;
		this.maxVal = maxVal;
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.MENU);
		mainPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JLabel hint = new JLabel("Move or add sliders to adjust color scheme.");
		hint.setForeground(GUIParams.DARKGRAY);
		hint.setFont(GUIParams.FONTS);
		
		colorList = new ArrayList<Color>();
		thumbList = new ArrayList<Thumb>();
		
		gradientBox = new GradientBox();
		
		addButton = GUIParams.setButtonLayout("Add Color", null);
		removeButton = GUIParams.setButtonLayout("Remove Selected", null);
//		saveButton = GUIParams.setButtonLayout("Save Colors", null);
		
		colorButtonGroup = new ButtonGroup();
		
		redGreenButton = GUIParams.setRadioButtonLayout("Red-Green");
		yellowBlueButton = GUIParams.setRadioButtonLayout("Yellow-Blue");
		customColorButton = GUIParams.setRadioButtonLayout("Custom Colors");
		
		colorButtonGroup.add(redGreenButton);
		colorButtonGroup.add(yellowBlueButton);
		colorButtonGroup.add(customColorButton);
		
		JPanel radioButtonPanel = new JPanel();
		radioButtonPanel.setLayout(new MigLayout());
		radioButtonPanel.setOpaque(false);
		radioButtonPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JLabel colorHint = new JLabel("Choose a Color Scheme:");
		colorHint.setFont(GUIParams.FONTS);
		colorHint.setForeground(GUIParams.DARKGRAY);
		
		radioButtonPanel.add(colorHint, "span, wrap");
		radioButtonPanel.add(redGreenButton, "span, wrap");
		radioButtonPanel.add(yellowBlueButton, "span, wrap");
		radioButtonPanel.add(customColorButton, "span");
		
		JPanel presetPanel = new JPanel();
		presetPanel.setLayout(new MigLayout());
		colorExtractorEditor = new ColorExtractorEditor2(colorExtractor);
		presetPanel.add(colorExtractorEditor, "alignx 50%, pushx, wrap");
		presetPanel.add(new CEEButtons(), "alignx 50%, pushx, wrap");

		colorPresetsPanel = new ColorPresetsPanel();
		final JScrollPane sp = new JScrollPane(colorPresetsPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setOpaque(false);
		presetPanel.add(sp, "alignx 50%, pushx, growx");
		
		mainPanel.add(hint, "span, wrap");
		mainPanel.add(gradientBox, "h 20%, growx, pushx, alignx 50%, " +
				"span, wrap");
		mainPanel.add(addButton, "pushx, alignx 100%");
		mainPanel.add(removeButton, "pushx, alignx 0%, wrap");
//		mainPanel.add(saveButton, "pushx, alignx 50%, span, wrap");
		mainPanel.add(radioButtonPanel, "pushx");
		mainPanel.add(presetPanel);
	}
	
	@Override
	public void setConfigNode(Preferences parentNode) {
		
		if (parentNode != null) {
			this.configNode = parentNode.node("GradientChooser");
			
		} else {
			LogBuffer.println("Could not find or create GradientChooser " +
					"node because parentNode was null.");
		}
	}
	
	/**
	 * Saves the current colors and fractions as a ColorSet to the configNode.
	 */
	public void saveStatus() {
		
		if(gradientBox != null) {
			gradientBox.savePresets();
		}
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
		private Rectangle2D numRect = new Rectangle2D.Float();
		
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
			
			verifyThumbs();
			
			drawThumbBox(g2);
			
			try {
				drawGradientBox(g2);
			} catch (IllegalArgumentException e) {
				System.out.println("Fraction Keyframe not increasing.");
				System.out.println("Fractions: " + Arrays.toString(fractions));
			}
			
			drawNumBox(g2);
			
			g2.dispose();
		}
		
		public void setupRects(int width, int height) {
			
			gradientRect.setRect(0, height * 1/4, width, height * 2/4);
			thumbRect.setRect(0, 0, width, height * 1/4);
			numRect.setRect(0, height * 3/4, width, height * 1/4);
		}
		
		public void drawGradientBox(Graphics2D g2) 
				throws IllegalArgumentException {

			// Dimensions 
			float startX = (float)gradientRect.getX();
			float startY = (float)gradientRect.getY();
			
			float endX = (float)gradientRect.getWidth() + startX;
			float endY = (float)gradientRect.getHeight() + startY;
			
			colors = new Color[colorList.size()];
			
			for(int i = 0; i < colors.length; i++) {
				
				colors[i] = colorList.get(i);
			}
			
			// Generating Gradient to fill the rectangle with
			LinearGradientPaint gradient = new LinearGradientPaint(startX, 
					startY, endX, endY, fractions, colors, 
					CycleMethod.NO_CYCLE);
				
			g2.setPaint(gradient);
			g2.fillRect((int)startX, (int)startY, (int)endX, (int)endY);
		}
		
		public void drawThumbBox(Graphics2D g2) {
			
			// Fill thumbRect with background color
			g2.setColor(GUIParams.MENU);
			g2.fill(thumbRect);
			
			// Paint the thumbs
			for(Thumb t : thumbList) {
				
				t.paint(g2);
			}
		}
		
		public void drawNumBox(Graphics2D g2) {
			
			g2.setColor(GUIParams.MENU);
			g2.fill(numRect);
			
			g2.setColor(Color.black);
			g2.setFont(GUIParams.FONTS);
			
			// Paint the thumbs
			if(thumbList.size() == fractions.length) {
				int i = 0;
				for(Thumb t : thumbList) {
					
					// Rounding to 3 decimals
					float fraction = fractions[i];
					Double value = Math.abs((maxVal - minVal) * fraction) 
							+ minVal;
					value = (double)Math.round(value * 1000) / 1000;
					
					g2.drawString(Double.toString(value), t.getX(), 
							(int)((numRect.getHeight()/2) + numRect.getMinY()));
					i++;
				}
			} else {
				System.out.println("ThumbList size (" + thumbList.size() 
						+ ") and fractions size (" + fractions.length 
						+ ") are different in drawNumbBox!");
			}
		}
		
		/**
		 * Adds a color to the gradient.
		 * @param newCol
		 */
		public void addColor(Color newCol) {
			
			int selectedIndex = 0;
			if(selectedThumb != null) {
				selectedIndex = thumbList.indexOf(selectedThumb);
			}
			
//			if((selectedIndex == thumbList.size() - 1) 
//					&& thumbList.size() > 0) {
			if(thumbList.get(selectedIndex).getX() 
					== thumbList.get(thumbList.size() - 1).getX()) {
				selectedIndex--;
			}
			
			colorList.add(selectedIndex + 1, newCol);
			
			double halfRange = (fractions[selectedIndex + 1] 
					- fractions[selectedIndex])/2;
			
			double newFraction = halfRange + fractions[selectedIndex];
			
			int x = (int)(newFraction * getGradientBox().getSize().getWidth());
			
			insertThumbAt(x, newCol);
			fractions = updateFractions();
			
			if(thumbList.size() != fractions.length) {
				System.out.println("ThumbList size (" + thumbList.size() 
						+ ") and fractions size (" + fractions.length 
						+ ") are different in drawNumbBox!");
			}
			
			setColors();
			selectCustom();
			repaint();
		}
		
		/** 
		 * Removes a color from the gradient.
		 */
		public void removeColor() {
			
			int index = 0;
			for(Thumb t : thumbList) {
				
				if(t.isSelected()){
//						&& !(index == 0 || index == thumbList.size() - 1)) {
					thumbList.remove(index);
					colorList.remove(index);
					selectedThumb = null;
					break;
				}
				index++;
			}
			
			fractions = updateFractions();
			
			if(thumbList.size() != fractions.length) {
				System.out.println("ThumbList size (" + thumbList.size() 
						+ ") and fractions size (" + fractions.length 
						+ ") are different in drawNumbBox!");
			}
			
			setColors();
			selectCustom();
			repaint();
		}
		
		/**
		 * Changes the gradient color in the area the mouse was clicked in.
		 * @param newCol
		 * @param point
		 */
		public void setGradientColor(Point point) {
			
			Color newCol = null;
			
			int clickPos = (int)point.getX();
			int index = 0;
			int distance = (int) gradientBox.getWidth();
			
			for(Thumb t : thumbList) {
				
				if(Math.abs(t.getX() - clickPos) < distance) {
					distance = Math.abs(t.getX() - clickPos);
					index = thumbList.indexOf(t);
				}
			}
			
			newCol = JColorChooser.showDialog(this, "Pick a Color", 
					thumbList.get(index).getColor());
			
			if(newCol != null) {
				colorList.set(index, newCol);
				thumbList.get(index).setColor(newCol);
				setColors();
				selectCustom();
			}
		}
		
		/**
		 * Sets the color in colors[] at the specified index.
		 * @param newCol
		 * @param index
		 */
		public void setColors() {
			
			colorExtractor.setNewParams(fractions, colorList);
			colorExtractor.notifyObservers();
			mainPanel.repaint();
		}
		
		/**
		 * Resets the color values to default.
		 * @param redGreen
		 */
		public void setPresets() {
			
			colorList.clear();
			thumbList.clear();
			fractions = resetFractions();
			
			ColorSet2 defaultSet = colorPresets.getDefaultColorSet();
			
			String defaultColors = "RedGreen";
			String colorScheme = configNode.get("activeColors", defaultColors);
			
			if(colorScheme.equalsIgnoreCase("Custom")) {
				customColorButton.setSelected(true);
				ColorSet2 savedColorSet = colorPresets.getColorSet(colorScheme);
				loadPresets(savedColorSet);
				
			} else if(colorScheme.equalsIgnoreCase(defaultColors)){
				// Colors should be defined as default ColorSet in ColorPresets2
				redGreenButton.setSelected(true);
				ColorSet2 rgColorSet = colorPresets.getColorSet(colorScheme);
				loadPresets(rgColorSet);
				
			} else if(colorScheme.equalsIgnoreCase("YellowBlue")){
				yellowBlueButton.setSelected(true);
				ColorSet2 ybColorSet = colorPresets.getColorSet("YellowBlue");
				loadPresets(ybColorSet);
				
			} else {
				LogBuffer.println("No matching ColorSet found in " +
						"ColorGradientChooser.setPresets()");
			}
			
			setColors();
		}
		
		/**
		 * Loads the values from a given ColorSet into colorList 
		 * and fractionList.
		 * @param colorSet
		 */
		public void loadPresets(ColorSet2 colorSet) {
			
			String[] colors = colorSet.getColors();
			
			for(String color : colors) {
				
				colorList.add(Color.decode(color));
			}
			
			float[] fracs = colorSet.getFractions();
			
			if(fractions.length == fracs.length) {
				fractions = fracs;
				
			} else {
				LogBuffer.println("Fractions and fracs are not the same " +
						"length in ColorGradientChooser.setPresets()!");
			}
		}
		
		public void savePresets() {
			
			ArrayList<Double> fractionList = new ArrayList<Double>();
			
			for(float f : fractions) {
				
				fractionList.add((double)f);
			}
			
			String colorSetName = "Custom";
			colorPresets.addColorSet(colorSetName, colorList, fractionList, 
					"#ffffff");
		}
		
		/**
		 * Checks if a passed Point is contained in the area of the gradient
		 * rectangle.
		 * @param point
		 * @return
		 */
		public boolean isGradientArea(Point point) {
			
			boolean inArea = false;
			
			if(gradientRect.contains(point)) {
				inArea = true;
			}
			
			return inArea;
		}
		
		// Thumb related methods
		/**
		 * Checks if a thumb is located at the x,y-coordinates.
		 * @param x
		 * @param y
		 * @return
		 */
		public boolean containsThumb(int x, int y) {
			
			boolean containsThumb = false;
			
			for(Thumb t : thumbList) {
				
				if(t.contains(x, y)) {
					
					containsThumb = true;
					break;
				}
			}
			
			return containsThumb;
		}
		
		/**
		 * Selects a thumb if it is not yet selected, deselects it otherwise.
		 * @param point
		 */
		public void selectThumb(Point point) {
			
			for(Thumb t : thumbList) {
				
				if(t.contains((int)point.getX(), (int)point.getY())) {
//						&& !(addIndex == 0 
//						|| addIndex == thumbList.size() - 1)) {
						t.setSelected(!t.isSelected());
						break;
				} else {
					t.setSelected(false);
				}
			}
			repaint();
		}
		
		/**
		 * Sets thumbs evenly depending on the amounts of colors currently
		 * added to the gradient.
		 */
		public void verifyThumbs() {
			
			int x = (int)thumbRect.getX();
			int w = (int)thumbRect.getWidth(); 
			
			for (int i = 0; i < fractions.length; i++) {  
	            
				// Avoid rounding errors when casting to int
				double widthFactor = Math.round(w * fractions[i]);
				int pos = x + (int) (widthFactor);
				
				if(!checkThumbPresence(i) 
						&& !((thumbList.size() == colorList.size())
								&& thumbList.size() == fractions.length)) {
					insertThumbAt(pos, colorList.get(i));  
					
				} else {
					adjustThumbPos(i);
				}
	        }
		}
		
		/**
		 * Checks if a thumb is present at the given x-position.
		 * @param pos
		 * @return
		 */
		public boolean checkThumbPresence(int thumbIndex) {
			
			boolean isPresent = false;
			
//			int checkIndex = 0;
//			for(Thumb t : thumbList) {
				
			if(thumbList.size() > thumbIndex) {
				double fraction = thumbList.get(thumbIndex).getX()
						/ thumbRect.getWidth();
				fraction = (double)Math.round(fraction * 10000)/ 10000;
				
				double fraction2 = (double)Math.round(fractions[thumbIndex] 
						* 10000)/ 10000;
				
//				if(t.getX() == pos || t.isSelected()) {
				if(fraction == fraction2 
						|| thumbList.get(thumbIndex).isSelected()) {
					isPresent = true;
//					break;
					
				} else {
//					checkIndex++;
				}
			}
//			}
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
		 * Sets all thumbs' selection status to 'false'.
		 */
		public void deselectAllThumbs() {
			
			for(Thumb t : thumbList) {
				
				t.setSelected(false);
				selectedThumb = null;
			}
		}
		
		public void updateThumbPos(int inputX) {
			
			if(thumbList.size() != fractions.length) {
				System.out.println("ThumbList size (" + thumbList.size() 
						+ ") and fractions size (" + fractions.length 
						+ ") are different in updateThumbPos!");
			}
			
			if(selectedThumb != null) {
				// get position of previous thumb
				int selectedIndex = thumbList.indexOf(selectedThumb);
				int previousPos = 0;
				int nextPos = gradientBox.getWidth();
				
				if(selectedIndex == 0) {
					nextPos = thumbList.get(selectedIndex + 1).getX();
					
				} else if(selectedIndex == thumbList.size() - 1) {
					previousPos = thumbList.get(selectedIndex - 1).getX();
					
				} else {
					previousPos = thumbList.get(selectedIndex - 1).getX();
					nextPos = thumbList.get(selectedIndex + 1).getX();
				}
	
				int deltaX = inputX - selectedThumb.getX();
				int newX = selectedThumb.getX() + deltaX;
				
				if(previousPos < newX && newX < nextPos) {
					selectedThumb.setCoords(newX, selectedThumb.getY());
					fractions = updateFractions();
					
				} else if(newX < previousPos && previousPos != 0) {
					Collections.swap(thumbList, selectedIndex, 
							selectedIndex - 1);
					Collections.swap(colorList, selectedIndex, 
							selectedIndex - 1);
					selectedThumb.setCoords(newX, selectedThumb.getY());
					fractions = updateFractions();
					
				} else if(newX > nextPos 
						&& nextPos != gradientBox.getWidth()) {
					Collections.swap(thumbList, selectedIndex, 
							selectedIndex + 1);
					Collections.swap(colorList, selectedIndex, 
							selectedIndex + 1);
					selectedThumb.setCoords(newX, selectedThumb.getY());
					fractions = updateFractions();
				}
				
				if(thumbList.size() != fractions.length) {
					System.out.println("ThumbList size (" + thumbList.size() 
							+ ") and fractions size (" + fractions.length 
							+ ") are different in updateThumbPos!");
				}
				
				setColors();
				selectCustom();
				repaint();
			}
		}
		
		/**
		 * Adjust thumb positions when the PreferencesMenu JDialog is
		 * resized.
		 * @param thumbIndex
		 */
		public void adjustThumbPos(int thumbIndex) {
			
			int inputX = (int)(fractions[thumbIndex] * thumbRect.getWidth());
			int deltaX = inputX - thumbList.get(thumbIndex).getX();
			int newX = thumbList.get(thumbIndex).getX() + deltaX;
			
			thumbList.get(thumbIndex).setCoords(newX,
					(int)thumbRect.getHeight());
			
			repaint();
		}
		
		public void specifyThumbPos(Point point) {
			
			for(Thumb t : thumbList) {
				
				if(t.contains((int)point.getX(), (int)point.getY())){
					
					final JDialog positionInputDialog = new JDialog();
					positionInputDialog.setModalityType(
							JDialog.DEFAULT_MODALITY_TYPE);
					positionInputDialog.setDefaultCloseOperation(
							JDialog.DISPOSE_ON_CLOSE);
					positionInputDialog.setTitle("New Position");
					
					JLabel enterPrompt = new JLabel("Enter data value: ");
					enterPrompt.setForeground(GUIParams.TEXT);
					enterPrompt.setFont(GUIParams.FONTS);
					
					final JTextField inputField = new JTextField();
					inputField.setEditable(true);

					JButton okButton = GUIParams.setButtonLayout("OK", null);
					okButton.addActionListener(new ActionListener(){

						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							try { 
								double inputDataValue = Double.parseDouble(
										inputField.getText());
								
								if(inputDataValue > minVal 
										&& inputDataValue < maxVal) {
									
									double fraction = Math.abs(inputDataValue 
											- minVal)/ (maxVal - minVal);
									
									int inputXValue = (int) (fraction 
											* gradientBox.getWidth());
									
									gradientBox.updateThumbPos(inputXValue);
									positionInputDialog.dispose();
									
								} else {
									inputField.setText("Number out of range!");
								}
							
							} catch(NumberFormatException e) {
								inputField.setText("Enter a number!");
							}
						}
					});
					
					JPanel panel = new JPanel();
					panel.setLayout(new MigLayout());
					panel.setBackground(GUIParams.BG_COLOR);
					
					panel.add(enterPrompt, "push, span, wrap");
					panel.add(inputField, "push, growx, span, wrap");
					panel.add(okButton, "pushx, alignx 50%");
					
					positionInputDialog.getContentPane().add(panel);
					
					positionInputDialog.pack();
					positionInputDialog.setLocationRelativeTo(applicationFrame);
					positionInputDialog.setVisible(true);
				}
			}
			setColors();
			selectCustom();
			repaint();
			
		}
		
		// Fraction list Methods
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
			
			return fractions;
		}
		
		/**
		 * Resets the fractions float[] to a default value with 3 colors.
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
		
		gradientBox.setPresets();
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
			
			if(selected) {
				selectedThumb = this;
			}
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
		 * Provides the currently set color for its thumb object.
		 * @return
		 */
		public void setColor(Color newCol) {
			
			thumbColor = newCol;
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
	
	// Accessors
	protected Preferences getConfigNode() {
		
		return configNode;
	}
	
	protected ButtonGroup getButtonGroup() {
		
		return colorButtonGroup;
	}
	
	protected JRadioButton getRGButton() {
		
		return redGreenButton;
	}
	
	protected JRadioButton getYBButton() {
		
		return yellowBlueButton;
	}
	
	protected JRadioButton getCustomColorButton() {
		
		return customColorButton;
	}
	
	// Mutators
	/**
	 * Sets the 'custom' button of the radiobutton group for colors to
	 * selected if it isn't already.
	 * Should set the preset here.
	 */
	public void selectRedGreen() {
		
		// Save current color status as preset. When user switches back to
		// custom, the preset should be called here.
		if(!getCustomColorButton().isSelected()) {
			redGreenButton.setSelected(true);
			configNode.put("activeColors", "RedGreen");
		}
	}
	
	/**
	 * Sets the 'custom' button of the radiobutton group for colors to
	 * selected if it isn't already.
	 * Should set the preset here.
	 */
	public void selectYellowBlue() {
		
		// Save current color status as preset. When user switches back to
		// custom, the preset should be called here.
		if(!getCustomColorButton().isSelected()) {
			yellowBlueButton.setSelected(true);
			configNode.put("activeColors", "YellowBlue");
		}
	}
	
	/**
	 * Sets the 'custom' button of the radiobutton group for colors to
	 * selected if it isn't already.
	 * Should set the preset here.
	 */
	public void selectCustom() {
		
		// Save current color status as preset. When user switches back to
		// custom, the preset should be called here.
		if(!getCustomColorButton().isSelected()) {
			customColorButton.setSelected(true);
			configNode.put("activeColors", "Custom");
		}
	}
	
	protected void addColorSet(ColorSet2 temp) {
		
		colorPresets.addColorSet(temp);
	}
	
	// Listeners
	protected void addThumbSelectionListener(MouseListener l) {
		
		gradientBox.addMouseListener(l);
	}
	
	protected void addThumbMotionListener(MouseMotionListener l) {
		
		gradientBox.addMouseMotionListener(l);
	}
	
	protected void addAddListener(ActionListener l) {
		
		addButton.addActionListener(l);
	}
	
	protected void addRemoveListener(ActionListener l) {
		
		removeButton.addActionListener(l);
	}
	
	protected void addDefaultListener(ActionListener l) {
		
		redGreenButton.addActionListener(l);
		yellowBlueButton.addActionListener(l);
	}
	
	protected void addSavePresetListener(ActionListener l) {
		
		saveButton.addActionListener(l);
	}
	
	// Inner Classes
	/**
	 * this class allows the presets to be selected...
	 */
	class ColorPresetsPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		ColorPresetsPanel() {

			redoLayout();
		}

		public void redoLayout() {

			removeAll();
			this.setBackground(GUIParams.BG_COLOR);
			final int nPresets = colorPresets.getNumPresets();
			final JButton[] buttons = new JButton[nPresets];
			for (int i = 0; i < nPresets; i++) {
				final JButton presetButton = GUIParams.setButtonLayout((
						colorPresets.getPresetNames())[i], null);
				final int index = i;
				presetButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent e) {

						colorExtractorEditor.copyStateFrom(colorPresets
								.getColorSet(index));
					}
				});
				this.add(presetButton);

				buttons[index] = presetButton;
			}
		}
	}
	
	class CEEButtons extends JPanel {

		private static final long serialVersionUID = 1L;

		CEEButtons() {

			this.setOpaque(false);
			final JButton loadButton = GUIParams.setButtonLayout("Load", null);
			loadButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final JFileChooser chooser = new JFileChooser();
					final int returnVal = chooser
							.showOpenDialog(CEEButtons.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						final File f = chooser.getSelectedFile();
						try {
							final ColorSet2 temp = new ColorSet2();
							temp.loadEisen(f);
							colorExtractorEditor.copyStateFrom(temp);

						} catch (final IOException ex) {
							JOptionPane.showMessageDialog(CEEButtons.this,
									"Could not load from " + f.toString()
											+ "\n" + ex);
						}
					}
				}
			});
			this.add(loadButton);

			final JButton saveButton = GUIParams.setButtonLayout("Save", null);
			saveButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final JFileChooser chooser = new JFileChooser();
					final int returnVal = chooser
							.showSaveDialog(CEEButtons.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						final File f = chooser.getSelectedFile();
						try {
							final ColorSet2 temp = new ColorSet2();
							colorExtractorEditor.copyStateTo(temp);
							temp.saveEisen(f);

						} catch (final IOException ex) {
							JOptionPane.showMessageDialog(CEEButtons.this,
									"Could not save to " + f.toString() + "\n"
											+ ex);
						}
					}
				}
			});
			this.add(saveButton);

			final JButton makeButton = GUIParams.setButtonLayout("Make Preset", 
					null);
			makeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final ColorSet2 temp = new ColorSet2();
					colorExtractorEditor.copyStateTo(temp);
					temp.setName("UserDefined");
					colorPresets.addColorSet(temp);
					colorPresetsPanel.redoLayout();
					colorPresetsPanel.invalidate();
					colorPresetsPanel.revalidate();
					colorPresetsPanel.repaint();
				}
			});
			this.add(makeButton);

			final JButton resetButton = GUIParams.setButtonLayout(
					"Reset Presets", null);
			resetButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					colorPresets.reset();
				}
			});
			this.add(resetButton);
		}
	}
}
