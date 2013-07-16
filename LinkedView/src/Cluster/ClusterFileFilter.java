package Cluster;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ClusterFileFilter extends FileFilter{

	public ClusterFileFilter(){
		
	}

	@Override
	public boolean accept(File f) {
		
		if(f.isDirectory())
		{
			return true;
		}
		
		return f.getName().endsWith(".gtr");
	}

	@Override
	public String getDescription() {
		
		return "GTR files (*.gtr)";
	}
}
