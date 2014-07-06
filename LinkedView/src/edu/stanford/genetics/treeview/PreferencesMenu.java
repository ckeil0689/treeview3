package edu.stanford.genetics.treeview;

import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import Controllers.DendroController;
import GradientColorChoice.ColorGradientChooser;
import GradientColorChoice.ColorGradientController;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;
import edu.stanford.genetics.treeview.plugin.dendroview.DoubleArrayDrawer;
import edu.stanford.genetics.treeview.plugin.dendroview.FontSettings;

public class PreferencesMenu {

	private final TreeViewFrame tvFrame;
	private final Preferences configNode;
	private final JDialog menuDialog;

	private final JPanel basisPanel;
	private final DendroView2 dendroView;
	private final DendroController dendroController;
	private JButton ok_button;

	// Menus
	private AnnotationPanel annotationSettings;

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
		this.dendroView = dendroView;
		this.dendroController = controller;
		this.configNode = tvFrame.getConfigNode().node(
				StringRes.pref_node_Preferences);

		
		// Setup JDialog
		menuDialog = new JDialog();
		menuDialog.setTitle(StringRes.dialog_title_prefs);
		menuDialog.setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
		menuDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		menuDialog.setResizable(false);

		// Setup the basic content panel
		basisPanel = GUIFactory.createJPanel(false, true, null);

		menuDialog.getContentPane().add(basisPanel);

		setupLayout(menuTitle);

		menuDialog.pack();
		menuDialog.setLocationRelativeTo(tvFrame.getAppFrame());
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

//	/**
//	 * Adds an ActionListener to the darkThemeButton in ThemeSettings
//	 * 
//	 * @param listener
//	 */
//	public void addThemeListener(final ActionListener listener) {
//
//		themeSettings.getDarkThemeButton().addActionListener(listener);
//		themeSettings.getLightThemeButton().addActionListener(listener);
//	}

	public void addCustomLabelListener(final ActionListener listener) {

		if (annotationSettings != null) {
			annotationSettings.getCustomLabelButton().addActionListener(
					listener);
		}
	}

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

//	/**
//	 * Create the panel for pixel settings.
//	 */
//	class PixelSettingsPanel {
//
//		private final ColorExtractor2 ce = null;
//		private final JScrollPane scrollPane;
//
//		public PixelSettingsPanel() {
//
//			scrollPane = new JScrollPane();
//
//			final JPanel panel = GUIFactory.createJPanel(false, true, null);
//
//			 try {
//			 ce = ((DoubleArrayDrawer) dendroController.getArrayDrawer())
//			 .getColorExtractor();
//			
//			 } catch (final Exception e) {
//			
//			 }
//			
//			 PixelSettingsSelector pss = new PixelSettingsSelector(
//			 dendroController.getGlobalXMap(),
//			 dendroController.getGlobalYMap(), ce,
//			 DendrogramFactory.getColorPresets());
//			
//			 panel.add(pss, "push, grow");
//			
//			 scrollPane.setViewportView(panel);
//		}
//
//		public JScrollPane makePSPanel() {
//
//			return scrollPane;
//		}
//	}

	/**
	 * This class provides a JPanel which contains components to control
	 * font settings for the label views.
	 */
	class FontPanel {

		private final JPanel mainPanel;

		public FontPanel() {

			mainPanel = GUIFactory.createJPanel(false, true, null);

			final FontSettings fontSettings = new FontSettings(
					dendroView.getTextview(), dendroView.getArraynameview());

			final JLabel labelFont = GUIFactory.setupHeader("Set Label Font:");

			mainPanel.add(labelFont, "span, wrap");
			mainPanel.add(fontSettings.makeFontPanel(), 
					"pushx, alignx 50%, w 95%");
		}

		public JPanel makeFontPanel() {

			return mainPanel;
		}
	}

	/**
	 * This class provides a JPanel which contains components to select URL
	 * information sources, which are used to respond in the event of a user
	 * selecting a label in the label views, e.g. by opening a browser and
	 * displaying additional information.
	 */
	class URLSettings {

		private final JPanel mainPanel;

		public URLSettings() {

			mainPanel = GUIFactory.createJPanel(false, true, null);

			final UrlSettingsPanel genePanel = new UrlSettingsPanel(
					tvFrame.getUrlExtractor(), tvFrame.getGeneUrlPresets());

			final UrlSettingsPanel arrayPanel = new UrlSettingsPanel(
					tvFrame.getArrayUrlExtractor(),
					tvFrame.getArrayUrlPresets());

			mainPanel.add(genePanel, "pushx, alignx 50%, w 95%, wrap");
			mainPanel.add(arrayPanel, "pushx, alignx 50%, w 95%");
		}

