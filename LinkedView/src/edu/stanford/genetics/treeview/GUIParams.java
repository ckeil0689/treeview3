package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;

import edu.stanford.genetics.treeview.core.AutoComboBox;

public class GUIParams {

	// Default
	public static String QUESTIONICON = "questionIcon_dark.png";
	public static Font FONTS = new Font("Sans Serif", Font.PLAIN, 14);
	public static Font FONTL = new Font("Sans Serif", Font.PLAIN, 20);

	public static Color PANEL_BG = new Color(60, 60, 60, 255);
	public static Color LIGHTGRAY = new Color(180, 180, 180, 255);
	public static Color DARKGRAY = new Color(200, 200, 200, 255);
	public static Color TEXT = new Color(200, 200, 200, 255);
	public static Color BORDERS = new Color(200, 200, 200, 255);
	public static Color TITLE_BG = new Color(255, 205, 65, 255);
	public static Color TITLE_TEXT = new Color(20, 20, 20, 255);
	public static Color ELEMENT = new Color(255, 205, 65, 255);
	public static Color ELEMENT_HOV = new Color(255, 174, 77, 255);
	public static Color BG_COLOR = new Color(39, 40, 34, 255);
	public static Color RED1 = new Color(240, 80, 50, 255);
	public static Color TABLEHEADERS = new Color(255, 205, 120, 255);
	public static Color PROGRESS1 = new Color(60, 60, 60, 255);
	public static Color PROGRESS2 = new Color(60, 60, 60, 255);
	
	private static boolean dark = true;

	public static void setDayLight() {
		
		dark = false;
		
		QUESTIONICON = "questionIcon_light.png";

		PANEL_BG = new Color(240, 240, 240, 255);
		LIGHTGRAY = new Color(140, 140, 140, 255);
		DARKGRAY = new Color(180, 180, 180, 255);
		TEXT = new Color(20, 20, 20, 255);
		BORDERS = new Color(100, 100, 100, 255);
		TITLE_BG = new Color(44, 185, 247, 255);
		TITLE_TEXT = new Color(254, 254, 254, 255);
		ELEMENT = new Color(44, 185, 247, 255);
		ELEMENT_HOV = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(254, 254, 254, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
		PROGRESS1 = new Color(0, 0, 0, 255);
		PROGRESS2 = new Color(254, 254, 254, 255);
	}

	public static void setNight() {

		dark = true;
		
		QUESTIONICON = "questionIcon_dark.png";
		
		PANEL_BG = new Color(60, 60, 60, 255);
		LIGHTGRAY = new Color(180, 180, 180, 255);
		DARKGRAY = new Color(200, 200, 200, 255);
		TEXT = new Color(200, 200, 200, 255);
		BORDERS = new Color(200, 200, 200, 255);
		TITLE_BG = new Color(255, 205, 65, 255);
		TITLE_TEXT = new Color(20, 20, 20, 255);
		ELEMENT = new Color(255, 205, 65, 255);
		ELEMENT_HOV = new Color(255, 174, 77, 255);
		BG_COLOR = new Color(39, 40, 34, 255);
		RED1 = new Color(240, 80, 50, 255);
		TABLEHEADERS = new Color(255, 205, 120, 255);
		PROGRESS1 = new Color(60, 60, 60, 255);
		PROGRESS2 = new Color(60, 60, 60, 255);
	}
	
	public static JButton setButtonLayout(String title, String iconFileName) {

		final JButton button = new JButton();
		
		// Basic layout
		button.setFont(FONTS);
		button.setBorder(null);
		
		// Set button color first
		button.setBackground(GUIParams.ELEMENT);
		button.setForeground(GUIParams.BG_COLOR);
		
		// Check if button has a title and change color if it's "Close" 
		if(title != null) {
			button.setText(title);
			
			if(title.equalsIgnoreCase("Close")) {
				button.setBackground(GUIParams.RED1);
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
		button.setMinimumSize(new Dimension(40, 40));
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
	public static JProgressBar setPBarLayout(final String text) {

		JProgressBar pBar = new JProgressBar();
		pBar.setMinimum(0);
		pBar.setStringPainted(true);
		pBar.setMaximumSize(new Dimension(2000, 40));
		pBar.setForeground(GUIParams.ELEMENT);
		pBar.setUI(new BasicProgressBarUI() {

			@Override
			protected Color getSelectionBackground() {
				return GUIParams.PROGRESS1;
			};

			@Override
			protected Color getSelectionForeground() {
				return GUIParams.PROGRESS2;
			};
		});
		pBar.setString(text);
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
		final Dimension d = comboBox.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
		comboBox.setPreferredSize(d);
		comboBox.setFont(GUIParams.FONTS);
		comboBox.setBackground(Color.white);

		return comboBox;
	}
}
