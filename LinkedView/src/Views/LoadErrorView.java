package Views;

import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Utilities.GUIFactory;
import Utilities.StringRes;

/**
 * A class which prepares JPanels to display in case of errors during the data
 * loading process.
 *
 * @author CKeil
 *
 */
public class LoadErrorView {

	private JPanel homePanel;
	private JPanel errorPanel;

	private JButton loadNew;

	private String errorMessage = "Weird. No issue has been determined.";

	/**
	 * Constructor with required parameters to show a load-error screen.
	 *
	 * @param tvFrame
	 *            The parent TreeViewFrame
	 * @param message
	 *            The error message
	 */
	public LoadErrorView() {

		setupMainPanel();
	}

	/**
	 * Returns the panel of this view instance, which contains all the content.
	 *
	 * @return
	 */
	public void setupMainPanel() {

		homePanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING, null);

		final JPanel title_bg = GUIFactory.createJPanel(false,
				GUIFactory.NO_PADDING, null);

		final JLabel jl = GUIFactory.createLabel(StringRes.load_Ohoh,
				GUIFactory.FONTXXL);

		title_bg.add(jl, "push, alignx 50%, span");

		homePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");

		errorPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);

		homePanel.add(errorPanel, "push, alignx 50%");

		loadNew = GUIFactory.createBtn(StringRes.btn_LoadNewFile);
	}

	public JPanel makeError() {

		errorPanel.removeAll();

		final JLabel errorLabel = GUIFactory.createLabel(StringRes.load_Error,
				GUIFactory.FONTL);
		final JLabel message = GUIFactory.createLabel(errorMessage,
				GUIFactory.FONTS);

		errorPanel.add(errorLabel, "pushx, alignx 50%, span, wrap");
		errorPanel.add(message, "pushx, alignx 50%, span, wrap");
		errorPanel.add(loadNew, "pushx, alignx 50%, span, wrap");

		homePanel.revalidate();
		homePanel.repaint();

		return homePanel;
	}

	/**
	 * Gives the 'Load New' button an ActionListener to be able to open new
	 * files.
	 *
	 * @param l
	 */
	public void addLoadNewListener(final ActionListener l) {

		if (loadNew.getActionListeners().length == 0) {
			loadNew.addActionListener(l);
		}
	}

	public void setErrorMessage(final String error) {

		this.errorMessage = error;
	}
}
