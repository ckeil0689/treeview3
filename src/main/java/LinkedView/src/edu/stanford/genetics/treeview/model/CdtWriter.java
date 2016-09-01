package edu.stanford.genetics.treeview.model;

import java.io.FileWriter;
import java.io.IOException;

import Utilities.Helper;
import edu.stanford.genetics.treeview.DataModel;

public class CdtWriter {

	DataModel dataModel;

	public CdtWriter(final DataModel dataModel) {

		this.dataModel = dataModel;
	}

	public void write(final String spool) throws IOException {

		FileWriter out = null;
		try {
			out = new FileWriter(spool);
			// first, the array annotations.
			for (int headerrow = 0; headerrow < dataModel.getColHeaderInfo()
					.getNumNames(); headerrow++) {
				for (int column = 0; column < dataModel.getColHeaderInfo()
						.getNumHeaders()
						+ dataModel.getRowHeaderInfo().getNumNames(); column++) {
					if (column > 0) {
						out.write("\t");
					}
					if (column < dataModel.getRowHeaderInfo().getNumNames()) {
						if (headerrow == 0) {
							// we need to write out the names from the gene
							// header info
							printNotNull(out, dataModel.getRowHeaderInfo()
									.getNames()[column]);
						} else if (column == 0) {
							// for the first column, write out the name from the
							// array header info.
							printNotNull(out, dataModel.getColHeaderInfo()
									.getNames()[headerrow]);
						} else {
							// otherwise, just leave empty.
						}
					} else {
						// write out actual array annotation.
						printNotNull(
								out,
								dataModel.getColHeaderInfo().getHeader(
										column
												- dataModel.getRowHeaderInfo()
														.getNumNames(),
										headerrow));
					}
				}
				out.write("\n");
			}
			// next the data rows.
			for (int gene = 0; gene < dataModel.getRowHeaderInfo()
					.getNumHeaders(); gene++) {
				for (int column = 0; column < dataModel.getColHeaderInfo()
						.getNumHeaders()
						+ dataModel.getRowHeaderInfo().getNumNames(); column++) {
					if (column > 0) {
						out.write("\t");
					}
					if (column < dataModel.getRowHeaderInfo().getNumNames()) {
						printNotNull(out, dataModel.getRowHeaderInfo()
								.getHeader(gene, column));
					} else {
						// write out actual data.
						printNotNull2(
								out,
								dataModel.getDataMatrix().getValue(
										column
												- dataModel.getRowHeaderInfo()
														.getNumNames(), gene));
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

	private void printNotNull2(final FileWriter out, final double value)
			throws IOException {

		if (Helper.nearlyEqual(value, DataModel.EMPTY)) {
			out.write("" + value);
		}
	}

	private void printNotNull(final FileWriter out, final String value)
			throws IOException {

		if (value != null) {
			out.write(value);
		}
	}

}
