/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ColorBarExportPanel.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-06-11 01:58:57 $
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
package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.ConfigNodePersistent;
import edu.stanford.genetics.treeview.DataMatrix;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.RotateImageFilter;

/**
 * This class is a superclass which implements a GUI for selection of options
 * relating to output. It makes most of the relevant variables accessible to
 * subclasses through protected methods.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.2 $ $Date: 2008-06-11 01:58:57 $
 */
public abstract class ColorBarExportPanel extends javax.swing.JPanel implements
		ConfigNodePersistent {

	private static final long serialVersionUID = 1L;

	private ConfigNode root;

	// external links
	private FileSet sourceSet;// FileSet from which current data was
								// constructed.
	private ColorExtractor colorExtractor;

	/**
	 * The ColorExtractor is used to determine colors for the color bar.
	 */
	public void setColorExtractor(final ColorExtractor colorExtractor) {
		this.colorExtractor = colorExtractor;
	}

	/**
	 * The ColorExtractor is used to determine colors for the global and zoom
	 * views.
	 */
	public ColorExtractor getColorExtractor() {
		return colorExtractor;
	}

	/** margin around actual stuff, to make images look good. */
	protected final int extraWidth = 5;
	/** margin around actual stuff, to make images look good. */
	protected final int extraHeight = 5;

	/**
	 * The sourceSet is used to suggest a file name for the exported image.
	 */
	public FileSet getSourceSet() {
		return sourceSet;
	}

	/**
	 * The sourceSet is used to suggest a file name for the exported image.
	 */
	public void setSourceSet(final FileSet fs) {
		sourceSet = fs;
		if (filePanel != null) {
			filePanel.setFilePath(getInitialFilePath());
		}
	}

	/**
	 * for communication with subclass. Should be overridden by subclass if you
	 * don't need the Bbox configuration stuff.
	 * 
	 * @return true if need Bbox options, false otherwise.
	 */
	protected boolean hasBbox() {
		return true;
	}

	// components

	private FilePanel filePanel;
	private SettingsPanel settingsPanel;
	private PreviewPanel previewPanel;

	// accessors for configuration information
	/**
	 * Reflects user choices from GUI. Indicates whether to include an explicit
	 * bounding box. Only meaningful for postscript.
	 */
	protected boolean includeBbox() {
		return settingsPanel.useBbox();
	}

	/**
	 * Reflects user choices from GUI.
	 * 
	 * @return A file path to print image to.
	 */
	protected File getFile() {
		return filePanel.getFile();
	}

	public String getFilePath() {
		return filePanel.getFilePath();
	}

	public void setFilePath(final String newFile) {
		filePanel.setFilePath(newFile);
	}

	/**
	 * Gets the initialExtension. Should be overriden by subclasses to specift a
	 * reasonable extension for the type.
	 * 
	 * @return The initialExtension value
	 */
	protected String getInitialExtension() {
		return ".ps";
	}

	/**
	 * Gets the initialFilePath. Constructed from the sourceSet and the
	 * initialExtension.
	 * 
	 * @return The initialFilePath value
	 */
	protected String getInitialFilePath() {
		String defaultPath = null;
		if (sourceSet == null) {
			defaultPath = System.getProperty("user.home");
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
	 * Constructor for the ColorBarExportPanel object
	 * 
	 * @param colorExtractor
	 *            Description of the Parameter
	 */
	public ColorBarExportPanel(final ColorExtractor colorExtractor) {
		this.colorExtractor = colorExtractor;
		setupWidgets();
	}

	/**
	 * Simple test program.
	 * 
	 * @param argv
	 *            none required
	 */
	public final static void main(final String[] argv) {
		final ColorExtractor colorE = new ColorExtractor();
		colorE.setDefaults();
		final double contrast = colorE.getContrast();
		colorE.setMissing(contrast * 2, contrast * 2);
		final ColorBarExportPanel testExportPanel = new TestColorBarExportPanel(
				colorE);
		final JFrame test = new JFrame("Test Export Panel");
		test.getContentPane().add(testExportPanel);
		test.pack();
		test.setVisible(true);
	}

	/* inherit description */
	@Override
	public void bindConfig(final ConfigNode configNode) {
		root = configNode;
	}

	private void setupWidgets() {
		Box upperPanel;// holds major widget panels
		upperPanel = new Box(BoxLayout.X_AXIS);
		settingsPanel = new SettingsPanel();
		settingsPanel.recalculateBbox();
		upperPanel.add(settingsPanel);
		previewPanel = new PreviewPanel();
		upperPanel.add(previewPanel);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(upperPanel);
		filePanel = new FilePanel(getInitialFilePath());
		add(filePanel);
		// can add more panels below in subclass.
	}

	// drawing specific convenience methods...
	/**
	 * Reflects user choices from GUI. number of pixels to use for each box
	 * along the x
	 * 
	 * @return The xscale value
	 */
	public double getXscale() {
		return settingsPanel.getXscale();
	}

	/**
	 * Reflects user choices from GUI. number of pixels to use for each box
	 * along the y
	 * 
	 * @return The yscale value
	 */
	public double getYscale() {
		return settingsPanel.getYscale();
	}

	/**
	 * Reflects user choices from GUI. Number of decimals to print for numbers
	 * on the color bar legend.
	 * 
	 * @return The decimals value
	 */
	public int getDecimals() {
		return settingsPanel.getDecimals();
	}

	/**
	 * Reflects user choices from GUI. number of boxes to print.
	 * 
	 * @return The numBoxes value
	 */
	public int getNumBoxes() {
		return settingsPanel.getNumBoxes();
	}

	/**
	 * Reflects user choices from GUI. Should the color bar be vertical or
	 * horizontal?
	 * 
	 * @return Description of the Return Value
	 */
	public boolean drawVertical() {
		return settingsPanel.drawVertical();
	}

	/**
	 * Reflects user choices from GUI. how wide should the bbox be?
	 * 
	 * @return The bboxWidth value
	 */
	public int getBboxWidth() {
		return settingsPanel.getBboxWidth();
	}

	/**
	 * Reflects user choices from GUI. how high should the bbox be?
	 * 
	 * @return The bboxHeight value
	 */
	public int getBboxHeight() {
		return settingsPanel.getBboxHeight();
	}

	/**
	 * The font to use for the legend.
	 * 
	 * @return The font value
	 */
	@Override
	public Font getFont() {
		return new Font("Courier", 0, 12);
	}

	/**
	 * Renders a double values as a string, with the correct number of decimals
	 * specified by the user.
	 */
	public String formatValue(double value) {
		try {
			final NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(getDecimals());
			nf.setMinimumFractionDigits(getDecimals());
			return nf.format(value);
		} catch (final Exception e) {
			for (int i = 0; i < getDecimals(); i++) {
				value *= 10;
			}
			final int intVal = (int) value;
			double doubleVal = intVal;
			for (int i = 0; i < getDecimals(); i++) {
				doubleVal /= 10;
			}
			return "" + doubleVal;
		}
		/*
		 * for (int i = 0; i < getDecimals();i++) { value *= 10; } int intVal =
		 * (int) value; double doubleVal = (double) intVal; String pad ="";
		 * doubleVal /= 10; for (int i = 0; i < getDecimals();i++) { doubleVal
		 * /= 10; if (doubleVal == (int) doubleVal) { pad += "0"; } } return ""
		 * + doubleVal + pad;
		 */
	}

	/**
	 * The length of the longest number string.
	 */
	public int textLength() {
		final FontMetrics fontMetrics = getFontMetrics(getFont());
		final double contrast = getColorExtractor().getContrast();
		final int boxes = getNumBoxes();
		int maxLength = 0;
		for (int i = 0; i < boxes; i++) {
			final double val = (i * contrast * 2.0) / ((double) boxes - 1)
					- contrast;
			final int length = fontMetrics.stringWidth(formatValue(val));
			if (length > maxLength) {
				maxLength = length;
			}
		}
		return maxLength + insetVal;
	}

	/**
	 * estimated height of box graphic alone.
	 */
	public int estimateHeight() {
		if (drawVertical()) {
			return (int) (getNumBoxes() * getYscale());
		} else {
			return (int) (getXscale() + textLength());
		}
	}

	/**
	 * estimated width of box graphic alone.
	 */
	public int estimateWidth() {
		if (drawVertical()) {
			return (int) (getXscale() + textLength());
		} else {
			return (int) (getNumBoxes() * getXscale());
		}
	}

	class PreviewPanel extends JPanel {

		private static final long serialVersionUID = 1L;

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

			private static final long serialVersionUID = 1L;

			double scale;

			@Override
			public void paintComponent(final Graphics g) {
				final Dimension size = getSize();
				int width = estimateWidth();
				int height = estimateHeight();
				if ((width == 0) || (height == 0)) {
					return;
				}
				// if the width * size.height is greater than the the height
				// *size.width
				// then if we make width = size.width, the height will be less
				// than size.height.
				if (width * size.height > height * size.width) {
					height = (height * size.width) / width;
					width = size.width;
				} else {// otherwise, the converse is true.
					width = (width * size.height) / height;
					height = size.height;
				}

				scale = (double) width / estimateWidth();
				if (scale > 1.0) {
					scale = 1.0;
				}
				width = (int) (estimateWidth() * scale);
				height = (int) (estimateHeight() * scale);
				if ((drawPreview == null) || drawPreview.isSelected()) {
					/*
					 * drawBoxes(g, 0, 0, scale); if (drawVertical()) {
					 * drawNumbersBox(g, (int) (getXscale()*scale), 0 , scale);
					 * } else { drawNumbersBox(g, 0, 0 , scale); }
					 */
					final Image i = generateImage();
					g.drawImage(i, 0, 0, width, height, null);
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

	/**
	 * Generates an image using the current settings from the GUI..
	 */
	protected BufferedImage generateImage() {
		final Rectangle destRect = new Rectangle(0, 0, estimateWidth(),
				estimateHeight());
		// Image i = createImage(destRect.width + extraWidth, destRect.height +
		// extraHeight);
		final BufferedImage i = new BufferedImage(destRect.width + extraWidth,
				destRect.height + extraHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = i.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, destRect.width + 1 + extraWidth, destRect.height + 1
				+ extraHeight);
		g.setColor(Color.black);
		g.translate(extraHeight / 2, extraWidth / 2);
		drawAll(g);
		return i;
	}

	/**
	 * does the dirty work by calling methods in the superclass.
	 * 
	 */
	private void drawAll(final Graphics g) {
		final int width = estimateWidth();
		final int height = estimateHeight();
		if ((width == 0) || (height == 0)) {
			return;
		}
		final double scale = 1.0;// full size, of course...
		drawBoxes(g, 0, 0, scale);
		if (drawVertical()) {
			drawNumbers(g, (int) (getXscale() * scale), 0, scale);
		} else {
			drawNumbers(g, 0, 0, scale);
		}
	}

	final int insetVal = 5;

	/**
	 * draws the boxes. The "scale" is used if you are drawing at some other
	 * scale than the final output; otherwise the regular xscale and yscale are
	 * used.
	 * 
	 * @param g
	 *            graphics element to draw on
	 * @param x
	 *            x coordinate of upper left hand corner of graphic
	 * @param y
	 *            y coordinate of upper left hand corner of graphic
	 * @param scale
	 *            scale relative to final output size. In case we're doing a
	 *            thumbnail.
	 */
	protected void drawBoxes(final Graphics g, final int x, final int y,
			final double scale) {
		final DoubleArrayDrawer drawer = new DoubleArrayDrawer();
		drawer.setColorExtractor(getColorExtractor());
		final int boxes = getNumBoxes();
		final double[] matrix = new double[getNumBoxes()];
		final double contrast = getColorExtractor().getContrast();
		for (int i = 0; i < boxes; i++) {
			double val = (i * contrast * 2.0) / ((double) boxes - 1) - contrast;
			if (drawVertical()) {
				val = ((getNumBoxes() - i - 1) * contrast * 2.0)
						/ ((double) boxes - 1) - contrast;
			}
			matrix[i] = val;
		}

		final Rectangle source = new Rectangle();
		final Rectangle dest = new Rectangle();
		if (drawVertical()) {
			drawer.setDataMatrix(new SimpleDataMatrix(matrix, boxes, 1));
			source.width = 1;
			source.height = boxes;
			dest.x = x;
			dest.y = y;
			dest.height = (int) (getYscale() * boxes * scale);
			dest.width = (int) (getXscale() * scale);
		} else {
			drawer.setDataMatrix(new SimpleDataMatrix(matrix, boxes, 1));
			source.width = boxes;
			source.height = 1;
			dest.x = x;
			dest.y = y + (int) (textLength() * scale);
			dest.width = (int) (getXscale() * boxes * scale);
			dest.height = (int) (getYscale() * scale);
		}

		drawer.paint(g, source, dest, null);
	}

	/**
	 * draws an appropriately sized box for each scale mark at the specific
	 * location
	 * 
	 * @param g
	 *            graphics element to draw on
	 * @param x
	 *            x coordinate of upper left hand corner of graphic
	 * @param y
	 *            y coordinate of upper left hand corner of graphic
	 * @param scale
	 *            scale relative to final output size. In case we're doing a
	 *            thumbnail.
	 */
	protected void drawNumbersBox(final Graphics g, final int x, final int y,
			final double scale) {
		double width;
		double height;
		if (drawVertical()) {
			width = (textLength() * scale);
			height = (getNumBoxes() * getYscale() * scale);
		} else {
			height = (textLength() * scale);
			width = (getNumBoxes() * getXscale() * scale);
		}
		g.setColor(Color.black);
		final FontMetrics fontMetrics = getFontMetrics(getFont());
		final int numberWidth = (int) (textLength() * scale);
		final int numberHeight = (int) (fontMetrics.getHeight() * scale);
		final int inset = (int) (scale * insetVal);
		for (int i = 0; i < getNumBoxes(); i++) {
			if (drawVertical()) {
				final double spacing = height / (getNumBoxes());
				g.fillRect(x + inset, y
						+ (int) (i * spacing + (spacing - numberHeight) / 2),
						numberWidth, numberHeight);
			} else {
				final double spacing = width / (getNumBoxes());
				g.fillRect(x
						+ (int) (i * spacing + (spacing - numberHeight) / 2),
						y, numberHeight, numberWidth);
			}
		}
	}

	/**
	 * renders the text of the values for the color bar.
	 * 
	 * @param g
	 *            graphics element to draw on
	 * @param x
	 *            x coordinate of upper left hand corner of graphic
	 * @param y
	 *            y coordinate of upper left hand corner of graphic
	 * @param scale
	 *            scale relative to final output size. In case we're doing a
	 *            thumbnail.
	 */
	protected void drawNumbers(final Graphics g, final int x, final int y,
			final double scale) {
		double width;
		double height;
		if (drawVertical()) {
			width = (textLength()) * scale;
			height = getNumBoxes() * getYscale() * scale;
		} else {
			height = (textLength()) * scale;
			width = getNumBoxes() * getXscale() * scale;
		}
		g.setColor(Color.black);
		final FontMetrics fontMetrics = getFontMetrics(getFont());
		// double numberWidth = (textLength() * scale);
		final double numberHeight = (fontMetrics.getHeight() * scale);
		final double inset = (scale * insetVal);
		final double contrast = getColorExtractor().getContrast();
		final int boxes = getNumBoxes();
		if (drawVertical()) {
			final double spacing = height / (getNumBoxes());
			for (int i = 0; i < getNumBoxes(); i++) {
				final double val = ((getNumBoxes() - i - 1) * contrast * 2.0)
						/ ((double) boxes - 1) - contrast;
				g.drawString(formatValue(val), (int) (x + inset), y
						+ (int) ((i) * spacing + (spacing + numberHeight) / 2));
			}
		} else {
			final double spacing = width / (getNumBoxes());
			Image back = createImage((int) (height + 5), (int) (width + 5));
			final Graphics backG = back.getGraphics();

			for (int i = 0; i < getNumBoxes(); i++) {
				final double val = (i * contrast * 2.0) / ((double) boxes - 1)
						- contrast;
				backG.drawString(formatValue(val), y, x
						+ (int) ((i) * spacing + (spacing + numberHeight) / 2));
				/*
				 * backG.fillRect(x + (int)(i * spacing + (spacing -
				 * numberHeight) /2), y, numberHeight,numberWidth);
				 */
			}
			// this flips the backbuffer...
			back = RotateImageFilter.rotate(this, back);
			g.drawImage(back, (x), (int) (y - inset), null);
		}
	}

	class SettingsPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		JCheckBox verticalBox, bboxBox;
		JTextField xScaleField, yScaleField, decimalsField, numBoxesField;

		BboxRow bboxRow;
		SizeRow sizeRow;

		public boolean useBbox() {
			return bboxBox.isSelected();
		}

		public boolean drawVertical() {
			return verticalBox.isSelected();
		}

		public int getNumBoxes() {
			try {
				final Double tmp = new Double(numBoxesField.getText());
				return (int) tmp.doubleValue();
			} catch (final java.lang.NumberFormatException e) {
				return 1;
			}
		}

		public int getDecimals() {
			try {
				final Double tmp = new Double(decimalsField.getText());
				return (int) tmp.doubleValue();
			} catch (final java.lang.NumberFormatException e) {
				return 0;
			}
		}

		public double getXscale() {
			try {
				final Double tmp = new Double(xScaleField.getText());
				return tmp.doubleValue();
			} catch (final java.lang.NumberFormatException e) {
				return 0.0;
			}
		}

		public double getYscale() {
			try {
				final Double tmp = new Double(yScaleField.getText());
				return tmp.doubleValue();
			} catch (final java.lang.NumberFormatException e) {
				return 0.0;
			}
		}

		public int getBboxWidth() {
			try {
				return bboxRow.xSize();
			} catch (final java.lang.NumberFormatException e) {
				return 0;
			}
		}

		public int getBboxHeight() {
			try {
				return bboxRow.ySize();
			} catch (final java.lang.NumberFormatException e) {
				return 0;
			}
		}

		public void recalculateBbox() {
			if (drawVertical()) {
				bboxRow.setXsize(textLength());
				bboxRow.setYsize(0);
			} else {
				bboxRow.setYsize(textLength());
				bboxRow.setXsize(0);
			}
		}

		public void updateSize() {
			try {
				sizeRow.setXsize(estimateWidth());
				sizeRow.setYsize(estimateHeight());
			} catch (final Exception e) {
				// ignore...
			}
		}

		SettingsPanel() {
			documentListener = new DocumentListener() {
				@Override
				public void changedUpdate(final DocumentEvent e) {
					updateSize();
					if (previewPanel != null) {
						previewPanel.updatePreview();
					}
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					updateSize();
					if (previewPanel != null) {
						previewPanel.updatePreview();
					}
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					updateSize();
					if (previewPanel != null) {
						previewPanel.updatePreview();
					}
				}
			};
			setupWidgets();
		}

		DocumentListener documentListener = null;

		private void setupWidgets() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			final ActionListener syncher = new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					updateSize();
					if (previewPanel != null) {
						previewPanel.updatePreview();
					}
				}
			};

			add(new JLabel("Settings"));
			verticalBox = new JCheckBox("Draw Vertically");
			verticalBox.addActionListener(syncher);
			JPanel outputPanel = new JPanel();
			outputPanel.add(verticalBox);
			add(outputPanel);

			final JPanel numBoxesPanel = new JPanel();
			numBoxesField = new JTextField(Double.toString(7));
			numBoxesPanel.add(new JLabel("Number of Boxes"));
			numBoxesPanel.add(numBoxesField);
			add(numBoxesPanel);
			numBoxesField.getDocument().addDocumentListener(documentListener);

			final JPanel decimalPanel = new JPanel();
			decimalsField = new JTextField(Double.toString(2));
			decimalPanel.add(new JLabel("decimals"));
			decimalPanel.add(decimalsField);
			add(decimalPanel);
			decimalsField.getDocument().addDocumentListener(documentListener);

			final JPanel scalePanel = new JPanel();
			scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.Y_AXIS));
			final JPanel Xsub = new JPanel();
			xScaleField = new JTextField(Double.toString(12));
			Xsub.add(new JLabel("x scale"));
			Xsub.add(xScaleField);
			scalePanel.add(Xsub);

			yScaleField = new JTextField(Double.toString(12));
			final JPanel Ysub = new JPanel();
			Ysub.add(new JLabel("y scale"));
			Ysub.add(yScaleField);
			scalePanel.add(Ysub);
			scalePanel.add(new JLabel(
					"Use apple key to select multiple headers"));

			add(scalePanel);

			xScaleField.getDocument().addDocumentListener(documentListener);
			yScaleField.getDocument().addDocumentListener(documentListener);

			bboxBox = new JCheckBox("Bounding Box?", hasBbox());

			bboxBox.addActionListener(syncher);

			outputPanel = new JPanel();
			outputPanel.add(bboxBox);
			bboxRow = new BboxRow();
			if (hasBbox()) {
				add(outputPanel);
				add(bboxRow);
			}
			sizeRow = new SizeRow();
			add(sizeRow);
		}

		class BboxRow extends SizeRow {

			private static final long serialVersionUID = 1L;

			@Override
			protected void setupWidgets() {
				final DocumentListener documentListener = new DocumentListener() {
					@Override
					public void changedUpdate(final DocumentEvent e) {
						updateSize();
						if (previewPanel != null) {
							previewPanel.updatePreview();
						}
					}

					@Override
					public void insertUpdate(final DocumentEvent e) {
						updateSize();
						if (previewPanel != null) {
							previewPanel.updatePreview();
						}
					}

					@Override
					public void removeUpdate(final DocumentEvent e) {
						updateSize();
						if (previewPanel != null) {
							previewPanel.updatePreview();
						}
					}
				};
				add(new JLabel("BBox size:"));
				xSize = new JTextField("2", 4);
				ySize = new JTextField("2", 4);
				add(xSize);
				add(new JLabel("x"));
				add(ySize);
				add(new JLabel("(inch)"));
				xSize.getDocument().addDocumentListener(documentListener);
				ySize.getDocument().addDocumentListener(documentListener);
			}
		}

		class SizeRow extends JPanel {

			private static final long serialVersionUID = 1L;

			JTextField xSize, ySize;

			/** Constructor for the SizeRow object */
			public SizeRow() {
				setupWidgets();
			}

			protected void setupWidgets() {
				add(new JLabel("Total Size:"));
				xSize = new JTextField("2", 5);
				ySize = new JTextField("2", 5);
				add(xSize);
				add(new JLabel("x"));
				add(ySize);
				add(new JLabel("(inch)"));
			}

			int xSize() {
				final Double inch = new Double(xSize.getText());
				return (int) (inch.doubleValue() * 72);
			}

			void setXsize(final int points) {
				xSize.setText(convert(points));
			}

			void setYsize(final int points) {
				ySize.setText(convert(points));
			}

			private String convert(final int points) {
				final Double inch = new Double(
						Math.rint(((double) points * 100) / 72) / 100.0);
				return inch.toString();
			}

			int ySize() {
				final Double inch = new Double(ySize.getText());
				return (int) (inch.doubleValue() * 72);
			}

			@Override
			public void setEnabled(final boolean flag) {
				super.setEnabled(flag);
				xSize.setEnabled(flag);
				ySize.setEnabled(flag);
			}

		}
	}

	class FilePanel extends JPanel {

		private static final long serialVersionUID = 1L;

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

		/**
		 * Constructor for the FilePanel object
		 * 
		 * @param initial
		 *            Description of the Parameter
		 */
		public FilePanel(final String initial) {
			super();
			add(new JLabel("Export To: "));
			fileField = new JTextField(initial);
			add(fileField);
			final JButton chooseButton = new JButton("Browse");
			chooseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						final JFileChooser chooser = new JFileChooser();
						final int returnVal = chooser
								.showSaveDialog(ColorBarExportPanel.this);
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							fileField.setText(chooser.getSelectedFile()
									.getCanonicalPath());
						}
					} catch (final java.io.IOException ex) {
						LogBuffer.println("Got exception " + ex);
					}
				}
			});
			add(chooseButton);
		}
	}
}

