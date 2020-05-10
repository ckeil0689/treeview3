/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */

package edu.stanford.genetics.treeview.reg;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import edu.stanford.genetics.treeview.BrowserControl;
import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.TreeViewApp;
import edu.stanford.genetics.treeview.model.LoadProgress;

/**
 * The purpose of this class is to allow registration of treeview
 * installations for purposes of grant writing.
 * 
 * Registration is by simple HTTP access.
 * 
 * This class works on ConfigNodes of type entry, which are children of the
 * Registration tag in the global xml configuration file. The recmommend way to
 * obtain these nodes is by creating a Registration object bound to the
 * Registration ConfigNode, and asking it for the confignode of the current
 * version.
 * 
 * @author aloksaldanha
 * 
 * 
 */
public class RegEngine {
	
	LoadProgress loadProgress;
	private final Entry regEntry;
	javax.swing.Timer loadTimer;
	/** url to send registration to. */
	private final static String regUrl = "http://jtreeview.sourceforge.net/reg/register.php";

	/**
	 * Build engine specifically to register indicated entry.
	 * 
	 * @param entry
	 *            - entry to register
	 */
	public RegEngine(final Entry entry) {
		this.regEntry = entry;
		loadTimer = new javax.swing.Timer(200, new TimerListener());
		loadTimer.stop();
	}

	/**
	 * 
	 * Verify that the given registration node is up-to-date for this version of
	 * treeview If not registered, and user has not yet declined to
	 * register, pop up dialog prompting user to register.
	 * 
	 * This is typically run at startup
	 * 
	 * 
	 * @param node
	 * 
	 *            Registration node, as of version 1.0, has multiple Entrys, one
	 *            for each version of treeview that has been registered.
	 * 
	 *            Each entry has the keys described in the Entry class, which
	 *            describe the relevant parameters of the registration (name,
	 *            email, treeview version, etc) as well as a special attribute,
	 *            status, which can be one of deferred, declined, pending,
	 *            complete.
	 * @throws Exception
	 * 
	 */
	public static void verify(final ConfigNode node) throws Exception {
		
		final Registration reg = new Registration(node);
		final String versionTag = TreeViewApp.getVersionTag();
		Entry entry = reg.getCurrentEntry();
		RegEngine engine = null;

		if ((entry == null) && (Entry.isNumericVersion(versionTag))) {
			// this version not registered yet, and has numeric version tag
			entry = reg.createEntry(versionTag);
			entry.initialize();
			engine = new RegEngine(entry);
			engine.suggestRegistration();
		} else if (entry.getStatus().equals("deferred")) {
			// deferred last time
			// overwrites all old node information.
			entry.initialize();
			engine = new RegEngine(entry);
			engine.suggestRegistration();
		} else {
			// could be pending registration from last time, so create engine
			// just
			// in case.
			engine = new RegEngine(entry);
		}
		if (entry.getStatus().equals("pending")) {
			engine.attemptRegistration();
		}
	}

	/**
	 * this is called in response to user action, and may indicate that the user
	 * wants to re-register for some reason. It should also display a summary of
	 * registration info so far.
	 * 
	 * @param node
	 * @throws Exception
	 */
	public static void reverify(final ConfigNode node) throws Exception {
		
		final Registration reg = new Registration(node);
		final String versionTag = TreeViewApp.getVersionTag();
		Entry entry = reg.getEntry(versionTag);

		if (entry == null) {
			// this version not registered yet
			entry = reg.createEntry(versionTag);
		}

		// this version alredy registered.
		if (JOptionPane.showConfirmDialog(null, new ExistingRegPanel(reg),
				"Current Registration", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			return;
		}

		entry.initialize();
		final RegEngine engine = new RegEngine(entry);
		engine.editEntry();
		if (entry.getStatus().equals("pending")) {
			engine.attemptRegistration();
		}
	}

	/**
	 * @throws Exception
	 */
	private void attemptRegistration() throws Exception {
		loadProgress = new LoadProgress("Registering Treeview...", null);
		final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				run();
				return null;
			}
		};
		// start up the worker thread
		worker.execute();
		loadTimer.start();

		// show a modal dialog, should block until loading done...
		loadProgress.setIndeterminate(true);
		loadProgress.pack();
		loadProgress.setVisible(true); // will be hidden by TimerListener
		// System.out.println("loadNew 6, ex: " + fileLoader.getException());

