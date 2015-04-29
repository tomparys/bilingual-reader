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
		// Compute and set the weight of the panels from the seekbar value
		panelWeight = (float) seekBarProgress / (float) seekbar.getMax();
		if (panelWeight <= 0.1) {
			panelWeight = 0.1f;
		}
		if (panelWeight >= 0.9) {
			panelWeight = 0.9f;
		}
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
