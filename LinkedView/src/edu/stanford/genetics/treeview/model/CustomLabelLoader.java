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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class CustomLabelLoader {

	private TreeViewFrame tvFrame;
	private HeaderInfo headerInfo;
	private String[][] labels;
	private String[] newNames;
	private boolean[] labelMatches;
	private int[] selectedIndeces;
	private int misses;
	private int lineNum;
	
    boolean namesFound = false;
	
	public CustomLabelLoader(TreeViewFrame tvFrame, HeaderInfo headerInfo, 
			int[] selectedIndeces) {
		
		this.tvFrame = tvFrame;
		this.headerInfo = headerInfo;
		this.selectedIndeces = selectedIndeces;
	}
	
	/**
	 * Loads the file with an InputStream and a BufferedReader and then
	 * assigns the file content to a String[][] which contains the new labels.
	 * The method then calls addNewLabels() to add the newly loaded labels to
	 * the currently loaded TVModel object.
	 * @param customFile
	 */
	public void load(File customFile) {
		
		try {
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
	        
	        // Number of row labels without GID
	        int geneLabelNum = tvFrame.getDataModel().getGeneHeaderInfo()
	        		.getNumNames();
	        
	        if(tvFrame.getDataModel().gidFound()) {
	        	geneLabelNum--;
	        }

	        labels = new String[lineNum][geneLabelNum];
	        
	        String line;
	        // iterate reader through each line
	        while((line = br.readLine()) != null) {
	        	
	        	final String[] lineAsStrings = line.split("\t");
		
				labels[rowN] = lineAsStrings;
				
	        	rowN++;
	        }
	        
	        br.close();
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Searches the loaded labels for header names such as ORF.
	 * If they are present, the newly loaded labels will later replace the old
	 * labels.
	 */
	public int checkForHeaders(TVModel model) {
		
		int checkLimit = headerInfo.getNumHeaders()/100; 
		
		newNames = new String[labels[0].length];
		
		if(checkLimit > 5) {
			checkLimit = 5;
		}
		
		// YORF regex pattern
//		Pattern pattern = Pattern.compile("(\\D{3}\\d{3}\\D{1})");
//		
//		for(int i = 0; i < checkRowLimit; i++) {
//			
//			for(int j = 0; j < labels[i].length; j++) {
//				
//				String yorf = labels[i][j];
//				Matcher matcher = pattern.matcher(yorf);
//				
//				if (!(matcher.find() || yorf.equalsIgnoreCase(""))) {
////					LogBuffer.println("Label: " + yorf);
//					newNames[j] = yorf;
//					namesFound = true;
//				    
//				} else {
////					LogBuffer.println(matcher.group(0));
//				}
//			}
//		}
		
		for(int i = 0; i < checkLimit; i++) {
			
			for(int j = 0; j < labels[i].length; j++) {
				
				String yorf = labels[i][j];
				
				if (!yorf.equalsIgnoreCase("")) {
					newNames[j] = yorf;
					namesFound = true;
				}
			}
		}
		
		// Check if old model already contains labels from the new list.
		List<String> existingLabels = Arrays.asList(headerInfo.getNames());
		
		labelMatches = new boolean[newNames.length];
		
		misses = 0;
		if(existingLabels != null) {
			for(int i = 0; i < newNames.length; i++) {
				
				if(existingLabels.contains(newNames[i])) {
					labelMatches[i] = true;
					
				} else {
					labelMatches[i] = false;
					misses++;
				}
			}
		} else {
			LogBuffer.println("No Label names could be loaded.");
			return 0;
		}
		
		int addIndex = 0;
		String[] finalNames = new String[misses];
		for(int i = 0; i < labelMatches.length; i ++) {
			
			if(!labelMatches[i]) {
				finalNames[addIndex] = newNames[i];
				addIndex++;
			}
		}
		
		newNames = finalNames;
		
		LogBuffer.println("Old Labels: " + existingLabels.toString());
		LogBuffer.println("New Labels: " + Arrays.toString(newNames));
		LogBuffer.println("Selected Indeces: " + Arrays.toString(
				selectedIndeces));
		LogBuffer.println("Match List: " + Arrays.toString(labelMatches));
		
		return labels[0].length;
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
	public String[] replaceLabel(String[] oldHeaders, String oldNames[]) {
		
		String[] newHeaders = null;
		String[] headerToAdd;
			
		newHeaders = findNewLabel(oldHeaders, oldNames, labels);
		headerToAdd = concatArrays(oldHeaders, newHeaders);
		
		return headerToAdd;
	}
	
	/**
	 * Checks the loadedLabels array whether it contains a label from the old
	 * headerArray and then replaces it accordingly with the 
	 * newly loaded version. Returns the new 
	 * @param oldLabels
	 * @param loadedLabels
	 * @return
	 */
	public String[] findNewLabel(String[] oldGene, String[] oldNames, 
			String[][] loadedLabels) {
		
		List<String> newLabels = new ArrayList<String>();
		boolean match = false;
		
		// Find a match
		int matchIndex = -1;
		for(int i = 0; i < loadedLabels.length; i++) {
			
			String[] loadedLabelElement = loadedLabels[i];
			
			for(int j = 0; j < loadedLabelElement.length; j++) {
			
				for(int k = 0; k < oldGene.length; k++) {
					
					if(loadedLabelElement[j].equalsIgnoreCase(oldGene[k])) {
						match = true;
						matchIndex = i;
						break;
					} 
				}
			}
			
			if(match) {
				break;
			}
		}
		
		// Replace matched element.
		if(match) {
			String[] matchedGene = loadedLabels[matchIndex];
			
			for(int i = 0; i < matchedGene.length; i++) {
				
				if(!labelMatches[i]) {
					newLabels.add(matchedGene[i]);
				}
			}
					
		} else {
			for(int i = 0; i < misses; i++) {
				
				newLabels.add("No match");
			}
		}
		
		// Change list to array for return
		return newLabels.toArray(new String[newLabels.size()]);
	}
	
	/**
	 * Sets the new names for the labels in the TVModel object.
	 * @param model
	 */
	public void setHeaders(TVModel model, String type, 
			String[][] headersToAdd) {
		
		// Set the new headers for the TVModel
		if(type.equalsIgnoreCase("Row")) {
			model.setGeneHeaders(headersToAdd);
			
		} else if(type.equalsIgnoreCase("Column")) {
			model.setArrayHeaders(headersToAdd);
		}
		
		model.notifyObservers();
		
		// Set the new Labels for the headers
		String[] oldNames = headerInfo.getNames();
		String[] namesToAdd = null;
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
		
		headerInfo.setPrefixArray(namesToAdd);
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
