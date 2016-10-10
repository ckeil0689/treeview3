package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Font;
import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.ModelLoadReset;

/** A container which is explicitly responsible for the management of all
 * defined label attributes. */
public class LabelAttributes implements ConfigNodePersistent, ModelLoadReset,
	FontSelectable {

	protected Preferences configNode;
	protected final LabelView labelView;

	// Default label settings
	protected final String d_face = "Courier";
	protected final int d_style = 0;
	protected final int d_size = 14;
	protected final int d_minSize = 11;
	protected final int d_maxSize = 30;
	protected final boolean d_fixed = false;
	protected boolean d_justified = false; // may be changed by LabelView children

	// Custom label settings
	private String face;
	private int style;
	private int size;

	private String lastDrawnFace; // used only by getMaxStringLength
	private int lastDrawnStyle; // used only by getMaxStringLength
	private int lastDrawnSize; // used only by getMaxStringLength

	private int minSize;
	private int maxSize;
	private int lastSize;
	private boolean isFixed;

	// Important to maintain scrolling behavior
	private int longest_str_index;
	private int longest_str_length;
	private String longest_str;

	// Alignment status
	private boolean isRightJustified;

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

		if(configNode == null) {
			LogBuffer.println("Could not synchronize state for " +	this																			.getClass()
																																	.getName() +
												" because configNode was null.");
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

		if(configNode == null) {
			LogBuffer.println("Could not store state for " +	this																.getClass()
																														.getName() +
												" because configNode was null.");
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
			LogBuffer.println("Could not import node for " +	this																.getClass()
																														.getName() +
												". Node not defined.");
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

		if(parentNode == null) {
			LogBuffer.println("parentNode for " +	this.getClass().getName() +
												" was null.");
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
		return "LabelAttributes [configNode=" +	configNode + ", d_face=" + d_face +
						", d_style=" + d_style + ", d_size=" + d_size + ", d_minSize=" +
						d_minSize + ", d_maxSize=" + d_maxSize + ", d_justified=" +
						d_justified + ", d_fixed=" + d_fixed + ", face=" + face +
						", style=" + style + ", size=" + size + ", lastDrawnFace=" +
						lastDrawnFace + ", lastDrawnStyle=" + lastDrawnStyle +
						", lastDrawnSize=" + lastDrawnSize + ", minSize=" + minSize +
						", maxSize=" + maxSize + ", lastSize=" + lastSize + ", isFixed=" +
						isFixed + ", longest_str_index=" + longest_str_index +
						", longest_str_length=" + longest_str_length + ", longest_str=" +
						longest_str + ", isRightJustified=" + isRightJustified + "]";
	}

	/** Saves the current state to detect changes later.
	 * 
	 * @param maxStrLen - Length of the longest string in the labels. */
	public void saveLastDrawnFontDetails(int maxStrLen) {

		this.lastDrawnFace = face;
		this.lastDrawnStyle = style;
		this.lastDrawnSize = size;
		this.longest_str_length = maxStrLen;
	}

	/** Updates the font, secondary scrollbar of the LabelView and initiates a
	 * repaint. */
	private void updateLabelView() {

		labelView.setFont(new Font(face, style, size));
		labelView.resetSecondaryScroll();
		labelView.repaint();
	}

	@Override
	public String getFace() {
		return face;
	}

	@Override
	public void setFace(String face) {
		this.face = face;
		storeState();
		updateLabelView();
	}

	@Override
	public int getStyle() {
		return style;
	}

	@Override
	public void setStyle(int style) {
		this.style = style;
		storeState();
		updateLabelView();
	}

	@Override
	public int getPoints() {
		return size;
	}

	@Override
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

	@Override
	public int getMinSize() {
		return minSize;
	}

	@Override
	public void setMinSize(int minSize) {

		// Remain within bounds
		if(minSize < 1 || maxSize > 0 && minSize > maxSize) { return; }

		this.minSize = minSize;
		storeState();
		updateLabelView();
	}

	@Override
	public int getMaxSize() {
		return maxSize;
	}

	@Override
	public void setMaxSize(int maxSize) {

		// Remain within bounds
		if(maxSize < 1 || minSize > 0 && maxSize < minSize) { return; }

		this.maxSize = maxSize;
		storeState();
		updateLabelView();
	}

	@Override
	public int getLastSize() {
		return lastSize;
	}

	@Override
	public void setLastSize(int lastSize) {
		this.lastSize = lastSize;
		setPoints(lastSize);
	}

	@Override
	public boolean isFixed() {
		return isFixed;
	}

	@Override
	public void setFixed(boolean isFixed) {
		this.isFixed = isFixed;
		storeState();
		updateLabelView();
	}

	public int getLongestStrIdx() {
		return longest_str_index;
	}

	public void setLongestStrIdx(int longest_str_index) {
		this.longest_str_index = longest_str_index;
	}

	public int getLongestStrLen() {
		return longest_str_length;
	}

	public void setLongestStrLen(int longest_str_length) {
		this.longest_str_length = longest_str_length;
	}

	public String getLongestStr() {
		return longest_str;
	}

	public void setLongestStr(String longest_str) {
		this.longest_str = longest_str;
	}

	@Override
	public boolean isRightJustified() {
		return isRightJustified;
	}

	@Override
	public void setRightJustified(boolean isRightJustified) {
		this.isRightJustified = isRightJustified;
		storeState();
		updateLabelView();
	}

	/** Necessary default justification change depending on LabelView axis. Should
	 * ONLY be called in child class of
	 * LabelView (RowLabelView, ColumnLabelView).
	 * 
	 * @param isRightJustified */
	public void setDefaultJustified(boolean isRightJustified) {

		this.d_justified = isRightJustified;
	}

	@Override
	public Font getFont() {
		return new Font(face, style, size);
	}

	/** @param tempStyle - A temporary font style.
	 * @return A font with only the style attribute temporarily changed (not
	 *         stored in state). */
	public Font getTempStyleFont(int tempStyle) {
		return new Font(face, tempStyle, size);
	}

	/** @param tempSize - A temporary font size.
	 * @return A font with only the size attribute temporarily changed (not stored
	 *         in state). */
	public Font getTempSizeFont(int tempSize) {
		return new Font(face, style, tempSize);
	}

	/** Tests if the currently stored font attributes (face, style, size) have
	 * changed compared to when they were last
	 * stored by saveLastDrawnFontDetails().
	 * 
	 * @return True if the font attributes have changed, false otherwise. */
	public boolean hasFontChanged() {
		return(lastDrawnFace != face ||	lastDrawnStyle != style ||
						lastDrawnSize != size);
	}

	public boolean isLongestStrIdxDefined() {
		return longest_str_index > -1;
	}

	public boolean isLongestStrEqualTo(String otherStr) {
		return longest_str.equals(otherStr);
	}
}
