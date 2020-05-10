/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Class to filter through files for .cdt, .txt, .csv and .pcl files.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.5 $ $Date: 2004-12-21 03:28:14 $
 */
public class AllowedFilesFilter extends javax.swing.filechooser.FileFilter implements
		FilenameFilter {
	/**
	 * from the <code>FilenameFilter</code> interface.
	 *
	 * @param dir
	 *            Directory to look in
	 * @param file
	 *            File name
	 * @return Returns true if file ends with .cdt or .pcl
	 */
	@Override
	public boolean accept(File dir, final String file) {
		dir = null;
		// don't use dir!!!
		if (file.toLowerCase().endsWith(".cdt"))
			return true;
		if (file.toLowerCase().endsWith(".pcl"))
			return true;
		if (file.toLowerCase().endsWith(".txt"))
			return true;
		if (file.toLowerCase().endsWith(".csv"))
			return true;
		return false;
	}

	/**
	 * accepts or rejects files and directories
	 *
	 * @param f
	 *            the file in question
	 * @return returns true if it's a directory, or if it ends in .pcl or .cdt
	 */
	@Override
	public boolean accept(final File f) {
		if (f.isDirectory())
			return true;
		return accept(f, f.getName());
	}

	/* inherit */
	@Override
	public String getDescription() {
		return "CSV, TXT, CDT or PCL Files";
	}

}
