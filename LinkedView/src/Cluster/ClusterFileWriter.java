package Cluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class ClusterFileWriter {
	
	private File file;
	private JFileChooser fc;
	private JFrame frame;
	private ClusterModel model;
	
	public ClusterFileWriter(JFrame currentFrame, ClusterModel model){
		
		this.frame = currentFrame;
		this.model = model;
	}
	
	public void writeFile(List<List<String>> input, String fileEnd){
		
		String content = "";
		
		for(int i = 0; i < input.size(); i++){
			
			String listElement = "";
			String elementPart;
			
			for(int j = 0; j < input.get(i).size(); j++){
				
				if(j != input.get(i).size() - 1){
					
					elementPart = input.get(i).get(j) + "\t";
				}
				else{
					
					elementPart = input.get(i).get(j) + "\n";
				}
				
				listElement = listElement + elementPart;
				
			}
			
			content = content + listElement;
		
		}
		
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
	
}
