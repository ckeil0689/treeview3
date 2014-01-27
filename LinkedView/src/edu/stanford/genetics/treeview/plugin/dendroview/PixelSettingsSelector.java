/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: PixelSettingsSelector.java,v $
 * $Revision: 1.3 $
 * $Date: 2008-03-09 21:06:33 $
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.SettingsPanel;

/**
 * A popup to allow interactive changing of the pixel scaling and contrast
 * settings of an array view.
 * 
 */
public class PixelSettingsSelector extends JPanel implements SettingsPanel {

	private static final long serialVersionUID = 1L;

	public PixelSettingsSelector(final MapContainer xmap,
			final MapContainer ymap, final ColorExtractor drawer,
			final ColorPresets colorPresets) {

		this(xmap, ymap, null, null, drawer, colorPresets);
	}

	/**
	 * decided to handle updates of Xmlconfig through a windowlistener. thus,
	 * this just calls the other constructor.
	 */
	public PixelSettingsSelector(final MapContainer xmap,
			final MapContainer ymap, final MapContainer xZmap,
			final MapContainer yZmap, final ConfigNode config,
			final ColorExtractor drawer, final ColorPresets colorPresets) {

		this(xmap, ymap, xZmap, yZmap, drawer, colorPresets);
	}

	public PixelSettingsSelector(final MapContainer xmap,
			final MapContainer ymap, final MapContainer xZmap,
			final MapContainer yZmap, final ColorExtractor drawer,
			final ColorPresets colorPresets) {
		
		super();
		
		this.m_xmap = xmap;
		this.m_ymap = ymap;
		this.m_xZmap = xZmap;
		this.m_yZmap = yZmap;
		this.m_drawer = drawer;
		this.m_presets = colorPresets;
		
		setLayout(new MigLayout());
		setOpaque(false);
		setupWidgets();
	}

