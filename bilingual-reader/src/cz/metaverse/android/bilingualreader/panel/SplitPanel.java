/*
The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package cz.metaverse.android.bilingualreader.panel;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;

/**
 *
 * Abstract fragment that represents a general book-reading panel containing
 * 	only the closing button.
 *
 */
public abstract class SplitPanel extends Fragment {

	private static final String LOG = "SplitPanel";

	protected Governor governor;
	protected PanelHolder panelHolder;
	protected int panelPosition;

	private RelativeLayout splitPanelLayout;
	protected RelativeLayout contentBoxLayout;
	protected float weight = 0.5f; // weight of the generalLayout
	protected boolean created; // tells whether the fragment has been created


	/**
	 * Constructor - let's get the important info filled.
	 * @param governor  The Governor of our application.
	 * @param panelHolder  The PanelHolder instance holding this panel.
	 * @param position  The position of this panel.
	 */
	public SplitPanel(Governor governor, PanelHolder panelHolder, int position) {
		this.governor = governor;
		this.panelHolder = panelHolder;
		updatePosition(position);
	}

	/**
	 * Checks whether there are any problems with this instance, if for example the Android system
	 * didn't close any important fields that would result in NullPointerExceptions.
	 * @return true if everything appears to be sound
	 */
	public boolean selfCheck() {
		boolean ok = governor != null && panelHolder != null;

		Log.d(LOG, "SplitPanel selfCheck - " + ok);
		return ok;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.panel_split, container,
				false);
		created = false;
		return v;
	}

	@Override
	public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
		created = true;

		// Set to retain this Fragment even if the underlying Activity gets re-created.
		setRetainInstance(true);

		splitPanelLayout = (RelativeLayout) getView().findViewById(R.id.GeneralLayout);
		contentBoxLayout = (RelativeLayout) getView().findViewById(R.id.Content);

		changeWeight(weight);
	}

	// Change the weight of the general layout
	public void changeWeight(float value) {
		weight = value;
		if (created) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, value);
			splitPanelLayout.setLayoutParams(params);
		}
	}

	public float getWeight() {
		return weight;
	}

	public int getPosition() {
		return panelPosition;
	}

	public void updatePosition(int value) {
		panelPosition = value;
	}

	public void errorMessage(String message) {
		((ReaderActivity) getActivity()).errorMessage(message);
	}

	// Saves the weight of this SplitPanel during saveState
	public void saveState(Editor editor) {
		editor.putFloat("weight" + panelPosition, weight);
	}

	// Restores the weight of this SplitPanel during saveState
	public void loadState(SharedPreferences preferences) {
		changeWeight(preferences.getFloat("weight" + panelPosition, 0.5f));
	}
}
