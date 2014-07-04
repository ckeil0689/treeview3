/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: alokito $
 * $RCSfile: KnnModelLoader.java,v $
 * $Revision: 1.13 $
 * $Date: 2008-06-11 01:58:58 $
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

package edu.stanford.genetics.treeview.model;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;

/**
 * @author aloksaldanha
 * 
 */
public class NewKnnModelLoader extends NewModelLoader {

	/**
	 * @param model
	 */
	public NewKnnModelLoader(final KnnModel model) {

		super(model);
	}

	private KnnModel getKnnModel() {
		// localize the cast
		return (KnnModel) targetModel;
	}

	/**
	 * This run() completely overrides the run() of TVModelLoader2.
	 * 
	 * @return
	 * 
	 * @see edu.stanford.genetics.treeview.model.TVModelLoader2#run()
	 */
	@Override
	public TVModel load() {

		try {
			final KnnModel model = getKnnModel();
			final FileSet fileSet = targetModel.getFileSet();

			model.gidFound(false);
			model.aidFound(false);

			// Read data from specified file location
			loadProgView.resetLoadBar();
			final int loadBarMax = count(new File(fileSet.getCdt()));
			loadProgView.setLoadBarMax(loadBarMax);

			loadProgView.setLoadLabel("Loading Data into TreeView.");

			FileInputStream fis = new FileInputStream(fileSet.getCdt());
			DataInputStream in = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			// Get data from file into String and double arrays
			// Put the arrays in ArrayLists for later access.
			System.out.println("Starting extract.");
			extractData(br);

			parseCDT();

			final String kggfilename = fileSet.getKgg();
			if (!kggfilename.equalsIgnoreCase("")) {
				try {
					fis = new FileInputStream(kggfilename);
					in = new DataInputStream(fis);
					br = new BufferedReader(new InputStreamReader(in));
					extractData(br);
					model.setGClusters(stringLabels, LoadException.KGGPARSE);

				} catch (final Exception e) {
					System.out.println("error parsing KGG: " + e.getCause());
					e.printStackTrace();
				}
			}

			final String kagfilename = fileSet.getKag();
			if (!kagfilename.equalsIgnoreCase("")) {
				try {
					fis = new FileInputStream(kagfilename);
					in = new DataInputStream(fis);
					br = new BufferedReader(new InputStreamReader(in));
					extractData(br);
					model.setAClusters(stringLabels, LoadException.KAGPARSE);

				} catch (final Exception e) {
					System.out.println("error parsing KAG: " + e.getCause());
					e.printStackTrace();
				}
			}

			try {
				// final Preferences documentConfig = new XmlConfig(targetModel
				// .getFileSet().getJtv(), "DocumentConfig");
				final Preferences documentConfig = Preferences.userRoot().node(
						"DocumentConfig");
				documentConfig.put("jtv", targetModel.getFileSet().getJtv());
				targetModel.setDocumentConfig(documentConfig);

			} catch (final Exception e) {
				targetModel.setDocumentConfig(null);
				LogBuffer.println("Exception in load() in "
						+ "NewKnnModelLoader: " + e.getMessage());
			}

			// ActionEvent(this, 0, "none",0);
		} catch (final java.lang.OutOfMemoryError ex) {
			LogBuffer.println("OutOfMemoryError in load() in "
					+ "NewKnnModelLoader: " + ex.getMessage());
			final JPanel temp = new JPanel();
			temp.add(new JLabel("Out of memory, allocate more RAM"));
			temp.add(new JLabel("see Chapter 3 of Help->Documentation... "
					+ "for Out of Memory"));
			JOptionPane.showMessageDialog(tvFrame.getAppFrame(), temp);

		} catch (final IOException e) {
			System.out.println("Loading resulted in an error. Cause: "
					+ e.getCause());
			e.printStackTrace();
		}

		targetModel.setLoaded(true);
		return targetModel;
	}

}
