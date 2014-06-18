package Views;

import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIParams;
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

		homePanel = new JPanel();
		homePanel.setLayout(new MigLayout("ins 0"));

		title_bg = new JPanel();
		title_bg.setLayout(new MigLayout());

		jl = new JLabel("Hello! How are you Gentlepeople?");
		jl.setFont(new Font("Sans Serif", Font.PLAIN, 30));

		jl2 = new JLabel("Welcome to " + StringRes.appName);
		jl2.setFont(new Font("Sans Serif", Font.BOLD, 50));

		title_bg.add(jl, "push, alignx 50%, span, wrap");
		title_bg.add(jl2, "push, alignx 50%, span");
		
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setOpaque(false);
		
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

		loadButton = GUIParams.setLargeButtonLayout("Open...");
		loadPanel.add(loadButton, "push, alignx 50%");

		// Set the colors
		homePanel.setBackground(GUIParams.BG_COLOR);
		title_bg.setBackground(GUIParams.MAIN);
		jl.setForeground(GUIParams.BG_COLOR);
		jl2.setForeground(GUIParams.BG_COLOR);
		
		homePanel.revalidate();
		homePanel.repaint();

		return homePanel;
	}

	public JPanel makeLoading() {

		isLoading = true;

		loadPanel.removeAll();

		jl.setText("One moment, please.");
		jl2.setText("Loading you data!");
		
		loadLabel = new JLabel();
		loadLabel.setFont(GUIParams.FONTL);
		loadLabel.setForeground(GUIParams.TEXT);
		
		loadBar = GUIParams.setPBarLayout();

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
