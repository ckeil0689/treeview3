/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.model;

import java.io.FileWriter;
import java.io.IOException;

import edu.stanford.genetics.treeview.LabelInfo;

/**
 * class that write out label info to flat file.
 */
public class LabelInfoWriter {
	private final LabelInfo labelInfo;

	/**
	 * @param atrLabelInfo
	 *            labelInfo to write out
	 */
	public LabelInfoWriter(final LabelInfo atrLabelInfo) {
		labelInfo = atrLabelInfo;
	}

	/**
	 * @param atr
	 *            file to write to
	 * @throws IOException
	 */
	public void write(final String atr) throws IOException {
		FileWriter out = null;
		try {
			out = new FileWriter(atr);
			// first, the label type
			final String[] labelTypes = labelInfo.getLabelTypes();
			if (labelTypes.length > 0) {
				out.write(labelTypes[0]);
			}
			for (int i = 1; i < labelTypes.length; i++) {
				out.write("\t");
				out.write(labelTypes[i]);
			}
			out.write("\n");
			final int rows = labelInfo.getNumLabels();
			for (int row = 0; row < rows; row++) {
				final String[] labels = labelInfo.getLabels(row);
				if (labels.length > 0) {
					out.write(labels[0]);
				}
				for (int i = 1; i < labels.length; i++) {
					out.write("\t");
					if (labels[i] != null) {
						out.write(labels[i]);
					}
				}
				out.write("\n");
			}
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}

}
