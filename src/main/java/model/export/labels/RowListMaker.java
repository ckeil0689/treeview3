/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package model.export.labels;

import model.data.labels.LabelInfo;
import model.data.matrix.DataMatrix;
import model.data.matrix.DataModel;
import model.data.trees.TreeSelectionI;
import preferences.ConfigNodePersistent;
import util.LogBuffer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

/**
 * This class is designed to save lists of genes to a file.
 *
 * The class will pop up a window and prompt the user for further interaction
 * before killing itself like a good slave.
 */

public class RowListMaker extends JDialog implements ConfigNodePersistent {

	private static final long serialVersionUID = 1L;

	private Preferences configNode = null;
	private final RowListTableModel tableModel;
	private final Notifier notifier = new Notifier();

	private final double PRECISION_LEVEL = 0.001;

	private final TreeSelectionI rowSelection;
	private final LabelInfo labelInfo;
	private LabelInfo colLabelInfo;
	private int nCols = 0;
	private DataMatrix dataMatrix = null;
	private double noData;
	private final String defaultFile;

	/**
	 * @author aloksaldanha
	 *
	 *         Table model to support preview of data. Probably should base
	 *         model.export off of it for simplicity.
	 */
	private class RowListTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		/**
		 *
		 * @return call to indicate table structure changed.
		 */
		public void dataChanged() {

			fireTableStructureChanged();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {

			if (fieldRow.includeHeader())
				return rowSelection.getNSelectedIndexes() + 1;
			else
				return rowSelection.getNSelectedIndexes();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {

			final int[] selectedLabelType = fieldRow.getSelectedLabelType();
			if (fieldRow.includeExpr())
				return nCols + selectedLabelType.length;
			else
				return selectedLabelType.length;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int rowIndex, final int columnIndex) {
			
			final int[] selectedLabelType = fieldRow.getSelectedLabelType();
			if (fieldRow.includeHeader()) {
				if (rowIndex == 0) {
					final String[] pNames = labelInfo.getLabelTypes();
					if (columnIndex < selectedLabelType.length)
						// gene annotation column headers
						return pNames[selectedLabelType[columnIndex]];
					else if (fieldRow.includeExpr()) {
						// array headers
						int gidRow = colLabelInfo.getIndex("GID");
						if (gidRow == -1) {
							gidRow = 0;
						}
						final String[] labels = colLabelInfo
								.getLabels(columnIndex - selectedLabelType.length);
						return labels[gidRow];
					}
				} else if (rowIndex == 1 && eRow != -1) {
					// eweight
					if ((selectedLabelType.length > 0) && (columnIndex == 0))
						return "EWEIGHT";
					else if (columnIndex < selectedLabelType.length)
						return "";
					else {
						final String[] labels = colLabelInfo
								.getLabels(columnIndex - selectedLabelType.length);
						return labels[eRow];
					}
				} else {
					rowIndex--;
				}
			}
			if (columnIndex < selectedLabelType.length) {
				final String[] labels = labelInfo.getLabels(rowIndex + top);
				return labels[selectedLabelType[columnIndex]];
			} else {
				final double val = dataMatrix.getValue(columnIndex
						- selectedLabelType.length, rowIndex + top);
				if (Math.abs(val - DataModel.NAN) < PRECISION_LEVEL)
					return null;
				if (Math.abs(val - DataModel.EMPTY) < PRECISION_LEVEL)
					return null;
				return new Double(val);
			}
			/*
			 * for (int i = top; i <= bot; i++) { if
			 * (geneSelection.isIndexSelected(i) == false) continue; String []
			 * headers = headerInfo.getHeader(i);
			 * output.print(headers[selectedLabelType[0]]); for (int j = 1; j <
			 * selectedLabelType.length; j++) { output.print("\t");
			 * output.print(headers[selectedLabelType[j]]); } if
			 * (fieldRow.includeExpr()) { for (int j = 0; j < nArray; j++) {
			 * output.print("\t"); double val = dataMatrix.getValue(j, i); if
			 * (val != noData) output.print(val); } } output.print("\n");
			 */
		}

	}

	private class Notifier implements ActionListener, ListSelectionListener {
		@Override
		public void actionPerformed(final ActionEvent e) {
			if (tableModel != null) {
				tableModel.dataChanged();
			}
		}

		@Override
		public void valueChanged(final ListSelectionEvent e) {
			if (tableModel != null) {
				tableModel.dataChanged();
			}
		}
	};

	// @Override
	// public void bindConfig(final Preferences configNode) {
	//
	// root = configNode;
	// }

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("GeneListMaker");

		} else {
			LogBuffer.println("Could not find or create GeneListMaker"
					+ "node because parentNode was null.");
		}
	}

	public Preferences createSubNode() {

		return configNode.node("File");
	}

	public String getFile() {

		if (configNode == null)
			return defaultFile;
		else
			return configNode.get("file", defaultFile);
	}

	FileRow fileRow = null;

	public void setFile(final String newdir) {

		configNode.put("file", newdir);

		if (fileRow != null) {
			fileRow.setFile(newdir);
		}
	}

	public RowListMaker(final JFrame f, final TreeSelectionI n,
			final LabelInfo hI, final String dd) {

		super(f, "Row Text Export", true);

		rowSelection = n;
		labelInfo = hI;
		defaultFile = dd;

		top = rowSelection.getMinIndex();
		bot = rowSelection.getMaxIndex();
		if (top > bot) {
			final int swap = top;
			top = bot;
			bot = swap;
		}
		final String[] first = labelInfo.getLabels(top);
		final String[] last = labelInfo.getLabels(bot);
		final int yorf = labelInfo.getIndex("YORF");
		fieldRow = new FieldRow();
		fieldRow.setSelectedIndex(yorf);
		fileRow = new FileRow();
		final JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		center.add(new JLabel("Genes from " + first[yorf] + " to " + last[yorf]
				+ " selected"));
		tableModel = new RowListTableModel();
		final JTable jTable = new JTable(tableModel);
		jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		center.add(new JScrollPane(jTable));
		center.add(fieldRow);
		center.add(fileRow);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(center, BorderLayout.CENTER);

		final JPanel bottom = new JPanel();
		final JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				RowListMaker.this.saveList();
			}
		});
		bottom.add(saveButton);

		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				RowListMaker.this.dispose();
			}
		});
		bottom.add(cancelButton);
		getContentPane().add(bottom, BorderLayout.SOUTH);

	}

	public void setDataMatrix(final DataMatrix data, final LabelInfo ahi,
			final double noData) {
		this.dataMatrix = data;
		this.nCols = dataMatrix.getNumCol();
		this.colLabelInfo = ahi;
		this.eRow = colLabelInfo.getIndex("EWEIGHT");
		this.noData = noData;
	}

	FieldRow fieldRow;
	int top, bot, eRow;

	private void saveList() {
		try {
			final int[] selectedLabelType = fieldRow.getSelectedLabelType();
			if (selectedLabelType.length == 0)
				return;
			setFile(fileRow.getFile());
			final PrintStream output = new PrintStream(
					new BufferedOutputStream(new FileOutputStream(new File(
							fileRow.getFile()))));

			if (fieldRow.includeHeader()) {
				// gid row...
				final String[] pNames = labelInfo.getLabelTypes();
				output.print(pNames[selectedLabelType[0]]);
				for (int j = 1; j < selectedLabelType.length; j++) {
					output.print('\t');
					output.print(pNames[selectedLabelType[j]]);
				}
				if (fieldRow.includeExpr()) {
					int gidRow = colLabelInfo.getIndex("GID");
					if (gidRow == -1) {
						gidRow = 0;
					}
					for (int j = 0; j < nCols; j++) {
						output.print('\t');
						try {
							final String[] headers = colLabelInfo.getLabels(j);
							final String out = headers[gidRow];
							output.print(out);

						} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
							LogBuffer.println("Exception when trying to "
									+ "save gene list: " + e.getMessage());
						}
					}
					output.print('\n');

					// EWEIGHT row
					output.print("EWEIGHT");
					for (int j = 1; j < selectedLabelType.length; j++) {
						output.print('\t');
					}
					final int eRow = colLabelInfo.getIndex("EWEIGHT");
					for (int j = 0; j < nCols; j++) {
						output.print('\t');
						try {
							final String[] headers = colLabelInfo.getLabels(j);
							final String out = headers[eRow];
							output.print(out);

						} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
							LogBuffer.println("Exception when trying to "
									+ "save gene list: " + e.getMessage());
							output.print('1');
						}
					}
				}
				output.print('\n');
			}
			for (int i = top; i <= bot; i++) {
				if (rowSelection.isIndexSelected(i) == false) {
					continue;
				}

				final String[] labels = labelInfo.getLabels(i);
				output.print(labels[selectedLabelType[0]]);
				for (int j = 1; j < selectedLabelType.length; j++) {
					output.print('\t');
					output.print(labels[selectedLabelType[j]]);
				}
				if (fieldRow.includeExpr()) {
					for (int j = 0; j < nCols; j++) {
						output.print('\t');
						final double val = dataMatrix.getValue(j, i);
						if (Math.abs(val - noData) < PRECISION_LEVEL) {
							output.print(val);
						}
					}
				}
				output.print('\n');
			}
			output.close();
			dispose();
		} catch (final Exception e) {
			e.printStackTrace();
			LogBuffer.println("In GeneListMaker.saveList(), "
					+ "got exception " + e.getMessage());
		}

	}

	public void includeAll() {
		fieldRow.includeAll();
		tableModel.dataChanged();
	}

	class FieldRow extends JPanel {

		private static final long serialVersionUID = 1L;

		JList<?> list;
		JCheckBox exprBox, labelBox;

		public void includeAll() {
			list.setSelectionInterval(0, (labelInfo.getLabelTypes()).length - 1);
			exprBox.setSelected(true);
			labelBox.setSelected(true);

		}

		public int[] getSelectedLabelType() {
			return list.getSelectedIndices();
		}

		public void setSelectedIndex(final int i) {
			list.setSelectedIndex(i);
		}

		public boolean includeExpr() {
			return exprBox.isSelected();
		}

		public boolean includeHeader() {
			return labelBox.isSelected();
		}

		public FieldRow() {
			super();
			add(new JLabel("Field(s) to print: "));
			list = new JList<Object>(labelInfo.getLabelTypes());
			list.addListSelectionListener(notifier);
			add(list);
			exprBox = new JCheckBox("Expression Data?");
			exprBox.addActionListener(notifier);
			add(exprBox);
			labelBox = new JCheckBox("Header Line?");
			labelBox.addActionListener(notifier);
			add(labelBox);
		}
	}

	class FileRow extends JPanel {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		JTextField file;

		public void setFile(final String newfile) {
			file.setText(newfile);
		}

		public String getFile() {
			return file.getText();
		}

		public FileRow() {
			super();
			add(new JLabel("Export To: "));
			file = new JTextField(RowListMaker.this.getFile());
			add(file);
			final JButton chooseButton = new JButton("Browse");
			chooseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						final JFileChooser chooser = new JFileChooser();
						chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
						final int returnVal = chooser
								.showOpenDialog(RowListMaker.this);
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							if (chooser.getSelectedFile().isDirectory()) {
								final File currentF = new File(getFile());
								RowListMaker.this.setFile(chooser
										.getSelectedFile().getCanonicalPath()
										+ File.separator + currentF.getName());
							} else {
								RowListMaker.this.setFile(chooser
										.getSelectedFile().getCanonicalPath());
							}
						}
					} catch (final java.io.IOException ex) {
						System.out.println("Got exception " + ex);
					}
				}
			});
			add(chooseButton);
		}
	}

	@Override
	public Preferences getConfigNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void requestStoredState() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void storeState() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void importStateFrom(Preferences oldNode) {
		// TODO Auto-generated method stub
		
	}

}
