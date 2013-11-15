package edu.stanford.genetics.treeview;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.model.TVModel;

import net.miginfocom.swing.MigLayout;

/**
 * This class creates clickable JPanel with different responses
 * from the MouseListener based on whether they take a DataModel when constructed
 * and if they do, which DataModel (TVModel for DendroView and ClusterModel for ClusterView).
 * 
 * @author CKeil
 *
 */
public class ClickableLabel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Instance variables
	 */
	private JLabel label;
	private TreeViewFrame frame;
	private String labelText;
	
	private Color BLUE1 = new Color(118, 193, 228, 255);
	
	/**
	 * Static variables
	 */
	private static TVModel tvModel = null;
	
	/**
	 * Chained constructor for the loading icons
	 * @param frame
	 * @param labelText
	 * @param fileName
	 */
	public ClickableLabel(TreeViewFrame frame, String labelText) {
		
		this(frame, labelText, tvModel);
	}
	
	/**
	 * Main constructor considering the status of both model types
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param tvModel_gen
	 * @param clusterModel_gen
	 */
	public ClickableLabel(TreeViewFrame frame, String labelText, 
			TVModel tvModel_gen) {
		
		this.frame = frame;
		
		this.setLayout(new MigLayout());
		this.setOpaque(false);
		
		this.labelText = labelText;
		
		label = new JLabel(labelText);
		label.setFont(new Font("Sans Serif", Font.PLAIN, 50));
		label.setForeground(BLUE1);

		this.add(label, "pushx, span, alignx 50%");
		this.addMListener(label, tvModel_gen);
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
	public void addMListener(final JLabel label, final TVModel tModel){
		
		this.addMouseListener(new SSMouseListener(this, label){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				if(tModel == null) {
					
					frame.openFile();
					
				} else if(tModel != null && labelText.equalsIgnoreCase(
						"Visualize >")){
					
					frame.setDataModel(tModel, false);
					frame.setLoaded(true);
					
				} else{
					
					frame.setDataModel(tModel, true);
					frame.setLoaded(true);
				}
			}
		});
	}
}
