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
		this.setMargin(new Insets(0,0,0,0));
	}
	
	@Override
	public Dimension getPreferredSize() {
		
//		Dimension d = super.getPreferredSize();
//		int w = (int) d.getWidth();
//		int h = (int) d.getHeight();
//		
//		int size = (int)(w > h ? w : h);
		
		return new Dimension(size, size);
	}
}
