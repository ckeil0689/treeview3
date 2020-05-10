/* BEGIN_HEADER                                                   TreeView 3
 *
 * Please refer to our LICENSE file if you wish to make changes to this software
 *
 * END_HEADER 
 */
package edu.stanford.genetics.treeview;

// NatField: custom Java component: text field that constrains input
// to be numeric.
// Copyright (C) Lemma 1 Ltd. 1997
// Author: Rob Arthan; rda@lemma-one.com

// This program is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free Software
// Foundation; either version 2 of the License, or (at your option) any later
// version.

// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License for more details.
// The GNU General Public License can be obtained at URL:
// http://www.lemma-one.com/scrapbook/gpl.html
// or by writing to the Free Software Foundation, Inc., 59 Temple Place,
// Suite 330, Boston, MA 02111-1307 USA.

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

public class NatField extends JTextField implements KeyListener {

	private static final long serialVersionUID = 1L;

	private int val;
	private int max = -1;

	public NatField(final int num, final int cols) {

		super(Integer.toString(num < 0 ? 0 : num), cols);
		val = num < 0 ? 0 : num;
		addKeyListener(this);
	}

	public NatField(final int num, final int cols, final int maximum) {

		super(Integer.toString(num < 0 ? 0 : num), cols);
		val = num < 0 ? 0 : num;
		max = maximum;
		addKeyListener(this);
	}

	@Override
	public void keyPressed(final KeyEvent evt) {
	}

	@Override
	public void keyReleased(final KeyEvent evt) {
	}

	@Override
	public void keyTyped(final KeyEvent evt) {

		boolean revert;
		int new_val = 10;

		try {
			new_val = Integer.parseInt(getText());
			revert = false;

			if (new_val < 0) { // revert if too small
				revert = true;
			}

			if (max > 0 && new_val > max) { // revert to max if too big
				val = max;
				revert = true;
			}
		} catch (final NumberFormatException e) {
			final int len = getText().length();
			if (len != 0) {
				revert = true; // revert if can't convert;
			} else {
				revert = false;
				val = 0;
			}
		}

		if (revert) {
			String string_val = Integer.toString(val);
			setText(string_val);
		} else {
			val = new_val;
		}
	}

	public int getNat() {

		keyTyped(null);
		return val;
	}
}
