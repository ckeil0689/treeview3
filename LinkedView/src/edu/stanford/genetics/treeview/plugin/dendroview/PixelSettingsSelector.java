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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;

import net.miginfocom.swing.MigLayout;

import edu.stanford.genetics.treeview.*;
/**
 * A popup to allow interactive changing of the pixel scaling and
 * contrast settings of an array view.
 * 
 */
public class PixelSettingsSelector extends JPanel implements SettingsPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * decided to handle updates of Xmlconfig through a windowlistener.
	 * thus, this just calls the other constructor.
	 */
	public PixelSettingsSelector (MapContainer xmap, MapContainer ymap, 
			ColorExtractor drawer, ColorPresets colorPresets) {
		
		this(xmap, ymap, null, null, drawer, colorPresets);
	}
	
	public PixelSettingsSelector (MapContainer xmap, MapContainer ymap,
			MapContainer xZmap, MapContainer yZmap,
			ConfigNode config, ColorExtractor drawer,
			ColorPresets colorPresets) {
		
		this(xmap, ymap, xZmap, yZmap, drawer, colorPresets);
	}
	
	public PixelSettingsSelector (MapContainer xmap, MapContainer ymap,
			MapContainer xZmap, MapContainer yZmap,
			ColorExtractor drawer,
			ColorPresets colorPresets) {
		
		this.setLayout(new MigLayout());
		this.setBackground(GUIColors.BG_COLOR);
		m_xmap = xmap;
		m_ymap = ymap;
		m_xZmap = xZmap;
		m_yZmap = yZmap;
		m_drawer = drawer;
		m_presets = colorPresets;
		setupWidgets();
	}
	
	private void setupWidgets() {
		
//		removeAll();
//		Border border = BorderFactory.createEtchedBorder();
//		setLayout(new GridBagLayout());
//		GridBagConstraints gbc = new GridBagConstraints();
//		gbc.fill = GridBagConstraints.BOTH;
//		gbc.insets = new Insets(5,5,5,5);
//		// scale stuff:
//		gbc.gridy = 0;
//		gbc.gridx = 0;
//		add(new JLabel("Global:"), gbc);
//		JPanel t = new JPanel();
//		t.setBorder(border);
//		m_xscale = new ScalePanel(m_xmap, "X:");
//		t.add(m_xscale);
//		m_yscale = new ScalePanel(m_ymap, "Y:");
//		t.add(m_yscale);
//		gbc.gridy = 0;
//		gbc.gridx = 1;
//		add(t, gbc);
//
//		if(m_xZmap != null && m_yZmap != null) {
//			gbc.gridy = 1;
//			gbc.gridx = 0;
//			add(new JLabel("Zoom:"), gbc);
//			t = new JPanel();
//			t.setBorder(border);
//			m_xZscale = new ScalePanel(m_xZmap, "X:");
//			t.add(m_xZscale);
//			m_yZscale = new ScalePanel(m_yZmap, "Y:");
//			t.add(m_yZscale);
//		}
//		
//		gbc.gridy = 1;
//		gbc.gridx = 1;
//		add(t, gbc);
//
//		gbc.gridy += 1;
//		gbc.gridx = 0;
//		if (m_drawer != null) {
//			add(new JLabel("Contrast:"), gbc);
//			m_contrast = new ContrastSelector(m_drawer);
//			m_contrast.setBorder(border);
//			gbc.gridx = 1;
//			add(m_contrast, gbc);
//
//
//			gbc.gridy += 1;
//			gbc.gridx = 0;
//			add(new JLabel("LogScale:"), gbc);
//			m_logscale = new LogScaleSelector();
//			m_logscale.setBorder(border);
//			gbc.gridx = 1;
//			add(m_logscale, gbc);
//
//
//			// color stuff...
//			gbc.gridy += 1;
//			add(new JLabel("Colors:"), gbc);
//			JPanel temp2 = new JPanel();
//			temp2.setBorder(border);
//			temp2.setLayout(new BoxLayout(temp2, BoxLayout.Y_AXIS));
//
//			colorExtractorEditor = new ColorExtractorEditor(m_drawer);
//			temp2.add(colorExtractorEditor);
//			temp2.add(new CEEButtons());
//			colorPresetsPanel = new ColorPresetsPanel();
//			temp2.add(new JScrollPane(colorPresetsPanel, 
//					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
//					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS));
//			gbc.gridx = 1;
//			add(temp2, gbc);
//		}
		
		removeAll();
		this.setLayout(new MigLayout());
		
		this.add(new JLabel("Global:"), "wrap");
		JPanel t = setPanelLayout();
		
		m_xscale = new ScalePanel(m_xmap, "X:");
		t.add(m_xscale, "pushx, growx");
		m_yscale = new ScalePanel(m_ymap, "Y:");
		t.add(m_yscale, "pushx");
		this.add(t, "pushx, growx, wrap");
		
		if(m_xZmap != null && m_yZmap != null) {
			
			this.add(new JLabel("Zoom:"));
			t = setPanelLayout();
			m_xZscale = new ScalePanel(m_xZmap, "X:");
			t.add(m_xZscale, "pushx");
			m_yZscale = new ScalePanel(m_yZmap, "Y:");
			t.add(m_yZscale, "pushx");
		}
		
		this.add(t, "pushx, growx, wrap");
		
		if (m_drawer != null) {
			this.add(new JLabel("Contrast:"), "wrap");
			m_contrast = new ContrastSelector(m_drawer);
			this.add(m_contrast, "pushx, growx, wrap");

			this.add(new JLabel("LogScale:"), "wrap");
			m_logscale = new LogScaleSelector();
			this.add(m_logscale, "pushx, growx, wrap");
			
			// color stuff...
			this.add(new JLabel("Colors:"), "wrap");
			JPanel temp2 = setPanelLayout();

			colorExtractorEditor = new ColorExtractorEditor(m_drawer);
			temp2.add(colorExtractorEditor, "alignx 50%, pushx, wrap");
			temp2.add(new CEEButtons(), "alignx 50%, pushx, wrap");
			
			colorPresetsPanel = new ColorPresetsPanel();
			JScrollPane sp = new JScrollPane(colorPresetsPanel, 
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBackground(GUIColors.BG_COLOR);
			temp2.add(sp, "alignx 50%, pushx, growx");
			this.add(temp2, "pushx, growx");
		}
	}

	class ScalePanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		
		private ButtonGroup type;
		private JRadioButton fixed, fill;
		private JTextField value;
		private MapContainer ymap;

		public ScalePanel(MapContainer xmc, String title) {
			
			ymap = xmc;

			this.setLayout(new MigLayout());
			this.setOpaque(false);
			
			this.add(new JLabel(title), "span, wrap");

			type = new ButtonGroup();
			fixed = new JRadioButton("Fixed Scale");
			fixed.setOpaque(false);
			type.add(fixed);
			this.add(fixed);

			value = new JTextField(Double.toString(ymap.getScale()), 5);
			this.add(value, "growx, wrap");

			fill = new JRadioButton("Fill");
			fill.setOpaque(false);
			type.add(fill);
			this.add(fill, "span");

			if (xmc.getCurrent().type().equals("Fixed")) {
				fixed.setSelected(true);
				//		type.setSelectedCheckbox(fixed);
				
			} else {
				fill.setSelected(true);
//				type.setSelectedCheckbox(fill);

			}

			fill.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent evt) {
					
					ScalePanel.this.updateCheck();
				}	    
			});
			
			fixed.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent evt) {
					
					ScalePanel.this.updateCheck();
				}	    
			});
			
			value.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					
					ScalePanel.this.updateValue();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					
					ScalePanel.this.updateValue();
				}
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					
					ScalePanel.this.updateValue();
				}
			});
		}
		
		public void updateCheck() {
			
			if (fixed.isSelected()) {
				ymap.setMap("Fixed");
				value.setEnabled(true);
				
			} else {
				ymap.setMap("Fill");
				value.setEnabled(false);
			}
			
			value.setText(Double.toString(ymap.getScale()));
			ymap.notifyObservers();
		}

		public void updateValue() {
			
			if (fixed.isSelected()) {
				try {
					Double d = new Double(value.getText());
					ymap.setScale(d.doubleValue());
					ymap.notifyObservers();
					
				} catch (java.lang.NumberFormatException e) {
					// do nothing if the format is bad...
				}
			}
		}
	}
	
	@Override
	public void synchronizeFrom() {
		
		setupWidgets();
	}
	
	@Override
	public void synchronizeTo() {
		/* don't do anything?
		m_contrast.signalAll();
	  m_xscale.updateValue();
	  m_yscale.updateValue();
		 */
	}

	public JDialog showDialog(JFrame f, String title) {
		
		final JDialog d = new JDialog(f, title);
		d.setLayout(new MigLayout());
		d.add(this, "push, grow, wrap");

		final JButton display_button = setButtonLayout("Close");
		display_button.addActionListener(new ActionListener() {
			// called when close button hit
			@Override
			public void actionPerformed(ActionEvent evt) {
				
				if(evt.getSource() == display_button) {
					synchronizeTo();
					d.dispose();
				}
			}
		});
		
		JPanel p = new JPanel();
		p.add(display_button);
		d.add(p, "pushx, growx, alignx 50%");


		d.addWindowListener(new WindowAdapter() {
			// called when closed by system menu...
			@Override
			public void windowClosing(WindowEvent we) {
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
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		CEEButtons() {

			this.setOpaque(false);
			JButton loadButton = setButtonLayout("Load...");
			loadButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showOpenDialog(CEEButtons.this);
					if(returnVal == JFileChooser.APPROVE_OPTION) {
						File f = chooser.getSelectedFile();
						try {
							ColorSet temp = new ColorSet();
							temp.loadEisen(f);
							colorExtractorEditor.copyStateFrom(temp);
							
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(CEEButtons.this, 
									"Could not load from " + f.toString() 
									+ "\n" + ex);
						}
					}
				}
			});
			this.add(loadButton);

			JButton saveButton = setButtonLayout("Save...");
			saveButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showSaveDialog(CEEButtons.this);
					if(returnVal == JFileChooser.APPROVE_OPTION) {
						File f = chooser.getSelectedFile();
						try {
							ColorSet temp = new ColorSet();
							colorExtractorEditor.copyStateTo(temp);
							temp.saveEisen(f);
							
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(CEEButtons.this, 
									"Could not save to " + f.toString() 
									+ "\n" + ex);
						}
					}
				}
			});
			this.add(saveButton);

			JButton makeButton = setButtonLayout("Make Preset");
			makeButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					ColorSet temp = new ColorSet();
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
		
		private JTextField logTextField;
		private JCheckBox logCheckBox;
		
		LogScaleSelector() {
			
			this.setLayout(new MigLayout());
			this.setOpaque(false);
			this.setBorder(BorderFactory.createLineBorder(GUIColors.BORDERS, 
					EtchedBorder.LOWERED));
			
			logCheckBox = new JCheckBox("Log (base 2)");
			logCheckBox.setBackground(GUIColors.BG_COLOR);
			logCheckBox.setSelected(m_drawer.getLogTransform());
			logCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					
					m_drawer.setLogTransform(logCheckBox.isSelected());
					logTextField.setEnabled(logCheckBox.isSelected());
					m_drawer.setLogBase(2.0);
					m_drawer.notifyObservers();
				}
			});
			this.add(logCheckBox, "pushx, span, wrap");
			
			logTextField = new JTextField(10);
			logTextField.setText("" +m_drawer.getLogCenter());
			this.add(new JLabel("Center:"), "alignx 50%");
			logTextField.getDocument().addDocumentListener(
					new DocumentListener() {
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					textBoxChanged();
				}
				@Override
				public void insertUpdate(DocumentEvent e) {
					textBoxChanged();
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					textBoxChanged();
				}
			});
			this.add(logTextField, "alignx 50%");
			logTextField.setEnabled(logCheckBox.isSelected());
			
		}
		
		private void textBoxChanged() {
			
			Double d;
			try {
				d = new Double(logTextField.getText());
				m_drawer.setLogCenter(d.doubleValue());
				m_drawer.notifyObservers();
				
			} catch (Exception e) {
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
			this.setBackground(GUIColors.BG_COLOR);
			int nPresets = m_presets.getNumPresets();
			JButton [] buttons = new JButton[nPresets];
			for (int i = 0; i < nPresets; i++) {
				JButton presetButton = setButtonLayout(
						(m_presets.getPresetNames()) [i]);
				final int index = i;
				presetButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						
						colorExtractorEditor.copyStateFrom(
								m_presets.getColorSet(index));
					}
				});
				this.add(presetButton);
				
				buttons[index] = presetButton;
			}
		}
	}
	
	public JButton setButtonLayout(String title){
		
		Font buttonFont = new Font("Sans Serif", Font.PLAIN, 14);
		
		JButton button = new JButton(title);
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
  		button.setPreferredSize(d);
  		
  		button.setFont(buttonFont);
  		button.setOpaque(true);
  		button.setBackground(GUIColors.ELEMENT);
  		button.setForeground(GUIColors.BG_COLOR);
  		
  		return button;
	}
	
	public JPanel setPanelLayout() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.setBackground(GUIColors.BG_COLOR);
		panel.setBorder(BorderFactory.createLineBorder(GUIColors.BORDERS, 
						EtchedBorder.LOWERED));
		
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
	private ColorExtractor colorExtractor;
	private ColorPanel colorPanel [] = new ColorPanel[4];
	
	public ColorExtractorEditor(ColorExtractor colorExtractor) {
		
		this.colorExtractor = colorExtractor;
		this.setOpaque(false);
		
		for (int i = 0; i < 4; i++) {
			
			colorPanel[i] = new ColorPanel(i);
			add(colorPanel[i]);
		}
	}

	public void copyStateFrom(ColorSet source) {
		
		colorPanel[UP].setColor(source.getUp());
		colorPanel[ZERO].setColor(source.getZero());
		colorPanel[DOWN].setColor(source.getDown());
		colorPanel[MISSING].setColor(source.getMissing());

	}
	
	public void copyStateTo(ColorSet dest) {
		
		dest.setUp(colorPanel[UP].getColor());
		dest.setZero(colorPanel[ZERO].getColor());
		dest.setDown(colorPanel[DOWN].getColor());
		dest.setMissing(colorPanel[MISSING].getColor());
	}

	class ColorPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		
		ColorIcon colorIcon;
		int type;
		
		public ColorPanel(int i) {
			
			type = i;
			redoComps();
		} 
		
		public void redoComps() {
			
			removeAll();
			this.setOpaque(false);
			colorIcon = new ColorIcon(10, 10, getColor());
			JButton pushButton = new JButton(getLabel(), colorIcon);
			Font buttonFont = new Font("Sans Serif", Font.PLAIN, 14);
			
	  		Dimension d = pushButton.getPreferredSize();
	  		d.setSize(d.getWidth()*1.5, d.getHeight()*1.5);
	  		pushButton.setPreferredSize(d);
	  		
	  		pushButton.setFont(buttonFont);
	  		pushButton.setOpaque(true);
	  		pushButton.setBackground(GUIColors.ELEMENT);
	  		pushButton.setForeground(GUIColors.BG_COLOR);
			pushButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					Color trial = JColorChooser.showDialog(
							ColorExtractorEditor.this, "Pick Color for " 
					+ getLabel(), getColor());
					if (trial != null) {
						setColor(trial);
					}
				}
			});

			add(pushButton);
		}
		
		private void setColor(Color c) {
			
			switch(type) {
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
//			redoComps();
			colorExtractor.notifyObservers();
			repaint();
		}
		
		private String getLabel() {
			
			switch(type) {
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
			
			switch(type) {
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
		
		private int width, height;
		private Color color;
		ColorIcon(int x, int y, Color c) {
			width = x;
			height = y;
			color = c;
		}
		
		public void setColor(Color c) {
			
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
		public void paintIcon(Component c, Graphics g, int x, int y) {
			
			Color old = g.getColor();
			g.setColor(color);
			g.fillRect(x, y, width, height);
			g.setColor(Color.black);
			g.drawRect(x, y, width, height);
			g.setColor(old);
		}
	}
}
