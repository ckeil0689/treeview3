package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import Utilities.GUIFactory;

/**
 * This class allows editing of a color set...
 */

public class ColorExtractorEditor2 extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ColorExtractor2 colorExtractor;

	public ColorExtractorEditor2(final ColorExtractor2 colorExtractor) {

		this.colorExtractor = colorExtractor;
		this.setOpaque(false);

		add(new ColorPanel());
	}

	public void copyStateFrom(final ColorSet2 source) {

		colorExtractor.setMissingColor(source.getMissing());
	}

	public void copyStateTo(final ColorSet2 dest) {

		dest.setMissing(colorExtractor.getMissing());
	}

	class ColorPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public ColorPanel() {

			redoComps();
		}

		public void redoComps() {

			removeAll();
			this.setOpaque(false);
			final JButton pushButton = GUIFactory.createBtn("Missing");
			pushButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {

					final Color trial = JColorChooser.showDialog(
							ColorExtractorEditor2.this, "Pick Color for "
									+ "Missing", colorExtractor.getMissing());
					if (trial != null) {
						colorExtractor.setMissingColor(trial);

						colorExtractor.notifyObservers();
						repaint();
					}
				}
			});

			add(pushButton);
		}
	}
}
