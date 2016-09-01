/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
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

		for (int i = 0; i < num_threads; i++) {
			print_thread_info(out, threads[i], indent + " ");
		}
		for (int i = 0; i < num_groups; i++) {
			list_group(out, groups[i], indent + " ");
		}
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