/**
 * Internal test class, used only by <code>main</code> test case.
 */
class TestColorBarExportPanel extends ColorBarExportPanel {

	private static final long serialVersionUID = 1L;

	TestColorBarExportPanel(final ColorExtractor colorExtractor) {
		super(colorExtractor);
	}
}

class SimpleDataMatrix implements DataMatrix {

	int nRow;
	int nCol;
	double[] dataMatrix;

	public SimpleDataMatrix(final double[] matrix, final int nRow,
			final int nCol) {
		dataMatrix = matrix;
		this.nRow = nRow;
		this.nCol = nCol;
	}

	@Override
	public int getNumRow() {
		return nRow;
	}

	@Override
	public int getNumCol() {
		return nCol;
	}

	@Override
	public int getNumUnappendedCol() {
		return appendIndex == -1 ? getNumCol() : appendIndex;
	}

	@Override
	public double getValue(final int x, final int y) {
		return dataMatrix[x + y * nCol];
	}

	public void removeAppended() {
		if (appendIndex == -1) {
			return;
		}

		final double[] temp = new double[nRow * appendIndex];

		for (int i = 0; i < nRow * appendIndex; i++) {
			temp[i] = dataMatrix[i];
		}
		appendIndex = -1;
	}

	public void append(final DataMatrix m) {
		final double[] temp = new double[dataMatrix.length + m.getNumRow()
				* m.getNumCol()];

		int i;
		for (i = 0; i < dataMatrix.length; i++) {
			temp[i] = dataMatrix[i];
		}
		for (int e = 0; e < m.getNumCol(); e++) {
			for (int g = 0; g < m.getNumRow(); g++) {
				temp[i++] = m.getValue(g, e);
			}
		}
		appendIndex = nCol;
		nCol += m.getNumCol();
		dataMatrix = temp;
	}

	int appendIndex = -1;

	@Override
	public void setValue(final double value, final int x, final int y) {
		dataMatrix[x + y * nCol] = value;
	}

	@Override
	public boolean getModified() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setModified(final boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumUnappendedRow() {
		// TODO Auto-generated method stub
		return 0;
	}

}
