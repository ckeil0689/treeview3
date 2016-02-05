package Cluster;

/**
 * A class used to organize and store relevant data for clustering an axis.
 * It can be passed and used throughout different phases of clustering and
 * makes it easier and cleaner to track important cluster information for 
 * an axis.
 * @author chris0689
 *
 */
public class ClusteredAxisData {

	private final int AXIS_ID;
	private final String AXIS_BASEID;
	
	private String[] reorderedIDs;
	private String[] axisHeaders;
	private String[][] axisLabels;
	private String[][] orderedAxisLabels;
	
	private boolean isAxisClustered;
	private boolean shouldReorderAxis;
	
	public ClusteredAxisData(final int axisID) {
		
		this.AXIS_ID = axisID;
		this.AXIS_BASEID = (axisID == 0) ? "ROW" : "COL";
		this.shouldReorderAxis = false;
		this.isAxisClustered = false;
		this.reorderedIDs = new String[] {};
	}
	
	/* Setters */
	public void setReorderedIDs(final String[] newReorderedIDs) {
		
		this.reorderedIDs = newReorderedIDs;
	}
	
	public void setAxisHeaders(final String[] newAxisHeaders) {
		
		this.axisHeaders = newAxisHeaders;
	}
	
	public void setAxisLabels(final String[][] newAxisLabels) {
		
		this.axisLabels = newAxisLabels;
	}
	
	public void setOrderedAxisLabels(final String[][] newOrderedAxisLabels) {
		
		this.orderedAxisLabels = newOrderedAxisLabels;
	}
	
	public void setAxisClustered(final boolean isAxisClustered) {
		
		this.isAxisClustered = isAxisClustered;
	}
	
	public void shouldReorderAxis(final boolean shouldReorderAxis) {
		
		this.shouldReorderAxis = shouldReorderAxis;
	}
	
	/* Getters */
	public int getAxisID() {
		
		return AXIS_ID;
	}
	
	public String getAxisBaseID() {
		
		return AXIS_BASEID;
	}
	
	public String[] getReorderedIDs() {
		
		return reorderedIDs;
	}
	
	public String[] getAxisHeaders() {
		
		return axisHeaders;
	}
	
	public String[][] getAxisLabels() {
		
		return axisLabels;
	}
	
	public int getAxisLabelNum() {
		
		return axisLabels.length;
	}
	
	public String[][] getOrderedAxisLabels() {
		
		return orderedAxisLabels;
	}
	
	public boolean isAxisClustered() {
		
		return isAxisClustered;
	}
	
	public boolean shouldReorderAxis() {
		
		return shouldReorderAxis;
	}
}