	private void setupWidgets() {

		removeAll();
		
		JPanel pixels = setPanelLayout();
		JPanel contrast = setPanelLayout();
		JPanel log = setPanelLayout();
		JPanel colors = setPanelLayout();
		
		pixels.add(GUIParams.setupHeader("Pixel Scale"), "span, wrap");

		m_xscale = new ScalePanel(m_xmap, "X-Scale:");
		pixels.add(m_xscale, "w 50%");
		m_yscale = new ScalePanel(m_ymap, "Y-Scale:");
		pixels.add(m_yscale, "w 50%");

		if (m_xZmap != null && m_yZmap != null) {

			pixels.add(makeLabel("Zoom:"), "wrap");
			m_xZscale = new ScalePanel(m_xZmap, "X:");
			pixels.add(m_xZscale, "pushx, growx");
			m_yZscale = new ScalePanel(m_yZmap, "Y:");
			pixels.add(m_yZscale, "pushx, growx");
		}
		add(pixels, "push, w 90%, wrap");

		if (m_drawer != null) {
			contrast.add(GUIParams.setupHeader("Contrast"), "wrap");
			m_contrast = new ContrastSelector(m_drawer);
			contrast.add(m_contrast, "pushx, growx");
			
			add(contrast, "push, w 90%, wrap");

			log.add(GUIParams.setupHeader("LogScale"), "wrap");
			m_logscale = new LogScaleSelector();
			log.add(m_logscale, "pushx, growx");
			
			add(log, "push, w 90%, wrap");

			// color stuff...
			colors.add(GUIParams.setupHeader("Colors"), "wrap");

			colorExtractorEditor = new ColorExtractorEditor(m_drawer);
			colors.add(colorExtractorEditor, "alignx 50%, pushx, wrap");
			colors.add(new CEEButtons(), "alignx 50%, pushx, wrap");

			colorPresetsPanel = new ColorPresetsPanel();
			final JScrollPane sp = new JScrollPane(colorPresetsPanel,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setOpaque(false);
			colors.add(sp, "alignx 50%, pushx, growx");
			
			add(colors, "push, w 90%");
		}
	}

	class ScalePanel extends JPanel {

		private static final long serialVersionUID = 1L;

		//private final ButtonGroup type;
		//private final JRadioButton fixed, fill;
		private final JTextField value;
		private final MapContainer ymap;

		public ScalePanel(final MapContainer xmc, final String title) {

			ymap = xmc;

			this.setLayout(new MigLayout());
			this.setOpaque(false);

			add(makeLabel(title), "span, wrap");

//			type = new ButtonGroup();
//			fixed = new JRadioButton("Fixed Scale");
//			fixed.setForeground(GUIParams.TEXT);
//			fixed.setOpaque(false);
//			type.add(fixed);
//			this.add(fixed);

			value = new JTextField(Double.toString(ymap.getScale()), 5);
			add(value, "w 95%, wrap");

//			fill = new JRadioButton("Fill");
//			fill.setForeground(GUIParams.TEXT);
//			fill.setOpaque(false);
//			type.add(fill);
//			this.add(fill, "span");

//			if (xmc.getCurrent().type().equals("Fixed")) {
//				fixed.setSelected(true);
//				// type.setSelectedCheckbox(fixed);
//
//			} else {
//				fill.setSelected(true);
//				// type.setSelectedCheckbox(fill);
//
//			}
//
//			fill.addItemListener(new ItemListener() {
//
//				@Override
//				public void itemStateChanged(final ItemEvent evt) {
//
//					ScalePanel.this.updateCheck();
//				}
//			});
//
//			fixed.addItemListener(new ItemListener() {
//				@Override
//				public void itemStateChanged(final ItemEvent evt) {
//
//					ScalePanel.this.updateCheck();
//				}
//			});

			value.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void changedUpdate(final DocumentEvent e) {

					ScalePanel.this.updateValue();
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {

					ScalePanel.this.updateValue();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {

					ScalePanel.this.updateValue();
				}
			});
		}

//		public void updateCheck() {
//
//			if (fixed.isSelected()) {
//				ymap.setMap("Fixed");
//				value.setEnabled(true);
//
//			} else {
//				ymap.setMap("Fill");
//				value.setEnabled(false);
//			}
//
//			value.setText(Double.toString(ymap.getScale()));
//			ymap.notifyObservers();
//		}

		public void updateValue() {

			//if (fixed.isSelected()) {
				try {
					final Double d = new Double(value.getText());
					ymap.setScale(d.doubleValue());
					ymap.notifyObservers();

				} catch (final java.lang.NumberFormatException e) {
					// do nothing if the format is bad...
				}
			}
		//}
	}

	@Override
	public void synchronizeFrom() {

		setupWidgets();
	}

	@Override
	public void synchronizeTo() {
		/*
		 * don't do anything? m_contrast.signalAll(); m_xscale.updateValue();
		 * m_yscale.updateValue();
		 */
	}

	public JDialog showDialog(final JFrame f, final String title) {

		final JDialog d = new JDialog(f, title);
		d.setLayout(new MigLayout());
		d.add(this, "push, grow, wrap");

		final JButton display_button = GUIParams.setButtonLayout("Close", null);
		display_button.addActionListener(new ActionListener() {
			// called when close button hit
			@Override
			public void actionPerformed(final ActionEvent evt) {

				if (evt.getSource() == display_button) {
					synchronizeTo();
					d.dispose();
				}
			}
		});

		final JPanel p = new JPanel();
		p.add(display_button);
		d.add(p, "pushx, growx, alignx 50%");

		d.addWindowListener(new WindowAdapter() {
			// called when closed by system menu...
			@Override
			public void windowClosing(final WindowEvent we) {
				synchronizeTo();
				d.dispose();
			}
		});
		d.pack();
		return d;
	}

	// let's go hungarian
	ScalePanel m_xscale, m_yscale;
	ScalePanel m_xZscale, m_yZscale;
	ContrastSelector m_contrast;
	LogScaleSelector m_logscale;
	MapContainer m_xmap, m_ymap;
	MapContainer m_xZmap, m_yZmap;
	ColorExtractor m_drawer;
	ColorPresets m_presets;
	ColorExtractorEditor colorExtractorEditor;
	ColorPresetsPanel colorPresetsPanel;

	class CEEButtons extends JPanel {

		private static final long serialVersionUID = 1L;

		CEEButtons() {

			this.setOpaque(false);
			final JButton loadButton = GUIParams.setButtonLayout("Load", null);
			loadButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final JFileChooser chooser = new JFileChooser();
					final int returnVal = chooser
							.showOpenDialog(CEEButtons.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						final File f = chooser.getSelectedFile();
						try {
							final ColorSet temp = new ColorSet();
							temp.loadEisen(f);
							colorExtractorEditor.copyStateFrom(temp);

						} catch (final IOException ex) {
							JOptionPane.showMessageDialog(CEEButtons.this,
									"Could not load from " + f.toString()
											+ "\n" + ex);
						}
					}
				}
			});
			this.add(loadButton);

