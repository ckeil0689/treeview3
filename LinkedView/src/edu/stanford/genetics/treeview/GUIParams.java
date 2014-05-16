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
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class GUIParams {

	// Default
	public static String QUESTIONICON = "questionIcon_dark.png";

	public static Font FONTS = new Font("Sans Serif", Font.PLAIN, 14);
	public static Font FONT_MENU = new Font("Sans Serif", Font.PLAIN, 16);
	public static Font FONTL = new Font("Sans Serif", Font.PLAIN, 20);

	public static Color LIGHTGRAY = new Color(200, 200, 200, 255);
	public static Color DARKGRAY = new Color(150, 150, 150, 255);
	public static Color RADIOTEXT = new Color(50, 50, 50, 255);
	public static Color MENU = new Color(254, 254, 254, 255);
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

		TEXT = new Color(20, 20, 20, 255);
		MAIN = new Color(30, 144, 255, 255);
		BORDERS = new Color(100, 100, 100, 255);
		ELEMENT_HOV = new Color(122, 214, 255, 255);
		BG_COLOR = new Color(254, 254, 254, 255);
		TABLEHEADERS = new Color(191, 235, 255, 255);
	}

	public static void setNight() {

		dark = true;

		QUESTIONICON = "questionIcon_dark.png";

		TEXT = new Color(200, 200, 200, 255);
		MAIN = new Color(255, 200, 65, 255);
		BORDERS = new Color(200, 200, 200, 255);
		ELEMENT_HOV = new Color(255, 174, 77, 255);
		BG_COLOR = new Color(39, 40, 34, 255);
		TABLEHEADERS = new Color(255, 205, 120, 255);
	}

	public static boolean isDarkTheme() {

		return dark;
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

		final JButton button = new JButton();

		// Basic layout
		button.setFont(FONTS);
		button.setBorder(null);
		button.setFocusPainted(false);

		// Set button color first
		button.setBackground(MAIN);
		button.setForeground(BG_COLOR);

		// Check if button has a title and change color if it's "Close"
		if (title != null) {
			button.setText(title);

			if (title.equalsIgnoreCase("Close")) {
				button.setBackground(RED1);
				button.setForeground(Color.white);
			}
		}

		// If provided, add icon to button
		if (iconFileName != null) {

			String iconType;

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

				final Image img = ImageIO.read(input);
				button.setIcon(new ImageIcon(img));

				button.setHorizontalTextPosition(SwingConstants.LEFT);

			} catch (final IOException ex) {
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
	 * Sets a layout for a button which is supposed to appear in the top menubar
	 * which belongs to the TVFrame.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton setMenuButtonLayout(final String title,
			final String iconFileName) {

		final JButton menuButton = setButtonLayout(title, iconFileName);

		menuButton.setBackground(BG_COLOR);
		menuButton.setForeground(MAIN);
		menuButton.setFont(FONTS);
		menuButton.setBorder(null);
		menuButton.setFocusPainted(false);

		return menuButton;
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
	 * Setting up a general layout for a ComboBox object The method is used to
	 * make all ComboBoxes appear consistent in aesthetics
	 * 
	 * @param combo
	 * @return
	 */
	public static WideComboBox setComboLayout(final String[] combos) {

		final WideComboBox comboBox = new WideComboBox(combos);
		comboBox.setFont(FONTS);
		comboBox.setBackground(Color.white);
		comboBox.setMaximumSize(new Dimension(200, 30));

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
