package edu.stanford.genetics.treeview;

import javax.swing.JLabel;
import javax.swing.JPanel;

import utilities.CustomDialog;
import utilities.GUIFactory;

public class HintDialog extends CustomDialog {

	/**
	 * Keeping Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	public HintDialog(final String hint) {
		
		super("Hint");
		
		JPanel hintPanel = GUIFactory.createJPanel(true, GUIFactory.DEFAULT);
		hintPanel.setSize(200, 50);
		
		JLabel indicator = GUIFactory.createLabel(hint, GUIFactory.FONTM);
		hintPanel.add(indicator, "push, grow");
		
		mainPanel.add(hintPanel, "push, grow");
	}
}
