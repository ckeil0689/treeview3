package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;
import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.HeaderSummary;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelView;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.UrlExtractor;

public class LabelView extends ModelView implements MouseListener, 
MouseMotionListener, FontSelectable, ConfigNodePersistent {

	protected final static int ROW = 0;
	protected final static int COL = 1;
	
	/* DataModel observes this view */
	protected DataModel dataModel;
	protected HeaderInfo headerInfo;
	protected HeaderSummary headerSummary;
	protected MapContainer map;
	protected UrlExtractor urlExtractor;
	
	protected final String d_face = "Dialog";
	protected final int d_style = 0;
	protected final int d_size = 12;
	protected final boolean d_justified = false;
	
	protected String face;
	protected int style;
	protected int size;
	protected int maxlength = 0;
	protected int hoverIndex;
	protected boolean isRightJustified;
	
	protected TreeSelectionI arraySelection;
	protected TreeSelectionI geneSelection;
	
	protected Preferences configNode;
	
	private final int axis_id;
	
	protected JScrollPane scrollPane;
	protected JLabel zoomHint;
	
	public LabelView(int axis_id) {
		
		super();
		
		this.axis_id = axis_id;
		this.setLayout(new MigLayout());
		
		String summary = (axis_id == ROW) ? "GeneSummary" : "ArraySummary";
		this.headerSummary = new HeaderSummary(summary);
		
//		this.urlExtractor = uExtractor;
		
		addMouseMotionListener(this);
		addMouseListener(this);

		zoomHint = GUIFactory.createLabel("", GUIFactory.FONTS);
		
		if(axis_id == ROW) {
			add(zoomHint, "alignx 0%, aligny 50%, push, wrap");
		} else {
			add(zoomHint, "alignx 50%, aligny 100%, push");
		}

		scrollPane = new JScrollPane(this,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);

		panel = scrollPane;
	}
	
	/**
	 * Used to space the array names.
	 * 
	 * @param im
	 *            A new mapcontainer.
	 */
	public void setMap(final MapContainer im) {

		if (map != null) map.deleteObserver(this);
		
		map = im;
		map.addObserver(this);

		revalidate();
		repaint();
	}
	
	public void setHeaderInfo(HeaderInfo headerInfo) {
		
		this.headerInfo = headerInfo;
	}
	
	public HeaderInfo getHeaderInfo() {

		return headerInfo;
	}
	
	public void setHeaderSummary(final HeaderSummary headerSummary) {

		this.headerSummary = headerSummary;
	}
	
	public HeaderSummary getHeaderSummary() {

		return headerSummary;
	}
	
	public void setUrlExtractor(UrlExtractor urlExtractor) {

		this.urlExtractor = urlExtractor;
	}
	
	public UrlExtractor getUrlExtractor() {

		return urlExtractor;
	}
	
	/**
	 * Set geneSelection
	 * 
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setGeneSelection(final TreeSelectionI geneSelection) {

		if (this.geneSelection != null) {
			this.geneSelection.deleteObserver(this);
		}

		this.geneSelection = geneSelection;
		this.geneSelection.addObserver(this);
	}
	
	/**
	 * Set geneSelection
	 * 
	 * @param geneSelection
	 *            The TreeSelection which is set by selecting genes in the
	 *            GlobalView
	 */
	public void setArraySelection(final TreeSelectionI arraySelection) {

		if (this.arraySelection != null) {
			this.arraySelection.deleteObserver(this);
		}
		this.arraySelection = arraySelection;
		this.arraySelection.addObserver(this);
	}

	@Override
	public void update(Observable o, Object arg) {
		

	}

	@Override
	public void setConfigNode(Preferences parentNode) {
		
		if (parentNode != null) {
			this.configNode = parentNode;
		} else {
			LogBuffer.println("parentNode for LabelView was null.");
			return;
		}
		
		setFace(configNode.get("face", d_face));
		setStyle(configNode.getInt("style", d_style));
		setPoints(configNode.getInt("size", d_size));
		setJustifyOption(configNode.getBoolean("isRightJustified", d_justified));

		getHeaderSummary().setConfigNode(configNode);
	}

	@Override
	public String getFace() {
		
		return face;
	}

	@Override
	public int getPoints() {
		
		return size;
	}

	@Override
	public int getStyle() {
		
		return style;
	}

	@Override
	public boolean getJustifyOption() {
		// TODO Auto-generated method stub
		return isRightJustified;
	}

	@Override
	public void setFace(String string) {
		
		if ((face == null) || (!face.equals(string))) {
			face = string;
			if (configNode != null) {
				configNode.put("face", face);
			}
			setFont(new Font(face, style, size));
//			backBufferValid = false;
			repaint();
		}
	}

	@Override
	public void setPoints(int i) {
		
		if (size != i) {
			size = i;
			if (configNode != null) {
				configNode.putInt("size", size);
			}
			setFont(new Font(face, style, size));
//			backBufferValid = false;
			repaint();
		}
	}

	@Override
	public void setStyle(int i) {
		
		if (style != i) {
			style = i;
//			backBufferValid = false;
			if (configNode != null) {
				configNode.putInt("style", style);
			}
			setFont(new Font(face, style, size));
			repaint();
		}
	}

	@Override
	public void setJustifyOption(boolean isRightJustified) {
		
		this.isRightJustified = isRightJustified;
		
		if (configNode != null) {
			configNode.putBoolean("isRightJustified", isRightJustified);
		}
	}

	@Override
	public String viewName() {
		
		return "LabelView";
	}
	

	@Override
	protected void updateBuffer(Graphics g) {
		// TODO Auto-generated method stub
		
	}
}
