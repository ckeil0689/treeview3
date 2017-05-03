package edu.stanford.genetics.treeview;

import org.freehep.graphicsio.PageConstants;

import Controllers.ExportHandler;
import Controllers.FormatType;
import Controllers.LabelExportOption;
import Controllers.RegionType;
import Controllers.TreeExportOption;

/**
 * This class holds and manages selected export options.
 *
 */
public class ExportOptions {

	private FormatType formatType;
	private PaperType paperType;
	private RegionType regionType;
	private AspectType aspectType;
	private String orientation;
	private boolean showSelections;
	private LabelExportOption rowLabelOption;
	private LabelExportOption colLabelOption;
	private TreeExportOption rowTreeOption;
	private TreeExportOption colTreeOption;
	
	public ExportOptions() {
		
		this.formatType = FormatType.getDefault();
		this.paperType = PaperType.getDefault();
		this.regionType = RegionType.getDefault();
		this.aspectType = AspectType.getDefault();
		this.orientation = ExportHandler.getPageOrientation();
		this.showSelections = false;
		this.rowLabelOption = LabelExportOption.getDefault();
		this.colLabelOption = LabelExportOption.getDefault();
		//This currently never changes
		this.rowTreeOption = TreeExportOption.getDefault();
		this.colTreeOption = TreeExportOption.getDefault();
	}
	
	public FormatType getFormatType() {
		return formatType;
	}
	
	public String getOrientation() {
		return orientation;
	}

	public PaperType getPaperType() {
		return paperType;
	}
	
	public RegionType getRegionType() {
		return regionType;
	}
	
	public AspectType getAspectType() {
		return aspectType;
	}
	
	public boolean isShowSelections() {
		return showSelections;
	}
	
	public void setOrientation(String orientation) {
		
		/* Make sure no weird String can be set */
		if(!orientation.equalsIgnoreCase(PageConstants.LANDSCAPE) 
				&& !orientation.equalsIgnoreCase(PageConstants.PORTRAIT)) {
			this.orientation = PageConstants.PORTRAIT;
			return;
		}
		
		this.orientation = orientation;
	}
	
	public void setFormatType(FormatType formatType) {
		this.formatType = formatType;
	}

	public void setPaperType(PaperType paperType) {
		this.paperType = paperType;
	}

	public void setRegionType(RegionType regionType) {
		this.regionType = regionType;
	}

	public void setAspectType(AspectType aspectType) {
		this.aspectType = aspectType;
	}

	public void setShowSelections(boolean showSelections) {
		this.showSelections = showSelections;
	}

	/**
	 * Getter for rowLabelOption
	 * @return the rowLabelOption
	 */
	public LabelExportOption getRowLabelOption() {
		return(rowLabelOption);
	}

	/**
	 * 
	 * @param rowLabelOption the rowLabelOption to set
	 */
	public void setRowLabelOption(LabelExportOption rowLabelOption) {
		this.rowLabelOption = rowLabelOption;
	}

	/**
	 * 
	 * @return the colLabelOption
	 */
	public LabelExportOption getColLabelOption() {
		return(colLabelOption);
	}

	/**
	 * 
	 * @param colLabelOption the colLabelOption to set
	 */
	public void setColLabelOption(LabelExportOption colLabelOption) {
		this.colLabelOption = colLabelOption;
	}

	public TreeExportOption getRowTreeOption() {
		return rowTreeOption;
	}

	public void setRowTreeOption(TreeExportOption rowTreeOption) {
		this.rowTreeOption = rowTreeOption;
	}

	public TreeExportOption getColTreeOption() {
		return colTreeOption;
	}

	public void setColTreeOption(TreeExportOption colTreeOption) {
		this.colTreeOption = colTreeOption;
	}
}
