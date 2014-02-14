package edu.stanford.genetics.treeview;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class StatsPanel {

	private TreeViewFrame viewFrame;
	private JFrame statsFrame;
	
	/**
	 * Constructor
	 * @param viewFrame
	 */
	public StatsPanel(TreeViewFrame viewFrame) {
		
		statsFrame = new JFrame("Stats");
		
		this.viewFrame = viewFrame;
		
		statsFrame.setResizable(false);
		
		final Dimension mainDim = GUIParams.getScreenSize();
		
		statsFrame.getContentPane().setSize(mainDim.width * 1/2, 
				mainDim.height * 1/2);
		
		statsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		statsFrame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(final WindowEvent we) {
				
				statsFrame.dispose();
			}
		});
		
		setupLayout();
		
		statsFrame.pack();
		statsFrame.setLocationRelativeTo(viewFrame.getAppFrame());
	}
	
	/**
	 * Sets up layout and content of this window.
	 */
	public void setupLayout() {
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.BG_COLOR);
		
		JLabel header = GUIParams.setupHeader("Stats for the loaded Model");
		
		if(viewFrame.getDataModel() != null) {
			JLabel source = new JLabel("Source: " + viewFrame.getDataModel()
					.getSource());
			source.setForeground(GUIParams.TEXT);
			source.setFont(GUIParams.FONTS);
			
			JLabel cols = new JLabel("Columns: " + viewFrame.getDataModel()
					.getArrayHeaderInfo().getNumHeaders());
			cols.setForeground(GUIParams.TEXT);
			cols.setFont(GUIParams.FONTS);
			
			int rowN = viewFrame.getDataModel().getGeneHeaderInfo()
					.getNumHeaders();
			int colN = viewFrame.getDataModel().getArrayHeaderInfo()
					.getNumHeaders();
			
			JLabel rows = new JLabel("Rows: " + viewFrame.getDataModel()
					.getGeneHeaderInfo().getNumHeaders());
			rows.setForeground(GUIParams.TEXT);
			rows.setFont(GUIParams.FONTS);
			
			JLabel size = new JLabel("Matrix Size (includes N/A-values): " 
					+ (rowN * colN));
			size.setForeground(GUIParams.TEXT);
			size.setFont(GUIParams.FONTS);
			
			mainPanel.add(header, "pushx, alignx 50%, wrap");
			mainPanel.add(source, "wrap");
			mainPanel.add(rows, "wrap");
			mainPanel.add(cols, "wrap");
			mainPanel.add(size, "wrap");
		
		} else {
			JLabel nLoad = new JLabel("It appears, the Model was not loaded.");
			nLoad.setForeground(GUIParams.TEXT);
			nLoad.setFont(GUIParams.FONTS);
			
			mainPanel.add(nLoad, "push, alignx 50%");
		}
		
		statsFrame.getContentPane().add(mainPanel);
	}
	
	/**
	 * Sets the visibility of statsFrame.
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		
		statsFrame.setVisible(true);
	}
}
