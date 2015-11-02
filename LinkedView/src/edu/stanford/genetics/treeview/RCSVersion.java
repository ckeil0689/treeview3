/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.Vector;

public class RCSVersion {

	static Vector versions;
	static boolean added;
	String version;

	public RCSVersion(final String version) {

		this.version = version;

		if (versions == null) {
			versions = new Vector();
		}
		versions.addElement(version);
	}

	public static Vector allVersions() {

		if (!added) {
			added = true;
			new RCSVersion("$Id: RCSVersion.java,v 1.4 2004-12-21 "
					+ "03:28:13 alokito Exp $");
		}
		return versions;
	}

	public String getVersion() {

		return version;
	}

	public static String getAllVersions() {

		final StringBuffer b = new StringBuffer();
		for (int i = 0; i < versions.size(); i++) {

			b.append(((RCSVersion) versions.elementAt(i)).version + "\n");
		}
		return b.toString();
	}
}
