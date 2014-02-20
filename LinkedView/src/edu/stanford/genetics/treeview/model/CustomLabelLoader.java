package edu.stanford.genetics.treeview.model;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class CustomLabelLoader {

	private TreeViewFrame tvFrame;
	private String[][] labels;
	private String[] newNames;
	private int lineNum;
	
    boolean namesFound = false;
	
	public CustomLabelLoader(TreeViewFrame tvFrame) {
		
		this.tvFrame = tvFrame;
	}
	
	public String[][] load() {
		
		File customFile;
		
		try {
			customFile = tvFrame.selectFile();
			
			final String fileName = customFile.getAbsolutePath();
			
			lineNum = count(fileName);
			
			// Next: read file, return string arrays with new names
			// Then: update currently loaded model.
			FileInputStream fis = new FileInputStream(fileName);
	        DataInputStream in = new DataInputStream(fis);
	        BufferedReader br = new BufferedReader(
	        		new InputStreamReader(in));
	        
	        // int to count the current row in while loop
	        int rowN = 0;
	        int headerStart = 0;
	        
	        // Number of row labels without GID
	        int cdtColN = tvFrame.getDataModel().getGeneHeaderInfo().getNumNames();
	        
	        if(tvFrame.getDataModel().gidFound() == true) {
	        	cdtColN--;
	        }
	        
	        setLabelArray(lineNum, cdtColN);
	        
	        String line;
	        // iterate reader through each line
	        while((line = br.readLine()) != null) {
	        	
	        	final String[] lineAsStrings = line.split("\t");
		
				labels[rowN - headerStart] = lineAsStrings;
				
	        	rowN++;
	        }
	        
	        br.close();
			
		} catch (LoadException exc) {
			exc.printStackTrace();
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return labels;
	}
	
	/**
	 * Searches the loaded file for labels.
	 */
	public void checkForLabels(TVModel model) {
		
		int checkRowLimit = (model.geneHeaderInfo.getNumHeaders()/100); 
		
		newNames = new String[labels[0].length];
		
		if(checkRowLimit > 5) {
			checkRowLimit = 5;
		}
		
		// YORF regex pattern
		Pattern pattern = Pattern.compile("(\\D{3}\\d{3}\\D{1})");
		
		for(int i = 0; i < checkRowLimit; i++) {
			
			for(int j = 0; j < labels[i].length; j++) {
				
				String yorf = labels[i][j];
				Matcher matcher = pattern.matcher(yorf);
				
				if (!(matcher.find() || yorf.equalsIgnoreCase(""))) {
					System.out.println("Label: " + yorf);
					newNames[j] = yorf;
					namesFound = true;
				    
				} else {
					System.out.println(matcher.group(0));
				}
			}
		}
	}
	
	/**
	 * Fuses two arrays together.
	 * @param a
	 * @param b
	 * @return
	 */
	public String[] concatArrays(String [] a, String[] b) {
		
		String [] c = new String[a.length + b.length];
		
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		
		return c;
	}
	
	/**
	 * Replacing matching values in the old headerArray with loaded labels.
	 * @param model
	 * @param loadedLabels
	 */
	public void replaceLabels(TVModel model) {
		
		String[][] oldHeaders = model.getGeneHeaderInfo().getHeaderArray();
		String[][] headersToAdd = 
				new String[oldHeaders.length + labels[0].length][];
		
		String[] newHeaders;
		// Iterate over loadedLabels
		for(int i = 0; i < oldHeaders.length; i++) {
			
			newHeaders = findNewHeader(oldHeaders[i], labels);
			headersToAdd[i] = concatArrays(oldHeaders[i], newHeaders);
		}
		
		model.setGeneHeaders(headersToAdd);
		model.notifyObservers();
	}
	
	/**
	 * Checks the loadedLabels array whether it contains a label from the old
	 * headerArray and then replaces it accordingly with the 
	 * newly loaded version.
	 * @param oldLabels
	 * @param loadedLabels
	 * @return
	 */
	public String[] findNewHeader(String[] oldGene, String[][] loadedLabels) {
		
		String[] newGene = new String[oldGene.length];
		boolean match = false;
		
		for(int i = 0; i < loadedLabels.length; i++) {
			
			for(int j = 0; j < loadedLabels[i].length; j++) {
			
				for(int k = 0; k < oldGene.length; k++) {
					
					if(loadedLabels[i][j].equalsIgnoreCase(oldGene[k])) {
						match = true;	
						newGene = loadedLabels[i];
						break;
					} 
				}
			}
		}
		
		if(!match) {
			newGene = oldGene;	
		}
		
		return newGene;
	}
	
	// Experimental
	public void addNewLabels(TVModel model, String[][] loadedLabels) {
		
		checkForLabels(model);
		
		String[] oldNames = model.getGeneHeaderInfo().getNames();
		
		String[] namesToAdd;
		// Change model prefix array
		if(namesFound) {
			namesToAdd = concatArrays(oldNames, newNames);
			
			// Check for empty or null value
			for(int i = 0; i < namesToAdd.length; i++) {
				
				if(namesToAdd[i] == null 
						|| namesToAdd[i].equalsIgnoreCase("")) {
					namesToAdd[i] = "CUSTOM " + (i + 1);
				}
			}
			
		} else {
			// Make headers for custom labels
			for(int i = 0; i < newNames.length; i++) {
				
				newNames[i] = "CUSTOM " + (i + 1);
			}
			
			namesToAdd = concatArrays(oldNames, newNames);
		}
		
		model.getGeneHeaderInfo().setPrefixArray(namesToAdd);
		
		// Change headerArrays (without matching actual names first)
		replaceLabels(model);
	}
	
	/**
	 * Sets the instance variable that represent a 2-dimensional String array
	 * for all the headers.
	 * @param rowN
	 * @param cdtColN
	 */
	public void setLabelArray(int rowN, int cdtColN) {
		
		labels = new String[rowN][cdtColN];
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
}
