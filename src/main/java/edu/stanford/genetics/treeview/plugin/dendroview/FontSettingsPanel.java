/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.DummyHeaderInfo;
import edu.stanford.genetics.treeview.GUIFactory;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.NatField;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.UrlExtractor;

/**
 * This class allows selection of Fonts for a FontSelectable.
 */
public class FontSettingsPanel implements SettingsPanel {

	private final FontSelectable client;
	private final FontSelectable client2;
	private JComboBox<String> font_choice;
	private JComboBox<String> style_choice;
	private NatField size_field;
	private JLabel exampleField;
	private final JPanel mainPanel;

	private String size_prop, face_prop, style_prop;
	
	// the allowed font styles
	/**
	 * Description of the Field
	 */
	public final static String[] styles = {"Plain", "Italic", 
		"Bold", "Bold Italic" };

	public FontSettingsPanel(final FontSelectable fs, 
			final FontSelectable fs2) {

		this.client = fs;
		this.client2 = fs2;
		
		mainPanel = GUIFactory.createJPanel(false, true, null);
		setupWidgets();
		updateExample();
	}

	public static void main(final String[] argv) {

		final HeaderInfo hi = new DummyHeaderInfo();
		final UrlExtractor ue = new UrlExtractor(hi);

		final FontSelectable fs = new TextView(hi, ue);
		final FontSelectable fs2 = new ArrayNameView(hi);
		fs.setPoints(10);
		final FontSettingsPanel e = new FontSettingsPanel(fs, fs2);
		final JFrame f = new JFrame("Font Settings Test");
		e.showDialog(f, "Font Settings Test");
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent we) {
				System.exit(0);
			}
		});
		f.pack();
		f.setVisible(true);
	}

	@Override
	public void synchronizeFrom() {

		setupWidgets();
	}

	@Override
	public void synchronizeTo() {
		// nothing to do...
	}

	/**
	 * turn a style number from class java.awt.Font into a string
	 * 
	 * @param style
	 *            style index
	 * @return string description
	 */
	public final static String decode_style(final int style) {

		switch (style) {
		
		case Font.PLAIN:
			return styles[0];
			
		case Font.ITALIC:
			return styles[1];
			
		case Font.BOLD:
			return styles[2];
			
		default:
			return styles[3];
		}
	}

	/**
	 * turn a string into a style number
	 * 
	 * @param style
	 *            string description
	 * @return integer encoded representation
	 */
	public final static int encode_style(final String style) {

		return style.equalsIgnoreCase(styles[0]) ? Font.PLAIN
				: style.equalsIgnoreCase(styles[1]) ? Font.ITALIC
						: style.equalsIgnoreCase(styles[2]) ? Font.BOLD : 
							Font.BOLD + Font.ITALIC;
	}

	/**
	 * Create a blocking dialog containing this component
	 * 
	 * @param f
	 *            frame to block
	 */
	public void showDialog(final JFrame parent, final String title) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				
				final JDialog d = new JDialog(parent, title);
				d.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
				d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
				d.add(mainPanel, "push, grow");
				
				d.pack();
				d.setLocationRelativeTo(parent);
				d.setVisible(true);
			}
		});
	}

	private void setupFontChoice() {

		final Font[] fonts = FontSelector.fonts;
		final String[] fontNames = new String[fonts.length];
		for (final Font f : fonts) {

			fontNames[Arrays.asList(fonts).indexOf(f)] = f.getName();
		}

		font_choice = GUIFactory.setComboLayout(fontNames);
		font_choice.setEditable(true);
		AutoCompleteDecorator.decorate(font_choice);
		font_choice.setSelectedItem(client.getFace());
		font_choice.addActionListener(new SelectionListener());
	}

	private void setupStyleChoice() {

		style_choice = GUIFactory.setComboLayout(styles);
		style_choice.setSelectedItem(decode_style(client.getStyle()));
		style_choice.addActionListener(new SelectionListener());
	}

	private void synchronizeClient() {

		final String string = (String) font_choice.getSelectedItem();
		final int i = encode_style((String) style_choice.getSelectedItem());
		final int size = size_field.getNat();

		client.setFace(string);
		client.setStyle(i);
		client.setPoints(size);
		
		client2.setFace(string);
		client2.setStyle(i);
		client2.setPoints(size);
	}

	/**
	 * Sets up widgets
	 */
	private void setupWidgets() {

		mainPanel.removeAll();

		setupFontChoice();
		mainPanel.add(font_choice, "span, wrap");

		setupStyleChoice();
		mainPanel.add(style_choice, "span, wrap");

		size_field = new NatField(client.getPoints(), 3);
		size_field.getDocument().addDocumentListener(
				new DocumentChangeListener());
		mainPanel.add(size_field, "span, wrap");
		
		exampleField = new JLabel("Font Example Text");
		exampleField.setForeground(GUIFactory.TEXT);
		mainPanel.add(exampleField, "pushx, alignx 50%, span");
	}

	private void updateExample() {

		final String string = (String) font_choice.getSelectedItem();
		final int i = encode_style((String) style_choice.getSelectedItem());
		final int size = size_field.getNat();
		// System.out.println("Setting size to " + size);
		exampleField.setFont(new Font(string, i, size));
		exampleField.revalidate();
		exampleField.repaint();
	}
	
	/**
	 * Listener to remove need for a button. When the user chooses a value
	 * for a Swing component with this listener, the font is automatically
	 * updated.
	 * @author CKeil
	 *
	 */
	class SelectionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			updateExample();
			synchronizeClient();
		}
	}
	
	class DocumentChangeListener implements DocumentListener {

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			
		}

		@Override
		public void insertUpdate(DocumentEvent arg0) {
			
			updateExample();
			synchronizeClient();
		}

		@Override
		public void removeUpdate(DocumentEvent arg0) {
			
			updateExample();
			synchronizeClient();
		}
		
	}
}
