package edu.stanford.genetics.treeview.model;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import Views.LoadProgressView;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.XmlConfig;

public class NewModelLoader {

	protected TVModel targetModel;
	private FileSet fileSet;
	protected TreeViewFrame tvFrame;
	protected LoadProgressView loadProgView;
	
	// Instance variables for the actual data to be loaded.
	protected String[][] stringLabels;
	private double[][] doubleData;
	
	private int lineNum;
	private int dataStartRow;
	private int dataStartCol;
	
	private boolean gidFound = false;
	private boolean aidFound = false;
	private boolean eWeightFound = false;
	private boolean gWeightFound = false;
	
	public NewModelLoader(TVModel model) {
		
		this.targetModel = model;
		this.tvFrame = (TreeViewFrame)model.getFrame();
		this.fileSet = model.getFileSet();
		this.loadProgView = tvFrame.getLoadProgView();
	}
	
	public TVModel load() {
		
		try {
			// Read data from specified file location
			loadProgView.resetLoadBar();
			lineNum = count(fileSet.getCdt());
			loadProgView.setLoadBarMax(lineNum);
			
			loadProgView.setLoadLabel("Loading Data into TreeView.");
			
			FileInputStream fis = new FileInputStream(fileSet.getCdt());
	        DataInputStream in = new DataInputStream(fis);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        
	        // Get data from file into String and double arrays 
	        // Put the arrays in ArrayLists for later access.
	        System.out.println("Starting extract.");
	        extractData(br);
	        
        } catch(FileNotFoundException e) {
        	System.out.println("CDT file could not be found.");
        	e.printStackTrace();
        	
        } catch (IOException e) {
        	System.out.println("CDT file could not be loaded into " +
        			"BufferedReader.");
        	e.printStackTrace();
		}
		
		
		// Parse the CDT File
		loadProgView.setLoadLabel("Making sense of CDT file.");
		parseCDT();
		
		// If present, parse ATR File
		if(aidFound) {
			loadProgView.setLoadLabel("Reading ATR file.");
			parseATR();
		
		} else {
			System.out.println("No ATR file found for this CDT file.");
			targetModel.aidFound(false);
		}
		
		// If present, parse GTR File
		if(gidFound) {
			loadProgView.setLoadLabel("Reading GTR file.");
			parseGTR();
			
		} else {
			System.out.println("No GTR file found for this CDT file.");
			targetModel.gidFound(false);
		}
		
		// Load Config File
		try {
			loadProgView.setLoadLabel("Loading configuration file.");
			final String xmlFile = targetModel.getFileSet().getJtv();

			XmlConfig documentConfig;
			if (xmlFile.startsWith("http:")) {
				documentConfig = new XmlConfig(new URL(xmlFile),
						"DocumentConfig");

			} else {
				documentConfig = new XmlConfig(xmlFile, "DocumentConfig");
			}
			targetModel.setDocumentConfig(documentConfig);

		} catch (final Exception e) {
			targetModel.setDocumentConfig(null);
			e.printStackTrace();
		}
		
		targetModel.setLoaded(true);
		return targetModel;
	}
	
