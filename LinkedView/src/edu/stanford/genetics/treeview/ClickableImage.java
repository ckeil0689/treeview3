package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.model.TVModel;

import Cluster.ClusterModel;

import net.miginfocom.swing.MigLayout;

/**
 * This class creates clickable JPanel with different responses
 * from the MouseListener based on whether they take a DataModel when constructed
 * and if they do, which DataModel (TVModel for DendroView and ClusterModel for ClusterView).
 * 
 * @author CKeil
 *
 */
public class ClickableImage extends JPanel {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Instance variables
	 */
	private JLabel label, icon;
	private BufferedImage labelImg;
	private ClassLoader classLoader;
	private InputStream input;
	private TreeViewFrame frame;
	
	private Color BLUE1 = new Color(60, 180, 220, 255);
	
	/**
	 * Static variables
	 */
	private static TVModel tvModel = null;
	private static ClusterModel clusterModel = null;
	
	/**
	 * Chained constructor for the loading icons
	 * @param frame
	 * @param labelText
	 * @param fileName
	 */
	public ClickableImage(TreeViewFrame frame, String labelText, String fileName) {
		
		this(frame, labelText, fileName, tvModel, clusterModel);
	}
	
	/**
	 * Chained constructor for the DendroView
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param tvModel_gen
	 */
	public ClickableImage(TreeViewFrame frame, String labelText, String fileName, 
			TVModel tvModel_gen) {
		
		this(frame, labelText, fileName, tvModel_gen, clusterModel);
	}
	
	/**
	 * Chained constructor for the ClusterView
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param clusterModel_gen
	 */
	public ClickableImage(TreeViewFrame frame, String labelText, String fileName, 
			ClusterModel clusterModel_gen) {
		
		this(frame, labelText, fileName, tvModel, clusterModel_gen);
	}
	
	/**
	 * Main constructor considering the status of both model types
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param tvModel_gen
	 * @param clusterModel_gen
	 */
	public ClickableImage(TreeViewFrame frame, String labelText, String fileName, 
			TVModel tvModel_gen, ClusterModel clusterModel_gen) {
		
		this.frame = frame;
		
		this.setLayout(new MigLayout());
		this.setOpaque(false);
		
		label = new JLabel(labelText);
		label.setFont(new Font("Sans Serif", Font.PLAIN, 60));
		label.setForeground(BLUE1);
		
		classLoader = Thread.currentThread().getContextClassLoader();
		input = classLoader.getResourceAsStream(fileName);
		
		try {
			
			labelImg = ImageIO.read(input);
			icon = new JLabel(new ImageIcon(labelImg));
			
			this.add(label, "pushx, span, alignx 50%, wrap");
			this.add(icon, "pushx, span, alignx 50%");
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		addMListener(label, clusterModel_gen, tvModel_gen);
	}
	
	/**
	 * The MouseListener for the JPanel which adds dynamic change in color
	 * and the ability to click the JPanel.
	 * If clicked it will call the appropriate View, depending on which model the 
	 * specific object of this class has loaded in TreeView frame (the other model
	 * is then null, if it's a label to load new a new file, both models are null).
	 * @param label
	 * @param cModel
	 * @param tModel
	 */
	public void addMListener(final JLabel label, final ClusterModel cModel, 
			final TVModel tModel){
		
		this.addMouseListener(new SSMouseListener(this, label){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				if(tModel == null && cModel == null) {
					
					frame.openFile();
					
				} else if(cModel == null && tModel != null){
					
					frame.setDataModel(tModel);
					frame.setLoaded(true);
					
				} else{
					
					frame.setClusterDataModel(cModel);
					frame.setLoaded(true);
				}
			}
		});
	}
}
