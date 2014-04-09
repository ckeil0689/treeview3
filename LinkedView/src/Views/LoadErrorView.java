package Views;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * A class which prepares JPanels to display in case of errors during the
 * data loading process.
 * @author CKeil
 *
 */
public class LoadErrorView {
	
	// Initial
	private TreeViewFrame tvFrame;
	
	private JPanel homePanel;
	private JPanel errorPanel;
	
	private JLabel errorLabel;
	private JLabel message;
	
	private JButton loadNew;
	
	private String errorMessage = "Weird. No issue has been determined.";
	
	private JLabel jl;

	/**
	 * Constructor with required parameters to show a load-error screen.
	 * @param tvFrame The parent TreeViewFrame
	 * @param message The error message
	 */
	public LoadErrorView(TreeViewFrame tvFrame, String message) {
		
		this.tvFrame = tvFrame;
		this.errorMessage = message;
		
		setupMainPanel();
	}
	
	
	/**
	 * Returns the panel of this view instance, which contains all the content.
	 * @return
	 */
	public void setupMainPanel() {
		
		homePanel = new JPanel();
		homePanel.setLayout(new MigLayout("ins 0"));
		homePanel.setBackground(GUIParams.BG_COLOR);
		
		JPanel title_bg;
		
		title_bg = new JPanel();
		title_bg.setLayout(new MigLayout());
		title_bg.setBackground(GUIParams.MAIN);

		jl = new JLabel("Oh oh!");
		jl.setFont(new Font("Sans Serif", Font.BOLD, 50));
		jl.setForeground(GUIParams.BG_COLOR);

		title_bg.add(jl, "push, alignx 50%, span");

		homePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
	}
	
	public JPanel makeErrorPanel() {
		
		errorPanel = new JPanel();
		errorPanel.setLayout(new MigLayout());
		errorPanel.setOpaque(false);
		
		errorLabel = new JLabel("Looks like we ran into the following issue: ");
		errorLabel.setFont(GUIParams.FONTL);
		errorLabel.setForeground(GUIParams.MAIN);
		
		message = new JLabel(errorMessage);
		message.setFont(GUIParams.FONTS);
		message.setForeground(GUIParams.TEXT);
		
		loadNew = GUIParams.setButtonLayout("Load New File", null);
		
		errorPanel.add(errorLabel, "pushx, alignx 50%, span, wrap");
		errorPanel.add(message, "pushx, alignx 50%, span, wrap");
		errorPanel.add(loadNew, "pushx, alignx 50%, span, wrap");
		
		homePanel.add(errorPanel, "push, alignx 50%");
		
		homePanel.revalidate();
		homePanel.repaint();
		
		return homePanel;
	}
	
	/**
	 * Gives the 'Load New' button an ActionListener to be able to open
	 * new files.
	 * @param l
	 */
	public void addLoadNewListener(ActionListener l) {
		
		loadNew.addActionListener(l);
	}
}
