/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

/** I, Alok, didn't write this. */
public class Debug {
	public static final boolean on = true;
	static RCSVersion version = new RCSVersion(
			"$Id: Debug.java,v 1.6 2006-08-18 06:50:17 rqluk Exp $, debugging"
					+ (on ? "on" : "off"));

	public static void print(final Object caller, final String message,
			final Object argument) {
		if (on) {
			String c = (caller == null) ? "" : caller.toString();
			String a = (argument == null) ? "" : argument.toString();

			if (c.length() > 79) {
				c = c.substring(0, 79);
			}
			if (a.length() > 77) {
				a = a.substring(0, 77);
			}
			System.out.println(c + ":" + message + "(" + a + ")");
		}
	}

	public static void print(final Object caller, final String message) {
		if (on) {
			print(caller, message, null);
		}
	}

	public static void print(final String message) {
		if (on) {
			print(null, message, null);
		}
	}

}
