package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Font;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelLoadReset;

/**
 * A container which is explicitly responsible for the management of all defined label attributes. 
 * @author ckeil0689
 *
 */
public class LabelAttributes implements ConfigNodePersistent, ModelLoadReset {

	protected Preferences configNode;
	protected final LabelView labelView;
	
	// Default label settings
	protected final String d_face = "Courier";
	protected final int d_style = 0;
	protected final int d_size = 14;
	protected final int d_minSize = 11;
	protected final int d_maxSize = 30;
	protected final boolean d_justified = false;
	protected final boolean d_fixed = false;

	// Custom label settings
	protected String face;
	protected int style;
	protected int size;
	
	protected String lastDrawnFace; // used only by getMaxStringLength
	protected int lastDrawnStyle; // used only by getMaxStringLength
	protected int lastDrawnSize; // used only by getMaxStringLength
	
	protected int minSize;
	protected int maxSize;
	protected int lastSize;
	protected boolean isFixed;
	
	// Important to maintain scrolling behavior
	protected int longest_str_index;
	protected int longest_str_length;
	protected String longest_str;

	// Alignment status
	protected boolean isRightJustified;
	
	public LabelAttributes(final LabelView labelView) {

		this.labelView = labelView;
		resetDefaults();
	}

	@Override
	public Preferences getConfigNode() {
		
		return configNode;
	}

	@Override
	public void requestStoredState() {
		
		if (configNode == null) {
			LogBuffer.println("Could not synchronize state for " + this.getClass().getName() 
					+ " because configNode was null.");
			return;
		}

		setMinSize(configNode.getInt("min", d_minSize));
		setMaxSize(configNode.getInt("max", d_maxSize));
		setFace(configNode.get("face", d_face));
		setStyle(configNode.getInt("style", d_style));
		setLastSize(configNode.getInt("size", d_size));
		setRightJustified(configNode.getBoolean("isRightJustified", d_justified));
		setFixed(configNode.getBoolean("isFixed", d_fixed));
	}

	@Override
	public void storeState() {
		
		if (configNode == null) {
			LogBuffer.println("Could not store state for " + this.getClass().getName() 
					+ " because configNode was null.");
			return;
		}

		configNode.putInt("min", getMinSize());
		configNode.putInt("max", getMaxSize());
		configNode.put("face", face);
		configNode.putInt("style", style);
		configNode.putInt("size", size);
		configNode.putBoolean("isRightJustified", d_justified);
		configNode.putBoolean("isFixed", d_fixed);
	}

	@Override
	public void importStateFrom(Preferences oldNode) {
		
		if(oldNode == null) {
			LogBuffer.println("Could not import node for " + this.getClass().getName() + ". Node not defined.");
			return;
		}
		
		setMinSize(oldNode.getInt("min", d_minSize));
		setMaxSize(oldNode.getInt("max", d_maxSize));
		setFace(oldNode.get("face", d_face));
		setStyle(oldNode.getInt("style", d_style));
		setLastSize(oldNode.getInt("size", d_size));
		setRightJustified(oldNode.getBoolean("isRightJustified", d_justified));
		setFixed(oldNode.getBoolean("isFixed", d_fixed));
	}

	@Override
	public void setConfigNode(Preferences parentNode) {
		
		if (parentNode == null) {
			LogBuffer.println("parentNode for " + this.getClass().getName() + " was null.");
			return;
		}

		this.configNode = parentNode.node(this.getClass().getName());
		requestStoredState();
	}

	@Override
	public void resetDefaults() {
		
		this.face = d_face;
		this.style = d_style;
		this.size = d_size;
		this.minSize = d_minSize;
		this.maxSize = d_maxSize;
		this.isRightJustified = d_justified;
		this.isFixed = d_fixed;

		this.longest_str_index = -1;
		this.longest_str_length = -1;
	}

