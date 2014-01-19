package edu.stanford.genetics.treeview.core;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;

public class AutoComboBox extends JComboBox implements FocusListener{

	private static final long serialVersionUID = 1L;
	
	private boolean layingOut;
    private boolean isAutoPopupWidth = true;
    private int popupWidth;
    private String label;
    private MutableComboBoxModel model;

    public AutoComboBox(Vector<String> items) {
        
    	super(items);
    	this.label = items.get(0);
    	
    	model = (MutableComboBoxModel)getModel();
    }

    public AutoComboBox(ArrayList<String> items) {
        
    	this(items.toArray());
    	this.label = items.get(0);
    	
    	model = (MutableComboBoxModel)getModel();
    }
    
    public AutoComboBox(Object[] items) {
        
    	super(items);
    	this.label = (String)items[0];
    	
    	model = (MutableComboBoxModel)getModel();
    }

    public AutoComboBox(ComboBoxModel comboBoxModel) {
        
    	super(comboBoxModel);
    	
    	model = (MutableComboBoxModel)getModel();
    }

    /**
     * Overriden to handle the popup Size
     */
    @Override
    public void doLayout() {
       
    	try {
            layingOut = true;
            super.doLayout();
        }
        finally {
            layingOut = false;
        }
    }

    /**
     * Overriden to handle the popup Size
     */
    public Dimension getSize() {
       
    	Dimension dim = super.getSize();
        if (!layingOut) {
            if (isAutoPopupWidth) {
                popupWidth = getOptimumPopupWidth();
            }
            
            if (popupWidth != 0) {
                dim.width = popupWidth;
            }
        }
        return dim;
    }

    // return the greater of the combo width and the optimum popup width.
    private int getOptimumPopupWidth() {
        
    	return Math.max(super.getSize().width, calculateOptimumPopupWidth());
    }

    // Find the maximum item text width
    private int calculateOptimumPopupWidth() {
        
    	int width = 0;
    	int extension = 3;
    	
        FontMetrics fontMetrics = getFontMetrics(getFont());
        for (int i = 0; i < getItemCount(); i++) {
           
        	String text = getItemAt(i).toString();
            width = Math.max(width, fontMetrics.stringWidth(text));
        }
        // kludge to allow for BasicComboPopup insets (default 1 left + 1 right)
        // which are subtracted from the returned width 
        //(see BasicComboPopup.getPopupLocation()).
        return width + extension;
    }

    /**
     * Set the popup width to be the optimum (as for setOptimumPopupWidth) 
     * before displaying it.
     *
     * @param autoPopupWidth true to set the optimum width, 
     * false for the default behavior.
     */
    public void setAutoPopupWidth(boolean autoPopupWidth) {
        
    	isAutoPopupWidth = autoPopupWidth;
        if (!autoPopupWidth) {
            popupWidth = 0;
        }
    }

    /**
     * Indicates whether the popup width is automatically optimised 
     * before display.
     *
     * @return true if the popup width is automatically optimised, 
     * false if not.
     */
    public boolean isAutoPopupWidth() {
       
    	return isAutoPopupWidth;
    }

	@Override
	public void focusGained(FocusEvent arg0) {
		
		model.removeElement(0);
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		
		model.insertElementAt(label, 0);
	}
}
