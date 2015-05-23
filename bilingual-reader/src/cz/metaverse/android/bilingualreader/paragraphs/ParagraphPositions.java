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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;

/**
 *
 * This class is devoted to the difficult task of finding the position of each paragraph in a given
 *  chapter of an ebook.
 * The mentioned position is the height from the top of the document/web page at which the paragraph starts
 *  when the document is displayed inside the WebView in one of the panels of our application.
 *
 * To find out this position of each paragraph, we utilize a separate WebView that is hidden behind the one
 *  that displays the content that user sees.
 * The hidden WebView is our specially extended ParagraphPositionsWebView.
 *
 * When user activates the "Sync Scrolling" function of our application, we first try to find out if we have
 *  computed the paragraph positions previously and stored them inside the SQLite database.
 * If we have not, we compute the paragraph positions thus:
 *  - We parse the HTML code of the ebook chapter looking for <p> and <h1>/<h2>/<h3> opening tags
 *     (we treat all these tags interchangeably, since they all are used for readable text):
 *  - Upon finding a new opening tag, we evaluate whether in this new tag a readable text is contained
 *      (after removing nested tags and html entities), if not, we discard it,
 *      because it does not constitute a paragraph.
 *  - If the tag contains readable text, we load all the HTML code preceding it into the hidden WebView.
 *  - WebView renders the given HTML code and reports back the height of the resulting document.
 *  - We note how high the document is preceding the new opening tag - and we save this as the position
 *     of the paragraph contained in the found opening tag.
 *  - Repeat until all text-containing tags are exhausted.
 *
 */
public class ParagraphPositions {

	/*
	 * Static: Used to access the singleton-array.
	 */
	private static SparseArray<ParagraphPositions> paragraphPositionsInstances;


	/*
	 * Fields that do _not_ change between individual runs:
	 */
	private int id; // Id in the singleton-array - denotes to which panel the instance belongs.

	// Regular expression matchers - faster then creating regular expression strings each time.
	private Matcher matcherHeadlineTag;
	private Matcher matcherHeadlineEndTag;


	/*
	 * Fields that change:  Make absolutely, POSITIVELY, ___SURE___ to zero them all out in start() !!!
	 */
	private boolean active;

	// What data to display and where
	private String fileUrl;  // It's a URL so it has to start with "file:///".
	private StringBuilder html;
	private ParagraphPositionsWebView webView; // Hidden WebView where we'll be displaying the data.

	// Fields of progress
	private int startFrom;	 // Length of the part of the HTML code we've already gone through.
	private int paragraphNo; // Paragraph number of the paragraph for which we're finding position now.

	// Fields used to save time so as not to search for the same tags repeatedly if we don't have to
	private int firstPSpace;	// The position of the next found "<p " after startFrom
	private int firstPBracket;	//	same for "<p>"
	private int firstHeadline;	//	same for <hX>, where X is a decimal
	// The position and type of the last "next tag" that was searched for.
	private Tag nextTag;

	// Final data - positions of paragraphs.
	private List<Integer> positions;


	/**
	 * Facilitates the singleton-array pattern.
	 * This is the only way to obtain an instance, due to the private constructor.
	 */
	public static ParagraphPositions instance(int panel) {
		if (paragraphPositionsInstances == null) {
			paragraphPositionsInstances = new SparseArray<ParagraphPositions>(2);
		}
		ParagraphPositions instance = paragraphPositionsInstances.get(panel, null);
		if (instance == null) {
			instance = new ParagraphPositions(panel);
			paragraphPositionsInstances.put(panel, instance);
		}
		return instance;
	}

	/**
	 * Private constructor so as to force the singleton-array pattern.
	 */
	private ParagraphPositions(int id) {
		this.id = id;
	}

	/**
	 * ParagraphPositionsWebView calls this method to report back the contentHeight of the newly
	 * rendered data. We note it as the height of the paragraph we're currently figuring out.
	 */
	public void reportContentHeight(int contentHeight) {
		/*Log.d("kokoska", "[" + id + "]{" + paragraphNo
				+ "} Report contentHeight: " + contentHeight); /**/

		// Save the contentHeight at the *paragraphNo*-th position in the *positions* list.
		// Which indicates the paragraph no. *paragraphNo* is at the *contentHeight* position.
		positions.add(contentHeight);
		paragraphNo += 1;

		// This paragraph is done, procede with the next one.
		goOn();
	}

