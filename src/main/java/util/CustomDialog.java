package util;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

/**
 * Basic custom JDialog that can be extended for more specified behavior.
 *
 * @author CKeil
 *
 */
public abstract class CustomDialog extends JDialog {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;

	protected JPanel mainPanel;
	protected JButton closeBtn;

	/**
	 * Constructs a basic JDialog with some custom behavior that differs from a
	 * raw JDialog. For example, CustomDialog objects are always modal by
	 * default (cannot be unfocused on Windows machines).
	 *
	 * @param String
	 *            Title to be given to this dialog.
	 */
	public CustomDialog(final String title) {

		super();
		setTitle(title);
		setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);
		
		setupClose();

		this.closeBtn = GUIFactory.createBtn("Close");
		closeBtn.addActionListener(new CloseListener());

		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		getContentPane().add(mainPanel);
	}
	
	private void setupClose() {

		//Escape = close
		KeyStroke escapeKeyStroke = 
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		String closeByEsc = "closeByEsc";

		//Meta-w = close
		KeyStroke metawKeyStroke = 
			KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_MASK);
		String closeByMetaW = "closeByMetaW";

		Action closeDialog = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				dispose();
			}
		};

		JRootPane root = getRootPane();

		root.getInputMap().put(escapeKeyStroke, closeByEsc);
		root.getActionMap().put(closeByEsc, closeDialog);

		root.getInputMap().put(metawKeyStroke, closeByMetaW);
		root.getActionMap().put(closeByMetaW, closeDialog);
	}

	private class CloseListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			dispose();
		}
	}
	
	/**
	 * Used to populate the mainPanel object with GUI components.
	 */
	protected abstract void setupLayout();

	/**
	 * Sets the visibility of exitFrame;
	 *
	 * @param ViewFrame
	 *            The main view, used to center the dialog on screen.
	 * @param boolean Sets the visibility status of the dialog.
	 */
	@Override
	public void setVisible(final boolean visible) {

		pack();
		setLocationRelativeTo(JFrame.getFrames()[0]);

		super.setVisible(visible);
	}
}
