package cz.metaverse.android.bilingualreader.manager;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.helper.PanelViewState;
import cz.metaverse.android.bilingualreader.panel.AudioPanel;
import cz.metaverse.android.bilingualreader.panel.BookPanel;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

public class PanelHolder {

	private static final String LOG = "hugo";

	/* Interactivity with the outside */
	private ReaderActivity activity;
	private PanelNavigator navigator;
	private PanelHolder sisterPanelHolder;

	/* Holding variables */
	private SplitPanel panel;

	// Position of the panel (0 = up, 1 = down)
	private int position;

	private EpubManipulator book;

	// Whether or not to extract the audio from this panel and show it in the sister panel.
	private boolean extractAudioFromThisPanel;



	// ============================================================================================
	//		Panel Holder
	// ============================================================================================

	/**
	 * Constructor
	 * @param position  Position of the panel (0 - top, 1 - bottom).
	 * @param activity  ReaderActivity instance.
	 * @param navigator  The Governor of our application.
	 */
	public PanelHolder(int position, ReaderActivity activity, PanelNavigator navigator) {
		this.position = position;
		this.activity = activity;
		this.navigator = navigator;
	}

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
			book.changeDirName(position + "");
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
				|| (((BookPanel) panel).enumState != PanelViewState.books))) {
			if (panel instanceof BookPanel) {
				// Change the content of the BookPanel back to the book.
				BookPanel bookPanel = getBookPanel();
				bookPanel.enumState = PanelViewState.books;
				bookPanel.loadPage(book.getCurrentPageURL());
				// setBookPage(books[index].getCurrentPageURL(), index);*/
			} else {
				// Make this panel into a BookView with the opened book instead of closing it.
				BookPanel v = new BookPanel(navigator, this, position);
				changePanel(v);
				v.loadPage(book.getCurrentPageURL());
			}
		} else // all other cases
		{
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

			// If this is the 1st panel (pos=0), and the sister panel holder has an open panel,
			// switch panel holder instances so that it is now first.
			if (position == 0 && sisterPanelHolder.hasOpenPanel()) {
				navigator.switchPanels();
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

	/**
	 * Removes the panels from the FragmentManager of the Activity. The panels will still exist,
	 * but won't be displayed.
	 */
	public void removePanel() {
		if (panel != null) {
			activity.removePanelWithoutClosing(panel);
		}
	}

	/**
	 * Removes the panels from the FragmentManager of the Activity. The panels will still exist,
	 * but won't be displayed.
	 */
	public void reAddPanels() {
		if (panel != null) {
			activity.addPanel(panel);
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
		// TODO streamline this by keeping link to WebView in a field?
		if (sisterPanelHolder.hasOpenPanel() && sisterPanelHolder.panel instanceof BookPanel) {
			return ((BookPanel) sisterPanelHolder.panel).getWebView();
		}
		return null;
	}



	// ============================================================================================
	//		Book
	// ============================================================================================

	public EpubManipulator getBook() {
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

			book = new EpubManipulator(path, "" + position, activity);
			changePanel(new BookPanel(navigator, this, position));
			setBookPage(book.getSpineElementPath(0));

			// If we opened a new book, we automatically cancelled the reading of a bilingual book,
			// because at least one panel now does now contain a different book.
			navigator.setReadingBilingualEbook(false);

			// Save the state to shared preferences
			SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
			saveState(editor);
			editor.commit();

			// Display the name of the book in the navigation drawer of our main activity.
			activity.setBookNameInDrawer(position, book.getTitle());

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Sets a given book page in a given panel.
	 * @param page  Page to be set
	 */
	public void setBookPage(String page) {

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
		PanelViewState enumState = PanelViewState.notes;

		// Only if the page is contained in the opened book in this panel,
		// set enumState to *books*
		if (book != null) {
			if ((pathOfPage.equals(book.getCurrentPageURL())) || (book.getPageIndex(pathOfPage) >= 0)) {
				enumState = PanelViewState.books;
			}
		}

		// If this panel has no opened book, set enumState to *empty*
		if (book == null) {
			enumState = PanelViewState.empty;
		}

		// If this panel isn't yet open or isn't instance of BookView, open it and make it BookView.
		if (panel == null || !(panel instanceof BookPanel)) {
			changePanel(new BookPanel(navigator, this, position));
		}

		// Set state and load appropriate page.
		((BookPanel) panel).enumState = enumState;
		((BookPanel) panel).loadPage(pathOfPage);

		// If the panel is indeed now displaying *notes* or *metadata*
		// save his index as the last panel that opened notes.
		if (enumState == PanelViewState.notes || enumState == PanelViewState.metadata) {
			navigator.notesDisplayedLastIn = this;
		}
	}

	/**
	 * Go to next chapter,
	 * if synchronized chapters are active, change chapter in both books.
	 */
	public void goToNextChapter() {
		// TODO Unify with Prev Chapter
		try {
			setBookPage(book.goToNextChapter());

			if (navigator.isChapterSync()) {
				if (sisterPanelHolder.book != null) {
					sisterPanelHolder.setBookPage(sisterPanelHolder.book.goToNextChapter());
				}
			}
		} catch (Exception e) {
			activity.errorMessage(activity.getString(R.string.error_cannotTurnPage));
		}
	}

	/**
	 * Go to previous chapter,
	 * if synchronized chapters are active, change chapter in each books.
	 */
	public void goToPrevChapter() {
		try {
			setBookPage(book.goToPreviousChapter());

			if (navigator.isChapterSync()) {
				if (sisterPanelHolder.book != null) {
					sisterPanelHolder.setBookPage(sisterPanelHolder.book.goToPreviousChapter());
				}
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
		navigator.notesDisplayedLastIn = this;

		if (book != null) {
			// Use the existing BookPanel to display metadata or open a new one if needed.
			BookPanel bookPanel = getBookPanel();
			if (bookPanel == null) {
				// Open a new BookPanel to display metadata
				bookPanel = new BookPanel(navigator, this, position);
				changePanel(bookPanel);
			}

			bookPanel.loadData(book.metadata(), book.tableOfContents());
			bookPanel.enumState = PanelViewState.metadata;

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
			Log.d(LOG, "Displaying ToC at " + book.tableOfContents());

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
			AudioPanel a = new AudioPanel(navigator, this, position);
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
	 */
	public void loadPanel(SharedPreferences preferences) {
		Log.d(LOG, "loadPanel");

		// Only load the panel if it isn't already up and ready.
		if (panel == null) {
			panel = newPanelByClassName(preferences.getString(getS(R.string.ViewType) + position, ""));
			if (panel != null) {
				panel.updatePosition(position);
				if (panel instanceof AudioPanel) {
					((AudioPanel) panel).setAudioList(
							sisterPanelHolder.book.getAudio());
				}
				panel.loadState(preferences);
			}
		}

		// If panel is properly setup, display it.
		if (panel != null) {
			activity.addPanel(panel);
		}
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

			// Close the book
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
			//activity.removePanelWithoutClosing(splitViews[i]);
		}
		else {
			editor.putString(getS(R.string.ViewType) + position, "");
		}
	}

	/**
	 * Load the state of the app after it is reopened.
	 * @param preferences	SharedPreferences instance
	 * @return				successfulness
	 */
	public boolean loadState(SharedPreferences preferences) {
		boolean ok = true;
		// Load the panel and its book
		// Data about the book in the panel
		int current = preferences.getInt(getS(R.string.CurrentPageBook) + position, 0);
		int lang = preferences.getInt(getS(R.string.LanguageBook) + position, 0);
		String name = preferences.getString(getS(R.string.nameEpub) + position, null);
		String path = preferences.getString(getS(R.string.pathBook) + position, null);
		extractAudioFromThisPanel = preferences.getBoolean(getS(R.string.exAudio) + position, false);

		// Try loading the already extracted book
		if (path != null) {
			try {
				book = new EpubManipulator(path, name, current, lang, activity);
				book.goToPage(current);
			} catch (Exception e1) {

				// Exception: Retry with re-extracting the book
				try {
					book = new EpubManipulator(path, position + "", activity);
					book.goToPage(current);
				} catch (Exception e2) {
					ok = false;
				} catch (Error e3) {
					ok = false;
				}
			} catch (Error e) {
				// Exception: Retry with re-extracting the book
				try {
					book = new EpubManipulator(path, position + "", activity);
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
		} else
			book = null;

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
			return new BookPanel(navigator, this, position);
		if (className.equals(AudioPanel.class.getName()))
			return new AudioPanel(navigator, this, position);
		return null;
	}



	// ============================================================================================
	//		Misc
	// ============================================================================================

	/**
	 * Shorthand for context.getResources().getString(id)
	 */
	public String getS(int id) {
		return activity.getString(id);
	}
}
