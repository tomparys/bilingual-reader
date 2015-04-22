package cz.metaverse.android.bilingualreader.helper;

import java.util.HashMap;
import java.util.Map;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

/**
 *
 * This class specializes in creating Intents that launch dictionaries in search for a given text.
 *
 */
public class DictionaryAPI {

	// Map that contains previously created instances of each dictionary API
	public static Map<String, DictionaryAPI> singletonMap;

	// Dictionary API attributes
	public final String dictionaryName;
	public final String packageName;
	public final String className;
	public final String intentAction;
	public final String intentKey;
	public final String intentDataPattern;


	/**
	 * Static method to get the dictionary API, uses a "mapped singleton" pattern.
	 */
	public static DictionaryAPI getApi(String dictionaryName, String packageName, String className,
									String intentAction, String intentKey, String intentDataPattern) {
		// Create the map if it hasn't been yet
		if (singletonMap == null) {
			singletonMap = new HashMap<String, DictionaryAPI>();
		}

		// If this dictionary API is called for the first time, create it
		if (!singletonMap.containsKey(dictionaryName)) {
			singletonMap.put(dictionaryName, new DictionaryAPI(dictionaryName, packageName, className,
																intentAction, intentKey, intentDataPattern));
		}

		// Return the appropriate dictionary API
		return singletonMap.get(dictionaryName);
	}

	/**
	 * Static method that gets the dictionary API and then asks it to produce the wanted Intent
	 * to launch the dictionary in search for a given *text*.
	 */
	public static Intent getIntent(String dictionaryName, String packageName, String className,
			String intentAction, String intentKey, String intentDataPattern, String text) {
		DictionaryAPI api = DictionaryAPI.getApi(dictionaryName, packageName, className, intentAction,
				intentKey, intentDataPattern);
		return api.getIntent(text);

	}


	/**
	 * Constructor.
	 */
	private DictionaryAPI(String dictionaryName, String packageName, String className, String intentAction,
							String intentKey, String intentDataPattern) {

		this.dictionaryName = dictionaryName;
		this.packageName = packageName;
		this.className = className;
		this.intentAction = intentAction;
		this.intentKey = intentKey;
		this.intentDataPattern = intentDataPattern;
	}

	/**
	 * And voila, the gist of the matter - method that produces the Intent.
	 * @param text	Text to be searched for.
	 * @return		Intent that launches dictionary in search of a given text.
	 */
	public Intent getIntent(String text) {
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
