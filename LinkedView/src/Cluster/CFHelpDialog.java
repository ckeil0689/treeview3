package Cluster;

import java.awt.Font;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

public class CFHelpDialog extends JDialog{
	
	public static final int DOPTION_1 = 1;
	public static final int DOPTION_2 = 2;
	public static final int DOPTION_3 = 3;
	public static final int DOPTION_4 = 4;
	public static final int DOPTION_5 = 5;
	public static final int DOPTION_6 = 6;
	public static final int DOPTION_7 = 7;
	
	private int currentDialog;
	
	public CFHelpDialog(int dialogOption){
		this.currentDialog = dialogOption;
		
		JFrame frame = new JFrame();
		this.setTitle("Information");
		
		frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new MigLayout());
		JTextArea textArea;
		
		switch(currentDialog){
			
		case 1: textArea = new JTextArea("This option removes elements with a specified percentage of missing values.");
    			textArea.setFont(new Font("Sans Serif", Font.PLAIN, 16));
    			textArea.setLineWrap(true);
    			textArea.setOpaque(false);
    			textArea.setEditable(false);
    			
    			textPanel.add(textArea, "grow, push");
    			break;
    			
		case 2: textArea = new JTextArea("This option removes elements with a standard deviation " +
				"of less than the user specified value.");
				textArea.setFont(new Font("Sans Serif", Font.PLAIN, 16));
				textArea.setLineWrap(true);
				textArea.setOpaque(false);
				textArea.setEditable(false);
				
				textPanel.add(textArea, "grow, push");
				break;
		
		case 3: textArea = new JTextArea("This option removes all elements that do not have at least a certain number " +
				"of observations  with absolute values greater than a user specified amount.");
				textArea.setFont(new Font("Sans Serif", Font.PLAIN, 16));
				textArea.setLineWrap(true);
				textArea.setOpaque(false);
				textArea.setEditable(false);
				
				textPanel.add(textArea, "grow, push");
				break;
		
		case 4: textArea = new JTextArea("Removes all elements with a difference between maximum and " +
				"minimum data values less or equal than a user specified amount.");
				textArea.setFont(new Font("Sans Serif", Font.PLAIN, 16));
				textArea.setLineWrap(true);
				textArea.setOpaque(false);
				textArea.setEditable(false);
				
				textPanel.add(textArea, "grow, push");
				break;
		}
		
		this.add(textPanel);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

}
