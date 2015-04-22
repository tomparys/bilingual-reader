package cz.metaverse.android.bilingualreader.helper;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

/**
 *
 * Helper class that serves to open a dictionary in search of a given text.
 *
 */
public enum Dictionary {

	// Available dictionaries
	aard ("Aard Dictionary", "aarddict.android", ".Article", "android.intent.action.SEARCH", "query", "%s"),

	aard_lookup ("Aard Dictionary Lookup", "aarddict.android", ".Lookup", "android.intent.action.SEARCH",
							"query", "%s"),

	colordict ("ColorDict Old Style"),

	colordict_old ("ColorDict Old Style", "com.socialnmobile.colordict", ".activity.Main",
							"android.intent.action.SEARCH", "query", "%s"),

	fora ("Fora Dictionary", "com.ngc.fora", ".ForaDictionary", "android.intent.action.SEARCH",
							"query", "%s"),

	free_dictionary_org ("Free Dictionary . org", "org.freedictionary", ".MainActivity",
							"android.intent.action.VIEW", null, "%s"),

	lingo_quiz_lite ("Lingo Quiz Lite", "mnm.lite.lingoquiz", ".ExchangeActivity",
							"lingoquiz.intent.action.ADD_WORD", "EXTRA_WORD", "%s"),

	lingo_quiz ("Lingo Quiz", "mnm.lingoquiz", ".ExchangeActivity", "lingoquiz.intent.action.ADD_WORD",
							"EXTRA_WORD", "%s"),

	leo ("LEO Dictionary", "org.leo.android.dict", ".LeoDict", "android.intent.action.SEARCH", "query", "%s"),

	abbyy ("ABBYY Lingvo", "com.abbyy.mobile.lingvo.market", null,
							"com.abbyy.mobile.lingvo.intent.action.TRANSLATE",
							"com.abbyy.mobile.lingvo.intent.extra.TEXT", "%s"),

	popup ("Popup Dictionary", "com.barisatamer.popupdictionary",
							".MainActivity", "android.intent.action.VIEW", null, "%s"),

	merriam_webster_unabridged ("Merriam-Webster's Unabridged",
							"com.slovoed.noreg.merriam_webster.english_english_unabridged", ".Start",
							"android.intent.action.VIEW", null, "%s/808595524");


	// Dictionary API attributes
	private final String dictionaryName;
	private final String packageName;
	private final String className;
	private final String intentAction;
	private final String intentKey;
	private final String intentDataPattern;
	private boolean hasAttributes; // Indicates whether the following attributes are filled in or not.

	/**
	 * Constructor filling the attributes that will be automagically turned into an intent.
	 */
	private Dictionary(String dictionaryName, String packageName, String className, String intentAction,
							String intentKey, String intentDataPattern) {

		this.dictionaryName = dictionaryName;
		this.packageName = packageName;
		this.className = className;
		this.intentAction = intentAction;
		this.intentKey = intentKey;
		this.intentDataPattern = intentDataPattern;
		this.hasAttributes = true;
	}

	/**
	 * Constructor for dictionaries with custom intent creation code.
	 */
	private Dictionary(String dictionaryName) {
		this(dictionaryName, null, null, null, null, null);
		hasAttributes = false;
	}


	/**
	 * Produces an intent to open a dictionary in search of a given text
	 * @param text	Text to search
	 * @return		Android Intent that opens a dictionary
	 */
	public Intent getIntent(String text) {
		if (hasAttributes) {
			// Automatic intent creation from attributes
			return getIntentFromAttributes(text);
		} else {
			// Manual intent creation from custom code
			Intent intent;
			switch (this) {

			// ColorDict / GoldenDict
			case colordict:
				intent = new Intent("colordict.intent.action.SEARCH");
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

	@Override
	public String toString() {
		return dictionaryName;
	}


	/**
	 * This method produces an Intent from the given attributes.
	 * @param text	Text to be searched for.
	 * @return		Intent that launches dictionary in search of a given text.
	 */
	private Intent getIntentFromAttributes(String text) {
		Intent intent = new Intent(intentAction);
		if (packageName != null) {
			if (className != null) {
				String classAddress = className;
				if (classAddress.startsWith(".")) {
					classAddress = packageName + classAddress;
				}
				intent.setComponent(new ComponentName(
					packageName, classAddress
				));
			}
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		text = intentDataPattern.replace("%s", text);
		if (intentKey != null) {
			return intent.putExtra(intentKey, text);
		} else {
			return intent.setData(Uri.parse(text));
		}
	}
}
