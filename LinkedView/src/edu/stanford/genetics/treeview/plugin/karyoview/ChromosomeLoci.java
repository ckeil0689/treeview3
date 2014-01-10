/* BEGIN_HEADER                                              Java TreeView
 *
 * $Author: rqluk $
 * $RCSfile: ChromosomeLoci.java,v $
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

/**
 * this class encapsulates the position of things. No expression data or headers
 * or other such nonsense allowed.
 */
class ChromosomeLoci {

	/**
	 * Adds an element. Multiple elements with the same cdtIndex are not
	 * allowed, so don't do it.
	 */
	public void addLocus(final ChromosomeLocus l) {
		loci[l.getCdtIndex()] = l;
		setStructValid(false);
	}

	/**
	 * finds the actual position of the i'th element.
	 */
	public double getPosition(final int chromosome, final int arm,
			final int index) throws ArrayIndexOutOfBoundsException {
		if (isStructValid() == false)
			buildTree();
		final ChromosomeLocus temp = chromosomes[chromosome - 1].getLocus(arm,
				index);
		if (temp == null) {
			throw new ArrayIndexOutOfBoundsException();
		} else {
			return temp.getPosition();
		}
	}

	/**
	 * Given a location, performs a binary search to find the closest locus.
	 */
	public ChromosomeLocus getClosestLocus(final int chromosome, final int arm,
			final double position) {
		return chromosomes[chromosome - 1].getClosestLocus(arm, position);
	}

	public ChromosomeLocus getLocus(final int i) {
		return loci[i];
	}

	/**
	 * returns the maximum distance from centromere in Loci
	 */
	public double getMaxPosition(final int chromosome, final int arm) {
		if (isStructValid() == false)
			buildTree();
		return chromosomes[chromosome - 1].getMaxPosition(arm);
	}

	/**
	 * returns the maximum distance from centromere in Loci
	 */
	public double getMaxPosition() {
		if (isStructValid() == false)
			buildTree();
		double maxPos = -1.0;
		for (int i = 0; i < chromosomes.length; i++) {
			final double thisMax = chromosomes[i].getMaxPosition();
			if (thisMax > maxPos)
				maxPos = thisMax;
		}
		return maxPos;
	}

	/**
	 * returns largest chromosome number in loci.
	 */
	public int getMaxChromosome() {
		if (isStructValid() == false)
			buildTree();
		return chromosomes.length;
	}

	private final ChromosomeLocus[] loci;
	/**
	 * This is just an array of all the chromosomes...
	 */
	private Chromosome[] chromosomes;
	private boolean structValid;

	private boolean isStructValid() {
		return structValid;
	}

	private void setStructValid(final boolean b) {
		structValid = b;
	}

	/**
	 * usually know how many elements we'll need...
	 */
	ChromosomeLoci(final int n) {
		loci = new ChromosomeLocus[n];
		structValid = false;
	}

	/**
	 * internal method to build fast datastructure
	 */
	private void buildTree() {
		if (isStructValid() == true)
			return;

		allocateDataStructure();
		loadDataStructure();
		setStructValid(true);
	}

	/**
	 * loads loci into allocated data structure.
	 */
	private void loadDataStructure() {
		// going to insertion sort, since I'm lazy.
		for (int i = 0; i < loci.length; i++) {
			final ChromosomeLocus locus = loci[i];
			if (locus.getChromosome() > 0) {
				chromosomes[locus.getChromosome() - 1].insertLocus(locus);
			}
		}
	}

	/**
	 * this routine allocates the proper space for chromosomes. it must be
	 * called immediately before loadDataStructure();
	 */
	private void allocateDataStructure() {
		// find max chromosome...
		int maxChr = -1;
		for (int i = 0; i < loci.length; i++) {
			if (loci[i] == null)
				continue;
			if (loci[i].getChromosome() > maxChr)
				maxChr = loci[i].getChromosome();
		}

		// counts of arms
		final int[] leftArmCount = new int[maxChr];
		final int[] rightArmCount = new int[maxChr];
		final int[] circularArmCount = new int[maxChr];
		for (int i = 0; i < maxChr; i++) {
			leftArmCount[i] = 0;
			rightArmCount[i] = 0;
			circularArmCount[i] = 0;
		}
		for (int i = 0; i < loci.length; i++) {
			final int chr = loci[i].getChromosome();
			final int arm = loci[i].getArm();
			if (arm == ChromosomeLocus.LEFT)
				leftArmCount[chr - 1]++;
			if (arm == ChromosomeLocus.RIGHT)
				rightArmCount[chr - 1]++;
			if (arm == ChromosomeLocus.CIRCULAR)
				circularArmCount[chr - 1]++;
		}

		chromosomes = new Chromosome[maxChr];
		for (int i = 0; i < maxChr; i++) {
			if (circularArmCount[i] != 0) {
				chromosomes[i] = new CircularChromosome(circularArmCount[i]);
			} else {
				chromosomes[i] = new LinearChromosome(leftArmCount[i],
						rightArmCount[i]);
			}
		}
	} // end allocateDataStructure

}
