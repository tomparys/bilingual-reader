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
import android.widget.Button;
import android.widget.TextView;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.helper.DontShowAgain;

/**
 *
 * Dialog that displays the important infotexts that can be found throughout the application
 * neatly all in one place. The user can browse them using the Next and Previous buttons.
 *
 */
public class InfotextBrowserDialog extends DialogFragment implements Button.OnClickListener {

	private int dontShowAgainId = 0;
	private TextView textView;

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// If orientation change recreated this activity, load variable from before.
		if (savedInstanceState != null) {
			dontShowAgainId = savedInstanceState.getInt("dontDisplayAgainId", 0);
		}

		// Inflate the form with EditTexts for data
		View form = getActivity().getLayoutInflater().inflate(R.layout.dialog_infotext_browser, null);
		textView = (TextView) form.findViewById(R.id.infotext_browser_textview);

		// Set up buttons
		Button next = (Button) form.findViewById(R.id.infotext_browser_next_button);
		Button prev = (Button) form.findViewById(R.id.infotext_browser_previous_button);
		Button ok = (Button) form.findViewById(R.id.infotext_browser_ok_button);

		next.setOnClickListener(this);
		prev.setOnClickListener(this);
		ok.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		// Set the displayed text
		textView.setText(DontShowAgain.getMessageResource(dontShowAgainId));

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Info)
				.setView(form);
		return builder.create();
	}

	/**
	 * Remember the information when the screen is just about to be rotated.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt("dontDisplayAgainId", dontShowAgainId);
	}


	/**
	 * Called when user clicks the Next or Previous button.
	 */
	@Override
	public void onClick(View v) {
		dontShowAgainId += v.getId() == R.id.infotext_browser_next_button ? 1 : -1;
		if (dontShowAgainId < 0) {
			dontShowAgainId = DontShowAgain.TEXTS_COUNT - 1;
		} else if (dontShowAgainId >= DontShowAgain.TEXTS_COUNT) {
			dontShowAgainId = 0;
		}

		textView.setText(DontShowAgain.getMessageResource(dontShowAgainId));
	}
}
