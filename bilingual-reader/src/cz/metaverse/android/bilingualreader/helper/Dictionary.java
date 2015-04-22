package cz.metaverse.android.bilingualreader.helper;

import android.content.Intent;

/**
 *
 * Helper class that serves to open a dictionary in search of a given text.
 *
 */
public enum Dictionary {

	// Available dictionaries
	aard, aard_lookup, colordict, colordict2, fora, free_dictionary_org, lingo_quiz_lite, lingo_quiz,
	leo, abbyy, popup, merriam_webster_unabridged;


	// Static method that opens the desired dictionary
	public static Intent open(String text, Dictionary dictionary) {
		Intent intent;
		switch (dictionary) {

		// Open-source dictionary aard
		case aard:
			return DictionaryAPI.getIntent("Aard Dictionary", "aarddict.android", ".Article",
					"android.intent.action.SEARCH", "query", "%s", text);

		case aard_lookup:
			return DictionaryAPI.getIntent("Aard Dictionary Lookup", "aarddict.android", ".Lookup",
					"android.intent.action.SEARCH", "query", "%s", text);

		// ColorDict / GoldenDict
		case colordict:
			intent = new Intent("colordict.intent.action.SEARCH");
			intent.putExtra("EXTRA_QUERY", text);
			//intent.putExtra("EXTRA_HEIGHT", 600 /*"fill_parent"*/);
			//intent.putExtra("EXTRA_GRAVITY", Gravity.CENTER);
			//intent.putExtra("EXTRA_MARGIN_LEFT", 50);
			//intent.putExtra("EXTRA_MARGIN_RIGHT", 50);
			return intent;

		case colordict2:
			return DictionaryAPI.getIntent("ColorDict Old Style", "com.socialnmobile.colordict",
					".activity.Main", "android.intent.action.SEARCH", "query", "%s", text);

		case fora:
			return DictionaryAPI.getIntent("Fora Dictionary", "com.ngc.fora", ".ForaDictionary",
					"android.intent.action.SEARCH", "query", "%s", text);

		case free_dictionary_org:
			return DictionaryAPI.getIntent("Free Dictionary . org", "org.freedictionary", ".MainActivity",
					"android.intent.action.VIEW", null, "%s", text);

		case lingo_quiz_lite:
			return DictionaryAPI.getIntent("Lingo Quiz Lite", "mnm.lite.lingoquiz", ".ExchangeActivity",
					"lingoquiz.intent.action.ADD_WORD", "EXTRA_WORD", "%s", text);

		case lingo_quiz:
			return DictionaryAPI.getIntent("Lingo Quiz", "mnm.lingoquiz", ".ExchangeActivity",
					"lingoquiz.intent.action.ADD_WORD", "EXTRA_WORD", "%s", text);

		case leo:
			return DictionaryAPI.getIntent("LEO Dictionary", "org.leo.android.dict", ".LeoDict",
					"android.intent.action.SEARCH", "query", "%s", text);

		case abbyy:
			return DictionaryAPI.getIntent("ABBYY Lingvo", "com.abbyy.mobile.lingvo.market", null,
					"com.abbyy.mobile.lingvo.intent.action.TRANSLATE",
					"com.abbyy.mobile.lingvo.intent.extra.TEXT", "%s", text);

		case popup:
			return DictionaryAPI.getIntent("Popup Dictionary", "com.barisatamer.popupdictionary",
					".MainActivity", "android.intent.action.VIEW", null, "%s", text);

		case merriam_webster_unabridged:
			return DictionaryAPI.getIntent("Merriam-Webster's Unabridged",
					"com.slovoed.noreg.merriam_webster.english_english_unabridged", ".Start",
					"android.intent.action.VIEW", null, "%s/808595524", text);


		// Sorry, we don't have that here.
		default:
			return null;

		}
	}
}
