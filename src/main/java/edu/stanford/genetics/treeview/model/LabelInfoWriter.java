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
 * class that write out header info to flat file.
 */
public class LabelInfoWriter {
	private final LabelInfo headerInfo;

	/**
	 * @param atrHeaderInfo
	 *            headerInfo to write out
	 */
	public LabelInfoWriter(final LabelInfo atrHeaderInfo) {
		headerInfo = atrHeaderInfo;
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
			// first, the header.
			final String[] names = headerInfo.getPrefixes();
			if (names.length > 0) {
				out.write(names[0]);
			}
			for (int i = 1; i < names.length; i++) {
				out.write("\t");
				out.write(names[i]);
			}
			out.write("\n");
			final int rows = headerInfo.getNumLabels();
			for (int row = 0; row < rows; row++) {
				final String[] headers = headerInfo.getLabels(row);
				if (headers.length > 0) {
					out.write(headers[0]);
				}
				for (int i = 1; i < headers.length; i++) {
					out.write("\t");
					if (headers[i] != null) {
						out.write(headers[i]);
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
