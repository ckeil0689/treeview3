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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;
import Controllers.DendroController;
import GradientColorChoice.ColorGradientChooser;
import GradientColorChoice.ColorGradientController;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.ColorExtractor2;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;
import edu.stanford.genetics.treeview.plugin.dendroview.DendrogramFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.FontSettings;

public class PreferencesMenu {

	private final TreeViewFrame tvFrame;
	private final Preferences configNode;
	private final JFrame applicationFrame;
	private final JDialog menuDialog;

	private final JPanel basisPanel;
//	private JPanel leftPanel;
	private final DendroView2 dendroView;
	private final DendroController dendroController;
	private JButton ok_button;
	private String activeMenu;

	// Menus
//	private PixelSettingsPanel pixelSettings = null;
	private AnnotationPanel annotationSettings = null;
	private FontPanel fontSettings = null;
	private ThemeSettingsPanel themeSettings = null;
	private final URLSettings urlSettings = null;

	private ColorGradientChooser gradientPick = null;

//	private final ArrayList<MenuPanel> menuPanelList;

	/**
	 * Chained constructor in case DendroView isn't available
	 * 
	 * @param viewFrame
	 */
	public PreferencesMenu(final TreeViewFrame tvFrame, 
			final String menuTitle) {

		this(tvFrame, null, null, menuTitle);
	}

	/**
	 * Main constructor for Preferences Menu
	 * 
	 * @param tvFrame
	 * @param dendroView
	 * @param menuTitle
	 */
	public PreferencesMenu(final TreeViewFrame tvFrame,
			final DendroView2 dendroView, final DendroController controller,
			final String menuTitle) {

		this.tvFrame = tvFrame;
		this.applicationFrame = tvFrame.getAppFrame();
		this.dendroView = dendroView;
		this.dendroController = controller;
		this.configNode = tvFrame.getConfigNode().node(
				StringRes.pref_node_Preferences);
		this.activeMenu = menuTitle;

//		menuPanelList = new ArrayList<MenuPanel>();

		menuDialog = new JDialog();
		menuDialog.setTitle(menuTitle);
		menuDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		menuDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		menuDialog.setResizable(true);

		basisPanel = new JPanel();
		basisPanel.setLayout(new MigLayout());

		// Setting preferred size for the ContentPane of this frame
		final Dimension mainDim = GUIFactory.getScreenSize();

		final int width = mainDim.width * 1 / 2;
		final int height = mainDim.height * 1 / 2;

		basisPanel.setPreferredSize(new Dimension(width, height));

		menuDialog.getContentPane().add(basisPanel);

		setupLayout(menuTitle);

		menuDialog.pack();
		menuDialog.setLocationRelativeTo(applicationFrame);
	}

	/**
	 * Sets the visibility of clusterFrame.
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {

		menuDialog.setVisible(visible);
	}

	/**
	 * Returns the menu frame holding all the JPanels to display to the user.
	 * 
	 * @return
	 */
	public JDialog getPreferencesFrame() {

		return menuDialog;
	}

	public void synchronizeAnnotation() {

		if (annotationSettings != null) {
			annotationSettings.synchronize();

		} else {
			LogBuffer.println("AnnotationSettings object was null. "
					+ "Could not synchronize.");
		}
	}

	/**
	 * Returns the two indeces which represent the currently selected la
	 * 
	 * @return
	 */
	public int[] getSelectedLabelIndexes() {

		if (annotationSettings != null) {
			return new int[] { annotationSettings.getSelectedGeneIndex(),
					annotationSettings.getSelectedArrayIndex() };
		} else {
			LogBuffer.println("AnnotationSettings object was null. "
					+ "Could not get selected indeces.");
			return null;
		}
	}

	// Listeners
	/**
	 * Adds an ActionListener to the ok_button.
	 * 
	 * @param listener
	 */
	public void addOKButtonListener(final ActionListener listener) {

		ok_button.addActionListener(listener);
	}

	/**
	 * Equips the preferences JFrame with a window listener.
	 * 
	 * @param listener
	 */
	public void addWindowListener(final WindowAdapter listener) {

		menuDialog.addWindowListener(listener);
	}

