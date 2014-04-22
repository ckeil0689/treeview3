//package Views;
//
//import java.awt.event.ActionListener;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.io.InputStream;
//
//import javax.imageio.ImageIO;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//
//import edu.stanford.genetics.treeview.GUIParams;
//import edu.stanford.genetics.treeview.HeaderInfo;
//import edu.stanford.genetics.treeview.model.TVModel;
//
//import net.miginfocom.swing.MigLayout;
//import Cluster.DataViewPanel;
//
///**
// * Subclass to add the initial JPanel containing some info and the data preview
// * table to the background panel.
// * 
// * @author CKeil
// * 
// */
//public class LoadCheckView {
//
//	// Instance variables
//	private JPanel loadCheckPanel;
//	
//	private JButton loadNewButton;
//	private JButton advanceButton;
//
//	private TVModel tvModel;
//	private final DataViewPanel dataView;
//
//	/**
//	 * Constructor Setting up the layout of the panel.
//	 */
//	public LoadCheckView(final TVModel model) {
//
//		this.tvModel = model;
//		this.dataView = new DataViewPanel(model);
//	}
//
//	/**
//	 * Returns the JPanel which contains the content of LoadCheckView.
//	 */
//	public JPanel makeLoadCheckView() {
//
//		// Variables
//		JPanel feedbackPanel;
//		JPanel numPanel;
//		JPanel buttonPanel;
//		
//		JLabel label3;
//		JLabel previewLabel;
//		JLabel numColLabel;
//		JLabel numRowLabel;
//		
//		int nRows;
//		int nCols;
//		
//		// Loading checkmark image
//		JLabel success;
//		JLabel icon;
//		BufferedImage labelImg;
//		ClassLoader classLoader;
//		InputStream input;
//		
//		loadCheckPanel = new JPanel();
//		loadCheckPanel.setLayout(new MigLayout());
//		loadCheckPanel.setBackground(GUIParams.BG_COLOR);
//		
//		loadCheckPanel.removeAll();
//		
//		loadNewButton = GUIParams.setButtonLayout("Load Different File", 
//				null);
//
//		advanceButton = GUIParams.setButtonLayout("Continue", 
//				"forwardIcon");
//
//		dataView.refresh();
//
//		feedbackPanel = new JPanel();
//		feedbackPanel.setLayout(new MigLayout());
//		feedbackPanel.setOpaque(false);
//
//		if (tvModel != null) {
//
//			classLoader = Thread.currentThread().getContextClassLoader();
//			input = classLoader.getResourceAsStream("checkIcon.png");
//
//			try {
//
//				labelImg = ImageIO.read(input);
//				icon = new JLabel(new ImageIcon(labelImg));
//
//				success = new JLabel("Great, loading was successful!");
//				success.setFont(GUIParams.FONTL);
//				success.setForeground(GUIParams.TEXT);
//
//				feedbackPanel.add(success);
//				feedbackPanel.add(icon);
//
//			} catch (final IOException e) {
//				System.out.println("Icon for loading success could not" +
//						"be retrieved.");
//			}
//
//			final HeaderInfo infoArray = tvModel.getArrayHeaderInfo();
//			final HeaderInfo infoGene = tvModel.getGeneHeaderInfo();
//
//			nCols = infoArray.getNumHeaders();
//			nRows = infoGene.getNumHeaders();
//
//			// Matrix Information
//			numPanel = new JPanel();
//			numPanel.setLayout(new MigLayout());
//			numPanel.setOpaque(false);
//
//			numColLabel = new JLabel("Columns: " + nCols);
//			numColLabel.setFont(GUIParams.FONTS);
//			numColLabel.setForeground(GUIParams.TEXT);
//
//			numRowLabel = new JLabel("Rows: " + nRows);
//			numRowLabel.setFont(GUIParams.FONTS);
//			numRowLabel.setForeground(GUIParams.TEXT);
//
//			label3 = new JLabel("Data Points: " + nCols * nRows);
//			label3.setFont(GUIParams.FONTS);
//			label3.setForeground(GUIParams.TEXT);
//
//			previewLabel = new JLabel("Sample Data Preview");
//			previewLabel.setFont(GUIParams.FONTL);
//			previewLabel.setForeground(GUIParams.TEXT);
//
//			// ButtonPanel
//			buttonPanel = new JPanel();
//			buttonPanel.setLayout(new MigLayout());
//			buttonPanel.setOpaque(false);
//
//			buttonPanel.add(loadNewButton, "alignx 50%, pushx");
//			buttonPanel.add(advanceButton, "alignx 50%, pushx");
//
//			numPanel.add(numRowLabel, "span, wrap");
//			numPanel.add(numColLabel, "span, wrap");
//			numPanel.add(label3);
//
//			loadCheckPanel.add(feedbackPanel, "alignx 50%, pushx, span, wrap");
//			loadCheckPanel.add(numPanel, "span, pushx, growx, alignx 50%, " +
//					"width ::60%, wrap");
//			loadCheckPanel.add(previewLabel, "span, alignx 50%, pushx, wrap");
//			loadCheckPanel.add(dataView.getDataView(), "span, push, grow, " +
//					"alignx 50%, width ::60%, height ::60%, wrap");
//			loadCheckPanel.add(buttonPanel, "span, alignx 50%, push");
//
//			loadCheckPanel.revalidate();
//			loadCheckPanel.repaint();
//
//		} else {
//			
//			final JLabel warning = new JLabel("Loading unsuccessful.");
//			warning.setFont(GUIParams.FONTL);
//			warning.setForeground(GUIParams.RED1);
//
//			loadNewButton = GUIParams.setButtonLayout("Load New File", null);
//			loadNewButton.setBackground(GUIParams.MAIN);
//
//			loadCheckPanel.add(warning, "alignx 50%, span, wrap");
//			loadCheckPanel.add(loadNewButton, "alignx 50%");
//
//			loadCheckPanel.revalidate();
//			loadCheckPanel.repaint();
//		}
//		
//		return loadCheckPanel;
//	}
//	
//	/**
//	 * Equipping the "Load New File" button with a ActionListener
//	 * @param loadNew
//	 */
//	public void addLoadListener(ActionListener loadNew) {
//		
//		loadNewButton.addActionListener(loadNew);
//	}
//	
//	/**
//	 * Equipping the "Continue" button with a ActionListener
//	 * @param loadNew
//	 */
//	public void addContinueListener(ActionListener cont) {
//		
//		advanceButton.addActionListener(cont);
//	}
//}
