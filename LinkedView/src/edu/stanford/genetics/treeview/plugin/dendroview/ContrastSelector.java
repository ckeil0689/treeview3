package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.ContrastSelectable;
import edu.stanford.genetics.treeview.GUIParams;

class ContrastSelector extends JPanel implements AdjustmentListener {

	private static final long serialVersionUID = 1L;
	
	private JTextField contrastTextField;
	private JFrame top;
	private JDialog d;
	private JScrollBar scrollbar;

	private double contrast;
	private ContrastSelectable client = null;

	public ContrastSelector(ContrastSelectable c) {
		
		client = c;
		contrast = client.getContrast();
		setupWidgets();
	}

	private void setupWidgets() {	
		
		this.setLayout(new MigLayout());
		this.setOpaque(false);
		this.setBorder(BorderFactory.createLineBorder(GUIParams.BORDERS, 
				EtchedBorder.LOWERED));
		
		JPanel inner = new JPanel();
		inner.setOpaque(false);
		inner.setLayout(new MigLayout());
		
		JLabel font_label = new JLabel("Value:", SwingConstants.LEFT);
		font_label.setForeground(GUIParams.TEXT);
		inner.add(font_label, "alignx 50%");

		font_label.setMaximumSize(new Dimension(Short.MAX_VALUE, 
				Short.MAX_VALUE)); 
		inner.setMaximumSize(new Dimension(Short.MAX_VALUE, 
				Short.MAX_VALUE)); 
		
		contrastTextField = new JTextField(Double.toString(contrast), 5);
		contrastTextField.setMaximumSize(new Dimension(Short.MAX_VALUE, 
				Short.MAX_VALUE)); 
		inner.add(contrastTextField, "alignx 50%, growx");

		contrastTextField.getDocument().addDocumentListener(
				new DocumentListener() {
			
					@Override
					public void changedUpdate(DocumentEvent e) {
						updateScrollbarFromText();
					}
					@Override
					public void insertUpdate(DocumentEvent e) {
						updateScrollbarFromText();
					}
					@Override
					public void removeUpdate(DocumentEvent e) {
						updateScrollbarFromText();
					}
		});

		this.add(inner, "pushx, alignx 50%, wrap");
		scrollbar = new JScrollBar(Adjustable.HORIZONTAL);
		scrollbar.setBackground(GUIParams.BG_COLOR);
		scrollbar.setValues((int)(contrast * 100.0), 0, 1, 500);
		scrollbar.addAdjustmentListener(this);
		this.add(scrollbar, "pushx, growx");
	}
	
	public JButton setButtonLayout(String title){
		
		Font buttonFont = new Font("Sans Serif", Font.PLAIN, 14);
		
		JButton button = new JButton(title);
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
  		button.setPreferredSize(d);
  		
  		button.setFont(buttonFont);
  		button.setOpaque(true);
  		button.setBackground(GUIParams.ELEMENT);
  		button.setForeground(GUIParams.BG_COLOR);
  		
  		return button;
	}
	
	public JPanel setPanelLayout() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.setBackground(GUIParams.BG_COLOR);
		panel.setBorder(BorderFactory.createLineBorder(GUIParams.BORDERS, 
						EtchedBorder.LOWERED));
		
		return panel;
	}

	public void signalAll() {
		// signal changes to array drawer, xml tree
		try {
			Double size = new Double(contrastTextField.getText());
			contrast = size.doubleValue();
			client.setContrast(contrast);
			client.notifyObservers();
		} catch (java.lang.NumberFormatException e) {
			// do nothing if cannot convert
		}
	}
	@Override
	public void adjustmentValueChanged(AdjustmentEvent evt) {
		if (!inUpdateScrollbarFromText) {
			contrastTextField.setText("" +(double) scrollbar.getValue() / 100);	
		}
		signalAll();
	}
	private boolean inUpdateScrollbarFromText = false;
	private void updateScrollbarFromText() {
		if (!inUpdateScrollbarFromText) {
			inUpdateScrollbarFromText = true;
			try {
				Double value = Double.parseDouble(contrastTextField.getText());
				scrollbar.setValue((int)(value*100));
			} catch (Exception ex) {
				// ignore silently.
			}
			inUpdateScrollbarFromText = false;
		}
	}


	class WindowCloser extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent we) {
			//	parent.store();
			we.getWindow().dispose();
		}
	}

	public void makeTop() {
		top = new JFrame(getTitle());
		top.add(this);
		top.addWindowListener(new WindowCloser());
		top.pack();
		top.setVisible(true);
	}
	public void showDialog(JFrame f) {
		d = new JDialog(f, getTitle());
		d.setLayout(new BorderLayout());
		d.add(this, BorderLayout.CENTER);
		top.addWindowListener( new WindowCloser());
		d.pack();
		d.setVisible(true);
	}
	protected String getTitle() {
		return "Contrast Selection";
	}
}