	@Override
	public String toString() {
		return "LabelAttributes [configNode=" + configNode + ", d_face=" + d_face + ", d_style=" + d_style + ", d_size="
				+ d_size + ", d_minSize=" + d_minSize + ", d_maxSize=" + d_maxSize + ", d_justified=" + d_justified
				+ ", d_fixed=" + d_fixed + ", face=" + face + ", style=" + style + ", size=" + size + ", lastDrawnFace="
				+ lastDrawnFace + ", lastDrawnStyle=" + lastDrawnStyle + ", lastDrawnSize=" + lastDrawnSize
				+ ", minSize=" + minSize + ", maxSize=" + maxSize + ", lastSize=" + lastSize + ", isFixed=" + isFixed
				+ ", longest_str_index=" + longest_str_index + ", longest_str_length=" + longest_str_length
				+ ", longest_str=" + longest_str + ", isRightJustified=" + isRightJustified + "]";
	}
	
	public void saveLastDrawnFontDetails(int maxStrLen) {
		
		// Save the state to detect changes upon the next call of this method
		this.lastDrawnFace = face;
		this.lastDrawnStyle = style;
		this.lastDrawnSize = size;
		this.longest_str_length = maxStrLen;
	}
	
	/**
	 * Updates the font, secondary scrollbar of the LabelView and initiates a repaint.
	 */
	private void updateLabelView() {
		
		labelView.setFont(new Font(face, style, size));
		labelView.resetSecondaryScroll();
		labelView.repaint();
	}

	public String getFace() {
		return face;
	}

	public void setFace(String face) {
		this.face = face;
		storeState();
		updateLabelView();
	}

	public int getStyle() {
		return style;
	}

	public void setStyle(int style) {
		this.style = style;
		storeState();
		updateLabelView();
	}

	public int getSize() {
		return size;
	}

	public void setPoints(int size) {
		this.size = size;
		storeState();
		updateLabelView();
	}

	public String getLastDrawnFace() {
		return lastDrawnFace;
	}

	public void setLastDrawnFace(String lastDrawnFace) {
		this.lastDrawnFace = lastDrawnFace;
	}

	public int getLastDrawnStyle() {
		return lastDrawnStyle;
	}

	public void setLastDrawnStyle(int lastDrawnStyle) {
		this.lastDrawnStyle = lastDrawnStyle;
	}

	public int getLastDrawnSize() {
		return lastDrawnSize;
	}

	public void setLastDrawnSize(int lastDrawnSize) {
		this.lastDrawnSize = lastDrawnSize;
	}

	public int getMinSize() {
		return minSize;
	}

	public void setMinSize(int minSize) {
		
		// Remain within bounds
		if (minSize < 1 || maxSize > 0 && minSize > maxSize) {
			return;
		}
		
		this.minSize = minSize;
		storeState();
		updateLabelView();
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		
		// Remain within bounds
		if (maxSize < 1 || minSize > 0 && maxSize < minSize) {
			return;
		}
		
		this.maxSize = maxSize;
		storeState();
		updateLabelView();
	}

	public int getLastSize() {
		return lastSize;
	}

	public void setLastSize(int lastSize) {
		this.lastSize = lastSize;
		setPoints(lastSize);
	}

	public boolean isFixed() {
		return isFixed;
	}

	public void setFixed(boolean isFixed) {
		this.isFixed = isFixed;
		storeState();
		updateLabelView();
	}

	public int getLongest_str_index() {
		return longest_str_index;
	}

	public void setLongest_str_index(int longest_str_index) {
		this.longest_str_index = longest_str_index;
	}

	public int getLongest_str_length() {
		return longest_str_length;
	}

	public void setLongest_str_length(int longest_str_length) {
		this.longest_str_length = longest_str_length;
	}

	public String getLongest_str() {
		return longest_str;
	}

	public void setLongest_str(String longest_str) {
		this.longest_str = longest_str;
	}

	public boolean isRightJustified() {
		return isRightJustified;
	}

	public void setRightJustified(boolean isRightJustified) {
		this.isRightJustified = isRightJustified;
		storeState();
		updateLabelView();
	}
}
