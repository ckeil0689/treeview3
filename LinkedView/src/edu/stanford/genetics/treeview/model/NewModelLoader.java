package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class NewModelLoader {

	private TVModel targetModel;
	private TreeViewFrame frame;
	
	private ArrayList<String[]> stringLabels;
	private ArrayList<double[]> doubleData;
	
	private int dataStartRow;
	private int dataStartCol;
	
	public NewModelLoader(TVModel model) {
		
		this.targetModel = model;
		this.frame = (TreeViewFrame)model.getFrame();
	}
	
	public void loadFile() {
		
//			System.out.println("Starting load.");
//			// Read data from specified file location
//			final FileSet fileSet = targetModel.getFileSet();
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
//	        // Set all the needed instance variables for the Model using 
//	        // the loaded data.
//	        System.out.println("Starting model settings.");
		
		ExtractionWorker worker = new ExtractionWorker();
		worker.execute();
		
		findDimensions();
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
						
						// Check for GWEIGHT to avoid the weight being
						// recognized as row start of actual data
						if(element.equalsIgnoreCase("GWEIGHT")) {
							gWeightCol = i;
						} 
						
						// Check for EWEIGHT to avoid the weight being
						// recognized as column start of actual data
						if(element.equalsIgnoreCase("EWEIGHT")) {
							containsEWeight = true;
							
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
						
						if (Pattern.matches(fpRegex, element)) {
							//Double.valueOf(element);
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
	
	public void findDimensions() {
		
		
	}
	
	/**
	 * Worker thread to run extraction task in background.
	 * @author CKeil
	 *
	 */
	class ExtractionWorker extends SwingWorker<Void, Void> {
	   
		protected Void doInBackground() throws Exception {
	       
			try {
				System.out.println("Starting load.");
				// Read data from specified file location
				final FileSet fileSet = targetModel.getFileSet();
				
				FileInputStream fis = new FileInputStream(fileSet.getCdt());
		        DataInputStream in = new DataInputStream(fis);
		        BufferedReader br = new BufferedReader(new InputStreamReader(in));
		        
		        // Get data from file into String and double arrays 
		        // Put the arrays in ArrayLists for later access.
		        System.out.println("Starting extract.");
		        extractData(br);
		        
	        } catch(FileNotFoundException e) {
	        	e.printStackTrace();
	        }
	 
			return null;
	    }

	    protected void done(){
	    	
	        try{
	        	System.out.println("Done extracting.");
	        	
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
}
