package gui.general;

import cmd.MemoryOpts;
import util.LogBuffer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutOfMemoryDialogController {

	private final OutOfMemoryDialog outOfMemoryDialog;
	private String xmxval;

	public OutOfMemoryDialogController(final OutOfMemoryDialog oomD) {

		this.outOfMemoryDialog = oomD;

		addListeners();

		try {
			outOfMemoryDialog.setVisible(true);
		} catch(Exception e) {
			LogBuffer.println("Possible memory exception during " +
				"oomDialog.setVisible(true).");
			e.printStackTrace();
			throw e;
		}
	
		LogBuffer.println("OomDialogController ready");
	}

	/**
	 * Adds listeners to the GUI components in OomDialog.
	 */
	private void addListeners() {
		outOfMemoryDialog.addRestartListener(new RestartListener());
		outOfMemoryDialog.addMemoryListener(new MemoryListener());
	}

	/**
	 * Describes the actions used to restart app.TreeView3 with more memory.
	 */
	private class RestartListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			if(arg0.getSource() != outOfMemoryDialog.getRestartButton()) {
				return;
			}

			MemoryOpts selMem = outOfMemoryDialog.getSelectedMemoryOpt();
			if(selMem != null) {
				xmxval = selMem.toJVMFlag();
			} else {
				xmxval = MemoryOpts.getDefault().toJVMFlag();
			}

			//Now restart app.TreeView3
			try {
				//The following is based on:
				//https://dzone.com/articles/programmatically-restart-java
				List<String> args =
					ManagementFactory.getRuntimeMXBean().getInputArguments();
				String args_str = "";
				Pattern p = Pattern.compile("^-Xmx");
				for(String arg : args) {
					Matcher m = p.matcher(arg);
					if(!m.find()) {
						args_str += " " + arg;
					}
				}
				String[] mainCommand =
					System.getProperty("sun.java.command").split(" ");
				String jarFile = "";
				String pattern = "\\.(?i:jar)$";
				p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
				for(String possiblejar : mainCommand) {
					Matcher m = p.matcher(possiblejar);
					if(m.find()) {
						jarFile = possiblejar;
					}
				}

				String restart_command = "java " + xmxval + args_str +
					" -jar " + jarFile;
				LogBuffer.println("Restarting with more memory: " +
					restart_command);
				Runtime.getRuntime().exec(restart_command);
				//sleep 3 seconds to allow the command time to work
				Thread.sleep(3000);
				System.exit(0);
			}
			catch(Exception iae) {
				LogBuffer.logException(iae);
//				showWarning(iae.getLocalizedMessage());
			}
		}
	}

	private class MemoryListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {

			MemoryOpts selMem = outOfMemoryDialog.getSelectedMemoryOpt();
			if(selMem != null) {
				xmxval = selMem.toJVMFlag();
			} else {
				xmxval = MemoryOpts.getDefault().toJVMFlag();
			}
		}
	}
}
