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


This file incorporates work covered by the following copyright and permission notice:


The MIT License (MIT)

Copyright (c) 2013, V. Giacometti, M. Giuriato, B. Petrantuono

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package cz.metaverse.android.bilingualreader.manager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.enums.BookPanelState;
import cz.metaverse.android.bilingualreader.enums.ScrollSyncMethod;
import cz.metaverse.android.bilingualreader.helper.DontShowAgain;
import cz.metaverse.android.bilingualreader.helper.ScrollSyncPoint;
import cz.metaverse.android.bilingualreader.panel.BookPanel;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * The Main logical and data class that governs our application by the Grace of God Emperor of Code.
 * The Louis XIV Sun King of classes, « L'application ? C'est moi. »
 *
 * - Functions as a central hub that connects data with Views the user sees and interacts with.
 * - Delegates some of its work on two PanelHolder instances that work more closely with their panels.
 * - Manages synchronized reading.
 * - And much much more; a limited time offer for just $39.99, call now!
 *
 */
public class Governor {

	/* Static */
	private static final String LOG = "Governor";

	// The magic number - the number of panels of our application.
	// Do NOT change this number, the application is not built for it.
	public static final int N_PANELS = 2;

	// A static instance of this class for the Singleton pattern herein employed.
	private static Governor governorInstance;

	/* Dynamic */
	private ReaderActivity activity;
	private PanelHolder[] panelHolder;
	public DontShowAgain dontDisplayAgain;

	/* Sync */
	private boolean chapterSync;
	private boolean readingBilingualEbook;

	// Scroll Sync
	private boolean scrollSync;
	private ScrollSyncPoint[] scrollSyncPoint;


	/* State holding - these fields are public, because they serve to interconnect with other classes.
	 * And because unnecessary getters and setters are costly on Android. */
	// For proper operation of the Back system button.
	public PanelHolder notesDisplayedLastIn;

	// For hiding panels.
	public PanelHolder hiddenPanel;



	// ============================================================================================
	//		Singleton pattern and constructor
	// ============================================================================================

	/**
	 * Singleton-pattern getter static method.
	 * @param activity	The ReaderActivity instance that's asking for an instance.
	 * @param preferences  SharedPreferences instance from which to load data
	 * @param creatingActivity  Whether or not the activity is just being created.
	 */
	public static Governor loadAndGetSingleton(ReaderActivity activity, SharedPreferences preferences,
			boolean creatingActivity) {
		if (governorInstance == null) {
			governorInstance = new Governor(activity);
		}

		governorInstance.setActivity(activity);

		governorInstance.loadState(preferences, creatingActivity);

		return governorInstance;
	}

	/**
	 * Private constructor for the singleton pattern.
	 * @param a  The ReaderActivity from which this is launched
	 */
	private Governor(ReaderActivity activity) {
		prepareComplexClasses();

		// Activity is set right after the constructor in the getSingleton() method, no need to do it here.
	}

	/**
	 * Update the Governor with a link to the current ReaderActivity.
	 */
	private void setActivity(ReaderActivity activity) {
		this.activity = activity;

		for (PanelHolder ph : panelHolder) {
			if (ph != null) {
				ph.setActivity(activity);
			}
		}
	}

	public ReaderActivity getActivity() {
		return activity;
	}

	/**
	 * Creates and saves two PanelHolders and one DontDisplayAgain class for basic operation of our application.
	 * Invoked upon creation or if Android system closed one of the instances to free memory.
	 */
	private void prepareComplexClasses() {
		panelHolder = new PanelHolder[] {
				new PanelHolder(0, activity, this),
				new PanelHolder(1, activity, this)};
		panelHolder[0].setSisterPanelHolder(panelHolder[1]);
		panelHolder[1].setSisterPanelHolder(panelHolder[0]);

		dontDisplayAgain = DontShowAgain.getInstance();
	}

	/**
	 * Checks whether there are any problems with this instance, if for example the Android system
	 * didn't close any important fields that would result in NullPointerExceptions.
	 * @return true if everything appears to be sound
	 */
	private boolean selfCheck() {
		return panelHolder != null && panelHolder[0] != null && panelHolder[1] != null
				&& dontDisplayAgain != null;
	}



	// ============================================================================================
	//		Panel Holders
	// ============================================================================================

	public PanelHolder getPanelHolder(int pos) {
		if (0 <= pos && pos < N_PANELS) {
			return panelHolder[pos];
		}
		return null;
	}

	public void switchPanels() {
		Log.d(LOG, "switchPanels");

		// Switch the panel holder positions.
		PanelHolder temp = panelHolder[0];
		panelHolder[0] = panelHolder[1];
		panelHolder[1] = temp;

		// Notify the panelholders and subsequently the panels of the new positions.
		panelHolder[0].updatePosition(0);
		panelHolder[1].updatePosition(1);

		// Switch the displayed book names in the navigation drawer.
		activity.switchBookNamesInDrawer();
	}



