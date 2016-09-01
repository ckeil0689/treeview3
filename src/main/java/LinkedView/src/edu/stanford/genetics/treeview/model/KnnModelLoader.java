///* BEGIN_HEADER                                                   TreeView 3
// *
// * Please refer to our LICENSE file if you wish to make changes to this software
// *
// * END_HEADER 
// */

//
//package edu.stanford.genetics.treeview.model;
//
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//
//import edu.stanford.genetics.treeview.FileSet;
//import edu.stanford.genetics.treeview.LoadException;
//import edu.stanford.genetics.treeview.LogBuffer;
//import edu.stanford.genetics.treeview.XmlConfig;
//
///**
// * @author aloksaldanha
// * 
// */
//public class KnnModelLoader extends TVModelLoader2 {
//
//	/**
//	 * @param model
//	 */
//	public KnnModelLoader(final KnnModel model) {
//		super(model);
//		loadProgress.setPhases(new String[] { "Starting", "Loading CDT",
//				"Parsing CDT", "Parsing KGG", "Parsing KAG",
//				"Loading Document Config", "Finished" });
//	}
//
//	private KnnModel getKnnModel() {
//		// localize the cast
//		return (KnnModel) targetModel;
//	}
//
//	/**
//	 * This run() completely overrides the run() of TVModelLoader2.
//	 * 
//	 * @see edu.stanford.genetics.treeview.model.TVModelLoader2#run()
//	 */
//	@Override
//	protected void run() {
//		try {
//			final KnnModel model = getKnnModel();
//			final FileSet fileSet = targetModel.getFileSet();
//			setPhase(0);
//			model.gidFound(false);
//			model.aidFound(false);
//
//			setPhase(1);
//			println("loading " + fileSet.getCdt() + " ... ");
//			try {
//				parser.setResource(fileSet.getCdt());
//				parser.setProgressTrackable(this);
//				final RectData tempTable = parser.loadIntoTable();
//				setPhase(2);
//				parseCDT(tempTable);
//			} catch (final LoadException e) {
//				throw e;
//			} catch (final Exception e) {
//				// this should never happen!
//				LogBuffer
//						.println("TVModelLoader2.run() : while parsing cdt got error "
//								+ e.getMessage());
//				LogBuffer.println("TVModel instance " + targetModel.getType());
//				e.printStackTrace();
//				throw new LoadException("Error Parsing CDT: " + e,
//						LoadException.CDTPARSE);
//			}
//
//			final String kggfilename = fileSet.getKgg();
//			if (kggfilename != "") {
//				println("parsing kgg");
//				try {
//					parser.setResource(fileSet.getKgg());
//					parser.setProgressTrackable(this);
//					setPhase(3);
//					final RectData tempTable = parser.loadIntoTable();
//					model.setGClusters(tempTable, LoadException.KGGPARSE);
//				} catch (final Exception e) {
//					e.printStackTrace();
//					println("ignoring gene k-means clusters.");
//					setHadProblem(true);
//				}
//			}
//
//			final String kagfilename = fileSet.getKag();
//			if (kagfilename != "") {
//				println("parsing kag");
//				try {
//					parser.setResource(fileSet.getKag());
//					parser.setProgressTrackable(this);
//					setPhase(4);
//					final RectData tempTable = parser.loadIntoTable();
//					model.setAClusters(tempTable, LoadException.KAGPARSE);
//				} catch (final Exception e) {
//					println("error parsing KAG: " + e.getMessage());
//					e.printStackTrace();
//					println("ignoring array k-means clusters.");
//					setHadProblem(true);
//				}
//			}
//
//			setPhase(5);
//			try {
//				println("parsing jtv config file");
//				final XmlConfig documentConfig = new XmlConfig(targetModel
//						.getFileSet().getJtv(), "DocumentConfig");
//				targetModel.setDocumentConfig(documentConfig);
//			} catch (final Exception e) {
//				targetModel.setDocumentConfig(null);
//				println("Got exception " + e);
//				setHadProblem(true);
//			}
//
//			setPhase(6);
//			if (getException() == null) {
//				/*
//				 * if (!fileLoader.getCompleted()) { throw new
//				 * LoadException("Parse not Completed", LoadException.INTPARSE);
//				 * } //System.out.println("f had no exceptoin set");
//				 */
//			} else {
//				throw getException();
//			}
//			// ActionEvent(this, 0, "none",0);
//		} catch (final java.lang.OutOfMemoryError ex) {
//			final JPanel temp = new JPanel();
//			temp.add(new JLabel("Out of memory, allocate more RAM"));
//			temp.add(new JLabel(
//					"see Chapter 3 of Help->Documentation... for Out of Memory"));
//			JOptionPane.showMessageDialog(parent, temp);
//		} catch (final LoadException e) {
//			setException(e);
//			println("error parsing File: " + e.getMessage());
//			println("parse cannot succeed. please fix.");
//			setHadProblem(true);
//		}
//		setFinished(true);
//
//	}
//
//}
