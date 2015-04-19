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

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.helper.PanelViewStateEnum;
import cz.metaverse.android.bilingualreader.panel.AudioPanel;
import cz.metaverse.android.bilingualreader.panel.BookPanel;
import cz.metaverse.android.bilingualreader.panel.DataPanel;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;

public class EpubsNavigator {

	private int nBooks;
	private EpubManipulator[] books;
	private SplitPanel[] splitViews;
	private boolean[] extractAudio;
	private boolean synchronizedReadingActive;
	private boolean parallelText = false;
	private ReaderActivity activity;
	private static Context context;

	/**
	 * Initialize EpubNavigator.
	 * @param numberOfBooks	number of book-viewing panels open
	 * @param a				the MainActivity from which this is launched
	 */
	public EpubsNavigator(int numberOfBooks, ReaderActivity a) {
		nBooks = numberOfBooks;
		books = new EpubManipulator[nBooks];
		splitViews = new SplitPanel[nBooks];
		extractAudio = new boolean[nBooks];
		activity = a;
		context = a.getBaseContext();
	}

	/**
	 * Opens a book on its first page.
	 * @param path	Path of the book
	 * @param index	Index of the panel into which to open it
	 * @return		Success
	 */
	public boolean openBook(String path, int index) {
		try {
			if (books[index] != null)
				books[index].destroy();

			books[index] = new EpubManipulator(path, index + "", context);
			changePanel(new BookPanel(), index);
			setBookPage(books[index].getSpineElementPath(0), index);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Sets a given book page in a given panel.
	 * @param page	Page to be set
	 * @param index	The Panel the page is to be set in
	 */
	public void setBookPage(String page, int index) {

		if (books[index] != null) {
			books[index].goToPage(page);
			
			// Extract audio into the other panel if appropriate.
			if (extractAudio[index]) {
				if (splitViews[(index + 1) % nBooks] instanceof AudioPanel)
					((AudioPanel) splitViews[(index + 1) % nBooks])
							.setAudioList(books[index].getAudio());
				else
					extractAudio(index);
			}
		}

		loadPageIntoView(page, index);
	}

	/** 
	 * Set note = Set the page in the next panel
	 * @param page	To be set
	 * @param index	The panel that will NOT be changed
	 */
	public void setNote(String page, int index) {
		loadPageIntoView(page, (index + 1) % nBooks);
	}

	/**
	 * Load given page into given panel
	 * @param pathOfPage	Path of the page to be loaded
	 * @param index			Index of the panel
	 */
	public void loadPageIntoView(String pathOfPage, int index) {
		PanelViewStateEnum enumState = PanelViewStateEnum.notes;

		// If the page is contained in the opened book in this panel, set enumState to *books*
		if (books[index] != null) {
			if ((pathOfPage.equals(books[index].getCurrentPageURL()))
					|| (books[index].getPageIndex(pathOfPage) >= 0)) {
				enumState = PanelViewStateEnum.books;
			}
		}

		// If this panel has no opened book, set enumState to *notes*
		if (books[index] == null)
			enumState = PanelViewStateEnum.notes;

		// If this panel isn't yet open or isn't instance of BookView, open it and make it BookView.
		if (splitViews[index] == null || !(splitViews[index] instanceof BookPanel))
			changePanel(new BookPanel(), index);

		// Set state and load appropriate page.
		((BookPanel) splitViews[index]).enumState = enumState;
		((BookPanel) splitViews[index]).loadPage(pathOfPage);
	}

	/**
	 * Go to next chapter,
	 * 	if synchronized reading is active, change chapter in both books.
	 * @param index			the panel wherein to primarily change chapter
	 * @throws Exception
	 */
	public void goToNextChapter(int index) throws Exception {
		setBookPage(books[index].goToNextChapter(), index);

		if (synchronizedReadingActive) {
			for (int i = 1; i < nBooks; i++) {
				if (books[(index + i) % nBooks] != null) {
					setBookPage(books[(index + i) % nBooks].goToNextChapter(),
							(index + i) % nBooks);
				}
			}
		}
	}

	/**
	 * Go to previous chapter,
	 * 	if synchronized reading is active, change chapter in each books.
	 * @param index			the panel	
	 * @throws Exception
	 */
	public void goToPrevChapter(int index) throws Exception {
		setBookPage(books[index].goToPreviousChapter(), index);

		if (synchronizedReadingActive) {
			for (int i = 1; i < nBooks; i++) {
				if (books[(index + i) % nBooks] != null) {
					setBookPage(
							books[(index + i) % nBooks].goToPreviousChapter(),
							(index + i) % nBooks);
				}
			}
		}
	}

	/**
	 * Close one of the panels
	 * @param index		The panel to be closed
	 */
	public void closeView(int index) {
		// If it's AudioView panel, stop the playback.
		if (splitViews[index] instanceof AudioPanel) {
			((AudioPanel) splitViews[index]).stop();
			extractAudio[index > 0 ? index - 1 : nBooks - 1] = false;
		}
		// If the other panel is an AudioView panel opened from this one, close the other one as well.
		if (extractAudio[index] && splitViews[(index + 1) % nBooks] instanceof AudioPanel) {
			closeView((index + 1) % nBooks);
			extractAudio[index] = false;
		}

		// If this panel isn't a BookView or if this panel's enumState isn't *books*,
		//   BUT there is a book (EpubManipulator) opened for this panel
		if (books[index] != null &&
				(
						!(splitViews[index] instanceof BookPanel)
						|| (((BookPanel) splitViews[index]).enumState != PanelViewStateEnum.books)
				)
			) {
			// Make this panel into a BookView with the opened book instead of closing it.
			BookPanel v = new BookPanel();
			changePanel(v, index);
			v.loadPage(books[index].getCurrentPageURL());
		}
		else // all other cases
		{
			// Remove the epub ebook
			if (books[index] != null)
				try {
					books[index].destroy();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			// Remove the panel
			activity.removePanel(splitViews[index]);

			// If this is the 1st panel (index=0), move the 2nd panel to the first position in the variables
			while (index < nBooks - 1) {
				books[index] = books[index + 1];
				if (books[index] != null)
					books[index].changeDirName(index + "");

				splitViews[index] = splitViews[index + 1]; // shift left the 2nd panel
				if (splitViews[index] != null) {
					// Update the panel key
					splitViews[index].setKey(index);
					
					if (splitViews[index] instanceof BookPanel
							&& ((BookPanel) splitViews[index]).enumState == PanelViewStateEnum.books) {
						
						// Reload the book page into the panel
						((BookPanel) splitViews[index]).loadPage(books[index].getCurrentPageURL());
					}

				}
				index++; // Generalization for more than 2 panels - unused currently
			}

			// Last book and last view don't exist anymore
			books[nBooks - 1] = null; 
			splitViews[nBooks - 1] = null;
		}
	}

	/**
	 * Get languages available in this epub
	 */
	public String[] getLanguagesInABook(int index) {
		return books[index].getLanguages();
	}

	/**
	 * If books[book] contains more than 1 language, it opens the same book in the other panel, opens the same page
	 *  and sets the first language into the given panel (id book) and the second language into the other.
	 * 	If all goes well it activates SynchronizedReading.
	 * @param book				id of the panel with the book that we want to open in parallel text mode
	 * @param firstLanguage		id of the first language to open
	 * @param secondLanguage	id of the second language to open
	 * @return
	 */
	public boolean parallelText(int book, int firstLanguage, int secondLanguage) {
		boolean ok = true;

		if (firstLanguage != -1) {
			try {
				if (secondLanguage != -1) {
					openBook(books[book].getFileName(), (book + 1) % 2);
					books[(book + 1) % 2].goToPage(books[book]
							.getCurrentSpineElementIndex());
					books[(book + 1) % 2].setLanguage(secondLanguage);
					setBookPage(books[(book + 1) % 2].getCurrentPageURL(),
							(book + 1) % 2);
				}
				books[book].setLanguage(firstLanguage);
				setBookPage(books[book].getCurrentPageURL(), book);
			} catch (Exception e) {
				ok = false;
			}

			if (ok && firstLanguage != -1 && secondLanguage != -1)
				setSynchronizedReadingActive(true);

			parallelText = true;
		}
		return ok;
	}

	public void setSynchronizedReadingActive(boolean value) {
		synchronizedReadingActive = value;
	}

	/**
	 * Flips the state of SynchronizedReading (active -> inactive, inactive -> active).
	 */
	public boolean flipSynchronizedReadingActive() {
		if (exactlyOneBookOpen())
			return false;
		synchronizedReadingActive = !synchronizedReadingActive;
		return true;
	}

	/**
	 * If panel *from* went to a new page, opens the same page in the other panel. 
	 * @param from
	 * @param to
	 * @return
	 * @throws Exception
	 */
	public boolean synchronizeView(int from, int to) throws Exception {
		if (!exactlyOneBookOpen()) {
			setBookPage(books[to].goToPage(books[from]
					.getCurrentSpineElementIndex()), to);
			return true;
		} else
			return false;
	}

	/**
	 * Display book metadata
	 * @param book	id of the panel containing the book
	 * @return		true if metadata are available, false otherwise
	 */
	public boolean displayMetadata(int book) {
		boolean res = true;

		if (books[book] != null) {
			DataPanel dv = new DataPanel();
			dv.loadData(books[book].metadata());
			changePanel(dv, book);
		} else
			res = false;

		return res;
	}

	/**
	 * Displays Table of Contents or returns false.
	 * @param book	id of the relevant panel
	 * @return		true if TOC is available, false otherwise
	 */
	public boolean displayTOC(int book) {
		boolean res = true;

		if (books[book] != null)
			setBookPage(books[book].tableOfContents(), book);
		else
			res = false;
		return res;
	}

	/**
	 * Passes CSS data to the book, redisplays the page.
	 */
	public void changeCSS(int book, String[] settings) {
		books[book].addCSS(settings);
		loadPageIntoView(books[book].getCurrentPageURL(), book);
	}

	public boolean extractAudio(int book) {
		if (books[book].getAudio().length > 0) {
			extractAudio[book] = true;
			AudioPanel a = new AudioPanel();
			a.setAudioList(books[book].getAudio());
			changePanel(a, (book + 1) % nBooks);
			return true;
		}
		return false;
	}

	/**
	 * Changes the screen area ratio between the two opened panels. 
	 * @param weight
	 */
	public void changeViewsSize(float weight) {
		if (splitViews[0] != null && splitViews[1] != null) {
			splitViews[0].changeWeight(1 - weight);
			splitViews[1].changeWeight(weight);
		}
	}

	public boolean isParallelTextOn() {
		return parallelText;
	}

	public boolean isSynchronized() {
		return synchronizedReadingActive;
	}

	/**
	 * @return true if at least one book is open
	 */
	public boolean atLeastOneBookOpen() {
		for (int i = 0; i < nBooks; i++)
			if (books[i] != null)
				return true;
		return false;
	}

	/**
	 * @return true if exactly one book is open
	 */
	public boolean exactlyOneBookOpen() {
		int i = 0;
		// find the first not null book
		while (i < nBooks && books[i] == null)
			i++;

		if (i == nBooks) // if every book is null
			return false; // there's no opened book and return false

		i++;

		while (i < nBooks && books[i] == null)
			i++; // find another not null book

		if (i == nBooks) // if there's no other not null book
			return true; // there's exactly one opened book
		else
			// otherwise
			return false; // there's more than one opened book
	}

	/**
	 * Changes the panel in position *index* with the new panel *p* 
	 * @param p			Instance of SplitPanel or one of its descendants (BookView, DataView, AudioView)
	 * @param index		Index of the relevant panel
	 */
	public void changePanel(SplitPanel p, int index) {
		if (splitViews[index] != null) {
			activity.removePanelWithoutClosing(splitViews[index]);
			p.changeWeight(splitViews[index].getWeight());
		}

		// If the given panel is already open, remove it.
		if (p.isAdded()) {
			activity.removePanelWithoutClosing(p);
		}

		splitViews[index] = p;
		activity.addPanel(p);
		p.setKey(index);

		// Detach and re-attach panels that are after the newly changed one.
		for (int i = index + 1; i < splitViews.length; i++)
			if (splitViews[i] != null) {
				activity.detachPanel(splitViews[i]);
				activity.attachPanel(splitViews[i]);
			}
	}

	/**
	 * Returns a class extending SplitPanel based on className in a String 
	 * @param className		String containing the className
	 * @return				the SplitPanel instance
	 */
	private SplitPanel newPanelByClassName(String className) {
		// TODO: update when a new SplitPanel's inherited class is created
		if (className.equals(BookPanel.class.getName()))
			return new BookPanel();
		if (className.equals(DataPanel.class.getName()))
			return new DataPanel();
		if (className.equals(AudioPanel.class.getName()))
			return new AudioPanel();
		return null;
	}

	/**
	 * Save the state of the app before its closing (both by the user or by the operating system).
	 * @param editor	SharedPreferences editor instance
	 */
	public void saveState(Editor editor) {

		editor.putBoolean(getS(R.string.sync), synchronizedReadingActive);
		editor.putBoolean(getS(R.string.parallelTextBool), parallelText);

		// Save each book
		for (int i = 0; i < nBooks; i++)
			if (books[i] != null) {
				// Save data about the book and position in it
				editor.putInt(getS(R.string.CurrentPageBook) + i,
						books[i].getCurrentSpineElementIndex());
				editor.putInt(getS(R.string.LanguageBook) + i,
						books[i].getCurrentLanguage());
				editor.putString(getS(R.string.nameEpub) + i,
						books[i].getDecompressedFolder());
				editor.putString(getS(R.string.pathBook) + i,
						books[i].getFileName());
				editor.putBoolean(getS(R.string.exAudio) + i, extractAudio[i]);
				
				// Close the book
				try {
					books[i].closeFileInputStream();
				} catch (IOException e) {
					Log.e(getS(R.string.error_CannotCloseStream),
							getS(R.string.Book_Stream) + (i + 1));
					e.printStackTrace();
				}
			} else {
				// Put null values for this panel if no book is open for it
				editor.putInt(getS(R.string.CurrentPageBook) + i, 0);
				editor.putInt(getS(R.string.LanguageBook) + i, 0);
				editor.putString(getS(R.string.nameEpub) + i, null);
				editor.putString(getS(R.string.pathBook) + i, null);
			}

		// Save views
		for (int i = 0; i < nBooks; i++)
			if (splitViews[i] != null) {
				editor.putString(getS(R.string.ViewType) + i, splitViews[i]
						.getClass().getName());
				splitViews[i].saveState(editor);
				
				// There is no need to remove panels upon losing focus.
				// 	Leaving it here commented out from the original project in case it causes trouble. 
				//activity.removePanelWithoutClosing(splitViews[i]);
			}
			else {
				editor.putString(getS(R.string.ViewType) + i, "");
			}
	}

	/**
	 * Load the state of the app after it is reopened.
	 * @param preferences	SharedPreferences instance
	 * @return				successfulness
	 */
	public boolean loadState(SharedPreferences preferences) {
		boolean ok = true;
		synchronizedReadingActive = preferences.getBoolean(getS(R.string.sync), false);
		parallelText = preferences.getBoolean(getS(R.string.parallelTextBool), false);

		// Load each panel and its book
		int current, lang;
		String name, path;
		for (int i = 0; i < nBooks; i++) {
			// Data about the book in the panel
			current = preferences.getInt(getS(R.string.CurrentPageBook) + i, 0);
			lang = preferences.getInt(getS(R.string.LanguageBook) + i, 0);
			name = preferences.getString(getS(R.string.nameEpub) + i, null);
			path = preferences.getString(getS(R.string.pathBook) + i, null);
			extractAudio[i] = preferences.getBoolean(getS(R.string.exAudio) + i, false);
			
			// Try loading the already extracted book
			if (path != null) {
				try {
					books[i] = new EpubManipulator(path, name, current, lang, context);
					books[i].goToPage(current);
				} catch (Exception e1) {

					// Exception: Retry with re-extracting the book
					try {
						books[i] = new EpubManipulator(path, i + "", context);
						books[i].goToPage(current);
					} catch (Exception e2) {
						ok = false;
					} catch (Error e3) {
						ok = false;
					}
				} catch (Error e) {
					// Exception: Retry with re-extracting the book
					try {
						books[i] = new EpubManipulator(path, i + "", context);
						books[i].goToPage(current);
					} catch (Exception e2) {
						ok = false;
					} catch (Error e3) {
						ok = false;
					}
				}
			} else
				books[i] = null;
		}

		return ok;
	}

	/**
	 * Recreates the panels from last time based on saved preferences
	 * @param preferences
	 */
	public void loadViews(SharedPreferences preferences) {
		for (int i = 0; i < nBooks; i++) {
			splitViews[i] = newPanelByClassName(preferences.getString(
					getS(R.string.ViewType) + i, ""));
			if (splitViews[i] != null) {
				activity.addPanel(splitViews[i]);
				splitViews[i].setKey(i);
				if (splitViews[i] instanceof AudioPanel) {
					((AudioPanel) splitViews[i]).setAudioList(books[i > 0 ? i - 1 : nBooks - 1].getAudio());
				}
				splitViews[i].loadState(preferences);
			}
		}
	}

	/**
	 * Shorthand for context.getResources().getString(id)
	 */
	public String getS(int id) {
		return context.getResources().getString(id);
	}
}
