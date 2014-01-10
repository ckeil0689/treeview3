/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: CircularChromosome.java,v $
 * $Revision: 1.1 $
 * $Date: 2006-08-16 19:13:49 $
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
package edu.stanford.genetics.treeview.plugin.karyoview;

class CircularChromosome extends Chromosome {
	ChromosomeLocus[] circularArm;

	CircularChromosome(final int nCircular) {
		circularArm = new ChromosomeLocus[nCircular];
	}

	@Override
	public void insertLocus(final ChromosomeLocus locus) {
		if (locus.getArm() == ChromosomeLocus.CIRCULAR) {
			insertLocusIntoArray(circularArm, locus);
		}
	}

	@Override
	public double getMaxPosition() {
		return circularArm[circularArm.length - 1].getPosition();
	}

	@Override
	public double getMaxPosition(final int arm) {
		if (arm == ChromosomeLocus.CIRCULAR) {
			return getMaxPosition();
		}
		return 0.0;
	}

	@Override
	public int getType() {
		return Chromosome.CIRCULAR;
	}

	/**
	 * returns locus at 0 min
	 */
	@Override
	public ChromosomeLocus getLeftEnd() {
		return circularArm[0];
	}

	/**
	 * returns locus at latest min
	 */
	@Override
	public ChromosomeLocus getRightEnd() {
		return circularArm[circularArm.length - 1];
	}

	@Override
	public ChromosomeLocus getClosestLocus(final int arm, final double position) {
		if (arm == ChromosomeLocus.CIRCULAR) {
			return getLocusRecursive(position, circularArm, 0,
					circularArm.length - 1);
		}
		return null;
	}

	@Override
	public ChromosomeLocus getLocus(final int arm, final int index) {
		if (arm == ChromosomeLocus.CIRCULAR) {
			return circularArm[index];
		}
		return null;
	}
}
