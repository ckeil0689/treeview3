package edu.stanford.genetics.treeview;

import java.util.Observable;
import java.util.Observer;

import edu.stanford.genetics.treeview.plugin.dendroview.InteractiveMatrixView;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

/**
 * This controller explicitly handles direct user interaction with the
 * InteractiveMatrixView.  
 * @author chris0689
 *
 */
public class InteractiveMatrixViewController implements Observer {

	private InteractiveMatrixView imView;
	
	private MapContainer interactiveXmap;
	private MapContainer interactiveYmap;

	private MapContainer globalXmap;
	private MapContainer globalYmap;
	
	public InteractiveMatrixViewController(final InteractiveMatrixView imView) {
		
		this.imView = imView;
	}

	@Override // Observer code
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Assigns references of MapContainer instances to be used for 
	 * interactivity and information display in InteractiveMatrixView. 
	 * @param xmap
	 * @param ymap
	 */
	public void setInteractiveMapContainers(final MapContainer xmap, 
			final MapContainer ymap) {
		
		this.interactiveXmap = xmap;
		this.interactiveYmap = ymap;
		
		interactiveXmap.addObserver(this);
		interactiveYmap.addObserver(this);
	}
	
	/**
	 * Assigns references of MapContainer instances to be used for 
	 * information display in GlobalMatrixView. 
	 * @param xmap
	 * @param ymap
	 */
	public void setGlobalMapContainers(final MapContainer xmap, 
			final MapContainer ymap) {
		
		this.globalXmap = xmap;
		this.globalYmap = ymap;
		
		globalXmap.addObserver(this);
		globalYmap.addObserver(this);
	}
}
