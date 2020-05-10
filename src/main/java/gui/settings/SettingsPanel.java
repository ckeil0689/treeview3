/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package gui.settings;

public interface SettingsPanel {
	/**
	 * Called before the window is closed. Allows windows to cleanup and things
	 * before returning control to the main application. This is generally
	 * limited to synchronization, since the window might get opened again.
	 */
	public void synchronizeTo();

	/**
	 * Called before the window is opened. This allows any components to
	 * reinitialize themselves off of any non-observable objects.
	 */
	public void synchronizeFrom();

}
