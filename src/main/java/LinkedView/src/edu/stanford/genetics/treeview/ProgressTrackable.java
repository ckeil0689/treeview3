/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

/**
 * The idea here is that objects which implement this interface can have their
 * progress depicted by a progress bar.
 *
 * Typically, classes that implement this interface will use a LoadProgress2
 * instance to actually display their progress, and will manage the "phases"
 * portion of their behavior. However, they will pass off a pointer to
 * themselves, cast to ProgressTrackable, to various routines within the phase
 * that will then update the within-phase scrollbar.
 *
 * The purpose of this interface is to allow these slow running subroutines to
 * communicate back to the object that manages the phases of the long running
 * task, which in turn manages the LoadProgress instance.
 *
 * @author aloksaldanha
 */
public interface ProgressTrackable {

	/**
	 * The length holds the length in bytes of the input stream, or -1 if not
	 * known.
	 */
	/** Getter for length */
	public int getLength();

	/**
	 * Enables classes to which we delegate loading to update the length of the
	 * progress bar
	 *
	 * @param i
	 */
	public void setLength(int i);

	/**
	 * The value holds the current position, which will be between 0 and length,
	 * if length is >= 0.
	 */
	/** Getter for value */
	public int getValue();

	/**
	 * sets value of scrollbar
	 *
	 * @param i
	 */
	public void setValue(int i);

	/**
	 * increments scrollbar by fixed amount
	 *
	 * @param i
	 */
	public void incrValue(int i);

	/**
	 *
	 * @return true if this task has been cancelled.
	 */
	public boolean getCanceled();

}
