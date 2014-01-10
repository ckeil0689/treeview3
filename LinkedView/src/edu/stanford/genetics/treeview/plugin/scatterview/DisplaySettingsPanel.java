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
package edu.stanford.genetics.treeview.plugin.scatterview;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
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
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.app.LinkedViewApp;

class DisplaySettingsPanel extends JPanel implements SettingsPanel {
	private ScatterColorPresets presets;

	/** Setter for presets */
	public void setPresets(final ScatterColorPresets presets) {
		this.presets = presets;
	}

	/** Getter for presets */
	public ScatterColorPresets getPresets() {
		return presets;
	}

	private HorizontalAxisPane horizontalAxisPane = null;
	private VerticalAxisPane verticalAxisPane = null;

	private ScatterView scatterPane;

	/** Setter for scatterPane */
	public void setScatterView(final ScatterView scatterPane) {
		this.scatterPane = scatterPane;
	}

	/** Getter for scatterPane */
	public ScatterView getScatterView() {
		return scatterPane;
	}

	public static final void main(final String[] argv) {
		final LinkedViewApp statview = new LinkedViewApp();
		final LinkedViewFrame testf = new LinkedViewFrame(statview);

		final ScatterPanel kp = new ScatterPanel(testf, new DummyConfigNode(
				"Display Settings Panel"));
		final ScatterColorPresets kcp = new ScatterColorPresets();

		final DisplaySettingsPanel panel = new DisplaySettingsPanel(
				kp.getScatterPane(), kcp, testf);
		panel.revalidate();
		final JFrame test = new JFrame("Test Display Settings Panel");
		test.getContentPane().add(panel);
		test.pack();
		test.setVisible(true);
	}

	public DisplaySettingsPanel(final ScatterView scatterPane,
			final ScatterColorPresets presets, final ViewFrame frame) {
		this(scatterPane, presets, frame, (HorizontalAxisPane) null,
				(VerticalAxisPane) null);
	}

	public DisplaySettingsPanel(final ScatterView scatterPane,
			final ScatterColorPresets presets, final ViewFrame frame,
			final HorizontalAxisPane horizontalAxisPane,
			final VerticalAxisPane verticalAxisPane) {
		this(scatterPane, presets, scatterPane.getXAxisInfo(), scatterPane
				.getYAxisInfo(), frame, horizontalAxisPane, verticalAxisPane);
	}

