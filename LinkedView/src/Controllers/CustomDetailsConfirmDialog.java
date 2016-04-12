/* BEGIN_HEADER												   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package Controllers;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * A dialog window class that has an expandable Details pane
 * Original code from:
 * http://www.javaknowledge.info/custom-jdialog-with-expandable-details-pane/
 */
public class CustomDetailsConfirmDialog {

	/**
	 * Shows a confirmation dialog of type OK_CANCEL_OPTION with a confirmation
	 * message and initially hidden explanation.  To be used for confirmations
	 * which have extensive ramifications a user should really understand before
	 * confirming, but which would be too verbose for someone who knows what
	 * they're doing.  The dialog is presented as a warning dialog.
	 * 
	 * @param parentFrame - For relative positioning of the dialog
	 * @param title - For the title in the dialog window's title bar
	 * @param summary - The short version of the confirmation message.
	 * @param details - An initially hidden extended explanation of the
	 *                  confirmation message
	 * @return
	 */
	public static int showDialog(Frame parentFrame,String title,String summary,
		String details) {

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content,BoxLayout.PAGE_AXIS));

		JLabel message = new JLabel("<HTML>" + summary + "</HTML>");
		message.setBorder(new EmptyBorder(10,10,10,10));
		Dimension labelSize = message.getPreferredSize();
		message.setPreferredSize(labelSize);
		content.add(message);

//		final JTextArea textPane = new JTextArea();
		final JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(details);
//		textPane.setWrapStyleWord(true);
//		textPane.setSize((int) labelSize.getWidth(),50);
		final Dimension detailSize = new Dimension((int) labelSize.getWidth(),(int) labelSize.getHeight() + 150);
//		detailSize.setSize(labelSize.getWidth(),100);
		textPane.setMaximumSize(detailSize);
		textPane.setSize(detailSize);
		content.setMaximumSize(detailSize);
		content.setSize(detailSize);
//		textPane.setSize(50,50);
		textPane.setEditable(false);

		final JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setAlignmentX(0);
		scrollPane.setMaximumSize(detailSize);
		scrollPane.setSize(detailSize);

		final JOptionPane pane = new JOptionPane(
			content,
			JOptionPane.WARNING_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION);
		final JDialog dialog = pane.createDialog(parentFrame,title);
		pane.setMaximumSize(detailSize);
		dialog.setMaximumSize(detailSize);
		final Dimension winSize = new Dimension((int) detailSize.getWidth() + 200,(int) labelSize.getHeight() + 150);
		final Dimension largeWinSize = new Dimension((int) detailSize.getWidth() + 200,(int) detailSize.getHeight() + 150);

		JCheckBox cb = new JCheckBox(new AbstractAction() {

			private static final long serialVersionUID = 1L;

			{
				this.putValue(Action.SELECTED_KEY,false);
				this.putValue(Action.NAME,"Details");
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Dimension newWinSize = new Dimension();
				if ((Boolean) this.getValue(Action.SELECTED_KEY)) {
					content.add(scrollPane);
					newWinSize = largeWinSize;
				} else {
					content.remove(scrollPane);
					newWinSize = winSize;
				}
				content.invalidate();
				content.setMaximumSize(detailSize);
				content.setSize(detailSize);
				dialog.invalidate();
//				dialog.pack();
				dialog.setMaximumSize(newWinSize);
				dialog.setSize(newWinSize);
			}
		});
		content.add(cb);

		dialog.setResizable(false);
		dialog.pack();
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(winSize);
		dialog.setVisible(true);

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
