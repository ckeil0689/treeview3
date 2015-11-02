/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.image.BufferedImage;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 * The purpose of this class is to collect all the messy special-case code for
 * exporting to bitmap images from java.
 *
 * Unlike the PpmWriter, it is fairly specific to Treeview.
 *
 * It will consist entirely of static methods
 */
public class BitmapWriter {
	public static final String[] formats = new String[] { "png", "ppm", "jpg" };

	/**
	 * write image in the specified format to the output stream, popping up
	 * dialogs with specified parent in the event of a problem.
	 *
	 */
	public static boolean writeBitmap(final BufferedImage i,
			final String format, final OutputStream output,
			final JComponent parent) {
		if (formats[0].equals(format)) { // png
			try {
				return writePng(i, output, parent);
			} catch (final Exception e) {
				JOptionPane.showMessageDialog(parent, new JTextArea(
						"PNG export had problem " + e));
				LogBuffer.println("Exception " + e);
				e.printStackTrace();
				return false;
			}
		} else if (formats[1].equals(format)) { // ppm
			try {
				PpmWriter.writePpm(i, output);
				return true;
			} catch (final Exception e) {
				JOptionPane.showMessageDialog(parent, new JTextArea(
						"PPM export had problem " + e));
				LogBuffer.println("Exception " + e);
				e.printStackTrace();
				return false;
			}
		} else if (formats[2].equals(format)) { // jpeg
			try {
				return writeJpg(i, output, parent);
			} catch (final Exception e) {
				JOptionPane.showMessageDialog(parent, new JTextArea(
						"JPEG export had problem " + e));
				LogBuffer.println("Exception " + e);
				e.printStackTrace();
				return false;
			}
		} else {
			JOptionPane.showMessageDialog(parent, new JTextArea("Format "
					+ format + " not supported."));
			return false;
		}
	}

	/**
	 * return true on success, false on failure.
	 *
	 * may throw up warning screens or messages using parent.
	 */
	public static boolean writeJpg(final BufferedImage i,
			final OutputStream output, final JComponent parent)
			throws java.io.IOException {
		final String version = System.getProperty("java.version");
		if (version.startsWith("1.4.0") || version.startsWith("1.4.1")) {
			JOptionPane
					.showMessageDialog(
							parent,
							new JTextArea(
									"You are using Java Version "
											+ version
											+ "\n which has known issues with JPEG export. \nPlease try PNG format or upgrade to 1.4.2 or later if this export fails."));
		}

		try {
			ImageIO.write(i, "jpg", output);
		} catch (final Exception e) {
			JOptionPane
					.showMessageDialog(
							parent,
							new JTextArea(
									"Problem Saving JPEG "
											+ e
											+ "\n"
											+ "Jpeg export requires Java Version 1.4.1 or better.\n"
											+ "You are running "
											+ version
											+ "\n"
											+ "If problem persists, try PPM format, which should always work"));
			return false;
		}
		return true;
	}

	/**
	 * return true on success, false on failure.
	 *
	 * may throw up warning screens or messages using parent.
	 */
	public static boolean writePng(final BufferedImage i,
			final OutputStream output, final JComponent parent)
			throws java.io.IOException {
		final String version = System.getProperty("java.version");

		try {
			ImageIO.write(i, "png", output);
		} catch (final Exception e) {
			JOptionPane
					.showMessageDialog(
							parent,
							new JTextArea(
									"Problem Saving PNG "
											+ e
											+ " \n"
											+ "Png export requires Java Version 1.4.1 or better.\n"
											+ "You are running "
											+ version
											+ "\n"
											+ "If problem persists, try PPM format, which should always work"));
			return false;
		}
		return true;
	}
}