			final JButton saveButton = GUIParams.setButtonLayout("Save", null);
			saveButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final JFileChooser chooser = new JFileChooser();
					final int returnVal = chooser
							.showSaveDialog(CEEButtons.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						final File f = chooser.getSelectedFile();
						try {
							final ColorSet temp = new ColorSet();
							colorExtractorEditor.copyStateTo(temp);
							temp.saveEisen(f);

						} catch (final IOException ex) {
							JOptionPane.showMessageDialog(CEEButtons.this,
									"Could not save to " + f.toString() + "\n"
											+ ex);
						}
					}
				}
			});
			this.add(saveButton);

			final JButton makeButton = GUIParams.setButtonLayout("Make Preset", 
					null);
			makeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final ColorSet temp = new ColorSet();
					colorExtractorEditor.copyStateTo(temp);
					temp.setName("UserDefined");
					m_presets.addColorSet(temp);
					colorPresetsPanel.redoLayout();
					colorPresetsPanel.invalidate();
					colorPresetsPanel.revalidate();
					colorPresetsPanel.repaint();
				}
			});
			this.add(makeButton);

			final JButton resetButton = GUIParams.setButtonLayout(
					"Reset Presets", null);
			resetButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					m_presets.reset();
				}
			});
			this.add(resetButton);
		}
	}

	class ColorSelector extends JPanel {
		/**
		 * I don't use serialization, this is to keep eclipse happy.
		 */
		private static final long serialVersionUID = 1L;

		ColorSelector() {

			this.setOpaque(false);
			this.add(new ColorExtractorEditor(m_drawer));
		}

	}

	class LogScaleSelector extends JPanel {

		/**
		 * I don't use serialization, this is to keep eclipse happy.
		 */
		private static final long serialVersionUID = 1L;

		private final JTextField logTextField;
		private final JCheckBox logCheckBox;

		LogScaleSelector() {

			this.setLayout(new MigLayout());
			this.setOpaque(false);

			logCheckBox = new JCheckBox("Log (base 2)");
			logCheckBox.setBackground(GUIParams.BG_COLOR);
			logCheckBox.setForeground(GUIParams.TEXT);
			logCheckBox.setSelected(m_drawer.getLogTransform());
			logCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent arg0) {

					m_drawer.setLogTransform(logCheckBox.isSelected());
					logTextField.setEnabled(logCheckBox.isSelected());
					m_drawer.setLogBase(2.0);
					m_drawer.notifyObservers();
				}
			});
			add(logCheckBox, "pushx, span, wrap");

			logTextField = new JTextField(10);
			logTextField.setText("" + m_drawer.getLogCenter());
			add(makeLabel("Center:"), "alignx 50%");
			logTextField.getDocument().addDocumentListener(
					new DocumentListener() {

						@Override
						public void changedUpdate(final DocumentEvent e) {
							textBoxChanged();
						}

						@Override
						public void insertUpdate(final DocumentEvent e) {
							textBoxChanged();
						}

						@Override
						public void removeUpdate(final DocumentEvent e) {
							textBoxChanged();
						}
					});
			add(logTextField);
			logTextField.setEnabled(logCheckBox.isSelected());
		}

		private void textBoxChanged() {

			Double d;
			try {
				d = new Double(logTextField.getText());
				m_drawer.setLogCenter(d.doubleValue());
				m_drawer.notifyObservers();

			} catch (final Exception e) {
			}
		}
	}

	/**
	 * this class allows the presets to be selected...
	 */
	class ColorPresetsPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		ColorPresetsPanel() {

			redoLayout();
		}

		public void redoLayout() {

			removeAll();
			this.setBackground(GUIParams.BG_COLOR);
			final int nPresets = m_presets.getNumPresets();
			final JButton[] buttons = new JButton[nPresets];
			for (int i = 0; i < nPresets; i++) {
				final JButton presetButton = GUIParams.setButtonLayout((
						m_presets.getPresetNames())[i], null);
				final int index = i;
				presetButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent e) {

						colorExtractorEditor.copyStateFrom(m_presets
								.getColorSet(index));
					}
				});
				this.add(presetButton);

				buttons[index] = presetButton;
			}
		}
	}

	public JLabel makeLabel(final String title) {

		final Font buttonFont = new Font("Sans Serif", Font.PLAIN, 14);

		final JLabel label = new JLabel(title);

		label.setFont(buttonFont);
		label.setForeground(GUIParams.TEXT);

		return label;
	}

	public JPanel setPanelLayout() {

		final JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.setOpaque(false);

		return panel;
	}
}

