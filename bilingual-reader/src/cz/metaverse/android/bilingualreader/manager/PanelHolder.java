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

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.db.BookDB;
import cz.metaverse.android.bilingualreader.db.BookPageDB;
import cz.metaverse.android.bilingualreader.db.BookPageDB.BookPage;
import cz.metaverse.android.bilingualreader.dialog.InfotextDialog;
import cz.metaverse.android.bilingualreader.enums.BookPanelState;
import cz.metaverse.android.bilingualreader.helper.DontShowAgain;
import cz.metaverse.android.bilingualreader.helper.Func;
import cz.metaverse.android.bilingualreader.panel.AudioPanel;
import cz.metaverse.android.bilingualreader.panel.BookPanel;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 * PanelHolder is a class that manages and *holds* an instance of a panel of our application.
 *
 */
public class PanelHolder {

	private static final String LOG = "PanelHolder";

	/* Interactivity with the outside */
	private ReaderActivity activity;
	private Governor governor;
	private PanelHolder sisterPanelHolder;

	/* Holding variables */
	private SplitPanel panel;

	// Position of the panel (0 = up, 1 = down)
	private int position;

	private Epub book;

	private boolean isPanelHidden;

	// Whether or not to extract the audio from this panel and show it in the sister panel.
	private boolean extractAudioFromThisPanel;



	// ============================================================================================
	//		Panel Holder
	// ============================================================================================

	/**
	 * Constructor
	 * @param position  Position of the panel (0 - top, 1 - bottom).
	 * @param activity  ReaderActivity instance.
	 * @param governor  The Governor of our application.
	 */
	public PanelHolder(int position, ReaderActivity activity, Governor governor) {
		this.position = position;
		this.activity = activity;
		this.governor = governor;
	}

	/**
	 * Update the PanelHolder with a link to the current ReaderActivity.
	 */
	public void setActivity(ReaderActivity activity) {
		this.activity = activity;

		// TODO Update activity further down the road?? Split/BookPanel?, WebView?, BookPanelOnTouchListener?
	}


	/**
	 * Returns the position of this panel.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Inform this panelHolder that his position has changed.
	 */
	public void updatePosition(int position) {
		this.position = position;

		if (book != null) {
			try {
				book.changeDirName(position + "");
			} catch (Exception e) {
				Log.e(LOG, "Exception in changeDirName while reopening a book: " + e.toString());
			}
		}

		if (panel != null) {
			panel.updatePosition(position);
		}
	}

	/**
	 * Inform this panelHolder where to find his sister.
	 */
	public void setSisterPanelHolder(PanelHolder panelHolder) {
		sisterPanelHolder = panelHolder;
	}

	/**
	 * Returns the this panelHolder's sister.
	 */
	public PanelHolder getSisterPanelHolder() {
		return sisterPanelHolder;
	}



	// ============================================================================================
	//		Panel
	// ============================================================================================

	/**
	 * Returns the SplitPanel instance.
	 */
	public SplitPanel getPanel() {
		return panel;
	}

	/**
	 * Returns true if it has an opened panel.
	 */
	public boolean hasOpenPanel() {
		return panel != null;
	}

	/**
	 * If there is any hidden panel, reappears it, if not, hides this panel.
	 */
	public void hideOrReappearPanel() {
		if (governor.hiddenPanel != null) {
			governor.hiddenPanel.reappearPanel();
		} else {
			hidePanel();
		}
	}

	/**
	 * Hides this panel if the other panel is not hidden.
	 */
	public void hidePanel() {
		if (governor.isOnlyOnePanelOpen()) {
			// Can't hide the only open panel.
			Toast.makeText(activity, R.string.Cannot_hide_the_only_open_panel, Toast.LENGTH_SHORT).show();
		} else if ((governor.hiddenPanel == null || governor.hiddenPanel == this)) {
			// No need to test for panel.isVisible(), because hiding it twice doesn't matter anyway.
			if (panel != null) {
				activity.hidePanel(panel);
			}

			governor.hiddenPanel = this;
			isPanelHidden = true;

			activity.setDrawerHideOrReappearPanelButton(false);
		}
	}

