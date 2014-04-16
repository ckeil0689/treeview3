package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import Controllers.DendroController;
import GradientColorChoice.ColorGradientChooser;
import GradientColorChoice.ColorGradientController;

import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor2;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.FontSettingsPanel;

import net.miginfocom.swing.MigLayout;

public class PreferencesMenu {
	
	private TreeViewFrame tvFrame;
	private Preferences configNode;
	private JFrame applicationFrame;
	private JDialog menuDialog;
	
	private JPanel basisPanel;
	private JPanel leftPanel;
	private DendroView2 dendroView;
	private DendroController dendroController;
	private JButton ok_button;
	private String activeMenu;
	
	// Menus
	private PixelSettingsPanel pixelSettings = null;
	private AnnotationPanel annotationSettings = null;
	private FontPanel fontSettings = null;
	private ThemeSettingsPanel themeSettings = null;
	private URLSettings urlSettings = null;
	
	private ColorGradientChooser gradientPick = null;
	
	private ArrayList<MenuPanel> menuPanelList;
	
	/**
	 * Chained constructor in case DendroView isn't available
	 * @param viewFrame
	 */
	public PreferencesMenu(TreeViewFrame tvFrame, String menuTitle, 
			String dialogTitle) {
		
		this(tvFrame, null, null, menuTitle, dialogTitle);
	}
	
	/**
	 * Main constructor for Preferences Menu
	 * @param tvFrame
	 * @param dendroView
	 * @param menuTitle
	 */
	public PreferencesMenu(TreeViewFrame tvFrame, DendroView2 dendroView, 
			DendroController controller, String menuTitle, String dialogTitle) {
		
		this.tvFrame = tvFrame;
		this.applicationFrame = tvFrame.getAppFrame();
		this.dendroView = dendroView;
		this.dendroController = controller;
		this.configNode = tvFrame.getConfigNode().node(
				StringRes.pref_node_Preferences);
		this.activeMenu = menuTitle;
		
		menuPanelList = new ArrayList<MenuPanel>();
		
		menuDialog = new JDialog();
		menuDialog.setTitle(dialogTitle);
		menuDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		menuDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		menuDialog.setResizable(true);
		
		
		basisPanel = new JPanel();
		basisPanel.setLayout(new MigLayout());
		
		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIParams.getScreenSize();
		
		int width = mainDim.width * 1/2;
		int height = mainDim.height * 1/2;
		
		basisPanel.setPreferredSize(new Dimension(width, height));
		
		menuDialog.getContentPane().add(basisPanel);
		
		setupLayout(menuTitle);
		
		menuDialog.pack();
		menuDialog.setLocationRelativeTo(applicationFrame);
	}
	
	/**
	 * Sets the visibility of clusterFrame.
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		
		menuDialog.setVisible(visible);
	}
	
	/**
	 * Returns the menu frame holding all the JPanels to display to the user.
	 * @return
	 */
	public JDialog getPreferencesFrame() {
		
		return menuDialog;
	}
	
	public void synchronizeAnnotation() {
		
		annotationSettings.synchronize();
		setupLayout(StringRes.menu_title_RowAndCol);
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
		
		menuDialog.addWindowListener(listener);
	}
	
	/**
	 * Adds an ActionListener to the darkThemeButton in ThemeSettings
	 * @param listener
	 */
	public void addThemeListener(ActionListener listener) {
		
		themeSettings.getDarkThemeButton().addActionListener(listener);
		themeSettings.getLightThemeButton().addActionListener(listener);
	}
	
	public void addCustomLabelListener(ActionListener listener) {
		
		if(annotationSettings != null) {
			annotationSettings.getCustomLabelButton()
			.addActionListener(listener);
		}
	}
	
	public void addMenuListeners(MouseListener l) {
		
		for(MenuPanel panel : menuPanelList) {
			
			panel.getMenuPanel().addMouseListener(l);
		}
	}
	
	/**
	 * Adds a component listener to the JDialog in which the content of
	 * this class is held. This ensures repainting of all child components
	 * when the JDialog is being resized.
	 * @param l
	 */
	public void addComponentListener(ComponentListener l) {
		
		menuDialog.addComponentListener(l);
	}
	
