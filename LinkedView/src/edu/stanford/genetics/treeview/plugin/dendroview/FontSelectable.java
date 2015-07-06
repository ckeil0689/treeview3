/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: FontSelectable.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:45 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular,
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview.plugin.dendroview;

/**
 * Interface for things which I want to change the font of
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.1 $ $Date: 2006-08-16 19:13:45 $
 */
public abstract interface FontSelectable {
	/**
	 * @return The current face
	 */
	public abstract String getFace();

	/**
	 * @return The current point size
	 */
	public abstract int getPoints();

	/**
	 * @return The last used main font size (excludes setting size of hint
	 *         label)
	 */
	public abstract int getLastSize();

	/**
	 * @return The current minimum font size.
	 */
	public abstract int getMin();

	/**
	 * @return The current maximum font size.
	 */
	public abstract int getMax();

	/**
	 * @return The current style
	 */
	public abstract int getStyle();

	/**
	 * 
	 * @return Whether fonts size is fixed.
	 */
	public abstract boolean getFixed();

	/**
	 * @return gets label justification
	 */
	public abstract boolean getJustifyOption();

	public abstract java.awt.Font getFont();

	/**
	 * sets the face
	 *
	 * @param string
	 *            The new face value
	 */
	public abstract void setFace(String string);

	/**
	 * Sets the point size but also remembers it in an instsnce variable.
	 *
	 * @param i
	 *            The new points value
	 */
	public abstract void setSavedPoints(int i);

	/**
	 * Sets the point size
	 *
	 * @param i
	 *            The new points value
	 */
	public abstract void setPoints(int i);

	/**
	 * Sets a minimum point size
	 *
	 * @param i
	 *            The new points value
	 */
	public abstract void setMin(int i);

	/**
	 * Sets a maximum point size
	 *
	 * @param i
	 *            The new points value
	 */
	public abstract void setMax(int i);

	/**
	 * Sets whether the font size changes dynamically or remains fixed.
	 * 
	 * @param fixed
	 */
	public abstract void setFixed(boolean fixed);

	/**
	 * Sets the style
	 *
	 * @param i
	 *            The new style value
	 */
	public abstract void setStyle(int i);

	/**
	 * Sets label justification
	 *
	 * @param i
	 *            The new style value
	 */
	public abstract void setJustifyOption(boolean isRightJustified);

}
