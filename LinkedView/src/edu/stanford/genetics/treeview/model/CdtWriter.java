package edu.stanford.genetics.treeview.model;

import java.io.FileWriter;
import java.io.IOException;

import edu.stanford.genetics.treeview.DataModel;

public class CdtWriter {
	DataModel dataModel;
	public CdtWriter(DataModel dataModel) {
		this.dataModel = dataModel;
	}

	public void write(String spool) throws IOException {
		FileWriter out = null;
		try {
			out = new FileWriter(spool);
			// first, the array annotations.
			for (int headerrow = 0; headerrow < dataModel.getArrayHeaderInfo().getNumNames(); headerrow++) {
				for (int column = 0; column < dataModel.getArrayHeaderInfo().getNumHeaders() + dataModel.getGeneHeaderInfo().getNumNames(); column++) {
					if (column > 0)
						out.write("\t");
					if (column < dataModel.getGeneHeaderInfo().getNumNames()) {
						if (headerrow == 0) {
							// we need to write out the names from the gene header info
							printNotNull(out,dataModel.getGeneHeaderInfo().getNames()[column]);
						} else if (column == 0) {
							// for the first column, write out the name from the array header info. 
							printNotNull(out,dataModel.getArrayHeaderInfo().getNames()[headerrow]);
						} else {
							// otherwise, just leave empty.
						}
					} else {
						// write out actual array annotation.
						printNotNull(out,dataModel.getArrayHeaderInfo().getHeader(column - dataModel.getGeneHeaderInfo().getNumNames(), headerrow));
					}
				}
				out.write("\n");
			}
			// next the data rows.
			for (int gene = 0; gene < dataModel.getGeneHeaderInfo().getNumHeaders(); gene++) {
				for (int column = 0; column < dataModel.getArrayHeaderInfo().getNumHeaders() + dataModel.getGeneHeaderInfo().getNumNames(); column++) {
					if (column > 0)
						out.write("\t");
					if (column < dataModel.getGeneHeaderInfo().getNumNames()) {
						printNotNull(out, dataModel.getGeneHeaderInfo().getHeader(gene, column));
					} else {
						// write out actual data.
						printNotNull2(out, dataModel.getDataMatrix().getValue(column - dataModel.getGeneHeaderInfo().getNumNames(), gene));
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

	private void printNotNull2(FileWriter out, double value) throws IOException {
		if (value != DataModel.EMPTY && value != DataModel.EMPTY)
			out.write("" + value);
		
	}

	private void printNotNull(FileWriter out, String value) throws IOException {
		if (value != null) out.write(value);
	}

}
