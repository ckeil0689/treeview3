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
public class ClickableLabel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Instance variables
	 */

	private final TreeViewFrame frame;
	private final String labelText;

	/**
	 * Static variables
	 */
	private static TVModel tvModel = null;

	/**
	 * Chained constructor for the loading icons
	 * 
	 * @param frame
	 * @param labelText
	 * @param fileName
	 */
	public ClickableLabel(final TreeViewFrame frame, final String labelText) {

		this(frame, labelText, tvModel);
	}

	/**
	 * Main constructor considering the status of both model types
	 * 
	 * @param frame
	 * @param labelText
	 * @param fileName
	 * @param tvModel_gen
	 * @param clusterModel_gen
	 */
	public ClickableLabel(final TreeViewFrame frame, final String labelText,
			final TVModel tvModel_gen) {

		this.frame = frame;
		this.labelText = labelText;
		
		JLabel label;

		setLayout(new MigLayout());
		setOpaque(false);

		label = new JLabel(labelText);
		label.setFont(new Font("Sans Serif", Font.PLAIN, 50));
		label.setForeground(GUIParams.ELEMENT);
		
		add(label, "pushx, alignx 50%");
		addMListener(label, tvModel_gen);
	}

	/**
	 * The MouseListener for the JPanel which adds dynamic change in color and
	 * the ability to click the JPanel. If clicked it will call the appropriate
	 * View, depending on which model the specific object of this class has
	 * loaded in TreeView frame (the other model is then null, if it's a label
	 * to load new a new file, both models are null).
	 * 
	 * @param label
	 * @param cModel
	 * @param tModel
	 */
	public void addMListener(final JLabel label, final TVModel tModel) {

		this.addMouseListener(new SSMouseListener(this, label) {

			@Override
			public void mouseClicked(final MouseEvent arg0) {

				if (labelText.equalsIgnoreCase("Load Data >")) {
					try {
						frame.openFile();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else if (labelText.equalsIgnoreCase("Preferences >")) {

				}

			}
		});
	}
}
