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

package cz.metaverse.android.bilingualreader.enums;

/**
 *
 * Enum describing the method which is used to keep the WebViews synchronized when ScrollSync is activated.
 *
 */
public enum ScrollSyncMethod {
	none, proportional, linear, syncPoints;


	/**
	 * Converts string to the appropriate enum. Or null.
	 */
	public static ScrollSyncMethod fromString(String string) {
		ScrollSyncMethod method = null;

		if (string != null) {
			try {
				method = ScrollSyncMethod.valueOf(string);
			} catch (IllegalArgumentException e) {}
		}

		return method;
	}
}
