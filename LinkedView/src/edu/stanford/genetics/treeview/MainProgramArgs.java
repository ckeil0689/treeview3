package edu.stanford.genetics.treeview;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.UnknownOptionException;

public class MainProgramArgs {
	private String sframeType;
	private String sResource;
	private String sExportType;
	private String[] remaining;
	final private CmdLineParser parser;
	final private CmdLineParser.Option resource;
	final private CmdLineParser.Option filePath;
	final private CmdLineParser.Option frameType;
	final private CmdLineParser.Option exportType;

	public MainProgramArgs(String [] args) {
		parser = new CmdLineParser();
		resource = parser.addStringOption('r', "resource");
		filePath = parser.addStringOption('f', "file");
		frameType = parser.addStringOption('t', "type");
		exportType = parser.addStringOption('x', "export");
        try {
			parser.parse(args);
			sframeType = (String) parser.getOptionValue(frameType, "auto");
			String resourceArg = (String) parser.getOptionValue(resource, null);
			if (resourceArg != null)
				sResource = resourceArg;
			else
				sResource = (String) parser.getOptionValue(filePath, null);
			sExportType = (String) parser.getOptionValue(exportType,null);
			remaining = parser.getRemainingArgs();
		} catch (IllegalOptionValueException e) {
            System.err.println("Error parsing args, defaulting to type auto, no file loading");
            System.err.println(e.getMessage());
            printUsage();
			e.printStackTrace();
			sframeType = "auto";
			sResource = null;
			remaining = null;
			sExportType = null;
		} catch (UnknownOptionException e) {
            System.err.println("Error parsing args, defaulting to type auto, no file loading");
            System.err.println(e.getMessage());
            printUsage();
			e.printStackTrace();
			sframeType = "auto";
			sResource = null;
			remaining = null;
			sExportType = null;
		}
	}

	public void printUsage() {
        System.err.println("Usage: all arguments are optional. By default will open a window with no files loaded.");
        System.err.println(" -" + resource.shortForm() + "/--" + resource.longForm() + ": resource to load (could be file or url)");
        System.err.println(" -" + filePath.shortForm() + "/--" + filePath.longForm() + ": path of file to load (deprecated, use resource arg instead)");
        System.err.println(" -" + frameType.shortForm() + "/--" + frameType.longForm() + ": string indicating type of frame (auto | linked | classic | kmeans), default is auto");
	}

	public String getFrameType() {
		return sframeType;
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
}
