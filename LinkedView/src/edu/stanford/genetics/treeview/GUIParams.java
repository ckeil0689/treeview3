package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;

import edu.stanford.genetics.treeview.core.AutoComboBox;

public class GUIParams {

	// Default
	public static String QUESTIONICON = "questionIcon_dark.png";
	
	public static Font FONTS = new Font("Sans Serif", Font.PLAIN, 14);
	public static Font FONTL = new Font("Sans Serif", Font.PLAIN, 20);

	public static Color LIGHTGRAY = new Color(180, 180, 180, 255);
	public static Color DARKGRAY = new Color(200, 200, 200, 255);
	public static Color TEXT = new Color(200, 200, 200, 255);
	public static Color BORDERS = new Color(200, 200, 200, 255);
	public static Color MAIN = new Color(255, 200, 65, 255);
	public static Color ELEMENT_HOV = new Color(255, 174, 77, 255);
	public static Color BG_COLOR = new Color(39, 40, 34, 255);
	public static Color RED1 = new Color(240, 80, 50, 255);
	public static Color PROGRESS = new Color(39, 40, 34, 255);
	public static Color TABLEHEADERS = new Color(255, 200, 120, 255);
	
	private static boolean dark = true;

	public static void setDayLight() {
		
		dark = false;
		
		QUESTIONICON = "questionIcon_light.png";

		LIGHTGRAY = new Color(140, 140, 140, 255);
		DARKGRAY = new Color(180, 180, 180, 255);
		TEXT = new Color(20, 20, 20, 255);
		MAIN = new Color(30, 144, 255, 255);
		BORDERS = new Color(100, 100, 100, 255);
		ELEMENT_HOV = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(254, 254, 254, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
	}

	public static void setNight() {

		dark = true;
		
		QUESTIONICON = "questionIcon_dark.png";
		
		LIGHTGRAY = new Color(180, 180, 180, 255);
		DARKGRAY = new Color(200, 200, 200, 255);
		TEXT = new Color(200, 200, 200, 255);
		MAIN = new Color(255, 200, 65, 255);
		BORDERS = new Color(200, 200, 200, 255);
		ELEMENT_HOV = new Color(255, 174, 77, 255);
		BG_COLOR = new Color(39, 40, 34, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(255, 205, 120, 255);
	}
	
	/**
	 * Creates a button with a title and icon if desired. The method 
	 * centralizes the layout setting for buttons so that all buttons will
	 * look similar.
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton setButtonLayout(String title, String iconFileName) {

		final JButton button = new JButton();
		
		// Basic layout
		button.setFont(FONTS);
		button.setBorder(null);
		
		// Set button color first
		button.setBackground(MAIN);
		button.setForeground(BG_COLOR);
		
		// Check if button has a title and change color if it's "Close" 
		if(title != null) {
			button.setText(title);
			
			if(title.equalsIgnoreCase("Close")) {
				button.setBackground(RED1);
				button.setForeground(Color.white);	
			} 
		}
		
		// If provided, add icon to button
		if(iconFileName != null) {	
			
			String iconType;
			
			if(!iconFileName.substring(iconFileName.length() - 4, 
					iconFileName.length() - 1).equalsIgnoreCase("png")) {
				if(dark) {
					iconType = "_dark.png";
					
				} else {
					iconType = "_light.png";
				}
			} else {
				iconType = "";
			}
			
			try {
				ClassLoader classLoader = Thread.currentThread()
						.getContextClassLoader();
				InputStream input = classLoader
						.getResourceAsStream(iconFileName + iconType);
				
			    Image img = ImageIO.read(input);
			    button.setIcon(new ImageIcon(img));
			    
			    button.setHorizontalTextPosition(SwingConstants.LEFT);
		
			  } catch (IOException ex) {
			  }
		} 
		
		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth()* 1.5, d.getHeight() * 1.5);
		
		if(iconFileName != null) {
			button.setMinimumSize(new Dimension(40, 40));
		
		} else {
			button.setMinimumSize(new Dimension(80, 40));
		}
		
		button.setPreferredSize(d);

		return button;
	}
	
	/**
	 * Method to setup a JProgressBar
	 * 
	 * @param pBar
	 * @param text
	 * @return
	 */
	public static JProgressBar setPBarLayout() {

		JProgressBar pBar = new JProgressBar();
		pBar.setMinimum(0);
		pBar.setStringPainted(true);
		pBar.setMaximumSize(new Dimension(2000, 20));
		pBar.setForeground(MAIN);
		pBar.setUI(new BasicProgressBarUI() {

			@Override
			protected Color getSelectionBackground() {
				return PROGRESS;
			};

			@Override
			protected Color getSelectionForeground() {
				return PROGRESS;
			};
		});
		pBar.setVisible(true);

		return pBar;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object The method is used to
	 * make all ComboBoxes appear consistent in aesthetics
	 * 
	 * @param combo
	 * @return
	 */
	public static AutoComboBox setComboLayout(final String[] combos) {

		final AutoComboBox comboBox = new AutoComboBox(combos);
		comboBox.setFont(FONTS);
		comboBox.setBackground(Color.white);

		return comboBox;
	}
	
	/**
	 * Returns the size of the current screen.
	 * @return Dimension size
	 */
	public static Dimension getScreenSize() {
		
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension dimension = toolkit.getScreenSize();
		
		return dimension;
	}
	
	/**
	 * Creates a header label.
	 * @param title
	 * @return
	 */
	public static JLabel setupHeader(String title) {
		
		JLabel header = new JLabel(title);
		header.setFont(FONTL);
		header.setForeground(MAIN);
		
		return header;
	}
}
