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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.VisualOptions;
import cz.metaverse.android.bilingualreader.manager.Governor;

/**
 *
 * This dialog allows the user to set Visual Options for displaying of the two opened e-books.
 *
 */
public class VisualOptionsDialog extends DialogFragment implements DialogInterface.OnClickListener {

	private Spinner spinnerTextColor;
	private Spinner spinnerBgColor;
	private Spinner spinnerFont;
	private Spinner spinnerFontSize;
	private Spinner spinnerTextAlign;
	private Spinner spinnerLineHeight;
	private Spinner spinnerMargins;
	private CheckBox checkBoxRemoveOriginalStyles;


	@Override
	@SuppressLint("InflateParams") // Normal
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Get the dialog builder and inflate the layout
		Activity activity = getActivity();
		Builder builder = new AlertDialog.Builder(activity);
		View view = activity.getLayoutInflater().inflate(R.layout.dialog_visual_options, null);

		// Get saved preferences
		VisualOptions vo = ((ReaderActivity) activity).governor.getVisualOptions();

		/* Set up the spinners. */
		spinnerTextColor = (Spinner) view.findViewById(R.id.spinnerTextColor);
		spinnerTextColor.setSelection(vo.textColor);

		spinnerBgColor = (Spinner) view.findViewById(R.id.spinnerBgColor);
		spinnerBgColor.setSelection(vo.bgColor);

		spinnerFont = (Spinner) view.findViewById(R.id.spinnerFont);
		spinnerFont.setSelection(vo.font);

		spinnerFontSize = (Spinner) view.findViewById(R.id.spinnerFontSize);
		spinnerFontSize.setSelection(vo.fontSize);

		spinnerTextAlign = (Spinner) view.findViewById(R.id.spinnerTextAlign);
		spinnerTextAlign.setSelection(vo.textAlign);

		spinnerLineHeight = (Spinner) view.findViewById(R.id.spinnerLineHeight);
		spinnerLineHeight.setSelection(vo.lineHeight);

		spinnerMargins = (Spinner) view.findViewById(R.id.spinnerMargins);
		spinnerMargins.setSelection(vo.margins);

		checkBoxRemoveOriginalStyles = (CheckBox) view.findViewById(R.id.d_vo_remove_original_styles);
		checkBoxRemoveOriginalStyles.setChecked(vo.removeOriginalStyles);

		// The Info button
		Button infoButton = (Button) view.findViewById(R.id.d_vo_remove_original_styles_info_button);
		infoButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Display Info dialog.
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setNegativeButton(R.string.Close, null);
				builder.setTitle(R.string.Info);
				builder.setMessage(R.string.Remove_original_styles_info_msg);
				builder.create().show();
			}
		});

		// The OK button
		builder.setPositiveButton(getString(R.string.OK), this);

		// The Default Styles button
		builder.setNeutralButton(getString(R.string.DefaultSettings), new DefaultButtonListener());

		// The Cancel button
		builder.setNegativeButton(getString(R.string.Cancel), null);

		// Set title and set the view
		builder.setTitle(R.string.Visual_Options);
		builder.setView(view);

		// Finally, create the dialogue and return it.
		return builder.create();
	}

	/**
	 * Called when the OK button is clicked.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Create a new VisualOptions object and fill it with the settings.
		VisualOptions newVisualOptions = new VisualOptions();

		newVisualOptions.applyOptions = true;
		newVisualOptions.removeOriginalStyles = checkBoxRemoveOriginalStyles.isChecked();

		newVisualOptions.textColor = (int) spinnerTextColor.getSelectedItemId();
		newVisualOptions.bgColor = (int) spinnerBgColor.getSelectedItemId();
		newVisualOptions.font = (int) spinnerFont.getSelectedItemId();
		newVisualOptions.fontSize = (int) spinnerFontSize.getSelectedItemId();
		newVisualOptions.textAlign = (int) spinnerTextAlign.getSelectedItemId();
		newVisualOptions.lineHeight = (int) spinnerLineHeight.getSelectedItemId();
		newVisualOptions.margins = (int) spinnerMargins.getSelectedItemId();

		// Apply the changes.
		((ReaderActivity) getActivity()).governor.setVisualOptions(newVisualOptions);
	}

	/**
	 * Called when the "Reset to default" button is clicked.
	 */
	private class DefaultButtonListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			Governor governor = ((ReaderActivity) getActivity()).governor;

			// Create a new VisualOptions object and fill it with the settings.
			VisualOptions newVisualOptions = governor.getVisualOptions();
			newVisualOptions.applyOptions = false;

			// Apply the changes.
			governor.setVisualOptions(newVisualOptions);
		}
	}
}
