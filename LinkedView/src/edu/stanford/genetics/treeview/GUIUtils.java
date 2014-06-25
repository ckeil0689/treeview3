package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;

import net.miginfocom.swing.MigLayout;

public class GUIUtils {

	// Default
	public static String QUESTIONICON = "questionIcon_dark.png";

	public static Font FONTS = new Font("Sans Serif", Font.PLAIN, 14);
	public static Font FONT_MENU = new Font("Sans Serif", Font.PLAIN, 16);
	public static Font FONTL = new Font("Sans Serif", Font.PLAIN, 20);
	public static Font FONTXXL = new Font("Sans Serif", Font.PLAIN, 30);

	public static Color LIGHTGRAY = new Color(200, 200, 200, 255);
	public static Color DARKGRAY = new Color(150, 150, 150, 255);
	public static Color RADIOTEXT = new Color(50, 50, 50, 255);
	public static Color MENU = new Color(254, 254, 254, 255);
	public static Color TEXT = new Color(200, 200, 200, 255);
	public static Color BORDERS = new Color(200, 200, 200, 255);
	public static Color MAIN = new Color(255, 200, 65, 255);
	public static Color BUTTON_DARK = new Color(255, 180, 45, 255);
	public static Color BUTTON_DARKEST = new Color(200, 145, 35, 255);
	public static Color BUTTON_BORDER_PRESS = new Color(185, 145, 45, 255);
	public static Color BUTTON_BORDER_ROLL = new Color(255, 205, 65, 255);
	public static Color ELEMENT_HOV = new Color(255, 174, 77, 255);
	public static Color BG_COLOR = new Color(39, 40, 34, 255);
	public static Color SCROLL_BG = new Color(80, 80, 80, 255);
	public static Color RED1 = new Color(240, 80, 50, 255);
	public static Color PROGRESS = new Color(39, 40, 34, 255);
	public static Color TABLEHEADERS = new Color(255, 200, 120, 255);

	private static boolean dark = true;

