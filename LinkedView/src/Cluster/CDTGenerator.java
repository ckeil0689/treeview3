package Cluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class is used to generate the .CDT tab delimited file which Java TreeView will use for visualization
 * It takes in the previously calculated data and forms String lists to make them writeable.
 * @author CKeil
 *
 */
public class CDTGenerator {

	//Instance variables
	private ClusterModel model;
	private File file;
	private String filePath, choice, choice2;
	private int buffer = 2;
	
	private List<List<Double>> sepList, cdtDataList;
	
	private String[][] rowNames, colNames;
	
	private List<List<String>> finalcdtTable, rowNameList, colNameList, 
	rowNameList2, colNameList2, cdtDataStrings;
	
	private List<String> orderedRows, orderedCols, cdtRowElement1, cdtRowElement2, cdtRowElement3;
	
	//Constructor (building the object)
	public CDTGenerator(ClusterModel model, List<List<Double>> sepList, 
			List<String> orderedRows, List<String> orderedCols, String choice, String choice2){
		
		this.model = model;
		this.sepList = sepList;
		this.orderedRows = orderedRows;
		this.orderedCols = orderedCols;
		this.choice = choice;
		this.choice2 = choice2;
	}
	
	public void generateCDT(){
    	
    	//The list of String-lists to be generated for file-writing, contains all data
    	finalcdtTable = new ArrayList<List<String>>();
    	
		//the list containing all the reorganized row-data
    	cdtDataList = new ArrayList<List<Double>>(); 
    	
    	//retrieving names and weights of row elements
    	//format: [[YAL063C, 1.0], ..., [...]]
    	rowNames = model.getGeneHeaderInfo().getHeaderArray();
    	
    	//retrieving names and weights of column elements
    	//format: [[YAL063C, 1.0], ..., [...]]
    	colNames = model.getArrayHeaderInfo().getHeaderArray();
    	
    	//first transform the String[][] to lists
    	rowNameList = new ArrayList<List<String>>();
    	colNameList = new ArrayList<List<String>>();
    	
    	rowNameList2 = new ArrayList<List<String>>();
    	colNameList2 = new ArrayList<List<String>>();
    	
    	
    	if(rowNames.length > 0){
    		
	    	for(String[] element : rowNames){
	    		
	    		rowNameList.add(Arrays.asList(element));
	    	}
    	}
    	
    	
    	if(colNames.length > 0){
    		
	    	for(String[] element : colNames){
	    		
	    		colNameList.add(Arrays.asList(element));
	    	}
    	}
    	
    	//order row data and names
    	if(!choice.contentEquals("Do Not Cluster")){
    		
	    	for(int i = 0; i < orderedRows.size(); i++){
	    		
		    	String rowElement = orderedRows.get(i);
		    	String adjusted = rowElement.replaceAll("[\\D]", "");
		    	
		    	int index = Integer.parseInt(adjusted);
		    	
		    	List<Double> rowData = sepList.get(index);
		    	
		    	cdtDataList.add(rowData);
		    	
		    	rowNameList2.add(rowNameList.get(index));
	    	}
    	}
    	else{
    		
    		rowNameList2.addAll(rowNameList);
    	}
    	
    	//order column data and names
    	if(!choice2.contentEquals("Do Not Cluster")){
    		
    		if(cdtDataList.size() == 0){
    			
    			cdtDataList.addAll(sepList);
    		}

    		for(int i = 0; i < orderedCols.size(); i++){
	    		
		    	String colElement = orderedCols.get(i);
		    	String adjusted = colElement.replaceAll("[\\D]", "");
		    	
		    	//gets index from ordered list, e.g. ARRY45X --> 45;
		    	int index = Integer.parseInt(adjusted);
		    	
		    	//going through every row
		    	for(int j = 0; j < cdtDataList.size(); j++){
		    		
		    		//swapping position in original column arrangement according to new ordered list
		    		//if Element 1 in orderedCols is ARRY45X, then element 1 and element 45 will be swapped in every row
		    		Collections.swap(cdtDataList.get(j), i, index);
		    		
		    	}
		    	
	    		//reordering names
		    	colNameList2.add(colNameList.get(index));;
	    	}
    	}
    	else{
    		
    		colNameList2.addAll(colNameList);
    	}
    	
    	//transform cdtDataFile from double lists to string lists
    	cdtDataStrings = new ArrayList<List<String>>();

    	//takes 3k ms
    	for(List<Double> element : cdtDataList){
    		
    		List<String> newStringData = new ArrayList<String>();
    		
    		for(Double element2 : element){
    			
    			newStringData.add(element2.toString());
    		}
    		
    		cdtDataStrings.add(newStringData);
    		
    	}
    	
    	//fuse them to create the final .CDT-write-ready List<List<String>>
    	finalcdtTable.addAll(cdtDataStrings);
    	
    	cdtRowElement1 = new ArrayList<String>();
    	
		if(!choice.contentEquals("Do Not Cluster")){
			
			cdtRowElement1.add("GID");
		}

		cdtRowElement1.add("ORF");
		cdtRowElement1.add("NAME");
		cdtRowElement1.add("GWEIGHT");
		
		for(int i = 0; i < colNameList2.size(); i++){
			
			cdtRowElement1.add(colNameList2.get(i).get(0));
		}
		
		finalcdtTable.add(0, cdtRowElement1);
		
		if(!choice2.contentEquals("Do Not Cluster")){
			
			buffer = 3;
			
			cdtRowElement2 = new ArrayList<String>();
			
			cdtRowElement2.add("AID");
			cdtRowElement2.add("");
			cdtRowElement2.add("");
			if(!choice.contentEquals("Do Not Cluster")){
				cdtRowElement2.add("");
			}
			
			for(int i = 0; i < orderedCols.size(); i++){
				
				cdtRowElement2.add(orderedCols.get(i));
			}
			
			finalcdtTable.add(1, cdtRowElement2);	
		}
		
		cdtRowElement3 = new ArrayList<String>();
		
		cdtRowElement3.add("EWEIGHT");
		cdtRowElement3.add("");
		cdtRowElement3.add("");
		
		if(!choice.contentEquals("Do Not Cluster")){
			
			cdtRowElement3.add("");
		}
		
		for(int i = 0; i < colNameList2.size(); i++){
			
			cdtRowElement3.add(colNameList2.get(i).get(1));
		}
		
		if(!choice2.contentEquals("Do Not Cluster")){
			
			finalcdtTable.add(2, cdtRowElement3);
		}
		else{
			
			finalcdtTable.add(1, cdtRowElement3);
		}
		
    	for(int i = 0; i < orderedRows.size(); i++){
    		
    		finalcdtTable.get(i + buffer).add(0, rowNameList2.get(i).get(0));
    		finalcdtTable.get(i + buffer).add(1, rowNameList2.get(i).get(0));
    		finalcdtTable.get(i + buffer).add(2, rowNameList2.get(i).get(1));
    		
    		if(!choice.contentEquals("Do Not Cluster")){
    			
    			finalcdtTable.get(i + buffer).add(0, orderedRows.get(i));
    		}
    			
    	}
    	
    	//save file as excel tab-delimited file
    	ClusterFileWriter dataFile = new ClusterFileWriter(model);
    	
    	//change boolean type to String file ending?
		dataFile.writeFile(finalcdtTable, ".cdt");
		
		file = dataFile.getFile();
		filePath = dataFile.getFilePath();				
    	
    }
    
    public String getFilePath(){
    	
    	return filePath;
    }
    
	public File getFile(){
		
		return file;
	}
    
}
