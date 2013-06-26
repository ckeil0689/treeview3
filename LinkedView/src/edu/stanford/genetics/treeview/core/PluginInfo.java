/*
 * Created on Dec 4, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;
import javax.swing.*;

import edu.stanford.genetics.treeview.PluginFactory;
/**
 * Prints info about the installed modules
 * Currently, this is a very boring class.
 * 
 * @author aloksaldanha
 *
 */
public class PluginInfo extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1205246332280997485L;
	public PluginInfo(PluginFactory [] plugins) {
		super();
		if (plugins == null || plugins.length == 0) {
			add(new JLabel("No Plugins Found"));
		} else {
			for (int i = 0 ; i < plugins.length; i++) {
				add(new JLabel(plugins[i].getPluginName()));
			}
		}
	}
	
}
