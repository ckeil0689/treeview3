/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: UrlExtractor2.java,v $
 * $Revision: 1.3 $
 * $Date: 2004-12-21 03:28:14 $
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

import java.util.prefs.Preferences;

/**
 * This is the second url extraction class I'm writing. It's designed to do
 * less... I'm going to make UrlPresets and UrlEditor as well.
 */

public class UrlExtractor2 implements ConfigNodePersistent {

	private Preferences configNode;
	private final UrlPresets presets;

	/**
	 * This class must have a config node to stash data in, even if it's a
	 * dummy. It also needs UrlPresets to infer templates from styles
	 */
	public UrlExtractor2(final Preferences n, final UrlPresets p) {

		configNode = n;
		presets = p;
	}

	/**
	 * returns the text of the header which is to be used for filling out the
	 * url template.
	 */
	public String getColHeader() {

		final String ret = configNode.get("header", null);
		return ret;
	}

	public void setColHeader(final String head) {

		configNode.put("header", head);
	}

	/**
	 * most common use, fills in current template with val
	 */
	public String substitute(final String val) {

		final String temp = getTemplate();
		if (temp == null)
			return null;
		if (val == null)
			return null;
		final int into = temp.indexOf("HEADER");
		if (into < 0)
			return temp;
		return temp.substring(0, into) + val + temp.substring(into + 6);
	}

	public String getTemplate() {

		String ret = configNode.get("template", null);
		if (ret != null)
			return ret;

		// try style preset
		if (ret == null) {
			ret = presets.getTemplate(configNode.get("style", "None"));
		}

		// try custom
		if (ret == null) {
			ret = configNode.get("custom", null);
		}

		// okay, first preset...
		if (ret == null) {
			ret = presets.getTemplate(0);
		}

		configNode.put("template", ret);
		return ret;
	}

	public void setTemplate(final String ret) {

		configNode.put("template", ret);
	}

	// @Override
	// public void bindConfig(final Preferences configNode) {
	// root = configNode;
	// }

	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node("UrlExtractor");

		} else {
			LogBuffer.println("Could not find or create UrlExtractor"
					+ "node because parentNode was null.");
		}
	}
}
