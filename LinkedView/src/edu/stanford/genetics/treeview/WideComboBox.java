package edu.stanford.genetics.treeview;

import javax.swing.*; 

import edu.stanford.genetics.treeview.core.AutoComboBox;

import java.awt.*; 

// got this workaround from the following bug: 
//      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607 
public class WideComboBox extends AutoComboBox { 

	private static final long serialVersionUID = 1L;

	private boolean layingOut = false;
	
	public WideComboBox(final Object items[]){ 
        
		super(items); 
    } 

    public WideComboBox(ComboBoxModel aModel) { 
        
    	super(aModel); 
    } 

    public void doLayout(){ 
        try{ 
            layingOut = true; 
                super.doLayout(); 
        }finally{ 
            layingOut = false; 
        } 
    } 

    public Dimension getSize(){ 
       
    	Dimension dim = super.getSize(); 
        if(!layingOut) 
            dim.width = Math.max(dim.width, getPreferredSize().width); 
        return dim; 
    } 
}
