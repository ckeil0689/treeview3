/* BEGIN_HEADER												   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package Controllers;
import Utilities.CustomDialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * A dialog window class that has an expandable Details pane
 * Original code from:
 * http://www.javaknowledge.info/custom-jdialog-with-expandable-details-pane/
 */
public class CustomDetailsConfirmDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;

	private final JFrame parentFrame;
	private final String title;
	private final String summary;
	private final String details;
	private JOptionPane pane;

	/**
	 * Constructor
	 * 
	 * @param parentFrame
	 * @param title
	 * @param summary
	 * @param details
	 */
	public CustomDetailsConfirmDialog(JFrame parentFrame,String title,String summary,
		String details) {

		super(title);
		
		this.parentFrame = parentFrame;
		this.title = title;
		this.summary = summary;
		this.details = details;

		setupLayout();
	}

	/**
	 * Set up the GUI elements
	 */
	@Override
	protected void setupLayout() {
		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));

		//Create a pane to put everything in and then create a dialog out of it.
		/* final JOptionPane */ pane = new JOptionPane(
			content,
			JOptionPane.WARNING_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);
		final JDialog dialog = pane.createDialog(parentFrame,title);

		//Create the summary message
		JLabel message = new JLabel("<HTML>" + summary + "</HTML>");
		message.setBorder(new EmptyBorder(10,10,10,10));
		message.setAlignmentX(0);
		Dimension labelSize = message.getPreferredSize();
		//If the message is long, set a preferred limited window width
		if(labelSize.width > 500) {
			labelSize.setSize(500,labelSize.height);
			message.setPreferredSize(labelSize);
		}
		content.add(message);

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
		Dimension detailSize = textArea.getPreferredSize();
		detailSize.setSize(labelSize.width,200);
		scrollPane.setPreferredSize(detailSize);

		//Create the details checkbox with a listener that adds/removes the
		//detail message and updates the window size to accommodate it
		JCheckBox cb = new JCheckBox(new AbstractAction() {

			private static final long serialVersionUID = 1L;

			//I'm not sure I understand why this needs to be in an anonymous
			//code block, but it doesn't compile without it.
			//Apparently it's an anonymous subclass to give the parent class an
			//instance initializer:
			//http://stackoverflow.com/questions/891380/java-anonymous-class-that-implements-actionlistener
			{
				this.putValue(Action.SELECTED_KEY,false);
				this.putValue(Action.NAME,"Details");
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				if ((Boolean) this.getValue(Action.SELECTED_KEY)) {
					content.add(scrollPane);
				} else {
					content.remove(scrollPane);
				}
				content.invalidate();
				dialog.invalidate();
				dialog.pack();
			}
		});
		content.add(cb);

		dialog.setResizable(false);
		dialog.pack();
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
	}

	public int getSelection() {
		//Retrieve the selected option
		Object selectedValue = pane.getValue();
		int selection;
		if(selectedValue == null) {
			LogBuffer.println("User closed dialog");
			selection = JOptionPane.CANCEL_OPTION;
		} else {
			switch(((Integer) selectedValue).intValue()) {
			case JOptionPane.OK_OPTION:
				selection = JOptionPane.OK_OPTION;
				LogBuffer.println("User selected OK");
				break;
			case JOptionPane.CANCEL_OPTION:
				selection = JOptionPane.CANCEL_OPTION;
				LogBuffer.println("User selected Cancel");
				break;
			default:
				selection = JOptionPane.CANCEL_OPTION;
				LogBuffer.println("User selected " + selectedValue);
			}
		}

		return(selection);
	}
}
