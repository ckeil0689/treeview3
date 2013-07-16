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
	
	public ClusterFileWriter(JFrame currentFrame){
		
		this.frame = currentFrame;
	}
	
	public void writeGTRFile(List<List<String>> input){
		
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
			
			fc = new JFileChooser();
			
			fc.setFileFilter(new ClusterFileFilter());
			
			int returnVal = fc.showSaveDialog(frame);
			
			if(returnVal == JFileChooser.APPROVE_OPTION){
				
				file = new File(fc.getSelectedFile() + ".gtr");
				
			}
				
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
	
}
