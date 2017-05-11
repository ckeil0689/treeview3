package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
 */
public class LabelSettings extends CustomDialog {

	/** Default serial version ID to keep Eclipse happy... */
	private static final long serialVersionUID = 1L;

	private final TreeViewFrame tvFrame;
	private String menu;
	private LabelInfo rowLabelInfo;
	private LabelInfo colLabelInfo;

	private final DendroView dendroView;
	private JButton okBtn;

	// Menus
	private AnnotationPanel annotationSettings;

	/**
	 * Main constructor for Preferences Menu
	 *
	 * @param tvFrame
	 */
	public LabelSettings(final TreeViewFrame tvFrame) {

		super(StringRes.dlg_Labels);
		this.tvFrame = tvFrame;
		dendroView = tvFrame.getDendroView();
	}

	public void setLabelInfo(final LabelInfo rowLabelInfo,
		final LabelInfo colLabelInfo) {

		this.rowLabelInfo = rowLabelInfo;
		this.colLabelInfo = colLabelInfo;
	}

	public void setMenu(final String menu) {

		this.menu = menu;
		setupLayout();
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

		if(annotationSettings != null) {
			annotationSettings.synchronize();

		}
		else {
			LogBuffer.println("AnnotationSettings object was null. " +
				"Could not synchronize.");
		}
	}

