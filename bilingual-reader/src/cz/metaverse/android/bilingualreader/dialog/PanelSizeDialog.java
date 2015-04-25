package cz.metaverse.android.bilingualreader.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Get the dialog builder and layout inflater
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// 	Pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.dialog_panel_size, null);

		// Load the seek bar value
		final SharedPreferences preferences = ((ReaderActivity) getActivity())
				.getPreferences(Context.MODE_PRIVATE);
		seekBarValue = preferences.getInt("seekBarValue", 50);

		// Set the value to the seekbar
		seekbar = (SeekBar) view.findViewById(R.id.progressBar);
		seekbar.setProgress(seekBarValue);

		// Set title
		builder.setTitle(getString(R.string.SetSizeTitle));

		// Add ok button
		builder.setPositiveButton(getString(R.string.OK),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// Compute and set the weight of the panels from the seekbar value
						panelWeight = (float) seekbar.getProgress() / (float) seekbar.getMax();
						if (panelWeight <= 0.1) {
							panelWeight = 0.1f;
						}
						if (panelWeight >= 0.9) {
							panelWeight = 0.9f;
						}
						((ReaderActivity) getActivity()).changePanelsWeight(panelWeight);

						// Save the value on the seek bar to preferences
						seekBarValue = seekbar.getProgress();

						SharedPreferences.Editor editor = preferences.edit();
						editor.putInt("seekBarValue", seekBarValue);
						editor.commit();
					}
				});

		// Add cancel button
		builder.setNegativeButton(getString(R.string.Cancel), null);

		// Create the dialog
		builder.setView(view);
		return builder.create();
	}

}
