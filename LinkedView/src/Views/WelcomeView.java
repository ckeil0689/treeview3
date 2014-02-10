package Views;

import java.awt.Font;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.ClickablePanel;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class WelcomeView extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private ClickablePanel load_Icon;

	public WelcomeView(TreeViewFrame tvFrame) {
		
		setLayout(new MigLayout("ins 0"));
		setOpaque(false);
		
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

		load_Icon = new ClickablePanel ("Load Data >");

		title_bg.add(jl, "push, alignx 50%, span, wrap");
		title_bg.add(jl2, "push, alignx 50%, span");

		add(title_bg, "pushx, growx, alignx 50%, span, "
				+ "height 20%::, wrap");
		add(load_Icon, "push, alignx 50%");
	}
	
	/**
	 * Access TreeViewFrame's load_Icon panel.
	 * @return
	 */
	public ClickablePanel getLoadIcon() {
		
		return load_Icon;
	}
	
	/**
	 * Equipping the load_Icon with a MouseListener
	 * @param loadData
	 */
	public void addLoadListener(MouseListener loadData) {
		
		load_Icon.addMouseListener(loadData);
	}
}
