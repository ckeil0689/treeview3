/*
 * Created on Jun 5, 2008
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview;

import javax.swing.*;

/**
 * This class encapsulates a panel that allows editing of the preferences.
 * I will put all of the actual class code below
 * @author aloksaldanha
 *
 */
class PreferencesPanel extends JPanel {

	/**
	 * this is a default value to appease java
	 */
	private static final long serialVersionUID = 1L;
	private Preferences prefs;
	private JCheckBox cbMacStyle = new JCheckBox("Use platform-specific menubar? (requires restart)");

	/**
	 * @param prefs
	 */
	public PreferencesPanel(Preferences prefs) {
		super();
		addComponents();
		this.prefs = prefs;
		load();
	}
	
	
	private void addComponents() {
		add(cbMacStyle);		
	}

	/**
	 * loads values from prefs to panel
	 */
	public void load() {
		cbMacStyle.setSelected(prefs.getMacStyleMenubar());		
	}

	/**
	 * stores values from panel to prefs
	 */
	public void save() {
		prefs.setMacStyleMenubar(cbMacStyle.isSelected());
	}	
}

public class Preferences implements ConfigNodePersistent {
	
	
	
	public static int DEFAULT_MacStyleMenubar = 1;
	private ConfigNode root;
	public void bindConfig(ConfigNode configNode) {
			root = configNode;
	}
	
	public boolean getMacStyleMenubar() {
		return (root.getAttribute("macStyleMenubar", Preferences.DEFAULT_MacStyleMenubar) == 1);
	}
	public void setMacStyleMenubar(boolean parse) {
		if (parse)
			root.setAttribute("macStyleMenubar",1, Preferences.DEFAULT_MacStyleMenubar);
		else
			root.setAttribute("macStyleMenubar",0, Preferences.DEFAULT_MacStyleMenubar);
	}	
	public void showEditor() {
		PreferencesPanel p = new PreferencesPanel(this);
		JOptionPane.showMessageDialog(null, p, "Please configure preferences", JOptionPane.QUESTION_MESSAGE);
		p.save();
	}	
}
