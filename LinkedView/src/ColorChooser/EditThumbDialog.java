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
	private JLabel enterPrompt;
	private final JTextField inputField;
	private final JButton colorButton;
	private final ColorIcon colorIcon;
	
	private ThumbBox thumbBox;
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
		this.thumbBox = thumbBox;
		this.colorList = colorList;
		this.index = thumbIndex;
		
		enterPrompt = GUIFactory.createLabel("Set data value: ", 
				GUIFactory.FONTS);
		
		/* default */
		inputX = thumb.getX();

		inputField = new JTextField();
		inputField.setEditable(true);

		/* Initially display thumb position */
		inputField.setText(Double.toString(thumbBox.getThumbDataVal(thumbIndex)));
		inputField.addActionListener(new SetValueListener());
		
		colorIcon = new ColorIcon(thumb.getColor());
		colorButton = GUIFactory.createColorIconBtn("Change Color", colorIcon);
		colorButton.addActionListener(new SetColorListener());

		final JButton okButton = GUIFactory.createBtn("OK");
		okButton.addActionListener(new SetValueListener());

		final JPanel panel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT, null);

		panel.add(colorButton, "push, alignx 0%, wrap");
		panel.add(enterPrompt, "push, span, wrap");
		panel.add(inputField, "push, growx, span, wrap");
		panel.add(okButton, "pushx, alignx 50%");

		mainPanel.add(panel, "w 200::, h 150::");
	}
	
	protected double showDialog(JPanel parent) {
		
		setVisible(true);
		return inputX;
	}
	
	private class SetValueListener implements ActionListener {
		
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if(isValueInvalid()) {
				return;
			}
			
			setVisible(false);
			dispose();
		}
	}
	
	/**
	 * Parses the input and checks if it should be accepted.
	 * @return Whether the user input is considered valid.
	 */
	private boolean isValueInvalid() {
		
		boolean isInvalid = false;
		try {
			inputX = Double.parseDouble(inputField.getText());
			isInvalid = thumbBox.hasThumbForDataVal(inputX);
			
			enterPrompt.setForeground(GUIFactory.RED1);
			enterPrompt.setText("Value already has a handle!");
				
			return isInvalid;
			
		} catch (final NumberFormatException e) {
			inputField.setText("Enter a valid number!");
			return false;
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
