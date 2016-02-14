package edu.stanford.genetics.treeview.plugin.dendroview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import Controllers.TVController;
import Utilities.StringRes;
import edu.stanford.genetics.treeview.CopyType;

public class LabelContextMenuController {

	private boolean isRows;
	private LabelContextMenu lCMenu;
	private TVController tvController;

	public LabelContextMenuController(LabelContextMenu lCMenu,
			TVController tvController, boolean isRows) {

		this.isRows = isRows;
		this.lCMenu = lCMenu;
		this.tvController = tvController;

		addAllListeners();
	}

	private void addAllListeners() {

		lCMenu.addStyleListener(new StyleMenuListener());
		lCMenu.addCopyAllListener(new CopyAllListener());
		lCMenu.addCopySelectedListener(new CopySelectedListener());
	}

	/**
	 * Listener which is used to open the label style editor.
	 * 
	 * @author chris0689
	 *
	 */
	private class StyleMenuListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			tvController.openPrefMenu(StringRes.menu_RowAndCol);
		}
	}
	
	private class CopyAllListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {

			tvController.copyLabels(CopyType.ALL, isRows);
		}
	}
	
	private class CopySelectedListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {

			tvController.copyLabels(CopyType.SELECTION, isRows);
		}
	}
}
