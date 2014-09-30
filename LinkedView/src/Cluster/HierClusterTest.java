package Cluster;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import Utilities.StringRes;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.model.TVModel;
import edu.stanford.genetics.treeview.model.TVModel.TVDataMatrix;

public class HierClusterTest {

	private File file;
	
	public void test() {
		
		try {
			TestDistWorker tdw = new TestDistWorker();
			tdw.execute();
		
			double[][] distMatrix = tdw.get();
			
			TestClusWorker tcw = new TestClusWorker(distMatrix);
			tcw.execute();
			
			String[] reorderedRows = tcw.get();
			
			LogBuffer.println(Arrays.asList(reorderedRows).subList(0, 15)
					.toString());
			
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public class TestDistWorker extends SwingWorker<double[][], Integer> {

		@Override
		protected double[][] doInBackground() throws Exception {
			
			/* Get test distance matrix */
			TVModel model = new TVModel();
			file = new File("C:/Users/CKeil/Programming/Princeton/"
					+ "TreeView Related Files/Test Files/testdata_alpha.txt");
			FileSet fileSet = new FileSet(file.getName(), file.getParent()
					+ File.separator);
			try {
				model.loadNoGUI(fileSet);
			} catch (OutOfMemoryError | LoadException | InterruptedException
					| ExecutionException e) {
				
			}
			
			DistMatrixCalculator dCalc = new DistMatrixCalculator(
					((TVDataMatrix) model.getDataMatrix()).getExprData(), 
					StringRes.cluster_pearsonUn, 0, this);
			
			dCalc.measureDistance();
			
			return dCalc.getDistanceMatrix();
		}
	}
	
	public class TestClusWorker extends SwingWorker<String[], Integer> {

		private double[][] distM;
		
		public TestClusWorker(double[][] distM) {
			
			this.distM = distM;
		}
		
		@Override
		protected String[] doInBackground() {
			
			/* Cluster test matrix */
			HierCluster hc = new HierCluster(file.getName(),
					StringRes.cluster_link_Avg, distM, 0, this);
			
			hc.cluster();
			
			return hc.getReorderedList();
		}
	}
	
	public static void main(String[] args) {

		HierClusterTest hct = new HierClusterTest();
		hct.test();

	}

}
