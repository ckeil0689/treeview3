package Cluster;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.GUIFactory;
import edu.stanford.genetics.treeview.StringRes;

/**
 * Provides a JPanel that displays information about a selected cluster
 * method.
 * @author CKeil
 *
 */
public class ClusterInfoFactory {

	private static JPanel mainPanel;
	private final static int KMEANS_INDEX = 3;
	
	static {
		
		mainPanel = GUIFactory.createJPanel(false, true, null);
	}
	
	/**
	 * Generates an infoPanel depending on the supplied method index that
	 * stands for the type of clustering algorithm.
	 * @param methodIndex
	 * @return
	 */
	public static JPanel makeHierInfoPanel(int methodIndex) {
		
		mainPanel.removeAll();
		
		JLabel label = GUIFactory.createBigLabel("Linkage Method Details");
		
		mainPanel.add(label, "pushx, alignx 0%, aligny 0%, span, wrap");
		
		JLabel similarity = GUIFactory.createSmallLabel("Similarity: ");
		
		TextDisplay similarityText = new TextDisplay(
				getSimilarity(methodIndex));
		
		JLabel type = GUIFactory.createSmallLabel("Type: ");
		
		TextDisplay typeText = new TextDisplay(
				getType(methodIndex));
		
		JLabel time = GUIFactory.createSmallLabel("Time: ");
		
		TextDisplay timeText = new TextDisplay(
				getTime(methodIndex));
		
		JLabel advantage = GUIFactory.createSmallLabel("Advantages: ");
		
		TextDisplay advantageText = new TextDisplay(
				getAdvantage(methodIndex));
		
		JLabel disadvantage = GUIFactory.createSmallLabel("Disadvantages: ");
		
		TextDisplay disadvantageText = new TextDisplay(
				getDisadvantage(methodIndex));
		
		mainPanel.add(similarity, "alignx 0%");
		mainPanel.add(similarityText, "w 400:500:, wrap");
		
		mainPanel.add(type, "alignx 0%");
		mainPanel.add(typeText, "w 400:500:, wrap");
		
		mainPanel.add(time, "alignx 0%");
		mainPanel.add(timeText, "w 400:500:, wrap");
		
		mainPanel.add(advantage, "alignx 0%");
		mainPanel.add(advantageText, "w 400:500:, wrap");
		
		mainPanel.add(disadvantage, "alignx 0%");
		mainPanel.add(disadvantageText, "w 400:500:, wrap");
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		return mainPanel;
	}
	
	/**
	 * Generates an infoPanel depending on the supplied method index that
	 * stands for the type of clustering algorithm.
	 * @param methodIndex
	 * @return
	 */
	public static JPanel makeKmeansInfoPanel() {
		
		mainPanel.removeAll();
		
		JLabel label = GUIFactory.createBigLabel("K-Means Details");
		
		mainPanel.add(label, "pushx, alignx 0%, aligny 0%, span, wrap");
		
		JLabel similarity = GUIFactory.createSmallLabel("Similarity: ");
		
		TextDisplay similarityText = new TextDisplay(
				getSimilarity(KMEANS_INDEX));
		
		mainPanel.add(similarity, "alignx 0%");
		mainPanel.add(similarityText, "w 400:500:, wrap");
		
		mainPanel.revalidate();
		mainPanel.repaint();
		
		return mainPanel;
	}
	
//	public JPanel makeTipPanel() {
//		
//		mainPanel.removeAll();
//		
//		JLabel label = GUIFactory.createBigLabel("Tip");
//		
//		mainPanel.add(label, "pushx, span, wrap");
//		
//		int index = 0;
//		TextDisplay tip = new TextDisplay(getTip(index));
//		
//		mainPanel.add(tip, "pushx, growx, wrap");
//		
//		mainPanel.revalidate();
//		mainPanel.repaint();
//		
//		return mainPanel;
//	}
	
	/**
	 * Gets the appropriate similarity String resource depending on which 
	 * method is selected by the user (designated by index).
	 * @param index
	 * @return
	 */
	private static String getSimilarity(int index) {
		
		String similarity;
		
		switch(index) {
			case 0: 	similarity = StringRes.clusterInfo_Single_Similarity;
						break;
			case 1: 	similarity = StringRes.clusterInfo_Avg_Similarity;
						break;
			case 2: 	similarity = StringRes.clusterInfo_Compl_Similarity;
						break;
			case 3: 	similarity = StringRes.clusterInfo_KMeans;
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
	private static String getType(int index) {
		
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
	private static String getTime(int index) {
		
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
	private static String getAdvantage(int index) {
		
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
	private static String getDisadvantage(int index) {
		
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
