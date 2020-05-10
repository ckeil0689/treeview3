package model.fileImport;

/**
 * This interface should be used for all classes which require a reset of their
 * state when a new model is loaded. This is important for main GUI classes 
 * (e.g. MapContainers) as well as classes handling extended information 
 * about the underlying data (e.g. HeaderSummary).
 * @author CKeil
 *
 */
public interface ModelLoadReset {

	/**
	 * Reset all member variables to a default state in order to create a fresh
	 * clean state which can later be used to sync with stored Preferences.
	 * This is important in order to avoid carrying over unwanted data when
	 * a new model is loaded if no new objects are created. 
	 */
	public void resetDefaults();
}
