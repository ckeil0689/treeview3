/* BEGIN_HEADER												   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package Controllers;
import Utilities.CustomDialog;
import Utilities.GUIFactory;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * A dialog window class that has an expandable Details pane
 * Original code from:
 * http://www.javaknowledge.info/custom-jdialog-with-expandable-details-pane/
 */
public class CustomDetailsConfirmDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;

	private final String summary;
	private final String details;
	private final String doString;
	private JButton doBtn;
	private int result;
	public static final int CANCEL_OPTION = 0;
	public static final int OK_OPTION = 1;

	/**
	 * Constructor
	 * 
	 * @param summary - The text of the summary
	 * @param details - The text of the initially hidden details scrollpane
	 * @param doString - the text on the 'OK' button
	 */
	public CustomDetailsConfirmDialog(String title,String summary,
		String details,String doString) {

		super(title);

		this.summary = summary;
		this.details = details;
		this.doString = doString;
		this.result = 0;

		setupLayout();
	}

	protected void setupLayout() {

		//Create the summary message
		JLabel message = new JLabel("<HTML>" + summary + "</HTML>");
		message.setBorder(new EmptyBorder(10,10,10,10));
		message.setAlignmentX(0);
		final Dimension labelSize = message.getPreferredSize();
		//If the message is long, set a preferred limited window width
		if(labelSize.width > 500) {
			labelSize.setSize(500,labelSize.height);
			message.setPreferredSize(labelSize);
		}
		mainPanel.add(message,"wrap");
	
		//Create the details pane with inserted line wraps
		final JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setText(details);
		textArea.setEditable(false);
	
		//The message may be too big to fit in a dialog window, so we will throw
		//it in a scrollpane.
		final JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setAlignmentX(0);
		final Dimension detailSize = textArea.getPreferredSize();
		detailSize.setSize(labelSize.width,0);
		scrollPane.setPreferredSize(detailSize);
		scrollPane.setVisible(false);
	
		//Create the details checkbox with a listener that adds/removes the
		//detail message and updates the window size to accommodate it
		JCheckBox cb = new JCheckBox(new AbstractAction() {
	
			private static final long serialVersionUID = 1L;
	
			//I'm not sure I understand why this needs to be in an anonymous
			//code block, but it doesn't compile without it.
			//Apparently it's an anonymous subclass to give the parent class an
			//instance initializer:
			//http://stackoverflow.com/questions/891380/java-anonymous-class-
			//that-implements-actionlistener
			{
				this.putValue(Action.SELECTED_KEY,false);
				this.putValue(Action.NAME,"Details");
			}
	
			@Override
			public void actionPerformed(ActionEvent e) {
				if ((Boolean) this.getValue(Action.SELECTED_KEY)) {
					detailSize.setSize(labelSize.width,212);
					scrollPane.setSize(detailSize);
					Dimension curSize = getPreferredSize();
					setSize(new Dimension(curSize.width,curSize.height));
					scrollPane.setVisible(true);
				} else {
					scrollPane.setVisible(false);
					detailSize.setSize(labelSize.width,0);
					scrollPane.setSize(detailSize);
					Dimension curSize = getPreferredSize();
					setSize(curSize);
				}
				revalidate();
				repaint();
			}
		});
		mainPanel.add(cb,"wrap");
		mainPanel.add(scrollPane,"wrap");

		doBtn = GUIFactory.createBtn(doString);
		doBtn.addActionListener(new DoItListener());
		closeBtn.setText("Cancel");
		JPanel btnPanel = GUIFactory.createJPanel(false,GUIFactory.NO_INSETS);
		btnPanel.add(closeBtn, "tag cancel, pushx, al right");
		btnPanel.add(doBtn, "al right");

		mainPanel.add(btnPanel,"bottom, pushx, growx, span");

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	private class DoItListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			result = CustomDetailsConfirmDialog.OK_OPTION;
			setVisible(false);
			dispose();
		}
	}

	public int showDialog() {
		setVisible(true);
		return result;
	}
}
