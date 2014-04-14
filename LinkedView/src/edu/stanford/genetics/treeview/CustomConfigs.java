/*
 * Created on Jun 5, 2008
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview;

import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This class encapsulates a panel that allows editing of the preferences. I
 * will put all of the actual class code below
 * 
 * @author aloksaldanha
 * 
 */
class PreferencesPanel extends JPanel {

	/**
	 * this is a default value to appease java
	 */
	private static final long serialVersionUID = 1L;
	private final CustomConfigs prefs;
	private final JCheckBox cbMacStyle = new JCheckBox(
			"Use platform-specific menubar? (requires restart)");

	/**
	 * @param prefs
	 */
	public PreferencesPanel(final CustomConfigs prefs) {
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

public class CustomConfigs implements ConfigNodePersistent{

	public static int DEFAULT_MacStyleMenubar = 1;
	
	// Root Preferences node of this class.
	private Preferences configNode;

//	@Override
//	public void bindConfig(final Preferences configNode) {
//		
//		this.root = configNode;
//	}
	
	@Override
	public void setConfigNode(Preferences parentNode) {
		
		if(parentNode != null) {
			this.configNode = parentNode.node("CustomConfigs");
			
		} else {
			LogBuffer.println("Could not find or create CustomConfigs" +
					"node because parentNode was null.");
		}
		
	}

	public boolean getMacStyleMenubar() {
		
		// Default will be true
		return (configNode.getInt("macStyleMenubar",
				CustomConfigs.DEFAULT_MacStyleMenubar) == 1);
	}

	public void setMacStyleMenubar(final boolean parse) {
		
		if (parse) {
			configNode.putInt("macStyleMenubar", 1);
			
		} else {
			configNode.putInt("macStyleMenubar", 0);
		}
	}

	public void showEditor() {
		
		final PreferencesPanel p = new PreferencesPanel(this);
		JOptionPane.showMessageDialog(null, p, "Please configure preferences",
				JOptionPane.QUESTION_MESSAGE);
		p.save();
	}
}