		if (getException() != null) {
			throw getException();
		}
	}

	private Exception exception;

	/**
	 * this method does the actual registering.
	 */
	protected void run() {
		// construct url
		String data = null;
		for (int i = 0; i < regEntry.getNumRegKeys(); i++) {
			final String key = regEntry.getRegKey(i);
			final String val = regEntry.getRegValue(i);
			try {
				if (data == null) {
					data = "";
				} else {
					data += "&";
				}
				data += URLEncoder.encode(key, "UTF-8") + "="
						+ URLEncoder.encode(val, "UTF-8");
			} catch (final Exception e) {
				loadProgress
						.println("Error formatting data, could not complete registration");
				loadProgress.println("key = " + key + ", value = " + val);
				loadProgress.println("exception " + e);
				loadProgress
						.println("if problems persist please alert treeview maintainers (jtreeview-users@lists.sourceforge.net");
				setException(e);
				setFinished(true);
				return;
			}
		}
		URL url;
		// Send data
		try {
			url = new URL(regUrl);
		} catch (final Exception e) {
			loadProgress
					.println("Error constructing URL, could not complete registration");
			loadProgress.println("url = " + regUrl);
			loadProgress.println("exception " + e);
			loadProgress
					.println("if problems persist please alert treeview maintainers (jtreeview-users@lists.sourceforge.net");
			setException(e);
			setFinished(true);
			return;
		}
		// make connection
		URLConnection conn;
		try {
			conn = url.openConnection();
		} catch (final Exception e) {
			loadProgress
					.println("Error opening connection, could not complete registration");
			loadProgress.println("exception " + e);
			loadProgress.println("please check network connection");
			loadProgress
					.println("if problems persist please alert treeview maintainers (jtreeview-users@lists.sourceforge.net");
			setException(e);
			setFinished(true);
			return;
		}

		OutputStreamWriter wr;
		try {
			conn.setDoOutput(true);
			wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(data);
			wr.flush();
		} catch (final Exception e) {
			loadProgress
					.println("Error sending data, could not complete registration");
			loadProgress.println("exception " + e);
			loadProgress.println("please check network connection");
			loadProgress
					.println("if problems persist please alert treeview maintainers (jtreeview-users@lists.sourceforge.net");
			setException(e);
			setFinished(true);
			return;
		}

		try {
			// Get the response
			final BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				// Process line...
				if (line.indexOf("FAILED") >= 0) {
					loadProgress.println(line);
					setComplete(false);
				} else if (line.indexOf("SUCCEEDED") >= 0) {
					setComplete(true);
				} else {
					// comment out for production use.
					loadProgress.println(line);

				}
			}
			wr.close();
			rd.close();
		} catch (final Exception e) {
			loadProgress
					.println("Error reading response, could not complete registration");
			loadProgress.println("exception " + e);
			loadProgress
					.println("if problems persist please alert treeview maintainers (jtreeview-users@lists.sourceforge.net");
			setException(e);
			setFinished(true);
			return;
		}
		if (isComplete()) {
			loadProgress.setButtonText("Close");
			loadProgress.clear();
			loadProgress.setValue(100);
			loadProgress.println("Registration Succeeded!!!\n\n");
			loadProgress.println("Thank you for your support.");
			regEntry.setStatus("complete");
		} else {
			loadProgress.setButtonText("Close");
			loadProgress.println("Registration failed. Must retry later");
		}
	}

	/**
	 * This method puts up a panel that allows the user to choose to register,
	 * to not register, or to defer.
	 * 
	 * If the user chooses to register, they get a chance to edit their
	 * registration.
	 * 
	 * Finaly, it sets the node's status attribute to either pending, declined
	 * or deferred.
	 */
	private void suggestRegistration() {
		final JTextArea message = new JTextArea();
		message.append("Please register Treeview.\n");
		message.append("It is critical for our funding that we track the\n");
		message.append("number of Treeview users.");
		// pop up dialog suggesting registration
		final String[] options = { "Register Now", "Register Later",
				"Do Not Register" };
		final int retval = JOptionPane.showOptionDialog(null, message,
				"JTreeview Registration", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		switch (retval) {
		case JOptionPane.YES_OPTION:
			editEntry(); // sets status too
			break;
		case JOptionPane.NO_OPTION:
			regEntry.setStatus("deferred");
			break;
		case JOptionPane.CANCEL_OPTION:
			regEntry.setStatus("declined");
			break;
		}
	}

	/**
	 * pop up registration editor Also, sets up actionlisteners that set status
	 * of entry depending on how editor is closed.
	 */
	private void editEntry() {
		final RegEditor regEditor = new RegEditor(regEntry);
		final JPanel holder = new JPanel();
		final JPanel buttons = new JPanel();
		final JButton cancelButton = new JButton("Cancel");
		final JButton okButton = new JButton("Ok");
		buttons.add(cancelButton);
		buttons.add(okButton);
		holder.setLayout(new BorderLayout());
		holder.add(regEditor, BorderLayout.CENTER);
		holder.add(buttons, BorderLayout.SOUTH);

		final JDialog dialog = new JDialog((JFrame) null,
				"Edit Registration Info", true);
		dialog.setContentPane(holder);
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				regEntry.setStatus("deferred");
				dialog.setVisible(false);
			}
		});
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				String err = "";
				for (int i = 0; i < regEntry.getNumEditableRegKeys(); i++) {
					final String key = regEntry.getEditableRegKey(i);
					regEntry.setEditableRegValue(i, regEditor.getAttribute(key));
					if (regEntry.getEditableRegValue(i).equals(""))
						err += "- enter " + key + "\n";
				}
				if (err.equals("")) {
					regEntry.setStatus("pending");
					dialog.setVisible(false);
				} else {
					JOptionPane.showMessageDialog(dialog, err);
				}
			}
		});
		dialog.pack();
		dialog.setVisible(true);
		if (regEntry.getRegValue(4).equals("N") == false) {
			final BrowserControl bc = BrowserControl.getBrowserControl();
			try {
				bc.displayURL(TreeViewApp.getAnnouncementUrl());
			} catch (final IOException e1) {
				JOptionPane.showMessageDialog(regEditor, "Could not open url "
						+ TreeViewApp.getAnnouncementUrl());
				e1.printStackTrace();
			}
		}
	}

	class TimerListener implements ActionListener { // manages the FileLoader
		// this method is invoked every few hundred ms
		@Override
		public void actionPerformed(final ActionEvent evt) {
			// loadProgress.setValue(getValue());
			if (loadProgress.getCanceled() || isFinished()) {
				setFinished(true);
				loadTimer.stop();
				if (isComplete() == true) {
					loadProgress.setVisible(false);
				} else {
					loadProgress.setButtonText("Dismiss");
					Toolkit.getDefaultToolkit().beep();
					loadProgress.getToolkit().beep();
				}
			}
		}
	}

	boolean finished;

	/**
	 * @return Returns the finished.
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * @param finished
	 *            The finished to set.
	 */
	public void setFinished(final boolean finished) {
		this.finished = finished;
	}

	/**
	 * if complete is false, we assume that registration is not yet complete.
	 */
	boolean complete = false;

	/**
	 * @return true if registration successful
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * @param hadProblem
	 *            The hadProblem to set.
	 */
	public void setComplete(final boolean hadProblem) {
		this.complete = hadProblem;
	}

	/**
	 * @return Returns the exception.
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * @param exception
	 *            The exception to set.
	 */
	public void setException(final Exception exception) {
		this.exception = exception;
	}

	/**
	 * Adds a declined entry for the current version of treeview to the
	 * registration node, to allow future calls to RegEngine.verify() to return
	 * without popping up windows.
	 * 
	 * @param registration
	 *            ConfigNode that stores Entry nodes from global config
	 */
	public static void addBogusComplete(final ConfigNode registration) {
		final Registration reg = new Registration(registration);
		final Entry entry = reg.getCurrentEntry();
		entry.setStatus("declined");
	}
}

