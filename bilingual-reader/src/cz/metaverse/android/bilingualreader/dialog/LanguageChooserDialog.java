/*
Bilingual Reader for Android

Copyright (c) 2015 Tomáš Orsava

This program is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License as published by the Free Software Foundation, either
version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this
program (see the LICENSE.html file). If not, see http://www.gnu.org/licenses/.

Contact: gpl at orsava.cz


This file incorporates work covered by the following copyright and permission notice:


The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package cz.metaverse.android.bilingualreader.dialog;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;

/**
 *
 * Language chooser for epubs that contain more than one language at once.
 * 	It has no use in our application - TODO.
 *
 */

public class LanguageChooserDialog extends DialogFragment {
	String[] languages;
	int book;
	boolean[] selected;
	int number_selected_elements;
	ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle b = this.getArguments();
		languages = b.getStringArray(getString(R.string.lang));
		book = b.getInt(getString(R.string.tome));
		selected = new boolean[languages.length];
		number_selected_elements = 0;

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setTitle(getString(R.string.LanguageChooserTitle));
		builder.setMultiChoiceItems(languages, selected,
				new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which,
							boolean isChecked) {
						if (isChecked) {
							if (number_selected_elements == 2) {
								selected[which] = false;
							} else {
								mSelectedItems.add(which);
								number_selected_elements++;
							}
						} else if (mSelectedItems.contains(which)) {
							mSelectedItems.remove(Integer.valueOf(which));
							number_selected_elements--;
						}
					}
				});

		builder.setPositiveButton(getString(R.string.OK),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						int first = -1;
						int second = -1;

						// keep the first two selected
						for (int i = 0; i < selected.length; i++) {
							if (selected[i]) {
								if (first == -1) {
									first = i;
								} else if (second == -1) {
									second = i;
								}
							}
						}

						if (number_selected_elements >= 2) {
							((ReaderActivity) getActivity()).startParallelText(book, first, second);
							getActivity().invalidateOptionsMenu();
						}
						else if (number_selected_elements == 1) {
							((ReaderActivity) getActivity()).startParallelText(book, first, -1);
							getActivity().invalidateOptionsMenu();
						}
					}
				});

		builder.setNegativeButton(getString(R.string.Cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
					}
				});

		return builder.create();
	}
}