	/**
	 * Sets up the layout for the menu.
	 */
	public void setupLayout(String startMenu) {
		
		menuPanelList.clear();
		setupPanels();
		
		leftPanel = new JPanel();
		leftPanel.setLayout(new MigLayout());
		leftPanel.setBackground(GUIParams.BG_COLOR);
		leftPanel.setBorder(BorderFactory.createEtchedBorder());
		
		if(startMenu.equalsIgnoreCase(StringRes.menu_title_Theme)
				|| startMenu.equalsIgnoreCase(StringRes.menu_title_Font)
				|| startMenu.equalsIgnoreCase(StringRes.menu_title_URL)) {
			setupMenuHeaders(false);
			
		} else {
			setupMenuHeaders(true);
		}
		
		basisPanel.add(leftPanel, "pushy, aligny 0%, w 20%, " +
				"h 75%");
		
		addMenu(startMenu);
		
		ok_button = GUIParams.setButtonLayout(StringRes.button_OK, null);
		basisPanel.add(ok_button, "pushx, alignx 100%, span");
		
		menuDialog.validate();
		menuDialog.repaint();
	}
	
	public void setupMenuHeaders(boolean analysis) {
		
		if(!analysis) {
			MenuPanel theme = new MenuPanel(StringRes.menu_title_Theme);
			JPanel themePanel = theme.getMenuPanel();
			leftPanel.add(themePanel, "pushx, w 90%, h 10%, alignx 50%, " +
					"span, wrap");
			menuPanelList.add(theme);
				
			MenuPanel font = new MenuPanel(StringRes.menu_title_Font);
			JPanel fontPanel = font.getMenuPanel();
			leftPanel.add(fontPanel, "pushx, w 90%, h 10%, alignx 50%, " +
					"span, wrap");
			menuPanelList.add(font);
				
			MenuPanel url = new MenuPanel(StringRes.menu_title_URL);
			JPanel urlPanel = url.getMenuPanel();
			leftPanel.add(urlPanel, "pushx, w 90%, h 10%, alignx 50%, span");
			menuPanelList.add(url);
			
		} else {
			MenuPanel annotations = new MenuPanel(
					StringRes.menu_title_RowAndCol);
			JPanel annotationsPanel = annotations.getMenuPanel();
			leftPanel.add(annotationsPanel, "pushx, w 90%, h 10%, " +
					"alignx 50%, span, wrap");
			menuPanelList.add(annotations);
			
			MenuPanel heatMap = new MenuPanel(StringRes.menu_title_Color);
			JPanel heatMapPanel = heatMap.getMenuPanel();
			leftPanel.add(heatMapPanel, "pushx, w 90%, h 10%, alignx 50%, " +
					"span");
			menuPanelList.add(heatMap);
		}
	}
	
	/**
	 * Setting up the menus depending on whether DendroView 
	 * has been instantiated.
	 */
	public void setupPanels() {
		
		if(dendroView != null) {
			pixelSettings = new PixelSettingsPanel();
			annotationSettings = new AnnotationPanel();
			fontSettings = new FontPanel();
			
			gradientPick = new ColorGradientChooser(((DoubleArrayDrawer) 
					dendroController.getArrayDrawer()).getColorExtractor(), 
					DendrogramFactory.getColorPresets(), 
					tvFrame.getDataModel().getDataMatrix().getMinVal(),
					tvFrame.getDataModel().getDataMatrix().getMaxVal(),
					applicationFrame);
			
			// Adding GradientColorChooser configurations to DendroView node.
			gradientPick.setConfigNode(((TVModel)tvFrame.getDataModel()).getDocumentConfig());
//			tvFrame.getConfigNode()
//					.node(StringRes.pref_node_File));
			
			ColorGradientController gradientControl = 
					new ColorGradientController(gradientPick);
		}
		
		themeSettings = new ThemeSettingsPanel();
	}
	
	/**
	 * Create the panel for pixel settings.
	 */
	class PixelSettingsPanel {
		
		private ColorExtractor2 ce = null;
		private JScrollPane scrollPane;

		public PixelSettingsPanel() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
//			try {
//				ce = ((DoubleArrayDrawer) dendroController.getArrayDrawer())
//						.getColorExtractor();
//	
//			} catch (final Exception e) {
//	
//			}
//	
//			PixelSettingsSelector pss = new PixelSettingsSelector(
//					dendroController.getGlobalXMap(), 
//					dendroController.getGlobalYMap(), ce, 
//					DendrogramFactory.getColorPresets()); 
//			
//			panel.add(pss, "push, grow");
//			
//			scrollPane.setViewportView(panel);
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

