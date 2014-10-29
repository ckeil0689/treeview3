package Views;

import javax.swing.JTextArea;

import Utilities.GUIFactory;

/**
 * Small helper class to set up TextAreas for the display of long texts.
 * 
 * @author CKeil
 * 
 */
public class TextDisplay extends JTextArea {

	private static final long serialVersionUID = 1L;

	public TextDisplay(final String text) {

		super(text);
		setOpaque(false);
		setFont(GUIFactory.FONTS);
		setEditable(false);
		setLineWrap(true);
		setWrapStyleWord(true);
		setFocusable(false);
	}
}