		public JPanel makeURLPanel() {

			return mainPanel;
		}
	}

//	/**
//	 * Subclass to create a panel that handles theme settings.
//	 * 
//	 * @author CKeil
//	 * 
//	 */
//	class ThemeSettingsPanel {
//
//		private final JRadioButton darkThemeButton;
//		private final JRadioButton lightThemeButton;
//
//		private final ButtonGroup themeButtonGroup;
//
//		private final JScrollPane scrollPane;
//
//		public ThemeSettingsPanel() {
//
//			scrollPane = new JScrollPane();
//
//			final JPanel panel = GUIFactory.createJPanel(false, true, null);
//
//			final JLabel label = GUIFactory.createBigLabel("Choose a Theme:");
//
//			panel.add(label, "span, wrap");
//
//			darkThemeButton = GUIFactory
//					.setRadioButtonLayout(StringRes.rButton_dark);
//			lightThemeButton = GUIFactory
//					.setRadioButtonLayout(StringRes.rButton_light);
//
//			themeButtonGroup = new ButtonGroup();
//			themeButtonGroup.add(darkThemeButton);
//			themeButtonGroup.add(lightThemeButton);
//
//			// Check for saved presets...
//			final String default_theme = StringRes.rButton_dark;
//			final String savedTheme = tvFrame.getConfigNode().get("theme",
//					default_theme);
//
//			// Since changing the theme resets the layout
//			if (savedTheme.equalsIgnoreCase(StringRes.rButton_dark)) {
//				darkThemeButton.setSelected(true);
//
//			} else {
//				lightThemeButton.setSelected(true);
//			}
//
//			panel.add(lightThemeButton, "span, wrap");
//			panel.add(darkThemeButton, "span");
//
//			scrollPane.setViewportView(panel);
//		}
//
//		public JScrollPane makeThemePanel() {
//
//			return scrollPane;
//		}
//
//		public JRadioButton getDarkThemeButton() {
//
//			return darkThemeButton;
//		}
//
//		public JRadioButton getLightThemeButton() {
//
//			return lightThemeButton;
//		}
//	}

	/**
	 * Subclass for the Annotation settings panel.
	 * 
	 * @author CKeil
	 * 
	 */
	class AnnotationPanel {

		private final JPanel mainPanel;
		private final JButton custom_button;
		private final HeaderSummaryPanel genePanel;
		private final HeaderSummaryPanel arrayPanel;

		public AnnotationPanel() {

			mainPanel = GUIFactory.createJPanel(false, true, null);

			genePanel = new HeaderSummaryPanel(tvFrame.getDataModel()
					.getGeneHeaderInfo(), dendroView.getTextview()
					.getHeaderSummary(), tvFrame);

			arrayPanel = new HeaderSummaryPanel(tvFrame.getDataModel()
					.getArrayHeaderInfo(), dendroView.getArraynameview()
					.getHeaderSummary(), tvFrame);

			final JPanel loadLabelPanel = GUIFactory.createJPanel(false, 
					true, null);
			loadLabelPanel.setBorder(BorderFactory.createEtchedBorder());

			custom_button = GUIFactory.createButton(
					StringRes.button_CustomLabels);

			final JLabel rows = GUIFactory.setupHeader(StringRes.main_rows);
			final JLabel cols = GUIFactory.setupHeader(StringRes.main_cols);

			mainPanel.add(rows, "pushx, alignx 50%");
			mainPanel.add(cols, "pushx, alignx 50%, wrap");
			mainPanel.add(genePanel, "pushx, alignx 50%, w 45%");
			mainPanel.add(arrayPanel, "pushx, alignx 50%, w 45%, wrap");
			mainPanel.add(custom_button, "pushx, alignx 50%, span");
		}

		public JPanel makeLabelPane() {

			return mainPanel;
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
	public void setupLayout(final String menu) {

		basisPanel.removeAll();
		
		JPanel menuPanel;
		if (menu.equalsIgnoreCase(StringRes.menu_title_Font)
				&& dendroView.isLoaded()) {
			menuPanel = new FontPanel().makeFontPanel();

		} else if (menu.equalsIgnoreCase(StringRes.menu_title_RowAndCol)) {
			annotationSettings = new AnnotationPanel();
			menuPanel = annotationSettings.makeLabelPane();

		} else if (menu.equalsIgnoreCase(StringRes.menu_title_Color)) {
			ColorGradientChooser gradientPick = new ColorGradientChooser(
					tvFrame, ((DoubleArrayDrawer) dendroController
							.getArrayDrawer()).getColorExtractor());

			// Adding GradientColorChooser configurations to DendroView node.
			gradientPick.setConfigNode(((TVModel) tvFrame.getDataModel())
					.getDocumentConfig());
			
			final ColorGradientController gradientControl = 
					new ColorGradientController(gradientPick);
			
			menuPanel = gradientPick.makeGradientPanel();

		} else if (menu.equalsIgnoreCase(StringRes.menu_title_URL)) {
			menuPanel = new URLSettings().makeURLPanel();
			
		} else {
			// In case menu cannot be loaded, display excuse.
			menuPanel = GUIFactory.createJPanel(false, true, null);

			final JLabel hint = GUIFactory.createSmallLabel("Menu cannot be "
					+ "shown because it wasn't loaded.");
			menuPanel.add(hint, "push, alignx 50%");
		}

		ok_button = GUIFactory.createButton(StringRes.button_OK);
		
		basisPanel.add(menuPanel, "push, grow, wrap");
		basisPanel.add(ok_button, "pushx, alignx 100%, span");

		basisPanel.revalidate();
		basisPanel.repaint();
	}

//	/**
//	 * Returns the darkThemeButton from ThemeSettings for the controller.
//	 * 
//	 * @return
//	 */
//	public JRadioButton getLightButton() {
//
//		return themeSettings.getLightThemeButton();
//	}
//
//	/**
//	 * Returns the darkThemeButton from ThemeSettings for the controller.
//	 * 
//	 * @return
//	 */
//	public JRadioButton getDarkButton() {
//
//		return themeSettings.getDarkThemeButton();
//	}

	/**
	 * Returns PreferencesMenu's configNode.
	 * 
	 * @return
	 */
	public Preferences getConfigNode() {

		return configNode;
	}

//	public ColorGradientChooser getGradientPick() {
//
//		return gradientPick;
//	}

//	/**
//	 * Returns the name of the last chosen menu.
//	 * 
//	 * @return String
//	 */
//	public String getActiveMenu() {
//
//		return activeMenu;
//	}
}
