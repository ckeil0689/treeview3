/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * This class encapsulates a list of URL presets. This is the class to edit the
 * default presets in...
 */

public class UrlPresets implements ConfigNodePersistent {

	private final static int dIndex = 0;

	private Preferences configNode; // which preset to use if not by
	// confignode?
	private final String type;

	public UrlPresets(final String type) {

		super();
		this.type = type;
	}
	
	@Override
	public void setConfigNode(final Preferences parentNode) {

		if (parentNode != null) {
			this.configNode = parentNode.node(type);

		} else if (parentNode == null && configNode == null) {
			// Unrelated dummy node for testing
			configNode = Preferences.userRoot().node(type);

		} else {
			LogBuffer.println("There was a problem with UrlPresets node "
					+ "setting.");
		}

		if (getPresetNames().length == 0) {
			addDefaultGenePresets();
		}
	}

	@Override
	public Preferences getConfigNode() {

		return configNode;
	}

	@Override
	public void requestStoredState() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void storeState() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * returns default preset, for use when opening a new file which has no url
	 * settings
	 */
	public int getDefaultPreset() {

		return configNode.getInt("default", dIndex);
	}

	public boolean isDefaultEnabled() {

		return (getDefaultPreset() != -1);
	}

	public String getDefaultTemplate() {

		final int defaultPreset = getDefaultPreset();

		if (defaultPreset == -1)
			return null;

		try {
			return getTemplate(defaultPreset);

		} catch (final Exception e) {
			return getTemplate(0);
		}
	}

	public void setDefaultPreset(final int i) {

		configNode.putInt("default", i);
	}

	public void addDefaultGenePresets() {

		addPreset("Google Scholar", "http://scholar.google.com/");

		addPreset("YPD",
				"http://www.proteome.com/databases/YPD/reports/HEADER.html");

		addPreset("WormBase", "http://www.wormbase.org/db/searches/"
				+ "basic?class=AnyGene&query=HEADER&Search=Search");

		addPreset("Source_CloneID", "source-search.princeton.edu/"
				+ "cgi-bin/source/sourceResult?"
				+ "option=CloneID&choice=Gene&criteria=HEADER");

		addPreset("FlyBase", "http://flybase.org/");

		addPreset("MouseGD", "http://www.informatics.jax.org/marker/");

		addPreset("GenomeNetEcoli", "http://www.genome.jp/dbget/");
	}

