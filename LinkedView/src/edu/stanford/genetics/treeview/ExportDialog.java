package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class ExportDialog extends CustomDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel previewComp;
	private JComboBox<Object> formatBox;
	private JComboBox<Object> paperBox;
	private JButton exportBtn;
	
	public ExportDialog() {
		
		super("Export");
		setupLayout();
	}
	
	@Override
	protected void setupLayout() {
		
		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);
		
		JPanel contentPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		JPanel optionsPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		
		/* Shows the preview */
		JPanel previewPanel = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS);
		previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
		this.previewComp = GUIFactory.createJPanel(false, 
				GUIFactory.NO_INSETS);
		
		JLabel format = GUIFactory.createLabel("Format:", GUIFactory.FONTS);
		JLabel paper = GUIFactory.createLabel("Paper Size:", GUIFactory.FONTS);
		
		formatBox = new JComboBox<Object>(EXP_FORMATS.values());
		paperBox = new JComboBox<Object>(PAPER_TYPE.values());
		
		previewPanel.add(previewComp, "grow, push");
		
		optionsPanel.add(format, "label, aligny 0");
		optionsPanel.add(formatBox, "aligny 0, growx, wrap");
		
		optionsPanel.add(paper, "label");
		optionsPanel.add(paperBox, "growx, wrap");
		
		contentPanel.add(previewPanel, "w 400!, h 400!");
		contentPanel.add(optionsPanel, "aligny 0%, growx, push");
		
		this.exportBtn = GUIFactory.createBtn("Export");
		closeBtn.setText("Cancel");
		
		JPanel btnPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		btnPanel.add(closeBtn, "tag cancel, pushx, al right");
		btnPanel.add(exportBtn, "al right");
		
		mainPanel.add(contentPanel, "push, grow, w 800!, wrap");
		mainPanel.add(btnPanel, "bottom, pushx, growx, span");
		
		getContentPane().add(mainPanel);
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
	/**
	 * Add export action to export button. Determines what happens, when
	 * the export button is clicked.
	 * @param l The ActionListener
	 */
	public void addExportListener(final ActionListener l) {
		
		exportBtn.addActionListener(l);
	}
	
	/**
	 * @return an int array which contains 2 values. The first value is the 
	 * selected index of the export type JComboBox. The second value is the
	 * selected index of the paper size JComboBox.
	 */
	public int[] getSelectedOptions() {
		
		int[] options = new int[2];
		
		if(formatBox == null) {
			options[0] = -1;
		} else {
			options[0] = formatBox.getSelectedIndex();
		}
		
		if(paperBox == null) {
			options[1] = -1;
		} else {
			options[1] = paperBox.getSelectedIndex();
		}
	
		return options;
	}
	
	public void setPreview(JPanel rowTrees, JPanel colTrees, JPanel matrix) {
		
		if(previewComp == null) {
			LogBuffer.println("Cannot set preview for Export.");
			return;
		}
		
		JPanel filler = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		
		previewComp.add(filler, "w 100!, h 100!");
		previewComp.add(colTrees, "growx, pushx, h 100!, wrap");
		previewComp.add(rowTrees, "growy, pushy, w 100!");
		previewComp.add(matrix, "push, grow");
	}
	
	/**
	 * Add a JComponent (e.g. JPanel) to the preview panel.
	 * @param comp The component to be added.
	 */
	public void addToPreviewPanel(final JComponent comp) {
		
		previewComp.add(comp, "push, grow");
		
		mainPanel.revalidate();
		mainPanel.repaint();
	}
}
