package edu.stanford.genetics.treeview;

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
	private ExportAspect aspectType;
	private boolean showSelections;
	
	public ExportOptions() {
		
		this.formatType = FormatType.getDefault();
		this.paperType = PaperType.getDefault();
		this.regionType = RegionType.getDefault();
		this.aspectType = ExportAspect.getDefault();
		this.showSelections = false;
	}
	
	public FormatType getFormatType() {
		return formatType;
	}

	public void setFormatType(FormatType formatType) {
		this.formatType = formatType;
	}

	public PaperType getPaperType() {
		return paperType;
	}

	public void setPaperType(PaperType paperType) {
		this.paperType = paperType;
	}

	public RegionType getRegionType() {
		return regionType;
	}

	public void setRegionType(RegionType regionType) {
		this.regionType = regionType;
	}

	public ExportAspect getAspectType() {
		return aspectType;
	}

	public void setAspectType(ExportAspect aspectType) {
		this.aspectType = aspectType;
	}

	public boolean isShowSelections() {
		return showSelections;
	}

	public void setShowSelections(boolean showSelections) {
		this.showSelections = showSelections;
	}
}
