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

package cz.metaverse.android.bilingualreader.paragraphs;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 *
 * Custom extension of WebView that allows us to find out the position of each paragraph.
 * Used exclusively with the ParagraphPositions algorithm.
 *
 */
public class ParagraphPositionsWebView extends WebView {

	// The height of the rendered content in the last run.
	int previousContentHeight = 0;

	// Callback instance, used to report the new contentHeight.
	ParagraphPositions paragraphPositionsInstance = null;


	/**
	 * Getting the content height of the currently loaded data is problematic, because it is computed
	 * very late in the lifecycle. Even at the time the onPageFinished() method is invoked, the contentHeight
	 * still does not represent the current loaded data, but instead reports the height of the previously
	 * rendered document. The checking of getProgress() == 100 does not work for the same reason.
	 *
	 * Through googling and trial-and-error I found out the only chance to get the contentHeight is
	 *  at the time of the actual drawing.
	 * When content in the WebView changes, it starts off a series of several onDraw calls, usually between
	 *  5 and 20, sometimes more.
	 * Often at the time of most of the onDraw calls, the getContentHeight() method still reports height
	 *  of the previously rendered document, and only the time of the very last onDraw call we can get
	 *  the actual contentHeight.
	 *
	 * It is also possible to override the invalidate() method, but this way we can stop the onDraw
	 *  from actually drawing and unnecessarily consuming resources, since this WebView is hidden.
	 */
	@Override
	protected void onDraw(android.graphics.Canvas canvas) {
		// We never really need to actually draw the content, the WebView is hidden in the background.
		//super.onDraw(canvas);

		// Only if we're in the process of finding Paragraph Positions.
		if (paragraphPositionsInstance != null && paragraphPositionsInstance.isActive()) {
			// Logging commented out
			/*Log.d("hugo", "[" + paragraphPositionsInstance.getId() + "] onDraw: " + getContentHeight()
					+ ", Progress = 100: " + (getProgress() == 100)); /**/

			// If the getContentHeight() returns the same value as the height of the previous rendered
			//  document, do nothing.
			// If the progress hasn't yet reached 100, do nothing as well,
			//  the new contentHeight might not be final for this rendering.
			if (previousContentHeight != getContentHeight() && getProgress() == 100) {
				previousContentHeight = getContentHeight();

				// This is the final contentHeight for the loaded data. Report it back.
				paragraphPositionsInstance.reportContentHeight(previousContentHeight);
			}
		}
	}

	/**
	 * Used by a ParagraphPositions instance to tell the WebView where to report the results.
	 */
	public void setParagraphPositionsInstance(ParagraphPositions paragraphPositions) {
		paragraphPositionsInstance = paragraphPositions;
	}


	/**
	 * Constructor in case of XML initialization.
	 * @param context		Activity context
	 * @param attributeSet	Set of attributes from the XML declaration
	 */
	public ParagraphPositionsWebView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	/**
	 * Constructor in case of programmatic initialization.
	 * @param context Activity context
	 */
	public ParagraphPositionsWebView(Context context) {
		this(context, null);
	}
}
