/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: ThreadListener.java,v $
 * $Revision: 1.4 $
 * $Date: 2010-05-02 13:34:53 $
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

/* Call static "List all threads" to list all running threads. Stolen
 from O'Reilly's Java in a nutshell first edition. alok@genome. */

/* I added a constuctor which will pop up a window which monitors
 running threads */

import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ThreadListener extends Thread {
	boolean runin = true; // instance variable to tell if we are done...
	Frame top; // frame to hold thread monitor
	TextArea textarea;

	// Display info about a thread
	private static void print_thread_info(final PrintStream out,
			final Thread t, final String indent) {
		if (t == null)
			return;
		out.println(indent + "Thread: " + t.getName() + " Priority: "
				+ t.getPriority() + (t.isDaemon() ? "Daemon" : "Not Daemon")
				+ (t.isAlive() ? " Alive" : " Not Alive"));
	}

	// Display info about a thread group and its threads and groups
	private static void list_group(final PrintStream out, final ThreadGroup g,
			final String indent) {
		if (g == null)
			return;
		final int num_threads = g.activeCount();
		final int num_groups = g.activeGroupCount();
		final Thread threads[] = new Thread[num_threads];
		final ThreadGroup groups[] = new ThreadGroup[num_groups];
		g.enumerate(threads, false);
		g.enumerate(groups, false);

		out.println(indent + "Thread Group: " + g.getName() + " Max Priority "
				+ g.getMaxPriority()
				+ (g.isDaemon() ? " Daemon" : " Not Daemon"));

		for (int i = 0; i < num_threads; i++)
			print_thread_info(out, threads[i], indent + " ");
		for (int i = 0; i < num_groups; i++)
			list_group(out, groups[i], indent + " ");
	}

	// find root thread and list recursively
	public static void listAllThreads(final PrintStream out) {
		ThreadGroup current_thread_group;
		ThreadGroup root_thread_group;
		ThreadGroup parent;

		// Get the current thread group
		current_thread_group = Thread.currentThread().getThreadGroup();
		// now, go find root thread group
		root_thread_group = current_thread_group;
		parent = root_thread_group.getParent();
		while (parent != null) {
			root_thread_group = parent;
			parent = parent.getParent();
		}
		// list recursively
		list_group(out, root_thread_group, "");
	}

	@Override
	public synchronized void run() {
		while (runin == true) {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final PrintStream ps = new PrintStream(os);
			listAllThreads(ps);
			textarea.setText(os.toString());
			textarea.validate();
			textarea.repaint();
			try {
				this.wait(1000);
			} catch (final InterruptedException e) {
				// catches InterruptedException
				System.out.println("Somebody set us up the bomb!");
			}
		}
	}

	public void finish() {
		runin = false;
	}

	public ThreadListener() {
		top = new Frame();
		textarea = new TextArea(20, 100);
		top.add(textarea);
		top.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				runin = false;
				top.dispose();
			}
		});

		top.pack();
		top.setVisible(true);
	}
}
