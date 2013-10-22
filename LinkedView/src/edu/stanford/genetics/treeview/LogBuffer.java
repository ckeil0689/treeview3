/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: LogBuffer.java,v $
 * $Revision: 1.2 $
 * $Date: 2009-09-04 13:34:37 $
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


import java.util.*;

public class LogBuffer extends Observable {
	
	private static LogBuffer singleton = new LogBuffer();

	private ConfigNode root = new DummyConfigNode("LogBuffer");
	private int defaultLog = 0; // false
	private Vector<String> buffer = new Vector<String>(100, 100);
	
	public void log(String msg) {
		
		if (getLog()) {
			append(msg);
		}
		
		if (getPrint()) {
			System.out.println(msg);
		}
	}
	
	private boolean getPrint() {
		if (root == null) {
			return true;
			
		} else {
			return (root.getAttribute("print", 1) == 1);
		}
	}
	
	/**
	 *
	 * @return true if messages are being logged in the buffer
	 */
	public boolean getLog() {
		
		return (root.getAttribute("log", defaultLog) == 1);
	}
	
	public void setLog(boolean bool) {
		
		System.err.println("Before " + getLog() );
		if (bool == getLog()) {
			return;
		}
		
		if (bool) { 
			root.setAttribute("log", 1, defaultLog);
			
		} else {
			root.setAttribute("log", 0, defaultLog);
		}
		
		setChanged();
		notifyObservers(null);
		System.err.println("After " + getLog() );
	}
	
	private void append(String msg) {
		
		buffer.add(msg);
		setChanged();
		notifyObservers(msg);
	}
	
	public Enumeration<String> getMessages() {
		
		return buffer.elements();
	}
	
	public static void logException(Exception e) {
		
		println(e.getMessage());
		StackTraceElement [] els =  e.getStackTrace();
		for (int i = 0; i < els.length; i++) {
			StackTraceElement el = els[i];
			println(" - " +el.toString());
		}
	}
	
	public static void println(String msg) {
		
		singleton.log(msg);
	}
	
	public static LogBuffer getSingleton() {
		
		return singleton;
	}
}

