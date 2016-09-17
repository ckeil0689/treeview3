package Cluster;

import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.LabelInfo;

/**
 * This class stores some useful data of a file that is about to be clustered. 
 * Examples of this are preferences settings such as color information and 
 * label view settings.
 * Accumulating the data here allows for easy and central access 
 * @author chris0689
 *
 */
public class PreClusterFileInfo {

	private Preferences colorNode;
	private Preferences labelNode;
	
	private LabelInfo rowLabelInfo;
	private LabelInfo colLabelInfo;
	
	public PreClusterFileInfo() {
		
		
	}
	
	public void setLabelInfo(final LabelInfo rowLabelInfo, 
			final LabelInfo colLabelInfo) {
		
		this.rowLabelInfo = rowLabelInfo;
		this.colLabelInfo = colLabelInfo;
	}
	
	public LabelInfo getRowLabelInfo() {
		
		return rowLabelInfo;
	}
	
	public LabelInfo getColLabelInfo() {
		
		return colLabelInfo;
	}
	
	/**
	 * Establish a reference for a color settings node so it can be retrieved
	 * later.
	 * @param colorNode The Preferences node containing color settings.
	 */
	public void saveColorSettings(Preferences colorNode) {
		
		this.colorNode = colorNode;
	}
	
	/**
	 * Establish a reference for a label settings node so it can be retrieved
	 * later.
	 * @param labelNode The Preferences node containing label settings.
	 */
	public void saveLabelSettings(Preferences labelNode) {
		
		this.labelNode = labelNode;
	}
}
