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
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.R;

/**
 * 
 * Dialog to set the relative size of the upper and lower book panel.
 *  Accessible from the main menu.
 *
 */
public class PanelSizeDialog extends DialogFragment {

	protected SeekBar seekbar;
	protected float value = (float) 0.2;
	protected int seekBarValue = 50;
	protected Context context;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Get the dialog builder and layout inflater
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// Inflate and set the layout for the dialog
		// 	Pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.set_panel_size, null);

		// Load the seek bar value
		final SharedPreferences preferences = ((ReaderActivity) getActivity())
				.getPreferences(Context.MODE_PRIVATE);
		
		seekBarValue = preferences.getInt("seekBarValue", 50);
		seekbar = (SeekBar) view.findViewById(R.id.progressBar);
		seekbar.setProgress(seekBarValue);

		// Set title
		builder.setTitle(getString(R.string.SetSizeTitle));
		builder.setView(view);

		// Add ok button
		builder.setPositiveButton(getString(R.string.OK),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// Get the resulting value from the seekbar
						float actual = (float) seekbar.getProgress();
						value = actual / (float) seekbar.getMax();
						if (value <= 0.1)
							value = (float) 0.1;
						if (value >= 0.9)
							value = (float) 0.9;

						// Set the value to view.
						((ReaderActivity) getActivity()).changeViewsSize(value);
						
						// Save the value to shared preferences
						SharedPreferences.Editor editor = preferences.edit();
						editor.putInt("seekBarValue", seekBarValue);
						editor.commit();

						// Save the actual value for future showing of the seekbar
						seekBarValue = seekbar.getProgress();
					}
				});
		
		// Add cancel button
		builder.setNegativeButton(getString(R.string.Cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});

		// Create the dialog
		return builder.create();
	}

}
