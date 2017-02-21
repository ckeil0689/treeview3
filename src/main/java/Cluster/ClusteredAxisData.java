package Cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * A class used to organize and store relevant data for clustering an axis.
 * It can be passed and used throughout different phases of clustering and
 * makes it easier and cleaner to track important cluster information for 
 * an axis.
 *
 */
public class ClusteredAxisData {

	private final int AXIS_ID;
	private final String AXIS_BASEID;
	
	private String[] reorderedIDs = new String[0];
	private String[] axisLabelTypes = new String[0];
	private String[][] axisLabels = new String[0][];
	private List<String[]> treeNodeData = new ArrayList<String[]>();
	
	private int kmeans_clusterN = 0;
	
	private boolean isAxisClustered;
	private boolean shouldReorderAxis;
	
	public ClusteredAxisData(final int axisID) {
		
		this.AXIS_ID = axisID;
		this.AXIS_BASEID = (axisID == 0) ? "ROW" : "COL";
	}
	
	public void setKmeansClusterNum(final int clusterNum) {
		
		this.kmeans_clusterN = clusterNum;
	}
	
	/* Setters */
	public void setReorderedIDs(final String[] newReorderedIDs) {
		
		this.reorderedIDs = newReorderedIDs;
	}
	
  public void setTreeNodeData(final List<String[]> newTreeNodeData) {
		
		this.treeNodeData = newTreeNodeData;
	}
	
	public void setLabelTypes(final String[] newAxisLabelTypes) {
		
		this.axisLabelTypes = newAxisLabelTypes;
	}
	
	public void setLabels(final String[][] newAxisLabels) {
		
		this.axisLabels = newAxisLabels;
	}
	
	public void setAxisClustered(final boolean isAxisClustered) {
		
		this.isAxisClustered = isAxisClustered;
	}
	
	public void shouldReorderAxis(final boolean shouldReorderAxis) {
		
		this.shouldReorderAxis = shouldReorderAxis;
	}
	
	/* Getters */
	public int getKmeansClusterNum() {
		
		return kmeans_clusterN;
	}
	
	public int getAxisID() {
		
		return AXIS_ID;
	}
	
	public String getAxisBaseID() {
		
		return AXIS_BASEID;
	}
	
	public String[] getReorderedIDs() {
		
		return reorderedIDs;
	}
	
	public List<String[]> getTreeNodeData() {
		
		return treeNodeData;
	}
	
	public String[] getAxisLabelTypes() {
		
		return axisLabelTypes;
	}
	
	public String[][] getAxisLabels() {
		
		return axisLabels;
	}
	
	public int getNumLabels() {
		
		return axisLabels.length;
	}
	
	public boolean isAxisClustered() {
		
		return isAxisClustered;
	}
	
	public boolean shouldReorderAxis() {
		
		return shouldReorderAxis;
	}
}
