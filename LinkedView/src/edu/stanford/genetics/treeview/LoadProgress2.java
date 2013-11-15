/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: LoadProgress2.java,v $
 * $Revision: 1.5 $
 * $Date: 2010-05-02 13:49:52 $
 * $Name:  $
 *
 * This file is part of Java TreeView
 * Copyright (C) 2001-2003 Alok Saldanha, All Rights Reserved. Modified by Alex Segal 2004/08/13. Modifications Copyright (C) Lawrence Berkeley Lab.
 *
 * This software is provided under the GNU GPL Version 2. In particular, 
 *
 * 1) If you modify a source file, make a comment in it containing your name and the date.
 * 2) If you distribute a modified version, you must do it under the GPL 2.
 * 3) Developers are encouraged but not required to notify the Java TreeView maintainers at alok@genome.stanford.edu when they make a useful addition. It would be nice if significant contributions could be merged into the main distribution.
 *
 * A full copy of the license can be found in gpl.txt or online at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * END_HEADER
 */
package edu.stanford.genetics.treeview;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;

import net.miginfocom.swing.MigLayout;

/**
 * This is like the original loadProgress, but it adds a setPhaseValue(int), 
 * setPhaseLength(int) and setPhaseText(String);
 * 
 * Typical use will involve three threads:
 * 
 * - a worker thread, that calls routines such as incrValue() asynchronously
 * - the Swing thread, which is the ever-present thread that services the 
 *    GUI components.
 * 
 * All routines that set values must be synchronized. This is to keep
 * the special "increment" routines behaving properly. In principle, I 
 * should also synchronize the get routines, but it's okay if things read stale
 * values now and then.
 * 
 */
public class LoadProgress2 extends JDialog implements LoadProgress2I {

	private static final long serialVersionUID = 1L;
	
	private final static Color BLUE1 = new Color(118, 193, 228, 255);
	
	/** set when we encounter a problem in parsing? */
	private boolean hadProblem = false;
	
	/** set when loading has been cancelled */
	private boolean cancelled;
	
	/** We hold fatal exceptions, for access by a reporting thread */
	LoadException exception = null;
	
	/**this is set when thread is finished, either 
	 * - had problem
	 * - cancelled
	 * - task completed (default assumption)
	 */
	boolean finished = false;  
	
	private JProgressBar phaseBar;
	private JProgressBar progressBar;
	private JTextArea taskOutput;
	private String newline = "\n";
	private JButton closeButton;
	private boolean indeterminate;
	private String[] phases;
	
	/**
	 * Main constructor
	 * @param title
	 * @param f
	 */
	public LoadProgress2(String title, Frame f) {
		
		super(f, title, true);
		phaseBar = setPBarLayout(phaseBar);

		progressBar = setPBarLayout(progressBar);

		taskOutput = new JTextArea(10, 40);
		taskOutput.setMargin(new Insets(5,5,5,5));
		taskOutput.setEditable(false);

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.setOpaque(false);
		panel.add(phaseBar, "span, pushx, growx, wrap");
		panel.add(progressBar, "span, pushx, growx");

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new MigLayout());
		contentPane.add(panel, "span, pushx, growx, wrap");
		contentPane.add(new JScrollPane(taskOutput), "push, grow, wrap");
		contentPane.setBackground(Color.white);
		
