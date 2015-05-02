package ColorChooser;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.Helper;

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
	
	private Color newColor;
	
	private double startX;
	private double inputX;
	private double finalX;
	
	private boolean colorChanged;
	
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
		startX = thumbBox.getThumbDataVal(thumbIndex);
		finalX = startX;
		
		newColor = t.getColor();

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
		return finalX;
	}
	
	private class SetValueListener implements ActionListener {
		
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if(isValueInvalid()) {
				return;
			}
			
			finalX = inputX;
			
			/* 
			 * Only change if enter or ok button was used to quit and
			 * the entered values were valid.
			 */
			if(colorChanged) {
				t.setColor(newColor);
				colorList.set(index, newColor);
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
			double currentThumbData = thumbBox.getThumbDataVal(index);
			boolean isCurrentThumbData = Helper.nearlyEqual(inputX, currentThumbData);
			isInvalid = thumbBox.hasThumbForDataVal(inputX);
			
			if(t instanceof BoundaryThumb) {
				BoundaryThumb bT = (BoundaryThumb) t;
				
				if(bT.isMin()) {
					isInvalid = !(inputX < thumbBox.getThumbDataVal(1));
					setError("Cannot be bigger than next thumb.");
				} else {
					int last = colorList.size() - 1; // same size as thumbList
					isInvalid = !(inputX < thumbBox.getThumbDataVal(last));
					setError("Cannot be smaller than last thumb.");
				}
				
			} else {
				setError("Value already has a handle!");
			}
			return isInvalid && !isCurrentThumbData;
			
		} catch (final NumberFormatException e) {
			inputField.setText("Enter a valid number!");
			return false;
		}
	}
	
	private void setError(String message) {
		
		enterPrompt.setForeground(GUIFactory.RED1);
		enterPrompt.setText(message);
	}
	
	private class SetColorListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			final Color newCol = JColorChooser.showDialog(parent, 
					"Pick Color for Missing", t.getColor());

			if (newCol != null) {
				newColor = newCol;
				colorIcon.setColor(newCol);
				colorChanged = true;
			}
		}
	}
}
