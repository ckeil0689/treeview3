/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/* I have decided to stuff random helper routines in here */

public class Util {
	// this class is mostly for static methods
	public static String URLtoFilePath(final String fileURL) {
		String dir = null;
		try {
			dir = URLDecoder.decode(fileURL, "UTF-8");
			dir = dir.replace('/', File.separatorChar);
		} catch (final UnsupportedEncodingException e) {
			// this should really never be called.
			e.printStackTrace();
		}
		return dir;
	}
}