	public static void setDayLight() {

		dark = false;

		QUESTIONICON = "questionIcon_light.png";

		TEXT = new Color(20, 20, 20, 255);
		MAIN = new Color(30, 144, 255, 255);
		BUTTON_DARK = new Color(30, 110, 255, 255);
		BUTTON_DARKEST = new Color(30, 90, 255, 255);
		BUTTON_BORDER_PRESS = new Color(30, 115, 255, 255);
		BUTTON_BORDER_ROLL = new Color(30, 175, 255, 255);
		BORDERS = new Color(100, 100, 100, 255);
		ELEMENT_HOV = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(254, 254, 254, 255);
		SCROLL_BG = new Color(230, 230, 230, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
	}

	public static void setNight() {

		dark = true;

		QUESTIONICON = "questionIcon_dark.png";

		TEXT = new Color(200, 200, 200, 255);
		MAIN = new Color(255, 200, 65, 255);
		BUTTON_DARK = new Color(255, 180, 45, 255);
		BUTTON_DARKEST = new Color(200, 145, 35, 255);
		BUTTON_BORDER_PRESS = new Color(185, 145, 45, 255);
		BUTTON_BORDER_ROLL = new Color(255, 205, 65, 255);
		BORDERS = new Color(200, 200, 200, 255);
		ELEMENT_HOV = new Color(255, 174, 77, 255);
		BG_COLOR = new Color(39, 40, 34, 255);
		SCROLL_BG = new Color(80, 80, 80, 255);
		TABLEHEADERS = new Color(255, 205, 120, 255);
	}

	public static boolean isDarkTheme() {

		return dark;
	}
	
	/**
	 * Creates and returns a simple JPanel with MigLayout to be used 
	 * as container. 
	 * Opaqueness, whether it should have padding, and a custom background
	 * color can be determined. This function is mainly used to reduce
	 * repeating code throughout the source code.
	 * @param opaque
	 * @param padding
	 * @param backgroundColor
	 * @return
	 */
	public static JPanel createJPanel(boolean opaque, boolean padding, 
			Color backgroundColor) {
		
		JPanel panel = new JPanel();
		panel.setOpaque(opaque);
		
		if(padding) {
			panel.setLayout(new MigLayout());
			
		} else {
			panel.setLayout(new MigLayout("ins 0"));
		}
		
		// specify background, otherwise default to theme's backgroundColor.
		if(backgroundColor != null) {
			panel.setBackground(backgroundColor);
			
		} else {
			panel.setBackground(BG_COLOR);
		}
		
		return panel;
	}
	
	/**
	 * Creates and returns a JLabel with the appropriate text color and a
	 * small font size.
	 * @param text
	 * @return
	 */
	public static JLabel createSmallLabel(String text) {
		
		JLabel label = new JLabel(text);
		label.setFont(FONTS);
		label.setForeground(TEXT);
		
		return label;
	}
	
	/**
	 * Creates and returns a JLabel with the appropriate text color and a
	 * large font size.
	 * @param text
	 * @return
	 */
	public static JLabel createBigLabel(String text) {
		
		JLabel label = new JLabel(text);
		label.setFont(FONTL);
		label.setForeground(MAIN);
		
		return label;
	}
	
	/**
	 * Creates and returns a JLabel with the appropriate text color and a
	 * very large font size.
	 * @param text
	 * @return
	 */
	public static JLabel createXXLLabel(String text) {
		
		JLabel label = new JLabel(text);
		label.setFont(FONTXXL);
		label.setForeground(TEXT);
		
		return label;
	}

	/**
	 * Creates a button with a title and icon if desired. The method centralizes
	 * the layout setting for buttons so that all buttons will look similar.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton setButtonLayout(final String title,
			final String iconFileName) {

		final JButton button;
		if(title != null) {
			button = new TVButton(title);
			
		} else {
			button = new TVButton("");
		}
		
		// Basic layout
		button.setFont(FONTS);
		button.setForeground(BG_COLOR);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		// If provided, add icon to button
		if (iconFileName != null) {
			Image img = getIconImage(iconFileName);
			
			if(img != null) {
				button.setIcon(new ImageIcon(img));
				button.setHorizontalTextPosition(SwingConstants.LEFT);
			}
		}

		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);

		if (iconFileName != null) {
			button.setMinimumSize(new Dimension(40, 40));

		} else {
			button.setMinimumSize(new Dimension(80, 40));
		}

		button.setPreferredSize(d);

		return button;
	}
	
	/**
	 * Creates a button with a title and icon if desired. The method centralizes
	 * the layout setting for buttons so that all buttons will look similar.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JToggleButton setToggleButtonLayout(final String title) {

		final JToggleButton button;
		if(title != null) {
			button = new TVToggleButton(title);
			
		} else {
			button = new TVToggleButton("");
		}
		
		// Basic layout
		button.setFont(FONTS);
		button.setForeground(BG_COLOR);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);

		button.setPreferredSize(d);

		return button;
	}

	/**
	 * Sets a layout for a button which is supposed to appear in the top menubar
	 * which belongs to the TVFrame.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton setMenuButtonLayout(final String title,
			final String iconFileName) {

		final JButton button = new JButton();

		button.setBackground(BG_COLOR);
		button.setForeground(MAIN);
		button.setFont(FONTS);
		button.setText(title);
		button.setBorder(null);
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		if (iconFileName != null) {
			Image img = getIconImage(iconFileName);
			
			if(img != null) {
				button.setIcon(new ImageIcon(img));
				button.setHorizontalTextPosition(SwingConstants.LEFT);
			}
		}
		
		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);

		button.setPreferredSize(d);

		return button;
	}
	
	/**
	 * Sets a layout for a very large button.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton setLargeButtonLayout(final String title) {

		final JButton button;
		if(title != null) {
			button = new TVButton(title);
			
		} else {
			button = new TVButton("No title.");
		}

		// Basic layout
		button.setFont(FONTXXL);
		button.setForeground(GUIUtils.BG_COLOR);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth() * 2.5, d.getHeight() * 2.5);

		button.setPreferredSize(d);
		
		return button;
	}
	
	/**
	 * Generates an image that can be used as an icon on a JButton.
	 * @param iconFileName
	 * @return
	 */
	public static Image getIconImage(String iconFileName) {
		
		String iconType;
		Image img = null;

		if (!iconFileName.substring(iconFileName.length() - 4,
				iconFileName.length() - 1).equalsIgnoreCase("png")) {
			if (dark) {
				iconType = "_dark.png";

			} else {
				iconType = "_light.png";
			}
		} else {
			iconType = "";
		}

		try {
			final ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();
			final InputStream input = classLoader
					.getResourceAsStream(iconFileName + iconType);

			img = ImageIO.read(input);
		} catch (final IOException ex) {
		}
		
		return img;
	}

	/**
	 * Sets the layout of JRadioButton.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JRadioButton setRadioButtonLayout(final String title) {

		final JRadioButton menuButton = new JRadioButton(title);
		menuButton.setOpaque(false);
		menuButton.setForeground(TEXT);
		menuButton.setFont(FONTS);
		menuButton.setBorder(null);
		menuButton.setFocusPainted(false);

		return menuButton;
	}

	/**
	 * Method to setup a JProgressBar
	 * 
	 * @param pBar
	 * @param text
	 * @return
	 */
	public static JProgressBar setPBarLayout() {

		final JProgressBar pBar = new JProgressBar();
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
	 * Setting up a layout for a wide ComboBox object. 
	 * @param combo
	 * @return
	 */
	public static WideComboBox setWideComboLayout(final String[] combos) {

		final WideComboBox comboBox = new WideComboBox(combos);
		comboBox.setFont(FONTS);
		comboBox.setBackground(Color.white);
		comboBox.setPreferredSize(new Dimension(300, 30));
		comboBox.setMinimumSize(comboBox.getPreferredSize());

		return comboBox;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object The method is used to
	 * make all ComboBoxes appear consistent in aesthetics
	 * 
	 * @param combo
	 * @return
	 */
	public static JComboBox<String> setComboLayout(final String[] combos) {

		final JComboBox<String> comboBox = new JComboBox<String>(combos);
		comboBox.setFont(FONTS);
		comboBox.setBackground(Color.white);

		return comboBox;
	}

	/**
	 * Returns the size of the current screen.
	 * 
	 * @return Dimension size
	 */
	public static Dimension getScreenSize() {

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension dimension = toolkit.getScreenSize();

		return dimension;
	}

	/**
	 * Creates a header label.
	 * 
	 * @param title
	 * @return
	 */
	public static JLabel setupHeader(final String title) {

		final JLabel header = new JLabel(title);
		header.setFont(FONTL);
		header.setForeground(MAIN);

		return header;
	}
}