	/**
	 * returns String [] of preset names for display
	 */
	public String[] getPresetNames() {

		try {
			// Preferences presetNode = root.node("Preset");
			final String[] astring = configNode.childrenNames();

			return astring;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a boolean array which contains information about whether each
	 * preset is enabled or not.
	 *
	 * @return
	 */
	public boolean[] getPresetEnablings() {

		final Preferences presetNode = configNode.node("Preset");
		try {
			final String[] childrenNodes = presetNode.childrenNames();
			final boolean aboolean[] = new boolean[childrenNodes.length];

			String temp;
			for (int i = 0; i < childrenNodes.length; i++) {

				temp = presetNode.node(childrenNodes[i])
						.get("enabled", "false");

				if (temp.toLowerCase().equals("true")) {
					aboolean[i] = true;

				} else {
					aboolean[i] = false;
				}
			}

			return aboolean;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a String array of preset headers.
	 *
	 * @return
	 */
	public String[] getPresetHeaders() {

		final Preferences presetNode = configNode.node("Preset");

		try {
			final String[] childrenNodes = presetNode.childrenNames();
			final String astring[] = new String[childrenNodes.length];

			for (int i = 0; i < childrenNodes.length; i++) {
				astring[i] = presetNode.node(childrenNodes[i])
						.get("header", "");
			}
			return astring;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * returns the template for the ith preset or null, if i too large.
	 */
	public String getTemplate(final int index) {

		// final Preferences presetNode = configNode.node("Preset");

		try {
			final String[] childrenNodes = configNode.childrenNames();
			if (index < childrenNodes.length)
				return configNode.node(childrenNodes[index]).get("template",
						null);
			else
				return null;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * returns the template for this name or null, if name not found in kids
	 */
	public String getTemplate(final String name) {

		final Preferences presetNode = configNode.node("Preset");
		String template = null;
		try {
			final String[] childrenNodes = presetNode.childrenNames();

			for (final String childrenNode : childrenNodes) {

				if (name.equalsIgnoreCase(childrenNode)) {
					template = presetNode.node(childrenNode).get("template",
							null);
				}
			}

			return template;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 *
	 * @param header
	 *            the header for which we want a URL template
	 * @return null if header is not specified in presets
	 */
	public String getTemplateByHeader(final String header) {

		final Preferences presetNode = configNode.node("Preset");
		try {
			final String[] childrenNodes = presetNode.childrenNames();
			final boolean[] enablings = getPresetEnablings();

			String template = null;
			for (int i = 0; i < childrenNodes.length; i++) {
				if (enablings[i]
						&& matchPattern(
								header,
								presetNode.node(childrenNodes[i]).get("header",
										null))) {
					// may cause compatibility issues with old .jtv files
					template = presetNode.node(childrenNodes[i]).get(
							"template", null);
				}
			}

			return template;

		} catch (final BackingStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean matchPattern(final String string, final String pattern) {

		for (int i = 0, j = 0; i < pattern.length(); i++, j++) {
			if (pattern.charAt(i) == '*') {
				if (i == pattern.length() - 1)
					return true;
				else if (j == string.length() - 1)
					return false;
				else if (pattern.charAt(i + 1) == '*') {
					j--;
					continue;
				} else if (pattern.charAt(i + 1) == string.charAt(j + 1)) {
					continue;
				} else {
					i--;
					continue;
				}
			} else if (pattern.charAt(i) != string.charAt(j))
				return false;
		}
		return true;
	}

	// public void setPresetName(final int index, final String name) {
	//
	// final Preferences presetNode = root.node("Preset");
	// String[] childrenNodes = presetNode.childrenNames();
	//
	// try {
	// childrenNodes[index].put("name", name, null);
	// } catch (final java.lang.ArrayIndexOutOfBoundsException e) {
	// System.out.println("UrlPresets.setPresetName() got error: " + e);
	// }
	// }

	public void setPresetHeader(final int index, final String header) {

		final Preferences presetNode = configNode.node("Preset");

		try {
			final String[] childrenNodes = presetNode.childrenNames();

			presetNode.node(childrenNodes[index]).put("header", header);

		} catch (final BackingStoreException e1) {
			e1.printStackTrace();
			LogBuffer.println("Error in URLPresets/setHeader(): "
					+ e1.getMessage());

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			LogBuffer.println("UrlPresets.setPresetHeader() got error: "
					+ e.getMessage());
		}
	}

	public void setPresetEnabled(final int index, final boolean enabled) {

		final Preferences presetNode = configNode.node("Preset");

		try {
			final String[] childrenNodes = presetNode.childrenNames();

			if (enabled) {
				presetNode.node(childrenNodes[index]).put("enabled", "true");

			} else {
				presetNode.node(childrenNodes[index]).put("enabled", "false");
			}

		} catch (final BackingStoreException e1) {
			e1.printStackTrace();
			LogBuffer.println("Error in URLPresets/setPresetEnabled(): "
					+ e1.getMessage());

		} catch (final java.lang.ArrayIndexOutOfBoundsException e) {
			System.out.println("UrlPresets.setPresetEnabled() got error: "
					+ e.getMessage());
		}
	}

	public void setPresetTemplate(final int index, final String template) {

		final Preferences presetNode = configNode.node("Preset");

		try {
			final String[] childrenNodes = presetNode.childrenNames();

			presetNode.node(childrenNodes[index]).put("template", template);

		} catch (final BackingStoreException e1) {
			e1.printStackTrace();
			LogBuffer.println("Error in URLPresets/setPresetEnabled(): "
					+ e1.getMessage());
		}
	}

	public void addPreset(final String name, final String template) {

		final Preferences preset = configNode.node(name);

		preset.put("template", template);// , null);
		preset.put("header", "*");// , null);
		preset.put("enabled", "false");// , null);
	}

	/**
	 * Creates a new subNode for the Preset node.
	 *
	 * @param name
	 * @param template
	 * @param header
	 * @param enabled
	 */
	public void addPreset(final String name, final String template,
			final String header, final boolean enabled) {

		final Preferences preset = configNode.node(name);

		preset.put("template", template);// , null);
		preset.put("header", header);// , null);

		if (enabled) {
			preset.put("enabled", "true");// , null);

		} else {
			preset.put("enabled", "false");// , null);
		}
	}
}
