/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

public class LoadException extends Exception {// errors when loading model

	private static final long serialVersionUID = 1L;

	public static final int EXT = 0; // Wrong extension, not cdt
	public static final int CDTPARSE = 1; // cdt parse error
	public static final int ATRPARSE = 2; // atr parse error
	public static final int GTRPARSE = 3; // gtr parse error
	public static final int INTPARSE = 4; // parse interrupted
	public static final int KAGPARSE = 5; // kag parse error
	public static final int KGGPARSE = 6; // kgg parse error
	public static final int NOFILE = 7; // no file selected
	public static final int TXTPARSE = 8; // txt parse error

	int type;

	public LoadException(final String message, final int t) {

		super(message);
		type = t;
	}

	public int getType() {

		return type;
	}

	@Override
	public String getMessage() {

		return "LoadException " + type + ": " + super.getMessage();
	}
}
