package cluster;

import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.HeaderInfo;

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
	
	private HeaderInfo rowHeaderInfo;
	private HeaderInfo colHeaderInfo;
	
	public PreClusterFileInfo() {
		
		
	}
	
	public void setHeaderInfo(final HeaderInfo rowHeaderInfo, 
			final HeaderInfo colHeaderInfo) {
		
		this.rowHeaderInfo = rowHeaderInfo;
		this.colHeaderInfo = colHeaderInfo;
	}
	
	public HeaderInfo getRowHeaderInfo() {
		
		return rowHeaderInfo;
	}
	
	public HeaderInfo getColHeaderInfo() {
		
		return colHeaderInfo;
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
