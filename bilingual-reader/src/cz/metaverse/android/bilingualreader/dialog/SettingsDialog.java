package cz.metaverse.android.bilingualreader.dialog;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import cz.metaverse.android.bilingualreader.R;
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

		// Initialize the spinner
		dictionaries = Dictionary.getInstalledDictionaries(getActivity());
		initializeDefaultDictSpinner(spinner, dictionaries);

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Settings)
				.setView(form)
				.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	private void initializeDefaultDictSpinner(Spinner spin, List<Dictionary> dicts) {
		// Create an ArrayAdapter using the default spinner layout
		// and list of Dictionaries (they implement toString() so it's not a problem that it's not String).
		ArrayAdapter<Dictionary> adapter = new ArrayAdapter<Dictionary>(
				getActivity(), android.R.layout.simple_spinner_item, dicts);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);

		// Set the selected item in the spinner
		Dictionary defaultDict = Dictionary.getDefault(getActivity());
		if (defaultDict != null) {
			int defaultDictPosition = dicts.indexOf(defaultDict);
			if (defaultDictPosition != -1) {
				spinner.setSelection(defaultDictPosition);
			}
		}
	}

	/**
	 * Called when user clicks the OK or the Cancel button.
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Spinner
		Dictionary selectedDict = (Dictionary) spinner.getSelectedItem();
		Dictionary.setDefault(getActivity(), selectedDict);
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