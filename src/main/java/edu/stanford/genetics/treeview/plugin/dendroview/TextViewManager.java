/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MessagePanel;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;
import edu.stanford.genetics.treeview.ViewFrame;

/**
 * @author avsegal
 * 
 */
public class TextViewManager extends ModelView implements ConfigNodePersistent,
		FontSelectable, PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private boolean ignoreDividerChange = false;
	private UrlExtractor uExtractor;
	private Component root;
	private int numViews;
	private int numShown;
	private HeaderInfo hI;
	private Vector<ModelView> textViews;
	private Preferences configNode;
	private HeaderSummary headerSummary;
	private int dividerLocations[];

	/**
	 * Constructor
	 * 
	 * @param hI
	 * @param uExtractor
	 */
	public TextViewManager() {

		super();
//		this.hI = hI;
//		this.uExtractor = uExtractor;
//
//		// Find out what kind of file was loaded
//		final String srcFileType = model.getSource().toLowerCase();
//
//		root = null;
//		textViews = new Vector<ModelView>();
//
//		panel = new JPanel();
//		panel.setLayout(new MigLayout("ins 0"));
//
//		dividerLocations = new int[hI.getNumNames() - 1];
//		// firstNotShown = null;
//		numShown = 0;
//
//		// could set up headerSummary...
//		final int GIDIndex = hI.getIndex("GID");
//		if (GIDIndex == -1 && !srcFileType.endsWith(".cdt")) {
//			headerSummary.setIncluded(new int[] { 1 }); // changed from {1}???
//
//		} else if (GIDIndex == -1 && srcFileType.endsWith(".cdt")) {
//			headerSummary.setIncluded(new int[] { 1 });
//
//		} else {
//			headerSummary.setIncluded(new int[] { 2 });
//		}
//		headerSummary.addObserver(this);
//
//		makeTextViews(hI.getNumNames());
//
//		for (int i = 0; i < numViews - 1; i++) {
//
//			dividerLocations[i] = 50;
//		}
//
//		addTextViews(1);
//		loadDividerLocations();
//		setVisible(true);
	}
	
	public void generateView(final HeaderInfo hI, final UrlExtractor uExtractor,
			final DataModel model) {
		
		this.hI = hI;
		this.uExtractor = uExtractor;
		this.headerSummary = new HeaderSummary("TextViewManagerSummary");

		// Find out what kind of file was loaded
		final String srcFileType = model.getSource().toLowerCase();

		root = null;
		textViews = new Vector<ModelView>();

		panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0"));

		dividerLocations = new int[hI.getNumNames() - 1];
		// firstNotShown = null;
		numShown = 0;

		// could set up headerSummary...
		final int GIDIndex = hI.getIndex("GID");
		if (GIDIndex == -1 && !srcFileType.endsWith(".cdt")) {
			headerSummary.setIncluded(new int[] { 1 }); // changed from {1}???

		} else if (GIDIndex == -1 && srcFileType.endsWith(".cdt")) {
			headerSummary.setIncluded(new int[] { 1 });

		} else {
			headerSummary.setIncluded(new int[] { 2 });
		}
		headerSummary.addObserver(this);

		makeTextViews(hI.getNumNames());

		for (int i = 0; i < numViews - 1; i++) {

			dividerLocations[i] = 50;
		}

		addTextViews(1);
		loadDividerLocations();
		setVisible(true);
	}

	/**
	 * called when confignode or headerSummary is changed.
	 */
	private void loadSelection() {

		if (configNode == null) {
			return;
		}

		// Need to get the children of Selection node
		// final ConfigNode[] nodes = configRoot.fetch("Selection");
		final String[] childrenNodes = getRootChildrenNodes();
		// Check how many selection nodes there are in the childrenNodes
		int numNodes = 0;
		for (final String node : childrenNodes) {

			if (node.contains("Selection")) {
				numNodes++;
			}
		}

		int[] included;
		int addIndex = 0;
		if (childrenNodes.length > 0) {
			included = new int[numNodes];
			for (int i = 0; i < childrenNodes.length; i++) {

				if (childrenNodes[i].contains("Selection")) {

					final int headerNum = configNode.node(childrenNodes[i])
							.getInt("index", -1);
					if (hI.getNumNames() >= headerNum) {
						included[addIndex] = headerNum;

					} else {
						included[addIndex] = hI.getNumNames();
					}
					addIndex++;
				}
			}
			headerSummary.setIncluded(included);
		}
	}

	/**
	 * called when headers to be displayed are changed.
	 */
	private void saveSelection() {

		if (configNode == null) {
			return;
		}

		// First remove all selection nodes.
		final String[] childrenNodes = getRootChildrenNodes();
		for (int i = 0; i < childrenNodes.length; i++) {

			if (childrenNodes[i].contains("Selection")) {
				configNode.remove(childrenNodes[i]);
			}
		}

		// Now add new selection nodes
		for (int i = 0; i < headerSummary.getIncluded().length; i++) {

			// Create children here.
			final int indexT = headerSummary.getIncluded()[i];
			configNode.node("Selection" + i).putInt("index", indexT);
		}
	}

	@Override
	public void update(final Observable ob, final Object obj) {

		if (ob == headerSummary) {
			saveSelection();
			saveDividerLocations();
			// saveDividerLocationsToConfig();
			addTextViews(headerSummary.getIncluded().length);
			loadDividerLocations();
		}

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).update(ob, obj);
		}
	}

	@Override
	public void updateBuffer(final Graphics g) {

		paintAll(g);
	}

	public void updateBuffer(final Image buf) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).updateBuffer(buf);
		}
	}

	@Override
	public String viewName() {

		return "TextViewManager";
	}

	/**
	 * Need to override ModelView.setViewFrame to account for the textviews that
	 * are contained.
	 * 
	 */
	@Override
	public void setViewFrame(final ViewFrame m) {

		super.setViewFrame(m);
		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setViewFrame(m);
		}
	}

	@Override
	public void setStatusPanel(final MessagePanel s) {

		super.setStatusPanel(s);
		for (int i = 0; i < textViews.size(); i++)
			((TextView) textViews.get(i)).setStatusPanel(s);
	}

	private void makeTextViews(final int n) {

		numViews = n;
		for (int i = 0; i < n; i++) {

			textViews.add(new TextView(hI, uExtractor, i));
			((TextView) textViews.lastElement())
					.setHeaderSummary(headerSummary);
			headerSummary.addObserver(textViews.lastElement());
		}
	}

	private void addTextViews(final int n) {

		JSplitPane temp;
		numShown = n;

		if (n <= 0) {
			return;

		} else if (n == 1) {
			root = ((TextView) textViews.get(0)).getComponent();

		} else {
			root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			((JSplitPane) root).setDividerSize(2);
			((JSplitPane) root).setBorder(null);
			((JSplitPane) root).setRightComponent(((TextView) textViews
					.get(n - 1)).getComponent());
			((JSplitPane) root).setLeftComponent(((TextView) textViews
					.get(n - 2)).getComponent());
			root.addPropertyChangeListener(
					JSplitPane.DIVIDER_LOCATION_PROPERTY, this);

			for (int i = n - 3; i >= 0; i--) {

				temp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				temp.setLeftComponent(((TextView) textViews.get(i))
						.getComponent());
				temp.setRightComponent(root);
				temp.setDividerSize(2);
				temp.setBorder(null);
				temp.addPropertyChangeListener(
						JSplitPane.DIVIDER_LOCATION_PROPERTY, this);
				root = temp;
			}
		}

		panel.removeAll();
		panel.add(root, "push, grow");
		panel.updateUI();
	}

	/**
	 * Need to override TextView.setGeneSelection() to account for the textviews
	 * that are contained.
	 * 
	 */
	public void setGeneSelection(final TreeSelectionI selection) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setGeneSelection(selection);
		}
	}

	/**
	 * Need to override TextView.setGeneSelection() to account for the textviews
	 * that are contained.
	 * 
	 */
	public void setArraySelection(final TreeSelectionI selection) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setArraySelection(selection);
		}
	}

	public void setMap(final MapContainer zoomYMap) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setMap(zoomYMap);
		}
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("TextViewManager");

		} else {
			LogBuffer.println("Could not find or create TextViewManager"
					+ "node because parentNode was null.");
		}

		loadSelection(); // doesn't quite work yet,
		// something more global then this headerSummary needs to be updated.

		// Get all children nodes.
		// ConfigNode[] viewNodes = configRoot.fetch("TextView");
		String[] childrenNodes = getRootChildrenNodes();

		// Creates new children nodes if TextView nodes differ from
		// amount of TextViews.
		for (int i = childrenNodes.length; i < textViews.size(); i++) {
			// configRoot.create("TextView");
			configNode.node("TextView" + i);
		}

		childrenNodes = getRootChildrenNodes();
		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setConfigNode(configNode
					.node(childrenNodes[i]));
		}

		// binding config can change fonts.
		if (textViews.size() > 0) {
			setFont(((TextView) textViews.firstElement()).getFont());
		}

		loadDividerLocationsFromConfig();
		loadDividerLocations();
	}

	@Override
	public String getFace() {

		return getFont().getName();
	}

	@Override
	public int getPoints() {

		return getFont().getSize();
	}

	@Override
	public int getStyle() {

		return getFont().getStyle();
	}

	@Override
	public void setFace(final String string) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setFace(string);
		}

		if (textViews.size() > 0) {
			setFont(((TextView) textViews.firstElement()).getFont());
		}

		revalidate();
		repaint();
	}

	@Override
	public void setPoints(final int size) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setPoints(size);
		}

		if (textViews.size() > 0) {
			setFont(((TextView) textViews.firstElement()).getFont());
		}

		revalidate();
		repaint();
	}

	@Override
	public void setStyle(final int style) {

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setStyle(style);
		}

		if (textViews.size() > 0) {
			setFont(((TextView) textViews.firstElement()).getFont());
		}

		revalidate();
		repaint();
	}

	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;

		for (int i = 0; i < textViews.size(); i++) {
			((TextView) textViews.get(i)).setHeaderSummary(headerSummary);
		}
	}

	/** Getter for headerSummary */
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}

	public void saveDividerLocationsToConfig() {

		Preferences node = null;
		if (configNode != null) {
			// node = configRoot.fetchFirst("Dividers");
			node = configNode.node("Dividers");

			// if (node == null) {
			// node = configRoot.create("Dividers");
			// }
		} else {
			return;
		}

		for (int i = 0; i < numViews - 1; i++) {

			if (node != null) {
				node.putInt("Position" + i, dividerLocations[i]);
			}
		}
	}

	public void saveDividerLocations() {

		Component temp = panel.getComponent(0);

		for (int i = 0; i < numShown - 1; i++) {

			dividerLocations[i] = ((JSplitPane) temp).getDividerLocation();
			temp = ((JSplitPane) temp).getRightComponent();
		}
	}

	public void loadDividerLocationsFromConfig() {

		Preferences node = null;
		if (configNode != null) {
			// node = configRoot.fetchFirst("Dividers");
			node = configNode.node("Dividers");

		} else {
			return;
		}

		for (int i = 0; i < numViews - 1; i++) {

			if (node != null) {
				dividerLocations[i] = node.getInt("Position" + i, 50);
			}
		}
	}

	public void loadDividerLocations() {

		ignoreDividerChange = true;
		Component temp = panel.getComponent(0);

		for (int i = 0; i < numShown - 1; i++) {

			((JSplitPane) temp).setDividerLocation(dividerLocations[i]);
			temp = ((JSplitPane) temp).getRightComponent();
		}

		ignoreDividerChange = false;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent pce) {

		if (!ignoreDividerChange
				&& pce.getPropertyName().equalsIgnoreCase(
						JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
			saveDividerLocations();
			saveDividerLocationsToConfig();
		}
	}

	/**
	 * Returns the names of the current children of this class' root node.
	 * 
	 * @return
	 */
	public String[] getRootChildrenNodes() {

		if (configNode != null) {
			String[] childrenNodes;
			try {
				childrenNodes = configNode.childrenNames();
				return childrenNodes;

			} catch (final BackingStoreException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}
}
