package Utilities;

import java.awt.Dialog;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Basic custom JDialog that can be extended for more specified behavior.
 * @author CKeil
 *
 */
public class CustomDialog {

	protected JDialog dialog;
	protected JPanel mainPanel;
	
	/**
	 * Constructs a basic JDialog with some custom behavior that differs
	 * from a raw JDialog. For example, CustomDialog objects are always modal
	 * by default (cannot be unfocused on Windows machines).
	 * @param String Title to be given to this dialog.
	 */
	public CustomDialog(String title) {
		
		dialog = new JDialog();
		dialog.setTitle(title);
		dialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setResizable(false);
		
		mainPanel = GUIFactory.createJPanel(false, true, null);
	}
	
	/**
	 * Sets the visibility of exitFrame;
	 * @param ViewFrame The main view, used to center the dialog on screen.
	 * @param boolean Sets the visibility status of the dialog.
	 */
	public void setVisible(boolean visible) {
		
		dialog.pack();
		dialog.setLocationRelativeTo(JFrame.getFrames()[0]);
		dialog.setVisible(visible);
	}
}
