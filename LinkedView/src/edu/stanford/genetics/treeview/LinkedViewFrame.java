///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
// *
// * END_HEADER 
// */
//package edu.stanford.genetics.treeview;
//
//import java.io.File;
//import java.util.Observer;
//
//import javax.swing.BorderFactory;
//import javax.swing.Box;
//import javax.swing.BoxLayout;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JFileChooser;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//
//import edu.stanford.genetics.treeview.app.LinkedViewApp;
//import edu.stanford.genetics.treeview.core.PluginManager;
//import edu.stanford.genetics.treeview.model.KnnModel;
//
///**
// * This class implements the GUI portion of the LinkedView application
// * 
// */
//public class LinkedViewFrame extends TreeViewFrame implements Observer {
//
//	private static final long serialVersionUID = 1L;
//
//	private static String appName = "TreeView 3.0";
//
//	public LinkedViewFrame(final LinkedViewApp treeview) {
//
//		super(treeview, appName);
//	}
//
//	public LinkedViewFrame(final TreeViewApp treeview, final String subName) {
//
//		// sorry this is so ugly, but can't call getAppName until
//		// superclass constructor's done.
//		super(treeview, subName);
//	}
//
//	@Override
//	public String getAppName() {
//
//		return appName;
//	}
//
//	private String getStyle(final FileSet fileSet) {
//
//		if (fileSet.getStyle() == FileSet.AUTO_STYLE) {
//			return "auto";
//		}
//
//		if (fileSet.getStyle() == FileSet.CLASSIC_STYLE) {
//			return "classic";
//		}
//
//		if (fileSet.getStyle() == FileSet.KMEANS_STYLE) {
//			return "kmeans";
//		}
//
//		if (fileSet.getStyle() == FileSet.LINKED_STYLE) {
//			return "linked";
//		}
//
//		return "unknown";
//	}
//
//	/**
//	 * r * This is the workhorse. It creates a new DataModel of the file, and
//	 * then sets the Datamodel. A side effect of setting the datamodel is to
//	 * update the running window.
//	 */
//	@Override
//	public void loadFileSet(final FileSet fileSet) throws LoadException {
//
//		LogBuffer.println("initial style " + getStyle(fileSet));
//
//		if (fileSet.getStyle() == FileSet.AUTO_STYLE) {
//
//			if (fileSet.getKag().equals("") && fileSet.getKgg().equals("")) {
//				super.loadFileSet(fileSet); // loads into TVModel.
//
//			} else {
//				loadKnnModel(fileSet);
//			}
//		} else {
//			if (fileSet.getStyle() == FileSet.KMEANS_STYLE) {
//				loadKnnModel(fileSet);
//			} else {
//				super.loadFileSet(fileSet);
//			}
//		}
//	}
//
//	private void loadKnnModel(final FileSet fileSet) throws LoadException {
//
//		final KnnModel knnModel = new KnnModel();
//		knnModel.setFrame(this);
//
//		try {
//			knnModel.loadNew(fileSet);
//			fileSet.setStyle(FileSet.KMEANS_STYLE);
//			setDataModel(knnModel);//, false, true);
//		} catch (final LoadException e) {
//			JOptionPane.showMessageDialog(this, e);
//			throw e;
//		}
//	}
//
//	@Override
//	protected void setupRunning() {
//
//		FileSet fileSet = getDataModel().getFileSet();
//		LogBuffer
//				.println("FileSet Type LinkedViewFrame: " + fileSet.getStyle()); // Auto
//
//		if (fileSet.getStyle() == FileSet.AUTO_STYLE) {
//			if (getDataModel().getDocumentConfigRoot().fetchFirst("Views") // Problem
//			!= null) {
//				fileSet.setStyle(FileSet.LINKED_STYLE);
//
//			} else {
//				final HeaderInfo geneHeaders = getDataModel()
//						.getGeneHeaderInfo();
//				final HeaderInfo arrayHeaders = getDataModel()
//						.getArrayHeaderInfo();
//
//				if ((geneHeaders.getNumNames() > 4)
//						|| (arrayHeaders.getNumNames() > 3)) {
//					fileSet.setStyle(FileSet.LINKED_STYLE);
//
//				} else {
//					// fileSet.setStyle(FileSet.CLASSIC_STYLE);
//					fileSet.setStyle(FileSet.LINKED_STYLE);
//
//				}
//			}
//		} else {
//			// default to linked
//			fileSet = new FileSet(null, null);
//			fileSet.setStyle(FileSet.LINKED_STYLE);
//		}
//
//		if (fileSet.getStyle() == FileSet.LINKED_STYLE) {
//			final LinkedPanel linkedPanel = new LinkedPanel(this);
//			linkedPanel.addChangeListener(new ChangeListener() {
//
//				@Override
//				public void stateChanged(final ChangeEvent e) {
//
//					// rebulid menus...?
//					// menuBar.rebuildMainPanel();
//					rebuildMainPanelMenu();
//				}
//			});
//			final ConfigNode documentConfig = getDataModel()
//					.getDocumentConfigRoot();
//			linkedPanel.setConfigNode(documentConfig.fetchOrCreate("Views"));
//			running = linkedPanel;
//
//		} else if (fileSet.getStyle() == FileSet.KMEANS_STYLE) {
//			// make sure selection objects are set up before instantiating
//			// plugins
//			final PluginFactory[] plugins = PluginManager.getPluginManager()
//					.getPluginFactories();
//			for (int j = 0; j < plugins.length; j++) {
//				if ("KnnDendrogram".equals(plugins[j].getPluginName())) {
//					running = plugins[j].restorePlugin(null, this);
//					break;
//				}
//			}
//
//		} else {
//			// make sure selection objects are set up before instantiating
//			// plugins
//			final PluginFactory[] plugins = PluginManager.getPluginManager()
//					.getPluginFactories();
//
//			for (int j = 0; j < plugins.length; j++) {
//
//				if ("Dendrogram".equals(plugins[j].getPluginName())) {
//					running = plugins[j].restorePlugin(null, this);
//					break;
//				}
//			}
//		}
//		LogBuffer.println("final style " + getStyle(fileSet));
//	}
//
//	/**
//	 * This class implements controls for file opening options. It is factored
//	 * into a separate class because it is used by both the offerSelection() and
//	 * offerUrlSelection dialogs.
//	 * 
//	 * @author aloksaldanha
//	 * 
//	 */
//	private class FileOptionsPanel extends Box {
//
//		private static final long serialVersionUID = 1L;
//
//		private final JComboBox dataList;
//		private final JCheckBox quoteBox;
//
//		public FileOptionsPanel() {
//
//			super(BoxLayout.Y_AXIS);
//			dataList = new JComboBox(FileSet.getStyles());
//			dataList.setEditable(false);
//
//			final JPanel stylePanel = new JPanel();
//			final JLabel style = new JLabel("Style:");
//			stylePanel.add(style);
//			stylePanel.add(dataList);
//
//			final JPanel quotePanel = new JPanel();
//			quoteBox = new JCheckBox("Parse quoted strings");
//			quotePanel.add(quoteBox);
//
//			// values from last time...
//			quoteBox.setSelected(fileMru.getParseQuotedStrings());
//			dataList.setSelectedIndex(fileMru.getStyle());
//
//			this.add(stylePanel);
//			this.add(quotePanel);
//			this.add(Box.createGlue());
//
//			try {
//				setBorder(BorderFactory.createTitledBorder("Options"));
//
//			} catch (final Exception e) {
//				LogBuffer.println("Could not create border in "
//						+ "LinkedViewFrame.offerSelection");
//			}
//		}
//
//		public int getSelectedStyleIndex() {
//
//			fileMru.setStyle(dataList.getSelectedIndex());
//			return dataList.getSelectedIndex();
//		}
//
//		public boolean isQuoteSelected() {
//
//			fileMru.setParseQuotedStrings(quoteBox.isSelected());
//			return quoteBox.isSelected();
//		}
//	}
//
//	/**
//	 * Open a dialog which allows the user to select a new data file
//	 * 
//	 * @return The fileset corresponding to the dataset.
//	 */
//	protected FileSet offerSelection() throws LoadException {
//
//		FileSet fileSet1; // will be chosen...
//		final JFileChooser fileDialog = new JFileChooser();
//		setupFileDialog(fileDialog);
//
//		final FileOptionsPanel boxPanel = new FileOptionsPanel();
//		fileDialog.setAccessory(boxPanel);
//
//		final int retVal = fileDialog.showOpenDialog(this);
//		if (retVal == JFileChooser.APPROVE_OPTION) {
//			final File chosen = fileDialog.getSelectedFile();
//			fileSet1 = new FileSet(chosen.getName(), chosen.getParent()
//					+ File.separator);
//
//		} else {
//			throw new LoadException("File Dialog closed without selection...",
//					LoadException.NOFILE);
//		}
//
//		fileSet1.setStyle(boxPanel.getSelectedStyleIndex());
//		fileSet1.setParseQuotedStrings(boxPanel.isQuoteSelected());
//
//		return fileSet1;
//	}
//
//	@Override
//	protected FileSet offerUrlSelection() throws LoadException {
//
//		FileSet fileSet1;
//		// get string from user...
//		final FileOptionsPanel boxPanel = new FileOptionsPanel();
//
//		final Box panel = new Box(BoxLayout.Y_AXIS);
//		panel.add(boxPanel);
//		panel.add(new JLabel("Enter a Url:"));
//
//		final String urlString = JOptionPane.showInputDialog(this, panel);
//
//		if (urlString != null) {
//			// must parse out name, parent + sep...
//			final int postfix = urlString.lastIndexOf("/") + 1;
//			final String name = urlString.substring(postfix);
//			final String parent = urlString.substring(0, postfix);
//			fileSet1 = new FileSet(name, parent);
//		} else {
//			throw new LoadException("Input Dialog closed "
//					+ "without selection...", LoadException.NOFILE);
//		}
//
//		fileSet1.setStyle(boxPanel.getSelectedStyleIndex());
//		fileSet1.setParseQuotedStrings(boxPanel.isQuoteSelected());
//
//		return fileSet1;
//	}
//}
