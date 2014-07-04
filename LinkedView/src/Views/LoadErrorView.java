package Views;

import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.GUIFactory;
import edu.stanford.genetics.treeview.StringRes;

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

		homePanel = GUIFactory.createJPanel(false, false, null);

		JPanel title_bg = GUIFactory.createJPanel(false, false, null);

		JLabel jl = GUIFactory.createXXLLabel(StringRes.loading_Ohoh);

		title_bg.add(jl, "push, alignx 50%, span");

		homePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
		
		errorPanel = GUIFactory.createJPanel(false, true, null);
		
		homePanel.add(errorPanel, "push, alignx 50%");
		
		loadNew = GUIFactory.setButtonLayout(StringRes.button_loadNewFile);
	}

	public JPanel makeErrorPanel() {

		errorPanel.removeAll();
		
		JLabel errorLabel = GUIFactory.createBigLabel(StringRes.loading_Error);
		JLabel message = GUIFactory.createSmallLabel(errorMessage);

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

		loadNew.addActionListener(l);
	}
	
	public void setErrorMessage(String error) {
		
		this.errorMessage = error;
	}
}
