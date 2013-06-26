/*
 * Created on Mar 2, 2009
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview;

public interface LoadProgress2I {

	public abstract void println(String s);

	/**
	 * sets value of phase progress bar
	 * @param i
	 */
	public abstract void setPhaseValue(int i);

	public abstract int getPhaseValue();

	/**
	 * sets length of phase progress bar
	 * @param i
	 */
	public abstract void setPhaseLength(int i);

	public abstract int getPhaseLength();

	/**
	 * sets test of phase bar
	 * @param i
	 */
	public abstract void setPhaseText(String i);

	public abstract void setButtonText(String text);

	/**
	 * makes progress bar determinate, sets length to particular value
	 * @param i
	 */
	public abstract void setLength(int i);

	public abstract int getLength();

	/**
	 * sets value of progress bar
	 * @param i
	 */
	public abstract void setValue(int i);

	public abstract int getValue();

	public abstract void incrValue(int i);

	/**
	 * sets determinate state of progress bar
	 * @param flag
	 */
	public abstract void setIndeterminate(boolean flag);

	/** Setter for canceled */
	public abstract void setCanceled(boolean canceled);

	/** Getter for canceled */
	public abstract boolean getCanceled();

	/** Setter for exception */
	public abstract void setException(LoadException exception);

	/** Getter for exception */
	public abstract LoadException getException();

	/** Setter for hadProblem */
	public abstract void setHadProblem(boolean hadProblem);

	/** Getter for hadProblem */
	public abstract boolean getHadProblem();

	/** Setter for finished */
	public abstract void setFinished(boolean finished);

	/** Getter for finished */
	public abstract boolean getFinished();

	public abstract String getPhaseText();

	public abstract void setPhase(int i);

	public abstract void setPhases(String[] strings);

	public abstract void setVisible(boolean b);

}