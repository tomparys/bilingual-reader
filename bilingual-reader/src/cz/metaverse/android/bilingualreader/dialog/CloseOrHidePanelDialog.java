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
 */

package cz.metaverse.android.bilingualreader.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.manager.Governor;

/**
 *
 * DialogFragment that gives the option to Close/Hide one of the panels while also
 * displaying info on how it could have been done through touch gestures.
 *
 */
public class CloseOrHidePanelDialog extends DialogFragment {

	// Whether this is a Close panel dialog or Hide panel dialog.
	private boolean close;

	// The XML form containing EditTexts that user fills with data
	private View form = null;
	private TextView textView;
	private Button button1;
	private Button button2;


	/**
	 * Parameterless constructor that gets called upon orientation change.
	 */
	public CloseOrHidePanelDialog() {}

	/**
	 * Constructor - so we know whether this is close or hide panel dialog.
	 */
	public CloseOrHidePanelDialog( boolean close) {
		this.close = close;
	}

	/**
	 * Remember the information when the screen is just about to be rotated.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("close", close);
	}

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// If orientation change recreated this activity, load variables from before.
		if (savedInstanceState != null) {
			close = savedInstanceState.getBoolean("close", false);
		}

		// Inflate the form with EditTexts for data
		form = getActivity().getLayoutInflater().inflate(R.layout.dialog_close_or_hide_panel, null);
		textView = (TextView) form.findViewById(R.id.close_or_hide_panel_info_textview);
		button1 = (Button) form.findViewById(R.id.close_or_hide_panel_1_button);
		button2 = (Button) form.findViewById(R.id.close_or_hide_panel_2_button);

		// Set up info text.
		textView.setText(close ? R.string.close_panel_info_notice : R.string.hide_panel_info_notice);

		/* Button 1 */
		button1.setText(close ? R.string.Close_Panel_1 : R.string.Hide_Panel_1);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (close) {
					getGovernor().getPanelHolder(0).closePanel();
				} else {
					getGovernor().getPanelHolder(0).hidePanel();
				}
				dismiss();
			}
		});

		/* Button 2 */
		if (!getGovernor().getPanelHolder(1).hasOpenPanel()) {
			// Hide the button if the second panel isn't open.
			button2.setVisibility(View.GONE);
		} else {
			button2.setText(close ? R.string.Close_Panel_2 : R.string.Hide_Panel_2);
			button2.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (close) {
						getGovernor().getPanelHolder(1).closePanel();
					} else {
						getGovernor().getPanelHolder(1).hidePanel();
					}
					dismiss();
				}
			});
		}

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(close ? R.string.Close_panel : R.string.Hide_panel)
				.setView(form)
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	/**
	 * Obtains a Governor instance from our ReaderActivity.
	 */
	private Governor getGovernor() {
		return ((ReaderActivity) getActivity()).governor;
	}
}
