package Views;

import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import Utilities.GUIFactory;
import Utilities.StringRes;

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

		title_bg = GUIFactory.createJPanel(false, true, null);
		
		JPanel titleContainer = GUIFactory.createJPanel(false, false, null);
		
		loadPanel = GUIFactory.createJPanel(false, true, null);

		jl = GUIFactory.createLabel(StringRes.title_Hello, GUIFactory.FONTL);

		jl2 = GUIFactory.createLabel(StringRes.title_Welcome 
				+ StringRes.appName + StringRes.dot, GUIFactory.FONTXXL);

		titleContainer.add(jl, "pushx, alignx 50%, aligny 50%, span, wrap");
		titleContainer.add(jl2, "pushx, alignx 50%, span");
		titleContainer.add(new JSeparator(JSeparator.HORIZONTAL), "w 30%, "
				+ "pushx, alignx 50%");
		
		title_bg.add(titleContainer, "push, growx, align 50%");
		
		homePanel.add(title_bg, "push, grow, alignx 50%, "
				+ "span, wrap");
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

		loadButton = GUIFactory.createLargeBtn("Open...");
		loadPanel.add(loadButton, "push, alignx 50%, aligny 0%");

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

		JPanel loadContainer = GUIFactory.createJPanel(false, false, null);
		
		jl.setText(StringRes.load_OneSec);
		jl2.setText(StringRes.load_active);
		
		loadLabel = GUIFactory.createLabel("", GUIFactory.FONTL);
		
		loadBar = GUIFactory.createPBar();

		loadContainer.add(loadLabel, "pushx, alignx 50%, wrap");
		loadContainer.add(loadBar, "pushx, growx");
		
		loadPanel.add(loadContainer, "pushx, growx, align 50%, w 60%!");

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
