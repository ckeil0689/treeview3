package Cluster;

import java.util.List;
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
		attachTreesToModel();
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
	 * Updates the TVModel by reordering data and labels.
	 */
	private void reorderClusteredModel() {

		// Reorder the data matrix
		final TVDataMatrix origMatrix = (TVDataMatrix) model.getDataMatrix();
		int[] reorderedRowIndices = rowCAD.getReorderedIdxs();
		int[] reorderedColIndices = colCAD.getReorderedIdxs();
		origMatrix.reorderMatrixData(reorderedRowIndices, reorderedColIndices);
	  
	  // Update labels associated with DataModel
		// Rows
		if(rowCAD.isAxisClustered()) {
			
			model.getRowLabelInfo().reorderLabels(reorderedRowIndices);
			
			if(!model.gidFound()) {
			  int idx = model.getRowLabelInfo().getNumLabelTypes();
			  model.getRowLabelInfo().addLabelType(ROW_ID_LABELTYPE, idx);
			  
			  final String[] orderedGIDs = constructAxisIDs(rowCAD);
			  model.getRowLabelInfo().addLabels(orderedGIDs);
			  
		    ((TVModel)model).gidFound(true);
			}
		  
//		  final String[] orderedGIDs = constructAxisIDs(rowCAD);
//			model.getRowLabelInfo().reorderLabels(reorderedRowIndices);
			
			// FIXME add when old label IDs do not exist -- otherwise replace!
//			model.getRowLabelInfo().addLabels(orderedGIDs);
		}
		
		// Columns
		if(colCAD.isAxisClustered()) {
			
			model.getColLabelInfo().reorderLabels(reorderedColIndices);
			
			if(!model.aidFound()) {
			  int idx = model.getColLabelInfo().getNumLabelTypes();
			  model.getColLabelInfo().addLabelType(COL_ID_LABELTYPE, idx);
			  
			  final String[] orderedAIDs = constructAxisIDs(colCAD);
			  model.getColLabelInfo().addLabels(orderedAIDs);
			  
		    ((TVModel)model).aidFound(true);
			}
		  
//		  final String[] orderedAIDs = constructAxisIDs(colCAD);
//			model.getColLabelInfo().reorderLabels(reorderedColIndices);
//			model.getColLabelInfo().addLabels(orderedAIDs);
		}
	}
	
	/**
	 * Adds tree data from ClusteredAxisData of both axes to the TVModel. 
	 */
	private void attachTreesToModel() {
		
		ModelTreeAdder mta = new ModelTreeAdder(model);
		
		if(rowCAD.isAxisClustered()) {
			constructTreeNodeData(rowCAD);
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
			constructTreeNodeData(colCAD);
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
	
	/**
	 * Uses the reordered axis indices to construct axis IDs used to attach trees
	 * to rows or columns (such as ROW32X).
	 * @param cad - Contains all information accumulated during clustering which 
	 * is needed to update the current model.
	 * @return An array of axis IDs, one for each row or column element.
	 */
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
	
	/**
	 * Tree node data depends on reordered indices of matrix elements. 
	 * In order to reuse possible existing axis identifiers (e.g. ROW34X or 
	 * COL67X) from previous clustering, the matrix elements which were connected 
	 * by hierarchical clustering are only recorded by their indices in 
	 * HierCluster.java. Here, the list of reordered indices is used to either 
	 * reorder existing axis identifiers or construct new ones if a model was
	 * not clustered before.
	 * @param cad
	 */
	private void constructTreeNodeData(final ClusteredAxisData cad) {
		
		String[] oldAxisIDs = cad.getPreviousIDs();
		List<String[]> treeNodeData = cad.getTreeNodeData();
		
		// Match only non-neggtive integers
		final String intPattern = "^\\d+$";
		
		// Case 1: No old IDs exist. Create new ones from the indices already 
		// recorded in HierCluster..
		if(oldAxisIDs.length == 0) {
			LogBuffer.println("No axis IDs exist. Creating new ones.");
			for(String[] node : treeNodeData) {
				// E.g. 34 --> ROW34X
				if(node[1].matches(intPattern)) {
					node[1] = cad.getAxisBaseID() + node[1] + "X";
				}
				
				if(node[2].matches(intPattern)) {
					node[2] = cad.getAxisBaseID() + node[2] + "X";
				}
			}
		}
		// Case 2: Previous axis IDs exist - reorder them according 
		// to reordered indices.
		else {
			LogBuffer.println("Old axis IDs exist. Reordering.");
			for(String[] node : treeNodeData) {
				// E.g. 34 --> ROW34X
				if(node[1].matches(intPattern)) {
					int idx = Integer.parseInt(node[1]);
					node[1] = oldAxisIDs[idx];
				}
				
				if(node[2].matches(intPattern)) {
					int idx = Integer.parseInt(node[2]);
					node[2] = oldAxisIDs[idx];
				}
			}
		}
	}
}
