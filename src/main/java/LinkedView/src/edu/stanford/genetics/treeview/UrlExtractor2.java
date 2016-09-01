/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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

		this.configNode = n;
		this.presets = p;
	}
	
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode == null) {
			LogBuffer.println("Could not find or create UrlExtractor"
					+ "node because parentNode was null.");
			return;

		} 
		this.configNode = parentNode.node("UrlExtractor");
		requestStoredState();
	}
	
	@Override
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {
		
		if(configNode == null) {
			LogBuffer.println("Could not restore state because parent node "
					+ "was null.");
			return;
		}
		
	}

	@Override
	public void storeState() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void importStateFrom(Preferences oldNode) {
		// TODO Auto-generated method stub
		
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
}
