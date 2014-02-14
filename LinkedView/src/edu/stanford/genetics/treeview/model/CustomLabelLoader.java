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

import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewFrame;

public class CustomLabelLoader {

	private TreeViewFrame tvFrame;
	private String[][] labels;
	private int lineNum;
	
    private boolean hasORF = false;
    private boolean hasNAME = false;
    private boolean hasGWeight = false;
	
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
	        
	        // booleans to determine whether certain labels were found
	        // in the custom file
	        boolean labelsFound = false;
	        boolean headersFound = false;
	        
	        // int to count the current row in while loop
	        int rowN = 0;
	        int headerStart = 0;
	        
	        // Number of row labels without GID
	        int cdtColN = tvFrame.getDataModel().getGeneHeaderInfo().getNumNames();
	        
	        if(tvFrame.getDataModel().gidFound() == true) {
	        	cdtColN--;
	        }
	        
	        // int variables for label locations in custom file
	        int orfCol = -1;
	        int nameCol = -1;
	        int gweightCol = -1;
	        
	        //Yeast ORF number of characters.
	        int orfLength = 7;
	        
	        String line;
			
	        // iterate reader through each line
	        while((line = br.readLine()) != null) {
	        	
	        	final String[] lineAsStrings = line.split("\t");
	        	
	        	if(!headersFound) {
		        	for(int i = 0; i < lineAsStrings.length; i++) {
						
						String element = lineAsStrings[i];
						
						if(element.equalsIgnoreCase("ORF")) {
							hasORF = true;
							orfCol = i;
							labelsFound = true;
							
						} else if(element.equalsIgnoreCase("NAME")) {
							hasNAME = true;
							nameCol = i;
							labelsFound = true;
							
						} else if(element.equalsIgnoreCase("GWEIGHT")) {
							hasGWeight = true;
							gweightCol = i;
							labelsFound = true;
							
						} else if(element.contains("y") && element.length() 
								== orfLength){
							headersFound = true;
							headerStart = rowN;
							setLabelArray(rowN, cdtColN);
							
							if(!labelsFound) {
								orfCol = 0;
								nameCol = 1;
								gweightCol = 2;
							}
						}
					}
	        	
				
				// after the labels were found add the headers to a Strig[][]
				// which can later update GeneHeaderInfo for the TVModel.
	        	} else {
					
					String[] rowLabels = new String[cdtColN];
					
					if(orfCol != -1) {
						rowLabels[0] = lineAsStrings[orfCol];
						
					} else {
						rowLabels[0] = "N/A";
					}
					
					if(nameCol != -1) {
						rowLabels[1] = lineAsStrings[nameCol];
						
					} else {
						rowLabels[1] = "N/A";
					}
					
					if(gweightCol != -1) {
						rowLabels[2] = lineAsStrings[gweightCol];
						
					} else {
						rowLabels[2] = "N/A";
					}
					
					labels[rowN - headerStart] = rowLabels;
	        	}
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
	
	public void replaceLabels(TVModel model, String[][] loadedLabels) {
		
		String[][] oldLabels = model.getGeneHeaderInfo().getHeaderArray();
		String[][] newLabels = oldLabels;
		
		for(int i = 0; i < oldLabels.length; i++) {
			
			newLabels[i] = findNewHeader(oldLabels[i], loadedLabels);
		}
		
		model.setGeneHeaders(newLabels);
		model.notifyObservers();	
	}
	
	public String[] findNewHeader(String[] oldLabels, String[][] loadedLabels) {
		
		String[] newHeaders = new String[oldLabels.length];
		
		for(int i = 0; i < loadedLabels.length; i++) {
			
			if(loadedLabels[i][0].equalsIgnoreCase(oldLabels[0])) {
				
				newHeaders = loadedLabels[i];
				
//				if(!hasORF) {
//					newHeaders[0] = oldLabels[0];
//				}
//				
//				if(!hasNAME) {
//					newHeaders[1] = oldLabels[1];
//				}
//				
//				if(!hasGWeight) {
//					newHeaders[2] = oldLabels[2];
//				}
				break;
			}
		}
		
		return newHeaders;
	}
	
	/**
	 * Sets the instance variable that represent a 2-dimensional String array
	 * for all the headers.
	 * @param rowN
	 * @param cdtColN
	 */
	public void setLabelArray(int rowN, int cdtColN) {
		
		labels = new String[lineNum - rowN][cdtColN];
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
