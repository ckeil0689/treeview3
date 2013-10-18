package Cluster;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Cluster.ClusterFrame;

import net.miginfocom.swing.MigLayout;

/**
 * This class creates clickable JPanel with different responses
 * from the MouseListener based on whether they take a DataModel when constructed
 * and if they do, which DataModel (TVModel for DendroView and ClusterModel for ClusterView).
 * 
 * @author CKeil
 *
 */
public class ClickableIcon extends JPanel {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Instance variables
	 */
	private JLabel icon;
	private BufferedImage labelImg;
	private ClassLoader classLoader;
	private InputStream input;
	private ClusterFrame frame;
	
	
	/**
	 * Main constructor considering the status of both model types
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param tvModel_gen
	 * @param clusterModel_gen
	 */
	public ClickableIcon(ClusterFrame frame, String fileName) {
		
		this.frame = frame;
		
		this.setLayout(new MigLayout());
		this.setOpaque(false);
		
		classLoader = Thread.currentThread().getContextClassLoader();
		input = classLoader.getResourceAsStream(fileName);
		
		try {
			
			labelImg = ImageIO.read(input);
			icon = new JLabel(new ImageIcon(labelImg));
			
			this.add(icon, "pushx, span, alignx 50%");
			
		} catch (IOException e) {
			
		}
		
		this.addMListener();
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
	public void addMListener(){
		
		this.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				frame.setVisible(true);
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				
				ClickableIcon.this.setCursor(
						new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				
				ClickableIcon.this.setCursor(
						new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
	}
}