	/**
	 * Reappears this panel.
	 */
	public void reappearPanel() {
		if (panel != null) {
			activity.showPanel(panel);
		}

		governor.hiddenPanel = null;
		isPanelHidden = false;

		activity.setDrawerHideOrReappearPanelButton(true);
	}

	/**
	 * Close one of the panels.
	 */
	public void closePanel() {
		// If it's AudioView panel, stop the playback.
		if (panel instanceof AudioPanel) {
			((AudioPanel) panel).stop();
			sisterPanelHolder.extractAudioFromThisPanel = false;
		}
		// If the other panel is an AudioView panel opened from this one, close the other one as well.
		if (extractAudioFromThisPanel && sisterPanelHolder.panel instanceof AudioPanel) {
			sisterPanelHolder.closePanel();
			extractAudioFromThisPanel = false;
		}

		// If this panel isn't a BookView or if this panel's enumState isn't *books*,
		// BUT there is a book (EpubManipulator) opened for this panel
		if (book != null && (!(panel instanceof BookPanel) // TODO this is wrong!
				|| (((BookPanel) panel).enumState != BookPanelState.books))) {
			if (panel instanceof BookPanel) {
				// Change the content of the BookPanel back to the book.
				BookPanel bookPanel = getBookPanel();
				bookPanel.enumState = BookPanelState.books;

				Log.d(LOG, LOG + ".closePanel() calling (v1) bookPanel.loadPage(" + book.getCurrentPageURL() + ")");
				bookPanel.loadPage(book.getCurrentPageURL());
				// setBookPage(books[index].getCurrentPageURL(), index);*/
			} else {
				// Make this panel into a BookView with the opened book instead of closing it.
				BookPanel v = new BookPanel(governor, this, position);
				changePanel(v);
				Log.d(LOG, LOG + ".closePanel() calling (v2) bookPanel.loadPage(" + book.getCurrentPageURL() + ")");
				v.loadPage(book.getCurrentPageURL());
			}
		}
		// all other cases
		else {
			// Remove the epub ebook
			if (book != null)
				try {
					book.destroy();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			// Remove the panel
			activity.removePanelWithClosing(panel);

			// Remove the displayed name of the book in the navigation drawer of our main activity.
			activity.setBookNameInDrawer(position, null);

			// Dereference the panel and its book before we switch panels.
			book = null;
			panel = null;

			// If one of the panels gets closed, user no longer reads bilingual ebook in a bilingual mode.
			governor.setReadingBilingualEbook(false);

			// If one of the panels was hidden, reappear it, so we don't end up with no panel visible.
			governor.reappearPanel();

			// If this is the 1st panel (pos=0), and the sister panel holder has an open panel,
			// switch panel holder instances so that it is now first.
			if (position == 0 && sisterPanelHolder.hasOpenPanel()) {
				governor.switchPanels();
			}
		}
	}

	/**
	 * Changes the screen area ratio between the two opened panels.
	 * @param weight
	 */
	public void changePanelWeight(float weight) {
		if (panel != null) {
			panel.changeWeight(weight);
		}
	}

	/**
	 * Changes the panel in position *index* with the new panel *p*
	 * @param p			Instance of SplitPanel or one of its descendants (BookView, DataView, AudioView)
	 * @param index		Index of the relevant panel
	 */
	public void changePanel(SplitPanel p) {
		if (panel != null) {
			// We're closing this panel, so if it was a BookPanel, save the reading progress
			// of the opened page to the BookPage database so we can reopen the book on the given page,
			// at a given scroll position.
			if (isBookPanel()) {
				getBookPanel().saveBookPageToDb();
			}

			activity.removePanel(panel);
			p.changeWeight(panel.getWeight());
		}

		// If the given panel is already open, remove it.
		// TODO ??????????????????????????? Why??
		if (p.isAdded()) {
			activity.removePanel(p);
		}

		p.updatePosition(position);
		panel = p;
		activity.addPanel(p);

		// Detach and re-attach the panel that is after the newly changed one.
		if (position == 0 && sisterPanelHolder.hasOpenPanel()) {

			// Because the sister panel will be reAttached, its "onActivityCreated" method will be
			// re-launched. And thus we need to inform it in advance so he can save state variables
			// (scroll position and ScrollSync data) so that it remains in the same state as before.
			BookPanel sister = getSisterBookPanel();
			if (sister != null) {
				sister.prepareForReloadOfTheSamePage();
			}

			sisterPanelHolder.reAttachPanel();
		}
	}

	public void reAttachPanel() {
		if (panel != null) {
			activity.detachPanel(panel);
			activity.attachPanel(panel);
		}
	}



	// ============================================================================================
	//		Book Panel
	// ============================================================================================

	/**
	 * If there is a BookPanel in the specified *panel*, it returns it, otherwise you get null.
	 */
	public BookPanel getBookPanel() {
		if (panel != null && panel instanceof BookPanel) {

			return (BookPanel) panel;
		} else {
			return null;
		}
	}

	public boolean isBookPanel() {
		return panel != null && panel instanceof BookPanel;
	}

	/**
	 * Returns the other book panel, if it exists.
	 */
	public BookPanel getSisterBookPanel() {
		return sisterPanelHolder.getBookPanel();
	}

	/**
	 * Returns the other book panel's WebView, if it exists.
	 */
	public SelectionWebView getSisterWebView() {
		if (sisterPanelHolder.hasOpenPanel() && sisterPanelHolder.panel instanceof BookPanel) {
			return ((BookPanel) sisterPanelHolder.panel).getWebView();
		}
		return null;
	}



	// ============================================================================================
	//		Book
	// ============================================================================================

	public Epub getBook() {
		return book;
	}

	/**
	 * Opens a new book into one of the panels.
	 * @param path  Path of the book
	 * @return Success
	 */
	public boolean openBook(String path) {
		/* Cancel ChapterSync and ScrollSync if they were on and announce the changes. */
		boolean changedChapterSync = governor.setChapterSync(false);
		Boolean changedScrollSync = governor.setScrollSync(false, true); // returns null if no change.
		Integer messageResource = null;

		if (changedChapterSync && changedScrollSync != null) {
			messageResource = R.string.Deactivated_Chapter_and_Scroll_sync;
		} else if (changedChapterSync) {
			messageResource = R.string.Deactivated_Chapter_sync;
		} else if (changedScrollSync != null) {
			messageResource = R.string.Deactivated_Scroll_sync;
		}

		if (messageResource != null) {
			Toast.makeText(activity, messageResource, Toast.LENGTH_LONG).show();
		}

		// Display infotext if appropriate.
		InfotextDialog.showIfAppropriate(activity, DontShowAgain.READER_ACTIVITY);


		try {
			if (book != null) {
				book.destroy();
			}

			book = new Epub(path, "" + position, activity);
			changePanel(new BookPanel(governor, this, position));

			/* Database. */
			String[] bookUniqueKey = new String[] {
					Func.fileNameFromPath(book.getFilePath()),
					book.getTitle()};
			Log.d(LOG, LOG + ".openBook: bookUniqueKey = (" + bookUniqueKey[0] + "; " + bookUniqueKey[1] + ")");

			// Save this book to the BookDB as recently opened.
			BookDB bookDB = BookDB.getInstance(activity);
			bookDB.replaceBook(bookUniqueKey[0], bookUniqueKey[1], path, System.currentTimeMillis());

			// Look through the database for the last page of this book we had opened.
			BookPageDB bookPageDB = BookPageDB.getInstance(activity);
			BookPage latestBookPage = bookPageDB.findLatestBookPage(bookUniqueKey[0], bookUniqueKey[1]);

			if (latestBookPage != null) {
				// Last page found - load it!
				setBookPage(Func.removeFragmentIdentifier(book.getAbsolutePathFromRelative(
						latestBookPage.getPageRelativePath())));

				// Tell BookPanel to load the scroll position and offset.
				getBookPanel().loadBookPageFromDb(latestBookPage);
			} else {
				// Last page not found, load the first page of the book.
				setBookPage(book.getSpineElementPath(0));
			}
			/* Database end */


			// If we opened a new book, we automatically cancelled the reading of a bilingual book,
			// because at least one panel now does now contain a different book.
			governor.setReadingBilingualEbook(false);

			// Save the state to shared preferences
			SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
			saveState(editor);
			editor.commit();

			// Display the name of the book in the navigation drawer of our main activity.
			activity.setBookNameInDrawer(position, book.getTitle());

			return true;

		} catch (Exception e) {
			Toast.makeText(activity, "Exception while opening a book. Please open a different one.", Toast.LENGTH_LONG).show();
			Log.e(LOG, "Exception while opening a book (path: " + path + "): " + e.toString());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Sets a given book page in a given panel.
	 * @param page  Page to be set
	 */
	public void setBookPage(String page) {
		Log.d(LOG, "setBookPage: " + page);

		if (book != null) {
			book.goToPage(page);

			// Extract audio into the other panel if appropriate.
			if (extractAudioFromThisPanel) {
				SplitPanel sisterPanel = sisterPanelHolder.getPanel();
				if (sisterPanel != null && sisterPanel instanceof AudioPanel) {
					((AudioPanel) sisterPanel).setAudioList(book.getAudio());
				} else {
					extractAudio();
				}
			}
		}

		loadPageIntoView(page);
	}

	/**
	 * Load given page into given panel
	 * @param pathOfPage  Path of the page to be loaded
	 */
	public void loadPageIntoView(String pathOfPage) {
		// Default - set panel state to *notes*.
		BookPanelState enumState = BookPanelState.notes;

		// If the page is contained in the opened book in this panel, set enumState to *books*
		if (book != null) {
			// Split the #... part from the URL so we can get a match.
			String cleanPath = pathOfPage.split("#", 2)[0];
			if ((cleanPath.equals(book.getCurrentPageURL())) || (book.getPageIndex(cleanPath) >= 0)) {

				// This is a newly opening book page (not Metadata or ToC)
				enumState = BookPanelState.books;

				// Reset Scroll Sync Points, because they're now relevant to a different set of two books.
				governor.resetScrollSyncPoints();
			}
		}

		// If this panel has no opened book, set enumState to *empty*
		if (book == null) {
			enumState = BookPanelState.empty;
		}

		// If this panel isn't yet open or isn't instance of BookView, open it and make it BookView.
		if (panel == null || !(panel instanceof BookPanel)) {
			Log.d(LOG, "PanelHolder.loadPageIntoView -> changePanel(new BookPanel(..))");
			changePanel(new BookPanel(governor, this, position));
		}

		// Set state and load appropriate page.
		((BookPanel) panel).enumState = enumState;
		Log.d(LOG, LOG + ".loadPageIntoView() calling bookPanel.loadPage(" + pathOfPage + ")");
		((BookPanel) panel).loadPage(pathOfPage);

		// If the panel is indeed now displaying *notes* or *metadata*
		// save his index as the last panel that opened notes.
		if (enumState == BookPanelState.notes || enumState == BookPanelState.metadata) {
			governor.notesDisplayedLastIn = this;
		}
	}

	/**
	 * Change chapter: Go to next or previous chapter.
	 * If ChapterSync is active, change chapter in both books/panels.
	 */
	public void changeChapter(boolean forward) {
		boolean changedChapter = false;
		boolean changedChapterInSync = false;
		try {
			if (forward) {
				if (book.hasNextChapter()) {
					// Set the appropriate slide-in animation
					if (isBookPanel()) {
						getBookPanel().setSlideInAnimation(false);
					}
					setBookPage(book.goToNextChapter());
					changedChapter = true;
				} else {
					Toast.makeText(activity, R.string.This_is_the_last_page, Toast.LENGTH_SHORT).show();
				}
			} else {
				if (book.hasPreviousChapter()) {
					// Set the appropriate slide-in animation
					if (isBookPanel()) {
						getBookPanel().setSlideInAnimation(true);
					}
					setBookPage(book.goToPreviousChapter());
					changedChapter = true;
				} else {
					Toast.makeText(activity, R.string.This_is_the_first_page, Toast.LENGTH_SHORT).show();
				}
			}

			if (changedChapter) {
				if (governor.isChapterSync()) {
					if (sisterPanelHolder.book != null) {
						if (forward) {
							if (sisterPanelHolder.book.hasNextChapter()) {
								// Set the appropriate slide-in animation
								if (sisterPanelHolder.isBookPanel()) {
									sisterPanelHolder.getBookPanel().setSlideInAnimation(false);
								}
								sisterPanelHolder.setBookPage(sisterPanelHolder.book.goToNextChapter());
								changedChapterInSync = true;
							}
						} else {
							if (sisterPanelHolder.book.hasPreviousChapter()) {
								// Set the appropriate slide-in animation
								if (sisterPanelHolder.isBookPanel()) {
									sisterPanelHolder.getBookPanel().setSlideInAnimation(true);
								}
								sisterPanelHolder.setBookPage(sisterPanelHolder.book.goToPreviousChapter());
								changedChapterInSync = true;
							}
						}
					}

					// If this book chapter has been changed, but the sister book's chapter hasn't:
					if (!changedChapterInSync) {
						// Deactivate Chapter sync AND Scroll Sync.
						governor.setChapterSync(false);

						if (governor.setScrollSync(false, true) != null) {
							// If ScrollSync was active:
							Toast.makeText(activity,
									R.string.Other_book_has_no_more_chapters_Deactivated_Chapter_and_Scroll_sync,
									Toast.LENGTH_LONG).show();
						} else {
							// If ScrollSync wasn't active:
							Toast.makeText(activity,
									R.string.Other_book_has_no_more_chapters_Deactivated_Chapter_sync,
									Toast.LENGTH_LONG).show();
						}
					}

				} else {
					if (governor.setScrollSync(false, true) != null) {
						Toast.makeText(activity, R.string.Deactivated_Scroll_sync, Toast.LENGTH_SHORT).show();
					}
				}
			}
		} catch (Exception e) {
			activity.errorMessage(activity.getString(R.string.error_cannotTurnPage));
			Log.e(LOG, "Exception while turning page: " + e.toString());
		}
	}



	// ============================================================================================
	//		Content
	// ============================================================================================

	/**
	 * Passes CSS data to the book, redisplays the page.
	 */
	public void changeCSS(String[] settings) {
		book.addCSS(settings);
		loadPageIntoView(book.getCurrentPageURL());
	}

	/**
	 * Set note = Set the page in the next panel
	 *
	 * @param page
	 *            To be set
	 * @param index
	 *            The panel that will NOT be changed
	 */
	public void setNote(String page) {
		// TODO what if other panel isn't opened??
		sisterPanelHolder.loadPageIntoView(page);
	}

	/**
	 * Display book metadata
	 * @return true if metadata are available, false otherwise
	 */
	public boolean displayMetadata() {
		// Display infotext if appropriate.
		InfotextDialog.showIfAppropriate(activity, DontShowAgain.METADATA_OR_TOC);

		governor.notesDisplayedLastIn = this;

		if (book != null) {
			// Use the existing BookPanel to display metadata or open a new one if needed.
			BookPanel bookPanel = getBookPanel();
			if (bookPanel == null) {
				// Open a new BookPanel to display metadata
				bookPanel = new BookPanel(governor, this, position);
				changePanel(bookPanel);
			}

			bookPanel.loadData(book.metadata(), book.tableOfContents());
			bookPanel.enumState = BookPanelState.metadata;

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Displays Table of Contents or returns false.
	 * @return true if TOC is available, false otherwise
	 */
	public boolean displayToC() {
		// Display infotext if appropriate.
		InfotextDialog.showIfAppropriate(activity, DontShowAgain.METADATA_OR_TOC);

		if (book != null) {
			//Log.d(LOG, "[" + position + "] Displaying ToC at " + book.tableOfContents());

			setBookPage(book.tableOfContents());
			return true;
		} else {
			return false;
		}
	}



	// ============================================================================================
	//		Bilingual book and Audio extraction
	// ============================================================================================

	/**
	 * Get languages available in this epub
	 */
	public String[] getLanguagesInABook() {
		if (book != null) {
			return book.getLanguages();
		}
		return null;
	}


	public boolean extractAudio() {
		if (book.getAudio().length > 0) {
			extractAudioFromThisPanel = true;
			AudioPanel a = new AudioPanel(governor, this, position);
			a.setAudioList(book.getAudio());
			sisterPanelHolder.changePanel(a);
			return true;
		}
		return false;
	}

	/**
	 * @return Whether there is audio to be extracted in either of the opened books.
	 */
	public boolean canExtractAudio() {
		return book != null && book.getAudio().length > 0;
	}



	// ============================================================================================
	//		Save and load state
	// ============================================================================================

	/**
	 * If necessary, recreates the panels from last time based on saved preferences,
	 *  and displays them.
	 * @param preferences
	 * @param creatingActivity  Whether or not the activity is just being created.
	 * @return 1 if this panel now exists, 0 if it doesn't
	 */
	public int loadPanel(SharedPreferences preferences, boolean creatingActivity) {
		Log.d(LOG, "PanelHolder.loadPanel");

		// If the panel doesn't exist or if it doesn't appear to be ok, recreate it.
		if (panel == null || !panel.selfCheck(creatingActivity)) {
			Log.d(LOG, "PanelHolder.loadPanel - creating panel from memory");

			panel = newPanelByClassName(preferences.getString(getS(R.string.ViewType) + position, ""));
			if (panel != null) {
				if (panel instanceof BookPanel && book == null) {
					// If this is a book panel, but the book isn't yet opened, close it again.
					panel = null;
				} else {
					panel.updatePosition(position);
					if (panel instanceof AudioPanel) {
						((AudioPanel) panel).setAudioList(
								sisterPanelHolder.book.getAudio());
					}
					panel.loadState(preferences, creatingActivity);
				}
			}

		// If the panel exists and is sound, but we're (re)creating the activity.
		} else if (creatingActivity) {
			if (isBookPanel()) {
				// Scroll position gets lost when changing to/from landscape mod, so load it.
				getBookPanel().loadScrollPosition(preferences);
			}
		}

		// If panel is properly setup but not displayed, display it!
		if (panel != null && !panel.isAdded()) {
			activity.addPanel(panel);
		}

		return panel != null ? 1 : 0;
	}

	/**
	 * Save the state of the app before its closing (both by the user or by the operating system).
	 * @param editor	SharedPreferences editor instance
	 */
	public void saveState(Editor editor) {

		// Save the book
		if (book != null) {
			// Save data about the book and position in it
			editor.putInt(getS(R.string.CurrentPageBook) + position, book.getCurrentSpineElementIndex());
			editor.putInt(getS(R.string.LanguageBook) + position, book.getCurrentLanguage());
			editor.putString(getS(R.string.nameEpub) + position, book.getDecompressedFolder());
			editor.putString(getS(R.string.pathBook) + position, book.getFileName());
			editor.putBoolean(getS(R.string.exAudio) + position, extractAudioFromThisPanel);

			// Free unnecessary resources in the book instance.
			try {
				book.closeFileInputStream();
			} catch (IOException e) {
				Log.e(getS(R.string.error_CannotCloseStream), getS(R.string.Book_Stream) + (position + 1));
				e.printStackTrace();
			}
		} else {
			// Put null values for this panel if no book is open for it
			editor.putInt(getS(R.string.CurrentPageBook) + position, 0);
			editor.putInt(getS(R.string.LanguageBook) + position, 0);
			editor.putString(getS(R.string.nameEpub) + position, null);
			editor.putString(getS(R.string.pathBook) + position, null);
		}

		// Save views
		if (panel != null) {
			editor.putString(getS(R.string.ViewType) + position, panel.getClass().getName());

			panel.saveState(editor);

			// There is no need to remove panels upon losing focus.
			// 	Leaving it here commented out from the original project in case it causes trouble.
			//activity.removePanelWithoutClosing(panel);
		}
		else {
			editor.putString(getS(R.string.ViewType) + position, "");
		}
	}

	/**
	 * Load the state of the app after it is reopened.
	 * @param preferences  SharedPreferences instance
	 * @param creatingActivity  Whether or not the activity is just being created.
	 * @return  successfulness
	 */
	public boolean loadState(SharedPreferences preferences, boolean creatingActivity) {
		boolean ok = true;
		// Load the panel and its book
		// Data about the book in the panel
		int current = preferences.getInt(getS(R.string.CurrentPageBook) + position, 0);
		int lang = preferences.getInt(getS(R.string.LanguageBook) + position, 0);
		String name = preferences.getString(getS(R.string.nameEpub) + position, null);
		String path = preferences.getString(getS(R.string.pathBook) + position, null);
		extractAudioFromThisPanel = preferences.getBoolean(getS(R.string.exAudio) + position, false);

		// Load the book if it doesn't exist or didn't pass through a selfCheck.
		if (book == null || !book.selfCheck()) {
			if (path != null) {
				try {
					// Try loading the already extracted book
					book = new Epub(path, name, current, lang, activity);
					book.goToPage(current);
				} catch (Exception e1) {

					// Exception: Retry with re-extracting the book
					try {
						book = new Epub(path, position + "", activity);
						book.goToPage(current);
					} catch (Exception e2) {
						ok = false;
					} catch (Error e3) {
						ok = false;
					}
				} catch (Error e) {
					// Exception: Retry with re-extracting the book
					try {
						book = new Epub(path, position + "", activity);
						book.goToPage(current);
					} catch (Exception e2) {
						ok = false;
					} catch (Error e3) {
						ok = false;
					}
				}

				if (book != null) {
					activity.setBookNameInDrawer(position, book.getTitle());
				}
			} else {
				book = null;
			}
		} else {
			// If the book instance still exists, but we're recreating the activity,
			// render the book names into the drawer again.
			if (creatingActivity && book != null) {
				activity.setBookNameInDrawer(position, book.getTitle());
			}
		}

		return ok;
	}

	/**
	 * Returns a class extending SplitPanel based on className in a String
	 * @param className		String containing the className
	 * @param index			Index for the newly created panel
	 * @return				the SplitPanel instance
	 */
	private SplitPanel newPanelByClassName(String className) {
		// TODO: update if a new SplitPanel's inherited class is created
		if (className.equals(BookPanel.class.getName()))
			return new BookPanel(governor, this, position);
		if (className.equals(AudioPanel.class.getName()))
			return new AudioPanel(governor, this, position);
		return null;
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

	/**
	 * Activity calls this when a runtime change happens in case we need to do something about it.
	 * Examples: Screen orientation changed, entered/exited fullscreen, etc.
	 */
	public void onRuntimeChange() {
		if (isBookPanel()) {
			getBookPanel().onRuntimeChange();
		}
	}
}
