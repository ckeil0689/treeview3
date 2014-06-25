package Views;

import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import edu.stanford.genetics.treeview.GUIFactory;
import edu.stanford.genetics.treeview.StringRes;

public class WelcomeView {

	// Initial
	private final JPanel loadPanel;
	private final JPanel homePanel;
	private final JPanel title_bg;

	private final JLabel jl;
	private final JLabel jl2;
	private JButton loadButton;

	private boolean isLoading = false;

	// Loading stuff
	private JProgressBar loadBar;
	private JLabel loadLabel;

	public WelcomeView() {

		homePanel = GUIFactory.createJPanel(false, false, null);

		title_bg = GUIFactory.createJPanel(true, true, null);

		jl = GUIFactory.createXXLLabel(StringRes.title_Hello);

		jl2 = GUIFactory.createXXLLabel(StringRes.title_Welcome 
				+ StringRes.appName);

		title_bg.add(jl, "push, alignx 50%, span, wrap");
		title_bg.add(jl2, "push, alignx 50%, span");
		
		loadPanel = GUIFactory.createJPanel(false, true, null);
		
		homePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
		homePanel.add(loadPanel, "push, grow, alignx 50%");
	}

	/**
	 * Access TreeViewFrame's load_Icon panel.
	 * 
	 * @return
	 */
	public JPanel getLoadIcon() {

		return loadPanel;
	}

	/**
	 * Access TreeViewFrame's load_Icon panel.
	 * 
	 * @return
	 */
	public JButton getLoadButton() {

		return loadButton;
	}

	public boolean isLoading() {

		return isLoading;
	}

	/**
	 * Equipping the loadButton with an ActionListener
	 * 
	 * @param loadData
	 */
	public void addLoadListener(final ActionListener loadData) {

		loadButton.addActionListener(loadData);
	}


	public JPanel makeInitial() {

		isLoading = false;

		loadPanel.removeAll();

		loadButton = GUIFactory.setLargeButtonLayout("Open...");
		loadPanel.add(loadButton, "push, alignx 50%");

		// Set the colors
//		homePanel.setBackground(GUIFactory.BG_COLOR);
//		title_bg.setBackground(GUIFactory.MAIN);
//		jl.setForeground(GUIFactory.BG_COLOR);
//		jl2.setForeground(GUIFactory.BG_COLOR);
		
		homePanel.revalidate();
		homePanel.repaint();

		return homePanel;
	}

	public JPanel makeLoading() {

		isLoading = true;

		loadPanel.removeAll();

		jl.setText(StringRes.loading_OneSec);
		jl2.setText(StringRes.loading_active);
		
		loadLabel = GUIFactory.createBigLabel("");
		
		loadBar = GUIFactory.setPBarLayout();

		loadPanel.add(loadLabel, "push, alignx 50%, aligny 100%, wrap");
		loadPanel.add(loadBar, "push, w 70%, alignx 50%, aligny 0%");

		homePanel.revalidate();
		homePanel.repaint();

		return homePanel;
	}

	// LoadBar functions
	/**
	 * Updates the loading bar by setting it to i.
	 * 
	 * @param i
	 */
	public void updateLoadBar(final int i) {

		if (i <= loadBar.getMaximum()) {
			loadBar.setValue(i);
		}
	}

	/**
	 * Resets the loading bar to 0.
	 * 
	 * @param i
	 */
	public void resetLoadBar() {

		loadBar.setValue(0);
	}

	/**
	 * Sets the maximum of the loading bar.
	 * 
	 * @param max
	 */
	public void setLoadBarMax(final int max) {

		loadBar.setMaximum(max);
	}

	/**
	 * Changes the text of the loading label.
	 * 
	 * @param text
	 */
	public void setLoadLabel(final String text) {

		loadLabel.setText(text);
	}
}
