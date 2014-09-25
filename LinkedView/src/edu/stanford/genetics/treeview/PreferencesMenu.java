package edu.stanford.genetics.treeview;

import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.StringRes;
import ColorChooser.ColorChooser;
import Controllers.PreferencesController;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView;
import edu.stanford.genetics.treeview.plugin.dendroview.FontSettings;

/**
 * A dialog that can contain various menus, depending on which the user
 * chooses to open. This is done by setting the contents of the dialog's
 * contentPane based on the clicked JMenuItem in the menubar.
 * @author CKeil
 *
 */
public class PreferencesMenu extends CustomDialog implements ConfigNodePersistent {

	private final TreeViewFrame tvFrame;
	private HeaderInfo geneHI;
	private HeaderInfo arrayHI;
	private Preferences configNode;
	
	private final DendroView dendroView;
	private ColorChooser gradientPick;
	private JButton ok_btn;

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

		dialog.getContentPane().add(mainPanel);
		dialog.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosed(WindowEvent e) {
				
				if(gradientPick != null && gradientPick.isCustomSelected()) {
					gradientPick.saveStatus();
				}
			}
		});
	}
	
	public void setGradientChooser(ColorChooser gradientPick) {
		
		this.gradientPick = gradientPick;
	}
	
	public void setHeaderInfo(HeaderInfo geneHI, HeaderInfo arrayHI) {
		
		this.geneHI = geneHI;
		this.arrayHI = arrayHI;
	}
	
	/**
	 * Setting the configNode for the PreferencesMenu
	 * @param configNode
	 */
	@Override
	public void setConfigNode(Preferences parentNode) {
		
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

		return dialog;
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

		ok_btn.addActionListener(listener);
	}

	/**
	 * Equips the preferences JFrame with a window listener.
	 * 
	 * @param listener
	 */
	public void addWindowListener(final WindowAdapter listener) {

		dialog.addWindowListener(listener);
	}

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

		dialog.addComponentListener(l);
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

			final JLabel labelFont = GUIFactory.createLabel("Set Label Font:", 
					GUIFactory.FONTL);

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

			genePanel = new HeaderSummaryPanel(geneHI, 
					dendroView.getTextview().getHeaderSummary(), tvFrame);

			arrayPanel = new HeaderSummaryPanel(arrayHI, 
					dendroView.getArraynameview().getHeaderSummary(), tvFrame);

			final JPanel loadLabelPanel = GUIFactory.createJPanel(false, 
					true, null);
			loadLabelPanel.setBorder(BorderFactory.createEtchedBorder());

			custom_button = GUIFactory.createBtn(
					StringRes.btn_CustomLabels);

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

		mainPanel.removeAll();
		
		JPanel menuPanel;
		if (menu.equalsIgnoreCase(StringRes.menu_Font)
				&& tvFrame.isLoaded()) {
			menuPanel = new FontPanel().makeFontPanel();

		} else if (menu.equalsIgnoreCase(StringRes.menu_RowAndCol)) {
			annotationSettings = new AnnotationPanel();
			menuPanel = annotationSettings.makeLabelPane();

		} else if (menu.equalsIgnoreCase(StringRes.menu_Color) 
				&& gradientPick != null) {
			menuPanel = gradientPick.makeGradientPanel();
		

		} else if (menu.equalsIgnoreCase(StringRes.menu_URL)) {
			menuPanel = new URLSettings().makeURLPanel();
			
		} else {
			// In case menu cannot be loaded, display excuse.
			menuPanel = GUIFactory.createJPanel(false, true, null);

			final JLabel hint = GUIFactory.createLabel("Menu cannot be "
					+ "shown because it wasn't loaded.", GUIFactory.FONTS);
			menuPanel.add(hint, "push, alignx 50%");
		}

		ok_btn = GUIFactory.createBtn(StringRes.btn_OK);
		
		mainPanel.add(menuPanel, "push, grow, wrap");
		mainPanel.add(ok_btn, "pushx, alignx 100%, span");

		mainPanel.revalidate();
		mainPanel.repaint();
		
		dialog.pack();
		dialog.setLocationRelativeTo(tvFrame.getAppFrame());
	}

	/**
	 * Returns PreferencesMenu's configNode.
	 * 
	 * @return
	 */
	public Preferences getConfigNode() {

		return configNode;
	}
	
	// Layout Test
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				
				String menuTitle = ""; // change this to see different menus
				
				PreferencesMenu prefMenu = new PreferencesMenu(null);
				
				final PreferencesController pController = 
						new PreferencesController(null, null, prefMenu);

				prefMenu.setVisible(true);
			}
		});
    }
}