	/**
	 * Returns the two indeces which represent the currently selected la
	 *
	 * @return
	 */
	public int[] getSelectedLabelIndexes() {

		if(annotationSettings == null) {
			LogBuffer.println("AnnotationSettings object was null. " +
				"Could not get selected indeces.");
			return null;
		}

		return new int[] {annotationSettings.getSelectedRowIndex(),
			annotationSettings.getSelectedColIndex()};
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

	public void addJustifyListener(final ActionListener listener) {

		if(annotationSettings != null) {
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

		private JPanel fontMainPanel;

		public FontPanel() {

			final FontSettings fontSettings = new FontSettings(dendroView
				.getRowLabelView(),
				dendroView.getColLabelView());

			fontMainPanel = fontSettings.makeFontPanel();
		}

		public JPanel makeFontPanel() {

			return fontMainPanel;
		}
	}

	/**
	 * This class provides a JPanel which contains components to select URL
	 * information sources, which are used to respond in the event of a user
	 * selecting a label in the label views, e.g. by opening a browser and
	 * displaying additional information.
	 */
	class URLSettings {

		private final JPanel urlMainPanel;

		public URLSettings() {

			urlMainPanel = GUIFactory.createJPanel(false,GUIFactory.NO_INSETS);

			final UrlSettingsPanel genePanel = new UrlSettingsPanel(
				tvFrame.getUrlExtractor(),
				tvFrame.getGeneUrlPresets());

			final UrlSettingsPanel arrayPanel = new UrlSettingsPanel(tvFrame
				.getArrayUrlExtractor(),
				tvFrame.getArrayUrlPresets());

			urlMainPanel.add(genePanel.generate("Row URLs"),"pushx, " +
				"alignx 50%, w 95%, wrap");
			urlMainPanel.add(arrayPanel.generate("Column URLs"),"pushx, " +
				"alignx 50%, w 95%");

			urlMainPanel.revalidate();
			urlMainPanel.repaint();
		}

		public JPanel makeURLPanel() {

			return urlMainPanel;
		}
	}

	/** Subclass for the Annotation settings panel. */
	class AnnotationPanel {

		private final JPanel annotationMainPanel;

		private final LabelSummaryPanel rowPanel;
		private final LabelSummaryPanel colPanel;

		private final JRadioButton rowRightJustBtn;
		private final JRadioButton rowLeftJustBtn;
		private final JRadioButton colRightJustBtn;
		private final JRadioButton colLeftJustBtn;

		public AnnotationPanel() {

			annotationMainPanel = GUIFactory.createJPanel(false,
				GUIFactory.NO_INSETS);

			rowPanel = new LabelSummaryPanel(rowLabelInfo,dendroView
				.getRowLabelView());

			colPanel = new LabelSummaryPanel(colLabelInfo,dendroView
				.getColLabelView());

			final JPanel loadLabelPanel = GUIFactory
				.createJPanel(false,GUIFactory.NO_INSETS);
			loadLabelPanel.setBorder(BorderFactory.createEtchedBorder());

			final JLabel rows = GUIFactory.setupLabelType(StringRes.main_rows);
			final JLabel cols = GUIFactory.setupLabelType(StringRes.main_cols);

			/* Label alignment */
			JPanel justifyPanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT);
			justifyPanel.setBorder(BorderFactory.createTitledBorder(
				"Label justification"));

			final ButtonGroup rowJustifyBtnGroup = new ButtonGroup();

			rowLeftJustBtn = GUIFactory.createRadioBtn("Left");
			rowRightJustBtn = GUIFactory.createRadioBtn("Right");

			if(dendroView.getRowLabelView().getLabelAttributes().isRightJustified()) {
				rowRightJustBtn.setSelected(true);
			}
			else {
				rowLeftJustBtn.setSelected(true);
			}

			rowJustifyBtnGroup.add(rowLeftJustBtn);
			rowJustifyBtnGroup.add(rowRightJustBtn);

			final JPanel rowRadioBtnPanel = GUIFactory
				.createJPanel(false,GUIFactory.DEFAULT);

			rowRadioBtnPanel.add(rowLeftJustBtn,"span, wrap");
			rowRadioBtnPanel.add(rowRightJustBtn,"span");

			final ButtonGroup colJustifyBtnGroup = new ButtonGroup();

			colRightJustBtn = GUIFactory.createRadioBtn("Top");
			colLeftJustBtn = GUIFactory.createRadioBtn("Bottom");

			if(dendroView.getColLabelView().getLabelAttributes().isRightJustified()) {
				colRightJustBtn.setSelected(true);
			}
			else {
				colLeftJustBtn.setSelected(true);
			}

			colJustifyBtnGroup.add(colRightJustBtn);
			colJustifyBtnGroup.add(colLeftJustBtn);

			final JPanel colRadioBtnPanel = GUIFactory
				.createJPanel(false,GUIFactory.DEFAULT);

			colRadioBtnPanel.add(colRightJustBtn,"span, wrap");
			colRadioBtnPanel.add(colLeftJustBtn,"span");

			annotationMainPanel.add(rows,"pushx, alignx 50%");
			annotationMainPanel.add(cols,"pushx, alignx 50%, wrap");
			annotationMainPanel.add(rowPanel,"pushx, alignx 50%, w 45%");
			annotationMainPanel.add(colPanel,"pushx, alignx 50%, w 45%, wrap");
			justifyPanel.add(rowRadioBtnPanel,"pushx, alignx 50%, w 45%");
			justifyPanel.add(colRadioBtnPanel,"pushx, alignx 50%, w 45%");
			annotationMainPanel.add(justifyPanel,
				"push, grow, alignx 50%, span, wrap");

			JPanel fontPanel = new FontPanel().makeFontPanel();

			annotationMainPanel.add(fontPanel,"push, grow, alignx 50%, span");

			annotationMainPanel.revalidate();
			annotationMainPanel.repaint();
		}

		public JPanel makeLabelPane() {

			return annotationMainPanel;
		}

		public void synchronize() {

			rowPanel.synchronizeTo();
			colPanel.synchronizeTo();
		}

		public void addJustifyListener(final ActionListener l) {

			rowLeftJustBtn.addActionListener(l);
			rowRightJustBtn.addActionListener(l);
			colLeftJustBtn.addActionListener(l);
			colRightJustBtn.addActionListener(l);
		}

		public int getSelectedRowIndex() {

			return rowPanel.getSmallestSelectedIndex();
		}

		public int getSelectedColIndex() {

			return colPanel.getSmallestSelectedIndex();
		}
	}

	/**
	 * Dynamically adds JScrollPane to the frame based on the MouseListener in
	 * MenuPanel.
	 *
	 * @param title
	 */
	@Override
	protected void setupLayout() {

		mainPanel.removeAll();

		JPanel menuPanel;
		if(menu.equalsIgnoreCase(StringRes.menu_Font) && tvFrame.isLoaded()) {
			menuPanel = new FontPanel().makeFontPanel();

		}
		else if(menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
			annotationSettings = new AnnotationPanel();
			menuPanel = annotationSettings.makeLabelPane();

		}
		else if(menu.equalsIgnoreCase(StringRes.menu_URL)) {
			menuPanel = new URLSettings().makeURLPanel();

		}
		else {
			// In case menu cannot be loaded, display excuse.
			menuPanel = GUIFactory.createJPanel(false,GUIFactory.NO_INSETS);

			final JLabel hint = GUIFactory.createLabel("Menu cannot be " +
				"shown because it wasn't loaded.",GUIFactory.FONTS);
			menuPanel.add(hint,"push, alignx 50%");
		}

		okBtn = GUIFactory.createBtn(StringRes.btn_OK);

		mainPanel.add(menuPanel,"push, grow, wrap");
		mainPanel.add(okBtn,"pushx, alignx 100%, span");

		mainPanel.revalidate();
		mainPanel.repaint();
	}
}