	/**
	 * Adds an ActionListener to the darkThemeButton in ThemeSettings
	 * 
	 * @param listener
	 */
	public void addThemeListener(final ActionListener listener) {

		themeSettings.getDarkThemeButton().addActionListener(listener);
		themeSettings.getLightThemeButton().addActionListener(listener);
	}

	public void addCustomLabelListener(final ActionListener listener) {

		if (annotationSettings != null) {
			annotationSettings.getCustomLabelButton().addActionListener(
					listener);
		}
	}

//	public void addMenuListeners(final MouseListener l) {
//
//		for (final MenuPanel panel : menuPanelList) {
//
//			panel.getMenuPanel().addMouseListener(l);
//		}
//	}

	/**
	 * Adds a component listener to the JDialog in which the content of this
	 * class is held. This ensures repainting of all child components when the
	 * JDialog is being resized.
	 * 
	 * @param l
	 */
	public void addComponentListener(final ComponentListener l) {

		menuDialog.addComponentListener(l);
	}

	/**
	 * Sets up the layout for the menu.
	 */
	public void setupLayout(final String startMenu) {

//		menuPanelList.clear();
		setupPanels();

//		leftPanel = new JPanel();
//		leftPanel.setLayout(new MigLayout());
//		leftPanel.setBackground(GUIParams.BG_COLOR);
//		leftPanel.setBorder(BorderFactory.createEtchedBorder());
//
//		if (startMenu.equalsIgnoreCase(StringRes.menu_title_Prefs)
//				|| startMenu.equalsIgnoreCase(StringRes.menu_title_Font)
//				|| startMenu.equalsIgnoreCase(StringRes.menu_title_URL)) {
//			setupMenuHeaders(false);
//
//		} else {
//			setupMenuHeaders(true);
//		}
//
//		basisPanel.add(leftPanel, "pushy, aligny 0%, w 20%, " + "h 75%");

		ok_button = GUIFactory.setButtonLayout(StringRes.button_OK, null);

		addMenu(startMenu);

		menuDialog.validate();
		menuDialog.repaint();
	}

//	public void setupMenuHeaders(final boolean analysis) {
//
//		if (!analysis) {
//			final MenuPanel theme = new MenuPanel(StringRes.menu_title_Theme);
//			final JPanel themePanel = theme.getMenuPanel();
//			leftPanel.add(themePanel, "pushx, w 90%, h 10%, alignx 50%, "
//					+ "span, wrap");
//			menuPanelList.add(theme);
//
//			final MenuPanel font = new MenuPanel(StringRes.menu_title_Font);
//			final JPanel fontPanel = font.getMenuPanel();
//			leftPanel.add(fontPanel, "pushx, w 90%, h 10%, alignx 50%, "
//					+ "span, wrap");
//			menuPanelList.add(font);
//
//			final MenuPanel url = new MenuPanel(StringRes.menu_title_URL);
//			final JPanel urlPanel = url.getMenuPanel();
//			leftPanel.add(urlPanel, "pushx, w 90%, h 10%, alignx 50%, span");
//			menuPanelList.add(url);
//
//		} else {
//			final MenuPanel annotations = new MenuPanel(
//					StringRes.menu_title_RowAndCol);
//			final JPanel annotationsPanel = annotations.getMenuPanel();
//			leftPanel.add(annotationsPanel, "pushx, w 90%, h 10%, "
//					+ "alignx 50%, span, wrap");
//			menuPanelList.add(annotations);
//
//			final MenuPanel heatMap = new MenuPanel(StringRes.menu_title_Color);
//			final JPanel heatMapPanel = heatMap.getMenuPanel();
//			leftPanel.add(heatMapPanel, "pushx, w 90%, h 10%, alignx 50%, "
//					+ "span");
//			menuPanelList.add(heatMap);
//		}
//	}

	/**
	 * Setting up the menus depending on whether DendroView has been
	 * instantiated.
	 */
	public void setupPanels() {

		if (dendroView.isLoaded()) {
//			pixelSettings = new PixelSettingsPanel();
			annotationSettings = new AnnotationPanel();
			fontSettings = new FontPanel();

			gradientPick = new ColorGradientChooser(
					((DoubleArrayDrawer) dendroController.getArrayDrawer())
							.getColorExtractor(),
					DendrogramFactory.getColorPresets(), tvFrame.getDataModel()
							.getDataMatrix().getMinVal(), tvFrame
							.getDataModel().getDataMatrix().getMaxVal(),
					applicationFrame);

			// Adding GradientColorChooser configurations to DendroView node.
			gradientPick.setConfigNode(((TVModel) tvFrame.getDataModel())
					.getDocumentConfig());

			final ColorGradientController gradientControl = 
					new ColorGradientController(gradientPick);
		}

		themeSettings = new ThemeSettingsPanel();
	}

