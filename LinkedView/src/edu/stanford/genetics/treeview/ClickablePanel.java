package edu.stanford.genetics.treeview;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class creates clickable JPanel with different responses from the
 * MouseListener based on whether they take a DataModel when constructed and if
 * they do, which DataModel (TVModel for DendroView and ClusterModel for
 * ClusterView).
 * 
 * @author CKeil
 * 
 */
public class ClickablePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final JLabel label;

	/**
	 * Main constructor considering the status of both model types
	 * 
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param tvModel_gen
	 * @param clusterModel_gen
	 */
	public ClickablePanel(final String labelText) {

		setLayout(new MigLayout());
		setOpaque(false);

		label = new JLabel(labelText);
		label.setFont(new Font("Sans Serif", Font.PLAIN, 50));
		label.setForeground(GUIParams.ELEMENT);
		
		add(label, "pushx, alignx 50%");
	}
	
	/**
	 * Returns the text this panel displays.
	 * @return
	 */
	public JLabel getLabel() {
		
		return label;
	}
}
