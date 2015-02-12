/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: MainPanel.java,v $
 * $Revision: 1.12 $
 * $Date: 2010-05-02 13:33:30 $
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
package edu.stanford.genetics.treeview;

import javax.swing.JMenu;

/**
 * implementing objects are expected to be subclasses of component. The purpose
 * of this class is to provide an interface for LinkedView, whereby different
 * views can be added to a tabbed panel. This is meant to eventually become a
 * plugin interface.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.12 $ $Date: 2010-05-02 13:33:30 $
 */
public interface DendroPanel {

	public void addDendroMenus(JMenu menu);

	public void addClusterMenus(JMenu menu);

	// public void addSearchMenus(JMenu menu);

	/**
	 *
	 * @return name suitable for displaying in tab
	 */
	public String getName();

}
