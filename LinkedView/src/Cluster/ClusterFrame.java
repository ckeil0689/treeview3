/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: HeaderFinder.java,v $
 * $Revision: 1.1 $
 * $Date: 2009-08-26 11:48:27 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package Cluster;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.TreeViewFrame;
/**
 * This class describes the GUI for the Cluster Application. 
 * It is separated into several panels which are
 * generated and painted when the user presses the related button.
 * There are also methods bound to buttons 
 * which invoke other classes to take over the calculation of
 * clustering algorithms etc.
 * 
 * @author CKeil
 *
 */
public class ClusterFrame extends JFrame{

	private static final long serialVersionUID = 1L;
	
	//Frame and Model instance variables declared
	protected TreeViewFrame viewFrame;
	protected DataModel clusterModel;
	
	//Various GUI Panels
	private JScrollPane scrollPane;
	private JPanel mainPanel;
	private JPanel backgroundPanel;
	private JPanel optionsPanel;
	private JPanel displayPanel;
	private JLabel head1; 
	private JLabel head2;
	private JLabel head3;
	private JLabel head4;
	private JLabel titleLine;
	private JLabel description;
	private JButton close_button;
	
	private SingleInfoPanel singleInfo = new SingleInfoPanel();
	private CompleteInfoPanel completeInfo = new CompleteInfoPanel();
	private AverageInfoPanel averageInfo = new AverageInfoPanel();
	private CentroidInfoPanel centroidInfo = new CentroidInfoPanel();
	
	private final Color BLUE1 = new Color(60, 180, 220, 255);
	private final Color RED1 = new Color(240, 80, 50, 255);
	
	private final String singleClosed = "Single Linkage";
	private final String completeClosed = "Complete Linkage";
	private final String averageClosed = "Average Linkage";
	private final String centroidClosed = "Centroid Linkage";
	
