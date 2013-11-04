package Cluster;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
import Cluster.ClusterView.HeaderPanel;
import Cluster.ClusterView.InitialPanel;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewFrame;

/**
 * Subclass to be added to the options panel.
 * Offers the choice between different clustering methods and generates
 * the panel in which ProgressBars and calculation status are displayed.
 * @author CKeil
 *
 */
class ClusterOptionsPanel extends JPanel {	
		  
	private static final long serialVersionUID = 1L;
		
	//Instance Variables
	private TreeViewFrame viewFrame;
	private JButton cluster_button; 
	private JButton back_button;
	private JButton cancel_button;
	private JButton dendro_button;
	private JLabel status1; 
	private JLabel status2;
	private JLabel method;
	private JLabel error1;
	private JLabel error2;
	private JLabel clusters;
	private JSpinner enterRC;
	private JSpinner enterCC;
	private JLabel its;
	private JSpinner enterRIt;
	private JSpinner enterCIt;
	private JProgressBar pBar;
	private JProgressBar pBar2;
	private JProgressBar pBar3;
	private JProgressBar pBar4;
	private JPanel loadPanel;
	private JPanel choicePanel;
	private JPanel rowPanel;
	private JPanel colPanel;
	private JPanel mainPanel;
	private JPanel buttonPanel;
	private JPanel optionsPanel;
	private HeaderPanel head1;
	private InitialPanel initialPanel;
	private JComboBox clusterChoice;
	private JComboBox geneCombo;
	private JComboBox arrayCombo;
	
	private String path;
	private File file;
	private DataModel outer;
	
	private String linkageMethod = null;
	private int row_clusterN = 0;
	private int row_iterations = 0;
	private int col_clusterN = 0;
	private int col_iterations = 0;
	
	private final SwingWorker<Void, Void> worker;
	private final String[] clusterMethods = {"Single Linkage", 
			"Centroid Linkage", "Average Linkage", "Complete Linkage"};
	private final Color BLUE2 = new Color(110, 210, 255, 150);
		    
