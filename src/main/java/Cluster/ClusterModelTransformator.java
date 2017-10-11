package Cluster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Controllers.ClusterDialogController;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.IntLabelInfo;
import edu.stanford.genetics.treeview.model.ModelTreeAdder;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

/**
 * The purpose of this class is to take the currently loaded (active) TVModel 
 * and apply changes from clustering to it, without the step of saving files.
 * For this, the class uses information stored in ClusteredAxisData objects 
 * for each axis.
 */
public class ClusterModelTransformator {

	public final static String ROW_ID_LABELTYPE = "GID";
	public final static String COL_ID_LABELTYPE = "AID";
	
	private final ClusteredAxisData rowCAD;
	private final ClusteredAxisData colCAD;
	
	private TVModel model;
	
	/**
	 * Constructor for the ClusterModelTransformator which uses ClusteredAxisData
	 * objects to manipulate an existing TVModel object.
	 * @param rowCAD - ClusteredAxisData for rows which contains all important
	 * information about the clustered rows.
	 * @param colCAD - ClusteredAxisData for columns which contains all important
	 * information about the clustered columns.
	 * @param model - The currently active TVModel which is reordered 
	 * by clustering.
	 */
	public ClusterModelTransformator(final ClusteredAxisData rowCAD, 
	                                 final ClusteredAxisData colCAD, 
	                                 final TVModel model) {
		
		this.rowCAD = rowCAD;
		this.colCAD = colCAD;
		this.model = model;
	}
	
	/**
	 * Uses ClusteredAxisData for both axes to apply clustering changes directly
	 * to the underlying TVModel without the need to store data in files beforehand.
	 * @return The active TVModel with reordered data, labels and added 
	 * Dendrograms, if applicable.
	 */
	public TVModel applyClusterChanges(boolean isHierarchical) {
		
		final IntLabelInfo rowLabelI = model.getRowLabelInfo();
		final IntLabelInfo colLabelI = model.getColLabelInfo();

		prepareModel(rowLabelI, colLabelI);
		model.setHierarchical(isHierarchical);
		reorderClusteredModel();
		model.setModified(true);
		model.setRowClustered(rowCAD.isAxisClustered());
		model.setColClustered(colCAD.isAxisClustered());
		
		return model;
	}
	
	/** Sets up instance variables needed for writing.
	 *
	 * @param rowLabelI - <code>IntLabelInfo</code> object for the row labels.
	 * @param colLabelI - <code>IntLabelInfo</code> object for the column
	 *          labels. */
	private void prepareModel(final IntLabelInfo rowLabelI,
	                          final IntLabelInfo colLabelI) {
		
		this.rowCAD.setLabelTypes(rowLabelI.getLabelTypes());
		this.colCAD.setLabelTypes(colLabelI.getLabelTypes());

		/* 
		 * retrieving names and weights of row elements
		 * format: [[YAL063C, 1.0], ..., [...]]
		 */
		this.rowCAD.setLabels(rowLabelI.getLabelArray());
		this.colCAD.setLabels(colLabelI.getLabelArray());
		
		rowCAD.setPreviousIDs(getOldIDs(ClusterDialogController.ROW_IDX));
		colCAD.setPreviousIDs(getOldIDs(ClusterDialogController.COL_IDX));
	}
	
	/**
	 * Extracts the old IDs
	 * @param axisID
	 * @return
	 */
	private String[] getOldIDs(final int axisID) {
		
		String[][] labelArray;
		String[] oldIDs; 
		Pattern p;
		int pos = 0;
		
    if(axisID == ClusterDialogController.ROW_IDX) {
    	labelArray = model.getRowLabelInfo().getLabelArray();
    	
    	if(!model.gidFound()) {
    		return new String[]{};
    	}
    	/* Find ID index */
    	p = Pattern.compile("ROW\\d+X");
    } 
    else {
    	labelArray = model.getColLabelInfo().getLabelArray();
    	
    	if(!model.aidFound()) {
    		return new String[]{};
    	}
    	p = Pattern.compile("COL\\d+X");
    }

    /* Find ID index */
	  for(int i = 0; i < labelArray[0].length; i++) {
		  Matcher m = p.matcher(labelArray[0][i]);
		  if(m.find()) {
			  pos = i;
			  break;
		  }
	  }
      	
		oldIDs = new String[labelArray.length];
		
		for(int i = 0; i < labelArray.length; i++) {
			oldIDs[i] = labelArray[i][pos];
		}
		
		return oldIDs;
	}
	
