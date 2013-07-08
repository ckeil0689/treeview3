package Cluster;

import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;

public class ClusterProgressBar extends JDialog{

	//Instance Variables
	JProgressBar pBar;
	static final int MY_MIN = 0;
	
	public ClusterProgressBar(String title, Frame f){
		super(f, title, true);
		//set up the Progressbar
		pBar = new JProgressBar();
		pBar.setMinimum(MY_MIN);
		pBar.setStringPainted(true);
		pBar.setIndeterminate(true);
		//Add it to JPanel Object
		JPanel panel = new JPanel();
		panel.add(pBar);
	  
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new MigLayout());
		contentPane.add(panel, "span");
		setContentPane(contentPane);
	}
	
	//method to update the bar
	public void updateBar(int newValue){
		pBar.setValue(newValue);
	}
	
	public void setLength(int max){
		pBar.setMaximum(max);
	}
}
