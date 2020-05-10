/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */


package edu.stanford.genetics.treeview.model;

import controller.TVController;

/**
 * @author avsegal
 *
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AtrTVModelLoader extends ModelLoader { // TVModelLoader2 {

	/**
	 * @param targetModel
	 */
	public AtrTVModelLoader(final AtrTVModel targetModel,
			final DataLoadInfo dataStartCoords, final TVController controller) {
		super(targetModel, controller, dataStartCoords);
	}

	// @Override
	// protected void parseCDT(final RectData tempVector) throws LoadException {
	// super.findCdtDimensions(tempVector);
	// super.loadArrayAnnotation(tempVector);
	// super.loadGeneAnnotation(tempVector);
	// }

}