	/**
	 * Updates the TVModel ... and more? TODO
	 */
	private void reorderClusteredModel() {

		// Reorder the data matrix
		final TVDataMatrix origMatrix = (TVDataMatrix) model.getDataMatrix();
		int[] reorderedRowIndices = rowCAD.getReorderedIdxs();
		int[] reorderedColIndices = colCAD.getReorderedIdxs();
		origMatrix.reorderMatrixData(reorderedRowIndices, reorderedColIndices);
	  
	  // Update labels associated with DataModel
		// Rows
		if(!model.gidFound() && rowCAD.isAxisClustered()) {
			int idx = model.getRowLabelInfo().getNumLabelTypes();
			model.getRowLabelInfo().addLabelType(ROW_ID_LABELTYPE, idx);
		  ((TVModel)model).gidFound(true);
		  
		  final String[] orderedGIDs = constructAxisIDs(rowCAD);
			model.getRowLabelInfo().reorderLabels(reorderedRowIndices);
			model.getRowLabelInfo().addLabels(orderedGIDs);
		}
		
		// Columns
		if(!model.aidFound() && colCAD.isAxisClustered()) {
			int idx = model.getColLabelInfo().getNumLabelTypes();
			model.getColLabelInfo().addLabelType(COL_ID_LABELTYPE, idx);
		  ((TVModel)model).aidFound(true);
		  
		  final String[] orderedAIDs = constructAxisIDs(colCAD);
			model.getColLabelInfo().reorderLabels(reorderedColIndices);
			model.getColLabelInfo().addLabels(orderedAIDs);
		}
		
		attachTreesToModel();
	}
	
	/**
	 * Adds tree data from ClusteredAxisData of both axes to the TVModel. 
	 */
	private void attachTreesToModel() {
		
		ModelTreeAdder mta = new ModelTreeAdder(model);
		
		if(rowCAD.isAxisClustered()) {
			boolean wasParsed = mta.parseGTR(rowCAD.getTreeNodeData());
			if(wasParsed) {
				model.setGTRData(rowCAD.getTreeNodeData());
				model.setGtrLabelTypes(mta.getGtrLabelTypes());
				model.setGtrLabels(mta.getGtrLabels());
				model.hashGIDs();
				model.hashGTRs();
			}
		}
		
		if(colCAD.isAxisClustered()) {
			boolean wasParsed = mta.parseATR(colCAD.getTreeNodeData());
			if(wasParsed) {
				model.setATRData(colCAD.getTreeNodeData());
				model.setAtrLabelTypes(mta.getAtrLabelTypes());
				model.setAtrLabels(mta.getAtrLabels());
				model.hashAIDs();
				model.hashATRs();
			}
		}
	}
	
	private String[] constructAxisIDs(final ClusteredAxisData cad) {
		
		int[] reorderedIdxs = cad.getReorderedIdxs();
		String[] constructedAxisIDs = new String[reorderedIdxs.length];
		
		// Case: Axis was not clustered before - build new IDs
		if(cad.getPreviousIDs().length == 0) {
			for(int i = 0; i < reorderedIdxs.length; i++) {
				constructedAxisIDs[i] = cad.getAxisBaseID() + reorderedIdxs[i] + "X";
			}
		}
		// Case: Axis was clustered before - rearrange existing IDs
		else {
			String[] previousIDs = cad.getPreviousIDs();
			for(int i = 0; i < reorderedIdxs.length; i++) {
				int pos = reorderedIdxs[i];
				constructedAxisIDs[i] = previousIDs[pos];
			}
		}
		
		cad.setNewIDs(constructedAxisIDs);
		return constructedAxisIDs;
	}
	
	/** Creates a list of the post-clustering axis index order.
	 * 
	 * @param cd The <code>ClusteredAxisData</code> object for the axis for which
	 *          indices are to be retrieved.
	 * @return An integer array of new axis indices, useful for reordering. 
	 * @deprecated*/
	private int[] getReorderedIndices(ClusteredAxisData cd) {

		int[] reorderedIndices = new int[cd.getNumLabels()];
		int orderedIDNum = cd.getReorderedIdxs().length;

		try {
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
		} catch(ArrayIndexOutOfBoundsException e) {
			LogBuffer.logException(e);
			LogBuffer.println("Problem when reordering model data. " +
				"Data will remain in original order.");
			for(int i = 0; i < reorderedIndices.length; i++) {
				reorderedIndices[i] = i;
			}
			return reorderedIndices;
		}
	}
	
	/** Orders the labels for the CDT data based on the ordered ID String arrays.
	 * 
	 * @param cd The ClusteredAxisData objects containing all relevant info
	 *          for label reordering.
	 * @return List of new element order indices that can be used to rearrange
	 *         the matrix data consistent with the new element ordering. 
	 *         @deprecated*/
	private int[] orderElements(ClusteredAxisData cd) {

		int[] reorderedIndices = new int[cd.getNumLabels()];

		// Make list of gene names to quickly access indexes
		final String[] geneNames = new String[cd.getNumLabels()];

		if(!model.isHierarchical()) {
			for(int i = 0; i < geneNames.length; i++) {
				geneNames[i] = cd.getAxisLabels()[i][0];
			}
		}

		int index = -1;
		// Make an array of indexes from the ordered column list.
		for(int i = 0; i < reorderedIndices.length; i++) {
			final String id = "";//cd.getReorderedIdxs()[i];

			if(model.isHierarchical()) {
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
	 * @param array - A String array to be searched.
	 * @param element - A String element to be found in the array.
	 * @return The last instance of the String element in the array. 
	 * @deprecated*/
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