	// ============================================================================================
	//		Panels
	// ============================================================================================

	/**
	 * @return True if only one panel is currently opened.
	 */
	public boolean isOnlyOnePanelOpen() {
		return !panelHolder[0].hasOpenPanel() || !panelHolder[1].hasOpenPanel();
	}

	/**
	 * Is one of the panels currently hidden?
	 */
	public boolean isAnyPanelHidden() {
		return hiddenPanel != null;
	}

	/**
	 * Reappear the currently hidden panel.
	 */
	public void reappearPanel() {
		if (hiddenPanel != null) {
			hiddenPanel.reappearPanel();
		}
	}

	/**
	 * Closes the last opened Metadata/Table of Contents/other non-book content panel view.
	 * @return
	 */
	public boolean closeLastOpenedNotes() {
		if (notesDisplayedLastIn != null) {
			BookPanel bookPanel = notesDisplayedLastIn.getBookPanel();
			if (bookPanel != null && (bookPanel.enumState == BookPanelState.notes
					|| bookPanel.enumState == BookPanelState.metadata)) {

				notesDisplayedLastIn.closePanel();
				notesDisplayedLastIn = notesDisplayedLastIn.getSisterPanelHolder();
				return true;
			}
		}
		return false;
	}

	/**
	 * Changes the screen area ratio between the two opened panels.
	 * @param weight
	 */
	public void changePanelsWeight(float weight) {
		if (panelHolder[0].hasOpenPanel() && panelHolder[1].hasOpenPanel()) {

			panelHolder[0].changePanelWeight(1 - weight);
			panelHolder[1].changePanelWeight(weight);
		}
	}

	/**
	 * Removes the panels from the FragmentManager of the Activity. The panels will still exist,
	 * but won't be displayed.
	 */
	public void removePanels() {
		for (PanelHolder ph : panelHolder) {
			if (ph.hasOpenPanel()) {
				activity.removePanel(ph.getPanel());
			}
		}
	}

	/**
	 * Given the position of one panel returns the position of the other.
	 */
	private static int otherPosition(int position) {
		return (position + 1) % N_PANELS;
	}



	// ============================================================================================
	//		Books
	// ============================================================================================

	/**
	 * @return Whether there is audio to be extracted in either of the opened books.
	 */
	public boolean canExtractAudio() {
		/*for (PanelHolder ph : panelHolder) {
			if (ph.canExtractAudio()) {
				return true;
			}
		}*/
		// Functionality has been disabled.
		return false;
	}

	/**
	 * @return true if exactly one book is open
	 */
	public boolean exactlyOneBookOpen() {
		int openedBooks = 0;

		for (PanelHolder ph : panelHolder) {
			if (ph.getBook() != null) {
				openedBooks += 1;
			}
		}
		return openedBooks == 1;
	}



	// ============================================================================================
	//		Scroll Sync
	// ============================================================================================

	/**
	 * Returns whether Scroll Sync is active or not.
	 */
	public boolean isScrollSync() {
		return scrollSync;
	}

	/**
	 *
	 *
	 */
	/**
	 * Activates or deactivates ScrollSync.
	 * @param setScrollSync  The new state we're switching to.
	 * @param checkSyncData  If we're switching Sync ON, this indicates whether we should check if the
	 *         scroll sync data in both WebViews agree with each other, and if they don't reset them.
	 *       - If you've just used the setScrollSyncMethod(), feel free to put false here.
	 *       - If you're setting first boolean to false, put this one true, in case you change
	 *           the original false to true. Just to be safe.
	 * @return  Null if change was not made, if it was, returns whether a new default sync method was set.
	 */
	public Boolean setScrollSync(boolean setScrollSync, boolean checkSyncData) {
		if (!exactlyOneBookOpen()) {
			boolean defaultScrollSyncMethodStarted = false;

			// If we're to check the data, if we're switching scrollSync ON and if it hasn't been before now.
			if (checkSyncData && setScrollSync && !scrollSync) {
				SelectionWebView[] webView = getTwinWebViews();

				if (webView != null) {
					// If the Scroll Sync methods and data loaded in the two panels don't agree
					// with each other, reset them out.
					if (!webView[0].areScrollSyncDataCongruentWithSister()) {
						webView[0].resetScrollSync(ScrollSyncMethod.proportional);
						webView[1].resetScrollSync(ScrollSyncMethod.proportional);
						defaultScrollSyncMethodStarted = true;
					}
				}
			}

			// If we did change the scrollSync value, return true that a change happened.
			if (scrollSync != setScrollSync) {
				scrollSync = setScrollSync;
				return defaultScrollSyncMethodStarted;
			}
		}
		return null;
	}

