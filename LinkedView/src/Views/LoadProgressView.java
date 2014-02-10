package Views;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * Displays a panel with a label and JProgressBar to indicate loading
 * progress and give feedback in case of loading errors.
 */
public class LoadProgressView extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JProgressBar loadBar;
	private JLabel loadLabel;

	public LoadProgressView(TreeViewFrame tvFrame) {
		
		setLayout(new MigLayout("ins 0"));
		setOpaque(false);
		
		loadLabel = new JLabel();
		loadLabel.setFont(GUIParams.FONTL);
		loadLabel.setForeground(GUIParams.TEXT);
		
		loadBar = GUIParams.setPBarLayout();
		
		add(loadLabel, "push, alignx 50%, aligny 100%, wrap");
		add(loadBar, "push, w 70%, alignx 50%, aligny 0%");
	}
	
	// LoadBar functions
	/**
	 * Updates the loading bar by setting it to i.
	 * @param i
	 */
	public void updateLoadBar(int i) {
		
		loadBar.setValue(i);
	}
	
	/**
	 * Resets the loading bar to 0.
	 * @param i
	 */
	public void resetLoadBar() {
		
		loadBar.setValue(0);
	}
	
	/**
	 * Sets the maximum of the loading bar.
	 * @param max
	 */
	public void setLoadBarMax(int max) {
		
		loadBar.setMaximum(max);
	}
	
	/**
	 * Changes the text of the loading label.
	 * @param text
	 */
	public void setLoadLabel(String text) {
		
		loadLabel.setText(text);
		
		revalidate();
		repaint();
	}
}