	/**
	 * Constructor
	 * Setting up layout and functionality for buttons.
	 */
	public ClusterOptionsPanel(final ClusterView cView,
			final boolean hierarchical) {
		
		this.setLayout(new MigLayout());
		this.setBackground(Color.white);
		
		this.viewFrame = cView.getViewFrame();
		this.mainPanel = cView.getMainPanel();
		this.optionsPanel = cView.getOptionsPanel();
		this.buttonPanel = cView.getButtonPanel();
		this.outer = cView.getDataModel();
		this.geneCombo = cView.getGeneCombo();
		this.arrayCombo = cView.getArrayCombo();
		this.initialPanel = cView.getInitialPanel();
		this.head1 = cView.getHeadPanel();
		
		final ClusterFrame clusterFrame = 
				new ClusterFrameWindow(viewFrame, outer);
		
		//worker thread for calculation off the EDT to give 
		//Swing elements time to update
		worker = new SwingWorker<Void, Void>() {	
			
			@Override
			public Void doInBackground() {

	        	try {
	        		//Set integers only if KMeans options are shown
	        		if(!hierarchical) {
		        		row_clusterN = (Integer) enterRC.getValue();
		        		col_clusterN = (Integer) enterCC.getValue();
		        		
		        		row_iterations = (Integer) enterRIt.getValue();
		        		col_iterations = (Integer) enterCIt.getValue();
	        		}
	        		
	        		//Setup a ClusterProcessor
	        		ClusterProcessor clusterTarget = 
	        				new ClusterProcessor( cView, pBar, pBar2, pBar3, 
	        						pBar4, linkageMethod, row_clusterN, 
	        						row_iterations, col_clusterN, 
	        						col_iterations);
	        		
	        		//Begin the actual clustering, hierarchical or kmeans
	        		clusterTarget.cluster(hierarchical);
	        		
				} catch (InterruptedException e) {
					
					
				} catch (ExecutionException e) {
					
				}
	        	
				return null;
			}
				
			@Override
			protected void done(){
				
				buttonPanel.remove(cancel_button);
				
				status1 = new JLabel("The file has been saved " +
						"in the original directory.");
				status1.setFont(new Font("Sans Serif", Font.PLAIN, 18));
				
				status2 = new JLabel("File Path: " + path);
				status2.setFont(new Font("Sans Serif", Font.ITALIC, 18));
				
				dendro_button.setEnabled(true);
				buttonPanel.add(back_button, "pushx, alignx 50%");
				buttonPanel.add(cluster_button, "pushx, alignx 50%");
				buttonPanel.add(dendro_button, "pushx, alignx 50%");
				
				loadPanel.add(status1, "growx, pushx, wrap");
				loadPanel.add(status2, "growx, pushx, wrap");
				
				mainPanel.revalidate();
				mainPanel.repaint();	
			}
		};
		
		//Panel containing the Linkage Choice
		choicePanel = new JPanel();
		choicePanel.setLayout(new MigLayout());
		choicePanel.setBackground(Color.white);
		
		rowPanel = new JPanel();
		rowPanel.setLayout(new MigLayout());
		rowPanel.setBackground(BLUE2);
		
		colPanel = new JPanel();
		colPanel.setLayout(new MigLayout());
		colPanel.setBackground(BLUE2);
		
		//Label
		method = new JLabel("Linkage Method");
		method.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		
		//Clickable Panel to call ClusterFrame
		ClickableIcon infoIcon = 
				new ClickableIcon(clusterFrame, "infoIcon.png");
    	
		//Linkage choice drop-down menu
		clusterChoice = new JComboBox(clusterMethods);
		clusterChoice = ClusterView.setComboLayout(clusterChoice);
		
		clusters = new JLabel("Clusters: ");
		clusters.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		its = new JLabel("Iterations: ");
		its.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		
		enterRC = setupSpinner();
		enterCC = setupSpinner();
		enterRIt = setupSpinner();
		enterCIt = setupSpinner();
		
		//ProgressBar Component
		loadPanel = new JPanel();
		loadPanel.setLayout(new MigLayout());
		loadPanel.setBackground(Color.white);
		loadPanel.setBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    	
  		//Button to go back to data preview
    	back_button = new JButton("< Back");
    	back_button = ClusterView.setButtonLayout(back_button);
		back_button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				mainPanel.removeAll();
				optionsPanel.removeAll();
				buttonPanel.removeAll();
				
				head1.setText("Cluster ", "Data Preview");
				
				cView.setInitialPanel();
				
				mainPanel.add(head1, "alignx 50%, pushx, wrap");
				mainPanel.add(initialPanel, "grow, push, span, wrap");
				mainPanel.add(buttonPanel, "alignx 50%, height 15%::");
				
				mainPanel.revalidate();
				mainPanel.repaint();	
			}
    	});
		
		//Button to show DendroView
    	dendro_button = new JButton("Clustergram > ");
    	dendro_button = ClusterView.setButtonLayout(dendro_button);
  		dendro_button.addActionListener(new ActionListener(){
	    		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				FileSet fileSet = new FileSet(file.getName(), 
						file.getParent() + File.separator);
				
				try {
					viewFrame.loadFileSet(fileSet);
					
				} catch (LoadException e) {
					
				}
				viewFrame.setLoaded(true);
			}
    	});
    	
    	//Button to begin Clustering
    	cluster_button = new JButton("Cluster");
    	cluster_button = ClusterView.setButtonLayout(cluster_button);
    	cluster_button.addActionListener(new ActionListener() {
	    		
			@Override
			public void actionPerformed(ActionEvent arg0) {
					
				final String choice = (String)geneCombo.getSelectedItem();
				final String choice2 = (String)arrayCombo.getSelectedItem();
				
				if(hierarchical) {
					linkageMethod = (String)clusterChoice.getSelectedItem();
				}
				
				//needs at least one box to be selected 
				//otherwise display error
				if(!choice.contentEquals("Do Not Cluster")
						||!choice2.contentEquals("Do Not Cluster")) {
					
					loadPanel.removeAll();
					dendro_button.setEnabled(false);
					buttonPanel.remove(cluster_button);
					buttonPanel.remove(back_button);
					
					//ProgressBars
					pBar = ClusterView.setPBarLayout(pBar, 
							"Row Distance Matrix");
					
					pBar2 = ClusterView.setPBarLayout(pBar2, "Row Clustering");
					
					pBar3 = ClusterView.setPBarLayout(pBar3, 
							"Column Distance Matrix");
					
					pBar4 = ClusterView.setPBarLayout(pBar4, 
							"Column Clustering");
					
					//Button to cancel process
					cancel_button = new JButton("Cancel");
					cancel_button = ClusterView.setButtonLayout(cancel_button);
					cancel_button.addActionListener(new ActionListener(){

						@Override
						public void actionPerformed(ActionEvent arg0) {
							
							worker.cancel(true);
							buttonPanel.removeAll();
							
							optionsPanel.remove(cView.getFinalPanel());
							optionsPanel.remove(loadPanel);
							
							if(hierarchical) {
								cView.setHPanel();
								
							} else { 
								cView.setKMPanel();
							}
							
							optionsPanel.add(cView.getFinalPanel(), 
									"pushx, alignx 50%, span, wrap");
							
							mainPanel.revalidate();
							mainPanel.repaint();	
						}
					});

					if(!choice.contentEquals("Do Not Cluster")
							&& !choice2.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar, "pushx, growx, span, wrap");
						loadPanel.add(pBar2, "pushx, growx, span, wrap");
						loadPanel.add(pBar3, "pushx, growx, span, wrap");
						loadPanel.add(pBar4, "pushx, growx, span, wrap");
						
					} else if(!choice.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar, "push, grow, span, wrap");
						loadPanel.add(pBar2, "push, grow, span, wrap");
						
					} else if(!choice2.contentEquals("Do Not Cluster")) {
						loadPanel.add(pBar3, "pushx, growx, span, wrap");
						loadPanel.add(pBar4, "pushx, growx, span, wrap");
					}
				
					optionsPanel.add(loadPanel, "push, grow, span, wrap");
					
					buttonPanel.add(cancel_button, "pushx, alignx 50%");
					buttonPanel.add(dendro_button, "pushx, alignx 50%");
					mainPanel.add(buttonPanel, 
							"pushx, alignx 50%, height 15%::");
					
					mainPanel.revalidate();
					mainPanel.repaint();

					//start new cluster process
					worker.execute();
					
				} else {
					error1 = new JLabel("Woah, that's too quick!");
					error1.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					error1.setForeground(new Color(240, 80, 50, 255));
					
					error2 = new JLabel("Please select either a " +
							"similarity metric for rows, columns, " +
							"or both to begin clustering!");
					error2.setFont(new Font("Sans Serif", Font.PLAIN, 22));
					
					loadPanel.add(error1, "alignx 50%, span, wrap");
					loadPanel.add(error2, "alignx 50%, span");
					optionsPanel.add(loadPanel, "alignx 50%, pushx, span");
					
					mainPanel.revalidate();
					mainPanel.repaint();
				}
			}	
    	});
    	
    	if(hierarchical) {
	    	choicePanel.add(method, "alignx 50%, pushx");
	    	choicePanel.add(infoIcon, "pushx, alignx 50%, wrap");
	    	choicePanel.add(clusterChoice, "span, alignx 50%, wrap");
	    	
    	} else {
    		rowPanel.add(clusters, "span, alignx 50%, wrap");
    		rowPanel.add(enterRC, "alignx 50%, pushx");
    		rowPanel.add(enterCC, "alignx 50%, pushx");
    		colPanel.add(its, "span, alignx 50%, wrap");
    		colPanel.add(enterRIt, "alignx 50%, pushx");
    		colPanel.add(enterCIt, "alignx 50%, pushx");
    		
    		choicePanel.add(rowPanel, "pushx, growx, alignx 50%, wrap");
    		choicePanel.add(colPanel, "pushx, growx, alignx 50%");
    	}
    	
    	dendro_button.setEnabled(false);
  		buttonPanel.add(back_button, "alignx 50%, pushx");
  		buttonPanel.add(cluster_button, "alignx 50%, pushx");
  		
    	this.add(choicePanel, "alignx 50%, push, grow, wrap");
	}
	
	/**
	 * Method to setup the look of an editable TextField
	 * @return
	 */
	public JSpinner setupSpinner() {
		
		SpinnerNumberModel amountChoice = new SpinnerNumberModel(0, 0, 5000, 1);
		JSpinner jft = new JSpinner(amountChoice);
		
		Dimension d = jft.getPreferredSize();
		d.setSize(d.getWidth(), d.getHeight()*2);
		jft.setPreferredSize(d);
		jft.setFont(new Font("Sans Serif", Font.PLAIN, 18));
		
		return jft;
	}
	
	/**
	 * Setter for file path
	 * @param filePath
	 */
	public void setPath(String filePath){
		
		path = filePath;
	}
	
	/**
	 * Setter for file
	 * @param cdtFile
	 */
	public void setFile(File cdtFile){
		
		file = cdtFile;
	}
}
