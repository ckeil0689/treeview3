package Cluster;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.GUIParams;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * This class is used to generate the data preview table in the Cluster section
 * 
 * @author CKeil
 * 
 */
public class DataViewPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Instance Variables
	 */
	private TVModel model;
	private TVDataMatrix matrix;
	private List<Double> gList;

	private String[] geneNames;
	private String[][] headerArray;

	/**
	 * Limit of rows/ columns for preview to display
	 */
	private final int max = 20;

	/**
	 * Swing Components
	 */
	private JTable table, headerTable;
	private JTableHeader header;
	private JScrollPane tableScroll;

	/**
	 * Constructor
	 * 
	 * @param model
	 * @param mainFrame
	 */
	public DataViewPanel(final DataModel cModel) {

		this.model = (TVModel) cModel;
		this.setLayout(new MigLayout("ins 0"));

		matrix = (TVDataMatrix) model.getDataMatrix();
		ArrayList<double[]> dataArrays = matrix.getExprData();

		headerArray = model.getGeneHeaderInfo().getHeaderArray();

//		gList = fillDList(dataArray);
//		arraysList = splitArrays(gList, model);
		List<double[]> arraysList = new ArrayList<double[]>();

		if (dataArrays.size() > max) {

			arraysList = dataArrays.subList(0, max);
		}

		geneNames = new String[max];

		for (int i = 0; i < max; i++) {

			geneNames[i] = headerArray[i][1];
		}

		table = new JTable(new ClusterTableModel(arraysList, model));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// Table Header Options
		header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.setBackground(GUIParams.TABLEHEADERS);

		// create a scrollPane with the table in it
		tableScroll = new JScrollPane(table,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		table.setFillsViewportHeight(true);
		tableScroll.setBackground(GUIParams.BG_COLOR);

		for (int i = 0; i < max; i++) {

			table.getColumnModel()
					.getColumn(i)
					.setWidth(
							table.getColumnModel().getColumn(i)
									.getPreferredWidth() * 2);

			table.setRowHeight(i, table.getRowHeight(i) * 2);
		}

		final DefaultTableModel model = new DefaultTableModel() {

			private static final long serialVersionUID = 1L;

			@Override
			public int getColumnCount() {

				return 1;
			}

			@Override
			public boolean isCellEditable(final int row, final int col) {

				return false;
			}

			@Override
			public int getRowCount() {

				return max;// table.getRowCount();
			}

			@Override
			public Class<?> getColumnClass(final int colNum) {

				switch (colNum) {
				case 0:
					return String.class;
				default:
					return super.getColumnClass(colNum);
				}
			}
		};
		;
		model.addColumn(geneNames);

		headerTable = new JTable(model);

		// int max = table.getRowCount();

		for (int i = 0; i < max; i++) {

			headerTable.setValueAt(geneNames[i], i, 0);
			headerTable.setRowHeight(i, headerTable.getRowHeight(i) * 2);
		}

		headerTable.setShowGrid(false);
		headerTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		headerTable.setPreferredScrollableViewportSize(new Dimension(100, 0));
		headerTable.getColumnModel().getColumn(0)
				.setCellRenderer(new TableCellRenderer() {

					@Override
					public Component getTableCellRendererComponent(
							final JTable x, final Object value,
							final boolean isSelected, final boolean hasFocus,
							final int row, final int column) {

						final Component component = table
								.getTableHeader()
								.getDefaultRenderer()
								.getTableCellRendererComponent(table, value,
										false, false, -1, -2);
						((JLabel) component)
								.setHorizontalAlignment(SwingConstants.CENTER);

						return component;
					}
				});

		tableScroll.setRowHeaderView(headerTable);

		this.add(tableScroll, "grow, push");
		this.setVisible(true);
	}

	/**
	 * Repaint the swing components of this class, used to change the theme.
	 */
	public void refresh() {

		header.setBackground(GUIParams.TABLEHEADERS);
		tableScroll.setBackground(GUIParams.BG_COLOR);

		this.revalidate();
		this.repaint();
	}

	public List<Double> fillDList(final double[] array) {

		final List<Double> doubleList = new ArrayList<Double>();

		for (final double d : array) {

			doubleList.add(d);
		}

		return doubleList;
	}

//	public List<List<Double>> splitArrays(final List<Double> gList,
//			final TVModel model) {
//
//		// number of rows/ columns
//		final int nArrays = model.nExpr();
//		final int nGenes = max; // model.nGene();
//
//		final List<List<Double>> arraysList = new ArrayList<List<Double>>();
//
//		// iterate through columns ...max
//		for (int j = 0; j < nArrays; j++) {
//
//			final List<Double> sArray = new ArrayList<Double>();
//
//			for (int i = 0; i < nGenes; i++) {
//
//				final int element = (i * max) + j;
//
//				sArray.add(gList.get(element));
//
//			}
//
//			arraysList.add(sArray);
//		}
//
//		return arraysList;
//	}
}
