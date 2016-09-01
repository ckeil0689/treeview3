package edu.stanford.genetics.treeview.plugin.dendroview;

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MatrixScrollBarUI extends BasicScrollBarUI {

	private JButton leftPlus;
	private JButton leftMinus;
	private JButton rightPlus;
	private JButton rightMinus;
	
	public MatrixScrollBarUI(JButton leftPlus, JButton leftMinus, 
			JButton rightPlus, JButton rightMinus) {
		
		super();
		
		this.leftPlus = leftPlus;
		this.leftMinus = leftMinus;
		this.rightPlus = rightPlus;
		this.rightMinus = rightMinus;
	}
	
	@Override
	protected void installComponents() {
		
		scrollbar.add(leftPlus);
		scrollbar.add(leftMinus);
		scrollbar.add(incrButton);
		scrollbar.add(decrButton);
		scrollbar.add(rightMinus);
		scrollbar.add(rightPlus);
		
		scrollbar.setEnabled(scrollbar.isEnabled());
	}
}
