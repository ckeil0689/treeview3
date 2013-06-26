package edu.stanford.genetics.treeview.plugin.dendroview;

import java.util.ArrayList;
import java.util.List;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.UnknownOptionException;

public class DendroviewArgs {
		private String sResource;
		private String sExportType;
		private String[] remaining;
		private int[] arrayHeaders = null;
		private int[] geneHeaders = null;
		private Double xScale = null;
		private Double yScale = null;
		private Double dContrast;
		private Double dAtrHeight;
		private Double dGtrWidth;
		private Double dLogcenter = null;
		private Boolean bBelow = false;
		final private CmdLineParser parser;
		final private CmdLineParser.Option filePath;
		final private CmdLineParser.Option exportType;
		final private CmdLineParser.Option scaling;
		final private CmdLineParser.Option aHeaders;
		final private CmdLineParser.Option gHeaders;
		final private CmdLineParser.Option contrast;
		final private CmdLineParser.Option atrHeight;
		final private CmdLineParser.Option gtrWidth;
		final private CmdLineParser.Option below;
		final private CmdLineParser.Option logcenter;

		public DendroviewArgs(String [] args) {
			parser = new CmdLineParser();
			filePath = parser.addStringOption('o', "output");
			exportType = parser.addStringOption('f', "format");
			scaling = parser.addStringOption('s', "scaling");
			aHeaders = parser.addStringOption('a', "arrayHeaders");
			gHeaders = parser.addStringOption('g', "geneHeaders");
			contrast = parser.addDoubleOption('c', "contrast");
			atrHeight = parser.addDoubleOption('h', "atrHeight");
			gtrWidth = parser.addDoubleOption('w', "gtrWidth");
			below = parser.addBooleanOption('b', "below");
			logcenter = parser.addDoubleOption('l', "logcenter");
			
	        try {
				parser.parse(args);
				sResource = (String) parser.getOptionValue(filePath, null);
				String defaultType = (sResource == null || sResource.lastIndexOf('.') < 0)?null:
					sResource.substring(sResource.lastIndexOf('.')+1);
				sExportType = (String) parser.getOptionValue(exportType,defaultType);
				parseScaling((String) parser.getOptionValue(scaling, null));
				remaining = parser.getRemainingArgs();
				arrayHeaders = parseHeaders((String) parser.getOptionValue(aHeaders, null));
				geneHeaders = parseHeaders((String) parser.getOptionValue(gHeaders, null));
				dContrast = (Double) parser.getOptionValue(contrast, null);
				dAtrHeight = (Double)parser.getOptionValue(atrHeight, null);
				dGtrWidth = (Double)parser.getOptionValue(gtrWidth, null);
				bBelow = (Boolean)parser.getOptionValue(below, bBelow);
				dLogcenter = (Double)parser.getOptionValue(logcenter, null);
			} catch (IllegalOptionValueException e) {
	            System.err.println("Error parsing args, defaulting to type auto, no file loading");
	            System.err.println(e.getMessage());
	            printUsage();
				e.printStackTrace();
				sResource = null;
				remaining = null;
				sExportType = null;
				dLogcenter = null;
			} catch (UnknownOptionException e) {
	            System.err.println("Error parsing args, defaulting to type auto, no file loading");
	            System.err.println(e.getMessage());
	            printUsage();
				e.printStackTrace();
				sResource = null;
				remaining = null;
				sExportType = null;
				dLogcenter = null;
			}
		}
		private static int[] emptyIntArray = new int[0];

		private int[] parseHeaders(String optionValue) {
			if (optionValue == null)
				return emptyIntArray;
			String [] numbers = optionValue.split(",");
			List<Integer> retval = new ArrayList<Integer>();
			for (String number : numbers ){
				Integer parsed = Integer.parseInt(number);
				if (parsed >= 0)
					retval.add(parsed);
			}
			int[] retArray = new int[retval.size()];
			for (int i = 0; i < retval.size(); i++)
				retArray[i] = retval.get(i);
			return retArray;
		}

		private void parseScaling(String optionValue) throws IllegalOptionValueException {
			if (optionValue ==null)
				return;
			String[] strings = optionValue.split("x");
			if (strings.length != 2) {
				throw new jargs.gnu.CmdLineParser.IllegalOptionValueException(scaling, optionValue);
			}
			xScale = Double.parseDouble(strings[0]);
			yScale = Double.parseDouble(strings[1]);
		}

		public void printUsage() {
	        System.err.println("Usage:");
	        System.err.println(" -" + filePath.shortForm() + "/--" + filePath.longForm() + ": path of file to export to (required)");
	        System.err.println(" -" + exportType.shortForm() + "/--" + exportType.longForm() + ": string indicating output format (ps | png | gif), defaults to ending of file path (after '.')");
	        System.err.println(" -" + scaling.shortForm() + "/--" + scaling.longForm() + ": string indicating pixel scaling, i.e. 10x2 for 10 pixels horizontal, 2 vertical.");
	        System.err.println(" -" + aHeaders.shortForm() + "/--" + aHeaders.longForm() + ": comma separated list of array headers to include (default is none).");
	        System.err.println(" -" + gHeaders.shortForm() + "/--" + gHeaders.longForm() + ": comma separated list of gene headers to include (default is none).");
	        System.err.println(" -" + atrHeight.shortForm() + "/--" + atrHeight.longForm() + ": explicitly set height of array tree");
	        System.err.println(" -" + gtrWidth.shortForm() + "/--" + gtrWidth.longForm() + ": explicitly set width of gene tree");
	        System.err.println("Note: the following two options will change the settings the next time the file is loaded.");
	        System.err.println("      they are similar to making the changes through the GUI, and they are saved in the .jtv file");
	        System.err.println(" -" + contrast.shortForm() + "/--" + contrast.longForm() + ": set contrast value, similar to that in Settings->Pixel Settings... from the GUI");
	        System.err.println(" -" + logcenter.shortForm() + "/--" + logcenter.longForm() + ": turn on logscaling base 2 of specified center, similar to Settings->Pixel Settings...");
		}

		public String getFilePath() {
			return sResource;
		}
		public String[] remainingArgs() {
			return remaining;
		}
		public String getExportType() {
			return sExportType;
		}
		public int[] getArrayHeaders() {
			return arrayHeaders;
		}
		public int[] getGeneHeaders() {
			return geneHeaders;
		}
		public Double getXScale() {
			return xScale;
		}

		public Double getYScale() {
			return yScale;
		}
		public Double getContrast() {
			return dContrast;
		}
		public Double getGtrWidth() {
			return dGtrWidth;
		}
		public Double getAtrHeight() {
			return dAtrHeight;
		}
		public Boolean getArrayAnnoInside() {
			return bBelow;
		}
		public Double getLogcenter() {
			return dLogcenter;
		}
}
