package Utilities;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.WideComboBox;

public class GUIFactory {

	public static final Font FONTXS = new Font("Sans Serif", Font.PLAIN, 12);
	public static final Font FONTS = new Font("Sans Serif", Font.PLAIN, 14);
	public static final Font FONTS_B = new Font("Sans Serif", Font.BOLD, 14);
	public static final Font FONTM = new Font("Sans Serif", Font.PLAIN, 16);
	public static final Font FONTM_B = new Font("Sans Serif", Font.BOLD, 16);
	public static final Font FONTL = new Font("Sans Serif", Font.PLAIN, 20);
	public static final Font FONTXXL = new Font("Sans Serif", Font.PLAIN, 30);

	public static final Color DEFAULT_BG = UIManager
			.getColor("Panel.background");
	public static final Color MAIN = new Color(30, 144, 255, 255);
	public static final Color DARK_BG = new Color(200, 200, 200, 255);
	public static final Color ELEMENT_HOV = new Color(122, 214, 255, 255);
	public static final Color RED1 = new Color(240, 80, 50, 255);

	public static final int DEFAULT = 0;
	public static final int NO_PADDING_FILL = 1;
	public static final int NO_PADDING = 2;
	public static final int NO_PADDING_X = 3;
	public static final int NO_PADDING_Y = 4;
	public static final int FILL = 5;
	public static final int NO_GAPS = 6;
	public static final int NO_GAPS_NO_INS_X = 7;
	public static final int NO_GAPS_NO_INS_Y = 8;
	public static final int NO_GAPS_NO_INS = 9;
	public static final int NO_GAPS_NO_INS_FILL = 10;
	public static final int TINY_GAPS_INS = 11;
	public static final int DEBUG = 12;

	/**
	 * Creates and returns a simple JPanel with MigLayout to be used as
	 * container. Opaqueness, whether it should have padding, and a custom
	 * background color can be determined. This function is mainly used to
	 * reduce repeating code throughout the source code.
	 *
	 * @param opaque
	 * @param panel_mode
	 * @param backgroundColor
	 * @return
	 */
	public static JPanel createJPanel(final boolean opaque,
			final int panel_mode, final Color backgroundColor) {

		final JPanel panel = new JPanel();
		panel.setOpaque(opaque);

		setComponentLayoutMode(panel, panel_mode);

		// specify background, otherwise default to theme's backgroundColor.
		if (backgroundColor != null) {
			panel.setBackground(backgroundColor);
		}

		return panel;
	}

	/**
	 * Overloaded method to return a JPanel without background color
	 * specification.
	 * 
	 * @param opaque
	 *            Whether the JPanel should be opaque or not.
	 * @param panel_mode
	 *            Sets the main MigLayout constrains for panel insets.
	 * @return A JPanel.
	 */
	public static JPanel createJPanel(final boolean opaque, final int panel_mode) {

		return createJPanel(opaque, panel_mode, null);
	}

	/**
	 * Used to set the layout manager for a JComponent.
	 * 
	 * @param comp
	 *            JComponent which needs a layout manager.
	 * @param panel_mode
	 *            Specific mode that is supposed to be applied to the new
	 *            JComponent's layout.
	 */
	public static void setComponentLayoutMode(JComponent comp,
			final int panel_mode) {

		switch (panel_mode) {
		case NO_PADDING_FILL:
			comp.setLayout(new MigLayout("ins 0", "[grow, fill]"));
			break;

		case NO_PADDING:
			comp.setLayout(new MigLayout("ins 0"));
			break;

		case NO_PADDING_X:
			comp.setLayout(new MigLayout("ins 5 0 5 0"));
			break;

		case NO_PADDING_Y:
			comp.setLayout(new MigLayout("ins 0 5 0 5"));
			break;

		case FILL:
			comp.setLayout(new MigLayout("", "[grow, fill]"));
			break;
			
		case NO_GAPS:
			comp.setLayout(new MigLayout("gap 0!"));
			break;
			
		case NO_GAPS_NO_INS:
			comp.setLayout(new MigLayout("gap 0!, ins 0"));
			break;
			
		case NO_GAPS_NO_INS_X:
			comp.setLayout(new MigLayout("gapx 0!, ins 5 0 5 0", "grow"));
			break;
			
		case NO_GAPS_NO_INS_Y:
			comp.setLayout(new MigLayout("gapy 0!, ins 0 5 0 5", "grow"));
			break;
			
		case NO_GAPS_NO_INS_FILL:
			comp.setLayout(new MigLayout("gap 0!, ins 0, debug", "grow"));
			break;
			
		case TINY_GAPS_INS:
			comp.setLayout(new MigLayout("gap 1!, ins 3"));
			break;
			
		case DEBUG:
			comp.setLayout(new MigLayout("debug"));
			break;

		default:
			comp.setLayout(new MigLayout());
			break;
		}
	}

