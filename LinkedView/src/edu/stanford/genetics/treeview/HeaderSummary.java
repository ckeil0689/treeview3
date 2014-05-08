/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: HeaderSummary.java,v $
 * $Revision: 1.11 $
 * $Date: 2005-12-05 05:27:53 $
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

import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * this class generates a single string summary of a HeaderInfo.
 */
public class HeaderSummary extends Observable implements ConfigNodePersistent {

	private Preferences configNode;
	private int[] included = new int[] { 1 };
	private final String type;

	public HeaderSummary(final String type) {

		super();
		this.type = type;
	}

	public void setIncluded(final int[] newIncluded) {

		included = newIncluded;
		synchronizeTo();
		setChanged();
		notifyObservers();
	}

	public int[] getIncluded() {

		return included;
	}

	/**
	 * returns the best possible summary for the specified index.
	 * 
	 * If no headers are applicable, will return the empty string.
	 */
	public String getSummary(final HeaderInfo headerInfo, final int index) {

		String[] strings = null;
		try {
			strings = headerInfo.getHeader(index);

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("index " + index
					+ " out of bounds on headers, continuing");
			LogBuffer.println("ArrayIndexOutOfBoundsException in "
					+ "getSummary() in HeaderSummary: " + e.getMessage());
			return null;
		}

		if (strings == null) {

			return "";
		}

		final StringBuffer out = new StringBuffer();
		int count = 0;
		if (included.length == 0) {
			return "";
		}

		for (int i = 0; i < included.length; i++) {
			try {
				final String test = strings[included[i]];
				if (test != null) {
					if (count != 0) {
						out.append(", ");
					}
					out.append(test);
					count++;
				}
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				// out.append(strings[1]);
				// LogBuffer.println("ArrayIndexOutOfBoundsException in " +
				// "getSummary() in HeaderSummary: " + e.getMessage());
				// LogBuffer.println("strings[]: " + Arrays.toString(strings));
				// LogBuffer.println("included[i]: " + included[i]);
			}
		}

		if (count == 0) {
			return "";

		} else {
			return out.toString();
		}
	}

	public String[] getSummaryArray(final HeaderInfo headerInfo, final int index) {

		String[] strings = null;
		try {
			strings = headerInfo.getHeader(index);

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			// LogBuffer.println("index " + index +
			// " out of bounds on headers, "
			// + "continuing");
			// LogBuffer.println("ArrayIndexOutOfBoundsException in " +
			// "getSummaryArray() in HeaderSummary: " + e.getMessage());
			return null;
		}

		if (strings == null) {
			return null;
		}

		if (included.length == 0) {
			return null;
		}

		final String[] out = new String[included.length];
		int count = 0;
		for (int i = 0; i < included.length; i++) {
			try {
				final String test = strings[included[i]];
				out[count] = test;
				count++;
			} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
				// out.append(strings[1]);
				// LogBuffer.println("ArrayIndexOutOfBoundsException in " +
				// "getSummaryArray() in HeaderSummary: "
				// + e.getMessage());
			}
		}
		return out;
	}

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(type);

		} else {
			LogBuffer.println("Could not find or create HeaderSummary"
					+ "node because parentNode was null.");
		}
		synchronizeFrom();
	}

	private void synchronizeFrom() {

		if (configNode == null) {
			return;
		}

		if (nodeHasAttribute("included")) {
			final String incString = configNode.get("included", "1");
			if (incString.equals("")) {
				setIncluded(new int[0]);

			} else {
				int numComma = 0;
				for (int i = 0; i < incString.length(); i++) {
					if (incString.charAt(i) == ',')
						numComma++;
				}

				final int[] array = new int[numComma + 1];
				numComma = 0;
				int last = 0;
				for (int i = 0; i < incString.length(); i++) {
					if (incString.charAt(i) == ',') {
						final Integer x = new Integer(incString.substring(last,
								i));
						array[numComma++] = x.intValue();
						last = i + 1;
					}
				}
				try {
					array[numComma] = Integer.parseInt(incString
							.substring(last));

				} catch (final NumberFormatException e) {
					LogBuffer.println("HeaderSummary has trouble "
							+ "restoring included list from " + incString);
					LogBuffer.println("NumberFormatException in "
							+ "synchronizeFrom() in " + "HeaderSummary: "
							+ e.getMessage());
				}
				setIncluded(array);
			}
		}
	}

	private void synchronizeTo() {

		if (configNode == null) {
			return;
		}

		final int[] vec = getIncluded();
		final StringBuffer temp = new StringBuffer();
		if (vec.length > 0) {
			temp.append(vec[0]);
		}

		for (int i = 1; i < vec.length; i++) {

			temp.append(',');
			temp.append(vec[i]);
		}
		configNode.put("included", temp.toString());
	}

	public boolean nodeHasAttribute(final String name) {

		boolean contains = false;

		try {
			final String[] keys = configNode.keys();

			for (int i = 0; i < keys.length; i++) {

				if (keys[i].equalsIgnoreCase(name)) {
					contains = true;
					break;
				}
			}
			return contains;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return contains;
		}
	}
}
