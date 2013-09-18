package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ClusterFileWriter {
	
	private File file;
	private ClusterModel model;
	
	public ClusterFileWriter(ClusterModel model){
		
		this.model = model;
	}
	
	public void writeFile(List<List<String>> input, String fileEnd){
		
		String content = "";
		
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		long ms = System.currentTimeMillis();
		
		for(List<String> element : input){
			
			String listElement = "";
			String elementPart = "";

			int last = element.size() - 1;
			
			for(int i = 0; i < last; i++){
				
//				if(j != last){
//					
//					elementPart = row.get(j) + "\t";
//				}
//				else{
//					
//					elementPart = row.get(j) + "\n";
//				}
				
				elementPart = element.get(i) + "\t";
				
				listElement = listElement + elementPart;
			}
			
			elementPart = element.get(last) + "\n";
			listElement = listElement + elementPart;
			
			content = content + listElement;
		}
		
		System.out.println("Generating Content: " + (System.currentTimeMillis() - ms));
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		
		try{
			
			file = new File(model.getSource().substring(0, model.getSource().length()- 4) + fileEnd);

			file.createNewFile();
				
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(content);
			bw.close();
			System.out.println("Done." + file.getAbsolutePath());
		
		} catch(IOException e){
			
			e.printStackTrace();
			
		}
	}
	
	public String getFilePath(){
		
		return file.getAbsolutePath();
	}
	
	public File getFile(){
		
		return file;
	}
	
}
