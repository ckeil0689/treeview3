package Views;

import java.awt.Font;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.StringRes;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class WelcomeView {
	
	// Initial
	private TreeViewFrame tvFrame;
	private JPanel loadPanel;
	private JPanel loadProgPanel;
	private JPanel homePanel;
	private JLabel label;
	
	private JLabel jl;
	private JLabel jl2;
	
	private boolean isLoading = false;
	
	// Loading stuff
	private JProgressBar loadBar;
	private JLabel loadLabel;

	public WelcomeView(TreeViewFrame tvFrame) {
		
		this.tvFrame = tvFrame;
		
		setupMainPanel();
	}
	
	/**
	 * Access TreeViewFrame's load_Icon panel.
	 * @return
	 */
	public JPanel getLoadIcon() {
		
		return loadPanel;
	}
	
	/**
	 * Access TreeViewFrame's load_Icon panel.
	 * @return
	 */
	public JLabel getLoadLabel() {
		
		return label;
	}
	
	public boolean isLoading() {
		
		return isLoading;
	}
	
	/**
	 * Equipping the load_Icon with a MouseListener
	 * @param loadData
	 */
	public void addLoadListener(MouseListener loadData) {
		
		loadPanel.addMouseListener(loadData);
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

		jl = new JLabel("Hello! How are you Gentlepeople?");
		jl.setFont(new Font("Sans Serif", Font.PLAIN, 30));
		jl.setForeground(GUIParams.BG_COLOR);

		jl2 = new JLabel("Welcome to " + StringRes.appName);
		jl2.setFont(new Font("Sans Serif", Font.BOLD, 50));
		jl2.setForeground(GUIParams.BG_COLOR);

		title_bg.add(jl, "push, alignx 50%, span, wrap");
		title_bg.add(jl2, "push, alignx 50%, span");

		homePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
	}
	
	public JPanel makeInitial() {
		
		isLoading = false;
		
		if(loadProgPanel != null && homePanel.isAncestorOf(loadProgPanel)) {
			homePanel.remove(loadProgPanel);
		}
		
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setOpaque(false);
		
		label = new JLabel("Load Data >");
		label.setFont(new Font("Sans Serif", Font.PLAIN, 50));
		label.setForeground(GUIParams.MAIN);
		
		loadPanel.add(label, "pushx, alignx 50%");
		
		homePanel.add(loadPanel, "push, alignx 50%");
		
		homePanel.revalidate();
		homePanel.repaint();
		
		return homePanel;
	}
	
	public JPanel makeLoading() {
		
		isLoading = true;
		
		if(loadPanel != null && homePanel.isAncestorOf(loadPanel)) {
			homePanel.remove(loadPanel);
		}
		
		jl.setText("One moment, please.");
		jl2.setText("Loading you data!");
		
		homePanel.add(getLoadPanel(), "push, grow");
		
		homePanel.revalidate();
		homePanel.repaint();
		
		return homePanel;
	}
	
	public JPanel getLoadPanel() {
		
		loadProgPanel = new JPanel();
		loadProgPanel.setLayout(new MigLayout("ins 0"));
		loadProgPanel.setOpaque(false);
		
		loadLabel = new JLabel();
		loadLabel.setFont(GUIParams.FONTL);
		loadLabel.setForeground(GUIParams.TEXT);
		
		loadBar = GUIParams.setPBarLayout();
		
		loadProgPanel.add(loadLabel, "push, alignx 50%, aligny 100%, wrap");
		loadProgPanel.add(loadBar, "push, w 70%, alignx 50%, aligny 0%");
		
		return loadProgPanel;
	}
	
	// LoadBar functions
	/**
	 * Updates the loading bar by setting it to i.
	 * @param i
	 */
	public void updateLoadBar(int i) {
		
		if(i <= loadBar.getMaximum()) {
			loadBar.setValue(i);
		}
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
	}
}
