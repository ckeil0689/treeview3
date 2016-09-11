/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.Observer;

/**
 * Interface to access header info about genes or arrays or treenodes This
 * interface is used many ways. The basic idea is that the "Header" refers to
 * which gene, array, or node you want information about, whereas the "Name" is
 * which header you want. Thus, getNumHeaders() is the number of genes, whereas
 * getNumNames() is the number of headers for each gene.
 *
 * Conceptually, the objects that are annotated (genes, arrays, nodes) can be
 * thought of as rows, and the various names as the headers of columns of
 * information about them. For historical reasons, the actual annotations are
 * called the headers, and the column headers are called names (i.e. names of
 * the annotation). This is because the first HeaderInfo objects represented
 * subtables of the CDT file.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.12 $ $Date: 2005-11-25 07:24:08 $
 */
public interface LabelInfo {
	/**
	 * Gets the header info for gene/array/node i
	 *
	 * @param i
	 *            index of the gene/array/node for which to get headers
	 * @return The array of header values
	 */
	public String[] getLabels(int i);

	/**
	 * Gets the header info for gene/array/node i, col name
	 *
	 * @param i
	 *            index of the gene/array/node for which to get headers
	 * @param name
	 *            name of the header to get
	 * @return header value
	 */
	public String getLabel(int i, String name);

	/**
	 * Gets the names of the headers
	 *
	 * @return The array of header names
	 */
	public String[] getPrefixes();

	/**
	 * The number of headers.
	 */
	public int getNumPrefixes();

	/**
	 * Gets the number of sets of headers. This will generally be the number
	 * things which have headers, i.e. number of genes/arrays/nodes.
	 */
	public int getNumLabels();

	/**
	 * Gets the index associated with a particular header name.
	 *
	 * usually, getIndex(getNames() [i]) == i.
	 *
	 * Note that some header info classes may have special ways of mapping names
	 * to indexes, so that the getNames() array at the returned index may not
	 * actually match the name argument. This is particularly true for fields
	 * like YORF, which may also be UID, etc...
	 *
	 * Should have been called "getNameIndex".
	 *
	 * @param prefix
	 *            A name to find the index of
	 * @return The index value
	 */
	public int getIndex(String prefix);

	/**
	 * gets the index of a gene/array/node given a value from the first column
	 * (the id column). Should have been called "getIndexById" or something.
	 *
	 * @param label
	 *            a particular id for a gene or array or node
	 * @return The index value, for use with getHeader() or similar thing.
	 *         Returns -1 if no header matching "id" can be found.
	 */
	public int getLabelIndex(String label);

	/**
	 * This is used by HeaderInfo objects that may change over time. If your
	 * HeaderInfo is static, you can just make this a noop.
	 *
	 * @param o
	 */
	public void addObserver(Observer o);

	/**
	 * This is used by HeaderInfo objects that may change over time. If your
	 * HeaderInfo is static, you can just make this a noop.
	 *
	 * @param o
	 */
	public void deleteObserver(Observer o);

	/**
	 * Adds a new named "column" of headers to this object Just return false if
	 * your header info is read only.
	 *
	 * @param prefix
	 *            name of column to add
	 * @param location
	 *            0 means make it first, getNumNames() means make it last
	 * @return true if successfully added, false if not.
	 */
	public boolean addPrefix(String prefix, int location);

	/**
	 * Sets indicated header to specified value Just return false if your header
	 * info is read only.
	 *
	 * @param prefix
	 *            name of column to change
	 * @param newLabel
	 *            new value for label.
	 *
	 * @return true if successfully modified, false if not.
	 */
	public boolean setLabel(int i, String prefix, String newLabel);

	public void setPrefixArray(String[] newPrefixArray);

	/**
	 * @return true if the HeaderInfo has been modified since last save
	 */
	public boolean getModified();

	/**
	 * should only be called externally after HeaderInfo has been saved to disk.
	 *
	 * @param mod
	 *            false if no longer out of synch with disk.
	 */
	public void setModified(boolean mod);

	/**
	 * lookup by row and column, which should correspond to position in the
	 * names array.
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getLabel(int rowIndex, int columnIndex);

	public String[][] getLabelArray();
}
