package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.stanford.genetics.treeview.ContrastSelectable;

class ContrastSelector extends JPanel 
implements AdjustmentListener {

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
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel inner = new JPanel();
		JLabel font_label = new JLabel("Value:", JLabel.LEFT);
		inner.add(font_label);

		font_label.setMaximumSize(new Dimension(Short.MAX_VALUE, 
				Short.MAX_VALUE)); 
		inner.setMaximumSize(new Dimension(Short.MAX_VALUE, 
				Short.MAX_VALUE)); 
		contrastTextField = new JTextField(Double.toString(contrast) ,  5);
		contrastTextField.setMaximumSize(new Dimension(Short.MAX_VALUE, 
				Short.MAX_VALUE)); 
		inner.add(contrastTextField);

		contrastTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				updateScrollbarFromText();
			}
			public void insertUpdate(DocumentEvent e) {
				updateScrollbarFromText();
			}
			public void removeUpdate(DocumentEvent e) {
				updateScrollbarFromText();
			}
		});

		add(inner);
		scrollbar = new JScrollBar(JScrollBar.HORIZONTAL);
		scrollbar.setValues((int)(contrast * 100.0), 0, 1, 500);
		scrollbar.addAdjustmentListener(this);
		add(scrollbar);
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