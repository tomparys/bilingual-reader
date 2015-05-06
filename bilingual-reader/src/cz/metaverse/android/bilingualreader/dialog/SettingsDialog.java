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

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.Dictionary;

/**
 *
 * DialogFragment that allows the user to add a given word
 *  to the Spaced Repetition Software (SRS) database of our application.
 *
 */
public class SettingsDialog extends DialogFragment implements DialogInterface.OnClickListener {

	// The XML form containing elements that user fills with data
	private View form;
	private Spinner spinner;

	// Data
	private List<Dictionary> dictionaries;

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Inflate the form with EditTexts for data
		form = getActivity().getLayoutInflater().inflate(R.layout.dialog_settings, null);
		spinner = (Spinner) form.findViewById(R.id.default_dict_spinner);
		Button button = (Button) form.findViewById(R.id.see_dictionaries_that_work_with_this_app);

		// Initialize the spinner
		dictionaries = Dictionary.getAvailable(getActivity());
		initializeDictSpinner(spinner);

		// Initialize the button that will test the selected dictionary.
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Test the dictionary
				if (dictionaries.size() > 0) {
					Dictionary selectedDict = (Dictionary) spinner.getSelectedItem();
					selectedDict.open(getActivity(),
							getActivity().getString(R.string.test_dictionary_string));
				}
			}
		});

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Settings)
				.setView(form)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	private void initializeDictSpinner(Spinner spin) {
		// If there are available apps.
		if (dictionaries.size() > 0) {
			// Create an ArrayAdapter using the default spinner layout
			// and list of Dictionaries (they implement toString() so it's not a problem).
			ArrayAdapter<Dictionary> adapter = new ArrayAdapter<Dictionary>(
					getActivity(), android.R.layout.simple_spinner_item, dictionaries);

			// Specify the layout to use when the list of choices appears and set the adapter
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);

			// Set the selected item in the spinner
			Dictionary defaultDict = Dictionary.getDefault((ReaderActivity) getActivity());
			if (defaultDict != null) {
				int defaultDictPosition = dictionaries.indexOf(defaultDict);
				if (defaultDictPosition != -1) {
					spinner.setSelection(defaultDictPosition);
				}
			}
		} else {
			// If there are no dictionaries, display info about that.
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
					R.array.spinner_no_dictionaries, android.R.layout.simple_spinner_item);

			// Specify the layout to use when the list of choices appears and set the adapter
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
		}
	}

	/**
	 * Called when user clicks the OK or the Cancel button.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Spinner
		if (dictionaries.size() > 0) {
			Dictionary selectedDict = (Dictionary) spinner.getSelectedItem();
			Dictionary.setDefault((ReaderActivity) getActivity(), selectedDict);
		}
	}

	/**
	 * Called when the dialog gets dismissed by the Cancel button.
	 */
	@Override
	public void onDismiss(DialogInterface unused) {
		super.onDismiss(unused);
	}

	/**
	 * Called when the dialog gets dismissed otherwise,
	 *  e.g. clicking around the dialog.
	 */
	@Override
	public void onCancel(DialogInterface unused) {
		super.onCancel(unused);
	}

}