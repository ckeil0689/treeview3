/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.model;

import java.io.FileWriter;
import java.io.IOException;

import edu.stanford.genetics.treeview.HeaderInfo;

/**
 * class that write out header info to flat file.
 */
public class HeaderInfoWriter {
	private final HeaderInfo headerInfo;

	/**
	 * @param atrHeaderInfo
	 *            headerInfo to write out
	 */
	public HeaderInfoWriter(final HeaderInfo atrHeaderInfo) {
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
			final String[] names = headerInfo.getNames();
			if (names.length > 0) {
				out.write(names[0]);
			}
			for (int i = 1; i < names.length; i++) {
				out.write("\t");
				out.write(names[i]);
			}
			out.write("\n");
			final int rows = headerInfo.getNumHeaders();
			for (int row = 0; row < rows; row++) {
				final String[] headers = headerInfo.getHeader(row);
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
