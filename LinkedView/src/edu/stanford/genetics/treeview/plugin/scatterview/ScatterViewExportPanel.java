/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: ScatterViewExportPanel.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:49 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview.plugin.scatterview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.stanford.genetics.treeview.CancelableSettingsDialog;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.SettingsPanel;

/**
 * This class is a superclass which implements a GUI for selection of options
 * relating to output. It makes most of the relevant variables accessible to
 * subclasses through protected methods.
 */
public abstract class ScatterViewExportPanel extends JPanel implements
		SettingsPanel {
	private ConfigNode root;

	// external links
	private final ScatterView scatterView; // we're not doing anything clever
											// here...
	private FileSet sourceSet; // FileSet from which current data was
								// constructed.

	// accessors

	public FileSet getSourceSet() {
		return sourceSet;
	}

	public void setSourceSet(final FileSet fs) {
		sourceSet = fs;
		if (filePanel != null) {
			filePanel.setFilePath(getInitialFilePath());
		}
	}

	/**
	 * for communication with subclass...
	 */
	// components
	private FilePanel filePanel;
	private InclusionPanel inclusionPanel;
	private PreviewPanel previewPanel;

	// accessors for configuration information

	protected File getFile() {
		return filePanel.getFile();
	}

	public String getFilePath() {
		return filePanel.getFilePath();
	}

	public void setFilePath(final String newFile) {
		filePanel.setFilePath(newFile);
	}

	protected String getInitialExtension() {
		return ".png";
	}

	protected String getInitialFilePath() {
		String defaultPath = null;
		if (sourceSet == null) {
			defaultPath = System.getProperty("user.home")
					+ System.getProperty("file.separator") + "scatterplot"
					+ getInitialExtension();
		} else {
			defaultPath = sourceSet.getDir() + sourceSet.getRoot()
					+ getInitialExtension();
		}
		if (root == null) {
			return defaultPath;
		} else {
			return root.getAttribute("file", defaultPath);
		}
	}

	/**
	 * This export panel literally just prints out the image to the specified
	 * file, with no configuration possible.
	 */
	public ScatterViewExportPanel(final ScatterView scatterView) {
		this.scatterView = scatterView;
		setupWidgets();
		inclusionPanel.synchSelected();
		inclusionPanel.synchEnabled();
	}

	public static final void main(final String[] argv) {
		final JFrame top = new JFrame("ScatterView Export Test");
		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		final ScatterView scatterView = new ScatterView();
		mainPanel.add(scatterView, BorderLayout.CENTER);
		final JButton testB = new JButton("Export...");
		final ScatterViewExportPanel ePanel = new TestExportPanel(scatterView);
		testB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final JDialog popup = new CancelableSettingsDialog(top,
						"Export to Bitmap", ePanel);
				popup.setSize(500, 500);
				popup.setVisible(true);
			}
		});
		mainPanel.add(testB, BorderLayout.SOUTH);

		top.getContentPane().add(mainPanel);
		top.setSize(500, 500);
		top.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent windowEvent) {
				System.exit(0);
			}
		});

		top.setVisible(true);

	}

	public void bindConfig(final ConfigNode configNode) {
		root = configNode;
	}

	public ConfigNode createSubNode() {
		return root.create("File");
	}

	private void setupWidgets() {
		inclusionPanel = new InclusionPanel();
		previewPanel = new PreviewPanel();
		previewPanel.setMinimumSize(new Dimension(100, 100));

		Box upperPanel; // holds major widget panels
		upperPanel = new Box(BoxLayout.X_AXIS);
		inclusionPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		upperPanel.add(inclusionPanel);
		previewPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		upperPanel.add(previewPanel);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(upperPanel);
		filePanel = new FilePanel(getInitialFilePath());
		add(filePanel);
	}

	public int estimateHeight() {
		return scatterView.getHeight();
	}

	public int estimateWidth() {
		return scatterView.getWidth();
	}

	class PreviewPanel extends JPanel {
		JCheckBox drawPreview;

		public void updatePreview() {
			repaint();
		}

		PreviewPanel() {
			setLayout(new BorderLayout());
			add(new JLabel("Preview"), BorderLayout.NORTH);
			add(new DrawingPanel(), BorderLayout.CENTER);
			drawPreview = new JCheckBox("Draw Preview");
			drawPreview.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					updatePreview();
				}
			});
			drawPreview.setSelected(true);
			add(drawPreview, BorderLayout.SOUTH);
		}

		class DrawingPanel extends JPanel {

			@Override
			public void paintComponent(final Graphics g) {
				final Dimension size = getSize();
				int width = estimateWidth();
				int height = estimateHeight();
				if ((width == 0) || (height == 0)) {
					return;
				}
				final Image i = generateImage();
				// if the width * size.height is greater than the the height
				// *size.width
				// then if we make width = size.width, the height will be less
				// than size.height.
				if (width * size.height > height * size.width) {
					height = (height * size.width) / width;
					width = size.width;
				} else { // otherwise, the converse is true.
					width = (width * size.height) / height;
					height = size.height;
				}
				if ((drawPreview == null) || drawPreview.isSelected()) {
					g.drawImage(i, 0, 0, width, height, Color.white, null);
				} else {
					g.setColor(Color.red);
					// g.drawOval(0,0,width,height);
					final int[] xPoints = new int[4];
					final int[] yPoints = new int[4];
					xPoints[0] = 0;
					xPoints[1] = 5;
					xPoints[2] = width;
					xPoints[3] = width - 5;
					yPoints[0] = 5;
					yPoints[1] = 0;
					yPoints[2] = height - 5;
					yPoints[3] = height;

					g.fillPolygon(xPoints, yPoints, 4);
					yPoints[0] = height - 5;
					yPoints[1] = height;
					yPoints[2] = 5;
					yPoints[3] = 0;
					g.fillPolygon(xPoints, yPoints, 4);

				}
			}
		}
	}

	protected BufferedImage generateImage() {
		// Image i = createImage(estimateWidth(), estimateHeight());
		final BufferedImage i = new BufferedImage(estimateWidth(),
				estimateHeight(), BufferedImage.TYPE_INT_ARGB);

		final Graphics gr = i.getGraphics();
		final Rectangle r = new Rectangle(0, 0, estimateWidth(),
				estimateHeight());
		gr.setClip(r);
		scatterView.updateBuffer(gr);
		return i;
	}

	class InclusionPanel extends JPanel {
		public void synchEnabled() {
		}

		/**
		 * This routine selects options so that they make sense with respect to
		 * the current data in the dendrogram. It should be called during
		 * initialization before synchEnabled()
		 */
		public void synchSelected() {
		}

		public void recalculateBbox() {
		}

		public void updateSize() {
		}

		InclusionPanel() {
			setupWidgets();
		}

		private void setupWidgets() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(new JLabel("No options supported yet."));
			add(new JLabel("Exported image will be exactly "));
			add(new JLabel("same size as screen image."));

		}
	}

	class FilePanel extends JPanel {
		private final JTextField fileField;

		String getFilePath() {
			return fileField.getText();
		}

		File getFile() {
			return new File(getFilePath());
		}

		void setFilePath(final String fp) {
			fileField.setText(fp);
			fileField.invalidate();
			fileField.revalidate();
			fileField.repaint();

		}

		public FilePanel(final String initial) {
			super();
			setLayout(new BorderLayout());
			add(new JLabel("Export To: "), BorderLayout.WEST);
			fileField = new JTextField(initial);
			add(fileField, BorderLayout.CENTER);
			final JButton chooseButton = new JButton("Browse");
			chooseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						final JFileChooser chooser = new JFileChooser();
						final int returnVal = chooser
								.showSaveDialog(ScatterViewExportPanel.this);
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							fileField.setText(chooser.getSelectedFile()
									.getCanonicalPath());
						}
					} catch (final java.io.IOException ex) {
						LogBuffer.println("Got exception " + ex);
					}
				}
			});
			add(chooseButton, BorderLayout.EAST);
		}
	}

}

class TestExportPanel extends ScatterViewExportPanel {
	TestExportPanel(final ScatterView scatterView) {
		super(scatterView);
	}

	@Override
	public void synchronizeTo() {
	}

	@Override
	public void synchronizeFrom() {
	}
}
