package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.ContrastSelectable;

class ContrastSelector extends JPanel implements AdjustmentListener {

	private static final long serialVersionUID = 1L;

	private JTextField contrastTextField;
	private JFrame top;
	private JDialog d;
	private JScrollBar scrollbar;

	private double contrast;
	private ContrastSelectable client = null;

	public ContrastSelector(final ContrastSelectable c) {

		client = c;
		contrast = client.getContrast();
		setupWidgets();
	}

	private void setupWidgets() {

		this.setLayout(new MigLayout());
		this.setOpaque(false);

		final JPanel inner = new JPanel();
		inner.setOpaque(false);
		inner.setLayout(new MigLayout());

		final JLabel font_label = new JLabel("Value:", SwingConstants.LEFT);
		font_label.setFont(GUIFactory.FONTS);
		inner.add(font_label, "alignx 50%");

		inner.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		contrastTextField = new JTextField(Double.toString(contrast), 5);
		contrastTextField.setMaximumSize(new Dimension(Short.MAX_VALUE,
				Short.MAX_VALUE));
		inner.add(contrastTextField, "alignx 50%, growx");

		contrastTextField.getDocument().addDocumentListener(
				new DocumentListener() {

					@Override
					public void changedUpdate(final DocumentEvent e) {
						updateScrollbarFromText();
					}

					@Override
					public void insertUpdate(final DocumentEvent e) {
						updateScrollbarFromText();
					}

					@Override
					public void removeUpdate(final DocumentEvent e) {
						updateScrollbarFromText();
					}
				});

		this.add(inner, "pushx, alignx 50%, wrap");
		scrollbar = new JScrollBar(Adjustable.HORIZONTAL);
		scrollbar.setValues((int) (contrast * 100.0), 0, 1, 500);
		scrollbar.addAdjustmentListener(this);
		this.add(scrollbar, "pushx, growx");
	}

	public JPanel setPanelLayout() {

		final JPanel panel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT,
				null);

		return panel;
	}

	public void signalAll() {
		// signal changes to array drawer, xml tree
		try {
			final Double size = new Double(contrastTextField.getText());
			contrast = size.doubleValue();
			client.setContrast(contrast);
			client.notifyObservers();
		} catch (final java.lang.NumberFormatException e) {
			// do nothing if cannot convert
		}
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent evt) {
		if (!inUpdateScrollbarFromText) {
			contrastTextField.setText("" + (double) scrollbar.getValue() / 100);
		}
		signalAll();
	}

	private boolean inUpdateScrollbarFromText = false;

	private void updateScrollbarFromText() {
		if (!inUpdateScrollbarFromText) {
			inUpdateScrollbarFromText = true;
			try {
				final Double value = Double.parseDouble(contrastTextField
						.getText());
				scrollbar.setValue((int) (value * 100));
			} catch (final Exception ex) {
				// ignore silently.
			}
			inUpdateScrollbarFromText = false;
		}
	}

	class WindowCloser extends WindowAdapter {
		@Override
		public void windowClosing(final WindowEvent we) {
			// parent.store();
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

	public void showDialog(final JFrame f) {
		d = new JDialog(f, getTitle());
		d.setLayout(new BorderLayout());
		d.add(this, BorderLayout.CENTER);
		top.addWindowListener(new WindowCloser());
		d.pack();
		d.setVisible(true);
	}

	protected String getTitle() {
		return "Contrast Selection";
	}
}