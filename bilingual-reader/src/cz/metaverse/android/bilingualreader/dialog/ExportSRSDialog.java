package cz.metaverse.android.bilingualreader.dialog;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.SRSDatabaseActivity;
import cz.metaverse.android.bilingualreader.db.SRSDB;

/**
 *
 * DialogFragment that allows the user to add a given word
 *  to the Spaced Repetition Software (SRS) database of our application.
 *
 */
public class ExportSRSDialog extends DialogFragment implements DialogInterface.OnClickListener {

	protected SRSDatabaseActivity srsDatabaseActivity;

	// The XML form containing EditTexts that user fills with data
	private View form = null;
	private Spinner separator;
	private CheckBox enclose;

	// Database and data
	private SRSDB srsDB;
	private Cursor exportData;


	/**
	 * Parameterless constructor that gets called upon orientation change.
	 */
	public ExportSRSDialog() {}

	/**
	 * Constructor.
	 */
	public ExportSRSDialog(SRSDatabaseActivity activity) {
		this.srsDatabaseActivity = activity;
	}

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Obtain the data
		srsDB = SRSDB.getInstance(getActivity());
		exportData = srsDB.getAll();

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Export_SRS_entries)
		       .setNegativeButton(android.R.string.cancel, null);

		// If there are data to export
		if (exportData != null && exportData.moveToFirst()) {
			// Inflate the form with EditTexts for data
			form = getActivity().getLayoutInflater().inflate(R.layout.dialog_export_srs, null);
			separator = (Spinner) form.findViewById(R.id.export_separator_spinner);
			enclose = (CheckBox) form.findViewById(R.id.export_enclose_in_quotation_marks);

			// Initialize the Spinner
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
					R.array.dialog_export_separator, android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			separator.setAdapter(adapter);

			// Create an Export button and set the view
			builder.setPositiveButton(R.string.Export, this);
			builder.setView(form);
		} else {
			// No data to export
			builder.setMessage(R.string.There_are_no_SRS_cards_to_export);
		}

		// Finish, build and return the dialog to be shown.
		return builder.create();
	}

	/**
	 * Called when user clicks the OK button.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// This dialog gets dismissed automatically when the new one gets created,
		// Calling dismiss() here only causes error.

		/* Export the data */
		char separatorChar = ';';
		switch (separator.getSelectedItemPosition()) {
			case 0: separatorChar = ';'; break;
			case 1: separatorChar = ','; break;
			case 2: separatorChar = '\t'; break;
		}

		final File exportedFile = srsDB.export(exportData, separatorChar, enclose.isChecked());

		/* Show a dialog with the results */
		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Export_SRS_entries)
				.setNegativeButton(R.string.Close, null);

		if (exportedFile == null) {
			// Encountered an error
			builder.setMessage(R.string.Encountered_an_error_while_exporting);

		} else {
			// All went correctly
			View subform = getActivity().getLayoutInflater().inflate(R.layout.dialog_export_successful, null);
			((TextView) subform.findViewById(R.id.export_successful_file_path_textview))
					.setText(exportedFile.getPath());
			builder.setView(subform);

			// Share button
			builder.setPositiveButton(R.string.Share_exported_file, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Create an Intent to share the file.
							Intent shareIntent = new Intent();
							shareIntent.setAction(Intent.ACTION_SEND);
							shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportedFile));
							shareIntent.setType("text/csv");

							// Send the intent to SRSDatabaseActivity to display the Intent chooser.
							// Calling it here causes really weird errors.
							srsDatabaseActivity.openShareIntent(shareIntent);
						}
					});
		}

		// Build and show
		builder.create().show();
	}

}