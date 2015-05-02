/*
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
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.BookPanelState;
import cz.metaverse.android.bilingualreader.panel.BookPanel;

/**
 *
 * The main logical and data class of our application.
 * Manages the panels that display books or other content in our application.
 * Manages ebooks that are displayed in the panels.
 * Manages synchronized reading.
 *
 */
public class Governor {

	/* Static */
	private static final String LOG = "hugo";

	// The magic number - the number of panels of our application.
	// Do NOT change this number, the application is not built for it.
	public static final int N_PANELS = 2;

	// A static instance of this class for the Singleton pattern herein employed.
	private static Governor governorInstance;

	/* Dynamic */
	private ReaderActivity activity;
	private PanelHolder[] panelHolder;

	/* Sync */
	private boolean scrollSync = true;
	private boolean chapterSync;
	private boolean readingBilingualEbook = false;

	/* For proper operation of the Back system button. */
	public PanelHolder notesDisplayedLastIn;



	// ============================================================================================
	//		Singleton pattern and constructor
	// ============================================================================================

	/**
	 * Singleton-pattern getter static method.
	 * @param activity	The ReaderActivity instance that's asking for an instance.
	 */
	public static Governor getSingleton(ReaderActivity activity) {
		if (governorInstance == null) {
			governorInstance = new Governor(activity);
		}

		governorInstance.setActivity(activity);
		return governorInstance;
	}

	/**
	 * Private constructor for the singleton pattern.
	 * @param a  The ReaderActivity from which this is launched
	 */
	private Governor(ReaderActivity activity) {
		panelHolder = new PanelHolder[] {
				new PanelHolder(0, activity, this), new PanelHolder(1, activity, this)};
		panelHolder[0].setSisterPanelHolder(panelHolder[1]);
		panelHolder[1].setSisterPanelHolder(panelHolder[0]);

		// Activity is set right after the constructor in the getSingleton() method, no need to do it here.
	}

	/**
	 * Update the Governor with a link to the current ReaderActivity.
	 */
	private void setActivity(ReaderActivity activity) {
		this.activity = activity;

		for (PanelHolder ph : panelHolder) {
			ph.setActivity(activity);
		}
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
				activity.removePanelWithoutClosing(ph.getPanel());
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
		for (PanelHolder ph : panelHolder) {
			if (ph.canExtractAudio()) {
				return true;
			}
		}
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
	//		Synchronization
	// ============================================================================================

	public boolean isChapterSync() {
		return chapterSync;
	}

	public void setChapterSync(boolean value) {
		chapterSync = value;
	}

	/**
	 * Flips the state of SynchronizedChapters (active -> inactive, inactive -> active).
	 */
	public boolean flipChapterSync() {
		if (exactlyOneBookOpen()) {
			return false;
		}
		chapterSync = !chapterSync;
		return true;
	}

	/**
	 * Returns whether Scroll Sync is active or not.
	 */
	public boolean isScrollSync() {
		return scrollSync;
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
	 * @param preferences
	 */
	public void loadPanels(SharedPreferences preferences) {
		Log.d(LOG, "loadPanels");
		for (PanelHolder ph : panelHolder) {
			ph.loadPanel(preferences);
		}
	}

	/**
	 * Save the state of the app before its closing (both by the user or by the operating system).
	 * @param editor	SharedPreferences editor instance
	 */
	public void saveState(Editor editor) {

		editor.putBoolean(getS(R.string.sync), chapterSync);
		editor.putBoolean(getS(R.string.readingBilingualEbookBool), readingBilingualEbook);

		for (PanelHolder ph : panelHolder) {
			ph.saveState(editor);
		}
	}

	/**
	 * Load the state of the app after it is reopened.
	 * @param preferences	SharedPreferences instance
	 * @return				successfulness
	 */
	public boolean loadState(SharedPreferences preferences) {
		chapterSync = preferences.getBoolean(getS(R.string.sync), false);
		readingBilingualEbook = preferences.getBoolean(getS(R.string.readingBilingualEbookBool), false);
		boolean ok = true;

		for (PanelHolder ph : panelHolder) {
			if (!ph.loadState(preferences)) {
				ok = false;
			}
		}

		return ok;
	}



	// ============================================================================================
	//		Misc
	// ============================================================================================

	/**
	 * Shorthand for context.getResources().getString(id)
	 */
	private String getS(int id) {
		return activity.getString(id);
	}
}
