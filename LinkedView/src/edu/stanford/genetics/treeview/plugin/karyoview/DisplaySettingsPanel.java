/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: DisplaySettingsPanel.java,v $
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
package edu.stanford.genetics.treeview.plugin.karyoview;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.stanford.genetics.treeview.ColorIcon;
import edu.stanford.genetics.treeview.DummyConfigNode;
import edu.stanford.genetics.treeview.LinkedViewFrame;
import edu.stanford.genetics.treeview.SettingsPanel;
import edu.stanford.genetics.treeview.TreeSelection;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.app.LinkedViewApp;
import edu.stanford.genetics.treeview.model.TVModel;

class DisplaySettingsPanel extends JPanel implements SettingsPanel {

	private KaryoColorPresets presets;

	/** Setter for presets */
	public void setPresets(final KaryoColorPresets presets) {
		this.presets = presets;
	}

	/** Getter for presets */
	public KaryoColorPresets getPresets() {
		return presets;
	}

	private KaryoPanel karyoPanel;

	/** Setter for karyoPanel */
	public void setKaryoPanel(final KaryoPanel karyoPanel) {
		this.karyoPanel = karyoPanel;
	}

	/** Getter for karyoPanel */
	public KaryoPanel getKaryoPanel() {
		return karyoPanel;
	}

	public static final void main(final String[] argv) {
		final LinkedViewApp statview = new LinkedViewApp();
		final ViewFrame testf = new LinkedViewFrame(statview);

		final KaryoPanel kp = new KaryoPanel(new TVModel(),
				new TreeSelection(2), testf, new DummyConfigNode(
						"Display Settings Panel"));
		final KaryoColorPresets kcp = new KaryoColorPresets();

		final DisplaySettingsPanel panel = new DisplaySettingsPanel(kp, kcp,
				testf);
		panel.revalidate();
		final JFrame test = new JFrame("Test Display Settings Panel");
		test.getContentPane().add(panel);
		test.pack();
		test.setVisible(true);
	}

	public DisplaySettingsPanel(final KaryoPanel karyoPanel,
			final KaryoColorPresets presets, final ViewFrame frame) {
		setKaryoPanel(karyoPanel);
		setPresets(presets);
		setFrame(frame);
		addWidgets();
	}

	private ViewFrame frame = null;

	/** Setter for frame */
	public void setFrame(final ViewFrame frame) {
		this.frame = frame;
	}

	/** Getter for frame */
	public ViewFrame getFrame() {
		return frame;
	}

	private ColorConfigPanel colorPanel;
	private ColorPresetsPanel colorPresetsPanel;
	private DrawPanel drawPanel;
	private ScalePanel scalePanel;
	private SelectedPanel selectedPanel;

	private void addWidgets() {
		setLayout(new GridBagLayout());
		final GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 100;
		gc.weighty = 100;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;

		add(new JLabel("Draw"), gc);
		gc.gridx = 1;
		drawPanel = new DrawPanel();
		add(drawPanel, gc);

		gc.gridx = 0;
		gc.gridy = 1;
		add(new JLabel("Scale Lines"), gc);
		gc.gridx = 1;
		scalePanel = new ScalePanel();
		add(scalePanel, gc);

		gc.gridx = 0;
		gc.gridy = 2;
		add(new JLabel("Colors"), gc);
		gc.gridx = 1;
		colorPanel = new ColorConfigPanel();
		add(colorPanel, gc);

		gc.gridx = 0;
		gc.gridy = 3;
		add(new JLabel("Selected"), gc);
		gc.gridx = 1;
		selectedPanel = new SelectedPanel();
		add(selectedPanel, gc);
	}

	@Override
	public void synchronizeTo() {
		selectedPanel.setValues();
		drawPanel.setValues();
		scalePanel.setValues();
	}

	@Override
	public void synchronizeFrom() {
		selectedPanel.getValues();
		drawPanel.getValues();
		scalePanel.getValues();
	}