	public DisplaySettingsPanel(final ScatterView scatterPane,
			final ScatterColorPresets presets, final AxisInfo xAxisInfo,
			final AxisInfo yAxisInfo, final ViewFrame frame,
			final HorizontalAxisPane horizontalAxisPane,
			final VerticalAxisPane verticalAxisPane) {
		this.horizontalAxisPane = horizontalAxisPane;
		this.verticalAxisPane = verticalAxisPane;
		setScatterView(scatterPane);
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

	private AxisPanel xAxisPanel, yAxisPanel;
	private ColorConfigPanel colorPanel;
	private ColorPresetsPanel colorPresetsPanel;

	private void addWidgets() {
		setLayout(new GridBagLayout());
		final GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 100;
		gc.weighty = 100;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;

		gc.gridx = 0;
		gc.gridy = 0;
		xAxisPanel = new AxisPanel(scatterPane.getXAxisInfo());
		add(xAxisPanel, gc);
		gc.gridx = 1;
		yAxisPanel = new AxisPanel(scatterPane.getYAxisInfo());
		add(yAxisPanel, gc);

		gc.gridx = 0;
		gc.gridy = 1;
		gc.gridwidth = 2;
		colorPanel = new ColorConfigPanel();
		add(colorPanel, gc);
	}

	@Override
	public void synchronizeTo() {
	}

	@Override
	public void synchronizeFrom() {
	}

	/**
	 * panel which allows setting of axis parameters
	 */
	class AxisPanel extends JPanel {

		/**
		 * all info is stored in the axis info
		 */
		AxisInfo axisInfo;

		/**
		 * Panel to configure Minimum Value for axis
		 */
		ParameterPanel minPanel;

		/**
		 * Panel to configure Maximum Value for axis
		 */
		ParameterPanel maxPanel;

		/**
		 * Panel to configure Minor tick spacing for axis
		 */
		ParameterPanel minorPanel;

		/**
		 * Panel to configure Major tick spacing for axis
		 */
		ParameterPanel majorPanel;

		AxisPanel(final AxisInfo axisInfo) {
			this.axisInfo = axisInfo;

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(new JLabel(axisInfo.getType() + " Axis"));

			minPanel = new ParameterPanel(
					axisInfo.getAxisParameter(AxisParameter.MIN));
			maxPanel = new ParameterPanel(
					axisInfo.getAxisParameter(AxisParameter.MAX));
			minorPanel = new ParameterPanel(
					axisInfo.getAxisParameter(AxisParameter.MINOR));
			majorPanel = new ParameterPanel(
					axisInfo.getAxisParameter(AxisParameter.MAJOR));

			add(minPanel);
			add(maxPanel);
			add(minorPanel);
			add(majorPanel);

		}

		public void getValues() {
			minPanel.getValues();
			maxPanel.getValues();
			majorPanel.getValues();
			minorPanel.getValues();
			revalidate();
		}

		public void setValues() {
			minPanel.setValues();
			maxPanel.setValues();
			majorPanel.setValues();
			minorPanel.setValues();
		}

		class ParameterPanel extends JPanel {
			private final AxisParameter axisParameter;
			/**
			 * text fields to hold value for parameter
			 */
			private final JTextField valueField;
			/**
			 * checkbox to enable/disable parameter
			 */
			private final JCheckBox enabledBox;

			ParameterPanel(final AxisParameter axisParameter) {
				this.axisParameter = axisParameter;

				valueField = new JTextField("" + axisParameter.getValue());
				enabledBox = new JCheckBox(axisParameter.getName());
				enabledBox.setSelected(axisParameter.getEnabled());

				enabledBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						setEnabledValue();
					}
				});

				valueField.addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(final KeyEvent e) {
						// setValue()
					};

					@Override
					public void keyReleased(final KeyEvent e) {
						// setValue()
					};

					@Override
					public void keyTyped(final KeyEvent e) {
						enabledBox.setSelected(true);
						setEnabledValue();
					}
				});

				valueField.getDocument().addDocumentListener(
						new DocumentListener() {
							@Override
							public void insertUpdate(final DocumentEvent e) {
								setValue();
							}

							@Override
							public void removeUpdate(final DocumentEvent e) {
								setValue();
							}

							@Override
							public void changedUpdate(final DocumentEvent e) {
								setValue();
							}
						});
				getValues();
				add(enabledBox);
				add(valueField);
			}

			public void getValues() {
				enabledBox.setSelected(axisParameter.getEnabled());
				valueField.setText("" + axisParameter.getValue());
			}

			public void setEnabledValue() {
				final boolean current = axisParameter.getEnabled();
				if (current == enabledBox.isSelected())
					return;

				axisParameter.setEnabled(enabledBox.isSelected());
				if (enabledBox.isSelected()) {
					setValue();
				} else {
					repaintScatterView();
				}
			}

			public void setValue() {
				if (enabledBox.isSelected() == false)
					return;
				try {
					final Double temp = new Double(valueField.getText());
					final double current = axisParameter.getValue();
					if (current == temp.doubleValue())
						return;
					axisParameter.setValue(temp.doubleValue());
				} catch (final java.lang.NumberFormatException e) {
				}
				repaintScatterView();
			}

			public void setValues() {
				setEnabledValue();
				setValue();
			}
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
			for (int i = 0; i < 4; i++) {
				colorPanels[i] = new ColorPanel(i);
			}
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			final JPanel row1 = new JPanel();
			row1.add(colorPanels[0]);
			row1.add(colorPanels[1]);
			row1.add(colorPanels[2]);
			row1.add(colorPanels[3]);
			add(row1);

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
						final ScatterColorSet colorSet = scatterPane
								.getColorSet();
						colorSet.load(f.getPath());
						for (int i = 0; i < 6; i++) {
							colorPanels[i].redoColor();
						}
						repaint();
						repaintScatterView();
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
						final ScatterColorSet colorSet = scatterPane
								.getColorSet();
						colorSet.save(f.getPath());
					}
				}
			});
			row3.add(saveButton);

			final JButton makeButton = new JButton("Make Preset");
			makeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final ScatterColorSet temp = new ScatterColorSet();
					final ScatterColorSet colorSet = scatterPane.getColorSet();
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
			revalidate();
		}

		public void copyStateFrom(final ScatterColorSet otherSet) {
			final ScatterColorSet colorSet = scatterPane.getColorSet();
			colorSet.copyStateFrom(otherSet);
			for (int i = 0; i < 4; i++) {
				colorPanels[i].redoColor();
			}
			repaint();
			repaintScatterView();
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
	 * inner class, must be inner so it can notify scatterPane when it changes
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
						repaintScatterView();
					}
				}
			});

			add(pushButton);
		}

		private void setColor(final Color c) {
			final ScatterColorSet colorSet = scatterPane.getColorSet();
			colorSet.setColor(type, c);
			colorIcon.setColor(getColor());
			repaint();
		}

		private String getLabel() {
			final ScatterColorSet colorSet = scatterPane.getColorSet();
			return colorSet.getType(type);
		}

		private Color getColor() {
			final ScatterColorSet colorSet = scatterPane.getColorSet();
			return colorSet.getColor(type);
		}
	}

	private void repaintScatterView() {
		scatterPane.setOffscreenValid(false);
		scatterPane.repaint();
		if (horizontalAxisPane != null) {
			horizontalAxisPane.repaint();
		}
		if (verticalAxisPane != null) {
			verticalAxisPane.repaint();
		}
	}

}