	/**
	 * The main method of this class. Iterates over all the text-containing <p> or <hX> tags,
	 * checks whether they correspond to paragraphs, and if so, sends data to the hidden WebView
	 * to figure out the paragraph's position.
	 */
	private void goOn() {
		// Use the information gained in previous goOn() to save ourselves looking for the next tag if possible.
		//  If the previously found nextTag is at -1 it means there are no more tags and we shouldn't look.
		if (nextTag == null || nextTag.position != -1 && nextTag.position < startFrom + 1) {
			nextTag = findNextTag(startFrom, nextTag);
		}
		startFrom = nextTag.position;

		/*Log.d("hugo", "[" + id + "] startFrom: " + startFrom + "(" + firstPSpace + ", " + firstPBracket
				+ ", " + firstHeadline + ")"); /**/

		if (startFrom != -1) {
			// Test if this <p> or <hX> contains text or not.

			// Find the position of the corresponding closing tag.
			int endTagPosition = 0;
			if (!nextTag.isHeadline) {
				endTagPosition = html.indexOf("</p", startFrom + 1); // Returns -1 if not found
			} else {
				if(matcherHeadlineEndTag.find(startFrom + 1)) {
					endTagPosition = matcherHeadlineEndTag.start();
			    } else {
			    	endTagPosition = -1; // To indicate the closing tag was not found, same as indexOf function
			    }
			}

			// Find a next <p or <hX tag
			nextTag = findNextTag(startFrom, nextTag);

			// If the ending tag was not found at all
			// or if the next <p or <hX tag is earlier than the closing tag.
			if (endTagPosition == -1 || nextTag.position != -1 && nextTag.position < endTagPosition) {
				// The closing tag is probably missing
				// Use the position of the next <p or <hX tag.
				endTagPosition = nextTag.position;
			}

			// If the endTagPosition is still -1 it means there are no more tags at all,
			// but we're still inside one last open tag (that's missing it's ending tag).
			if (endTagPosition == -1) {
				// Set endTagPosition to the very end of the html file, so we find out
				// the height of the entire document at least.
				endTagPosition = html.length();
			}


			/*Log.d("hugo", "[" + id + "] substring to position: " + endTagPosition
					+ " (tag.position:" + tag.position + ")"); /**/

			// Get the content inside the current open tag.
			String text = html.substring(html.indexOf(">", startFrom) + 1,
					endTagPosition);

			// Removing all nested tags, new lines and HTML entities (&nbsp; etc.),
			// so we can find out if the content contains any displayed readable characters.
			//  TODO It may be more efficient to reuse a compiled Pattern.
			text = text.replaceAll("<[^<>]*>", "");	// Nested tags
			text = text.replaceAll("\\n", "");		// New lines
			text = text.replaceAll("&\\w*;", "");	// HTML entities (e.g. &nbsp;)

			// Find out if the content inside the tag contains any displayed readable characters.
			if (!text.matches(".*\\w.*")) { // TODO It may be more efficient to reuse a compiled Pattern.
				// Paragraph with no readable characters:

				//Log.d("kokoska", "[" + id + "]{" + paragraphNo + "} Non-text paragraph [" + text + "]");

				// Start this method again from the top and do nothing else.
				goOn();
			} else {
				// Paragraph with readable characters:

				//Log.d("kokoska", "[" + id + "]{" + paragraphNo + "} Text paragraph [" + text + "]");

				// Load all the HTML from the start up to the current open tag,
				//  to find out where the current tag would have been displayed.
				// Provides also the original *fileUrl* so that CSS files can be loaded properly from
				//  relative addresses.
				webView.loadDataWithBaseURL(fileUrl, html.substring(0, startFrom), "text/html", null, null);
			}
		} else {
			// All is well that ends well. We have found out the positions of the paragraphs.
			active = false;

			Log.d("hugo", "[" + id + "] Ending ParagraphPositions No. " + id + ", paragraphs: " + paragraphNo);
			//Log.d("hugo", "    + positions.toString());

			// TODO Save and use the gained data for good.
			Toast.makeText(ReaderActivity.debugContext, "\n\n\n\nParagraph Positions found for panel "
					+ id + "\n\n\n\n", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Find the next "<p" or "<hX" html tag
	 * @param lStartFrom  Position in the html file to start the search from
	 * @return  Tag object with: Position of the next tag or -1 if there is none,
	 * 								and boolean saying whether the tag is <hX tag or not.
	 */
	private Tag findNextTag(int lStartFrom, Tag reusedTag) {
		// Search for the next "<p " tag
		// Search again if:
		// 1. The following tag found before is now behind (startFrom +1)
		// 2. We previously found there are still some of these tags in the document (!= -1)
		if (firstPSpace < lStartFrom + 1 && firstPSpace != -1) {
			firstPSpace = html.indexOf("<p ", lStartFrom + 1);
		}
		// Search for the next "<p>" tag
		if (firstPBracket < lStartFrom + 1 && firstPBracket != -1) {
			firstPBracket = html.indexOf("<p>", lStartFrom + 1);
		}
		// Search for the next "<hX" tag
		if (firstHeadline < lStartFrom + 1 && firstHeadline != -1) {
		    if(matcherHeadlineTag.find(lStartFrom + 1)) {
		    	firstHeadline = matcherHeadlineTag.start();
		    } else {
		    	firstHeadline = -1; // So we do not search for <hX tags again
		    }
		}

		// Tag reusing - create new only if necessary
		if (reusedTag == null) {
			reusedTag = new Tag();
		}

		// Find the lowest non -1 value of the three:
		lStartFrom = firstPSpace;
		lStartFrom = (firstPBracket != -1 && firstPBracket < lStartFrom || lStartFrom == -1) ? firstPBracket : lStartFrom;
		if           (firstHeadline != -1 && firstHeadline < lStartFrom || lStartFrom == -1) {
			lStartFrom = firstHeadline;
			return reusedTag.set(lStartFrom, true);
		} else {
			return reusedTag.set(lStartFrom, false);
		}
	}

	/**
	 * Starts off the ParagraphPositions algorithm with a given panel fragment and page URL.
	 * @param fragment	Fragment of the panel that we're dealing with.
	 * @param sFileurl	URL of the page to be examined.
	 */
	public void start(Fragment fragment, String sFileurl) {
		// Make sure the file URL does not contain double slashes.
		sFileurl = sFileurl.replace("//", "/");
		sFileurl = sFileurl.replace("file://", "file:///"); // To repair the damage done to the beginning.

		Log.d("hugo", "[" + id + "] Starting ParagraphPositions No. " + id);
		Log.d("hugo", "    path: " + fileUrl);

		/*
		 * Make absolutely, POSITIVELY, ___SURE___ to zero-out all the necessary fields.
		 */
		active = true;

		// What data to display and where
		fileUrl = sFileurl;
		html = new StringBuilder();
		webView = null; //(ParagraphPositionsWebView) fragment.getView().findViewById(R.id.ParagraphPositionsViewport);
		                // The ParagraphPositionsViewport was disabled due to this method not being used.
		webView.setParagraphPositionsInstance(this);

		// Fields of progress
		startFrom = 0;
		paragraphNo = 0;

		// Fields used to save time so as not to search for the same tags repeatedly if we don't have to
		firstPSpace = 0;
		firstPBracket = 0;
		firstHeadline = 0;

		// The position and type of the last "next tag" that was searched for.
		nextTag = null;

		// Final data - positions of paragraphs.
		positions = new ArrayList<Integer>();

		/* end */

		// Read the entire HTML file into a StringBuilder html variable.
		try {
			String line;
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fileUrl.replace("file:///", "")));
			while ((line = bufferedReader.readLine()) != null) {
				html.append(line).append("\n");
			}
			bufferedReader.close();

		} catch (FileNotFoundException e) {
			Toast.makeText(fragment.getActivity(), R.string.error_LoadPage, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(fragment.getActivity(), R.string.error_readingFile, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		//Log.d("alfons", "[" + id + "] HTML: " + html);

		// Inject CSS: no bottom margin/padding, padding-top 1px so that the page has always non-zero height.
		String css = " style=\"margin-bottom: 0px; padding-bottom: 0px; padding-top: 1px;\" ";
		int bodyPosition = html.indexOf("<body ");
		int bodyEndPosition = html.indexOf(">", bodyPosition);
		html.insert(bodyEndPosition, css);

		// Initialize Regexp patterns and matchers for future use, but only if necessary:
		if (matcherHeadlineTag == null || matcherHeadlineEndTag == null) {
		    Pattern pattern = Pattern.compile("<h\\d");
		    matcherHeadlineTag = pattern.matcher(html);
		    pattern = Pattern.compile("</h\\d");
		    matcherHeadlineEndTag = pattern.matcher(html);
		}

		// Find the first paragraph, before goingOn, because if the first time we only display the header,
		// the contentHeight might actually be 0, in which case we'll never get a callback from
		// ParagraphPositionsWebView to continue rendering.
		//  -- Not needed, injecting CSS above so far seems to sufficiently solve the issue.
		//startFrom = html.indexOf("<p ", startFrom + 1);

		// Start the first round.
		goOn();
	}


	/**
	 * Inner class to pass the position and type of the next found tag between methods.
	 */
	private class Tag {
		public int position;
		public boolean isHeadline;

		public Tag set(int position, boolean isHeadline) {
			this.position = position;
			this.isHeadline = isHeadline;
			return this;
		}
	}

	/**
	 * Id of this ParagraphPositions instance - correlates to the panel which this instance belongs to.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Tells whomever it may concern if this instance of the ParagraphPositions algorithm is in progress.
	 */
	public boolean isActive() {
		return active;
	}
}
