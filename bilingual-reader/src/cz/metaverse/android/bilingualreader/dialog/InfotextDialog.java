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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.helper.DontShowAgain;

/**
 *
 * Dialog that displays important infotexts throughout the application,
 * along with "Don't Show this again" checkbox.
 *
 */
public class InfotextDialog extends DialogFragment implements DialogInterface.OnClickListener {

	private Integer dontShowAgainId;
	private CheckBox checkBox;

	/**
	 * Static method that launches the Infobox dialog only if the user hasn't clicked
	 *  the Don't Show This Again checkbox.
	 * @param activity            Activity where the dialog is to be opened
	 * @param dontDisplayAgainId  The type of this infobox.
	 * @return                    Whether a dialog was launched or not.
	 */
	public static boolean showIfAppropriate(Activity activity, int dontDisplayAgainId) {
		if (!DontShowAgain.getInstance().get(dontDisplayAgainId)) {
			// Open the dialog.
			InfotextDialog infodial = new InfotextDialog();
			infodial.dontShowAgainId = dontDisplayAgainId;
			infodial.show(activity.getFragmentManager(), "infotext_" + dontDisplayAgainId);
			return true;
		}
		return false;
	}

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// If orientation change recreated this activity, load variable from before.
		if (savedInstanceState != null) {
			dontShowAgainId = savedInstanceState.getInt("dontDisplayAgainId", -1);
			if (dontShowAgainId == -1) {
				dontShowAgainId = null;
			}
		}

		// Inflate the form with EditTexts for data
		View form = getActivity().getLayoutInflater().inflate(R.layout.dialog_infotext, null);
		TextView textView = (TextView) form.findViewById(R.id.infotext_textview);
		checkBox = (CheckBox) form.findViewById(R.id.infotext_dont_display_again_checkbox);

		// Set the displayed text
		if (dontShowAgainId != null) {
			textView.setText(DontShowAgain.getMessageResource(dontShowAgainId));
		}

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Info)
				.setView(form)
				.setPositiveButton(android.R.string.ok, this);
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
	 * Called when user clicks the OK button.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Check on checkBox
		if (dontShowAgainId != null && checkBox.isChecked()) {
			DontShowAgain.getInstance().set(dontShowAgainId, true);
		}
	}
}