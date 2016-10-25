package Utilities;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.UIManager;

public class TinyButton extends JButton {

	/**
	 * Default so no warnings are shown.
	 */
	private static final long serialVersionUID = 1L;

	private final int size = ((Integer)UIManager.get("ScrollBar.width")).intValue();

	public TinyButton(String s) {

		super(s);
		
		setFont(GUIFactory.FONTXS);
	}

	@Override
	public Dimension getPreferredSize() {

		return new Dimension(size, size);
	}

	@Override
	public Dimension getMinimumSize() {

		return new Dimension(size, size);
	}
	
	@Override
	public Dimension getMaximumSize() {

		return new Dimension(size, size);
	}
}
