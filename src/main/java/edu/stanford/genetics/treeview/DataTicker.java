/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import Utilities.GUIFactory;
import edu.stanford.genetics.treeview.plugin.dendroview.MapContainer;
import net.miginfocom.swing.MigLayout;

public class DataTicker implements Observer{

	private final JPanel tickerPanel;
	private final JTextArea valTextArea;
	/* 
	 * describes which of the average values that is being displayed
	 * e.g. Data Value, Row Ave, Column Ave...
	 */
	private final JLabel textLabel;
	/* dataModel contains the actual data  */
	private DataModel dataModel;
	/* Maps to know the position of the matrix*/
	private MapContainer xmap;
	private MapContainer ymap;

	//Timer to delay performing costly mean calculations until matrix navigation
	//is done
	private int meanCalcDelay = 500;
	private javax.swing.Timer meanCalcTimer;

	/**
	 * Creates a new DataTicker instance.
	 */
	public DataTicker() {

		this.valTextArea = GUIFactory.createWrappableTextArea();
		this.textLabel = GUIFactory.createLabel("",GUIFactory.FONTS);
		textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		this.tickerPanel = new TickerPanel();
	}

	public JPanel getTickerPanel() {

		return tickerPanel;
	}

	/**
	 * MessageCanvas subclass
	 */
	public class TickerPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public TickerPanel() {

			super();

			setLayout(new MigLayout());
			setOpaque(false);
//			setBorder(BorderFactory.createTitledBorder("Tile Value"));

			setupDataTicker();
		}

		private void setupDataTicker() {
			
			add(textLabel, "w 120!");
			add(valTextArea, "wrap");

			revalidate();
			repaint();
		}
	}

	/**
	 * Updates the JTextArea which displays the value as a String.
	 * @param val - The value to be displayed as String.
	 */
	public void setValue(final double val) {
		
		String val_s = Double.toString(val);
		valTextArea.setText(val_s);
	}
	
	/**
	 * Updates the JTextArea which displays the String.
	 * @param text - The value to be displayed as String.
	 */
	public void setText(String text){
		textLabel.setText(text);
	}
	
	public void setVisibleMean(){
		if(isZoomed()){
			setZoomMeanDataTickerValue();
		}else{
			setMeanDataValue();
		}
	}
	
	public void setModel(DataModel dataModel){
		this.dataModel = dataModel;
	}
	
	public void setMaps(MapContainer xMap, MapContainer yMap){
		
		if(xMap != null){
			if(xmap != null) {
				xmap.deleteObserver(this);
			}
			this.xmap = xMap;
			xmap.addObserver(this);
		}else{
			LogBuffer.println("Warning: Please dont mess with the maps!");
		}
		
		if(xMap != null){
			if(ymap != null) {
				ymap.deleteObserver(this);
			}
			this.ymap = yMap;
			ymap.addObserver(this);
		}else{
			LogBuffer.println("Warning: Please dont mess with the maps!");
		}
	}
	
	/** 
	 * Set the data ticker to matrix average
	 */
	public void setMeanDataValue() {
		setText("Data Average:");
		setValue(dataModel.getDataMatrix().getMean());
	}
	
	/**
	 * Set the data ticker to Zoomed matrix average
	 */
	public void setZoomMeanDataTickerValue() {
		int startingRow = ymap.getFirstVisible();
		int endingRow = ymap.getLastVisible();
		int startingCol = xmap.getFirstVisible();
		int endingCol = xmap.getLastVisible();
		setText("Zoom Average:");
		setValue(dataModel.getDataMatrix().getZoomedMean(startingRow,endingRow,
			startingCol, endingCol));
	}

	/**
	 * Returns true if the visible area is a part of the matrix, 
	 * false if whole matrix is visible
	 * @author smd.faizan
	 * @return boolean
	 */
	private boolean isZoomed() {
	
		return(
			!(ymap.getMaxIndex()+1 ==
			ymap.getNumVisible() &&
			xmap.getMaxIndex()+1 ==
			xmap.getNumVisible()));
	}

	@Override
	public void update(Observable o, Object arg) {
		if(o == xmap || o == ymap ) {
			if(meanCalcTimer == null) {
				meanCalcTimer = new Timer(
					meanCalcDelay,
					meanCalcListener);
				meanCalcTimer.start();
			} else if(meanCalcTimer.isRunning()) {
				meanCalcTimer.restart();
			} else {
				meanCalcTimer = null;
			}
		}
		else {
			LogBuffer.println("Warning: Data Ticker got funny update!");
		}
	}

	ActionListener meanCalcListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() == meanCalcTimer) {
				meanCalcTimer.stop();
				meanCalcTimer = null;
				setVisibleMean();
			}
		}
	};
}
