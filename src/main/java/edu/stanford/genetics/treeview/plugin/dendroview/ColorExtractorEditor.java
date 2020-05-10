package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import util.GUIFactory;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * This class allows editing of a color set...
 */

public class ColorExtractorEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ColorExtractor colorExtractor;

	public ColorExtractorEditor(final ColorExtractor colorExtractor) {

		LogBuffer.println("Instantiated colorextractor editor");
		this.colorExtractor = colorExtractor;
		this.setOpaque(false);

		add(new ColorPanel());
	}

	public void copyStateFrom(final ColorSet source) {

		colorExtractor.setMissingColor(source.getMissing());
	}

	public void copyStateTo(final ColorSet dest) {

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
							ColorExtractorEditor.this, "Pick Color for "
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
