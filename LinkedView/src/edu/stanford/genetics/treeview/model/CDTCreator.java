package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class transforms any loaded file of non-cdt format to a cdt file
 * in order to use existing infrastructure in the JTV source code and expanding
 * the compatibility of loaded files to other delimited file types without
 * rewriting and adjusting a lot of the source code. The cdt file type used by
 * Java TreeView assumes a certain format, so any loaded file will be scanned
 * for the required data and a formatted cdt file is generated.
 * 
 * @author CKeil
 *
 */
public class CDTCreator {

	private BufferedReader reader = null;
	private File file;
	private ArrayList<List<String>> dataSet;
	
	//label positions
	private List<Integer> gidInd = new ArrayList<Integer>();
	private List<Integer> aidInd = new ArrayList<Integer>();
	private List<Integer> orfInd = new ArrayList<Integer>();
	private List<Integer> gweightInd = new ArrayList<Integer>();
	private List<Integer> eweightInd = new ArrayList<Integer>();
	private List<Integer> dataStart = new ArrayList<Integer>();
	private List<Integer> nameInd = new ArrayList<Integer>();
	
	private boolean gid = false;
	
	private String fileType = "";
	private String filePath = "";
	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";
	
	/**
	 * Constructor
	 * @param file
	 */
	public CDTCreator(File file, String fileType) {
		
		this.file = file;
		this.fileType = fileType;
	}
	
