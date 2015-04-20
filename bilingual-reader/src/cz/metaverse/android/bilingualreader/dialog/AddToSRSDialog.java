package cz.metaverse.android.bilingualreader.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.db.SRSDatabaseTable;

/**
 *
 * DialogFragment that allows the user to add a given word
 *  to the Spaced Repetition Software (SRS) database of our application.
 *
 */
public class AddToSRSDialog extends DialogFragment implements DialogInterface.OnClickListener {

	// The XML form containing EditTexts that user fills with data
	private View form = null;
	private EditText word, translation;
	private String original_word;

	/**
	 * Constructor to set the data to be displayed in the EditTexts.
	 * @param word	The word to set to the "word" EditText
	 */
	public AddToSRSDialog(String word) {
		super();
		original_word = word;
	}

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Inflate the form with EditTexts for data
		form = getActivity().getLayoutInflater().inflate(R.layout.add_to_srs_dialog, null);
		word = (EditText) form.findViewById(R.id.word_edit_text);
		translation = (EditText) form.findViewById(R.id.translation_edit_text);

		// Fill the form with data and set focus to the translation EditText
		word.setText(original_word);
		translation.requestFocus();

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.add_to_srs)
				.setView(form)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	/**
	 * Called when user clicks the OK or the Cancel button.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		SRSDatabaseTable db = SRSDatabaseTable.getInstance(getActivity());
		db.addWord(word.getText().toString(), translation.getText().toString());
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