/**
 * @author aloksaldanha
 * 
 *         displays summary of existing registrations
 * 
 * */
class ExistingRegPanel extends JPanel {
	private Registration reg = null;

	/**
	 * @author aloksaldanha
	 * 
	 *         Table that displays information about current registration state.
	 * 
	 */
	public class RegTableModel extends AbstractTableModel {

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {
			return reg.getNumEntries();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			// version,status,summary
			return 3;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			final Entry entry = reg.getEntry(rowIndex);
			switch (columnIndex) {
			case 0:
				return entry.getVersionTag();
			case 1:
				return entry.getStatus();
			case 2:
				return entry.getSummary();
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(final int column) {
			switch (column) {
			case 0:
				return "Version";
			case 1:
				return "Status";
			case 2:
				return "Summary";
			}
			return super.getColumnName(column);
		}
	}

	/**
	 * @param reg
	 */
	public ExistingRegPanel(final Registration reg) {
		this.reg = reg;
		setLayout(new BorderLayout());
		add(new JLabel("Registration History"), BorderLayout.NORTH);
		final JTable table = new JTable(new RegTableModel());
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// Set the third visible column to 100 pixels wide
		final int vColIndex = 2;
		final TableColumn col = table.getColumnModel().getColumn(vColIndex);
		final FontMetrics fontMetrics = getFontMetrics(getFont());
		final int length = fontMetrics.stringWidth(reg.getCurrentEntry()
				.getSummary());
		col.setPreferredWidth(length);

		add(new JScrollPane(table), BorderLayout.CENTER);
		add(new JLabel("Would you like to submit a registration?"),
				BorderLayout.SOUTH);
	}
}
