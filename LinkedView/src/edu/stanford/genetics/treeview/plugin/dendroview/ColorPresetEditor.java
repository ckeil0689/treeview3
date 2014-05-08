/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ColorPresetEditor.java,v $
 * $Revision: 1.2 $
 * $Date: 2006-10-17 22:17:17 $
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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import edu.stanford.genetics.treeview.SettingsPanel;

/**
 * This class allows graphical editing of ColorPresets Also included is a class
 * to pop up a configuration window.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.2 $ $Date: 2006-10-17 22:17:17 $
 */
public class ColorPresetEditor extends JPanel implements SettingsPanel {

	private static final long serialVersionUID = 1L;

	private final ColorPresets2 presets;
	private Window window;

	/**
	 * This class is to enable editing of a UrlPresets object. XXX HACK I
	 * botched the design pretty badly here, but I'm too busy to clean it up
	 * now.
	 * 
	 * @param up
	 *            Description of the Parameter
	 */
	public ColorPresetEditor(final ColorPresets2 up) {

		super();
		presets = up;
		presetEditPanel = new PresetEditPanel();
		add(presetEditPanel);
	}

	/**
	 * pops up a configuration dialog.
	 * 
	 * @param f
	 *            Description of the Parameter
	 */
	public void showConfig(final Frame f) {

		if (window == null) {
			final Dialog d = new Dialog(f, getTitle(), true);
			d.setLayout(new BorderLayout());
			d.add(new JScrollPane(presetEditPanel));
			// d.add(presetEditPanel);
			d.add(new JLabel(getTitle()), BorderLayout.NORTH);
			d.add(new ButtonPanel(), BorderLayout.SOUTH);
			d.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(final WindowEvent we) {
					we.getWindow().dispose();
				}
			});
			d.pack();
			window = d;
		}
		window.setVisible(true);
	}

	/**
	 * Test program, prints out results of editing on exit.
	 * 
	 * @param argv
	 *            ignored
	 */
	public static void main(final String[] argv) {

		final ColorPresets2 p = new ColorPresets2(Preferences.userRoot().node(
				"ColorPresets"));
		final ColorPresetEditor e = new ColorPresetEditor(p);
		final Frame f = new Frame(e.getTitle());
		e.showConfig(f);
		System.out.println("on exit, presets were\n" + p.toString());
		System.exit(0);
	}

	/**
	 * Adds this editor pane to the specified frame.
	 */
	public void addToFrame(final Frame f) {
		f.setLayout(new BorderLayout());
		presetEditPanel = new PresetEditPanel();
		f.add(new JScrollPane(presetEditPanel));
		// f.add(new Label(getTitle(),Label.CENTER), BorderLayout.NORTH);
		f.add(new ButtonPanel(), BorderLayout.SOUTH);
		window = f;
	}

	private String title = "Color Preset Editor";

	/**
	 * The title to be displayed when this panel is in a window.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * The title to be displayed when this panel is in a window.
	 */
	public void setTitle(final String s) {
		title = s;
	}

	private PresetEditPanel presetEditPanel;

	/** Changes the GUI to represent the state of the presets */
	@Override
	public void synchronizeFrom() {
		presetEditPanel.initialize();
		presetEditPanel.redoLayout();
	}

	/** Changes the presets to match the state of the GUI */
	@Override
	public void synchronizeTo() {
		presetEditPanel.saveAll();
	}

	// inner classes

	private class ButtonPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		ButtonPanel() {
			final JButton save_button = new JButton("Save");
			save_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					synchronizeTo();
					window.dispose();
				}
			});
			add(save_button);

			final JButton cancel_button = new JButton("Cancel");
			cancel_button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					synchronizeFrom();
					window.dispose();
				}
			});
			add(cancel_button);
		}
	}

	private class PresetEditPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		PresetEditPanel() {
			initialize();
			redoLayout();
		}

		private GridBagConstraints gbc;
		private JRadioButton[] defaultButtons;
		private JTextField[] presetNames;
		private ColorSet2[] presetColors;
		private final ButtonGroup bob = new ButtonGroup();

		private void initialize() {

			final int nPresets = presets.getNumPresets();
			defaultButtons = new JRadioButton[nPresets];
			presetNames = new JTextField[nPresets];
			presetColors = new ColorSet2[nPresets];

			for (int i = 0; i < nPresets; i++) {
				initializePreset(i);
				bob.add(defaultButtons[i]);
			}
			if (nPresets > 0) {
				if (presets.getDefaultIndex() == -1
						|| presets.getDefaultIndex() >= defaultButtons.length) {
					defaultButtons[0].setSelected(true);
				} else {
					defaultButtons[presets.getDefaultIndex()].setSelected(true);
				}
			}
		}

		/**
		 * Creates components and copies state from presets for preset i...
		 * 
		 * @param i
		 *            index of the preset
		 */
		private void initializePreset(final int i) {

			final int index = i;
			final ColorSet2 colorSet = new ColorSet2();
			final JTextField nameField = new JTextField();
			nameField.setText((presets.getPresetNames())[index]);
			presetNames[index] = nameField;

			colorSet.copyStateFrom(presets.getColorSet(index));
			presetColors[index] = colorSet;

			final JRadioButton set = new JRadioButton();
			defaultButtons[index] = set;
			set.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					presets.setDefaultIndex(index);
				}
			});
		}

		/**
		 * Assumes that defaultButtons, presetNames, and presetColors have been
		 * properly set up.
		 */
		public void redoLayout() {
			final int nPresets = defaultButtons.length - 1;
			removeAll();
			setLayout(new GridBagLayout());
			gbc = new GridBagConstraints();
			gbc.weighty = 100;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTH;
			gbc.gridy = 0;
			gbc.gridx = 0;

			gbc.weighty = 100;
			add(new JLabel("Modify Color Presets", SwingConstants.CENTER), gbc);
			gbc.weighty = 0;
			gbc.gridy = 1;
			gbc.gridx = 0;
			add(new JLabel("Name"), gbc);
			gbc.gridx = 1;
			add(new JLabel("Colors"), gbc);
			gbc.gridx = 3;
			add(new JLabel("Default?"), gbc);

			for (int i = 0; i < nPresets; i++) {
				gbc.gridy++;
				addPreset(i);
			}

			gbc.gridy++;

			final JButton addP = new JButton("Add New");
			addP.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					createPreset();
				}
			});
			gbc.gridy++;
			gbc.gridx = 2;
			add(addP, gbc);

			final JButton addS = new JButton("Add Standards");
			addS.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final ColorSet2[] toAdd = ColorPresets2.defaultColorSets;
					for (int i = 0; i < toAdd.length; i++) {

						addPreset(toAdd[i]);
					}
				}
			});
			gbc.gridy++;
			gbc.gridx = 2;
			add(addS, gbc);

			revalidate();
			repaint();
		}

		/**
		 * This just adds a preset to the GUI, assuming that presetNames,
		 * presetColors and defaultButtons are properly set up...
		 * 
		 * @param i
		 *            The index of the preset to add
		 */
		private void addPreset(final int i) {
			final int index = i;
			gbc.gridx = 0;
			add(presetNames[index], gbc);

			gbc.gridx = 1;
			gbc.weightx = 100;
			add(new ColorSetEditor2(presetColors[index]), gbc);

			gbc.gridx = 2;
			gbc.weightx = 0;
			final JButton rem = new JButton("Remove");
			rem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					removePreset(index);
				}
			});
			add(rem, gbc);

			gbc.gridx = 3;
			add(defaultButtons[index], gbc);
		}

		private void saveAll() {
			// first, make sure that there's the same number of presets...
			final int n = presetNames.length - 1;// number of presets to be
			int o = presets.getNumPresets();// current number of presets...

			while (n > o) {// need to add more presets...
				final ColorSet2 temp = new ColorSet2();
				temp.setName("Preset" + o);
				presets.addColorSet(temp);
				o++;
			}

			while (o > n) {// need to delete some...
				o--;
				presets.removeColorSet(o);
			}

			// next, copy the state over
			for (int i = 0; i < n; i++) {
				presetColors[i].setName(presetNames[i].getText());
			}
			for (int i = 0; i < n; i++) {
				presets.getColorSet(i).copyStateFrom(presetColors[i]);
			}
		}

		// this removes a preset
		private void removePreset(final int mark) {
			final ColorSet2[] tPresetColors = new ColorSet2[presetColors.length - 1];
			final JTextField[] tPresetNames = new JTextField[presetNames.length - 1];
			final JRadioButton[] tDefaultButtons = new JRadioButton[defaultButtons.length - 1];
			for (int i = 0; i < tPresetColors.length; i++) {
				int j = i;
				if (i >= mark) {
					j++;
				}
				tPresetColors[i] = presetColors[j];
				tPresetNames[i] = presetNames[j];
				tDefaultButtons[i] = defaultButtons[j];
			}
			bob.remove(defaultButtons[mark]);
			int selectedIndex = 0;
			for (int i = 0; i < (defaultButtons.length); i++) {
				if (defaultButtons[i] == null) {
					continue;
				}
				if (defaultButtons[i].isSelected()) {
					selectedIndex = i;
				}
			}

			if (selectedIndex > (tPresetNames.length - 2)) {
				tDefaultButtons[tPresetNames.length - 2].setSelected(true);
			} else {
				tDefaultButtons[selectedIndex].setSelected(true);
			}

			presetNames = tPresetNames;
			presetColors = tPresetColors;
			defaultButtons = tDefaultButtons;
			redoLayout();

		}

		// this creates a brand new preset and adds stuff to the GUI for it...

		private void createPreset() {

			final ColorSet2 toAdd = new ColorSet2();
			addPreset(toAdd);
		}

		private void addPreset(final ColorSet2 toAdd) {

			final ColorSet2[] tPresetColors = new ColorSet2[presetColors.length + 1];
			final JTextField[] tPresetNames = new JTextField[presetNames.length + 1];
			final JRadioButton[] tDefaultButtons = new JRadioButton[defaultButtons.length + 1];

			for (int i = 0; i < presetColors.length; i++) {

				tPresetColors[i] = presetColors[i];
				tPresetNames[i] = presetNames[i];
				tDefaultButtons[i] = defaultButtons[i];
			}

			// for null...
			final int newIndex = tPresetNames.length - 2;
			tPresetColors[newIndex + 1] = tPresetColors[newIndex];
			tPresetNames[newIndex + 1] = presetNames[newIndex];
			tDefaultButtons[newIndex + 1] = tDefaultButtons[newIndex];

			tPresetNames[newIndex] = new JTextField("Preset" + newIndex);
			tPresetColors[newIndex] = toAdd;
			final JRadioButton set = new JRadioButton();
			tDefaultButtons[newIndex] = set;
			set.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					presets.setDefaultIndex(newIndex);
				}
			});
			bob.add(set);

			presetNames = tPresetNames;
			presetColors = tPresetColors;
			defaultButtons = tDefaultButtons;
			redoLayout();
		}

		/*
		 * private void addNonePreset(int i) { final int index = i; final
		 * JTextField templateField = new JTextField(); final JTextField
		 * nameField = new JTextField(); gbc.gridx = 0;
		 * nameField.setText("None"); presetNames[index] = nameField;
		 * add(nameField, gbc); gbc.gridx = 1; gbc.weightx = 100; //
		 * templateField.setText(presets.getTemplate(index));
		 * presetTemplates[index] = null; // add(templateField, gbc); gbc.gridx
		 * = 2; gbc.weightx = 0; JRadioButton set = new JRadioButton();
		 * defaultButtons[index] = set; set.addActionListener(new
		 * ActionListener() { public void actionPerformed(ActionEvent e) {
		 * presets.setDefaultPreset(-1); } }); add(set, gbc); }
		 */
	}
}
