package ColorChooser;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class EditThumbDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	/* GUI components */
	private JPanel parent;
	private final JTextField inputField;
	private final JButton colorButton;
	private final ColorIcon colorIcon;
	
	private List<Color> colorList;
	private final Thumb t;
	private final int index;
	
	private double inputX;
	
	/**
	 * Constructs a dialog which is allows the user to edit the details 
	 * of a thumb.
	 * @param thumb The thumb to be edited.
	 * @param thumbIndex The index of the thumb in ColorPicker's thumbList.
	 * @param thumbBox The thumbBox in which the thumbs are painted.
	 * @param colorList List of colors which may be updated by this dialog.
	 */
	public EditThumbDialog(Thumb thumb, int thumbIndex, ThumbBox thumbBox, 
			List<Color> colorList) {
		
		super("Edit Thumb");
		
		this.t = thumb;
		this.colorList = colorList;
		this.index = thumbIndex;
		
		final JLabel enterPrompt = GUIFactory.createLabel(
				"Enter data value: ", GUIFactory.FONTS);
		
		/* default */
		inputX = thumb.getX();

		inputField = new JTextField();
		inputField.setEditable(true);

		/* Initially display thumb position */
		inputField.setText(Double.toString(thumbBox.getThumbPosition(thumb)));
		inputField.addActionListener(new SetValueListener());
		
		colorIcon = new ColorIcon(thumb.getColor());
		colorButton = GUIFactory.createColorIconBtn("Change Color", colorIcon);
		colorButton.addActionListener(new SetColorListener());

		final JButton okButton = GUIFactory.createBtn("OK");
		okButton.addActionListener(new SetValueListener());

		final JPanel panel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		panel.add(colorButton, "pushx, alignx 0%, wrap");
		panel.add(enterPrompt, "push, span, wrap");
		panel.add(inputField, "push, growx, span, wrap");
		panel.add(okButton, "pushx, alignx 50%");

		getContentPane().add(panel);
	}
	
	public double showDialog(JPanel parent) {
		
		setVisible(true);
		return inputX;
	}
	
	private class SetValueListener implements ActionListener {
		
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			try {
				inputX = Double.parseDouble(inputField.getText());
				
				setVisible(false);
				dispose();
				
			} catch (final NumberFormatException e) {
				inputField.setText("Enter a valid number!");
			}
		}
	}
	
	private class SetColorListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			final Color newCol = JColorChooser.showDialog(parent, 
					"Pick Color for Missing", t.getColor());

			if (newCol != null) {
				t.setColor(newCol);
				colorIcon.setColor(newCol);
				colorList.set(index, newCol);
			}
		
		}
	}
}
