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

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.SRSDatabaseActivity;
import cz.metaverse.android.bilingualreader.db.SRSDB;

/**
 *
 * DialogFragment that allows the user to add a given word
 *  to the Spaced Repetition Software (SRS) database of our application.
 *
 */
public class AddToSRSDialog extends DialogFragment implements DialogInterface.OnClickListener {

	// The XML form containing EditTexts that user fills with data
	private View form = null;
	private EditText word;
	private EditText description;

	// Text to be filled into the EditTexts
	private String original_word;
	private String original_description;

	// Data to allow working with SRSDatabaseActivity, e.g. editing existing SRS cards.
	private boolean startedFromSRSDatabaseActivity;
	private Long rowid;


	/**
	 * Parameterless constructor that gets called upon orientation change.
	 */
	public AddToSRSDialog() {}

	/**
	 * Constructor to set the data to be displayed in the EditTexts.
	 * @param word	The word to set to the "word" EditText
	 */
	public AddToSRSDialog(String word) {
		startedFromSRSDatabaseActivity = false;
		original_word = word;
		if (original_word != null) {
			original_word = original_word.toLowerCase(Locale.getDefault());
		}
	}

	/**
	 * Constructor to allow working with SRSDatabaseActivity on editing existing SRS cards.
	 * @param rowid	Id of the DB row that we're going to update with new data.
	 */
	public AddToSRSDialog(boolean startedFromSRSDActivity, Long rowid, String word, String description) {
		this.startedFromSRSDatabaseActivity = startedFromSRSDActivity;
		this.rowid = rowid;
		this.original_word = word;
		this.original_description = description;
	}

	/**
	 * Constructor to allow refreshing of data in the SRSDialogActivity afterwards.
	 */
	public AddToSRSDialog(boolean startedFromSRSDActivity) {
		this(startedFromSRSDActivity, null, null, null);
	}

	/**
	 * Remember the information when the screen is just about to be rotated.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean("startedFromSRSDActivity", startedFromSRSDatabaseActivity);
		outState.putLong("rowid", rowid);
	}

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// If orientation change recreated this activity, load variables from before.
		if (savedInstanceState != null) {
			startedFromSRSDatabaseActivity = savedInstanceState.getBoolean("startedFromSRSDActivity", false);
			rowid = savedInstanceState.getLong("rowid", -1);
			if (rowid == -1) {
				rowid = null;
			}
		}

		// Inflate the form with EditTexts for data
		form = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_to_srs, null);
		word = (EditText) form.findViewById(R.id.word_edit_text);
		description = (EditText) form.findViewById(R.id.translation_edit_text);

		// Pre-fill in the words if available
		if (original_word != null) {
			word.setText(original_word);

			if (original_description == null) {
				// If the description is missing, put the cursor there.
				description.requestFocus();
			}
		}

		if (original_description != null) {
			description.setText(original_description);
		}

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.add_to_srs)
				.setView(form)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	/**
	 * Called when user clicks the OK button.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		SRSDB db = SRSDB.getInstance(getActivity());

		if (rowid != null) {
			// Edit the already existing card in the database
			db.editCard(rowid, word.getText().toString(), description.getText().toString());
		} else {
			// Add a new card to the database
			db.addCard(word.getText().toString(), description.getText().toString());
		}

		// Reload the data in the SRSDatabaseActivity so that the new/edited card is immediately visible.
		if (startedFromSRSDatabaseActivity) {
			((SRSDatabaseActivity) getActivity()).reloadData();
		}
	}
}