	/**
	 * Creates and returns a JLabel with the appropriate text color and a small
	 * font size.
	 *
	 * @param text
	 * @return
	 */
	public static JLabel createLabel(final String text, final Font font) {

		final JLabel label = new JLabel(text);
		label.setFont(font);

		return label;
	}

	/**
	 * Create a JTextArea that acts like a JLabel that can wrap. Good for long
	 * passages of text.
	 * 
	 * @return a JTextArea that resembles a JLabel.
	 */
	public static JTextArea createWrappableTextArea() {

		final JTextArea label = new JTextArea();
		label.setFont(GUIFactory.FONTS);
		label.setBorder(null);
		label.setOpaque(false);
		label.setEditable(false);
		label.setFocusable(false);
		label.setLineWrap(true);
		label.setWrapStyleWord(true);

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

		return createColorIconBtn(title, null);
	}

	/**
	 * Creates a button with a title and icon if desired. The method centralizes
	 * the layout setting for buttons so that all buttons will look similar.
	 *
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton createColorIconBtn(final String title, Icon icon) {

		final JButton btn = new JButton(title, icon);
		btn.setFocusPainted(true);

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
	public static SquareButton createSquareBtn(final String title, int size) {

		return new SquareButton(title, size);
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
	 * the layout setting for buttons so that all navigation buttons will look
	 * similar.
	 *
	 * @param title
	 * @param iconFileName
	 * @return
	 */
	public static JButton createIconBtn(final String iconFileName) {

		final JButton button = new JButton();
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));

		// If provided, add icon to button
		if (iconFileName != null) {
			final BufferedImage img = getIconImage(iconFileName);

			if (img != null) {
				button.setIcon(new ImageIcon(img));
				button.setHorizontalTextPosition(SwingConstants.LEFT);
			}
		} else {
			button.setText("Missing Icon");
		}

		button.setPreferredSize(new Dimension(button.getWidth(), button
				.getWidth()));

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

		// Basic layout
		btn.setFont(FONTXXL);

		final Dimension d = btn.getPreferredSize();
		d.setSize(d.getWidth() * 2.5, d.getHeight() * 2.5);

		btn.setPreferredSize(d);

		return btn;
	}

	/**
	 * Generates an image that can be used as an icon on a JButton.
	 *
	 * @param iconFileName
	 * @return
	 */
	public static BufferedImage getIconImage(final String iconFileName) {

		String iconType = "";
		BufferedImage img = null;
		final String subStr = iconFileName.substring(iconFileName.length() - 3,
				iconFileName.length());

		if (!subStr.equalsIgnoreCase("png")) {
			iconType = "_dark.png";
		}

		LogBuffer.println("Icon image used: [" + iconFileName + "].");

		try {
			final ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();
			final InputStream input = classLoader
					.getResourceAsStream(iconFileName + iconType);

			img = ImageIO.read(input);

			input.close();

		} catch (final IOException ex) {
			LogBuffer.logException(ex);
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
	 *
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
	 * @param String
	 *            [] combos
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
