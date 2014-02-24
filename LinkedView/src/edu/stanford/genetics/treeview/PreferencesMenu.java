package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.FontSettingsPanel;
import edu.stanford.genetics.treeview.plugin.dendroview.PixelSettingsSelector;

import net.miginfocom.swing.MigLayout;

public class PreferencesMenu {
	
	private TreeViewFrame tvFrame;
	private JFrame applicationFrame;
	private JFrame menuFrame;
	
	private JPanel basisPanel;
	private JPanel leftPanel;
	private DendroView dendroView;
	private JButton ok_button;
	
	// Menus
	private PixelSettingsPanel pixelSettings = null;
	private AnnotationPanel annotationSettings = null;
	private FontPanel fontSettings = null;
	private ThemeSettingsPanel themeSettings = null;
	private URLSettings urlSettings = null;
	
	/**
	 * Chained constructor in case DendroView isn't available
	 * @param viewFrame
	 */
	public PreferencesMenu(TreeViewFrame tvFrame, String menuTitle) {
		
		this(tvFrame, null, menuTitle);
	}
	
	/**
	 * Main constructor for Preferences Menu
	 * @param tvFrame
	 * @param dendroView
	 * @param menuTitle
	 */
	public PreferencesMenu(TreeViewFrame tvFrame, DendroView dendroView, 
			String menuTitle) {
		
		this.tvFrame = tvFrame;
		this.applicationFrame = tvFrame.getAppFrame();
		this.dendroView = dendroView;
		
		menuFrame = new JFrame("Preferences");
		
		basisPanel = new JPanel();
		basisPanel.setLayout(new MigLayout());
		
		ok_button = GUIParams.setButtonLayout("OK", null);
		
		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIParams.getScreenSize();
		
		int width = mainDim.width * 1/2;
		
		if(width > 640) {
			width = 640;
		}
		
		int height = mainDim.height * 3/4;
		
		basisPanel.setPreferredSize(
				new Dimension(width, height));
		
		menuFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		setupLayout(menuTitle);
		
		menuFrame.getContentPane().add(basisPanel);
		
		menuFrame.pack();
		menuFrame.setLocationRelativeTo(applicationFrame);
	}
	
	/**
	 * Returns the menu frame holding all the JPanels to display to the user.
	 * @return
	 */
	public JFrame getPreferencesFrame() {
		
		return menuFrame;
	}
	
	public void synchronizeAnnotation() {
		
		annotationSettings.synchronize();
		setupLayout("Row and Column Labels");
	}
	
	// Listeners
	/**
	 * Adds an ActionListener to the ok_button.
	 * @param listener
	 */
	public void addOKButtonListener(ActionListener listener) {
		
		ok_button.addActionListener(listener);
	}
	
	/**
	 * Equips the preferences JFrame with a window listener.
	 * @param listener
	 */
	public void addWindowListener(WindowAdapter listener) {
		
		menuFrame.addWindowListener(listener);
	}
	
	/**
	 * Adds an ActionListener to the theme_button in ThemeSettings
	 * @param listener
	 */
	public void addThemeListener(ActionListener listener) {
		
		themeSettings.getThemeButton().addActionListener(listener);
	}
	
	public void addCustomLabelListener(ActionListener listener) {
		
		System.out.println("Custom label Listener added.");
		
		if(annotationSettings != null) {
			annotationSettings.getCustomLabelButton()
			.addActionListener(listener);
		}
	}
	
	/**
	 * Sets up the layout for the menu.
	 */
	public void setupLayout(String startMenu) {
		
		setupMenus();
		
		leftPanel = new JPanel();
		leftPanel.setLayout(new MigLayout());
		leftPanel.setBackground(GUIParams.BG_COLOR);
		leftPanel.setBorder(BorderFactory.createEtchedBorder());
		
		setupMenuHeaders();
		
		basisPanel.add(leftPanel, "pushy, aligny 0%, w 20%, " +
				"h 75%");
		
		addMenu(startMenu);
		
		basisPanel.add(ok_button, "pushx, alignx 100%, span");
		
		menuFrame.validate();
		menuFrame.repaint();
	}
	
