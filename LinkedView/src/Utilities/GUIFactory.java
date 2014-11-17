package Utilities;

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
import javax.swing.UIManager;

import edu.stanford.genetics.treeview.WideComboBox;
import net.miginfocom.swing.MigLayout;

public class GUIFactory {

	// Default
	public static String QUESTIONICON = "questionIcon_dark.png";

	public static final Font FONTS = new Font("Sans Serif", Font.PLAIN, 14);
	public static final Font FONTM = new Font("Sans Serif", Font.PLAIN, 16);
	public static final Font FONTL = new Font("Sans Serif", Font.PLAIN, 20);
	public static final Font FONTXXL = new Font("Sans Serif", Font.PLAIN, 30);

	public static final Color DEFAULT_BG = UIManager.getColor("Panel.background");
	public static final Color MAIN = new Color(30, 144, 255, 255);
	public static final Color ELEMENT_HOV = new Color(122, 214, 255, 255);
	public static final Color RED1 = new Color(240, 80, 50, 255);
	
	public static final int DEFAULT = 0;
	public static final int NO_PADDING_FILL = 1;
	public static final int NO_PADDING = 2;
	public static final int FILL = 3;
	
	/**
	 * Creates and returns a simple JPanel with MigLayout to be used 
	 * as container. 
	 * Opaqueness, whether it should have padding, and a custom background
	 * color can be determined. This function is mainly used to reduce
	 * repeating code throughout the source code.
	 * @param opaque
	 * @param panel_mode
	 * @param backgroundColor
	 * @return
	 */
	public static JPanel createJPanel(boolean opaque, int panel_mode, 
			Color backgroundColor) {
		
		JPanel panel = new JPanel();
		panel.setOpaque(opaque);
		
		switch(panel_mode) {
			case NO_PADDING_FILL:
				panel.setLayout(new MigLayout("ins 0", "[grow, fill]"));
				break;
				
			case NO_PADDING:
				panel.setLayout(new MigLayout("ins 0"));
				break;
			case FILL:
				panel.setLayout(new MigLayout("", "[grow, fill]"));
				break;
			default: 
				panel.setLayout(new MigLayout());
				break;
		}
		
		// specify background, otherwise default to theme's backgroundColor.
		if(backgroundColor != null) {
			panel.setBackground(backgroundColor);
		}
		
		return panel;
	}
	
	/**
	 * Creates and returns a JLabel with the appropriate text color and a
	 * small font size.
	 * @param text
	 * @return
	 */
	public static JLabel createLabel(String text, Font font) {
		
		JLabel label = new JLabel(text);
		label.setFont(font);
		
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
	public static JButton createBtn(final String title) {

		final JButton btn = new JButton(title);
		btn.setFocusPainted(false);

		return btn;
	}
	
	/**
	 * Creates a button with a title and icon if desired. The method centralizes
	 * the layout setting for buttons so that all buttons will look similar.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JToggleButton createToggleBtn(final String title) {

		final JToggleButton btn = new JToggleButton(title);
		btn.setFocusPainted(false);

		return btn;
	}
	
	/**
	 * Creates a button with a title and icon if desired. The method centralizes
	 * the layout setting for buttons so that all navigation buttons 
	 * will look similar.
	 * 
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton createNavBtn(final String iconFileName) {

		final JButton button = new JButton();
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		// If provided, add icon to button
		if (iconFileName != null) {
			Image img = getIconImage(iconFileName);
			
			if(img != null) {
				button.setIcon(new ImageIcon(img));
				button.setHorizontalTextPosition(SwingConstants.LEFT);
			}
		} else {
			button.setText("Missing Icon");
		}
		
		button.setPreferredSize(new Dimension(button.getWidth(), 
				button.getWidth()));

		final Dimension d = button.getPreferredSize();
		d.setSize(d.getWidth() * 1.5, d.getHeight() * 1.5);
		
		if (iconFileName != null) {
			button.setMinimumSize(new Dimension(40, 40));
		}

		button.setPreferredSize(d);

		return button;
	}
	
	/**
	 * Sets a layout for a very large button.
	 * 
	 * @param title
	 * @return
	 */
	public static JButton createLargeBtn(final String title) {

		final JButton btn = new JButton(title);
		btn.setFocusPainted(false);

		// Basic layout
		btn.setFont(FONTXXL);
		
		final Dimension d = btn.getPreferredSize();
		d.setSize(d.getWidth() * 2.5, d.getHeight() * 2.5);

		btn.setPreferredSize(d);
		
		return btn;
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
			iconType = "_dark.png";
//			if (dark) {
//				iconType = "_dark.png";
//
//			} else {
//				iconType = "_light.png";
//			}
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
	public static JRadioButton createRadioBtn(final String title) {

		final JRadioButton menuBtn = new JRadioButton(title);
		menuBtn.setOpaque(false);
		menuBtn.setFont(FONTS);
		menuBtn.setBorder(null);
		menuBtn.setFocusPainted(false);

		return menuBtn;
	}

	/**
	 * Method to setup a JProgressBar
	 * 
	 * @param pBar
	 * @param text
	 * @return
	 */
	public static JProgressBar createPBar() {

		final JProgressBar pBar = new JProgressBar();
		pBar.setMinimum(0);
		pBar.setStringPainted(true);
		pBar.setMaximumSize(new Dimension(2000, 20));
		pBar.setVisible(true);

		return pBar;
	}

	/**
	 * Setting up a layout for a wide ComboBox object. 
	 * @param combo
	 * @return
	 */
	public static WideComboBox createWideComboBox(final String[] combos) {

		final WideComboBox comboBox = new WideComboBox(combos);
		comboBox.setPreferredSize(new Dimension(300, 30));
		comboBox.setMinimumSize(comboBox.getPreferredSize());

		return comboBox;
	}
	
	/**
	 * Setting up a general layout for a ComboBox object The method is used to
	 * make all ComboBoxes appear consistent in aesthetics.
	 * 
	 * @param String[] combos
	 * @return JComboBox<String>
	 */
	public static JComboBox<String> createComboBox(final String[] combos) {

		final JComboBox<String> comboBox = new JComboBox<String>(combos);

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

		return header;
	}
}
