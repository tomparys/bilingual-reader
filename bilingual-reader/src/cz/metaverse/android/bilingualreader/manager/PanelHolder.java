package cz.metaverse.android.bilingualreader.manager;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.BookPanelState;
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

	private static final String LOG = "hugo";

	/* Interactivity with the outside */
	private ReaderActivity activity;
	private Governor governor;
	private PanelHolder sisterPanelHolder;

	/* Holding variables */
	private SplitPanel panel;

	// Position of the panel (0 = up, 1 = down)
	private int position;

	private Epub book;

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

	public SplitPanel getPanel() {
		return panel;
	}

	public boolean hasOpenPanel() {
		return panel != null;
	}

	public void hidePanel() {
		// TODO
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
				bookPanel.loadPage(book.getCurrentPageURL());
				// setBookPage(books[index].getCurrentPageURL(), index);*/
			} else {
				// Make this panel into a BookView with the opened book instead of closing it.
				BookPanel v = new BookPanel(governor, this, position);
				changePanel(v);
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
			activity.removePanel(panel);

			// Remove the displayed name of the book in the navigation drawer of our main activity.
			activity.setBookNameInDrawer(position, null);

			// Dereference the panel and its book before we switch panels.
			book = null;
			panel = null;

			// If one of the panels gets closed, user no longer reads bilingual ebook in a bilingual mode.
			governor.setReadingBilingualEbook(false);

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
			activity.removePanelWithoutClosing(panel);
			p.changeWeight(panel.getWeight());
		}

		// If the given panel is already open, remove it.
		// TODO ??????????????????????????? Why??
		if (p.isAdded()) {
			activity.removePanelWithoutClosing(p);
		}

		p.updatePosition(position);
		panel = p;
		activity.addPanel(p);

		// Detach and re-attach the panel that is after the newly changed one.
		if (position == 0 && sisterPanelHolder.hasOpenPanel()) {
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
		try {
			if (book != null) {
				book.destroy();
			}

			book = new Epub(path, "" + position, activity);
			changePanel(new BookPanel(governor, this, position));
			setBookPage(book.getSpineElementPath(0));

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
			Log.e(LOG, "Exception while opening a book (path: " + path + "): " + e.toString());
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
				enumState = BookPanelState.books;
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
		try {
			setBookPage(forward ? book.goToNextChapter() : book.goToPreviousChapter());

			if (governor.isChapterSync()) {
				if (sisterPanelHolder.book != null) {
					if (forward) {
						sisterPanelHolder.setBookPage(sisterPanelHolder.book.goToNextChapter());
					} else {
						sisterPanelHolder.setBookPage(sisterPanelHolder.book.goToPreviousChapter());
					}
				}
			} else {
				governor.setScrollSync(false);
			}
		} catch (Exception e) {
			activity.errorMessage(activity.getString(R.string.error_cannotTurnPage));
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
		if (panel == null || !panel.selfCheck()) {
			Log.d(LOG, "PanelHolder.loadPanel - creating panel from memory");

			panel = newPanelByClassName(preferences.getString(getS(R.string.ViewType) + position, ""));
			if (panel != null) {
				panel.updatePosition(position);
				if (panel instanceof AudioPanel) {
					((AudioPanel) panel).setAudioList(
							sisterPanelHolder.book.getAudio());
				}
				panel.loadState(preferences);
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
}