	public void createFile() {
	
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
			System.out.println("Dataset length " + dataSet.size());
			
			//find GWEIGHT and EWEIGHT
			findLabel(gidInd, "GID");
			findLabel(aidInd, "AID");
			findLabel(orfInd, "ORF");
			findLabel(nameInd, "NAME");
			findLabel(eweightInd, "EWEIGHT");
			findLabel(gweightInd, "GWEIGHT");
			
			//currently making an assumption rather than actually finding
			//the beginning of data values...
			dataStart.add(eweightInd.get(0) + 1);
			dataStart.add(gweightInd.get(1) + 1);
			
			System.out.println("GID: " + gidInd);
			System.out.println("AID: " + aidInd);
			System.out.println("ORF: " + orfInd);
			System.out.println("GWEIGHT: " + gweightInd);
			System.out.println("EWEIGHT: " + eweightInd);
			System.out.println("DataStart: " + dataStart);
			System.out.println("NAME: " + nameInd);
			
			String cdt = generateCDT();
			
			saveCDT(cdt);
			
			//System.out.println(cdt);
			
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
	
	private void findLabel(List<Integer> labelPos, String label) {
		
		//check for labels in the first 20 rows
		int threshold = 20;
		
		//find GWEIGHT and EWEIGHT
		for(List<String> row : dataSet) {
			for(String element : row) {
				
				if(element.equalsIgnoreCase(label)) {
					labelPos.add(dataSet.indexOf(row));
					labelPos.add(row.indexOf(element));
					break;
				}
			}
			
			if(labelPos.size() > 0 || dataSet.indexOf(row) > threshold) {
				if(labelPos.size() == 0) {
					labelPos.add(null);
					labelPos.add(null);
				}
				break;
			}
		}	
	}
	
	/**
	 * Composes a .cdt file to further be used by JTV from any tab-delimited
	 * file. JTV uses the specific .cdt-format with very defined row and column
	 * elements to display certain data in its various views.
	 * This function assembles such a .cdt file from the scavenged data of the
	 * loaded input file. 
	 */
	private String generateCDT() {
		
		int orfRow = orfInd.get(0);
		int orfCol = orfInd.get(1);
		int eweightRow = eweightInd.get(0);
		int eweightCol = eweightInd.get(1);
		int gweightCol = gweightInd.get(1);
		int dataCol = dataStart.get(1);
		int dataRow = dataStart.get(0);
		int gidCol = 0;
		
		if(gidInd.get(0) != null) {
			gidCol = gidInd.get(1);
			gid = true;
		}
		
		String finalCDT = "";
		
		StringBuilder sb = new StringBuilder();
		
		if(gid) {
			sb.append("GID");
			sb.append(SEPARATOR);
		}
	
		sb.append("ORF");
		sb.append(SEPARATOR);
		sb.append("NAME");
		sb.append(SEPARATOR);
		sb.append("GWEIGHT");
		sb.append(SEPARATOR);
		
		//add array name row
		List<String> arrayNames = dataSet.get(orfRow).subList(
				dataCol, dataSet.get(orfRow).size());
		
		for(int i = 0; i < arrayNames.size(); i++) {
			
			sb.append(arrayNames.get(i));
			
			if(i == arrayNames.size() - 1) {
				sb.append(END_OF_ROW);
				
			} else {
				sb.append(SEPARATOR);
			}
		}
		
		//add array id row
		if(aidInd.get(1) != null) {
			sb.append("AID");
			sb.append(SEPARATOR);
			
			for(int i = 0; i < dataCol; i++) {
				
				sb.append("");
				sb.append(SEPARATOR);
			}
			
			int aidRow = aidInd.get(0);
			
			sb.append(dataSet.get(aidRow).subList(dataCol, 
					dataSet.get(aidRow).size()));
			sb.append(END_OF_ROW);
			
		}
		
		//add EWEIGHT row
		sb.append("EWEIGHT");
		sb.append(SEPARATOR);
		
		//start at 1 because EWEIGHT takes position 0
		for(int i = eweightCol + 1; i < dataStart.get(1); i++) {
			
			sb.append("");
			sb.append(SEPARATOR);
		}
		
		for(int i = dataCol; i < dataSet.get(0).size(); i++) {
		
			sb.append(dataSet.get(eweightRow).get(i));
			
			if(i == dataSet.get(0).size() - 1) {
				sb.append(END_OF_ROW);
				
			} else {
				sb.append(SEPARATOR);
			}
		}

		//continue with each data row, just each element + data sublist values
		//for the size of the dataSet - 3 (amount of rows already filled)
		for(int i = dataRow; i < dataSet.size(); i++) {
			
			List<String> row = dataSet.get(i);
			
			if(gid) {
				sb.append(row.get(gidCol));
				sb.append(SEPARATOR);
				
			}
			
			sb.append(row.get(orfCol));
			sb.append(SEPARATOR);
			
			if(nameInd.get(0) != null) {
				sb.append(row.get(nameInd.get(1)));
				sb.append(SEPARATOR);
				
			} else {
				sb.append(row.get(orfCol));
				sb.append(SEPARATOR);
			}
			
			sb.append(row.get(gweightCol));
			sb.append(SEPARATOR);
			
			for(int j = dataCol; j < row.size(); j++) {
				
				sb.append(row.get(j));
				if(j == row.size() - 1) {
					sb.append(END_OF_ROW);
					
				} else {
					sb.append(SEPARATOR);
				}
			}
		}
		
		finalCDT = sb.toString();
		
		//System.out.println(finalCDT);
		
		return finalCDT;
	}
	
	/**
	 * Saves a cdt-file, built from the loaded file,
	 * to the same directory as the loaded file. This file will then be used
	 * in JTV!
	 */
	public void saveCDT(String content) {
		
		String fileEnd = "_adjusted.cdt";
		String fileName = file.getAbsolutePath().substring(0, 
				file.getAbsolutePath().length() - fileType.length());
		
		try{
			File file2 = new File(fileName + fileEnd);

			file2.createNewFile();
				
			FileWriter fw = new FileWriter(file2.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(content);
			bw.close();
			
			filePath = file2.getAbsolutePath();
			System.out.println("Done." + file2.getAbsolutePath());
		
		} catch(IOException e){
			
		}	
	}
	
	public String getFilePath() {
		
		return filePath;
	}
	 
}
