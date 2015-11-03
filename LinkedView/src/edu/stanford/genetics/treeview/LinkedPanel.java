///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
// *
// * END_HEADER 
// */
//package edu.stanford.genetics.treeview;
//
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.Rectangle;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.KeyEvent;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.awt.event.WindowListener;
//import java.util.Enumeration;
//import java.util.Vector;
//
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JFrame;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JTabbedPane;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//
//import edu.stanford.genetics.treeview.core.PluginManager;
//
//public class LinkedPanel extends JTabbedPane implements MainPanel {
//
//	private static final long serialVersionUID = 1L;
//
//	private ViewFrame viewFrame;
//	private ConfigNode configNode = null;
//
//	/**
//	 * used to hold list of open mp dialogs
//	 */
//	private final Vector<JFrame> mpdialogs = new Vector<JFrame>();
//
//	public LinkedPanel(final ViewFrame viewFrame) {
//
//		super();
//		setName("LinkedPanel");
//		setViewFrame(viewFrame);
//		addChangeListener(new ChangeListener() {
//
//			@Override
//			public void stateChanged(final ChangeEvent e) {
//
//				final ConfigNode viewsNode = getConfigNode();
//				if (viewsNode != null) {
//					viewsNode.setAttribute("selected", getSelectedIndex(), 0);
//				}
//			}
//		});
//	}
//
//	/** Setter for viewFrame */
//	public void setViewFrame(final ViewFrame viewFrame) {
//
//		this.viewFrame = viewFrame;
//	}
//
//	/** Getter for viewFrame */
//	public ViewFrame getViewFrame() {
//
//		return viewFrame;
//	}
//
//	/**
//	 * This syncronizes the sub compnents with their persistent storage.
//	 */
//	@Override
//	public void syncConfig() {
//
//		final int n = getComponentCount();
//		for (int i = 0; i < n; i++) {
//			final MainPanel modelView = (MainPanel) getComponentAt(i);
//			modelView.syncConfig();
//		}
//	}
//
//	/** Setter for configNode */
//	public void setConfigNode(final ConfigNode configNode) {
//
//		this.configNode = configNode;
//		restoreState();
//	}
//
//	public void restoreState() {
//
//		this.removeAll();
//		// alright, setup views...
//		final ConfigNode viewsNode = getConfigNode();
//		if (viewsNode != null) {
//			final ConfigNode[] viewNodes = viewsNode.fetch("View");
//			final PluginFactory[] plugins = PluginManager.getPluginManager()
//					.getPluginFactories();
//
//			for (int i = 0; i < viewNodes.length; i++) {
//				final ConfigNode thisNode = viewNodes[i];
//				String thisType = thisNode.getAttribute("type", null);
//
//				// check to see if covered by plugin...
//				for (int j = 0; j < plugins.length; j++) {
//					if (thisType.equals(plugins[j].getPluginName())) {
//						restorePlugin(thisNode, plugins[j]);
//						thisType = null; // forestall further adds
//						break;
//					}
//				}
//
//				if (thisType == null) {
//					// do nothing...
//
//				} else {
//					LogBuffer.println(viewFrame.getDataModel().getSource()
//							+ ": encountered unknown View of type " + thisType);
//				}
//			}
//		}
//
//		if (getComponentCount() == 0) {
//			PluginFactory foo = PluginManager.getPluginManager()
//					.getPluginFactory(0);
//			final PluginFactory[] plugins = PluginManager.getPluginManager()
//					.getPluginFactories();
//
//			for (int i = 0; i < plugins.length; i++) {
//				if ("Dendrogram".equals(plugins[i].getPluginName())) {
//					foo = plugins[i];
//				}
//			}
//
//			if (foo != null) {
//				addPlugin(foo);
//
//			} else {
//				JOptionPane.showMessageDialog(this, "No plugins loaded");
//			}
//		}
//
//		if (getComponentCount() > 0) {
//			final int selected = viewsNode.getAttribute("selected", 0);
//			setSelectedIndex(selected);
//		}
//	}
//
//	/**
//	 * this method gets the config node on which this component is based, or
//	 * null.
//	 */
//	@Override
//	public ConfigNode getConfigNode() {
//
//		return configNode;
//	}
//
//	/**
//	 * Add items related to settings
//	 * 
//	 * @param menu
//	 *            A menu to add items to.
//	 */
//	@Override
//	public void populateSettingsMenu(final TreeviewMenuBarI menu) {
//
//		final MainPanel panel = (MainPanel) getSelectedComponent();
//		if (panel != null) {
//			panel.populateSettingsMenu(menu);
//		}
//	}
//
//	/**
//	 * Add items which do some kind of analysis
//	 * 
//	 * @param menu
//	 *            A menu to add items to.
//	 */
//	@Override
//	public void populateAnalysisMenu(final TreeviewMenuBarI menu) {
//
//		final MainPanel panel = (MainPanel) getSelectedComponent();
//		if (panel != null) {
//			panel.populateAnalysisMenu(menu);
//		}
//
//		if (menu.getItemCount() > 0) {
//			menu.addSeparator();
//		}
//
//		final PluginFactory[] plugins = PluginManager.getPluginManager()
//				.getPluginFactories();
//		for (int i = 0; i < plugins.length; i++) {
//			final PluginFactory thisFactory = plugins[i];
//			menu.addMenuItem(thisFactory.getPluginName(), new ActionListener() {
//
//				@Override
//				public void actionPerformed(final ActionEvent e) {
//
//					// MainPanel plugin =
//					addPlugin(thisFactory);
//				}
//			});
//		}
//
//		if (plugins.length == 0) {
//			menu.addMenuItem("No Plugins Found", new ActionListener() {
//
//				@Override
//				public void actionPerformed(final ActionEvent e) {
//				}
//			});
//		}
//
//		if (menu.getItemCount() > 0) {
//			menu.addSeparator();
//		}
//		menu.addSeparator();
//
//		menu.addMenuItem("Remove Current", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//
//				removeCurrent();
//			}
//		});
//		menu.setMnemonic(KeyEvent.VK_R);
//
//		menu.addMenuItem("Detach Current", new ActionListener() {
//
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//
//				detachCurrent();
//			}
//		});
//		menu.setMnemonic(KeyEvent.VK_D);
//
//		/*
//		 * MenuItem menuItem2 = getModel().getStatMenuItem();
//		 * menu.add(menuItem2);
//		 */
//	}
//
//	/**
//	 * Add items which allow for export, if any.
//	 * 
//	 * @param menu
//	 *            A menu to add items to.
//	 */
//	@Override
//	public void populateExportMenu(final TreeviewMenuBarI menu) {
//
//		final MainPanel panel = (MainPanel) getSelectedComponent();
//		if (panel != null) {
//			if (menu.getItemCount() > 0) {
//				menu.addSeparator();
//			}
//			panel.populateExportMenu(menu);
//		}
//	}
//
//	/**
//	 * ensure a particular index is visible. Used by Find.
//	 * 
//	 * @param index
//	 *            Index of gene in cdt to make visible
//	 */
//	@Override
//	public void scrollToGene(final int index) {
//
//		final int n = getComponentCount();
//		for (int i = 0; i < n; i++) {
//			final MainPanel modelView = (MainPanel) getComponentAt(i);
//			modelView.scrollToGene(index);
//		}
//	}
//
//	@Override
//	public void scrollToArray(final int index) {
//
//		final int n = getComponentCount();
//		for (int i = 0; i < n; i++) {
//			final MainPanel modelView = (MainPanel) getComponentAt(i);
//			modelView.scrollToArray(index);
//		}
//	}
//
//	/**
//	 * used to add existing instances, with state stored in confignode
//	 * 
//	 * @param thisNode
//	 * @param f
//	 * @return
//	 */
//	public MainPanel restorePlugin(final ConfigNode thisNode,
//			final PluginFactory f) {
//
//		final MainPanel plugin = f.restorePlugin(thisNode, getViewFrame());
//		if (plugin != null) {
//			switch (plugin.getConfigNode().getAttribute("dock", -1)) {
//			case 0:
//				addDialog(plugin);
//				break;
//			case 1:
//				addTab(plugin);
//				setSelectedComponent((Component) plugin);
//				break;
//			case -1:
//				addTab(plugin);
//				setSelectedComponent((Component) plugin);
//				break;
//			}
//		}
//		return plugin;
//	}
//
//	/**
//	 * used to add new instances of the plugin
//	 */
//	public MainPanel addPlugin(final PluginFactory f) {
//
//		final ConfigNode thisNode = getConfigNode().create("View");
//		thisNode.setAttribute("type", f.getPluginName(), null);
//		f.configurePlugin(thisNode, getViewFrame());
//		return restorePlugin(thisNode, f);
//	}
//
//	public void addTab(final MainPanel mp) {
//
//		addTab(mp.getName(), mp.getIcon(), (Component) mp,
//				"What's this button do?");
//		mp.getConfigNode().setAttribute("dock", 1, -1);
//	}
//
//	public void addDialog(final MainPanel mp) {
//
//		final MainPanelFrame nmp = new MainPanelFrame(mp);
//		mpdialogs.add(nmp);
//		final Rectangle r = viewFrame.getBounds();
//		r.height -= 10;
//		r.width -= 10;
//		r.x += 10;
//		r.y += 10;
//		nmp.setBounds(r);
//		nmp.setVisible(true);
//		mp.getConfigNode().setAttribute("dock", 0, -1);
//	}
//
//	/**
//	 * removed ConfigNode of mainpanel as well as dialog window
//	 * 
//	 * @param mp
//	 *            mainpanel to remove
//	 */
//	public void removeDialog(final MainPanel mp) {
//
//		final Enumeration<JFrame> e = mpdialogs.elements();
//		while (e.hasMoreElements()) {
//			final MainPanelFrame mpd = (MainPanelFrame) e.nextElement();
//			if (mpd.getMainPanel() == mp) {
//				removeDialog(mpd);
//			}
//		}
//	}
//
//	/**
//	 * removed ConfigNode of mainpanel as well as dialog window
//	 * 
//	 * @param mp
//	 *            mainpanel to remove
//	 */
//	public void removeDialog(final MainPanelFrame mpd) {
//
//		mpdialogs.remove(mpd);
//		final ConfigNode viewsNode = getConfigNode();
//		viewsNode.remove(mpd.getMainPanel().getConfigNode());
//		mpd.dispose();
//	}
//
//	public void dockMainPanelDialog(final MainPanelFrame mpd) {
//
//		final MainPanel mp = mpd.getMainPanel();
//		mpdialogs.remove(mpd);
//		mpd.setVisible(false);
//		addTab(mp);
//		mpd.dispose();
//		mp.getConfigNode().setAttribute("dock", 1, -1);
//	}
//
//	public void detachCurrent() {
//
//		final Component current = getSelectedComponent();
//		if (current != null) {
//			final MainPanel mp = (MainPanel) current;
//			remove(current);
//			addDialog(mp);
//			mp.getConfigNode().setAttribute("dock", 0, -1);
//		}
//	}
//
//	public void removeCurrent() {
//
//		final Component current = getSelectedComponent();
//		if (current != null) {
//			final MainPanel cPanel = (MainPanel) current;
//			cPanel.syncConfig();
//			final ConfigNode viewsNode = getConfigNode();
//			viewsNode.remove(cPanel.getConfigNode());
//			remove(current);
//		}
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see edu.stanford.genetics.treeview.MainPanel#getIcon()
//	 */
//	@Override
//	public ImageIcon getIcon() {
//
//		// can't nest linked panels yet.
//		return null;
//	}
//
//	public MainPanel[] getMainPanelsByName(final String name) {
//
//		final Vector<MainPanel> matches = new Vector<MainPanel>();
//
//		// check the detached plugins
//		final Enumeration<JFrame> e = mpdialogs.elements();
//		while (e.hasMoreElements()) {
//			final MainPanelFrame mpd = (MainPanelFrame) e.nextElement();
//			final MainPanel mp = mpd.getMainPanel();
//			if (name.equals(mp.getName())) {
//				matches.add(mp);
//			}
//		}
//
//		final Component[] docked = this.getComponents();
//
//		// check the docked plugins
//		for (int i = 0; i < docked.length; i++) {
//			final MainPanel mp = (MainPanel) docked[i];
//			if (name.equals(mp.getName())) {
//				matches.add(mp);
//			}
//		}
//
//		final Object[] comps = matches.toArray();
//		final MainPanel[] ret = new MainPanel[comps.length];
//		for (int i = 0; i < comps.length; i++) {
//			ret[i] = (MainPanel) comps[i];
//		}
//		return ret;
//	}
//
//	public MainPanel[] getMainPanels() {
//
//		final Vector<MainPanel> matches = new Vector<MainPanel>();
//
//		// check the detached plugins
//		final Enumeration<JFrame> e = mpdialogs.elements();
//		while (e.hasMoreElements()) {
//			final MainPanelFrame mpd = (MainPanelFrame) e.nextElement();
//			final MainPanel mp = mpd.getMainPanel();
//			matches.add(mp);
//		}
//		final Component[] docked = this.getComponents();
//
//		// check the docked plugins
//		for (int i = 0; i < docked.length; i++) {
//			final MainPanel mp = (MainPanel) docked[i];
//			matches.add(mp);
//		}
//
//		final Object[] comps = matches.toArray();
//		final MainPanel[] ret = new MainPanel[comps.length];
//		for (int i = 0; i < comps.length; i++) {
//			ret[i] = (MainPanel) comps[i];
//		}
//		return ret;
//	}
//
//	@Override
//	public void export(final MainProgramArgs args) throws ExportException {
//
//		throw new ExportException("Export not implemented for plugin "
//				+ getName());
//	}
//
//	/**
//	 * Refreshing the current active linked panel
//	 */
//	@Override
//	public void refresh() {
//
//		restoreState();
//		this.revalidate();
//		this.repaint();
//	}
//
//	/**
//	 * This class enables you to put a MainPanel in a separate window. the
//	 * separate window is not a ViewFrame and should be thought of as
//	 * subordinate to the ViewFrame that holds the LinkedPanel, although there's
//	 * no way to enforce that from java without always having the subwindow on
//	 * top.
//	 */
//	private class MainPanelFrame extends JFrame {
//
//		private static final long serialVersionUID = 1L;
//
//		private final MainPanel mainPanel;
//
//		/**
//		 * @param mp
//		 *            main panel to display
//		 */
//		public MainPanelFrame(final MainPanel mp) {
//
//			super();
//			mainPanel = mp;
//			final WindowListener listener = new WindowAdapter() {
//
//				@Override
//				public void windowClosing(final WindowEvent e) {
//					//
//					dockMainPanelDialog(MainPanelFrame.this);
//					removeDialog(MainPanelFrame.this);
//				}
//
//				@Override
//				public void windowClosed(final WindowEvent e) {
//				}
//
//				@Override
//				public void windowIconified(final WindowEvent e) {
//				}
//
//				@Override
//				public void windowDeiconified(final WindowEvent e) {
//				}
//			};
//			addWindowListener(listener);
//
//			final JButton dockButton = new JButton("Dock");
//			dockButton.addActionListener(new ActionListener() {
//
//				@Override
//				public void actionPerformed(final ActionEvent e) {
//
//					removeWindowListener(listener);
//					dockMainPanelDialog(MainPanelFrame.this);
//				}
//			});
//
//			final JButton closeButton = new JButton("Close");
//			closeButton.addActionListener(new ActionListener() {
//
//				@Override
//				public void actionPerformed(final ActionEvent e) {
//
//					removeWindowListener(listener);
//					removeDialog(MainPanelFrame.this);
//				}
//			});
//
//			final JPanel buttonPanel = new JPanel();
//			buttonPanel.add(dockButton);
//			buttonPanel.add(closeButton);
//
//			getContentPane().setLayout(new BorderLayout());
//			getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//			getContentPane().add((Component) mainPanel, BorderLayout.CENTER);
//			setTitle(mp.getName() + ": " + viewFrame.getDataModel().getSource());
//		}
//
//		/**
//		 * @return main panel displayed by dialog
//		 */
//		public MainPanel getMainPanel() {
//
//			return mainPanel;
//		}
//	}
//}