	/**
	 * Count amount of lines in the file to be loaded so that the progressBar
	 * can get correct values for extractData().
	 * Code from StackOverflow (https://stackoverflow.com/questions/453018).
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public int count(String filename) throws IOException {
	    
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    
		try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	        
	    } finally {
	        is.close();
	    }
	}
	
	/**
	 * Reading the data separated by tab delimited \t
	 * The regex check if a String can be parsed to double is taken from
	 * StackOverflow (https://stackoverflow.com/questions/8564896).
	 * @param reader
	 * @param dataExtract
	 */
	public  void extractData(BufferedReader reader) {
		
		final String Digits     = "(\\p{Digit}+)";
		final String emptyDigits = "(\\p{Digit}*)";
		final String HexDigits  = "(\\p{XDigit}+)";

		// an exponent is 'e' or 'E' followed by an optionally 
		// signed decimal integer.
		final String exp        = "[eE][+-]?" + emptyDigits;
		final String fpRegex    =
		    ("[\\x00-\\x20]*" +  // Optional leading "whitespace"
		     "[+-]?(" + // Optional sign character
		     "NaN|" +           // "NaN" string
		     "Infinity|" +      // "Infinity" string
		     // Digits ._opt Digits_opt ExponentPart_opt 
		     //FloatTypeSuffix_opt
		     "(((" + Digits + "(\\.)?(" + Digits + "?)(" + exp + ")?)|" +
		     // . Digits ExponentPart_opt FloatTypeSuffix_opt
		     "(\\.(" + Digits + ")(" + exp + ")?)|" +
		     // Hexadecimal strings
		     "((" +
		     // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
		     "(0[xX]" + HexDigits + "(\\.)?)|" +    
			// 0[xX] HexDigits_opt . HexDigits BinaryExponent 
		     //FloatTypeSuffix_opt
		     "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +
		     ")[pP][+-]?" + Digits + "))" +
		     "[fFdD]?))" +
		     "[\\x00-\\x20]*");// Optional trailing "whitespace"

		  
		stringLabels = new String[lineNum][];
		
		String line;
		dataStartRow = 0;
		dataStartCol = 0;
		int gWeightCol = 0;
		int rowN = 0;
		boolean dataFound = false;
		
		try {
			while ((line = reader.readLine()) != null) {
				
				loadProgView.updateLoadBar(rowN);
	
				// load line as String array
				final String[] lineAsStrings = line.split("\t");
				String[] labels;
				double[] dataValues;
				
				// loop over String array to convert applicable String to double
				// first find data start
				if(dataFound == false) {
					labels = new String[lineAsStrings.length];
					boolean containsEWeight = false;
					
					for(int i = 0; i < lineAsStrings.length; i++) {
						
						String element = lineAsStrings[i];
						
						if(element.equalsIgnoreCase("GID")) {
							gidFound = true;
						} 
						
						// Check for GWEIGHT to avoid the weight being
						// recognized as row start of actual data
						if(element.equalsIgnoreCase("GWEIGHT")) {
							gWeightCol = i;
							gWeightFound = true;
						} 
						
						if(element.equalsIgnoreCase("AID")) {
							aidFound = true;
						}
						
						// Check for EWEIGHT to avoid the weight being
						// recognized as column start of actual data
						if(element.equalsIgnoreCase("EWEIGHT")) {
							containsEWeight = true;
							eWeightFound = true;
						}
					
						if (Pattern.matches(fpRegex, element) 
								&& (!containsEWeight && i != gWeightCol)) {
							
							dataStartRow = rowN;
							dataStartCol = i;
							
							dataFound = true;
							break;
							
						} else {
							labels[i] = element;
						}
					}
					
					// avoid first datarow to be added with null values
					if(dataFound) {
						String[] firstDataRow = new String[dataStartCol];
						
						for(int i = 0; i < labels.length; i++) {
							
							if(labels[i] != null) {
								firstDataRow[i] = labels[i];
								
							} else {
								break;
							}
						}
						stringLabels[rowN] = firstDataRow;
						
					} else {
						// handle line in which data has been found
						stringLabels[rowN] = labels;
					}
					
					if(dataFound) {
						doubleData = new double[lineNum - dataStartRow][];
						dataValues = new double[lineAsStrings.length 
						                        - dataStartCol];
						
						for(int i = 0; i < lineAsStrings.length 
		                        - dataStartCol; i++) {
							
							String element = lineAsStrings[i + dataStartCol];
						
							// Check whether string can be double and is not 
							// gweight
							if (Pattern.matches(fpRegex, element)
									&& i != gWeightCol) {
								
								// For empty exponents apparently 
								// caused by Windows .txt
								if(element.endsWith("e") 
										|| element.endsWith("E")) {
									element = element + "+00";
								}
								
								double val = Double.parseDouble(element);
								dataValues[i] = val;
								
							} else {
								dataValues[i] = 0;
							}
						}
						
						doubleData[rowN - dataStartRow] = dataValues;
					}
					
				} else {
					labels = new String[dataStartCol];
					dataValues = new double[lineAsStrings.length 
					                        - dataStartCol];
					
					for(int i = 0; i < dataStartCol; i++) {
						
						String element = lineAsStrings[i];
					
						labels[i] = element;
					}
					
					for(int i = 0; i < lineAsStrings.length - dataStartCol;
							i++) {
						
						String element = lineAsStrings[i + dataStartCol];
						
						// handle parseDouble error somehow? 
						// using the Pattern.matches method screws up 
						// loading time by a factor of 1000....
						if(element.endsWith("e") 
								|| element.endsWith("E")) {
							element = element + "+00";
						}
						
						double val = Double.parseDouble(element);
						dataValues[i] = val;
					}
					
					stringLabels[rowN] = labels;
					doubleData[rowN - dataStartRow] = dataValues;
				} 
				rowN++;
			}
			
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<String[]> extractGTR(BufferedReader reader) {
		
		ArrayList<String[]> gtrData = new ArrayList<String[]>();
		String line;
		
		try {
			while ((line = reader.readLine()) != null) {
	
				// load line as String array
				final String[] lineAsStrings = line.split("\t");
				gtrData.add(lineAsStrings);
			}
		} catch (IOException e) {
				
		}
		
		return gtrData;
	}
	
	public void parseCDT() {
		
		// Tell model whether EWEIGHT and GWEIGHT where found
		targetModel.setEweightFound(eWeightFound);
		targetModel.setGweightFound(gWeightFound);
		
		int nExpr = doubleData[0].length;
		int nExprPrefix = dataStartRow;
		
		int nGenePrefix = dataStartCol;
		int nGene = doubleData.length;
		
		// Set Array Prefix and Headers
		final String[] arrayPrefix = new String[nExprPrefix];
		final String[][] aHeaders = new String[nExpr][nExprPrefix];
		
		// fill prefix array
		for(int i = 0; i < nExprPrefix; i++) {
			
			arrayPrefix[i] = stringLabels[i][0];
			
			String[] labelRow = stringLabels[i];
			
			// fill column header array
			for(int j = 0; j < nExpr; j++) {
				
				aHeaders[j][i] = labelRow[j + nGenePrefix];
			}
		}
		
		targetModel.setArrayPrefix(arrayPrefix);
		targetModel.setArrayHeaders(aHeaders);
		
		final String[] genePrefix = new String[nGenePrefix];
		final String[][] gHeaders = new String[nGene][nGenePrefix];
		
		// Fill row prefix array
		for(int i = 0; i < nGenePrefix; i++) {
			
			genePrefix[i] = stringLabels[0][i];
		}
		
		// Fill Header array
		for (int i = 0; i < nGene; i++) {
			
			gHeaders[i] = stringLabels[i + nExprPrefix];
		}
		
		targetModel.setGenePrefix(genePrefix);
		targetModel.setGeneHeaders(gHeaders);
		targetModel.setExprData(doubleData);
	}
	
	public void parseGTR() {
		
		// First, load the GTR File
		ArrayList<String[]> gtrData = loadSet(fileSet.getGtr());
			
		final String[] firstRow = gtrData.get(0);
		if ( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& (firstRow[0].equalsIgnoreCase("NODEID") == false)) { 
			
			// okay, need to assign headers...
			targetModel.setGtrPrefix(new String[] { "NODEID", "LEFT", 
					"RIGHT", "CORRELATION" });
			
			final String[][] gtrHeaders = new String[gtrData.size()][];
			for (int i = 0; i < gtrHeaders.length; i++) {
				gtrHeaders[i] = gtrData.get(i);
			}
			targetModel.setGtrHeaders(gtrHeaders);
		} else {// first row of tempVector is actual header names...
			targetModel.setGtrPrefix(firstRow);

			final String[][] gtrHeaders = new String[gtrData.size() - 1][];
			for (int i = 0; i < gtrHeaders.length; i++) {
				gtrHeaders[i] = gtrData.get(i + 1);
			}
			targetModel.setGtrHeaders(gtrHeaders);
		}
		
		targetModel.hashGIDs();
		targetModel.hashGTRs();
		targetModel.gidFound(gidFound);
	}
	
	public void parseATR() {
		
		// First, load the GTR File
		ArrayList<String[]> atrData = loadSet(fileSet.getAtr());
		
		final String[] firstRow = atrData.get(0);
		if ( // decide if this is not an extended file..
		(firstRow.length == 4)// is the length classic?
				&& (firstRow[0].equalsIgnoreCase("NODEID") == false)) { 
			
			// okay, need to assign headers...
			targetModel.setAtrPrefix(new String[] { "NODEID", "LEFT", 
					"RIGHT", "CORRELATION" });
			
			final String[][] atrHeaders = new String[atrData.size()][];
			for (int i = 0; i < atrHeaders.length; i++) {
				atrHeaders[i] = atrData.get(i);
			}
			targetModel.setAtrHeaders(atrHeaders);
		} else {// first row of tempVector is actual header names...
			targetModel.setAtrPrefix(firstRow);

			final String[][] atrHeaders = new String[atrData.size() - 1][];
			for (int i = 0; i < atrHeaders.length; i++) {
				atrHeaders[i] = atrData.get(i + 1);
			}
			targetModel.setAtrHeaders(atrHeaders);
		}
		
		targetModel.hashAIDs();
		targetModel.hashATRs();
		targetModel.aidFound(aidFound);
	}
	
	public ArrayList<String[]> loadSet(String loadingSet) {
		
		ArrayList<String[]> gtrData = new ArrayList<String[]>();
		
		try {
			System.out.println("Starting GTR load.");
			// Read data from specified file location
			
			FileInputStream fis = new FileInputStream(loadingSet);
	        DataInputStream in = new DataInputStream(fis);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        
	        // Get data from file into String and double arrays 
	        // Put the arrays in ArrayLists for later access.
	        System.out.println("Starting GTR extract.");
	        gtrData = extractGTR(br);
	        
        } catch(FileNotFoundException e) {
        	e.printStackTrace();
        }
		
		return gtrData;
	}
	
	class LoadWorker extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			
			load();
			return null;
		}
		
		@Override
		protected void done() {

			try {
				// Wait for worker to finish.
				get();
				
			} catch (InterruptedException e) {
				e.printStackTrace();
				
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
	}
}
