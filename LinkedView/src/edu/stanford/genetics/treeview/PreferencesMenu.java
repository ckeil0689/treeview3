package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

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
		
		menuFrame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(final WindowEvent we) {
				
				menuFrame.dispose();
			}
		});
		
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
	
	/**
	 * Sets up the layout for the menu.
	 */
	public void setupLayout(String startMenu) {
		
		menuFrame.getContentPane().removeAll();
		
		ok_button = GUIParams.setButtonLayout("OK", null);
		ok_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				menuFrame.dispose();
			}
			
		});
		
		setupMenus();
		
		leftPanel = new JPanel();
		leftPanel.setLayout(new MigLayout());
		leftPanel.setBackground(GUIParams.BG_COLOR);
		leftPanel.setBorder(BorderFactory.createEtchedBorder());
		
		setupMenuHeaders();
		
		basisPanel.add(leftPanel, "pushy, aligny 0%, w 20%, " +
				"h 75%");
		
		if(startMenu.equalsIgnoreCase("Theme") && themeSettings != null) {
			basisPanel.add(themeSettings, "w 79%, h 95%, wrap");
		
		} else if(startMenu.equalsIgnoreCase("Fonts") 
				&& fontSettings != null) {
			basisPanel.add(fontSettings, "w 79%, h 95%, wrap");
			
		} else if(startMenu.equalsIgnoreCase("URL") 
				&& urlSettings != null) {
			basisPanel.add(urlSettings, "w 79%, h 95%, wrap");
		
		} else if(startMenu.equalsIgnoreCase("Row and Column Labels") 
				&& annotationSettings != null) {
			basisPanel.add(annotationSettings, "w 79%, " +
					"h 95%, wrap");
			
		} else if(startMenu.equalsIgnoreCase("Color Settings") 
				&& pixelSettings != null) {
			basisPanel.add(pixelSettings, "w 79%, h 95%, wrap");
		}
		
		basisPanel.add(ok_button, "pushx, alignx 100%, span");
		
		menuFrame.validate();
		menuFrame.repaint();
	}
	
	public void setupMenuHeaders() {
		
		MenuPanel theme = new MenuPanel("Theme", this);
		leftPanel.add(theme, "pushx, w 90%, h 10%, alignx 50%, span, wrap");
		
		MenuPanel annotations = new MenuPanel("Annotations", this);
		leftPanel.add(annotations, "pushx, w 90%, h 10%, alignx 50%, " +
				"span, wrap");
			
		MenuPanel font = new MenuPanel("Font", this);
		leftPanel.add(font, "pushx, w 90%, h 10%, alignx 50%, span, wrap");
			
		MenuPanel url = new MenuPanel("URL", this);
		leftPanel.add(url, "pushx, w 90%, h 10%, alignx 50%, span, wrap");
		
		MenuPanel heatMap = new MenuPanel("Color Settings", this);
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
	class PixelSettingsPanel extends JScrollPane {

		private static final long serialVersionUID = 1L;
		
		private ColorExtractor ce = null;

		public PixelSettingsPanel() {
			
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
			
			setViewportView(panel);
		}
	}
	
	/**
	 * Create the panel for font settings.
	 */
	class FontPanel extends JScrollPane {

		private static final long serialVersionUID = 1L;

		public FontPanel() {
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			final FontSettingsPanel fontChangePanel = new FontSettingsPanel(
					dendroView.getTextview(), dendroView.getArraynameview());
			
			JLabel labelFont = GUIParams.setupHeader("Set Label Font:");
			
			panel.add(labelFont, "span, wrap");
			panel.add(fontChangePanel, "pushx, alignx 50%, w 95%");
			
			setViewportView(panel);
		}
	}
	
	/**
	 * Create the panel for font settings.
	 */
	class URLSettings extends JScrollPane {

		private static final long serialVersionUID = 1L;

		public URLSettings() {
			
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
			
			setViewportView(panel);
		}
	}
	
	/**
	 * Subclass to create a panel that handles theme settings.
	 * @author CKeil
	 *
	 */
	class ThemeSettingsPanel extends JScrollPane {

		private static final long serialVersionUID = 1L;

		private JButton day;
		private JButton night;
		
		public ThemeSettingsPanel() {
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			panel.add(GUIParams.setupHeader("Select a theme:"), "span, wrap");
			
			// Choice #1
			day = GUIParams.setButtonLayout("Daylight", null);
			
			// Choice #2
			night = GUIParams.setButtonLayout("Night", null);
			
			day.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					ThemeSettingsPanel.this.updateCheck(true);
				}
			});
	
			night.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					ThemeSettingsPanel.this.updateCheck(false);
				}
			});
			
			panel.add(day);
			panel.add(night);
			
			setViewportView(panel);
		}
		
		/**
		 * Sets the theme according to which radio button is selected.
		 */
		public void updateCheck(boolean day) {

			if (day) {
				GUIParams.setDayLight();
				resetTheme();

			} else {
				GUIParams.setNight();
				resetTheme();
			}
		}
		
		public void resetTheme() {
			
			PreferencesMenu.this.setupLayout("Theme");
			
			if (tvFrame.getDataModel() != null 
					&& tvFrame.getRunning() != null) {
				if(tvFrame.getConfirmPanel() != null) {
					tvFrame.setView("LoadCheckView");
				}
				tvFrame.setLoaded(false);
				tvFrame.setView("DendroView");

			} else if (tvFrame.getDataModel() != null 
					&& tvFrame.getRunning() == null) {
				tvFrame.setView("LoadCheckView");

			} else {
				tvFrame.setView("WelcomeView");
			}
			
			menuFrame.dispose();
		}
	}
	
	/**
	 * Subclass for the Annotation settings panel.
	 * @author CKeil
	 *
	 */
	class AnnotationPanel extends JScrollPane {
		
		private static final long serialVersionUID = 1L;
		
		public AnnotationPanel() {
			
			super();
			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.setBackground(GUIParams.BG_COLOR);
			
			final HeaderSummaryPanel genePanel = new HeaderSummaryPanel(
					tvFrame.getDataModel().getGeneHeaderInfo(), 
					dendroView.getTextview().getHeaderSummary(), tvFrame);

			final HeaderSummaryPanel arrayPanel = new HeaderSummaryPanel(
					dendroView.getArraynameview().getHeaderInfo(), 
					dendroView.getArraynameview().getHeaderSummary(), tvFrame);

			final HeaderSummaryPanel atrPanel = new HeaderSummaryPanel(
					tvFrame.getDataModel().getAtrHeaderInfo(), 
					dendroView.getAtrview().getHeaderSummary(), tvFrame);

			final HeaderSummaryPanel gtrPanel = new HeaderSummaryPanel(
					tvFrame.getDataModel().getGtrHeaderInfo(), 
					dendroView.getGtrview().getHeaderSummary(), tvFrame);
			
			final JButton custom_button = GUIParams.setButtonLayout(
					"Use Custom Labels", null);
			custom_button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					File customFile;
					FileSet loadedSet = tvFrame.getDataModel().getFileSet();
					File file = new File(loadedSet.getDir() 
							+ loadedSet.getRoot() + loadedSet.getExt());
					
					try {
						customFile = tvFrame.selectFile();
						
						final String fileName = file.getAbsolutePath();
						final int dotIndex = fileName.indexOf(".");

						final int suffixLength = fileName.length() - dotIndex;

						final String fileType = file.getAbsolutePath()
								.substring(fileName.length() - suffixLength, 
										fileName.length());
						
						// Next: read file, return string arrays with new names
						// Then: update currently loaded model.
						
						menuFrame.dispose();
						
					} catch (LoadException e) {
						e.printStackTrace();
					}
				}
				
			});
			
			JLabel rows = GUIParams.setupHeader("Rows");
			JLabel cols = GUIParams.setupHeader("Columns");
			JLabel rTrees = GUIParams.setupHeader("Row Trees");
			JLabel cTrees = GUIParams.setupHeader("Column Trees");
			
			panel.add(rows, "span, wrap");
			panel.add(genePanel, "pushx, alignx 50%, w 95%, span, wrap");
			panel.add(custom_button, "pushx, alignx 50%, span, wrap");
			
			panel.add(cols, "span, wrap");
			panel.add(arrayPanel, "pushx, alignx 50%, w 95%, span, wrap");
			
			panel.add(rTrees, "span, wrap");
			panel.add(gtrPanel, "pushx, alignx 50%, w 95%, span, wrap");
			
			panel.add(cTrees, "span, wrap");
			panel.add(atrPanel, "pushx, alignx 50%, w 95%, span");
			
			setViewportView(panel);
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
			basisPanel.add(themeSettings, "w 79%, h 95%, wrap");
			
		} else if(title.equalsIgnoreCase("Annotations") 
				&& annotationSettings != null) {
			basisPanel.add(annotationSettings, "w 79%, h 95%, wrap");
			
		} else if(title.equalsIgnoreCase("Font") && fontSettings != null) {
			basisPanel.add(fontSettings, "w 79%, h 95%, wrap");
		
		} else if(title.equalsIgnoreCase("Color Settings") 
				&& pixelSettings != null) {
			basisPanel.add(pixelSettings, "w 79%, h 95%, wrap");
		
		} else if(title.equalsIgnoreCase("URL") && urlSettings != null) {
			basisPanel.add(pixelSettings, "w 79%, h 95%, wrap");
		
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
		
		menuFrame.validate();
		menuFrame.repaint();
	}
}
