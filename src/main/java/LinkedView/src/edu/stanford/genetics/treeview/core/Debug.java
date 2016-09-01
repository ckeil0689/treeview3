/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.core;

import edu.stanford.genetics.treeview.RCSVersion;

public class Debug {
	/*
	 * (0 < level < 5) 0 - Debug off 1 - Programmer Statements 2 - 3 - 4 - 5 -
	 * Everything
	 */
	public static int level = 1;
	static RCSVersion version = new RCSVersion(
			"$Id: Debug.java,v 1.5 2004/12/21 03:28:14 "
					+ "alokito Exp $, debugging "
					+ (level == 0 ? "off" : "level " + level));

	public static void print(final Object caller, final String message,
			final Object argument) {
		if (level > 0) {
			String c = (caller == null) ? "" : caller.toString();
			String a = (argument == null) ? "" : argument.toString();

			if (c.length() > 100) {
				c = c.substring(0, 99);
			}
			if (a.length() > 100) {
				a = a.substring(0, 99);
			}
			System.out.println(c + " : " + message + " : " + a);
		}
	}

	public static void print(final Object caller, final String message) {
		if (level > 0) {
			print(caller, message, "No Argument");
		}
	}

	public static void print(final String message, final Object argument) {
		if (level > 0) {
			print("No Caller", message, argument);
		}
	}

	public static void print(final String message) {
		if (level > 0) {
			print("No Caller", message, "No Arguments");
		}
	}

	public static void print(final Exception e) {
		if (level == 5) {
			e.printStackTrace();
		}
	}
}
