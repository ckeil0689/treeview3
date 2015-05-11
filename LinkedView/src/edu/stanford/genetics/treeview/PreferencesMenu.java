package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.FontSettings;

/**
 * A dialog that can contain various menus, depending on which the user chooses
 * to open. This is done by setting the contents of the dialog's contentPane
 * based on the clicked JMenuItem in the menubar.
 *
 * @author CKeil
 *
 */
public class PreferencesMenu extends CustomDialog implements
ConfigNodePersistent {

	/**
	 * Default serial version ID to keep Eclipse happy...
	 */
	private static final long serialVersionUID = 1L;
	
	private final TreeViewFrame tvFrame;
	private HeaderInfo geneHI;
	private HeaderInfo arrayHI;
	private Preferences configNode;

	private final DendroView dendroView;
	private JButton okBtn;

	// Menus
	private AnnotationPanel annotationSettings;

	/**
	 * Chained constructor
	 *
	 * @param viewFrame
	 */
	public PreferencesMenu() {

		this(null);
	}

	/**
	 * Main constructor for Preferences Menu
	 *
	 * @param tvFrame
	 */
	public PreferencesMenu(final TreeViewFrame tvFrame) {

		super(StringRes.dlg_prefs);
		this.tvFrame = tvFrame;
		this.dendroView = tvFrame.getDendroView();

		getContentPane().add(mainPanel);
	}

	public void setHeaderInfo(final HeaderInfo geneHI, final HeaderInfo arrayHI) {

		this.geneHI = geneHI;
		this.arrayHI = arrayHI;
	}

	/**
	 * Setting the configNode for the PreferencesMenu
	 *
	 * @param configNode
	 */
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(StringRes.pnode_Preferences);

		} else {
			LogBuffer.println("Could not find or create PreferencesMenu "
					+ "node because parentNode was null.");
		}
	}

	/**
	 * Returns the menu frame holding all the JPanels to display to the user.
	 *
	 * @return JDialog
	 */
	public JDialog getPreferencesFrame() {

		return this;
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

		if (annotationSettings == null) {
			LogBuffer.println("AnnotationSettings object was null. "
					+ "Could not get selected indeces.");
			return null;
		}
		
		return new int[] { annotationSettings.getSelectedGeneIndex(),
				annotationSettings.getSelectedArrayIndex() };
	}

	/* >>>>>> GUI component listeners <<<<< */
	/**
	 * Adds an ActionListener to the ok_button.
	 *
	 * @param listener
	 */
	public void addOKButtonListener(final ActionListener listener) {

		okBtn.addActionListener(listener);
	}

	/**
	 * Equips the preferences JFrame with a window listener.
	 *
	 * @param listener
	 */
	public void addSaveAndCloseListener(final WindowAdapter listener) {

		addWindowListener(listener);
	}

	public void addCustomLabelListener(final ActionListener listener) {

		if (annotationSettings != null) {
			annotationSettings.getCustomLabelButton().addActionListener(
					listener);
		}
	}

	public void addJustifyListener(final ActionListener listener) {

		if (annotationSettings != null) {
			annotationSettings.addJustifyListener(listener);
		}
	}

	/**
	 * Adds a component listener to the JDialog in which the content of this
	 * class is held. This ensures repainting of all child components when the
	 * JDialog is being resized.
	 *
	 * @param l
	 */
	public void addResizeDialogListener(final ComponentListener l) {

		addComponentListener(l);
	}

	/**
	 * This class provides a JPanel which contains components to control font
	 * settings for the label views.
	 */
	class FontPanel {

		private JPanel mainPanel;

		public FontPanel() {

			final FontSettings fontSettings = new FontSettings(
					dendroView.getRowLabelView(),
					dendroView.getColumnLabelView());

			mainPanel = fontSettings.makeFontPanel();
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

			mainPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING,
					null);

			final UrlSettingsPanel genePanel = new UrlSettingsPanel(
					tvFrame.getUrlExtractor(), tvFrame.getGeneUrlPresets());

			final UrlSettingsPanel arrayPanel = new UrlSettingsPanel(
					tvFrame.getArrayUrlExtractor(),
					tvFrame.getArrayUrlPresets());

			mainPanel.add(genePanel.generate("Row URLs"), "pushx, "
					+ "alignx 50%, w 95%, wrap");
			mainPanel.add(arrayPanel.generate("Column URLs"), "pushx, "
					+ "alignx 50%, w 95%");
		}

		public JPanel makeURLPanel() {

			return mainPanel;
		}
	}

	/**
	 * Subclass for the Annotation settings panel.
	 *
	 * @author CKeil
	 *
	 */
	class AnnotationPanel {

		private final JPanel mainPanel;
		private final JButton custom_btn;

		private final HeaderSummaryPanel genePanel;
		private final HeaderSummaryPanel arrayPanel;

		private final JRadioButton rowRightJustBtn;
		private final JRadioButton rowLeftJustBtn;
		private final JRadioButton colRightJustBtn;
		private final JRadioButton colLeftJustBtn;

		public AnnotationPanel() {

			mainPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING,
					null);

			genePanel = new HeaderSummaryPanel(geneHI, dendroView
					.getRowLabelView().getHeaderSummary());

			arrayPanel = new HeaderSummaryPanel(arrayHI, dendroView
					.getColumnLabelView().getHeaderSummary());

			final JPanel loadLabelPanel = GUIFactory.createJPanel(false,
					GUIFactory.NO_PADDING, null);
			loadLabelPanel.setBorder(BorderFactory.createEtchedBorder());

			custom_btn = GUIFactory.createBtn(StringRes.btn_CustomLabels);

			final JLabel rows = GUIFactory.setupHeader(StringRes.main_rows);
			final JLabel cols = GUIFactory.setupHeader(StringRes.main_cols);

			/* Label alignment */
			JPanel justifyPanel = GUIFactory.createJPanel(false, 
					GUIFactory.DEFAULT, null);
			justifyPanel.setBorder(BorderFactory.createTitledBorder(
					"Label justification"));

			final ButtonGroup rowJustifyBtnGroup = new ButtonGroup();

			rowLeftJustBtn = GUIFactory.createRadioBtn("Left");
			rowRightJustBtn = GUIFactory.createRadioBtn("Right");

			if (dendroView.getRowLabelView().getJustifyOption()) {
				rowRightJustBtn.setSelected(true);
			} else {
				rowLeftJustBtn.setSelected(true);
			}

			rowJustifyBtnGroup.add(rowLeftJustBtn);
			rowJustifyBtnGroup.add(rowRightJustBtn);

			final JPanel rowRadioBtnPanel = GUIFactory.createJPanel(false,
					GUIFactory.DEFAULT, null);

			rowRadioBtnPanel.add(rowLeftJustBtn, "span, wrap");
			rowRadioBtnPanel.add(rowRightJustBtn, "span");

			final ButtonGroup colJustifyBtnGroup = new ButtonGroup();

			colRightJustBtn = GUIFactory.createRadioBtn("Top");
			colLeftJustBtn = GUIFactory.createRadioBtn("Bottom");

			if (dendroView.getColumnLabelView().getJustifyOption()) {
				colRightJustBtn.setSelected(true);
			} else {
				colLeftJustBtn.setSelected(true);
			}

			colJustifyBtnGroup.add(colRightJustBtn);
			colJustifyBtnGroup.add(colLeftJustBtn);

			final JPanel colRadioBtnPanel = GUIFactory.createJPanel(false,
					GUIFactory.DEFAULT, null);

			colRadioBtnPanel.add(colRightJustBtn, "span, wrap");
			colRadioBtnPanel.add(colLeftJustBtn, "span");

			mainPanel.add(rows, "pushx, alignx 50%");
			mainPanel.add(cols, "pushx, alignx 50%, wrap");
			mainPanel.add(genePanel, "pushx, alignx 50%, w 45%");
			mainPanel.add(arrayPanel, "pushx, alignx 50%, w 45%, wrap");
			justifyPanel.add(rowRadioBtnPanel, "pushx, alignx 50%, w 45%");
			justifyPanel.add(colRadioBtnPanel, "pushx, alignx 50%, w 45%");
			mainPanel.add(justifyPanel, "push, grow, alignx 50%, span, wrap");
			
			JPanel fontPanel = new FontPanel().makeFontPanel();
			
			mainPanel.add(fontPanel, "push, grow, alignx 50%, span");

			// Commented out for version 3.0alpha1 because it doesn't work yet
			// mainPanel.add(custom_button, "pushx, alignx 50%, span");

		}

		public JPanel makeLabelPane() {

			return mainPanel;
		}

		public JButton getCustomLabelButton() {

			return custom_btn;
		}

		public void synchronize() {

			genePanel.synchronizeTo();
			arrayPanel.synchronizeTo();
		}

		public void addJustifyListener(final ActionListener l) {

			rowLeftJustBtn.addActionListener(l);
			rowRightJustBtn.addActionListener(l);
			colLeftJustBtn.addActionListener(l);
			colRightJustBtn.addActionListener(l);
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

		mainPanel.removeAll();

		JPanel menuPanel;
		if (menu.equalsIgnoreCase(StringRes.menu_Font) && tvFrame.isLoaded()) {
			menuPanel = new FontPanel().makeFontPanel();

		} else if (menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
			annotationSettings = new AnnotationPanel();
			menuPanel = annotationSettings.makeLabelPane();

		} else if (menu.equalsIgnoreCase(StringRes.menu_URL)) {
			menuPanel = new URLSettings().makeURLPanel();

		} else {
			// In case menu cannot be loaded, display excuse.
			menuPanel = GUIFactory.createJPanel(false, GUIFactory.NO_PADDING,
					null);

			final JLabel hint = GUIFactory.createLabel("Menu cannot be "
					+ "shown because it wasn't loaded.", GUIFactory.FONTS);
			menuPanel.add(hint, "push, alignx 50%");
		}

		okBtn = GUIFactory.createBtn(StringRes.btn_OK);

		mainPanel.add(menuPanel, "push, grow, wrap");
		mainPanel.add(okBtn, "pushx, alignx 100%, span");

		mainPanel.revalidate();
		mainPanel.repaint();

//		dialog.pack();
//		dialog.setLocationRelativeTo(tvFrame.getAppFrame());
	}

	/**
	 * Returns PreferencesMenu's configNode.
	 *
	 * @return
	 */
	public Preferences getConfigNode() {

		return configNode;
	}
}
