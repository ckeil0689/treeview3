package edu.stanford.genetics.treeview;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.model.TVModel;

import net.miginfocom.swing.MigLayout;
import Cluster.DataViewPanel;

/**
 * Subclass to add the initial JPanel containing some info and the data preview
 * table to the background panel.
 * 
 * @author CKeil
 * 
 */
class LoadCheckView extends JPanel {

	private static final long serialVersionUID = 1L;

	// Two Font Sizes
	private static Font fontS = new Font("Sans Serif", Font.PLAIN, 18);
	private static Font fontL = new Font("Sans Serif", Font.PLAIN, 24);

	// Instance variables
	private int nRows;
	private int nCols;
	private JLabel label3;
	private JLabel previewLabel;
	private JLabel numColLabel;
	private JLabel numRowLabel;
	private JButton loadNewButton;
	private JButton advanceButton;
	private JPanel feedbackPanel;
	private JPanel numPanel;
	private JPanel buttonPanel;

	// Variables for checkmark
	private JLabel success;
	private JLabel icon;
	private BufferedImage labelImg;
	private ClassLoader classLoader;
	private InputStream input;

	private TVModel dataModel;
	private final DataViewPanel dataView;

	/**
	 * Constructor Setting up the layout of the panel.
	 */
	public LoadCheckView(final TVModel model) {

		this.dataModel = model;
		this.dataView = new DataViewPanel(model);

		setupLayout();
	}

	public void setupLayout() {

		setLayout(new MigLayout());
		
		removeAll();
		setBackground(GUIParams.BG_COLOR);
		
		loadNewButton = GUIParams.setButtonLayout("Load Different File", 
				null);

		advanceButton = GUIParams.setButtonLayout("Continue", 
				"forwardIcon");

		dataView.refresh();

		feedbackPanel = new JPanel();
		feedbackPanel.setLayout(new MigLayout());
		feedbackPanel.setOpaque(false);

		if (dataModel != null) {

			classLoader = Thread.currentThread().getContextClassLoader();
			input = classLoader.getResourceAsStream("checkIcon.png");

			try {

				labelImg = ImageIO.read(input);
				icon = new JLabel(new ImageIcon(labelImg));

				success = new JLabel("Great, loading was successful!");
				success.setFont(fontL);
				success.setForeground(GUIParams.TEXT);

				feedbackPanel.add(success);
				feedbackPanel.add(icon);

			} catch (final IOException e) {
				System.out.println("Icon for loading success could not" +
						"be retrieved.");
			}

			final HeaderInfo infoArray = dataModel.getArrayHeaderInfo();
			final HeaderInfo infoGene = dataModel.getGeneHeaderInfo();

			nCols = infoArray.getNumHeaders();
			nRows = infoGene.getNumHeaders();

			// Matrix Information
			numPanel = new JPanel();
			numPanel.setLayout(new MigLayout());
			numPanel.setOpaque(false);

			numColLabel = new JLabel("Columns: " + nCols);
			numColLabel.setFont(fontS);
			numColLabel.setForeground(GUIParams.TEXT);

			numRowLabel = new JLabel("Rows: " + nRows);
			numRowLabel.setFont(fontS);
			numRowLabel.setForeground(GUIParams.TEXT);

			label3 = new JLabel("Data Points: " + nCols * nRows);
			label3.setFont(fontS);
			label3.setForeground(GUIParams.TEXT);

			previewLabel = new JLabel("Sample Data Preview");
			previewLabel.setFont(fontL);
			previewLabel.setForeground(GUIParams.TEXT);

			// ButtonPanel
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new MigLayout());
			buttonPanel.setOpaque(false);

			buttonPanel.add(loadNewButton, "alignx 50%, pushx");
			buttonPanel.add(advanceButton, "alignx 50%, pushx");

			numPanel.add(numRowLabel, "span, wrap");
			numPanel.add(numColLabel, "span, wrap");
			numPanel.add(label3);

			add(feedbackPanel, "alignx 50%, pushx, span, wrap");
			add(numPanel, "span, pushx, growx, alignx 50%, width ::60%, "
					+ "wrap");
			add(previewLabel, "span, alignx 50%, pushx, wrap");
			add(dataView, "span, push, grow, alignx 50%, width ::60%, "
					+ "height ::60%, wrap");
			add(buttonPanel, "span, alignx 50%, push");

			revalidate();
			repaint();

		} else {
			
			final JLabel warning = new JLabel("Loading unsuccessful.");
			warning.setFont(fontL);
			warning.setForeground(GUIParams.RED1);

			loadNewButton = GUIParams.setButtonLayout("Load New File", null);
			loadNewButton.setBackground(GUIParams.MAIN);

			add(warning, "alignx 50%, span, wrap");
			add(loadNewButton, "alignx 50%");

			revalidate();
			repaint();
		}
	}
	
	/**
	 * Equipping the "Load New File" button with a ActionListener
	 * @param loadNew
	 */
	public void addLoadListener(ActionListener loadNew) {
		
		loadNewButton.addActionListener(loadNew);
	}
	
	/**
	 * Equipping the "Continue" button with a ActionListener
	 * @param loadNew
	 */
	public void addContinueListener(ActionListener cont) {
		
		advanceButton.addActionListener(cont);
	}
}
