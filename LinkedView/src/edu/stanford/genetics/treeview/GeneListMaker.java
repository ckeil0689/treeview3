/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: GeneListMaker.java,v $
 * $Revision: 1.14 $
 * $Date: 2006-03-20 06:17:44 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 * This class is designed to save lists of genes to a file.
 *
 * The class will pop up a window and prompt the user for further interaction
 * before killing itself like a good slave.
 */

public class GeneListMaker extends JDialog implements ConfigNodePersistent {

	private static final long serialVersionUID = 1L;

	private Preferences configNode = null;
	private final GeneListTableModel tableModel;
	private final Notifier notifier = new Notifier();

	private final double PRECISION_LEVEL = 0.001;

	private final TreeSelectionI geneSelection;
	private final HeaderInfo headerInfo;
	private HeaderInfo aHeaderInfo;
	private int nArray = 0;
	private DataMatrix dataMatrix = null;
	private double noData;
	private final String defaultFile;

	/**
	 * @author aloksaldanha
	 *
	 *         Table model to support preview of data. Probably should base
	 *         export off of it for simplicity.
	 */
	private class GeneListTableModel extends AbstractTableModel {

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
				return geneSelection.getNSelectedIndexes() + 1;
			else
				return geneSelection.getNSelectedIndexes();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {

			final int[] selectedPrefix = fieldRow.getSelectedPrefix();
			if (fieldRow.includeExpr())
				return nArray + selectedPrefix.length;
			else
				return selectedPrefix.length;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int rowIndex, final int columnIndex) {
			final int[] selectedPrefix = fieldRow.getSelectedPrefix();
			if (fieldRow.includeHeader()) {
				if (rowIndex == 0) {
					final String[] pNames = headerInfo.getNames();
					if (columnIndex < selectedPrefix.length)
						// gene annotation column headers
						return pNames[selectedPrefix[columnIndex]];
					else if (fieldRow.includeExpr()) {
						// array headers
						int gidRow = aHeaderInfo.getIndex("GID");
						if (gidRow == -1) {
							gidRow = 0;
						}
						final String[] headers = aHeaderInfo
								.getHeader(columnIndex - selectedPrefix.length);
						return headers[gidRow];
					}
				} else if (rowIndex == 1 && eRow != -1) {
					// eweight
					if ((selectedPrefix.length > 0) && (columnIndex == 0))
						return "EWEIGHT";
					else if (columnIndex < selectedPrefix.length)
						return "";
					else {
						final String[] headers = aHeaderInfo
								.getHeader(columnIndex - selectedPrefix.length);
						return headers[eRow];
					}
				} else {
					rowIndex--;
				}
			}
			if (columnIndex < selectedPrefix.length) {
				final String[] headers = headerInfo.getHeader(rowIndex + top);
				return headers[selectedPrefix[columnIndex]];
			} else {
				final double val = dataMatrix.getValue(columnIndex
						- selectedPrefix.length, rowIndex + top);
				if (Math.abs(val - DataModel.NODATA) < PRECISION_LEVEL)
					return null;
				if (Math.abs(val - DataModel.EMPTY) < PRECISION_LEVEL)
					return null;
				return new Double(val);
			}
			/*
			 * for (int i = top; i <= bot; i++) { if
			 * (geneSelection.isIndexSelected(i) == false) continue; String []
			 * headers = headerInfo.getHeader(i);
			 * output.print(headers[selectedPrefix[0]]); for (int j = 1; j <
			 * selectedPrefix.length; j++) { output.print("\t");
			 * output.print(headers[selectedPrefix[j]]); } if
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

	public GeneListMaker(final JFrame f, final TreeSelectionI n,
			final HeaderInfo hI, final String dd) {

		super(f, "Gene Text Export", true);

		geneSelection = n;
		headerInfo = hI;
		defaultFile = dd;

		top = geneSelection.getMinIndex();
		bot = geneSelection.getMaxIndex();
		if (top > bot) {
			final int swap = top;
			top = bot;
			bot = swap;
		}
		final String[] first = headerInfo.getHeader(top);
		final String[] last = headerInfo.getHeader(bot);
		final int yorf = headerInfo.getIndex("YORF");
		fieldRow = new FieldRow();
		fieldRow.setSelectedIndex(yorf);
		fileRow = new FileRow();
		final JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		center.add(new JLabel("Genes from " + first[yorf] + " to " + last[yorf]
				+ " selected"));
		tableModel = new GeneListTableModel();
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
				GeneListMaker.this.saveList();
			}
		});
		bottom.add(saveButton);

		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				GeneListMaker.this.dispose();
			}
		});
		bottom.add(cancelButton);
		getContentPane().add(bottom, BorderLayout.SOUTH);

	}

	public void setDataMatrix(final DataMatrix data, final HeaderInfo ahi,
			final double noData) {
		this.dataMatrix = data;
		this.nArray = dataMatrix.getNumCol();
		this.aHeaderInfo = ahi;
		this.eRow = aHeaderInfo.getIndex("EWEIGHT");
		this.noData = noData;
	}

	FieldRow fieldRow;
	int top, bot, eRow;

	private void saveList() {
		try {
			final int[] selectedPrefix = fieldRow.getSelectedPrefix();
			if (selectedPrefix.length == 0)
				return;
			setFile(fileRow.getFile());
			final PrintStream output = new PrintStream(
					new BufferedOutputStream(new FileOutputStream(new File(
							fileRow.getFile()))));

			if (fieldRow.includeHeader()) {
				// gid row...
				final String[] pNames = headerInfo.getNames();
				output.print(pNames[selectedPrefix[0]]);
				for (int j = 1; j < selectedPrefix.length; j++) {
					output.print('\t');
					output.print(pNames[selectedPrefix[j]]);
				}
				if (fieldRow.includeExpr()) {
					int gidRow = aHeaderInfo.getIndex("GID");
					if (gidRow == -1) {
						gidRow = 0;
					}
					for (int j = 0; j < nArray; j++) {
						output.print('\t');
						try {
							final String[] headers = aHeaderInfo.getHeader(j);
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
					for (int j = 1; j < selectedPrefix.length; j++) {
						output.print('\t');
					}
					final int eRow = aHeaderInfo.getIndex("EWEIGHT");
					for (int j = 0; j < nArray; j++) {
						output.print('\t');
						try {
							final String[] headers = aHeaderInfo.getHeader(j);
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
				if (geneSelection.isIndexSelected(i) == false) {
					continue;
				}

				final String[] headers = headerInfo.getHeader(i);
				output.print(headers[selectedPrefix[0]]);
				for (int j = 1; j < selectedPrefix.length; j++) {
					output.print('\t');
					output.print(headers[selectedPrefix[j]]);
				}
				if (fieldRow.includeExpr()) {
					for (int j = 0; j < nArray; j++) {
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

		JList list;
		JCheckBox exprBox, headerBox;

		public void includeAll() {
			list.setSelectionInterval(0, (headerInfo.getNames()).length - 1);
			exprBox.setSelected(true);
			headerBox.setSelected(true);

		}

		public int[] getSelectedPrefix() {
			return list.getSelectedIndices();
		}

		public void setSelectedIndex(final int i) {
			list.setSelectedIndex(i);
		}

		public boolean includeExpr() {
			return exprBox.isSelected();
		}

		public boolean includeHeader() {
			return headerBox.isSelected();
		}

		public FieldRow() {
			super();
			add(new JLabel("Field(s) to print: "));
			list = new JList(headerInfo.getNames());
			list.addListSelectionListener(notifier);
			add(list);
			exprBox = new JCheckBox("Expression Data?");
			exprBox.addActionListener(notifier);
			add(exprBox);
			headerBox = new JCheckBox("Header Line?");
			headerBox.addActionListener(notifier);
			add(headerBox);
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
			file = new JTextField(GeneListMaker.this.getFile());
			add(file);
			final JButton chooseButton = new JButton("Browse");
			chooseButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						final JFileChooser chooser = new JFileChooser();
						chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
						final int returnVal = chooser
								.showOpenDialog(GeneListMaker.this);
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							if (chooser.getSelectedFile().isDirectory()) {
								final File currentF = new File(getFile());
								GeneListMaker.this.setFile(chooser
										.getSelectedFile().getCanonicalPath()
										+ File.separator + currentF.getName());
							} else {
								GeneListMaker.this.setFile(chooser
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

}
