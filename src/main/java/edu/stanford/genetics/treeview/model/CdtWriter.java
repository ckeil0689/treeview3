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
			for (int colPrefixIdx = 0; colPrefixIdx < dataModel.getColLabelInfo()
					.getNumPrefixes(); colPrefixIdx++) {
				for (int colIdx = 0; colIdx < dataModel.getColLabelInfo()
						.getNumLabels()
						+ dataModel.getRowLabelInfo().getNumPrefixes(); colIdx++) {
					if (colIdx > 0) {
						out.write("\t");
					}
					if (colIdx < dataModel.getRowLabelInfo().getNumPrefixes()) {
						if (colPrefixIdx == 0) {
							// we need to write out the names from the row
							// label info
							printNotNull(out, dataModel.getRowLabelInfo()
									.getPrefixes()[colIdx]);
						} else if (colIdx == 0) {
							// for the first column, write out the name from the
							// column label info.
							printNotNull(out, dataModel.getColLabelInfo()
									.getPrefixes()[colPrefixIdx]);
						} else {
							// otherwise, just leave empty.
						}
					} else {
						// write out actual array annotation.
						printNotNull(
								out,
								dataModel.getColLabelInfo().getLabel(
										colIdx
												- dataModel.getRowLabelInfo()
														.getNumPrefixes(),
										colPrefixIdx));
					}
				}
				out.write("\n");
			}
			// next the data rows.
			for (int rowIdx = 0; rowIdx < dataModel.getRowLabelInfo()
					.getNumLabels(); rowIdx++) {
				for (int colIdx = 0; colIdx < dataModel.getColLabelInfo()
						.getNumLabels()
						+ dataModel.getRowLabelInfo().getNumPrefixes(); colIdx++) {
					if (colIdx > 0) {
						out.write("\t");
					}
					if (colIdx < dataModel.getRowLabelInfo().getNumPrefixes()) {
						printNotNull(out, dataModel.getRowLabelInfo()
								.getLabel(rowIdx, colIdx));
					} else {
						// write out actual data.
						printNotNull2(
								out,
								dataModel.getDataMatrix().getValue(
										colIdx
												- dataModel.getRowLabelInfo()
														.getNumPrefixes(), rowIdx));
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
