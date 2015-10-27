/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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

	private Preferences root;
	UrlPresets uPresets;

	/**
	 * This class must be constructed around gene header info
	 */
	public UrlExtractor(final HeaderInfo hI) {

		this.headerInfo = hI;
		this.urlTemplate = dUrlTemplate;
		this.index = dindex;
		this.isEnabled = isDefaultEnabled;
		this.uPresets = null;
	}

	public UrlExtractor(final HeaderInfo hI, final UrlPresets uPresets) {

		this.headerInfo = hI;
		this.urlTemplate = dUrlTemplate;
		this.index = dindex;
		this.isEnabled = isDefaultEnabled;
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
		if (isDefaultEnabled == true) {
			ide = 1;
		}
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
		if (tmpTemplate == null) {
			tmpTemplate = urlTemplate;
		}
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
		if (root != null) {
			root.putInt("index", index);
		}
	}

	public int getIndex() {
		return index;
	}

	public void setUrlTemplate(final String c) {
		urlTemplate = c;
		if (root != null) {
			root.put("urlTemplate", urlTemplate);
		}
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
		if (isDefaultEnabled == true) {
		}

		int ie = 0;
		if (isEnabled == true) {
			ie = 1;
		}

		if (root != null) {
			root.putInt("isEnabled", ie);
		}

	}

	public boolean isEnabled() {
		return isEnabled;
	}

	/** Setter for headerInfo */
	public void setHeaderInfo(final HeaderInfo headerInfo) {
		this.headerInfo = headerInfo;
	}

	/** Getter for headerInfo */
	public HeaderInfo getHeaderInfo() {
		return headerInfo;
	}
}
