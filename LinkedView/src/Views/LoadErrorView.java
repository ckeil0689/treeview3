package Views;

import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIUtils;
import edu.stanford.genetics.treeview.TreeViewFrame;

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

		homePanel = new JPanel();
		homePanel.setLayout(new MigLayout("ins 0"));
		homePanel.setBackground(GUIUtils.BG_COLOR);

		JPanel title_bg;

		title_bg = new JPanel();
		title_bg.setLayout(new MigLayout());
		title_bg.setBackground(GUIUtils.MAIN);

		JLabel jl = new JLabel("Oh oh!");
		jl.setFont(new Font("Sans Serif", Font.BOLD, 50));
		jl.setForeground(GUIUtils.BG_COLOR);

		title_bg.add(jl, "push, alignx 50%, span");

		homePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
		
		errorPanel = new JPanel();
		errorPanel.setLayout(new MigLayout());
		errorPanel.setOpaque(false);
		
		homePanel.add(errorPanel, "push, alignx 50%");
		
		loadNew = GUIUtils.setButtonLayout("Load New File", null);
	}

	public JPanel makeErrorPanel() {

		errorPanel.removeAll();
		
		JLabel errorLabel = new JLabel("Looks like we ran into the " +
				"following issue: ");
		errorLabel.setFont(GUIUtils.FONTL);
		errorLabel.setForeground(GUIUtils.MAIN);

		JLabel message = new JLabel(errorMessage);
		message.setFont(GUIUtils.FONTS);
		message.setForeground(GUIUtils.TEXT);

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