	/**
	 * panel with checkboxes for whether to draw lines and/or bars
	 */
	class DrawPanel extends JPanel {
		JCheckBox lineBox, barBox;

		DrawPanel() {
			setAlignmentX(Component.LEFT_ALIGNMENT);
			lineBox = new JCheckBox("lines");
			barBox = new JCheckBox("bars");
			add(lineBox);
			add(barBox);
			lineBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					setValues();
				}
			});
			barBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					setValues();
				}
			});
		}

		public void getValues() {
			final KaryoDrawer karyoDrawer = karyoPanel.getKaryoDrawer();
			lineBox.setSelected(karyoDrawer.getLineChart());
			barBox.setSelected(karyoDrawer.getBarChart());
			revalidate();
		}

		public void setValues() {
			final KaryoDrawer karyoDrawer = karyoPanel.getKaryoDrawer();
			karyoDrawer.setLineChart(lineBox.isSelected());
			karyoDrawer.setBarChart(barBox.isSelected());
			karyoDrawer.notifyObservers();
		}
	}

	/**
	 * panel with checkboxes and configuration for scale lines
	 */
	class ScalePanel extends JPanel {
		JCheckBox aboveBox, belowBox;
		JTextField baseField, maxField;

		ScalePanel() {
			setAlignmentX(Component.LEFT_ALIGNMENT);
			aboveBox = new JCheckBox("above");
			belowBox = new JCheckBox("below");
			baseField = new JTextField("2.0");
			maxField = new JTextField("5");

			add(aboveBox);
			add(belowBox);
			add(new JLabel(" base"));
			add(baseField);
			add(new JLabel(" #"));
			add(maxField);

			aboveBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					setValues();
				}
			});
			belowBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					setValues();
				}
			});
			baseField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(final DocumentEvent e) {
					setBase();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					setBase();
				}

				@Override
				public void changedUpdate(final DocumentEvent e) {
					setBase();
				}
			});
			maxField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(final DocumentEvent e) {
					setMax();
				}

				@Override
				public void removeUpdate(final DocumentEvent e) {
					setMax();
				}

				@Override
				public void changedUpdate(final DocumentEvent e) {
					setMax();
				}
			});
		}

		public void getValues() {
			final KaryoDrawer karyoDrawer = karyoPanel.getKaryoDrawer();
			aboveBox.setSelected(karyoDrawer.getLinesAbove());
			belowBox.setSelected(karyoDrawer.getLinesBelow());
			baseField.setText(reformatDouble(karyoDrawer.getLinesBase()));
			final int max = karyoDrawer.getLinesMax();
			maxField.setText(reformatInt(max));
			revalidate();
		}

		public void setBase() {
			final KaryoDrawer karyoDrawer = karyoPanel.getKaryoDrawer();
			try {
				final Double temp = new Double(baseField.getText());
				karyoDrawer.setLinesBase(temp.doubleValue());
			} catch (final java.lang.NumberFormatException e) {
			}
			karyoDrawer.notifyObservers();
		}

		public void setMax() {
			final KaryoDrawer karyoDrawer = karyoPanel.getKaryoDrawer();
			try {
				final Integer temp = new Integer(maxField.getText());
				karyoDrawer.setLinesMax(temp.intValue());
			} catch (final java.lang.NumberFormatException e) {
			}
			karyoDrawer.notifyObservers();
		}

		public void setValues() {
			final KaryoDrawer karyoDrawer = karyoPanel.getKaryoDrawer();
			karyoDrawer.setLinesAbove(aboveBox.isSelected());
			karyoDrawer.setLinesBelow(belowBox.isSelected());
			setBase();
			setMax();
			karyoDrawer.notifyObservers();
		}
	}

	/**
	 * Panel which allows configuration of all colors
	 */
	class ColorConfigPanel extends JPanel {
		private final ColorPanel[] colorPanels = new ColorPanel[6];

		ColorConfigPanel() {
			try {
				setBorder(BorderFactory
						.createEtchedBorder(EtchedBorder.LOWERED));
			} catch (final java.lang.NoSuchMethodError err) {
				// god damn MRJ for os 9.
			}
			for (int i = 0; i < 6; i++) {
				colorPanels[i] = new ColorPanel(i);
			}
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			final JPanel row1 = new JPanel();
			row1.add(colorPanels[0]);
			row1.add(colorPanels[1]);
			row1.add(colorPanels[2]);
			add(row1);
			final JPanel row2 = new JPanel();
			row2.add(colorPanels[3]);
			row2.add(colorPanels[4]);
			row2.add(colorPanels[5]);
			add(row2);

			final JPanel row3 = new JPanel();
			final JButton loadButton = new JButton("Load...");
			loadButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final JFileChooser chooser = new JFileChooser();
					final int returnVal = chooser
							.showOpenDialog(DisplaySettingsPanel.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						final File f = chooser.getSelectedFile();
						final KaryoDrawer karyoDrawer = getKaryoPanel()
								.getKaryoDrawer();
						final KaryoColorSet colorSet = karyoDrawer
								.getKaryoColorSet();
						colorSet.load(f.getPath());
						for (int i = 0; i < 6; i++) {
							colorPanels[i].redoColor();
						}
						repaint();
						karyoPanel.getKaryoView().repaint();
						/*
						 * try { } catch (IOException ex) {
						 * JOptionPane.showMessageDialog
						 * (DisplaySettingsPanel.this, "Could not load from " +
						 * f.toString() + "\n" + ex); }
						 */
					}
				}
			});
			row3.add(loadButton);

			final JButton saveButton = new JButton("Save...");
			saveButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final JFileChooser chooser = new JFileChooser();
					final int returnVal = chooser
							.showSaveDialog(DisplaySettingsPanel.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						final File f = chooser.getSelectedFile();
						final KaryoDrawer karyoDrawer = getKaryoPanel()
								.getKaryoDrawer();
						final KaryoColorSet colorSet = karyoDrawer
								.getKaryoColorSet();
						colorSet.save(f.getPath());
					}
				}
			});
			row3.add(saveButton);

			final JButton makeButton = new JButton("Make Preset");
			makeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final KaryoColorSet temp = new KaryoColorSet();
					final KaryoDrawer karyoDrawer = getKaryoPanel()
							.getKaryoDrawer();
					final KaryoColorSet colorSet = karyoDrawer
							.getKaryoColorSet();
					temp.copyStateFrom(colorSet);
					temp.setName("UserDefined");
					presets.addColorSet(temp);
					colorPresetsPanel.redoLayout();
					colorPresetsPanel.invalidate();
					colorPresetsPanel.revalidate();
					colorPresetsPanel.repaint();
				}
			});
			row3.add(makeButton);

			add(row3);
			colorPresetsPanel = new ColorPresetsPanel();
			add(new JScrollPane(colorPresetsPanel));
		}

		public void copyStateFrom(final KaryoColorSet otherSet) {
			final KaryoDrawer karyoDrawer = getKaryoPanel().getKaryoDrawer();
			final KaryoColorSet colorSet = karyoDrawer.getKaryoColorSet();
			colorSet.copyStateFrom(otherSet);
			for (int i = 0; i < 6; i++) {
				colorPanels[i].redoColor();
			}
			repaint();
			karyoPanel.getKaryoView().repaint();
		}

		public void getValues() {
		}

		public void setValues() {
		}
	}

	/**
	 * this class allows the presets to be selected...
	 */
	class ColorPresetsPanel extends JPanel {
		ColorPresetsPanel() {
			redoLayout();
		}

		public void redoLayout() {
			removeAll();
			final int nPresets = presets.getNumPresets();
			final JButton[] buttons = new JButton[nPresets];
			for (int i = 0; i < nPresets; i++) {
				final JButton presetButton = new JButton(
						(presets.getPresetNames())[i]);
				final int index = i;
				presetButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						colorPanel.copyStateFrom(presets.getColorSet(index));
					}
				});
				add(presetButton);
				buttons[index] = presetButton;
			}
		}
	}

	/**
	 * inner class, must be inner so it can notify karyoDrawer when it changes
	 * the colorSet.
	 */
	public class ColorPanel extends JPanel {
		ColorIcon colorIcon;
		int type;

		public ColorPanel(final int i) {
			type = i;
			redoComps();
		}

		public void redoColor() {
			colorIcon.setColor(getColor());
		}

		public void redoComps() {
			removeAll();
			colorIcon = new ColorIcon(10, 10, getColor());
			final JButton pushButton = new JButton(getLabel(), colorIcon);
			pushButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final Color trial = JColorChooser.showDialog(
							ColorPanel.this, "Pick Color for " + getLabel(),
							getColor());
					if (trial != null) {
						setColor(trial);
						karyoPanel.getKaryoView().repaint();
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {
			final KaryoDrawer karyoDrawer = getKaryoPanel().getKaryoDrawer();
			final KaryoColorSet colorSet = karyoDrawer.getKaryoColorSet();
			colorSet.setColor(type, c);
			colorIcon.setColor(getColor());
			repaint();
		}

		private String getLabel() {
			final KaryoDrawer karyoDrawer = getKaryoPanel().getKaryoDrawer();
			final KaryoColorSet colorSet = karyoDrawer.getKaryoColorSet();
			return colorSet.getType(type);
		}

		private Color getColor() {
			final KaryoDrawer karyoDrawer = getKaryoPanel().getKaryoDrawer();
			final KaryoColorSet colorSet = karyoDrawer.getKaryoColorSet();
			return colorSet.getColor(type);
		}
	}

	class SelectedPanel extends JPanel {
		JComboBox iconBox, iconSize;
		private KaryoDrawer karyoDrawer = null;

		/**
		 * Constructor for the SizePanel object
		 */
		public SelectedPanel() {
			if (karyoPanel != null)
				karyoDrawer = karyoPanel.getKaryoDrawer();
			iconBox = new JComboBox();

			final String[] types = karyoDrawer.getIconTypes();
			for (int i = 0; i < types.length; i++) {
				iconBox.addItem(types[i]);
			}

			iconSize = new JComboBox();
			final int[] sizes = karyoDrawer.getIconSizes();
			for (int i = 0; i < sizes.length; i++) {
				iconSize.addItem(reformatInt(sizes[i]));
			}
			add(new JLabel("Highlight Selected with "));
			add(iconBox);
			add(iconSize);

			getValues();

			iconBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					setValues();
				}
			});
			iconSize.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					setValues();
				}
			});
		}

		public void getValues() {
			if (karyoDrawer != null) {
				iconBox.setSelectedIndex(karyoDrawer.getIconType());
				iconSize.setSelectedIndex(karyoDrawer.getIconSize());
				revalidate();
			}
		}

		public void setValues() {
			if (karyoDrawer != null) {
				karyoDrawer.setIconType(iconBox.getSelectedIndex());
				karyoDrawer.setIconSize(iconSize.getSelectedIndex());
				karyoDrawer.notifyObservers();
			}
		}
	}

	private static String reformatInt(final int td) {
		final Integer tx = new Integer(td);
		return tx.toString();
	}

	private static String reformatDouble(final double td) {
		int order = 1;
		if (Math.abs(td) < 0.0000001) {
			return "0.0000";
		}
		while (Math.abs(td * order) < 1000) {
			order *= 10;
		}
		final int val = (int) (td * order);
		final Double tx = new Double(((double) val) / order);
		return tx.toString();
	}

}
