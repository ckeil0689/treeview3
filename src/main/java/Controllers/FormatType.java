/**
 * 
 */
package Controllers;

import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import edu.stanford.genetics.treeview.LogBuffer;

/**
 * The export file formats supported.
 * @author rleach
 */
public enum FormatType {
	PDF("PDF"),SVG("SVG"),PS("PS"),PNG("PNG"),JPG("JPG"),PPM("PPM");

	private final String toString;
	
	private FormatType(String toString) {
		this.toString = toString;
	}
	
	@Override
	public String toString() {
		return toString;
	}

	public static FormatType getDefault() {
		return(FormatType.PNG);
	}

	/**
	 * This returns the format that has the least required amount of memory, as
	 * imposed by BufferedImage to create the preview
	 * 
	 * @return
	 */
	public static FormatType getMinDefault() {
		return(FormatType.PDF);
	}

	public static FormatType getDefaultDocumentFormat() {
		return(FormatType.PDF);
	}

	public static FormatType[] getHiResFormats() {
		FormatType[] hiResFormats = {PDF,SVG,PS,PNG,PPM};
		return(hiResFormats);
	}

	public static FormatType[] getDocumentFormats() {
		FormatType[] docFormats = {PDF,SVG,PS};
		return(docFormats);
	}

	public static FormatType[] getImageFormats() {
		FormatType[] imgFormats = {PNG,JPG,PPM};
		return(imgFormats);
	}

	public boolean isDocumentFormat() {
		FormatType[] docFormats = FormatType.getDocumentFormats();
		for(int i = 0;i < docFormats.length;i++) {
			if(this == docFormats[i]) {
				return(true);
			}
		}
		return(false);
	}

	public String appendExtension(String fileName) {
		if(this == PDF) {
			if(!fileName.endsWith(".pdf") && !fileName.endsWith(".PDF")) {
				fileName += ".pdf";
			}
		} else if(this == PS) {
			if(!fileName.endsWith(".ps") && !fileName.endsWith(".PS")) {
				fileName += ".ps";
			}
		} else if(this == SVG) {
			if(!fileName.endsWith(".svg") && !fileName.endsWith(".SVG")) {
				fileName += ".svg";
			}
		} else if(this == PNG) {
			if(!fileName.endsWith(".png") && !fileName.endsWith(".PNG")) {
				fileName += ".png";
			}
		} else if(this == JPG) {
			if(!fileName.endsWith(".jpg") && !fileName.endsWith(".JPG") &&
				!fileName.endsWith(".jpeg") && !fileName.endsWith(".JPEG")) {
				fileName += ".jpg";
			}
		} else if(this == PPM) {
			if(!fileName.endsWith(".ppm") && !fileName.endsWith(".PPM")) {
				fileName += ".ppm";
			}
		}
		return(fileName);
	}

	public boolean hasAlpha() {
		if(this.isDocumentFormat() || this == FormatType.JPG) {
			return(false);
		} else {
			return(true);
		}
	}

	public boolean hasDefaultBackground() {
		if((this == FormatType.JPG) || (this == FormatType.PPM)) {
			return(true);
		}
		return(false);
	}
}
