package ColorChooser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class EditThumbDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclispe happy...
	 */
	private static final long serialVersionUID = 1L;

	/* GUI components */
	private final JTextField inputField;
	private final JButton colorButton;
	
	private double inputX;
	
	
	public EditThumbDialog(Thumb thumb, ThumbBox thumbBox) {
		
		super("Edit Thumb");
		
		final JLabel enterPrompt = GUIFactory.createLabel(
				"Enter data value: ", GUIFactory.FONTS);
		
		/* default */
		inputX = thumb.getX();

		inputField = new JTextField();
		inputField.setEditable(true);

		/* Initially display thumb position */
		inputField.setText(Double.toString(thumbBox.getThumbPosition(thumb)));
		inputField.addActionListener(new SetValueListener());
		
		colorButton = GUIFactory.createColorIconBtn("Change Color", 
				new ColorIcon(thumb.getColor()));

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
	
	public double showDialog() {
		
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
	
	private class ColorChanger implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			
		}
	}
}
