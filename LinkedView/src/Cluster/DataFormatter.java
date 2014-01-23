package Cluster;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.model.TVModel;

/**
 * This class is used to make an object which can take in the loaded data in its
 * format as originally coded in Java TreeView's first version and format it for
 * use in the clustering module.
 * 
 * @author CKeil
 */
public class DataFormatter {

	// Instance variables
	private final TVModel model;
	private final ClusterView clusterView;
	private final List<Double> list;
	private final int rowPBar = 1;
	private final int colPBar = 3;

	private final List<List<Double>> rowList = new ArrayList<List<Double>>();
	private final List<List<Double>> colList = new ArrayList<List<Double>>();

	// Constructor (building the object)
	public DataFormatter(final DataModel model, final ClusterView clusterView, 
			final List<Double> list) {

		this.model = (TVModel) model;
		this.clusterView = clusterView;
		this.list = list;
	}

	// extracting rows from raw data array
	public void splitRows() {

		int lower = 0;
		int upper = 0;

		// number of arrays
		final int max = list.size() / model.nExpr();

		clusterView.setPBarMax(max, rowPBar);

		for (int i = 0; i < list.size() / max; i++) {

			clusterView.updatePBar(i, rowPBar);

			upper += max;

			rowList.add(list.subList(lower, upper));

			lower = upper;
		}

		if (upper < list.size() - 1) {
			lower = upper;
			upper = list.size();

			rowList.add(list.subList(lower, upper));
		}
	}

	// getting the columns from raw data array
	public void splitColumns() {

		// Number of arrays/ columns
		final int max = list.size() / model.nExpr();
		final int nGenes = model.nGene();

		// Setting up ProgressBar
		clusterView.setPBarMax(max, colPBar);

		// Iterate through all columns
		for (int j = 0; j < max; j++) {

			clusterView.updatePBar(j, colPBar);

			final List<Double> sArray = new ArrayList<Double>();

			for (int i = 0; i < nGenes; i++) {

				final int element = (i * max) + j;

				sArray.add(list.get(element));
			}

			colList.add(sArray);
		}
	}

	// Accessor methods to return each data list
	public List<List<Double>> getRowList() {

		return rowList;
	}

	public List<List<Double>> getColList() {

		return colList;
	}
}
