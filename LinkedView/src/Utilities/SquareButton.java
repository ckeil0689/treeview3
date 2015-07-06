package Utilities;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;

public class SquareButton extends JButton {

	/**
	 * Default so no warnings are shown.
	 */
	private static final long serialVersionUID = 1L;

	private int size;

	public SquareButton(String s, int size) {

		super(s);
		this.size = size;
		this.setMargin(new Insets(0, 0, 0, 0));
	}

	@Override
	public Dimension getPreferredSize() {

		return new Dimension(size, size);
	}

	@Override
	public Dimension getMinimumSize() {

		return new Dimension(size, size);
	}
}
