package Controllers;

public interface Controller {

	/**
	 * Manages the addition of all locally defined listener classes 
	 * (MouseListeners, ActionListeners etc.) to the respective view's 
	 * components such as buttons.
	 */
	public abstract void addListeners();
	
	/**
	 * Define key bindings for a view.
	 */
	public abstract void addKeyBindings();
}