	public void setupMenuHeaders() {
		
		JPanel theme = new MenuPanel("Theme", this).makeMenuPanel();
		leftPanel.add(theme, "pushx, w 90%, h 10%, alignx 50%, span, wrap");
		
		JPanel annotations = new MenuPanel("Row and Column Labels", 
				this).makeMenuPanel();
		leftPanel.add(annotations, "pushx, w 90%, h 10%, alignx 50%, " +
				"span, wrap");
			
		JPanel font = new MenuPanel("Font", this).makeMenuPanel();
		leftPanel.add(font, "pushx, w 90%, h 10%, alignx 50%, span, wrap");
			
		JPanel url = new MenuPanel("URL", this).makeMenuPanel();
		leftPanel.add(url, "pushx, w 90%, h 10%, alignx 50%, span, wrap");
		
		JPanel heatMap = new MenuPanel("Color Settings", this).makeMenuPanel();
		leftPanel.add(heatMap, "pushx, w 90%, h 10%, alignx 50%, span");
	}
	
	/**
	 * Setting up the menus depending on whether DendroView 
	 * has been instantiated.
	 */
	public void setupMenus() {
		
		if(dendroView != null) {
			
			pixelSettings = new PixelSettingsPanel();
			annotationSettings = new AnnotationPanel();
			fontSettings = new FontPanel();
		}
		
		themeSettings = new ThemeSettingsPanel();
	}
	
	/**
	 * Create the panel for pixel settings.
	 */
	class PixelSettingsPanel {
		
		private ColorExtractor ce = null;
		private JScrollPane scrollPane;

		public PixelSettingsPanel() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			try {
				ce = ((DoubleArrayDrawer) dendroView.getArrayDrawer())
						.getColorExtractor();
	
			} catch (final Exception e) {
	
			}
	
			PixelSettingsSelector pss = new PixelSettingsSelector(
					dendroView.getGlobalXmap(), dendroView.getGlobalYmap(), ce, 
					DendrogramFactory.getColorPresets()); 
			
			panel.add(pss, "push, grow");
			
			scrollPane.setViewportView(panel);
		}
		
