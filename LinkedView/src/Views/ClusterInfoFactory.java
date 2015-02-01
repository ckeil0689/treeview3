package Views;

import javax.swing.JLabel;
import javax.swing.JPanel;

import Utilities.GUIFactory;
import Utilities.StringRes;

/**
 * Provides a JPanel that displays information about a selected cluster method.
 * 
 * @author CKeil
 *
 */
public class ClusterInfoFactory {

	private static JPanel mainPanel;
	private final static int KMEANS_INDEX = 3;

	static {

		mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT, null);
	}

	/**
	 * Generates an infoPanel depending on the supplied method index that stands
	 * for the type of clustering algorithm.
	 * 
	 * @param methodIndex
	 * @return
	 */
	public static JPanel makeHierInfoPanel(final int methodIndex) {

		mainPanel.removeAll();

		final JLabel label = GUIFactory.createLabel("Linkage Method Details",
				GUIFactory.FONTL);

		mainPanel.add(label, "pushx, alignx 0%, aligny 0%, span, wrap");

		final JLabel similarity = GUIFactory.createLabel("Similarity: ",
				GUIFactory.FONTS);

		final TextDisplay similarityText = new TextDisplay(
				getSimilarity(methodIndex));

		final JLabel type = GUIFactory.createLabel("Type: ", GUIFactory.FONTS);

		final TextDisplay typeText = new TextDisplay(getType(methodIndex));

		final JLabel time = GUIFactory.createLabel("Time: ", GUIFactory.FONTS);

		final TextDisplay timeText = new TextDisplay(getTime(methodIndex));

		final JLabel advantage = GUIFactory.createLabel("Advantages: ",
				GUIFactory.FONTS);

		final TextDisplay advantageText = new TextDisplay(
				getAdvantage(methodIndex));

		final JLabel disadvantage = GUIFactory.createLabel("Disadvantages: ",
				GUIFactory.FONTS);

		final TextDisplay disadvantageText = new TextDisplay(
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
	 * Generates an infoPanel depending on the supplied method index that stands
	 * for the type of clustering algorithm.
	 * 
	 * @param methodIndex
	 * @return
	 */
	public static JPanel makeKmeansInfoPanel() {

		mainPanel.removeAll();

		final JLabel label = GUIFactory.createLabel("K-Means Details",
				GUIFactory.FONTL);

		mainPanel.add(label, "pushx, alignx 0%, aligny 0%, span, wrap");

		final JLabel similarity = GUIFactory.createLabel("Similarity: ",
				GUIFactory.FONTS);

		final TextDisplay similarityText = new TextDisplay(
				getSimilarity(KMEANS_INDEX));

		final JLabel time = GUIFactory.createLabel("Time: ", GUIFactory.FONTS);

		final TextDisplay timeText = new TextDisplay(getTime(KMEANS_INDEX));

		final JLabel advantage = GUIFactory.createLabel("Advantages: ",
				GUIFactory.FONTS);

		final TextDisplay advantageText = new TextDisplay(
				getAdvantage(KMEANS_INDEX));

		final JLabel disadvantage = GUIFactory.createLabel("Disadvantages: ",
				GUIFactory.FONTS);

		final TextDisplay disadvantageText = new TextDisplay(
				getDisadvantage(KMEANS_INDEX));

		mainPanel.add(similarity, "alignx 0%");
		mainPanel.add(similarityText, "w 400:500:, wrap");

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
	 * Gets the appropriate similarity String resource depending on which method
	 * is selected by the user (designated by index).
	 * 
	 * @param index
	 * @return
	 */
	private static String getSimilarity(final int index) {

		String similarity;

		switch (index) {
		case 0:
			similarity = StringRes.clusterInfo_Single_Similarity;
			break;
		case 1:
			similarity = StringRes.clusterInfo_Compl_Similarity;
			break;
		case 2:
			similarity = StringRes.clusterInfo_Avg_Similarity;
			break;
		case 3:
			similarity = StringRes.clusterInfo_KMeans;
			break;
		default:
			similarity = "N/ A";
			break;
		}
		return similarity;
	}

	/**
	 * Gets the appropriate type String resource depending on which method is
	 * selected by the user (designated by index).
	 * 
	 * @param index
	 * @return
	 */
	private static String getType(final int index) {

		String type;

		switch (index) {
		case 0:
			type = StringRes.clusterInfo_Single_Type;
			break;
		case 1:
			type = StringRes.clusterInfo_Compl_Type;
			break;
		case 2:
			type = StringRes.clusterInfo_Avg_Type;
			break;
		default:
			type = "N/ A";
			break;
		}
		return type;
	}

	/**
	 * Gets the appropriate time String resource depending on which method is
	 * selected by the user (designated by index).
	 * 
	 * @param index
	 * @return
	 */
	private static String getTime(final int index) {

		String time;

		switch (index) {
		case 0:
			time = StringRes.clusterInfo_Single_Time;
			break;
		case 1:
			time = StringRes.clusterInfo_Compl_Time;
			break;
		case 2:
			time = StringRes.clusterInfo_Avg_Time;
			break;
		default:
			time = "N/ A";
			break;
		}
		return time;
	}

	/**
	 * Gets the appropriate advantage String resource depending on which method
	 * is selected by the user (designated by index).
	 * 
	 * @param index
	 * @return
	 */
	private static String getAdvantage(final int index) {

		String advantage;

		switch (index) {
		case 0:
			advantage = StringRes.clusterInfo_Single_Adv;
			break;
		case 1:
			advantage = StringRes.clusterInfo_Compl_Adv;
			break;
		case 2:
			advantage = StringRes.clusterInfo_Avg_Adv;
			break;
		case 3:
			advantage = StringRes.clusterInfo_KMeans_Adv;
			break;
		default:
			advantage = "N/ A";
			break;
		}
		return advantage;
	}

	/**
	 * Gets the appropriate disadvantage String resource depending on which
	 * method is selected by the user (designated by index).
	 * 
	 * @param index
	 * @return
	 */
	private static String getDisadvantage(final int index) {

		String disadvantage;

		switch (index) {
		case 0:
			disadvantage = StringRes.clusterInfo_Single_DisAdv;
			break;
		case 1:
			disadvantage = StringRes.clusterInfo_Compl_DisAdv;
			break;
		case 2:
			disadvantage = StringRes.clusterInfo_Avg_DisAdv;
			break;
		case 3:
			disadvantage = StringRes.clusterInfo_KMeans_DisAdv;
			break;
		default:
			disadvantage = "N/ A";
			break;
		}
		return disadvantage;
	}
}
