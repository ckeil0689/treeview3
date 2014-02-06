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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.XmlConfig;

public class NewModelLoader {

	private TVModel targetModel;
	private FileSet fileSet;
	private TreeViewFrame frame;
	
	private ArrayList<String[]> stringLabels;
	private ArrayList<double[]> doubleData;
	
	private int dataStartRow;
	private int dataStartCol;
	
	private int gidCol;
	private int aidRow;
	
	private boolean gidFound = false;
	private boolean aidFound = false;
	private boolean eWeightFound = false;
	private boolean gWeightFound = false;
	
	public NewModelLoader(TVModel model) {
		
		this.targetModel = model;
		this.frame = (TreeViewFrame)model.getFrame();
		this.fileSet = model.getFileSet();
	}
	
	public void loadFile() {
		
		LoadWorker worker = new LoadWorker();
		worker.execute();
		
//		try {
//			System.out.println("Starting load.");
//			// Read data from specified file location
//			
//			int loadBarMax = count(fileSet.getCdt());
//			frame.setLoadBarMax(loadBarMax);
//			
//			FileInputStream fis = new FileInputStream(fileSet.getCdt());
//	        DataInputStream in = new DataInputStream(fis);
//	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
//	        
//	        // Get data from file into String and double arrays 
//	        // Put the arrays in ArrayLists for later access.
//	        System.out.println("Starting extract.");
//	        extractData(br);
//	        
//        } catch(FileNotFoundException e) {
//        	e.printStackTrace();
//        	
//        } catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//		// Parse the CDT File
//		parseCDT();
//		
//		// If present, parse ATR File
//		if(aidFound) {
//			parseATR();
//		
//		} else {
//			targetModel.aidFound(false);
//		}
//		
//		// If present, parse GTR File
//		if(gidFound) {
//			parseGTR();
//			
//		} else {
//			targetModel.gidFound(false);
//		}
//		
//		// Load Config File
//		try {
//			final String xmlFile = targetModel.getFileSet().getJtv();
//
//			XmlConfig documentConfig;
//			if (xmlFile.startsWith("http:")) {
//				documentConfig = new XmlConfig(new URL(xmlFile),
//						"DocumentConfig");
//
//			} else {
//				documentConfig = new XmlConfig(xmlFile, "DocumentConfig");
//			}
//			targetModel.setDocumentConfig(documentConfig);
//
//		} catch (final Exception e) {
//			targetModel.setDocumentConfig(null);
//			e.printStackTrace();
//		}
//		
//		targetModel.setLoaded(true);
	}
	
	public void load() {
		
		try {
			System.out.println("Starting load.");
			// Read data from specified file location
			
			int loadBarMax = count(fileSet.getCdt());
			frame.setLoadBarMax(loadBarMax);
			
			FileInputStream fis = new FileInputStream(fileSet.getCdt());
	        DataInputStream in = new DataInputStream(fis);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        
	        // Get data from file into String and double arrays 
	        // Put the arrays in ArrayLists for later access.
	        System.out.println("Starting extract.");
	        extractData(br);
	        
        } catch(FileNotFoundException e) {
        	e.printStackTrace();
        	
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// Parse the CDT File
		parseCDT();
		
		// If present, parse ATR File
		if(aidFound) {
			parseATR();
		
		} else {
			targetModel.aidFound(false);
		}
		
		// If present, parse GTR File
		if(gidFound) {
			parseGTR();
			
		} else {
			targetModel.gidFound(false);
		}
		
		// Load Config File
		try {
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
		final String HexDigits  = "(\\p{XDigit}+)";

		// an exponent is 'e' or 'E' followed by an optionally 
		// signed decimal integer.
		final String Exp        = "[eE][+-]?"+Digits;
		final String fpRegex    =
		    ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
		     "[+-]?(" + // Optional sign character
		     "NaN|" +           // "NaN" string
		     "Infinity|" +      // "Infinity" string
		     // Digits ._opt Digits_opt ExponentPart_opt 
		     //FloatTypeSuffix_opt
		     "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+
		     // . Digits ExponentPart_opt FloatTypeSuffix_opt
		     "(\\.("+Digits+")("+Exp+")?)|"+
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

		  
		stringLabels = new ArrayList<String[]>();
		doubleData = new ArrayList<double[]>();
		
		String line;
		dataStartRow = 0;
		dataStartCol = 0;
		int gWeightCol = 0;
		int rowN = 0;
		boolean dataFound = false;
		
		try {
			while ((line = reader.readLine()) != null) {
				
				frame.updateLoadBar(rowN);
	
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
					
					// handle line in which data has been found
					stringLabels.add(labels);
					
					if(dataFound) {
						dataValues = new double[lineAsStrings.length 
						                        - dataStartCol];
						
						for(int i = 0; i < lineAsStrings.length 
		                        - dataStartCol; i++) {
							
							String element = lineAsStrings[i];
						
							// Check whether string can be double and is not 
							// gweight
							if (Pattern.matches(fpRegex, element)
									&& i != gWeightCol) {
								
								double val = Double.parseDouble(element);
								dataValues[i] = val;
								
							} else {
								dataValues[i] = 0;
							}
						}
						
						doubleData.add(dataValues);
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
						
						if (i >= dataStartCol) { 
							// handle parseDouble error somehow? 
							// using the Pattern.matches method screws up 
							// loading time by a factor of 1000....
							double val = Double.parseDouble(element);
							dataValues[i] = val;
							
						} else {
							dataValues[i] = 0;
						}
					}
					
					stringLabels.add(labels);
					doubleData.add(dataValues);
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
		
		int nExpr = doubleData.get(0).length;
		int nExprPrefix = dataStartRow;
		
		int nGenePrefix = dataStartCol;
		int nGene = doubleData.size();
		
		// Set Array Prefix and Headers
		final String[] arrayPrefix = new String[nExprPrefix];
		final String[][] aHeaders = new String[nExpr][nExprPrefix];
		
		// fill prefix array
		for(int i = 0; i < nExprPrefix; i++) {
			
			arrayPrefix[i] = stringLabels.get(i)[0];
			
			String[] labelRow = stringLabels.get(i);
			
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
			
			genePrefix[i] = stringLabels.get(0)[i];
		}
		
		// Fill Header array
		for (int i = 0; i < nGene; i++) {
			
			gHeaders[i] = stringLabels.get(i + nExprPrefix);
		}
		
		targetModel.setGenePrefix(genePrefix);
		targetModel.setGeneHeaders(gHeaders);
		
//		final double[] exprData = concatAll(doubleData);
		
//		targetModel.setExprData(exprData);
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
				get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			frame.setDataModel(targetModel);
			frame.confirmLoaded();
		}
		
	}
}
