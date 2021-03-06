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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.Func;

/**
 *
 * Dialog to set the relative size of the upper and lower book panel.
 *  Accessible from the main menu.
 *
 */
public class PanelSizeDialog extends DialogFragment {

	protected SeekBar seekbar;
	protected float panelWeight;
	protected int seekBarValue;
	protected Context context;
	protected SharedPreferences preferences;

	@SuppressLint("InflateParams")  // Normal practice for inflation of DialogFragments.
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Get the dialog builder and layout inflater
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// 	Pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.dialog_panel_size, null);

		// Load the seek bar value
		preferences = ((ReaderActivity) getActivity()).getPreferences(Context.MODE_PRIVATE);
		seekBarValue = preferences.getInt("seekBarValue", 50);

		// Set the value to the seekbar
		seekbar = (SeekBar) view.findViewById(R.id.progressBar);
		seekbar.setProgress(seekBarValue);

		// Set title
		builder.setTitle(getString(R.string.SetSizeTitle));

		// Reset to 50/50 button
		((Button) view.findViewById(R.id.reset_panel_size_to_50_50_button)).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						applyAndSave(50);
						dismiss();
					}
				});

		// Add ok button
		builder.setPositiveButton(getString(R.string.OK),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						applyAndSave(seekbar.getProgress());
					}
				});

		// Add cancel button
		builder.setNegativeButton(getString(R.string.Cancel), null);

		// Create the dialog
		builder.setView(view);
		return builder.create();
	}

	/**
	 * Applies the desired panel size and saves it into preferences.
	 * @param seekBarProgress
	 */
	protected void applyAndSave(int seekBarProgress) {
		// Compute the fraction the user set
		panelWeight = (float) seekBarProgress / (float) seekbar.getMax();

		// Compute the actual weight of the panels from the seekbar fraction.
		panelWeight = (Func.PANEL_WEIGHT_MAX - Func.PANEL_WEIGHT_MIN) * panelWeight + Func.PANEL_WEIGHT_MIN;

		// Assure that the weight is between the allowed minimum and maximum.
		panelWeight = Func.minMaxRange(Func.PANEL_WEIGHT_MIN, panelWeight,
				Func.PANEL_WEIGHT_MAX);

		// Set the weight
		((ReaderActivity) getActivity()).changePanelsWeight(panelWeight);

		// Save the value on the seek bar to preferences
		seekBarValue = seekBarProgress;

		saveSeekBarValue(preferences, seekBarValue);
	}

	public static void saveSeekBarValue(SharedPreferences preferences, int seekBarValue) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("seekBarValue", seekBarValue);
		editor.commit();
	}

	public static void saveSeekBarValue(SharedPreferences preferences, float weight) {
		saveSeekBarValue(preferences, (int) (weight * 100));
	}
}
