package Views;

import java.awt.Font;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class WelcomeView {
	
	private TreeViewFrame tvFrame;
	private JPanel loadPanel;
	private JPanel welcomePanel;
	private JLabel label;

	public WelcomeView(TreeViewFrame tvFrame) {
		
		this.tvFrame = tvFrame;
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
	public JPanel makeWelcomePanel() {
		
		welcomePanel = new JPanel();
		welcomePanel.setLayout(new MigLayout("ins 0"));
		welcomePanel.setBackground(GUIParams.BG_COLOR);
		
		JPanel title_bg;
		JLabel jl;
		JLabel jl2;
		
		title_bg = new JPanel();
		title_bg.setLayout(new MigLayout());
		title_bg.setBackground(GUIParams.MAIN);

		jl = new JLabel("Hello! How are you Gentlepeople?");
		jl.setFont(new Font("Sans Serif", Font.PLAIN, 30));
		jl.setForeground(GUIParams.BG_COLOR);

		jl2 = new JLabel("Welcome to " + tvFrame.getAppName());
		jl2.setFont(new Font("Sans Serif", Font.BOLD, 50));
		jl2.setForeground(GUIParams.BG_COLOR);

		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setOpaque(false);
		
		label = new JLabel("Load Data >");
		label.setFont(new Font("Sans Serif", Font.PLAIN, 50));
		label.setForeground(GUIParams.MAIN);
		
		loadPanel.add(label, "pushx, alignx 50%");

		title_bg.add(jl, "push, alignx 50%, span, wrap");
		title_bg.add(jl2, "push, alignx 50%, span");

		welcomePanel.add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
		welcomePanel.add(loadPanel, "push, alignx 50%");
		
		return welcomePanel;
	}
}
