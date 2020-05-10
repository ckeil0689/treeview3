/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package gui.colorPicker;

/**
 * Mostly a fossil interface, since the only contrast selectable thing is the
 * ColorExtractor.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.4 $ $Date: 2004-12-21 03:28:13 $
 */
public interface ContrastSelectable {
	/**
	 * @return The contrast value
	 */
	public double getContrast();

	/**
	 * @param newContrast
	 *            The new contrast value
	 */
	public void setContrast(double newContrast);

	/** notify any observers if your value has changed. */
	public void notifyObservers();
}
