package cz.metaverse.android.bilingualreader.helper;

import android.content.Intent;

public enum Dictionary {

	// Available dictionaries
	colordict, fora, free_dictionary_org, aard, aard_lookup;


	// Static method that opens the desired dictionary
	public static Intent open(String text, Dictionary dictionary) {
		Intent intent;
		switch (dictionary) {

		// ColorDict / GoldenDict
		case colordict:
			intent = new Intent("colordict.intent.action.SEARCH");
			intent.putExtra("EXTRA_QUERY", text);
			//intent.putExtra("EXTRA_HEIGHT", 600 /*"fill_parent"*/);
			//intent.putExtra("EXTRA_GRAVITY", Gravity.CENTER);
			//intent.putExtra("EXTRA_MARGIN_LEFT", 50);
			//intent.putExtra("EXTRA_MARGIN_RIGHT", 50);
			return intent;

		case fora:
			return DictionaryAPI.getIntent("Fora Dictionary", "com.ngc.fora", ".ForaDictionary",
					"android.intent.action.SEARCH", "query", "%s", text);

		case free_dictionary_org:
			return DictionaryAPI.getIntent("Free Dictionary . org", "org.freedictionary", ".MainActivity",
					"android.intent.action.VIEW", null, "%s", text);

		case aard:
			return DictionaryAPI.getIntent("Aard Dictionary", "aarddict.android", ".Article",
					"android.intent.action.SEARCH", "query", "%s", text);

		case aard_lookup:
			return DictionaryAPI.getIntent("Aard Dictionary Lookup", "aarddict.android", ".Lookup",
					"android.intent.action.SEARCH", "query", "%s", text);

		// Sorry, we don't have that here.
		default:
			return null;

		}
	}
}
