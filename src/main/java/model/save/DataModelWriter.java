/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package model.save;

import model.data.labels.LabelInfo;
import model.data.matrix.DataModel;
import model.export.labels.LabelInfoWriter;
import model.fileType.DataModelFileType;
import model.fileType.FileSet;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

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
		if (writeAtr(fileSet.getAtr())) {
			written.add(DataModelFileType.ATR);
		}
		if (writeGtr(fileSet.getGtr())) {
			written.add(DataModelFileType.GTR);
		}
		if (writeCdt(fileSet.getCdt())) {
			written.add(DataModelFileType.CDT);
		}
		return written;
	}

	public Set<DataModelFileType> writeIncremental(final FileSet fileSet) {
		final EnumSet<DataModelFileType> written = EnumSet
				.noneOf(DataModelFileType.class);
		if (dataModel.aidFound() && dataModel.getAtrLabelInfo().getModified()) {
			if (writeAtr(fileSet.getAtr())) {
				written.add(DataModelFileType.ATR);
			}
		}
		if (dataModel.gidFound() && dataModel.getGtrLabelInfo().getModified()) {
			if (writeGtr(fileSet.getGtr())) {
				written.add(DataModelFileType.GTR);
			}
		}
		if (dataModel.getDataMatrix().getModified()
				|| dataModel.getColLabelInfo().getModified()
				|| dataModel.getRowLabelInfo().getModified()) {
			if (writeCdt(fileSet.getCdt())) {
				written.add(DataModelFileType.CDT);
			}
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
		return writeTree(dataModel.getAtrLabelInfo(), atr);
	}

	/**
	 * write out gtr to file
	 *
	 * @param gtr
	 *            complete path of file to write to
	 */
	private boolean writeGtr(final String gtr) {
		return writeTree(dataModel.getGtrLabelInfo(), gtr);
	}

	/**
	 * write out LabelInfo of tree to file
	 *
	 * @param labelInfo
	 *            LabelInfo to write out
	 * @param filePath
	 *            complete path of file to write to
	 */
	private boolean writeTree(final LabelInfo labelInfo, final String file) {
		final LabelInfoWriter writer = new LabelInfoWriter(labelInfo);
		try {
			final String spool = file + ".spool";
			writer.write(spool);
			final File f = new File(spool);
			if (f.renameTo(new File(file))) {
				labelInfo.setModified(false);
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
				dataModel.getColLabelInfo().setModified(false);
				dataModel.getRowLabelInfo().setModified(false);
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
