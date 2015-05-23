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

package cz.metaverse.android.bilingualreader.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.util.Log;
import cz.metaverse.android.bilingualreader.R;

/**
 *
 * Class that holds the Visual Options for displaying e-books.
 * It also constructs the appropriate CSS styles, and is able to
 *  load and save its own state from SharedPreferences.
 *
 */
public class VisualOptions {

	// Logging
	private static final String LOG = "VisualOptions";

	// This field indicates whether the options should be applied at all,
	// or the default CSS styles of the e-books should be used.
	public boolean applyOptions;
	// This indicates whether the original CSS styles of the e-books should be removed or not.
	public boolean removeOriginalStyles;

	/* Visual Options */
	public int textColor;
	public int bgColor;
	public int font;
	public int fontSize;
	public int textAlign;
	public int lineHeight;
	public int margins;

	// Constructed CSS styles from the visual options.
	private String css;

	/**
	 * Returns the CSS style corresponding to the Visual Options.
	 */
	public String getCSS(Context context) {
		if (css == null) {
			css = constructCSS(context);
		}
		return css;
	}

	/**
	 * Constructs the CSS style from the Visual Options.
	 */
	private String constructCSS(Context context) {
		if (applyOptions) {
			Resources res = context.getResources();
			String sTextColor = res.getStringArray(R.array.textColor_value)[textColor];
			String sBgColor = res.getStringArray(R.array.bgColor_value)[bgColor];
			String sFont = res.getStringArray(R.array.font)[font];
			String sFontSize = res.getStringArray(R.array.fontSize)[fontSize];
			String sTextAlign = res.getStringArray(R.array.textAlign)[textAlign];
			String sLineHeight = res.getStringArray(R.array.lineHeight)[lineHeight];
			String sMargins = res.getStringArray(R.array.margins)[margins];

			String css = "\n\n<style type=\"text/css\">\n"
					+ "body {\n"
					+ "		color:" + sTextColor + ";\n"
					+ "		background-color:" + sBgColor + ";\n"
					+ "		margin-left:" + sMargins + ";\n"
					+ "		margin-right:" + sMargins + ";\n"
					+ "}\n\n"
					+ "p {\n"
					+ "		font-family:" + sFont + ";\n"
					+ "		font-size:" + sFontSize + ";\n"
					+ "		text-align:" + sTextAlign + ";\n"
					+ "		line-height:" + sLineHeight + ";\n"
					+ "}\n\n"
					+ "a:link {color:" + sTextColor + ";}\n"
					+ "</style>\n\n";

			return css;
		}
		return "";
	}

	/**
	 * Save state.
	 * @param editor	SharedPreferences editor instance
	 */
	public void saveState(Editor editor) {
		Log.d(LOG, LOG + ".saveState");

		editor.putBoolean("vo_applyOptions", applyOptions);
		editor.putBoolean("vo_removeOriginalStyles", removeOriginalStyles);

		editor.putInt("vo_textColor", textColor);
		editor.putInt("vo_bgColor", bgColor);
		editor.putInt("vo_font", font);
		editor.putInt("vo_fontSize", fontSize);
		editor.putInt("vo_textAlign", textAlign);
		editor.putInt("vo_lineHeight", lineHeight);
		editor.putInt("vo_margins", margins);

	}

	/**
	 * Load state.
	 * @param preferences  SharedPreferences instance
	 * @param creatingActivity  Whether or not the activity is just being created.
	 * @return				successfulness
	 */
	public boolean loadState(SharedPreferences preferences, boolean creatingActivity) {
		Log.d(LOG, LOG + ".loadState");

		applyOptions = preferences.getBoolean("vo_applyOptions", false);
		removeOriginalStyles = preferences.getBoolean("vo_removeOriginalStyles", false);

		textColor = preferences.getInt("vo_textColor", 0);
		bgColor = preferences.getInt("vo_bgColor", 0);
		font = preferences.getInt("vo_font", 0);
		fontSize = preferences.getInt("vo_fontSize", 0);
		textAlign = preferences.getInt("vo_textAlign", 0);
		lineHeight = preferences.getInt("vo_lineHeight", 0);
		margins = preferences.getInt("vo_margins", 0);
		return true;
	}
}
