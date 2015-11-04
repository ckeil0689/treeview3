package Cluster;

import edu.stanford.genetics.treeview.HeaderInfo;

/**
 * This class stores some useful data of a file that is about to be clustered.
 * Accumulating the data here allows for easy and central access 
 * @author chris0689
 *
 */
public class PreClusterFileInfo {

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
}
