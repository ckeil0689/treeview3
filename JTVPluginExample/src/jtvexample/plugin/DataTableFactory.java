/*
 * Created on Sep 27, 2006
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package jtvexample.plugin;

import edu.stanford.genetics.treeview.*;
import edu.stanford.genetics.treeview.core.PluginManager;

public class DataTableFactory extends PluginFactory {
	
	// this piece of magic actually registers the plugin.
	static {
		PluginManager.registerPlugin(new DataTableFactory());
	}
	@Override
	public String getPluginName() {
		return "DataTable";
	}

	@Override
	public MainPanel restorePlugin(ConfigNode configNode, ViewFrame viewFrame) {
		return new DataTablePanel(configNode, viewFrame);
	}

	@Override
	public boolean configurePlugin(ConfigNode node, ViewFrame viewFrame) {
		return true;
	}

}