	//Constructor
	protected ClusterFrame(TreeViewFrame f, String title, DataModel dataModel) { 
	  
		//Inherit constructor from JFrame, title passed from ClusterFrameWindow  
		super(title);
		
		//Initialize instance variables
		this.viewFrame = f;
		this.clusterModel = dataModel;
		
		//setup frame options
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setResizable(true);
		
		//Makes the frame invisible when the window is closed
		this.addWindowListener(new WindowAdapter () {
			
			@Override
			public void windowClosing(WindowEvent we) {
			    
				setVisible(false);
			}
		});
		
		//Set layout for initial window
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(Color.white);
		
		backgroundPanel = new JPanel();
		backgroundPanel.setLayout(new MigLayout());
		backgroundPanel.setPreferredSize(viewFrame.getSize());
		
		titleLine = new JLabel("Cluster Methods Information");
		titleLine.setFont(new Font("Sans Serif", Font.BOLD, 35));
		
		description = new JLabel("Choose a method to find out more about it.");
		description.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setOpaque(false);
		
		displayPanel = new JPanel();
		displayPanel.setLayout(new MigLayout());
		displayPanel.setOpaque(false);
		displayPanel.setBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		close_button = new JButton("Close");
		close_button.setOpaque(true);
		close_button.setBackground(RED1);
		close_button.setForeground(Color.white);
		Dimension d = close_button.getPreferredSize();
		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
		close_button.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		close_button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				ClusterFrame.this.dispose();
				
			}
		});
		
		head1 = setupHeadLabel(singleClosed);
		head2 = setupHeadLabel(completeClosed);
		head3 = setupHeadLabel(averageClosed);
		head4 = setupHeadLabel(centroidClosed);
		
		addMListener(head1, singleInfo);
		addMListener(head2, completeInfo);
		addMListener(head3, averageInfo);
		addMListener(head4, centroidInfo);
	
		optionsPanel.add(head1, "pushx, growx, span, wrap");
		optionsPanel.add(head2, "pushx, growx, span, wrap");
		optionsPanel.add(head3, "pushx, growx, span, wrap");
		optionsPanel.add(head4, "pushx, growx, span");
		
		mainPanel.add(titleLine, "pushx, growx, span, wrap");
		mainPanel.add(description, "pushx, growx, span, wrap");
		mainPanel.add(optionsPanel, "aligny 0%");
		mainPanel.add(displayPanel, "pushx, growx");
		
		backgroundPanel.add(mainPanel, "pushx, growx, wrap");
		backgroundPanel.add(close_button, "alignx 50%, pushx");
		
		//make scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(backgroundPanel, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		//Add the mainPanel to the ContentPane
		getContentPane().add(scrollPane);
		
		//packs items so that the frame fits them
		pack();
		
		//Center JFrame on screen, needs to be used after pack();
		this.setLocationRelativeTo(null);
    }
	
	
	//Information panels to be shown when labels are clicked
	/**
	 * This subclass makes up a JPanel which displays the information
	 * relevant for the Single Linkage Clustering method.
	 * @author CKeil
	 *
	 */
	class SingleInfoPanel extends JPanel {	
		
		private static final long serialVersionUID = 1L;
		
		private String fileName = "Slink.png";
		private String desContent = "All elements are initially " +
				"their own cluster. At every step the elements with" +
				" the shortest distance between them are combined to" +
				" form a new cluster.";
		private String adv = "Add advantages....";
		private String dis = "The 'chaining phenomenon' " +
				"causes the resulting clusters to be extremely " +
				"heterogeneous as they gradually grow. It may become " +
				"difficult to define useful subdivisions of the data.";
		    
		public SingleInfoPanel() {
			
			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}

	/**
	 * This subclass makes up a JPanel which displays the information
	 * relevant for the Complete Linkage Clustering method.
	 * @author CKeil
	 *
	 */
	class CompleteInfoPanel extends JPanel {	

		private static final long serialVersionUID = 1L;
		
		private String fileName = "Slink.png";
		private String desContent = "";
		private String adv = "";
		private String dis = "";
		
		public CompleteInfoPanel() {
			
			setupClassContent(this, fileName, desContent, adv, dis);	
		}
	}
	
	/**
	 * This subclass makes up a JPanel which displays the information
	 * relevant for the average Linkage Clustering method.
	 * @author CKeil
	 *
	 */
	class AverageInfoPanel extends JPanel {	

		private static final long serialVersionUID = 1L;
		
		private String fileName = "Slink.png";
		private String desContent = "";
		private String adv = "";
		private String dis = "";
		
		public AverageInfoPanel() {
			
			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}
	
	/**
	 * This subclass makes up a JPanel which displays the information
	 * relevant for the Centroid Linkage Clustering method.
	 * @author CKeil
	 *
	 */
	class CentroidInfoPanel extends JPanel {	

		private static final long serialVersionUID = 1L;
		
		private String fileName = "Slink.png";
		private String desContent = "";
		private String adv = "";
		private String dis = "";
		
		public CentroidInfoPanel() {
			
			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}
	
	
	//Methods
	public JTextArea setupContentField(String content) {
		
		JTextArea field = new JTextArea(content);
		field.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		field.setLineWrap(true);
		field.setBorder(null);
		field.setOpaque(false);
		
		return field;
	}
	
	public JLabel setupContentLabel(String content) {
		
		JLabel head = new JLabel(content);
		head.setFont(new Font("Sans Serif", Font.BOLD, 18));
		head.setOpaque(false);
		
		return head;
	}
	
	public JLabel setupHeadLabel(String closedText){
		
		JLabel head = new JLabel(closedText);
		head.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		head.setForeground(BLUE1);
		
		return head;
	}
	
	public void setupClassContent(JPanel panel, String fileName,
			String descContent, String adv, String dis) {
		
		JTextArea description;
		JLabel advantages;
		JTextArea advantages2;
		JLabel disadvantages;
		JTextArea disadvantages2;
		JLabel icon;
		BufferedImage labelImg;
		ClassLoader classLoader;
		InputStream input;
		
		panel.setLayout(new MigLayout());
		panel.setOpaque(false);
		
		description = setupContentField(descContent);
	
		advantages = setupContentLabel("Advantages: ");
		
		advantages2 = setupContentField(adv);
		
		disadvantages = setupContentLabel("Disadvantages: ");
		
		disadvantages2 = setupContentField(dis);
		
		panel.add(description, "pushx, growx, span, wrap");
		panel.add(advantages);
		panel.add(advantages2, "pushx, growx, wrap");
		panel.add(disadvantages);
		panel.add(disadvantages2, "pushx, growx, wrap");
		
		classLoader = Thread.currentThread().getContextClassLoader();
		input = classLoader.getResourceAsStream(fileName);
		
		try {
			
			labelImg = ImageIO.read(input);
			icon = new JLabel(new ImageIcon(labelImg));
			
			panel.add(icon, "push, span, alignx 50%");
			
		} catch (IOException e) {
			
		}
	}
	
	public void addMListener(final JLabel title, final JPanel infoPanel) {
		
		title.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				displayPanel.removeAll();
				
				title.setForeground(RED1);
				displayPanel.add(infoPanel, "push, grow, span");
				
				mainPanel.revalidate();
				mainPanel.repaint();
		
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
					title.setForeground(RED1);
					title.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent arg0) {

					title.setForeground(BLUE1);
					title.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				
				title.setForeground(Color.LIGHT_GRAY);
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {}
			
		});
	}
}
