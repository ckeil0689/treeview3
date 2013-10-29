package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * This class is used to save data from clustering to the local storage 
 * by converting it to a tab-delimited string
 * and using a BufferedWriter.
 * @author CKeil
 *
 */
public class ClusterFileWriter {
	
	private File file;
	private ClusterModel model;
	private final String SEPARATOR = "\t";
	private final String END_OF_ROW = "\n";
	
	public ClusterFileWriter(ClusterModel model){
		
		this.model = model;
	}
	
	/**
	 * This methods writes the string from the doParse() method to 
	 * local storage using the original name of the file and the 
	 * specified file extension.
	 * @param input
	 * @param fileEnd
	 */
	public void writeFile(List<List<String>> input, String fileEnd){

		String content = doParse(input);

		try{
			file = new File(model.getSource().substring(0, 
					model.getSource().length()- 4) + fileEnd);

			file.createNewFile();
				
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(content);
			bw.close();
			System.out.println("Done." + file.getAbsolutePath());
		
		} catch(IOException e){
			
		}
	}
	
	/**
	 * A method to parse the String matrix into a tab-delimited string
	 * @param input
	 * @return
	 */
	public String doParse(List<List<String>> input){
		
		StringBuilder sb = new StringBuilder();
		
		for(List<String> element : input){

			int last = element.size() - 1;
			
			for(int i = 0; i < last; i++){
				
				sb.append(element.get(i));
				sb.append(SEPARATOR);
			}
			
			sb.append(element.get(last));
			sb.append(END_OF_ROW);
		}
		
		return sb.toString();
	}
	
	public String getFilePath(){
		
		return file.getAbsolutePath();
	}
	
	public File getFile(){
		
		return file;
	}
	
}
