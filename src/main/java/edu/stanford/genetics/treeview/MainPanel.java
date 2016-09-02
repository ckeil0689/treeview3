/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

import java.util.prefs.Preferences;

import javax.swing.ImageIcon;

/**
 * implementing objects are expected to be subclasses of component. The purpose
 * of this class is to provide an interface for LinkedView, whereby different
 * views can be added to a tabbed panel. This is meant to eventually become a
 * plugin interface.
 *
 * @author Alok Saldanha <alok@genome.stanford.edu>
 * @version $Revision: 1.12 $ $Date: 2010-05-02 13:33:30 $
 */
public interface MainPanel {

	public void refresh();

	/**
	 * This syncronizes the sub compnents with their persistent storage.
	 */
	public void syncConfig();

	/**
	 * this method gets the config node on which this component is based, or
	 * null.
	 */
	public Preferences getConfigNode();

	/**
	 * Add items related to settings
	 *
	 * @param menubar
	 *            A menu to add items to.
	 */
	public void populateSettingsMenu(TreeviewMenuBarI menubar);

	/**
	 * Add items which do some kind of analysis
	 *
	 * @param menubar
	 *            A menu to add items to.
	 */
	public void populateAnalysisMenu(TreeviewMenuBarI menubar);

	/**
	 * Add items which allow for export, if any.
	 *
	 * @param menubar
	 *            A menu to add items to.
	 */
	public void populateExportMenu(TreeviewMenuBarI menubar);

	/**
	 * ensure a particular gene is visible. Used by Find.
	 *
	 * The index is relative to the shared TreeSelection object associated with
	 * the enclosing ViewFrame.
	 *
	 * @param i
	 *            Index of gene to make visible
	 */
	public void scrollToGene(int i);

	public void scrollToArray(int i);

	/**
	 *
	 * @return name suitable for displaying in tab
	 */
	public String getName();

	/**
	 *
	 * @return Icon suitable for putting in tab, or in minimized window.
	 */
	public ImageIcon getIcon();

	/**
	 * This exists to allow plugins to have scripted output to image files. This
	 * function is triggered by a -x PluginName argument to the main app.
	 *
	 * @param argv
	 * @throws ExportException
	 */
	public void export(MainProgramArgs args) throws ExportException;

}
