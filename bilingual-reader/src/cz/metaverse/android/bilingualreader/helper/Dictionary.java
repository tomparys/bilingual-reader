package cz.metaverse.android.bilingualreader.helper;

import android.content.Intent;

public enum Dictionary {

	// Available dictionaries
	colordict, aard;


	// Static method that opens the desired dictionary
	public static Intent open(String text, Dictionary dictionary) {
		switch (dictionary) {

		// ColorDict / GoldenDict
		case colordict:
			Intent intent = new Intent("colordict.intent.action.SEARCH");
			intent.putExtra("EXTRA_QUERY", text);
			//intent.putExtra("EXTRA_HEIGHT", 600 /*"fill_parent"*/);
			//intent.putExtra("EXTRA_GRAVITY", Gravity.CENTER);
			//intent.putExtra("EXTRA_MARGIN_LEFT", 50);
			//intent.putExtra("EXTRA_MARGIN_RIGHT", 50);
			return intent;

		// Sorry, we don't have that here.
		default:
			return null;

		}
	}
}