	/**
	 * Flips the state of ScrollSync (active -> inactive, inactive -> active) if possible.
	 * @return Passes results from setScrollSync() (possibly including null) or null if the flip was not attempted.
	 */
	public Boolean flipScrollSync() {
		if (!exactlyOneBookOpen()) {
			return setScrollSync(!scrollSync, true);
		}
		return null;
	}

	/**
	 * Get the currently used ScrollSync method. Returns null if method isn't set yet.
	 */
	public ScrollSyncMethod getScrollSyncMethod() {
		SelectionWebView[] webView = getTwinWebViews();
		if (webView != null) {

			// If ScrollSync is active we can be sure that ScrollSync method and data are the same
			// in both WebViews, so grab it from the first one.
			if (isScrollSync()) {
				return webView[0].getScrollSyncMethod();
			}
			// If ScrollSync isn't active, check first if the ScrollSync data are congruent in both WebViews,
			// if not, we're going to reset the method and data when ScrollSync is activated,
			// thus in that case we return null.
			else {
				if (webView[0].areScrollSyncDataCongruentWithSister()) {
					return webView[0].getScrollSyncMethod();
				}
			}
		}
		return null;
	}

	/**
	 * Sets the Scroll Sync method.
	 * @return True if the Scroll Sync data was successfully set, false otherwise.
	 */
	public boolean setScrollSyncMethod(ScrollSyncMethod ssMethod) {
		if (!exactlyOneBookOpen()) {
			SelectionWebView[] webView = getTwinWebViews();
			if (webView != null) {

				if (ssMethod == ScrollSyncMethod.syncPoints) {
					if (scrollSyncPoint != null && scrollSyncPoint[0] != null && scrollSyncPoint[1] != null) {

						if (scrollSyncPoint[0].computeAndActivateScrollSync(scrollSyncPoint[1], webView)) {
							// If the computation was successful, activate ScrollSync.
							return true;
						}
						else {
							// Position of one book was identical in both books, can't activate.
							Toast.makeText(activity, R.string.Cant_activate_sync_points_msg, Toast.LENGTH_LONG).show();
							return false;
						}
					}
				}
				// Method is NOT syncPoints.
				else {
					webView[0].resetScrollSync();
					webView[1].resetScrollSync();

					webView[0].setScrollSyncMethod(ssMethod);
					webView[1].setScrollSyncMethod(ssMethod);

					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Take the current Scroll positions of both WebViews and save them as a ScrollSyncPoint.
	 */
	public void setScrollSyncPointNow(int point) {
		if (scrollSyncPoint == null) {
			scrollSyncPoint = new ScrollSyncPoint[N_PANELS];
		}

		SelectionWebView[] webView = getTwinWebViews();
		if (webView != null) {
			scrollSyncPoint[point] = new ScrollSyncPoint(webView[0].getScrollY(), webView[1].getScrollY());
		}
	}

	public ScrollSyncPoint[] getScrollSyncPoints() {
		return scrollSyncPoint;
	}

	/**
	 * Erases both Scroll Sync Points.
	 * Done when changing pages or books.
	 */
	public void resetScrollSyncPoints() {
		scrollSyncPoint = null;
	}



	// ============================================================================================
	//		Chapter Sync
	// ============================================================================================

	public boolean isChapterSync() {
		return chapterSync;
	}

	/**
	 * @return Whether the state was changed or if it was already like that.
	 */
	public boolean setChapterSync(boolean value) {
		if (chapterSync != value) {
			chapterSync = value;
			return true;
		}
		return false;
	}

	/**
	 * Flips the state of SynchronizedChapters (active -> inactive, inactive -> active).
	 * @return If the change will be active right now or not (if only one book is open, sync does not work).
	 */
	public boolean flipChapterSync() {
		if (exactlyOneBookOpen()) {
			return false;
		}
		chapterSync = !chapterSync;
		return true;
	}

	// ============================================================================================
	//		Bilingual book handling
	// ============================================================================================

	/**
	 * If books[book] contains more than 1 language, it opens the same book in the other panel, opens the same page
	 *  and sets the first language into the given panel (id book) and the second language into the other.
	 * 	If all goes well it activates Synchronized Chapter.
	 * @param book				id of the panel with the book that we want to open in bilingual ebook mode
	 * @param firstLanguage		id of the first language to open
	 * @param secondLanguage	id of the second language to open
	 * @return
	 */
	public boolean activateBilingualEbook(int book, int firstLanguage, int secondLanguage) {
		boolean ok = true;

		if (firstLanguage != -1) {
			try {
				if (secondLanguage != -1) {
					Log.d(LOG, "activateBilingualEbook - first and second language ready");

					panelHolder[otherPosition(book)].openBook(
							panelHolder[book].getBook().getFileName());
					panelHolder[otherPosition(book)].getBook().goToPage(
							panelHolder[book].getBook().getCurrentSpineElementIndex());
					panelHolder[otherPosition(book)].getBook().setLanguage(secondLanguage);
					panelHolder[otherPosition(book)].setBookPage(
							panelHolder[otherPosition(book)].getBook().getCurrentPageURL());
				}
				panelHolder[book].getBook().setLanguage(firstLanguage);
				panelHolder[book].setBookPage(panelHolder[book].getBook().getCurrentPageURL());
			} catch (Exception e) {
				ok = false;
			}

			if (ok && firstLanguage != -1 && secondLanguage != -1) {
				setChapterSync(true);
			}

			readingBilingualEbook = true;
		}
		return ok;
	}

	public boolean isReadingBilingualEbook() {
		return readingBilingualEbook;
	}

	public void setReadingBilingualEbook(boolean readingBilingualEbook) {
		this.readingBilingualEbook = readingBilingualEbook;
	}



	// ============================================================================================
	//		Load and save state to preferences
	// ============================================================================================

	/**
	 * If necessary, recreates the panels from last time based on saved preferences,
	 *  and displays them.
	 * @param preferences SharedPreferences to load from.
	 * @param creatingActivity  Whether or not the activity is just being created.
	 * @return Number of now existing panels.
	 */
	public int loadPanels(SharedPreferences preferences, boolean creatingActivity) {
		Log.d(LOG, "Governor.loadPanels");

		int panelCount = 0;

		for (PanelHolder ph : panelHolder) {
			panelCount += ph.loadPanel(preferences, creatingActivity);
		}

		if (creatingActivity) {
			// Load if one of the panels was hidden, if so, hide it.
			int hiddenPanelPosition = preferences.getInt(getS(R.string.HiddenPanelPosition), -1);
			if (hiddenPanelPosition != -1) {
				getPanelHolder(hiddenPanelPosition).hidePanel();
			} else {
				hiddenPanel = null;
			}
			Log.d(LOG, "Governor.loadPanels, hiddenPanelPosition: " + hiddenPanelPosition);
		}

		return panelCount;
	}

	/**
	 * Save the state of the app before its closing (both by the user or by the operating system).
	 * @param editor	SharedPreferences editor instance
	 */
	public void saveState(Editor editor) {

		editor.putBoolean(getS(R.string.scrollSync), scrollSync);
		editor.putBoolean(getS(R.string.chapterSync), chapterSync);
		editor.putBoolean(getS(R.string.readingBilingualEbookBool), readingBilingualEbook);
		editor.putInt(getS(R.string.HiddenPanelPosition), hiddenPanel != null ? hiddenPanel.getPosition() : -1);

		dontDisplayAgain.saveState(editor);

		for (PanelHolder ph : panelHolder) {
			ph.saveState(editor);
		}
	}

	/**
	 * Load the state of the app after it is reopened.
	 * @param preferences  SharedPreferences instance
	 * @param creatingActivity  Whether or not the activity is just being created.
	 * @return				successfulness
	 */
	public boolean loadState(SharedPreferences preferences, boolean creatingActivity) {
		scrollSync = preferences.getBoolean(getS(R.string.scrollSync), false);
		chapterSync = preferences.getBoolean(getS(R.string.chapterSync), false);
		readingBilingualEbook = preferences.getBoolean(getS(R.string.readingBilingualEbookBool), false);
		boolean ok = true;

		// Check if everything is as it should, if not recreate it.
		if (!selfCheck()) {
			prepareComplexClasses();
		}

		ok = ok && dontDisplayAgain.loadState(preferences, creatingActivity);

		for (PanelHolder ph : panelHolder) {
			if (!ph.loadState(preferences, creatingActivity)) {
				ok = false;
			}
		}

		return ok;
	}



	// ============================================================================================
	//		Misc
	// ============================================================================================

	private SelectionWebView[] getTwinWebViews() {
		BookPanel[] bookPanels = new BookPanel[] {
				panelHolder[0].getBookPanel(), panelHolder[1].getBookPanel()};

		if (bookPanels[0] != null && bookPanels[1] != null) {
			SelectionWebView[] webViews = new SelectionWebView[] {
					bookPanels[0].getWebView(), bookPanels[1].getWebView()};

			if (webViews[0] != null && webViews[1] != null) {
				return webViews;
			}
		}
		return null;
	}

	/**
	 * Shorthand for context.getResources().getString(id)
	 */
	private String getS(int id) {
		return activity.getString(id);
	}

	/**
	 * Activity calls this when a runtime change happens in case we need to do something about it.
	 * Examples: Screen orientation changed, entered/exited fullscreen, etc.
	 */
	public void onRuntimeChange() {
		for (PanelHolder ph : panelHolder) {
			ph.onRuntimeChange();
		}
	}
}
