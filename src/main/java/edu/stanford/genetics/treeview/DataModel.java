/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.util.prefs.Preferences;

import edu.stanford.genetics.treeview.model.IntLabelInfo;

/**
 * This file defines the bare bones of what needs to be implemented by a data
 * model which wants to be used with a ViewFrame and some ModelViews.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.18 $ $Date: 2008-03-09 21:06:34 $
 */

public interface DataModel {

	public final static double NAN = Double.NaN;
	public final static double EMPTY = Double.POSITIVE_INFINITY;

	/**
	 * Gets the documentConfig attribute of the DataModel object
	 *
	 * values stored in the <code>ConfigNode</code>s of this
	 * <code>XmlConfig</code> should be persistent across multiple openings of
	 * this DataModel.
	 *
	 * <p>
	 * Of course, if you don't care about persistence you could subclass
	 * XmlConfig to create one which doesn't store things to file.
	 *
	 * @return The documentConfig value
	 */
	public Preferences getDocumentConfigRoot();

	/**
	 * Gets the file path or url which this <code>DataModel</code> was built
	 * from.
	 *
	 * @return String representation of file path or url
	 */
	public String getSource();

	/**
	 * Gets the file name with extension <code>DataModel</code> was built from.
	 *
	 * @return String representation of file name with file type
	 */
	public String getFileName();

	/**
	 * Gets a short name, unique for this <code>DataModel</code>, suitable for
	 * putting in a window menu.
	 *
	 * @return Short name of data model.
	 */
	public String getName();

	/**
	 * Sets a data model to be compare to this model.
	 *
	 * @param dm
	 *            The data model.
	 */
	public void setModelForCompare(DataModel dm);

	/**
	 * Gets the fileSet which this <code>DataModel</code> was built from.
	 *
	 * @return The actual <code>Fileset</code> which generated this
	 *         <code>DataModel</code>
	 */
	public FileSet getFileSet();

	public void clearFileSetListeners();

	public void addFileSetListener(FileSetListener listener);

	/**
	 * Gets the LabelInfo associated with genes for this DataModel.
	 *
	 * There are two special indexes, YORF and NAME, which mean the unique id
	 * column and the description column, respectively. See
	 * TVModel.TVModelLabelInfo for details.
	 */
	public IntLabelInfo getRowLabelInfo();

	/**
	 * Gets the LabelInfo associated with arrays for this DataModel.
	 */
	public IntLabelInfo getColLabelInfo();

	/**
	 * Gets the LabelInfo associated with gene tree for this DataModel.
	 *
	 * There are two special indexes, YORF and NAME, which mean the unique id
	 * column and the description column, respectively. See
	 * TVModel.TVModelHeaderInfo for details.
	 */
	public LabelInfo getGtrLabelInfo();

	/**
	 * Gets the HeaderInfo associated with array tree for this DataModel.
	 */
	public LabelInfo getAtrLabelInfo();

	/**
	 * This not-so-object-oriented hack is in those rare instances where it is
	 * not enough to know that we've got a DataModel.
	 *
	 * @return a string representation of the type of this
	 *         <code>DataModel</code>
	 */
	public String getType();

	/**
	 * returns the datamatrix which underlies this data model, typically the
	 * matrix of measured intensity ratios.
	 */
	public DataMatrix getDataMatrix();

	void append(DataModel m);

	/**
	 * Removes the previously appended DataMatrix.
	 *
	 */
	void removeAppended();

	/**
	 * @return
	 */
	public boolean aidFound();

	/**
	 * @return
	 */
	public boolean gidFound();

	/**
	 * Set the modified member which can be used to check if changes occurred
	 * and need to be saved.
	 * @param wasModified - indicate whether the model was modified
	 */
	public void setModified(boolean wasModified);
	
	/**
	 * @return true if data model has been modified since last save to source.
	 *         always returning false is generally a safe thing, if you have an
	 *         immutable data model.
	 */
	public boolean getModified();

	/**
	 *
	 * @return true if data model has been sucessfully loaded.
	 */
	public boolean isLoaded();
	
	public void setHierarchical(final boolean isHierarchical);
	public boolean isHierarchical();
	
	public void setClustered(final boolean isClustered);
	public boolean isClustered();
}
