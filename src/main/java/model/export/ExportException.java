package model.export;

import util.LogBuffer;

import java.math.BigDecimal;
import java.math.MathContext;

public class ExportException extends Exception {

	private static final long serialVersionUID = 1L;

	public ExportException(final String string) {
		super(string);
	}

	public ExportException(ExportHandler eh,RegionType region) {
		super(getMessage(eh,region));
	}

	private static String getMessage(ExportHandler eh,RegionType region) {
		if(region == null) {
			if(eh.isExportValid(RegionType.SELECTION)) {
				region = RegionType.SELECTION;
			} else {
				region = RegionType.VISIBLE;
			}
		}
		String string = "Export exception";
		int x = (eh.getFormat().isDocumentFormat() ?
			eh.getMatrixXDim(region) : eh.getXDim(region));
		int y = (eh.getFormat().isDocumentFormat() ?
			eh.getMatrixYDim(region) : eh.getYDim(region));
		double tooBig =
			((double) x / (double) ExportHandler.getMaxImageSize()) * (double) y;
		if(tooBig > 1.0) {
			BigDecimal bd = new BigDecimal(tooBig);
			bd = bd.round(new MathContext(4));
			double rounded = bd.doubleValue();
			int overflow = 0;
			if(tooBig < 2.0) {
				overflow = (int) Math.round(
					(double) ExportHandler.getMaxImageSize() *
					(tooBig - 1.0));
			}
			LogBuffer.println("Export too big.  [x" + x + " * y" + y +
				"] > [" + ExportHandler.getMaxImageSize() + "].");
			string = "Error: Unable to model.export image.\n\n" +
				"The smallest available model.export region [" +
				region + ": " +
				eh.getNumXExportIndexes(region) + "cols x " +
				eh.getNumYExportIndexes(region) + "rows] is about [" +
				(overflow == 0 ?
					rounded + "] times" : overflow + "] points") +
				" too big for image model.export.\n\nTry selecting or zooming to a " +
				"smaller area to model.export.";
		}
		return(string);
	}
}
