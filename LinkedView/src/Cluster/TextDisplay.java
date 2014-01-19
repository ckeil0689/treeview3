package Cluster;

import javax.swing.JTextArea;

import edu.stanford.genetics.treeview.GUIParams;

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
		setFont(GUIParams.FONTS);
		setForeground(GUIParams.TEXT);
		setEditable(false);
		setBorder(null);
		setLineWrap(true);
		setWrapStyleWord(true);
		setFocusable(false);
	}
}
