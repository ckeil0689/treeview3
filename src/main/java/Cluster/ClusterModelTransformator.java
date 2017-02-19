package Cluster;

import edu.stanford.genetics.treeview.model.IntLabelInfo;
import edu.stanford.genetics.treeview.model.ModelTreeAdder;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * The purpose of this class is to take the currently loaded TVModel and 
 * apply changes from clustering to it, without the step of saving files.
 * For this it takes information stored in ClusteredAxisData objects 
 * for each axis.
 */
public class ClusterModelTransformator {

	public final static String ROW_ID_LABELTYPE = "GID";
	public final static String COL_ID_LABELTYPE = "AID";
	
	private final ClusteredAxisData rowCAD;
	private final ClusteredAxisData colCAD;
	private final TVModel model;
	
	private boolean isHierarchical = true; 
	
	public ClusterModelTransformator(ClusteredAxisData rowCAD, 
	                                 ClusteredAxisData colCAD, TVModel model) {
		
		this.rowCAD = rowCAD;
		this.colCAD = colCAD;
		this.model = model;
	}
	
	/**
	 * Uses ClusteredAxisData for both axes to apply clustering changes directly
	 * to the underlying TVModel without the need to store data in files beforehand.
	 * @return A tranformed TVModel with reordered data, labels and added 
	 * Dendrograms, if applicable.
	 */
	public TVModel applyClusterChanges(boolean isHierarchical) {
		
		this.isHierarchical = isHierarchical;
		
		final IntLabelInfo rowLabelI = model.getRowLabelInfo();
		final IntLabelInfo colLabelI = model.getColLabelInfo();

		prepareModel(rowLabelI, colLabelI);
		reorderClusteredModel();
		
		return model;
	}
	
	/** Sets up instance variables needed for writing.
	 *
	 * @param rowLabelI - <code>IntLabelInfo</code> object for the row labels.
	 * @param colLabelI - <code>IntLabelInfo</code> object for the column
	 *          labels. */
	public void prepareModel(final IntLabelInfo rowLabelI,
											final IntLabelInfo colLabelI) {
		
		this.rowCAD.setLabelTypes(rowLabelI.getLabelTypes());
		this.colCAD.setLabelTypes(colLabelI.getLabelTypes());

		/* 
		 * retrieving names and weights of row elements
		 * format: [[YAL063C, 1.0], ..., [...]]
		 */
		this.rowCAD.setLabels(rowLabelI.getLabelArray());
		this.colCAD.setLabels(colLabelI.getLabelArray());
	}
	
	/**
	 * Updates the TVModel ... and more? TODO
	 */
	private void reorderClusteredModel() {

		// data matrix
		final TVDataMatrix origMatrix = (TVDataMatrix) model.getDataMatrix();
		int[] reorderedRowIndices = getReorderedIndices(rowCAD);
		int[] reorderedColIndices = getReorderedIndices(colCAD);
		origMatrix.reorderMatrixData(reorderedRowIndices, reorderedColIndices);
	  
	  // update label types
		String[] rLabelTypes = rowCAD.getAxisLabelTypes();
		if(!model.gidFound() && rowCAD.shouldReorderAxis()) {
			int idx = model.getRowLabelInfo().getNumLabelTypes();
			model.getRowLabelInfo().addLabelType(ROW_ID_LABELTYPE, idx);
		  ((TVModel)model).gidFound(true);
		}
		
		String[] cLabelTypes = colCAD.getAxisLabelTypes();
		if(!model.aidFound() && colCAD.shouldReorderAxis()) {
			int idx = model.getColLabelInfo().getNumLabelTypes();
			model.getColLabelInfo().addLabelType(COL_ID_LABELTYPE, idx);
		  ((TVModel)model).aidFound(true);
		}
		
		// labels
	  final String[] orderedGIDs = rowCAD.getReorderedIDs();
		final String[] orderedAIDs = colCAD.getReorderedIDs();
		
		model.getRowLabelInfo().reorderLabels(reorderedRowIndices);
		model.getColLabelInfo().reorderLabels(reorderedColIndices);
		
		model.getRowLabelInfo().setLabelTypeArray(rLabelTypes);
		model.getColLabelInfo().setLabelTypeArray(cLabelTypes);
		model.getRowLabelInfo().addLabels(orderedGIDs);
		model.getColLabelInfo().addLabels(orderedAIDs);
		
		addTrees();
	}
	
	/**
	 * Adds tree data from ClusteredAxisData of both axes to the TVModel. 
	 */
	private void addTrees() {
		
		ModelTreeAdder mta = new ModelTreeAdder(model);
		
		if(rowCAD.shouldReorderAxis()) {
			mta.parseGTR(rowCAD.getTreeNodeData());
		}
		
		if(colCAD.shouldReorderAxis()) {
			mta.parseATR(colCAD.getTreeNodeData());
		}
	}
	
	/** Creates a list of the post-clustering axis index order.
	 * 
	 * @param cd The <code>ClusteredAxisData</code> object for the axis for which
	 *          indices are to be retrieved.
	 * @return An integer array of new axis indices, useful for reordering. */
	private int[] getReorderedIndices(ClusteredAxisData cd) {

		int[] reorderedIndices = new int[cd.getNumLabels()];
		int orderedIDNum = cd.getReorderedIDs().length;

		if(cd.shouldReorderAxis() && cd.isAxisClustered() && orderedIDNum != 0) {
			reorderedIndices = orderElements(cd);

			/* old order simply remains */
		}
		else {
			for(int i = 0; i < reorderedIndices.length; i++) {
				reorderedIndices[i] = i;
			}
		}

		return reorderedIndices;
	}
	
	/** Orders the labels for the CDT data based on the ordered ID String arrays.
	 * 
	 * @param cd The ClusteredAxisData objects containing all relevant info
	 *          for label reordering.
	 * @return List of new element order indices that can be used to rearrange
	 *         the matrix data consistent with the new element ordering. */
	private int[] orderElements(ClusteredAxisData cd) {

		int[] reorderedIndices = new int[cd.getNumLabels()];

		// Make list of gene names to quickly access indexes
		final String[] geneNames = new String[cd.getNumLabels()];

		if(!isHierarchical) {
			for(int i = 0; i < geneNames.length; i++) {
				geneNames[i] = cd.getAxisLabels()[i][0];
			}
		}

		int index = -1;
		// Make an array of indexes from the ordered column list.
		for(int i = 0; i < reorderedIndices.length; i++) {
			final String id = cd.getReorderedIDs()[i];

			if(isHierarchical) {
				// extract numerical part of element ID
				final String adjusted = id.replaceAll("[\\D]", "");
				// gets index from ordered list, e.g. COL45X --> 45;
				index = Integer.parseInt(adjusted);

			}
			else {
				index = findIndex(geneNames, id);
			}

			reorderedIndices[i] = index;
		}

		return reorderedIndices;
	}
	
	/** Finds the last index of an element match in a String array.
	 *
	 * @param array
	 * @param element
	 * @return */
	private int findIndex(final String[] array, final String element) {

		int index = -1;
		for(int i = 0; i < array.length; i++) {

			if(array[i].equalsIgnoreCase(element)) {
				index = i;
			}
		}

		return index;
	}
}
