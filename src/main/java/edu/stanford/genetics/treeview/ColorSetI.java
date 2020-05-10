/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.Color;

/**
 * This is a general interface to color sets. This should be subclassed for
 * particular color sets. Other classes, such as presets, which want to
 * configure or get colors can do it through this interface in a general way.
 * They can also down-cast if they need to. In general, the types should be
 * constants within the subclass. See an existing example to get the idea.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version @version $Revision: 1.4 $ $Date: 2004-12-21 03:28:14 $
 */
public interface ColorSetI {
	/**
	 * textual descriptions of the types of colors in this set
	 */
	public abstract String[] getTypes();

	/**
	 * get the color corresponding to the i'th type.
	 *
	 * @param i
	 *            type index
	 * @return The actual color
	 */
	public abstract Color getColor(int i);

	/**
	 * set the i'th color to something new.
	 *
	 * @param i
	 *            index of the type
	 * @param newColor
	 *            The new color
	 */
	public abstract void setColor(int i, Color newColor);

	/**
	 * get the description of the i'th type
	 *
	 * @param i
	 *            index of the type
	 * @return The description of the type
	 */
	public abstract String getType(int i);

	/**
	 * get the name of the color set
	 *
	 * @return Usually the name of the subclass.
	 */
	public String getName();
}