	/**
	 * Create the panel for pixel settings.
	 */
	class PixelSettingsPanel {

		private final ColorExtractor2 ce = null;
		private final JScrollPane scrollPane;

		public PixelSettingsPanel() {

			scrollPane = new JScrollPane();

			final JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIFactory.BG_COLOR);

			// try {
			// ce = ((DoubleArrayDrawer) dendroController.getArrayDrawer())
			// .getColorExtractor();
			//
			// } catch (final Exception e) {
			//
			// }
			//
			// PixelSettingsSelector pss = new PixelSettingsSelector(
			// dendroController.getGlobalXMap(),
			// dendroController.getGlobalYMap(), ce,
			// DendrogramFactory.getColorPresets());
			//
			// panel.add(pss, "push, grow");
			//
			// scrollPane.setViewportView(panel);
		}

		public JScrollPane makePSPanel() {

			return scrollPane;
		}
	}

	/**
	 * Create the panel for font settings.
	 */
	class FontPanel {

		private final JScrollPane scrollPane;

		public FontPanel() {

			scrollPane = new JScrollPane();

			final JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIFactory.BG_COLOR);

			final FontSettings fontSettings = new FontSettings(
					dendroView.getTextview(), dendroView.getArraynameview());

			final JLabel labelFont = GUIFactory.setupHeader("Set Label Font:");

			panel.add(labelFont, "span, wrap");
			panel.add(fontSettings.makeFontPanel(), "pushx, alignx 50%, w 95%");

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

		private final JScrollPane scrollPane;

		public URLSettings() {

			scrollPane = new JScrollPane();

			final JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIFactory.BG_COLOR);

			final UrlSettingsPanel genePanel = new UrlSettingsPanel(
					tvFrame.getUrlExtractor(), tvFrame.getGeneUrlPresets());

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
	 * 
	 * @author CKeil
	 * 
	 */
	class ThemeSettingsPanel {

		private final JRadioButton darkThemeButton;
		private final JRadioButton lightThemeButton;

		private final ButtonGroup themeButtonGroup;

		private final JScrollPane scrollPane;

		public ThemeSettingsPanel() {

			scrollPane = new JScrollPane();

			final JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIFactory.BG_COLOR);

			final JLabel label = new JLabel("Choose a Theme:");
			label.setForeground(GUIFactory.MAIN);
			label.setFont(GUIFactory.FONTL);

			panel.add(label, "span, wrap");

			darkThemeButton = GUIFactory
					.setRadioButtonLayout(StringRes.rButton_dark);
			lightThemeButton = GUIFactory
					.setRadioButtonLayout(StringRes.rButton_light);

			themeButtonGroup = new ButtonGroup();
			themeButtonGroup.add(darkThemeButton);
			themeButtonGroup.add(lightThemeButton);

			// Check for saved presets...
			final String default_theme = StringRes.rButton_dark;
			final String savedTheme = tvFrame.getConfigNode().get("theme",
					default_theme);

			// Since changing the theme resets the layout
			if (savedTheme.equalsIgnoreCase(StringRes.rButton_dark)) {
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
	 * 
	 * @author CKeil
	 * 
	 */
	class AnnotationPanel {

		private final JScrollPane scrollPane;
		private final JButton custom_button;
		private final HeaderSummaryPanel genePanel;
		private final HeaderSummaryPanel arrayPanel;

		public AnnotationPanel() {

			scrollPane = new JScrollPane();

			final JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIFactory.BG_COLOR);

			genePanel = new HeaderSummaryPanel(tvFrame.getDataModel()
					.getGeneHeaderInfo(), dendroView.getTextview()
					.getHeaderSummary(), tvFrame);

			arrayPanel = new HeaderSummaryPanel(tvFrame.getDataModel()
					.getArrayHeaderInfo(), dendroView.getArraynameview()
					.getHeaderSummary(), tvFrame);

			final JPanel loadLabelPanel = new JPanel();
			loadLabelPanel.setLayout(new MigLayout());
			loadLabelPanel.setOpaque(false);
			loadLabelPanel.setBorder(BorderFactory.createEtchedBorder());

			custom_button = GUIFactory.setButtonLayout(
					StringRes.button_customLabels, null);

			final JLabel rows = GUIFactory.setupHeader(StringRes.main_rows);
			final JLabel cols = GUIFactory.setupHeader(StringRes.main_cols);

			panel.add(rows, "pushx, alignx 50%");
			panel.add(cols, "pushx, alignx 50%, wrap");
			panel.add(genePanel, "pushx, alignx 50%, w 45%");
			panel.add(arrayPanel, "pushx, alignx 50%, w 45%, wrap");
			panel.add(custom_button, "pushx, alignx 50%, span");

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

		public int getSelectedGeneIndex() {

			return genePanel.getSmallestSelectedIndex();
		}

		public int getSelectedArrayIndex() {

			return arrayPanel.getSmallestSelectedIndex();
		}
	}

	/**
	 * Dynamically adds JScrollPane to the frame based on the MouseListener in
	 * MenuPanel.
	 * 
	 * @param title
	 */
	public void addMenu(final String title) {

		basisPanel.removeAll();
//		basisPanel.add(leftPanel, "pushy, aligny 0%, w 20%, h 75%");

		activeMenu = title;

		if (title.equalsIgnoreCase(StringRes.menu_title_Prefs)
				&& !dendroView.isLoaded()) {
			basisPanel.add(themeSettings.makeThemePanel(), "push, grow, wrap");
			
		} else if (title.equalsIgnoreCase(StringRes.menu_title_Prefs)
				&& dendroView.isLoaded()) {
			basisPanel.add(themeSettings.makeThemePanel(), "push, grow, " +
					"wrap");
			basisPanel.add(fontSettings.makeFontPanel(), "push, grow, wrap");

		} else if (title.equalsIgnoreCase(StringRes.menu_title_RowAndCol)
				&& annotationSettings != null) {
			basisPanel.add(annotationSettings.makeLabelPane(),"push, grow, " +
					"wrap");

		} else if (title.equalsIgnoreCase(StringRes.menu_title_Color)
				&& gradientPick != null) {
			basisPanel.add(gradientPick.makeGradientPanel(),"push, grow, " +
					"wrap");

		} else if (title.equalsIgnoreCase(StringRes.menu_title_URL)
				&& urlSettings != null) {

		} else {
			// In case menu cannot be loaded, display excuse.
			final JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIFactory.BG_COLOR);

			final JLabel hint = new JLabel("Menu cannot be shown because it "
					+ "wasn't loaded.");
			hint.setFont(GUIFactory.FONTS);
			hint.setForeground(GUIFactory.TEXT);
			panel.add(hint, "push, alignx 50%");

			basisPanel.add(panel, "w 79%, h 95%, wrap");
		}

		basisPanel.add(ok_button, "pushx, alignx 100%, span");

//		for (final MenuPanel panel : menuPanelList) {
//
//			if (panel.getLabelText().equals(title)) {
//
//				panel.setSelected(true);
//			}
//		}

		basisPanel.revalidate();
		basisPanel.repaint();
	}

	/**
	 * Returns the darkThemeButton from ThemeSettings for the controller.
	 * 
	 * @return
	 */
	public JRadioButton getLightButton() {

		return themeSettings.getLightThemeButton();
	}

	/**
	 * Returns the darkThemeButton from ThemeSettings for the controller.
	 * 
	 * @return
	 */
	public JRadioButton getDarkButton() {

		return themeSettings.getDarkThemeButton();
	}

//	/**
//	 * Gets the list which contains the JPanels used to select the menu type in
//	 * the PreferencesMenu window.
//	 * 
//	 * @return
//	 */
//	public ArrayList<MenuPanel> getMenuPanelList() {
//
//		return menuPanelList;
//	}

	/**
	 * Returns PreferencesMenu's configNode.
	 * 
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
	 * 
	 * @return String
	 */
	public String getActiveMenu() {

		return activeMenu;
	}
}
