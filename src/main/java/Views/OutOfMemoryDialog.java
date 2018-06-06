package Views;

import java.awt.event.ActionListener;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MemoryOpts;
import Utilities.CustomDialog;
import Utilities.GUIFactory;
import Utilities.StringRes;

public class OutOfMemoryDialog extends CustomDialog {

	private static final long serialVersionUID = 1L;

	private JComboBox<MemoryOpts> memoryOpts;
	private JButton restartBtn;
	private String suggestions;
	private String error;
	private MemoryOpts selmem;
	private String trymore;

	public OutOfMemoryDialog(final String suggestions)
		throws ExportException {

		super("Out of Memory");

		this.suggestions = suggestions;
		//The URL for instructions on how to increase memory will be after this:
		this.error =
			"<B>ERROR: TreeView3 has reached its maximum allotted memory.</B>" +
			"<BR><BR>\nIf this happens frequently, and you have more memory " +
			"available on your<BR>\nsystem, you may be able to increase the " +
			"default amount of memory java<BR>\ngives to TreeView3 every " +
			"time it starts.  See these instructions on how to<BR>\nincrease " +
			"the memory given to TreeView3 for your system:";
		//Note, the line wrap length is shorter for this one to accommodate the
		//drop-down list
		this.trymore = "<BR><B>RESTART:</B><BR><BR>\nYou can try restarting " +
			"just this once with more<BR>\nmemory (though note you may lose " +
			"unsaved changes<BR>\nand will have to re-open the current file)." +
			"  Select<BR>\nthe amount of memory you would like to " +
			"restart<BR>\nwith.  All available memory options provided<BR>\n" +
			"are larger than the current java memory limit.";
		this.selmem = MemoryOpts.getDefault();

		setupLayout();

		LogBuffer.println("OutOfMemoryError ready.");
	}

	@Override
	protected void setupLayout() {

		//The following is based on:
		//https://dzone.com/articles/programmatically-restart-java
		//Add the command that will be executed to restart to the dialog
		List<String> args =
			ManagementFactory.getRuntimeMXBean().getInputArguments();
		String args_str = "";
		Pattern p = Pattern.compile("^-Xmx");
		String prev_xmx = "";
		for(String arg : args) {
			Matcher m = p.matcher(arg);
			if(m.find()) {
				prev_xmx = arg;
			} else {
				args_str += " " + arg;
			}
		}
		//Got args and jar file separately because the below does not always
		//return all args on all systems.  Note, the below might have to be
		//altered if the jar file isn't always the first argument.
		String[] mainCommand =
			System.getProperty("sun.java.command").split(" ");
		String jarFile = "";
		String pattern = "\\.(?i:jar)$";
		p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		boolean canRestart = false;
		for(String possiblejar : mainCommand) {
			Matcher m = p.matcher(possiblejar);
			if(m.find()) {
				canRestart = true;
				jarFile = possiblejar;
			}
		}

		this.mainPanel = GUIFactory.createJPanel(false, GUIFactory.DEFAULT);

		JPanel contentPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);
		JPanel optionsPanel = GUIFactory.createJPanel(false, 
				GUIFactory.DEFAULT);

		JLabel msgLabel =
			GUIFactory.createLabel("<HTML>" + this.error + "</HTML>",
				GUIFactory.FONTS);
		optionsPanel.add(msgLabel,"growy, span, wrap");
		optionsPanel.add(GUIFactory.getHyperlinkButton(StringRes.memoryUrl),
			"span, wrap");