/**
 * This class allows editing of a color set...
 */

class ColorExtractorEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int UP = 0;
	private static final int ZERO = 1;
	private static final int DOWN = 2;
	private static final int MISSING = 3;
	private final ColorExtractor colorExtractor;
	private final ColorPanel colorPanel[] = new ColorPanel[4];

	public ColorExtractorEditor(final ColorExtractor colorExtractor) {

		this.colorExtractor = colorExtractor;
		this.setOpaque(false);

		for (int i = 0; i < 4; i++) {

			colorPanel[i] = new ColorPanel(i);
			add(colorPanel[i]);
		}
	}

	public void copyStateFrom(final ColorSet source) {

		colorPanel[UP].setColor(source.getUp());
		colorPanel[ZERO].setColor(source.getZero());
		colorPanel[DOWN].setColor(source.getDown());
		colorPanel[MISSING].setColor(source.getMissing());

	}

	public void copyStateTo(final ColorSet dest) {

		dest.setUp(colorPanel[UP].getColor());
		dest.setZero(colorPanel[ZERO].getColor());
		dest.setDown(colorPanel[DOWN].getColor());
		dest.setMissing(colorPanel[MISSING].getColor());
	}

	class ColorPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		ColorIcon colorIcon;
		int type;

		public ColorPanel(final int i) {

			type = i;
			redoComps();
		}

		public void redoComps() {

			removeAll();
			this.setOpaque(false);
			colorIcon = new ColorIcon(10, 10, getColor());
			final JButton pushButton = GUIParams.setButtonLayout(getLabel(), 
					null);
			pushButton.setIcon(colorIcon);
			pushButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final Color trial = JColorChooser.showDialog(
							ColorExtractorEditor.this, "Pick Color for "
									+ getLabel(), getColor());
					if (trial != null) {
						setColor(trial);
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {

			switch (type) {
			case UP:
				colorExtractor.setUpColor(c);
				break;
			case ZERO:
				colorExtractor.setZeroColor(c);
				break;
			case DOWN:
				colorExtractor.setDownColor(c);
				break;
			case MISSING:
				colorExtractor.setMissingColor(c);
				break;
			}

			colorIcon.setColor(getColor());
			// redoComps();
			colorExtractor.notifyObservers();
			repaint();
		}

		private String getLabel() {

			switch (type) {
			case UP:
				return "Positive";
			case ZERO:
				return "Zero";
			case DOWN:
				return "Negative";
			case MISSING:
				return "Missing";
			}

			return null;
		}

		private Color getColor() {

			switch (type) {
			case UP:
				return colorExtractor.getUp();
			case ZERO:
				return colorExtractor.getZero();
			case DOWN:
				return colorExtractor.getDown();
			case MISSING:
				return colorExtractor.getMissing();
			}

			return null;
		}
	}

	class ColorIcon implements Icon {

		private final int width, height;
		private Color color;

		ColorIcon(final int x, final int y, final Color c) {
			width = x;
			height = y;
			color = c;
		}

		public void setColor(final Color c) {

			color = c;
		}

		@Override
		public int getIconHeight() {

			return height;
		}

		@Override
		public int getIconWidth() {

			return width;
		}

		@Override
		public void paintIcon(final Component c, final Graphics g, final int x,
				final int y) {

			final Color old = g.getColor();
			g.setColor(color);
			g.fillRect(x, y, width, height);
			g.setColor(Color.black);
			g.drawRect(x, y, width, height);
			g.setColor(old);
		}
	}
}
