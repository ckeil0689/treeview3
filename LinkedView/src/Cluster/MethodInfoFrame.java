/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: HeaderFinder.java,v $
 * $Revision: 1.1 $
 * $Date: 2009-08-26 11:48:27 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. 
 * Modified by Alex Segal 2004/08/13. 
 * Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name 
 * and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the 
 * Java TreeView maintainers at alok@genome.stanford.edu when they make 
 * a useful addition. 
 * It would be nice if significant contributions could be merged 
 * into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package Cluster;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.core.ScrollablePanel;

/**
 * This frame is a GUI which contains explanation of cluster methods along with
 * useful information to help people understand the advantages and disadvantages
 * of various clustering methods.
 * 
 * @author CKeil
 * 
 */
public class MethodInfoFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	// Frame and Model instance variables declared
	protected TreeViewFrame viewFrame;

	// Various GUI Panels
	private final JScrollPane scrollPane;
	private final ScrollablePanel mainPanel;
	private final JPanel optionsPanel;
	private final JPanel displayPanel;
	private final JLabel head1;
	private final JLabel head2;
	private final JLabel head3;
	private final JLabel head4;
	private final JLabel titleLine;
	private final TextDisplay description;
	private final JButton close_button;

	private final SingleInfoPanel singleInfo = new SingleInfoPanel();
	private final CompleteInfoPanel completeInfo = new CompleteInfoPanel();
	private final AverageInfoPanel averageInfo = new AverageInfoPanel();
	private final CentroidInfoPanel centroidInfo = new CentroidInfoPanel();

	private final String singleClosed = "Single Linkage";
	private final String completeClosed = "Complete Linkage";
	private final String averageClosed = "Average Linkage";
	private final String centroidClosed = "Centroid Linkage";

	// Constructor
	protected MethodInfoFrame(final TreeViewFrame f, final String title) {

		// Inherit constructor from JFrame, title passed from ClusterFrameWindow
		super(title);

		// Initialize instance variables
		this.viewFrame = f;

		// Setting preferred size for the mainPanel
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension mainDim = toolkit.getScreenSize();
		final Rectangle rectangle = new Rectangle(mainDim);
		mainDim.setSize(rectangle.height, rectangle.height * 3 / 4);

		// setup frame options
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);

		// Makes the frame invisible when the window is closed
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent we) {

				MethodInfoFrame.this.dispose();
			}
		});

		// Set layout for initial window
		mainPanel = new ScrollablePanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.setBackground(GUIParams.BG_COLOR);
		mainPanel.setPreferredSize(mainDim);

		titleLine = new JLabel("Cluster Methods Information");
		titleLine.setFont(new Font("Sans Serif", Font.BOLD, 35));
		titleLine.setForeground(GUIParams.TEXT);

		description = new TextDisplay(
				"Choose a method to find out more "
						+ "about it. All described methods are agglomerative "
						+ "hierarchical clustering, meaning each data point starts as "
						+ "its own cluster. These methods make it possible to generate "
						+ "dendrograms which depict the sequence of cluster fusion.");
		description.setFont(new Font("Sans Serif", Font.PLAIN, 22));
		description.setForeground(GUIParams.TEXT);

		optionsPanel = new JPanel();
		optionsPanel.setLayout(new MigLayout());
		optionsPanel.setOpaque(false);

		displayPanel = new ScrollablePanel();
		displayPanel.setLayout(new MigLayout());
		displayPanel.setOpaque(false);
		displayPanel.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.LOWERED));
		displayPanel.add(singleInfo, "push, grow");

		close_button = GUIParams.setButtonLayout("Close", null);
		close_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent arg0) {

				MethodInfoFrame.this.dispose();
			}
		});

		head1 = setupHeadLabel(singleClosed);
		head2 = setupHeadLabel(completeClosed);
		head3 = setupHeadLabel(averageClosed);
		head4 = setupHeadLabel(centroidClosed);

		addMListener(head1, singleInfo);
		addMListener(head2, completeInfo);
		addMListener(head3, averageInfo);
		addMListener(head4, centroidInfo);

		optionsPanel.add(head1, "pushx, growx, span, wrap");
		optionsPanel.add(head2, "pushx, growx, span, wrap");
		optionsPanel.add(head3, "pushx, growx, span, wrap");
		optionsPanel.add(head4, "pushx, growx, span, wrap");
		optionsPanel.add(close_button, "pushx, span, wrap");

		mainPanel.add(titleLine, "pushx, growx, span, wrap");
		mainPanel.add(description, "pushx, growx, span, wrap");
		mainPanel.add(optionsPanel, "aligny 0%, width 20%");
		mainPanel.add(displayPanel, "width 80%, pushx, grow");

		// make scrollable by adding it to scrollpane
		scrollPane = new JScrollPane(mainPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Add the mainPanel to the ContentPane
		getContentPane().add(scrollPane);

		// packs items so that the frame fits them
		pack();

		// Center JFrame on screen, needs to be used after pack();
		setLocationRelativeTo(viewFrame);
	}

	// Information panels to be shown when labels are clicked
	/**
	 * This subclass makes up a JPanel which displays the information relevant
	 * for the Single Linkage Clustering method.
	 * 
	 * @author CKeil
	 * 
	 */
	class SingleInfoPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String fileName = "Slink.png";
		private final String desContent = "At every step a link is made between "
				+ "the two clusters separated by the shortest distance. "
				+ "The distances between clusters are defined by the two "
				+ "elements closest to each other. Thus, a new larger cluster "
				+ "is formed.";
		private final String adv = "Add advantages....";
		private final String dis = "The 'chaining phenomenon', resulting from "
				+ "gradual addition of the closest element, sometimes forces "
				+ "two clusters together due to single elements being close "
				+ "while many other elements in the clusters may be very "
				+ "distant. This may cause the resulting clusters to be "
				+ "extremely heterogeneous. It may become difficult to define "
				+ "useful subdivisions of the data.";

		public SingleInfoPanel() {

			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}

	/**
	 * This subclass makes up a JPanel which displays the information relevant
	 * for the Complete Linkage Clustering method.
	 * 
	 * @author CKeil
	 * 
	 */
	class CompleteInfoPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String fileName = "Slink.png";
		private final String desContent = "At every step a link is made between "
				+ "the two clusters separated by the shortest distance. "
				+ "The distances between clusters are defined by the two "
				+ "elements furthest away from each other. Thus, a new larger "
				+ "cluster is formed.";
		private final String adv = "Avoids chaining phenomenon from the single "
				+ "linkage method. Tends to find compact clusters with "
				+ "approximately equal diameters.";
		private final String dis = "";

		public CompleteInfoPanel() {

			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}

	/**
	 * This subclass makes up a JPanel which displays the information relevant
	 * for the average Linkage Clustering method.
	 * 
	 * @author CKeil
	 * 
	 */
	class AverageInfoPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String fileName = "Slink.png";
		private final String desContent = "At every step a link is made between "
				+ "the two elements (one per cluster) which are closest "
				+ "to each other. The distance between two clusters is defined "
				+ "as the mean distance between their elements."
				+ "When two clusters are combined, a new larger cluster "
				+ "is formed.";
		private final String adv = "";
		private final String dis = "";

		public AverageInfoPanel() {

			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}

	/**
	 * This subclass makes up a JPanel which displays the information relevant
	 * for the Centroid Linkage Clustering method.
	 * 
	 * @author CKeil
	 * 
	 */
	class CentroidInfoPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final String fileName = "Slink.png";
		private final String desContent = "";
		private final String adv = "";
		private final String dis = "";

		public CentroidInfoPanel() {

			setupClassContent(this, fileName, desContent, adv, dis);
		}
	}

	// Methods
	public TextDisplay setupContentField(final String content) {

		final TextDisplay field = new TextDisplay(content);
		field.setFont(new Font("Sans Serif", Font.PLAIN, 18));

		return field;
	}

	public JLabel setupContentLabel(final String content) {

		final JLabel head = new JLabel(content);
		head.setFont(new Font("Sans Serif", Font.BOLD, 18));
		head.setForeground(GUIParams.TEXT);
		head.setOpaque(false);

		return head;
	}

	public JLabel setupHeadLabel(final String closedText) {

		final JLabel head = new JLabel(closedText);
		head.setFont(new Font("Sans Serif", Font.PLAIN, 28));
		head.setForeground(GUIParams.ELEMENT);

		return head;
	}

	public void setupClassContent(final JPanel panel, final String fileName,
			final String descContent, final String adv, final String dis) {

		TextDisplay description;
		JLabel advantages;
		TextDisplay advantages2;
		JLabel disadvantages;
		TextDisplay disadvantages2;
		JLabel icon;
		BufferedImage labelImg;
		ClassLoader classLoader;
		InputStream input;

		panel.setLayout(new MigLayout());
		panel.setOpaque(false);

		description = setupContentField(descContent);

		advantages = setupContentLabel("Advantages: ");

		advantages2 = setupContentField(adv);

		disadvantages = setupContentLabel("Disadvantages: ");

		disadvantages2 = setupContentField(dis);

		panel.add(description, "pushx, growx, span, wrap");
		panel.add(advantages);
		panel.add(advantages2, "pushx, growx, wrap");
		panel.add(disadvantages);
		panel.add(disadvantages2, "pushx, growx, wrap");

		classLoader = Thread.currentThread().getContextClassLoader();
		input = classLoader.getResourceAsStream(fileName);

		try {

			labelImg = ImageIO.read(input);
			icon = new JLabel(new ImageIcon(labelImg));

			panel.add(icon, "push, span");

		} catch (final IOException e) {

		}
	}

	public void addMListener(final JLabel title, final JPanel infoPanel) {

		title.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(final MouseEvent arg0) {

				displayPanel.removeAll();

				title.setForeground(GUIParams.RED1);
				displayPanel.add(infoPanel, "push, grow");

				mainPanel.revalidate();
				mainPanel.repaint();

			}

			@Override
			public void mouseEntered(final MouseEvent arg0) {

				title.setForeground(GUIParams.LIGHTGRAY);
				title.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(final MouseEvent arg0) {

				title.setForeground(GUIParams.ELEMENT);
				title.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mousePressed(final MouseEvent arg0) {

				title.setForeground(Color.LIGHT_GRAY);
			}

			@Override
			public void mouseReleased(final MouseEvent arg0) {
			}

		});
	}
}
