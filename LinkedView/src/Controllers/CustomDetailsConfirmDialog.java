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
		//Determine how big to make the line length in the details pane
		int maxLineLen = getLongestLineLength(summary,null);
		//The font size of the detail message for some reason is smaller than
		//that of the summary which determines dialog window size, so this is an
		//estimate to increase the line length in the details to roughly match
		//the pixel line length of the summary
		maxLineLen += (int) ((double) maxLineLen * 0.1);
		int lineLen = maxLineLen > 50 ? maxLineLen : 50;

		//Create the details pane with inserted line wraps
		final JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText("<HTML>" + getWrappedString(details,lineLen,"<BR>\n") +
			"</HTML>");
		textPane.setEditable(false);

		//The message may be too big to fit in a dialog window, so we will throw
		//it in a scrollpane.
		final JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setAlignmentX(0);

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

	/**
	 * Returns the length of the longest line in a string.
	 * 
	 * @param s
	 * @param lineDelimiter - defaults to "\n"
	 * @return
	 */
	public static int getLongestLineLength(final String s,String lineDelimiter) {
		if(lineDelimiter == null) {
			lineDelimiter = "\n";
		}
		int maxLineLen = 0;
		int lastLinePos = -1;
		int i = 0;
		while((i = s.indexOf(lineDelimiter,lastLinePos + 1)) != -1) {
			if((i - lastLinePos) > maxLineLen) {
				maxLineLen = i - lastLinePos;
			}
			lastLinePos = i;
		}
		return(maxLineLen);
	}

	/**
	 * Takes a string and returns the same string with supplied delimiters
	 * inserted where spaces are found just before indicated line length
	 * positions
	 * 
	 * @param s - supplied string
	 * @param lineLength - defaults to 80
	 * @param lineDelimiter - Defaults to "\n"
	 * @return wrapped string
	 */
	public static String getWrappedString(final String s,int lineLength,
		String lineDelimiter) {

		if(((Integer) lineLength) == null || lineLength < 1) {
			lineLength = 80;
		}

		if(lineDelimiter == null) {
			lineDelimiter = "\n";
		}

		StringBuilder sb = new StringBuilder(s);

		//Check whether there are already delimiters in the string
		if(sb.indexOf(lineDelimiter,0) != -1) {
			LogBuffer.println("ERROR: The submitted string appears to already " +
				"have been wrapped.");
			return(s);
		}

		int i = 0;
		while(i + lineLength < sb.length() &&
			(i = sb.lastIndexOf(" ",i + lineLength)) != -1) {

			sb.replace(i,i + 1,lineDelimiter);
			i += lineDelimiter.length() - 1;
		}

		return(sb.toString());
	}
}