		public JScrollPane makePSPanel() {
			
			return scrollPane;
		}
	}
	
	/**
	 * Create the panel for font settings.
	 */
	class FontPanel {

		private JScrollPane scrollPane;
		
		public FontPanel() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			final FontSettingsPanel fontChangePanel = new FontSettingsPanel(
					dendroView.getTextview(), dendroView.getArraynameview());
			
			JLabel labelFont = GUIParams.setupHeader("Set Label Font:");
			
			panel.add(labelFont, "span, wrap");
			panel.add(fontChangePanel, "pushx, alignx 50%, w 95%");
			
			scrollPane.setViewportView(panel);
		}
		
		public JScrollPane makeFontPanel() {
			
			return scrollPane;
		}
	}
	
	/**
	 * Create the panel for font settings.
	 */
	class URLSettings {

		private JScrollPane scrollPane;

		public URLSettings() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			final UrlSettingsPanel genePanel = new UrlSettingsPanel(
					tvFrame.getUrlExtractor(), 
					tvFrame.getGeneUrlPresets());

			final UrlSettingsPanel arrayPanel = new UrlSettingsPanel(
					tvFrame.getArrayUrlExtractor(), 
					tvFrame.getArrayUrlPresets());
			
			panel.add(genePanel, "pushx, alignx 50%, w 95%, wrap");
			panel.add(arrayPanel, "pushx, alignx 50%, w 95%");
			
			scrollPane.setViewportView(panel);
		}
		
		public JScrollPane makeURLPanel() {
			
			return scrollPane;
		}
	}
	
	/**
	 * Subclass to create a panel that handles theme settings.
	 * @author CKeil
	 *
	 */
	class ThemeSettingsPanel {

		private JButton theme_button;
		private JScrollPane scrollPane;
		
		public ThemeSettingsPanel() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			panel.add(GUIParams.setupHeader("Click to switch theme:"), 
					"span, wrap");
			
			// Choice #1
			theme_button = GUIParams.setButtonLayout("Switch Theme", null);
			
			panel.add(theme_button);
			
			scrollPane.setViewportView(panel);
		}
		
		public JScrollPane makeThemePanel() {
			
			return scrollPane;
		}
		
		public JButton getThemeButton() {
			
			return theme_button;
		}
	}
	
	/**
	 * Subclass for the Annotation settings panel.
	 * @author CKeil
	 *
	 */
	class AnnotationPanel {
		
		private JScrollPane scrollPane;
		private final JButton custom_button;
		private final HeaderSummaryPanel genePanel;
		private final HeaderSummaryPanel arrayPanel;
		
		public AnnotationPanel() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			genePanel = new HeaderSummaryPanel(
					tvFrame.getDataModel().getGeneHeaderInfo(), 
					dendroView.getTextview().getHeaderSummary(), tvFrame);

			arrayPanel = new HeaderSummaryPanel(
					dendroView.getArraynameview().getHeaderInfo(), 
					dendroView.getArraynameview().getHeaderSummary(), tvFrame);

//			final HeaderSummaryPanel atrPanel = new HeaderSummaryPanel(
//					tvFrame.getDataModel().getAtrHeaderInfo(), 
//					dendroView.getAtrview().getHeaderSummary(), tvFrame);
//
//			final HeaderSummaryPanel gtrPanel = new HeaderSummaryPanel(
//					tvFrame.getDataModel().getGtrHeaderInfo(), 
//					dendroView.getGtrview().getHeaderSummary(), tvFrame);
			
			custom_button = GUIParams.setButtonLayout(
					"Use Custom Labels", null);
			
			JLabel rows = GUIParams.setupHeader("Rows");
			JLabel cols = GUIParams.setupHeader("Columns");
//			JLabel rTrees = GUIParams.setupHeader("Row Trees");
//			JLabel cTrees = GUIParams.setupHeader("Column Trees");
			
			panel.add(rows, "pushx, alignx 50%");
			panel.add(cols, "pushx, alignx 50%, wrap");
			panel.add(genePanel, "pushx, alignx 50%, w 45%");
			panel.add(arrayPanel, "pushx, alignx 50%, w 45%, wrap");
			
			panel.add(custom_button, "pushx, alignx 50%, span");
			
//			panel.add(rTrees, "span, wrap");
//			panel.add(gtrPanel, "pushx, alignx 50%, w 95%, span, wrap");
//			
//			panel.add(cTrees, "span, wrap");
//			panel.add(atrPanel, "pushx, alignx 50%, w 95%, span");
			
			scrollPane.setViewportView(panel);
		}
		
		public JScrollPane makeLabelPane() {
			
			return scrollPane;
		}
		
		public JButton getCustomLabelButton() {
			
			return custom_button;
		}
		
		public void synchronize() {
			
			genePanel.synchronizeTo();
			arrayPanel.synchronizeTo();
		}
	}
	
	/**
	 * Dynamically adds JScrollPane to the frame based on the MouseListener
	 * in MenuPanel.
	 * @param title
	 */
	public void addMenu(String title) {
		
		basisPanel.removeAll();
		
		basisPanel.add(leftPanel, "pushy, aligny 0%, w 20%, h 75%");
		
		if(title.equalsIgnoreCase("Theme") && themeSettings != null) {
			basisPanel.add(themeSettings.makeThemePanel(), 
					"w 79%, h 95%, wrap");
			
		} else if(title.equalsIgnoreCase("Row and Column Labels") 
				&& annotationSettings != null) {
			basisPanel.add(annotationSettings.makeLabelPane(), 
					"w 79%, h 95%, wrap");
			
		} else if(title.equalsIgnoreCase("Font") && fontSettings != null) {
			basisPanel.add(fontSettings.makeFontPanel(), "w 79%, h 95%, wrap");
		
		} else if(title.equalsIgnoreCase("Color Settings") 
				&& pixelSettings != null) {
			basisPanel.add(pixelSettings.makePSPanel(), "w 79%, h 95%, wrap");
		
		} else if(title.equalsIgnoreCase("URL") && urlSettings != null) {
//			basisPanel.add(pixelSettings.makePSPanel(), "w 79%, h 95%, wrap");
		
		} else {
			//In case menu cannot be loaded, display excuse.
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			JLabel hint = new JLabel("Menu cannot be shown because it " +
					"wasn't loaded.");
			hint.setFont(GUIParams.FONTS);
			hint.setForeground(GUIParams.TEXT);
			panel.add(hint, "push, alignx 50%");
			
			basisPanel.add(panel, "w 79%, h 95%, wrap");
		}
		
		basisPanel.add(ok_button, "pushx, alignx 100%, span");
		
		basisPanel.revalidate();
		basisPanel.repaint();
	}
}
