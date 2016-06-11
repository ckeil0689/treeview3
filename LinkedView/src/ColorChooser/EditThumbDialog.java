package ColorChooser;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.Helper;

public class EditThumbDialog extends CustomDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	private final Thumb t;
	private final int index;
	private final int thumbIndex;
	private final double mean;
	private final double median;
	private final double center;
	private final double min;
	private final double max;

	/* GUI components */
	private JLabel enterPrompt;
	private JTextArea valueStatus;
	private JTextField inputField;
	private JButton colorButton;
	private ColorIcon colorIcon;

	private ThumbBox thumbBox;
	private List<Color> colorList;

	private Color newColor;

	private double startX;
	private double inputX;
	private double finalX;

	private boolean colorChanged;

	/**
	 * Constructs a dialog which is allows the user to edit the details of a
	 * thumb.
	 * 
	 * @param thumb
	 *            The thumb to be edited.
	 * @param thumbIndex
	 *            The index of the thumb in ColorPicker's thumbList.
	 * @param thumbBox
	 *            The thumbBox in which the thumbs are painted.
	 * @param colorList
	 *            List of colors which may be updated by this dialog.
	 *            
	 */
	public EditThumbDialog(Thumb thumb,int thumbIndex,ThumbBox thumbBox,
		List<Color> colorList,final double mean,final double median,
		final double center,final double min,final double max) {

		super("Edit Color");

		this.t = thumb;
		this.thumbIndex = thumbIndex;
		this.thumbBox = thumbBox;
		this.colorList = colorList;
		this.index = thumbIndex;

		this.mean = Helper.roundDouble(mean, 4);
		this.median = Helper.roundDouble(median, 4);
		this.center = Helper.roundDouble(center, 4);
		this.min = Helper.roundDouble(min, 4);
		this.max = Helper.roundDouble(max, 4);

		setupLayout();
	}

	protected double showDialog(JPanel parent) {

		setVisible(true);
		return finalX;
	}

	private class SetValueListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			if (isValueInvalid()) {
				return;
			}

			finalX = inputX;

			/*
			 * Only change if enter or ok button was used to quit and the
			 * entered values were valid.
			 */
			if (colorChanged) {
				t.setColor(newColor);
				colorList.set(index, newColor);
			}

			setVisible(false);
			dispose();
		}
	}

	/**
	 * Parses the input and checks if it should be accepted.
	 * 
	 * @return Whether the user input is considered valid.
	 */
	private boolean isValueInvalid() {

		boolean isInvalid = false;
		int minThumbIdx = 0;
		int maxThumbIdx = colorList.size() - 1;
		setError(""); // ensure label reset
		
		try {
			inputX = Double.parseDouble(inputField.getText());
			double thumbVal = thumbBox.calcThumbVal(index);
			boolean isInputEqualToThumbVal = Helper.nearlyEqual(inputX, thumbVal);

			// If edited thumb is boundary thumb
			if (t instanceof BoundaryThumb) {
				BoundaryThumb bT = (BoundaryThumb) t;
				double otherBoundVal;
				int otherBoundIdx;
				// Min bound
				if (bT.isMin()) {
					otherBoundIdx = maxThumbIdx;
					otherBoundVal = thumbBox.calcThumbVal(otherBoundIdx);
					isInvalid = !(inputX < otherBoundVal);
					if(isInvalid) {
						setError("Cannot be equal to or greater than right-most handle.");
					} 
        
				// Max bound
				} else {
					otherBoundIdx = minThumbIdx;
					otherBoundVal = thumbBox.calcThumbVal(otherBoundIdx);
					isInvalid = !(inputX > otherBoundVal);
					if(isInvalid) {
						setError("Cannot be equal to or less than left-most handle.");
					}
				}
		  // Overshoots or equals min boundary
			} else if(!(inputX > thumbBox.calcThumbVal(minThumbIdx))) {
				// Left here in case we decide to do a replace in the future
//				thumbBox.replaceThumbAt(minThumbIdx, t);
				setError("Value has to be greater than left-most handle.");
				isInvalid = true;
				
			// Overshoots or equals max boundary
			} else if(!(inputX < thumbBox.calcThumbVal(maxThumbIdx))) {
			// Left here in case we decide to do a replace in the future
//				thumbBox.replaceThumbAt(maxThumbIdx, t);
				setError("Value has to be less than right-most handle.");
				isInvalid = true;
				
			// Replace other handle if other handle with same data value exists
			} else if(!isInputEqualToThumbVal && thumbBox.hasThumbForVal(inputX)) {
				setError("A handle exists for this value.");
				thumbBox.removeThumbWithVal(inputX);
			}
			
			return isInvalid;

		} catch (final NumberFormatException e) {
			setError("Enter a valid number!");
			return true;
		}
	}

	/**
	 * Changes the text of the error JTextArea to the supplied message.
	 * @param message - The text to display.
	 */
	private void setError(final String message) {

		valueStatus.setText(message);
		this.setVisible(true); // message may chase mainPanel size
	}

	private class SetColorListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			final Color newCol = JColorChooser.showDialog(mainPanel.getParent(),
					"Pick Color", t.getColor());

			if (newCol != null) {
				newColor = newCol;
				colorIcon.setColor(newCol);
				colorChanged = true;
			}
		}
	}

	private class SetToMeanListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			inputField.setText(Double.toString(mean));
		}
	}
	private class SetToMedianListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			inputField.setText(Double.toString(median));
		}
	}
	private class SetToCenterListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			inputField.setText(Double.toString(center));
		}
	}
	private class SetToMinListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			inputField.setText(Double.toString(min));
		}
	}
	private class SetToMaxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			inputField.setText(Double.toString(max));
		}
	}

	private class CloseListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			dispose();
		}
	}

	@Override
	protected void setupLayout() {

		final JLabel colorPrompt = GUIFactory.createLabel("Set color:", 
			GUIFactory.FONTS);
		enterPrompt = GUIFactory.createLabel("Set data value:", 
			GUIFactory.FONTS);
		
		valueStatus = GUIFactory.createWrappableTextArea();
		valueStatus.setForeground(GUIFactory.RED1);

		/* default */
		startX = thumbBox.calcThumbVal(thumbIndex);
		finalX = startX;

		newColor = t.getColor();

		inputField = new JTextField();
		inputField.setEditable(true);

		/* Initially display thumb position */
		inputField
				.setText(Double.toString(thumbBox.calcThumbVal(thumbIndex)));
		inputField.addActionListener(new SetValueListener());

		colorIcon = new ColorIcon(t.getColor());
		colorButton = GUIFactory.createColorIconBtn("",colorIcon);
		colorButton.addActionListener(new SetColorListener());

		final JButton meanBtn = GUIFactory.getTextButton(String.valueOf(mean));
		meanBtn.addActionListener(new SetToMeanListener());
		final JButton medianBtn = GUIFactory.getTextButton(String.valueOf(median));
		medianBtn.addActionListener(new SetToMedianListener());
		final JButton centerBtn = GUIFactory.getTextButton(String.valueOf(center));
		centerBtn.addActionListener(new SetToCenterListener());
		final JButton minBtn = GUIFactory.getTextButton(String.valueOf(min));
		minBtn.addActionListener(new SetToMinListener());
		final JButton maxBtn = GUIFactory.getTextButton(String.valueOf(max));
		maxBtn.addActionListener(new SetToMaxListener());

		final JLabel meanLabel = GUIFactory.createLabel("Mean:",
			GUIFactory.FONTS_B);
		final JLabel medianLabel = GUIFactory.createLabel("Median:",
			GUIFactory.FONTS_B);
		final JLabel centerLabel = GUIFactory.createLabel("Center:",
			GUIFactory.FONTS_B);
		final JLabel minLabel = GUIFactory.createLabel("Min:",
			GUIFactory.FONTS_B);
		final JLabel maxLabel = GUIFactory.createLabel("Max:",
			GUIFactory.FONTS_B);

		final JButton okButton = GUIFactory.createBtn("OK");
		okButton.addActionListener(new SetValueListener());

		final JButton closeBtn = GUIFactory.createBtn("Close");
		closeBtn.setText("Cancel");
		closeBtn.addActionListener(new CloseListener());

		final JPanel panel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		panel.add(colorPrompt, "align right, span 2");
		panel.add(colorButton, "push, span, align left, wrap");
		panel.add(enterPrompt, "align right, span 2");
		panel.add(inputField, "growx, span 2, align left, wrap");

		panel.add(valueStatus, "growx, span, wrap");

		panel.add(meanLabel, "pushx, align right");
		panel.add(meanBtn, "pushx, align left, gapright 8px");
		panel.add(minLabel, "pushx, align right, gapleft 8px");
		panel.add(minBtn, "pushx, align left, wrap");
		panel.add(medianLabel, "pushx, align right");
		panel.add(medianBtn, "pushx, align left, gapright 8px");
		panel.add(centerLabel, "pushx, align right, gapleft 8px");
		panel.add(centerBtn, "pushx, align left, wrap");
		panel.add(GUIFactory.createLabel("",GUIFactory.FONTS));
		panel.add(GUIFactory.createLabel("",GUIFactory.FONTS));
		panel.add(maxLabel, "pushx, align right, gapleft 8px");
		panel.add(maxBtn, "pushx, align left, wrap");

		panel.add(GUIFactory.createLabel(" ",GUIFactory.FONTS),"wrap");

		panel.add(closeBtn, "pushx, span 2, align right");
		panel.add(okButton, "pushx, span 2, align left");

		mainPanel.add(panel, "w 200::, h 150::");
	}
}
