package com.apple.mrj;

import java.io.IOException;

/**
 * This is a stub for the MRJFileUtils class in Mac OS, so I can compile on
 * other platforms.
 * 
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.3 $ $Date: 2003-06-22 08:39:03 $
 */
public class MRJFileUtils {
	/**
	 * Opens URL in some operating system dependant fashion.
	 * 
	 * @param url
	 *            url to be loaded
	 * @exception IOException
	 *                Problem opening url.
	 */
	public static void openURL(final String url) throws IOException {
		throw new IOException("Somehow, you called Alok's stub MRJ");
	}
}
