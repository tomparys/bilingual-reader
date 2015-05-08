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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.enums.ScrollSyncMethod;
import cz.metaverse.android.bilingualreader.helper.ScrollSyncPoint;
import cz.metaverse.android.bilingualreader.manager.Governor;

/**
 *
 * DialogFragment that allows the user to add a given word
 *  to the Spaced Repetition Software (SRS) database of our application.
 *
 */
public class ScrollSyncDialog extends DialogFragment
		implements OnCheckedChangeListener, View.OnClickListener {

	private static final String LOG = "ScrollSyncDialog";

	// The XML form containing elements that user fills with data
	private View form;
	private RadioButton[] radioButton;
	private Button[] infoButton;
	private Button[] syncPointButton;
	private Button syncPointRecomputeButton;

	private boolean justSetSyncPoint = false;

	private ScrollSyncMethod originalScrollSyncMethod;

	/**
	 * Called when the dialog gets created.
	 */
	@SuppressLint("InflateParams") // Normal for DialogFragments
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Inflate the form and find Buttons, RadioButtons and other shennanigans.
		form = getActivity().getLayoutInflater().inflate(R.layout.dialog_scroll_sync, null);
		radioButton = new RadioButton[] {
				(RadioButton) form.findViewById(R.id.proportional_scroll_sync_radio_button),
				(RadioButton) form.findViewById(R.id.linear_scroll_sync_radio_button),
				(RadioButton) form.findViewById(R.id.sync_point_scroll_sync_radio_button)};
		infoButton = new Button[] {
				(Button) form.findViewById(R.id.proportional_scroll_sync_info_button),
				(Button) form.findViewById(R.id.linear_scroll_sync_info_button),
				(Button) form.findViewById(R.id.sync_point_scroll_sync_info_button)};
		syncPointButton = new Button[] {
				(Button) form.findViewById(R.id.sync_point_1_button),
				(Button) form.findViewById(R.id.sync_point_2_button)};
		syncPointRecomputeButton = (Button) form.findViewById(R.id.scroll_sync_point_recompute_button);

		// Set onCheckedChange/onClick listeners to everything that we can. EVERYTHING!
		radioButton[0].setOnCheckedChangeListener(this);
		radioButton[1].setOnCheckedChangeListener(this);
		radioButton[2].setOnCheckedChangeListener(this);
		infoButton[0].setOnClickListener(this);
		infoButton[1].setOnClickListener(this);
		infoButton[2].setOnClickListener(this);
		OnClickListener syncPointListener = new ScrollPointButtonOnClickListener();
		syncPointButton[0].setOnClickListener(syncPointListener);
		syncPointButton[1].setOnClickListener(syncPointListener);

		// Set the checked radio button.
		Governor governor = getGovernor();
		originalScrollSyncMethod = governor.getScrollSyncMethod();
		if (originalScrollSyncMethod != null) {
			switch (originalScrollSyncMethod) {
				case proportional: radioButton[0].setChecked(true); break;
				case linear: radioButton[1].setChecked(true); break;
				case syncPoints: radioButton[2].setChecked(true); break;
				case none: break;
			}
		}

		// Set the visibility of the syncPointRecomputeButton and its OnClickListener.
		if (originalScrollSyncMethod == ScrollSyncMethod.syncPoints) {
			syncPointRecomputeButton.setVisibility(View.VISIBLE);

			syncPointRecomputeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (computeSyncPoints()) {
						originalScrollSyncMethod = ScrollSyncMethod.syncPoints;
						Toast.makeText(getActivity(), R.string.Sync_points_were_recomputed, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		// Set the texts on SyncPointButtons
		ScrollSyncPoint[] scrollSyncPoint = getGovernor().getScrollSyncPoints();
		if (scrollSyncPoint != null && scrollSyncPoint[0] != null) {
			syncPointButton[0].setText(R.string.Sync_Point_1_is_set);
		}
		if (scrollSyncPoint != null && scrollSyncPoint[1] != null) {
			syncPointButton[1].setText(R.string.Sync_Point_2_is_set);
		}

		// Use builder to create the rest of the Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.Scroll_Synchronization)
				.setView(form)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(android.R.string.cancel, null);
		return builder.create();
	}

	/**
	 * Overriding onStart() so we can intercept the already created "OK" button and change his listener.
	 * That way we can decide if we will dismiss() the dialog after the user clicked OK or if we won't.
	 */
	@Override
	public void onStart() {
		super.onStart();

		AlertDialog dialog = (AlertDialog) getDialog();
		if (dialog != null) {
			Button positiveButton = (Button) dialog.getButton(Dialog.BUTTON_POSITIVE);
			positiveButton.setOnClickListener(new OKButtonOnClickListener());
	    }
	}

	/**
	 * Obtains and returns instance of the Governor. Don't ask how he does it.
	 */
	private Governor getGovernor() {
		return ((ReaderActivity) getActivity()).governor;
	}


	/**
	 * Called when user clicks the OK button.
	 */
	private class OKButtonOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// Find which method has been chosen if any.
			ScrollSyncMethod selectedMethod = null;
			if (radioButton[0].isChecked()) {
				selectedMethod = ScrollSyncMethod.proportional;
			} else if (radioButton[1].isChecked()) {
				selectedMethod = ScrollSyncMethod.linear;
			} else if (radioButton[2].isChecked()) {
				selectedMethod = ScrollSyncMethod.syncPoints;
			}

			// Activate the chosen method of ScrollSync.
			if (selectedMethod != null) {

				// If method is syncPoints, we need to check things.
				if (selectedMethod == ScrollSyncMethod.syncPoints) {

					// If the syncPoints was already set before.
					if (selectedMethod == originalScrollSyncMethod) {
						// We switch ScrollSync on, and if it has changed state, we give a toast.
						if (getGovernor().setScrollSync(true, true)) {
							displaySyncActivatedToast();
						}
						dismiss();
					}
					// We're switching TO syncPoints
					else {
						if (computeSyncPoints()) {
							getGovernor().setScrollSync(true, false);
							// Display Activated toast even if ScrollSync was already active,
							// because we switched to a different ScrollSync method.
							displaySyncActivatedToast();
							dismiss();
						}
					}
				}
				// If method is NOT syncPoints, start Scroll Sync.
				else {
					if (selectedMethod == originalScrollSyncMethod) {
						if (getGovernor().setScrollSync(true, true)) {
							displaySyncActivatedToast();
						}
					} else {
						if (getGovernor().setScrollSyncMethod(selectedMethod)) {
							getGovernor().setScrollSync(true, false);
							// Display Activated toast even if ScrollSync was already active,
							// because we switched to a different ScrollSync method.
							displaySyncActivatedToast();
						}
					}
					dismiss();

				}
			} else {
				Toast.makeText(getActivity(), R.string.Choose_a_synchronization_method, Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Computes Scroll Sync data for the syncPoints SS method from the two provided Sync Points.
	 * @return  Whether the computation was successful and scroll sync data set or not.
	 */
	private boolean computeSyncPoints() {
		ScrollSyncPoint[] scrollSyncPoint = getGovernor().getScrollSyncPoints();

		if (scrollSyncPoint != null && scrollSyncPoint[0] != null && scrollSyncPoint[1] != null) {
			// Both sync points are set, we compute the Scroll Sync data.
			return getGovernor().setScrollSyncMethod(ScrollSyncMethod.syncPoints);
		} else {
			// Some sync points are not set, cannot start Scroll Sync.
			Toast.makeText(getActivity(), R.string.Cant_set_Sync_Point_scroll_msg, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	/**
	 * Displays a toast that scroll sync was activated.
	 */
	private void displaySyncActivatedToast() {
		Toast.makeText(getActivity(), R.string.Scroll_Sync_was_activated, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Called when a RadioButton is checked (or unchecked).
	 */
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Log.d(LOG, LOG + ".onCheckedChanged: isChecked: " + isChecked);

		// If this radio button is now checked, uncheck the button that was checked before it.
		if (isChecked) {
			for (RadioButton rb : radioButton) {
				if (!rb.equals(buttonView) && rb.isChecked()) {
					rb.setChecked(false);
					Log.d(LOG, LOG + ".onCheckedChanged: setChecked(false)");
				}
			}
		}
	}

	/**
	 * onClickListener for the Sync Point buttons.
	 */
	private class ScrollPointButtonOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			// Set the "Synchronization Points" radio button as checked.
			radioButton[2].setChecked(true);

			if (justSetSyncPoint) {
				// User already set a SyncPoint while in this dialog.
				Toast.makeText(getActivity(), R.string.Sync_point_just_set_msg, Toast.LENGTH_LONG).show();

			} else {
				int point = v.getId() == R.id.sync_point_1_button ? 0 : 1;
				getGovernor().setScrollSyncPointNow(point);
				justSetSyncPoint = true;

				syncPointButton[point].setText(
						point == 0 ? R.string.Sync_Point_1_is_set : R.string.Sync_Point_2_is_set);

				Toast.makeText(getActivity(),
						point == 0 ? R.string.Sync_Point_1_was_set : R.string.Sync_Point_2_was_set,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	 * Called when one of the "Info" buttons gets clicked.
	 * Creates and shows an Info dialog.
	 */
	@Override
	public void onClick(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setNegativeButton(R.string.Close, null);

		if (v.getId() == R.id.proportional_scroll_sync_info_button) {
			builder.setTitle(R.string.Proportional_Scroll_Sync);
			builder.setMessage(R.string.Proportional_Scroll_Sync_info_msg);
		}
		else if (v.getId() == R.id.linear_scroll_sync_info_button) {
			builder.setTitle(R.string.Linear_Scroll_Sync);
			builder.setMessage(R.string.Linear_Scroll_Sync_info_msg);
		} else {
			builder.setTitle(R.string.Sync_Points_Scroll_Sync);
			builder.setMessage(R.string.Sync_Points_Scroll_Sync_info_msg);
		}

		// Build and show
		builder.create().show();
	}
}