		JLabel memLabel;
		long precisionkb = 25000;
		MemoryOpts[] avail = MemoryOpts.getAvailable(precisionkb,prev_xmx);
		selmem = MemoryOpts.getDefault();
		if(avail.length > 0 && canRestart) {
			memLabel =
				GUIFactory.createLabel("<HTML>" + this.trymore + "</HTML>",
					GUIFactory.FONTS);
			memoryOpts = new JComboBox<MemoryOpts>(avail);
			selmem = MemoryOpts.getNextOptUp(precisionkb,prev_xmx);
			if(selmem == null) {
				selmem = MemoryOpts.getDefault();
			}
			memoryOpts.setSelectedItem(selmem);
			optionsPanel.add(memLabel, "growy");
			optionsPanel.add(memoryOpts, "growx, wrap");

			String shortname = jarFile.replaceAll(".*\\/","");
			String xmx_example = selmem.toJVMFlag();
			xmx_example = xmx_example.replaceAll("-Xmx","-Xmx<B>");
			xmx_example += "</B>";
			String restart_command = "java " + xmx_example + args_str +
				" -jar " + shortname;
			String javacmd = "<I>Restarting will use a command similar to:" +
				"<BR>\n" + restart_command + "</I>";
			JLabel cmdLabel = GUIFactory.createLabel("<HTML>" + javacmd +
				"</HTML>",GUIFactory.FONTS);
			optionsPanel.add(cmdLabel, "span, growy, wrap");
		} else {
			String msg = "<I>(Note, unable to provide auto-restart as an " +
				"option: " + (avail.length <= 0 ? "Memory maxed out" :
				"Not started from jar file") + ".)</I>";
			memLabel = GUIFactory.createLabel("<HTML>" + msg + "</HTML>",
				GUIFactory.FONTS);
			MemoryOpts[] none = {};
			memoryOpts = new JComboBox<MemoryOpts>(none);
			selmem = null;
			memoryOpts.setSelectedItem(null);
			optionsPanel.add(memLabel,"span, growy, wrap");
		}

		if(this.suggestions != "") {
			String sugintro = "<BR><B>CONTINUE:</B><BR><BR>\nYou can try " +
				"continuing and following one of the following suggestions " +
				"to work<BR>\naround the current memory limit:";
			JLabel introLabel =
				GUIFactory.createLabel("<HTML>" + sugintro + "</HTML>",
					GUIFactory.FONTS);
			JLabel altLabel =
				GUIFactory.createLabel("<HTML>" + this.suggestions + "</HTML>",
					GUIFactory.FONTS);
			optionsPanel.add(introLabel, "span, growy, wrap");
			optionsPanel.add(altLabel, "span, growy, wrap");
		}

		contentPanel.add(optionsPanel, "aligny 0%, growy, growx, push");

		this.restartBtn = GUIFactory.createBtn("Restart");
		closeBtn.setText("Continue");

		JPanel btnPanel = GUIFactory.createJPanel(false, GUIFactory.NO_INSETS);
		btnPanel.add(closeBtn, "tag cancel, pushx, al right");
		if(canRestart) {
			btnPanel.add(restartBtn, "al right");
		}

		mainPanel.add(contentPanel, "push, grow, wrap, hmax 500");
		mainPanel.add(btnPanel, "bottom, pushx, growx, span");

		getContentPane().add(mainPanel);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	/**
	 * Add restart action to restart button. Determines what happens, when
	 * the restart button is clicked.
	 * @param l The ActionListener
	 */
	public void addRestartListener(final ActionListener l) {
		restartBtn.addActionListener(l);
	}

	/**
	 * An item listener that allows us to fire events upon selection changes
	 * in the components that it has been added to.
	 * @param l - The item listener.
	 */
	public void addMemoryListener(final ActionListener l) {
		memoryOpts.addActionListener(l);
	}

	public Object getMemoryList() {
		return(memoryOpts);
	}

	public MemoryOpts getSelectedMemoryOpt() {
		MemoryOpts selected = (MemoryOpts) memoryOpts.getSelectedItem();
		if(selected == null) {
			selected = selmem;
		}
		return(selected);
	}

	/**
	 * Returns the restart button object so we can tell if actions came from it.
	 * @return
	 */
	public Object getRestartButton() {
		return(restartBtn);
	}
}
