package Cluster;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;

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

	private String[] geneNames;
	private String[][] headerArray;

	/**
	 * Limit of rows/ columns for preview to display
	 */
	private final int MAX = 20;

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

		double[][] dataArrays = Arrays.copyOfRange(((TVDataMatrix) 
				model.getDataMatrix()).getExprData(), 0, MAX);

		headerArray = model.getGeneHeaderInfo().getHeaderArray();

		double[][] arraysList = new double[MAX][MAX];

		// Fill 2-dimensional 20x20 (max size) array to be sued for data
		// in the JTable
		if (dataArrays.length > MAX) {
			for(int i = 0; i < MAX; i++) {
				
				for(int j = 0; j < MAX; j++) {
					
					arraysList[i][j] = dataArrays[i][j];
				}
			}
		}
		
		dataArrays = null;

		geneNames = new String[MAX];

		for (int i = 0; i < MAX; i++) {

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

		for (int i = 0; i < MAX; i++) {

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

				return MAX;
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

		for (int i = 0; i < MAX; i++) {

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

		add(tableScroll, "grow, push");
		setVisible(true);
	}

	/**
	 * Repaint the swing components of this class, used to change the theme.
	 */
	public void refresh() {

		header.setBackground(GUIParams.TABLEHEADERS);
		tableScroll.setBackground(GUIParams.BG_COLOR);

		revalidate();
		repaint();
	}
}
