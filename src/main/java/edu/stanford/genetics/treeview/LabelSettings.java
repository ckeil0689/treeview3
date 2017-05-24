package edu.stanford.genetics.treeview;

import java.awt.Dimension;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

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

		private final JRadioButton showAllPossible;
		private final JRadioButton showSome;
		private final JRadioButton showNone;
		private final JSpinner neighborSpinner;

		public AnnotationPanel() {

			annotationMainPanel = GUIFactory.createJPanel(false,
				GUIFactory.DEFAULT);
			annotationMainPanel.setMinimumSize(new Dimension(400,400));

			/* Label alignment */
			JPanel includePanel = GUIFactory.createJPanel(false,
				GUIFactory.TINY_GAPS_AND_INSETS);
			includePanel.setBorder(BorderFactory.createTitledBorder(
				"Show Label Type(s)"));

			rowPanel = new LabelSummaryPanel(rowLabelInfo,
				dendroView.getRowLabelView());

			colPanel = new LabelSummaryPanel(colLabelInfo,
				dendroView.getColLabelView());

			final JPanel loadLabelPanel = GUIFactory
				.createJPanel(false,GUIFactory.NO_INSETS);
			loadLabelPanel.setBorder(BorderFactory.createEtchedBorder());

			final JLabel rows = GUIFactory.setupLabelType(StringRes.main_rows);
			final JLabel cols = GUIFactory.setupLabelType(StringRes.main_cols);

			/* Label alignment */
			JPanel justifyPanel = GUIFactory.createJPanel(false,
				GUIFactory.TINY_GAPS_AND_INSETS);
			justifyPanel.setBorder(BorderFactory.createTitledBorder(
				"Justification"));

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

			/* Label alignment */
			JPanel showPanel = GUIFactory.createJPanel(false,
				GUIFactory.TINY_GAPS_AND_INSETS);
			showPanel.setBorder(BorderFactory.createTitledBorder(
				"When labels don't fit, show...  [spacebar toggles]"));
			showAllPossible = GUIFactory.createRadioBtn("As many as possible");
			showSome =
				GUIFactory.createRadioBtn("Hovered label and ");
			neighborSpinner = new JSpinner(new SpinnerNumberModel(
				dendroView.getRowLabelView().getMaxLabelPortFlankSize(),
				0,getMaxNumLabels(),1));
			showNone =
				GUIFactory.createRadioBtn("None");

			//Both row & column label views have this, but we only need to check
			//one because we synch their behavior
			if(dendroView.getRowLabelView().inLabelPortMode() &&
				!dendroView.getRowLabelView().isLabelPortFlankMode()) {

				showAllPossible.setSelected(true);
				setShowBaseIsNone(false);
			} else if(dendroView.getRowLabelView().inLabelPortMode() &&
				dendroView.getRowLabelView().isLabelPortFlankMode()) {

				showSome.setSelected(true);
				setShowBaseIsNone(false);
			} else {
				showNone.setSelected(true);
				setShowBaseIsNone(true);
			}

			final ButtonGroup showBtnGroup = new ButtonGroup();
			showBtnGroup.add(showAllPossible);
			showBtnGroup.add(showSome);
			showBtnGroup.add(showNone);

			showPanel.add(showAllPossible,"span, wrap");
			showPanel.add(showSome,"");
			showPanel.add(neighborSpinner,"alignx left");
			showPanel.add(new JLabel(" neighboring labels"),
				"alignx left, wrap");
			showPanel.add(showNone);

			annotationMainPanel.add(rows,"pushx, alignx 50%, aligny 0%, h 15%");
			annotationMainPanel.add(cols,"pushx, alignx 50%, aligny 0%, h 15%, wrap");
			includePanel.add(rowPanel,"pushx, alignx 50%, w 45%");
			includePanel.add(colPanel,"pushx, alignx 50%, w 45%");
			annotationMainPanel.add(includePanel,
				"push, grow, alignx 50%, span, aligny 0%, h 25%:null:null, wrap");
			justifyPanel.add(rowRadioBtnPanel,"pushx, alignx 50%, w 45%");
			justifyPanel.add(colRadioBtnPanel,"pushx, alignx 50%, w 45%");
			annotationMainPanel.add(justifyPanel,
				"push, grow, alignx 50%, span, wrap");

			JPanel fontPanel = new FontPanel().makeFontPanel();

			annotationMainPanel.add(fontPanel,
				"push, grow, alignx 50%, span, wrap");

			annotationMainPanel.add(showPanel,"push, grow, alignx 50%, span");

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

		public void addShowListener(final ActionListener l) {

			showAllPossible.addActionListener(l);
			showSome.addActionListener(l);
			showNone.addActionListener(l);
		}

		public void addFlankSizeListener(final ChangeListener l) {
			neighborSpinner.addChangeListener(l);
		}

		public int getSelectedRowIndex() {

			return rowPanel.getSmallestSelectedIndex();
		}

		public int getSelectedColIndex() {

			return colPanel.getSmallestSelectedIndex();
		}

		public int getNumNeighbors() {
			return((int) neighborSpinner.getValue());
		}
	}

	public int getNumNeighbors() {
		return(annotationSettings.getNumNeighbors());
	}

	public void addShowListener(ActionListener l) {
		annotationSettings.addShowListener(l);
	}

	public void addFlankSizeListener(final ChangeListener l) {
		annotationSettings.addFlankSizeListener(l);
	}

	/**
	 * This method determines what toggling the labels from show as many as
	 * possible will result in (either no labels or some labels)
	 * @return the showBaseIsNone
	 */
	public boolean isShowBaseIsNone() {
		return(dendroView.getRowLabelView().isLabelPortDefaultNone());
	}

	/**
	 * This method sets what toggling the labels from show as many as
	 * possible will be toggled to (either no labels or some labels)
	 * @param showBaseIsNone the showBaseIsNone to set
	 */
	public void setShowBaseIsNone(boolean showBaseIsNone) {
		dendroView.getRowLabelView().setLabelPortDefaultNone(showBaseIsNone);
		dendroView.getColLabelView().setLabelPortDefaultNone(showBaseIsNone);
	}

	public void setFlankSize(final int s) {
		dendroView.getColLabelView().setMaxLabelPortFlankSize(s);
		dendroView.getRowLabelView().setMaxLabelPortFlankSize(s);
	}

	protected int getMaxNumLabels() {
		int numXLabels =
			dendroView.getInteractiveMatrixView().getXMap().getTotalTileNum();
		int numYLabels =
			dendroView.getInteractiveMatrixView().getYMap().getTotalTileNum();
		if(numXLabels > numYLabels) {
			return(numXLabels);
		}
		return(numYLabels);
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
