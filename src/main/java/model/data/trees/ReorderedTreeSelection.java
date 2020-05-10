package model.data.trees;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * represents a reordered version of the underlying TreeSelectionI
 *
 * @author Alok@caltech.edu
 *
 */
public class ReorderedTreeSelection extends Observable implements
		TreeSelectionI, Observer {

	private final TreeSelectionI parent;
	private final int[] reorderedIndex;
	private final Vector<Observer> observers = new Vector<Observer>();

	public ReorderedTreeSelection(final TreeSelectionI p, final int[] ri) {

		parent = p;
		reorderedIndex = ri;
		p.addObserver(this);
	}

	@Override
	public void resize(final int nIndex) {

		parent.resize(nIndex);
	}

	@Override
	public void deselectAllIndexes() {

		parent.deselectAllIndexes();
	}

	@Override
	public void selectAllIndexes() {

		parent.selectAllIndexes();
	}

	@Override
	public void setIndexSelection(int i, final boolean b) {
		if (i >= reorderedIndex.length) {
			i = reorderedIndex.length - 1;
		}

		if (i < 0) {
			i = 0;
		}

		final int index = reorderedIndex[i];
		if (index != -1) {
			parent.setIndexSelection(index, b);
		}
	}

	@Override
	public boolean isIndexSelected(final int i) {

		final int index = reorderedIndex[i];
		if (index != -1)
			return parent.isIndexSelected(index);

		return false;
	}

	@Override
	public int getMinIndex() {

		final int start = parent.getMinIndex();
		if (start == -1)
			return -1;

		for (int i = 0; i < reorderedIndex.length; i++) {

			if (reorderedIndex[i] == start)
				return i;
		}
		return -1;
	}

	@Override
	public int[] getSelectedIndexes() {

		return parent.getSelectedIndexes();
	}

	@Override
	public int getMaxIndex() {

		final int stop = parent.getMaxIndex();
		if (stop == -1)
			return -1;

		for (int i = reorderedIndex.length - 1; i >= 0; i--) {
			if (reorderedIndex[i] == stop)
				return i;
		}
		return -1;
	}

	/**
	 * Given a selected index, return the minimum selected index that is
	 * separated from the initial index be a contiguous series of 0 or more
	 * selected indexes
	 * @author rleach
	 * @param i
	 * @return int
	 */
	public int getMinContiguousIndex(final int i) {

		final int stop = parent.getMinContiguousIndex(i);
		if(stop == -1) {
			return -1;
		}

		for(int j = reorderedIndex.length - 1;j >= 0;j--) {
			if(reorderedIndex[j] == stop) {
				return j;
			}
		}
		return(-1);
	}

	/**
	 * Given a selected index, return the minimum selected index that is
	 * separated from the initial index be a contiguous series of 0 or more
	 * selected indexes
	 * @author rleach
	 * @param i
	 * @return int
	 */
	public int getMaxContiguousIndex(final int i) {

		final int stop = parent.getMinContiguousIndex(i);
		if(stop == -1) {
			return -1;
		}

		for(int j = reorderedIndex.length - 1;j >= 0;j--) {
			if(reorderedIndex[j] == stop) {
				return j;
			}
		}
		return(-1);
	}

	@Override
	public int getNumIndexes() {

		return reorderedIndex.length;
	}

	@Override
	public void selectNewIndexRange(int min, int max) {

		while ((reorderedIndex[min] == -1) && (min < reorderedIndex.length)) {
			min++;
		}

		while ((reorderedIndex[max] == -1) && (max > 0)) {
			max--;
		}

		if ((max >= 0) && (min <= reorderedIndex.length)) {
			parent.selectNewIndexRange(reorderedIndex[min], reorderedIndex[max]);
		}
	}

	@Override
	public void selectIndexRange(int min, int max) {

		while((reorderedIndex[min] == -1) && (min < reorderedIndex.length)) {
			min++;
		}

		while((reorderedIndex[max] == -1) && (max > 0)) {
			max--;
		}

		if((max >= 0) && (min <= reorderedIndex.length)) {
			parent.selectIndexRange(reorderedIndex[min],
				reorderedIndex[max]);
		}
	}

	@Override
	public int getNSelectedIndexes() {

		return parent.getNSelectedIndexes();
	}

	@Override
	public void setSelectedNode(final String n) {

		parent.setSelectedNode(n);
	}

	@Override
	public String getSelectedNode() {

		return parent.getSelectedNode();
	}

	@Override
	public void notifyObservers() {

		parent.notifyObservers();
	}

	@Override
	public void update(final Observable arg0, final Object arg1) {

		final Enumeration<Observer> e = observers.elements();
		while (e.hasMoreElements()) {
			e.nextElement().update(this, arg1);
		}
	}

	@Override
	public void addObserver(final Observer view) {

		observers.addElement(view);
	}

	@Override
	public void deleteObserver(final Observer view) {
		observers.remove(view);
	}

	@Override
	public int getFullSelectionRange() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasSelection() {
		
		return getNSelectedIndexes() > 0;
	}

}
