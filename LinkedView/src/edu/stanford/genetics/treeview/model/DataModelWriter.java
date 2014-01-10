/*
 * Created on Mar 7, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.model;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.JOptionPane;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DataModelFileType;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.HeaderInfo;

/**
 * 
 * The purpose of this class is to write a DataModel out to flat file format.
 * 
 */
public class DataModelWriter {
	DataModel dataModel;

	public DataModelWriter(final DataModel source) {
		dataModel = source;
	}

	/**
	 * Write all parts of Datamodel out to disk
	 * 
	 * @param fileSet
	 *            fileset to write to
	 * @return
	 */
	public Set<DataModelFileType> writeAll(final FileSet fileSet) {
		final EnumSet<DataModelFileType> written = EnumSet
				.noneOf(DataModelFileType.class);
		if (writeAtr(fileSet.getAtr()))
			written.add(DataModelFileType.ATR);
		if (writeGtr(fileSet.getGtr()))
			written.add(DataModelFileType.GTR);
		if (writeCdt(fileSet.getCdt()))
			written.add(DataModelFileType.CDT);
		return written;
	}

	public Set<DataModelFileType> writeIncremental(final FileSet fileSet) {
		final EnumSet<DataModelFileType> written = EnumSet
				.noneOf(DataModelFileType.class);
		if (dataModel.aidFound() && dataModel.getAtrHeaderInfo().getModified()) {
			if (writeAtr(fileSet.getAtr()))
				written.add(DataModelFileType.ATR);
		}
		if (dataModel.gidFound() && dataModel.getGtrHeaderInfo().getModified()) {
			if (writeGtr(fileSet.getGtr()))
				written.add(DataModelFileType.GTR);
		}
		if (dataModel.getDataMatrix().getModified()
				|| dataModel.getArrayHeaderInfo().getModified()
				|| dataModel.getGeneHeaderInfo().getModified()) {
			if (writeCdt(fileSet.getCdt()))
				written.add(DataModelFileType.CDT);
		}
		return written;
	}

	/**
	 * write out atr to file
	 * 
	 * @param atr
	 *            complete path of file to write to
	 */
	private boolean writeAtr(final String atr) {
		return writeTree(dataModel.getAtrHeaderInfo(), atr);
	}

	/**
	 * write out gtr to file
	 * 
	 * @param gtr
	 *            complete path of file to write to
	 */
	private boolean writeGtr(final String gtr) {
		return writeTree(dataModel.getGtrHeaderInfo(), gtr);
	}

	/**
	 * write out HeaderInfo of tree to file
	 * 
	 * @param info
	 *            HeaderInfo to write out
	 * @param filePath
	 *            complete path of file to write to
	 */
	private boolean writeTree(final HeaderInfo info, final String file) {
		final HeaderInfoWriter writer = new HeaderInfoWriter(info);
		try {
			final String spool = file + ".spool";
			writer.write(spool);
			final File f = new File(spool);
			if (f.renameTo(new File(file))) {
				info.setModified(false);
			}
			return true;
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(null, "Error writing " + file + " "
					+ e, "Save Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		}
	}

	private boolean writeCdt(final String file) {
		final CdtWriter writer = new CdtWriter(dataModel);
		try {
			final String spool = file + ".spool";
			writer.write(spool);
			final File f = new File(spool);
			if (f.renameTo(new File(file))) {
				dataModel.getDataMatrix().setModified(false);
				dataModel.getArrayHeaderInfo().setModified(false);
				dataModel.getGeneHeaderInfo().setModified(false);
			}
			return true;
		} catch (final IOException e) {
			JOptionPane.showMessageDialog(null, "Error writing " + file + " "
					+ e, "Save Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		}
	}

}
