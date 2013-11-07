package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CDTCreator {

	private BufferedReader reader = null;
	private File file;
	private ArrayList<List<String>> dataSet;
	
	public CDTCreator(File file) {
		
		this.file = file;
	}
	
	public void readFile() {
	
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line;
			ArrayList<String[]> dataExtract = new ArrayList<String[]>();
			
			//Reading the data separated by tab delimited \t
			while((line = reader.readLine()) != null) {
				
				String[] row = line.split("\t");
				dataExtract.add(row);
			}
			
			//Making the Array matrix to an ArrayList matrix
			dataSet = new ArrayList<List<String>>();
			
			for(String[] row : dataExtract) {
				
				ArrayList<String> newRow = new ArrayList<String>();
				
				for(String element : row) {
					
					newRow.add(element);
				}
				
				dataSet.add(newRow);
			}
			
			System.out.println("First row PRE: " + dataSet.get(0));
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			try {
				reader.close();
				
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	
	private void findLabels() {
		
	}
	
	private void generateCDT() {
		
	}
	 
}
