package Controllers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.plugin.dendroview.DendroView2;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;

public class DendroController {

	private DendroView2 dendroView;
	private TreeViewFrame tvFrame;
	
	public DendroController(DendroView2 dendroView, TreeViewFrame tvFrame) {
		
		this.dendroView = dendroView;
		this.tvFrame = tvFrame;
		
		// add listeners
		dendroView.addScaleListener(new ScaleListener());
		dendroView.addZoomListener(new ZoomListener());
		dendroView.addCompListener(new ResizeListener());
	}
	
	/**
	 * Listener for the setScale-buttons in DendroView.
	 * Changes the scale in xMap and yMap MapContainers, allowing the user
	 * to zoom in or out of each individual axis in GlobalView.
	 * @author CKeil
	 *
	 */
	class ScaleListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource() == dendroView.getXPlusButton()) {
				dendroView.getGlobalXmap().zoomIn();
				
			} else if(e.getSource() == dendroView.getXMinusButton()) {
				dendroView.getGlobalXmap().zoomOut();
				
			} else if(e.getSource() == dendroView.getYPlusButton()) {
				dendroView.getGlobalYmap().zoomIn();
				
			} else if(e.getSource() == dendroView.getYMinusButton()) {
				dendroView.getGlobalYmap().zoomOut();
				
			} else if(e.getSource() == dendroView.getHomeButton()) {
				dendroView.getGlobalXmap().setHome();
				dendroView.getGlobalYmap().setHome();
			}
		}
	}
	
	/**
	 * The Zoom listener which allows the user to zoom into a selection.
	 * @author CKeil
	 *
	 */
	class ZoomListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			dendroView.getGlobalView().zoomSelection();
			dendroView.getGlobalView().centerSelection();
		}
		
	}
	
	/**
	 * Listens to the resizing of DendroView2 and makes 
	 * changes to MapContainers as a result.
	 * @author CKeil
	 *
	 */
	class ResizeListener implements ComponentListener {

		private MapContainer globalXmap = dendroView.getGlobalXmap();
		private MapContainer globalYmap = dendroView.getGlobalYmap();
				
		// Component Listeners
		@Override
		public void componentHidden(final ComponentEvent arg0) {
		}

		@Override
		public void componentMoved(final ComponentEvent arg0) {
		}

		@Override
		public void componentResized(final ComponentEvent arg0) {
			
			if (globalXmap.getAvailablePixels() > globalXmap.getUsedPixels()
					&& globalXmap.getScale() == globalXmap.getMinScale()) {
				globalXmap.setHome();
			}

			if (globalYmap.getAvailablePixels() > globalYmap.getUsedPixels()
					&& globalYmap.getScale() == globalYmap.getMinScale()) {
				globalYmap.setHome();
			}

			dendroView.getDendroPane().revalidate();
			dendroView.getDendroPane().repaint();
		}

		@Override
		public void componentShown(final ComponentEvent arg0) {
		}
	}
}
