package Views;

import javax.swing.JLabel;
import javax.swing.JTable;

import edu.stanford.genetics.treeview.FileSet;
import Utilities.CustomDialog;
import Utilities.GUIFactory;

public class LoadPreviewDialog extends CustomDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final FileSet fileSet;
	private JTable dataTable;
	
	public LoadPreviewDialog(String title, FileSet fileSet) {
		
		super(title);
		
		this.fileSet = fileSet;
		
		setupLayout();
	}
	
	private void setupLayout() {
		
		String previewText = "Inspect this data preview:";
		JLabel firstLine = GUIFactory.createLabel(previewText, GUIFactory.FONTS);
		
		mainPanel.add(firstLine, "span, wrap");
		setVisible(true);
	}
}