		closeButton = new JButton("Cancel");
		closeButton = setButtonLayout(closeButton);
		closeButton.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				setCanceled(true);
				LoadProgress2.this.dispose();
			}
		});
		
		panel = new JPanel(new MigLayout());
		panel.setOpaque(false);
		
		panel.add(closeButton);
		contentPane.add(panel, "alignx 50%, span");

		contentPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		setContentPane(contentPane);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I
	 * #println(java.lang.String)
	 */
	@Override
	public void println(String s) {
		
		taskOutput.append(s + newline);
		taskOutput.setCaretPosition
		(taskOutput.getDocument().getLength());
	}

	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setPhaseValue(int)
	 */
	@Override
	public synchronized void setPhaseValue(int i) {
		
		phaseBar.setValue(i);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getPhaseValue()
	 */
	@Override
	public int getPhaseValue() {
		
		return phaseBar.getValue();
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setPhaseLength(int)
	 */
	@Override
	public void setPhaseLength(int i) {
		
		phaseBar.setMinimum(0);
		phaseBar.setMaximum(i);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getPhaseLength()
	 */
	@Override
	public int getPhaseLength() {
		
		return phaseBar.getMaximum();
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I
	 * #setPhaseText(java.lang.String)
	 */
	@Override
	public void setPhaseText(String i) {
		
		phaseBar.setString(i);
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I
	 * #setButtonText(java.lang.String)
	 */
	@Override
	public void setButtonText(String text) {
		
		closeButton.setText(text);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setLength(int)
	 */
	@Override
	public void setLength(int i) {
		
		if (i < 0) {
			setIndeterminate(true);
			
		} else {
			setIndeterminate(false);
			if (progressBar.getMaximum() != i) {
				progressBar.setMinimum(0);
				progressBar.setMaximum(i);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getLength()
	 */
	@Override
	public int getLength() {
		
		if (indeterminate) {
			return -1;
			
		} else {
			return progressBar.getMaximum();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setValue(int)
	 */
	@Override
	public synchronized void setValue(int i) {
		
		progressBar.setValue(i);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getValue()
	 */
	@Override
	public int getValue() {
		
		return progressBar.getValue();
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#incrValue(int)
	 */
	@Override
	public synchronized void incrValue(int i) {
		
		setValue(getValue() + i);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I
	 * #setIndeterminate(boolean)
	 */
	@Override
	public synchronized void setIndeterminate(boolean flag) {
		
		// actually, this only works in jdk 1.4 and up...
		progressBar.setIndeterminate(flag);
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setCanceled(boolean)
	 */
	@Override
	public synchronized  void setCanceled(boolean canceled) {
		
		this.cancelled = canceled;
		setButtonText("Waiting...");
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getCanceled()
	 */
	@Override
	public boolean getCanceled() {
		
		return cancelled;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I
	 * #setException(edu.stanford.genetics.treeview.LoadException)
	 */
	@Override
	public synchronized void setException(LoadException exception) {
		
		this.exception = exception;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getException()
	 */
	@Override
	public LoadException getException() {
		
		return exception;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setHadProblem(boolean)
	 */
	@Override
	public synchronized void setHadProblem(boolean hadProblem) {
		this.hadProblem = hadProblem;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getHadProblem()
	 */
	@Override
	public boolean getHadProblem() {
		
		return hadProblem;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setFinished(boolean)
	 */
	@Override
	public synchronized void setFinished(boolean finished) {
		
		this.finished = finished;
		if (getHadProblem() == false) { 
			// let the host timer decide when to hide us.
			// setVisible(false);
		} else {
			setButtonText("Dismiss");
			Toolkit.getDefaultToolkit().beep();
			getToolkit().beep();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getFinished()
	 */
	@Override
	public boolean getFinished() {
		
		return finished;
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#getPhaseText()
	 */
	@Override
	public String getPhaseText() {
		
		return phaseBar.getString();
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setPhase(int)
	 */
	@Override
	public void setPhase(int i) {
		
		setPhaseValue(i+1);
		setPhaseText(phases[i]);
	}
	
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I
	 * #setPhases(java.lang.String[])
	 */
	@Override
	public synchronized void setPhases(String[] strings) {
	
		phases = strings;
		setPhaseLength(phases.length);
	}


	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.LoadProgress2I#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean b) {
		
		try {
			super.setVisible(b);
			
		} catch (java.lang.ArrayIndexOutOfBoundsException ex) {
			// this exception is thrown on first load on 64 bit linux.
			System.out.println("Caught ArrayIndexOutOfBoundsException");
			
	} catch (java.lang.IndexOutOfBoundsException ex) {
		// this exception is thrown on first load on 64 bit linux.
		System.out.println("Caught IndexOutOfBoundsException");
	}
		if (b == false) {
			finished = true;
		}
	}
	
	
	//Layout setups for some Swing elements
	/**
	 * Setting up a general layout for a button object
	 * The method is used to make all buttons appear consistent in aesthetics
	 * @param button
	 * @return
	 */
	public static JButton setButtonLayout(JButton button){
		
		Font buttonFont = new Font("Sans Serif", Font.PLAIN, 16);
		
  		Dimension d = button.getPreferredSize();
  		d.setSize(d.getWidth(), d.getHeight());
  		button.setPreferredSize(d);
  		
  		button.setFont(buttonFont);
  		button.setOpaque(true);
  		button.setBackground(BLUE1);
  		button.setForeground(Color.white);
  		
  		return button;
	}
	
	/**
	 * Method to setup a JProgressBar
	 * @param pBar
	 * @param text
	 * @return
	 */
	public JProgressBar setPBarLayout(JProgressBar pBar){
		
		final Dimension d = new Dimension(2000, 40);
		
		pBar = new JProgressBar();
		pBar.setMinimum(0);
		pBar.setStringPainted(true);
		pBar.setMaximumSize(d);
		pBar.setForeground(BLUE1);
		pBar.setUI(new BasicProgressBarUI(){
			@Override
			protected Color getSelectionBackground(){return Color.black;};
			@Override
			protected Color getSelectionForeground(){return Color.white;};
		});
		pBar.setVisible(true);
		
		return pBar;
	}
}
