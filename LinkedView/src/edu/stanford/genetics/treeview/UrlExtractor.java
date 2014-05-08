/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: UrlExtractor.java,v $
 * $Revision: 1.7 $
 * $Date: 2008-04-23 23:26:48 $
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

import java.awt.Frame;
import java.io.UnsupportedEncodingException;
import java.util.prefs.Preferences;

/**
 * This class extracts Urls from HeaderInfo. Also included is a class to pop up
 * a configuration window.
 */
public class UrlExtractor {
	/**
	 * This class must be constructed around gene header info
	 */
	public UrlExtractor(final HeaderInfo hI) {

		headerInfo = hI;
		urlTemplate = dUrlTemplate;
		index = dindex;
		isEnabled = isDefaultEnabled;
		uPresets = null;
	}

	public UrlExtractor(final HeaderInfo hI, final UrlPresets uPresets) {

		headerInfo = hI;
		urlTemplate = dUrlTemplate;
		index = dindex;
		isEnabled = isDefaultEnabled;
		this.uPresets = uPresets;
		setDefaultTemplate(uPresets.getDefaultTemplate());
		setDefaultEnabled(uPresets.isDefaultEnabled());
	}

	/**
	 * can be bound to config node to provide persistence
	 */
	public void bindConfig(final Preferences n) {

		root = n;
		// extract state...
		urlTemplate = root.get("urlTemplate", dUrlTemplate);
		index = root.getInt("index", dindex);
		// some shennanigans since I can't store booleans in a confignode...
		int ide = 0;
		if (isDefaultEnabled == true)
			ide = 1;
		isEnabled = (root.getInt("isEnabled", ide) == 1);
	}

	/**
	 * most common use, returns a String rep of a url given an index returns
	 * null if not enabled, or if the header for this gene is null.
	 */
	public String getUrl(final int i) {
		if (isEnabled() == false)
			return null;
		final String[] headers = headerInfo.getHeader(i);
		if (headers == null)
			return null;
		return substitute(urlTemplate, headers[index]);

	}

	public String getUrl(final int i, final String header) {
		if (uPresets == null)
			return null;
		if (isEnabled() == false)
			return null;
		final String[] headers = headerInfo.getHeader(i);
		if (headers == null)
			return null;
		String tmpTemplate = uPresets.getTemplateByHeader(header);
		if (tmpTemplate == null)
			tmpTemplate = urlTemplate;
		return substitute(tmpTemplate, headers[index]);
	}

	public String substitute(final String val) {
		return substitute(urlTemplate, val);
	}

	private String substitute(final String temp, final String val) {
		if (val == null)
			return null;
		final int into = temp.indexOf("HEADER");
		if (into < 0)
			return temp;
		try {
			return temp.substring(0, into)
					+ java.net.URLEncoder.encode(val, "UTF-8")
					+ temp.substring(into + 6);
		} catch (final UnsupportedEncodingException e) {
			LogBuffer.println("unsupported encoding? this shouldn't happen. "
					+ e);
			e.printStackTrace();
			return temp;
		}
	}

	/**
	 * pops up a configuration dialog.
	 */
	public void showConfig(final Frame f) {
		// deprecated...
	}

	// accessors
	public void setIndex(final int i) {
		index = i;
		if (root != null)
			root.putInt("index", index);
	}

	public int getIndex() {
		return index;
	}

	public void setUrlTemplate(final String c) {
		urlTemplate = c;
		if (root != null)
			root.put("urlTemplate", urlTemplate);
	}

	public String getUrlTemplate() {
		return urlTemplate;
	}

	public void setDefaultTemplate(final String temp) {
		dUrlTemplate = temp;
	}

	public void setDefaultIndex(final int i) {
		dindex = i;
	}

	public void setDefaultEnabled(final boolean b) {
		isDefaultEnabled = b;
	}

	public void setEnabled(final boolean b) {
		isEnabled = b;
		// some shennanigans since I can't store booleans in a confignode...
		int ide = 0;
		if (isDefaultEnabled == true)
			ide = 1;

		int ie = 0;
		if (isEnabled == true)
			ie = 1;

		if (root != null)
			root.putInt("isEnabled", ie);

	}

	public boolean isEnabled() {
		return isEnabled;
	}

	// does the user actually want linking to happen?
	private boolean isEnabled;
	private boolean isDefaultEnabled = true;

	// durlTemplate is the actual text of the url to be substituted
	private String urlTemplate;
	private String dUrlTemplate = "http://www.google.com/search?q=HEADER";

	// the index is the header column of the cdt/pcl which is used for
	// substitution
	private int index;
	private int dindex = 1;

	private HeaderInfo headerInfo;

	/** Setter for headerInfo */
	public void setHeaderInfo(final HeaderInfo headerInfo) {
		this.headerInfo = headerInfo;
	}

	/** Getter for headerInfo */
	public HeaderInfo getHeaderInfo() {
		return headerInfo;
	}

	private Preferences root;
	UrlPresets uPresets;
}
