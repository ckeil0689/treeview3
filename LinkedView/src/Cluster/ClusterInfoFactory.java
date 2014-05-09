package Cluster;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.StringRes;

import net.miginfocom.swing.MigLayout;

/**
 * Provides a JPanel that displays information about a selected cluster
 * method.
 * @author CKeil
 *
 */
public class ClusterInfoFactory {

	private JPanel mainPanel;
	
	public ClusterInfoFactory() {
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setOpaque(false);
	}
	
	public JPanel makeMethodInfoPanel(int methodIndex) {
		
		mainPanel.removeAll();
		
		JLabel label = new JLabel("Linkage Method Details");
		label.setFont(GUIParams.FONTL);
		label.setForeground(GUIParams.MAIN);
		
		mainPanel.add(label, "push, alignx 0%, aligny 0%, span, wrap");
		
		JLabel similarity = new JLabel("Similarity: ");
		similarity.setFont(GUIParams.FONTS);
		similarity.setForeground(GUIParams.TEXT);
		
		TextDisplay similarityText = new TextDisplay(
				getSimilarity(methodIndex));
		
		JLabel type = new JLabel("Type: ");
		type.setFont(GUIParams.FONTS);
		type.setForeground(GUIParams.TEXT);
		
		TextDisplay typeText = new TextDisplay(
				getType(methodIndex));
		
		JLabel time = new JLabel("Time: ");
		time.setFont(GUIParams.FONTS);
		time.setForeground(GUIParams.TEXT);
		
		TextDisplay timeText = new TextDisplay(
				getTime(methodIndex));
		
		JLabel advantage = new JLabel("Advantages: ");
		advantage.setFont(GUIParams.FONTS);
		advantage.setForeground(GUIParams.TEXT);
		
		TextDisplay advantageText = new TextDisplay(
				getAdvantage(methodIndex));
		
		JLabel disadvantage = new JLabel("Disadvantages: ");
		disadvantage.setFont(GUIParams.FONTS);
		disadvantage.setForeground(GUIParams.TEXT);
		
		TextDisplay disadvantageText = new TextDisplay(
				getDisadvantage(methodIndex));
		
		mainPanel.add(similarity, "pushx, alignx 0%");
		mainPanel.add(similarityText, "pushx, growx, wrap");
		
		mainPanel.add(type, "pushx, alignx 0%");
		mainPanel.add(typeText, "pushx, growx, wrap");
		
		mainPanel.add(time, "pushx, alignx 0%");
		mainPanel.add(timeText, "pushx, growx, wrap");
		
		mainPanel.add(advantage, "pushx, alignx 0%");
		mainPanel.add(advantageText, "pushx, growx, wrap");
		
		mainPanel.add(disadvantage, "pushx, alignx 0%");
		mainPanel.add(disadvantageText, "pushx, growx, wrap");
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		return mainPanel;
	}
	
	public JPanel makeTipPanel() {
		
		mainPanel.removeAll();
		
		JLabel label = new JLabel("Tip");
		label.setFont(GUIParams.FONTL);
		label.setForeground(GUIParams.MAIN);
		
		mainPanel.add(label, "pushx, span, wrap");
		
		int index = 0;
		TextDisplay tip = new TextDisplay(getTip(index));
		
		mainPanel.add(tip, "pushx, growx, wrap");
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		return mainPanel;
	}
	
	/**
	 * Gets the appropriate similarity String resource depending on which 
	 * method is selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	public String getSimilarity(int index) {
		
		String similarity;
		
		switch(index) {
			case 0: 	similarity = StringRes.clusterInfo_Single_Similarity;
						break;
			case 1: 	similarity = StringRes.clusterInfo_Avg_Similarity;
						break;
			case 2: 	similarity = StringRes.clusterInfo_Compl_Similarity;
						break;
			default: 	similarity = "N/ A";
		}
		
		return similarity;
	}
	
	/**
	 * Gets the appropriate type String resource depending on which method is
	 * selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	public String getTip(int index) {
		
		String tip;
		
		switch(index) {
			case 0: 	tip = StringRes.clusterTip_Memory;
						break;
						
			default: 	tip = "Clustering is a computationally expensive " +
					"process. Larger matrices need exponentially more time.";
		}
		
		return tip;
	}
	
	/**
	 * Gets the appropriate type String resource depending on which method is
	 * selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	public String getType(int index) {
		
		String type;
		
		switch(index) {
			case 0: 	type = StringRes.clusterInfo_Single_Type;
						break;
			case 1: 	type = StringRes.clusterInfo_Avg_Type;
						break;
			case 2: 	type = StringRes.clusterInfo_Compl_Type;
						break;
			default: 	type = "N/ A";
		}
		
		return type;
	}
	
	/**
	 * Gets the appropriate time String resource depending on which method is
	 * selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	public String getTime(int index) {
		
		String time;
		
		switch(index) {
			case 0: 	time = StringRes.clusterInfo_Single_Time;
						break;
			case 1: 	time = StringRes.clusterInfo_Avg_Time;
						break;
			case 2: 	time = StringRes.clusterInfo_Compl_Time;
						break;
			default: 	time = "N/ A";
		}
		
		return time;
	}
	
	/**
	 * Gets the appropriate advantage String resource depending on which
	 * method is selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	public String getAdvantage(int index) {
		
		String advantage;
		
		switch(index) {
			case 0: 	advantage = StringRes.clusterInfo_Single_Adv;
						break;
			case 1: 	advantage = StringRes.clusterInfo_Avg_Adv;
						break;
			case 2: 	advantage = StringRes.clusterInfo_Compl_Adv;
						break;
			default: 	advantage = "N/ A";
		}
		
		return advantage;
	}
	
	/**
	 * Gets the appropriate disadvantage String resource depending on which 
	 * method is selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	public String getDisadvantage(int index) {
		
		String disadvantage;
		
		switch(index) {
			case 0: 	disadvantage = StringRes.clusterInfo_Single_DisAdv;
						break;
			case 1: 	disadvantage = StringRes.clusterInfo_Avg_DisAdv;
						break;
			case 2: 	disadvantage = StringRes.clusterInfo_Compl_DisAdv;
						break;
			default: 	disadvantage = "N/ A";
		}
		
		return disadvantage;
	}
	
}