		private JRadioButton darkThemeButton;
		private JRadioButton lightThemeButton;
		
		private ButtonGroup themeButtonGroup;
		
		private JScrollPane scrollPane;
		
		public ThemeSettingsPanel() {
			
			scrollPane = new JScrollPane();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.MENU);
			
			JLabel label = new JLabel("Choose a Theme:");
			label.setForeground(GUIParams.RADIOTEXT);
			label.setFont(GUIParams.FONTL);
			
			panel.add(label, "span, wrap");
			
			darkThemeButton = GUIParams.setRadioButtonLayout(
					StringRes.rButton_dark);
			lightThemeButton = GUIParams.setRadioButtonLayout(
					StringRes.rButton_light);
			
			themeButtonGroup = new ButtonGroup();
			themeButtonGroup.add(darkThemeButton);
			themeButtonGroup.add(lightThemeButton);
			
			// Check for saved presets...
			String default_theme = StringRes.rButton_dark;
			String savedTheme = tvFrame.getConfigNode().get("theme", 
					default_theme);
			
			// Since changing the theme resets the layout
			if(savedTheme.equalsIgnoreCase(StringRes.rButton_dark)) {
				darkThemeButton.setSelected(true);
				
			} else {
				lightThemeButton.setSelected(true);
			}
			
			panel.add(lightThemeButton, "span, wrap");
			panel.add(darkThemeButton, "span");
			
			scrollPane.setViewportView(panel);
		}
		
		public JScrollPane makeThemePanel() {
			
			return scrollPane;
		}
		
		public JRadioButton getDarkThemeButton() {
			
			return darkThemeButton;
		}
		
		public JRadioButton getLightThemeButton() {
			
			return lightThemeButton;
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
					StringRes.button_customLabels, null);
			
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
		
		activeMenu = title;
		
		if(title.equalsIgnoreCase(StringRes.menu_title_Theme) 
				&& themeSettings != null) {
			basisPanel.add(themeSettings.makeThemePanel(), 
					"w 79%, h 95%, wrap");
			
		} else if(title.equalsIgnoreCase(StringRes.menu_title_RowAndCol) 
				&& annotationSettings != null) {
			basisPanel.add(annotationSettings.makeLabelPane(), 
					"w 79%, h 95%, wrap");
			
		} else if(title.equalsIgnoreCase(StringRes.menu_title_Font) 
				&& fontSettings != null) {
			basisPanel.add(fontSettings.makeFontPanel(), "w 79%, h 95%, wrap");
		
		} else if(title.equalsIgnoreCase(StringRes.menu_title_Color) 
				&& gradientPick != null) {
//			basisPanel.add(pixelSettings.makePSPanel(), "w 79%, h 95%, wrap");
			basisPanel.add(gradientPick.makeGradientPanel(), 
					"w 79%, h 95%, wrap");
		
		} else if(title.equalsIgnoreCase(StringRes.menu_title_URL) 
				&& urlSettings != null) {
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
		
		for(MenuPanel panel : menuPanelList) {
			
			if(panel.getLabelText().equals(title)) {
				
				panel.setSelected(true);
			}
		}
		
		basisPanel.revalidate();
		basisPanel.repaint();
	}
	
	/**
	 * Returns the darkThemeButton from ThemeSettings for the controller.
	 * @return
	 */
	public JRadioButton getLightButton() {
		
		return themeSettings.getLightThemeButton();
	}
	
	/**
	 * Returns the darkThemeButton from ThemeSettings for the controller.
	 * @return
	 */
	public JRadioButton getDarkButton() {
		
		return themeSettings.getDarkThemeButton();
	}
	
	/**
	 * Gets the list which contains the JPanels used to select the menu
	 * type in the PreferencesMenu window.
	 * @return
	 */
	public ArrayList<MenuPanel> getMenuPanelList() {
		
		return menuPanelList;
	}
	
	/**
	 * Returns PreferencesMenu's configNode.
	 * @return
	 */
	public Preferences getConfigNode() {
		
		return configNode;
	}
	
	public ColorGradientChooser getGradientPick() {
		
		return gradientPick;
	}
	
	/**
	 * Returns the name of the last chosen menu.
	 * @return String
	 */
	public String getActiveMenu() {
		
		return activeMenu;
	}
}
