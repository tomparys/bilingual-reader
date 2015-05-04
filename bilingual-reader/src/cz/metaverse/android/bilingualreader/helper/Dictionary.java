package cz.metaverse.android.bilingualreader.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;

/**
 *
 * Helper class that serves to open a dictionary in search of a given text.
 *
 */
public class Dictionary implements Comparable<Dictionary> {

	private static final String LOG = "Dictionary";

	// Statically stored the default dictionary.
	private static Dictionary defaultDictionary;

	// Fields of the dictionary.
	public String packageName;
	public String name;
	public Drawable icon;


	public Dictionary(String packageName) {
		this.packageName = packageName;
	}

	public Dictionary(String name, String packageName, Drawable icon) {
		this.packageName = packageName;
		this.name = name;
		this.icon = icon;
	}


	/**
	 * Opens the dictionary by making starting its Intent.
	 * @param activity	Activity instance for launching other Activities
	 * @param text		Text to search for
	 */
	public void open(Activity activity, String text) {
		if (packageName != null) {
			activity.startActivity(makeIntent(packageName, text));
		}
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Dictionaries are equal if their packageNames are equal.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Dictionary) {
			if (packageName != null) {
				return packageName.equals(((Dictionary) o).packageName);
			}
		}
		return false;
	}

	/**
	 * Hash code returns the hash code of the packageName String.
	 */
	@Override
	public int hashCode() {
		if (packageName != null) {
			return packageName.hashCode();
		}
		return 0;
	};

	/**
	 * For sorting Dictionaries based on their name.
	 */
	@Override
	public int compareTo(Dictionary another) {
		if (name != null) {
			return name.compareTo(another.name);
		}
		return 0;
	}



	// ============================================================================================
	//		Static methods
	// ============================================================================================

	/**
	 * Returns apps that repond to text-based Intent.
	 * @param activity	Needed to test the activities
	 * @return			List of available apps that could be dictionaries
	 */
	public static List<Dictionary> getAvailable(Activity activity) {
		// Text-based Intent
		Intent testIntent = new Intent(Intent.ACTION_SEND);
		testIntent.setType("text/plain");
		testIntent.putExtra(android.content.Intent.EXTRA_TEXT, "test");

		// Ask the PackageManager to give us apps that would respond to such a text-based Intent.
		PackageManager pm = activity.getPackageManager();
		List<ResolveInfo> resolveInfo = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY);

		List<Dictionary> dictionaries = new ArrayList<Dictionary>();

		for (ResolveInfo ri : resolveInfo) {
			ApplicationInfo ai = ri.activityInfo.applicationInfo;
			dictionaries.add(new Dictionary(ai.loadLabel(pm).toString(), ai.packageName.toString(),
					pm.getApplicationIcon(ai)));
		}

		// Sort based on app name
		Collections.sort(dictionaries);
		return dictionaries;
	}

	/**
	 * Opens the default dictionary (if one is set and available) and searches for given text.
	 * @return	Whether a dictionary was launched or not.
	 */
	public static boolean openDefault(ReaderActivity activity, String text) {
		if (getDefault(activity) != null) {
			getDefault(activity).open(activity, text);
			return true;
		}
		return false;
	}

	/**
	 * Sets the default dictionary into preferences.
	 * @param activity	ReaderActivity instance to get its shared preferences
	 */
	public static void setDefault(ReaderActivity activity, Dictionary dictionary) {
		// Save the value to shared preferences
		SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
		// Save the packageName of the given dictionary into preferences
		editor.putString(activity.getString(R.string.putString_defaultDictionary), dictionary.packageName);
		editor.commit();

		// Keep it saved here as well
		defaultDictionary = dictionary;
	}

	/**
	 * Returns the default dictionary from preferences.
	 * @param activity	ReaderActivity instance to get its shared preferences
	 * @return			If no default is set, or the default one is no longer available, returns null.
	 */
	public static Dictionary getDefault(ReaderActivity activity) {
		if (defaultDictionary == null) {
			// Load default dictionary from preferences.
			SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
			String defaultDictionaryString = preferences.getString(
					activity.getString(R.string.putString_defaultDictionary), null);

			Log.d(LOG, LOG + ".getDefault: defaultDictionaryString=" + defaultDictionaryString);

			if (defaultDictionaryString != null) {
				if (respondsPackageToIntent(activity, defaultDictionaryString)) {
					defaultDictionary = new Dictionary(defaultDictionaryString);
				}
			}
		}

		return defaultDictionary;
	}

	/**
	 * Checks if a given package responds to text-based intent.
	 * @param activity	Activity is needed to test if the Intents are available
	 * @return
	 */
	private static boolean respondsPackageToIntent(Activity activity, String packageName) {
		List<ResolveInfo> resolveInfo = activity.getPackageManager().
				queryIntentActivities(makeIntent(packageName, "test"), PackageManager.MATCH_DEFAULT_ONLY);

		return resolveInfo != null && resolveInfo.size() > 0;
	}

	/**
	 * Creates a text-based Intent with a given text.
	 */
	private static Intent makeIntent(String packageName, String text) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
		intent.setPackage(packageName);
		return intent;
	}



	/* Code that launches ColorDict in an overlay transparent window instead of as a full-screen Activity.
	 * It is the only thing this new method of picking dictionaries can not do.
	 * Leaving it here to possibly re-incorporate it later.

			intent = new Intent("colordict.intent.action.SEARCH");
			intent.putExtra("EXTRA_QUERY", text);
			//intent.putExtra("EXTRA_HEIGHT", 600 ); //"match_parent"
			//intent.putExtra("EXTRA_GRAVITY", Gravity.CENTER);
			//intent.putExtra("EXTRA_MARGIN_LEFT", 50);
			//intent.putExtra("EXTRA_MARGIN_RIGHT", 50);
			return intent;
	 */
}
