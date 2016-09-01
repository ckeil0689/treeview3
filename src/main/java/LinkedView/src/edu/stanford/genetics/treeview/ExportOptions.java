package edu.stanford.genetics.treeview;

import org.freehep.graphicsio.PageConstants;

import Controllers.ExportHandler;
import Controllers.FormatType;
import Controllers.RegionType;

/**
 * This class holds and manages selected export options.
 * @author CKeil
 *
 */
public class ExportOptions {

	private FormatType formatType;
	private PaperType paperType;
	private RegionType regionType;
	private AspectType aspectType;
	private String orientation;
	private boolean showSelections;
	
	public ExportOptions() {
		
		this.formatType = FormatType.getDefault();
		this.paperType = PaperType.getDefault();
		this.regionType = RegionType.getDefault();
		this.aspectType = AspectType.getDefault();
		this.orientation = ExportHandler.getDefaultPageOrientation();
		this.showSelections = false;
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
}
