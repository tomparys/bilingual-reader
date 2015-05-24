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

package cz.metaverse.android.bilingualreader.panel;

import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.R;
import cz.metaverse.android.bilingualreader.ReaderActivity;
import cz.metaverse.android.bilingualreader.db.BookPageDB;
import cz.metaverse.android.bilingualreader.db.BookPageDB.BookPage;
import cz.metaverse.android.bilingualreader.enums.BookPanelState;
import cz.metaverse.android.bilingualreader.enums.ScrollSyncMethod;
import cz.metaverse.android.bilingualreader.helper.BookPanelOnTouchListener;
import cz.metaverse.android.bilingualreader.helper.Func;
import cz.metaverse.android.bilingualreader.helper.VisualOptions;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.manager.PanelHolder;
import cz.metaverse.android.bilingualreader.selectionwebview.SelectionWebView;

/**
 *
 *                                                       /===-_---~~~~~~~~~------____
 *                                                      |===-~___                _,-'
 *                       -==\\                         `//~\\   ~~~~`---.___.-~~
 *                   ______-==|                         | |  \\           _-~`
 *             __--~~~  ,-/-==\\                        | |   `\        ,'
 *          _-~       /'    |  \\                      / /      \      /
 *        .'        /       |   \\                   /' /        \   /'
 *       /  ____  /         |    \`\.__/-~~ ~ \ _ _/'  /          \/'
 *      /-'~    ~~~~~---__  |     ~-/~         ( )   /'        _--~`
 *                        \_|      /        _)   ;  ),   __--~~
 *                          '~~--_/      _-~/-  / \   '-~ \
 *                         {\__--_/}    / \\_>- )<__\      \
 *                         /'   (_/  _-~  | |__>--<__|      |
 *                        |0  0 _/) )-~     | |__>--<__|     |
 *                        / /~ ,_/       / /__>---<__/      |
 *                       o o _//        /-~_>---<__-~      /
 *                       (^(~          /~_>---<__-      _-~
 *                      ,/|           /__>--<__/     _-~
 *                   ,//('(          |__>--<__|     /                  .----_
 *                  ( ( '))          |__>--<__|    |                 /' _---_~\
 *               `-)) )) (           |__>--<__|    |               /'  /     ~\`\
 *              ,/,'//( (             \__>--<__\    \            /'  //        ||
 *            ,( ( ((, ))              ~-__>--<_~-_  ~--____---~' _/'/        /'
 *          `~/  )` ) ,/|                 ~-_~>--<_/-__       __-~ _/
 *        ._-~//( )/ )) `                    ~~-'_/_/ /~~~~~~~__--~
 *         ;'( ')/ ,)(                              ~~~~~~~~~~
 *        ' ') '( (/
 *          '   '  `
 *
 *                                HERE BE DRAGONS
 *
 *
 *
 * Be warned, stranger, editing this class is perilous.
 * Editing one functionality can and will have unforeseen adverse effects in other areas.
 * Test your changes profusely.
 *
 *
 *
 * An Extension of the SplitPanel Panel specialized in visualizing EPUB pages.
 * This class' complexity arises from not knowing when a page has finished loading in WebView,
 * so be aware of the sequence of events and watch the log messages before changing anything.
 *
 */
public class BookPanel extends SplitPanel {

	private static final String LOG = "BookPanel";
	private String LOGID = "BookPageDB[?]";

	private ReaderActivity activity;

	/* Prepared static variables for interaction with the DB. DB columns to be updated.*/
	private static final String[] colsUpdate = new String[] {BookPageDB.COL_SCROLL_Y,
			BookPageDB.COL_SCROLLSYNC_METHOD, BookPageDB.COL_SCROLLSYNC_OFFSET,
			BookPageDB.COL_SCROLLSYNC_RATIO, BookPageDB.COL_LAST_OPENED};

	private static final String[] colsInsert = new String[] {BookPageDB.COL_BOOK_FILENAME,
			BookPageDB.COL_BOOK_TITLE, BookPageDB.COL_PAGE_FILENAME, BookPageDB.COL_PAGE_RELATIVE_PATH,
			BookPageDB.COL_SCROLL_Y, BookPageDB.COL_SCROLLSYNC_METHOD, BookPageDB.COL_SCROLLSYNC_OFFSET,
			BookPageDB.COL_SCROLLSYNC_RATIO, BookPageDB.COL_LAST_OPENED};

	// Graphical elements
	private RelativeLayout panelRelativeLayout;

	// Information about the content
	public BookPanelState enumState = BookPanelState.books;
	protected String displayedPage;
	protected String displayedData;

	// Position within the page and ScrollSync offset loaded from before.
	private Float loadPositionY;
	private ScrollSyncMethod loadScrollSyncMethod;
	private Float loadScrollSyncOffset;
	private Float loadScrollSyncRatio;

	// Fields concerning communication with the DB
	private Long loadedFromBookPageRowId;
	private String[] displayedBookPageKey;

	// Whether the current page has been fully rendered.
	private AtomicBoolean finishedRenderingContent = new AtomicBoolean(false);
	private boolean firstRenderingAfterCreatingActivity = false;

	// Our customized WebView and its onTouchListener
	protected SelectionWebView webView;
	private BookPanelOnTouchListener onTouchListener;

	// Slide in from the side animation - when switching chapters.
	private boolean animate = false;
	private boolean animateFromLeft = false;
	private Animation animationSlideInLeft;
	private Animation animationSlideInRigth;
	private Animation animationSlideOutLeft;
	private Animation animationSlideOutRigth;


	/**
	 * Constructor for our BookPanel.
	 * @param governor  The Governor of our application.
	 * @param panelHolder  PanelHolder instance that's holding our panel.
	 * @param position  Position of this panel.
	 */
	public BookPanel(Governor governor, PanelHolder panelHolder, int position) {
		super(governor, panelHolder, position); // Invokes changePosition(position)

		LOGID = LOG + "[" + position + "]";
		Log.d(LOG, LOGID + ".new BookPanel (note. constructor)");
	}

	/**
	 * Empty constructor for when Android needs to recreate the Fragment.
	 */
	public BookPanel() {
		super();
	}

	/**
	 * Checks whether there are any problems with this instance, if for example the Android system
	 * didn't close any important fields that would result in NullPointerExceptions.
	 * @return true if everything appears to be sound
	 */
	@Override
	public boolean selfCheck(boolean creatingActivity) {
		// If the ReaderActivity is being recreated due to a runtime change (e.g. switch to/from landscape),
		// than this BookPanel has persisted and is still open. But since we need this BookPage to act
		// like it's just being created, we have to set finishedRenderingContent=false.
		// Otherwise it would screw up in loadData() and loadPage() where it would null out
		// loadPositionY and loadScrollSyncOffset, which are to be nulled out only after switching to a new
		// page, not after opening the first, due to how loading these values in loadState() works.
		//   - For more information see comment inside loadPage().
		if (creatingActivity) {
			//finishedRenderingContent = false;
			finishedRenderingContent.set(false);
		}

		boolean ok = super.selfCheck(creatingActivity) && webView != null && onTouchListener != null && enumState != null;

		Log.d(LOG, LOGID + ".selfCheck - " + ok);
		return ok;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)	{
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.panel_book, container, false);
		return v;
	}

	/**
	 * onActivityCreated()
	 */
	@SuppressLint("SetJavaScriptEnabled") // Our opensource application has literally nothing to hide.
	@Override
	public void onActivityCreated(Bundle saved) {
		Log.d(LOG, LOGID + ".onActivityCreated, Bundle saved - " + (saved != null ? "filled" : "empty"));
		super.onActivityCreated(saved);

		// Our panels are designed to work strictly with our ReaderActivity.
		activity = (ReaderActivity) getActivity();

		// Find the RelativeLayout which contains this all
		panelRelativeLayout = (RelativeLayout) getView().findViewById(R.id.GeneralLayout);
		changeCSS(governor.getVisualOptions());

		// Find our customized web view that will server as our viewport
		webView = (SelectionWebView) getView().findViewById(R.id.Viewport);
		webView.setPanelHolderAndPosition(panelHolder, panelPosition);
		// Enable JavaScript for cool things to happen!
		webView.getSettings().setJavaScriptEnabled(true);

		// Disable horizontal scroll bar.
		webView.setHorizontalScrollBarEnabled(false);
		webView.setOverScrollMode(View.OVER_SCROLL_NEVER); // Sadly, disables also vertical over scroll.

		onTouchListener = new BookPanelOnTouchListener(activity, governor, panelHolder, this, webView, panelPosition);
		webView.setOnTouchListener(onTouchListener);

		animationSlideInLeft = AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left);
		animationSlideInRigth = AnimationUtils.loadAnimation(activity, R.anim.slide_in_right);
		animationSlideOutLeft = AnimationUtils.loadAnimation(activity, R.anim.slide_out_left);
		animationSlideOutRigth = AnimationUtils.loadAnimation(activity, android.R.anim.slide_out_right);

		// Set a custom WebViewClient that has overwritten method for loading URLs.
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.d(LOG, LOGID + ":WebViewClient.shouldOverrideUrlLoading");

				// Inform our onTouchListener to ignore this click, since it was meant
				//  to click on this URL link.
				// Warning: on slower devices, this method is called LATER than
				//    the onSingleTapConfirmed gesture is evaluated, rendering this sadly useless.
				onTouchListener.setJustClickedOnUrlLink();

				/* Cancel ChapterSync and ScrollSync if they were on and announce the changes. */
				boolean changedChapterSync = governor.setChapterSync(false);
				Boolean changedScrollSync = governor.setScrollSync(false, true); // returns null if no change.
				Func.toastDeactivatedSync(changedChapterSync, changedScrollSync != null, activity);

				try {
					// Decode any HTML entities in the URL so we get the correct path.
					url = URLDecoder.decode(url, "UTF-8");

					// Set book page through the governor if possible.
					panelHolder.setBookPage(url);
				} catch (Exception e) {
					errorMessage(getString(R.string.error_LoadPage));
				}

				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				// Set appropriate animation if desired.
				if (animate) {
					view.startAnimation(animateFromLeft ? animationSlideInLeft : animationSlideInRigth);
				}
				view.setVisibility(View.VISIBLE);

				Log.d(LOG, LOGID + ":WebViewClient.onPageFinished - view.setVisibility VISIBLE");

				super.onPageFinished(view, url);
			}
		});

		// Set long-click listener:
		//  If the user long-clicks on any URL link we open it in the other panel.
		webView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// Create a message and a method that will serve as its target.
				// The target method will look for URL in the messages data,
				//  and if there is one, it dispatches it to the Governor
				//  to open it as a Note in the other panel.
				Message msg = new Message();
				msg.setTarget(new Handler() {
					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);

						// Extract url from the message
						String url = msg.getData().getString(getString(R.string.url));

						// If url isn't empty, set note with the url into this SplitPanel/fragment.
						if (url != null) {
							panelHolder.setNote(url);
						}
					}
				});

				// Puts data about the last pressed object into the message and dispatches it.
				//  If the user didn't click on anything, message is not dispatched.
				webView.requestFocusNodeHref(msg);

				return false;
			}
		});

		// Because Activity is just being created, set finishedRenderingContent to false.
		finishedRenderingContent.set(false);

		// Because this OnActivityCreated method might just be launched because of
		// a runtime change, make sure all is prepared for it.
		//onRuntimeChange();

		// Load the page.
		if (enumState == BookPanelState.metadata) {
			loadData(displayedData, displayedPage);
		} else {
			Log.d(LOG, LOGID + ".onActivityCreated() calling loadPage(" + displayedPage + ")");
			loadPage(displayedPage);
		}

		//Log.d(LOG, LOGID + ".onActivityCreated finished");
	}

	/**
	 * Returns the URL of the currently displayed page.
	 */
	public String getViewedPage() {
		return displayedPage;
	}

	/**
	 * Returns this panel's SelectionWebView.
	 */
	public SelectionWebView getWebView() {
		return webView;
	}

	/**
	 * Update the position of the panel.
	 */
	@Override
	public void updatePosition(int position) {
		super.updatePosition(position);

		// Pass the new index onto classes that work with it.
		if (webView != null) {
			webView.updatePanelPosition(position);
			onTouchListener.updatePanelPosition(position);
		}

		LOGID = LOG + "[" + position + "]";
	}

	/**
	 * Sets the slide-in animation for the next loaded page.
	 */
	public void setSlideInAnimation(boolean fromLeft) {
		animate = true;
		animateFromLeft = fromLeft;
	}

	/**
	 * Changes the background of the BookPanel to match the background of the book page,
	 * so that transitions between pages are smooth.
	 * @param visualOptions  Visual options for displaying of the book.
	 */
	public void changeCSS(VisualOptions visualOptions) {
		if (panelRelativeLayout != null) {
			if (visualOptions.applyOptions) {
				panelRelativeLayout.setBackgroundColor(visualOptions.getBgColorAsColorInt(governor.getActivity()));
			} else {
				panelRelativeLayout.setBackgroundColor(Color.WHITE);
			}
		}
	}


	// ============================================================================================
	//		Display page or HTML data
	// ============================================================================================

	/**
	 * Loads text data into the WebView.
	 * @param data 		String data to display
	 * @param baseUrl	URL of any file from the associated epub to get proper encoding from it.
	 */
	public void loadData(String data, String baseUrl) {
		Log.d(LOG, LOGID + ".loadData");

		// For explanation see the same section in loadPage().
		if (finishedRenderingContent.get()) {
			loadPositionY = null;
			firstRenderingAfterCreatingActivity = false;

			// Scroll Sync data
			loadScrollSyncMethod = null;
			loadScrollSyncOffset = null;
			loadScrollSyncRatio = null;

			Log.d(LOG, LOGID + " nulling load* variables");
		}

		saveBookPageToDb();

		// This needs to be set only *after* calling saveBookPageToDb().
		finishedRenderingContent.set(false);

		/* Save the data to be loaded and display them if possible. */
		displayedPage = baseUrl;
		displayedData = data;

		if (created) {
			webView.loadDataWithBaseURL(baseUrl, data,
					getActivity().getString(R.string.textOrHTML), null, null);
		}
	}

	/**
	 * Load page through URL path.
	 * @param path to load
	 */
	public void loadPage(String path) {
		Log.d(LOG, LOGID + ".loadPage, created: " + created + ", finishedRenderingContent: " + finishedRenderingContent);

		// We have to null these fields here, because onFinishedRenderingContent() might very rarely get
		// called twice, first time with wrong content height, and nulling the variables then would mean
		// that when it gets called for the second time, it wouldn't do anything. Now when it gets called
		// for the second time, it recalculates the values and corrects the previous mistake.
		if (finishedRenderingContent.get()) {
			loadPositionY = null;
			firstRenderingAfterCreatingActivity = false;

			// Scroll Sync data
			loadScrollSyncMethod = null;
			loadScrollSyncOffset = null;
			loadScrollSyncRatio = null;

			Log.d(LOG, LOGID + ".loadPage: nulling load* variables");
		}

		saveBookPageToDb();

		// Save the page to be displayed and display it if possible.
		displayedPage = path;
		if(created) {
			webView.loadUrl(path);
		}

		loadBookPageFromDb(null);

		// This needs to be set only after calling both saveBookPageToDb() and loadBookPageFromDb().
		finishedRenderingContent.set(false);

		if (animate) {
			// Animate WebView out of visibility
			// so we can animate it back into view in WebViewClient.onPageFinished().
			webView.startAnimation(animateFromLeft ? animationSlideOutRigth : animationSlideOutLeft);
			webView.setVisibility(View.GONE);

			Log.d(LOG, LOGID + ".loadPage: webView.setVisibility GONE");
		}
	}


	// ============================================================================================
	//		Save and load state to preferences or database
	// ============================================================================================

	/**
	 * Saves scroll position and ScrollSync offset into the BookPage database.
	 */
	public void saveBookPageToDb() {
		// Only save when there is something to save and if it's a book page.
		if (displayedPage != null && webView != null && finishedRenderingContent.get()
				&& displayedBookPageKey != null) {

			// Compute the values to be saved.
			String scrollY = "" + getPositionYAsFloat();
			String scrollSyncMethod = "" + webView.getScrollSyncMethod();
			String scrollSyncOffset = "" + webView.getScrollSyncOffsetAsFloat();
			String scrollSyncRatio = "" + webView.getScrollSyncRatio();

			// Obtain database access.
			BookPageDB bookPageDB = BookPageDB.getInstance(governor.getActivity());

			//Log.d(LOG, LOGID + " loadedFromBookPageRowId: " + loadedFromBookPageRowId);

			if (loadedFromBookPageRowId != null) {
				// Update the existing BookPage entry in the DB with new values.
				String[] values = new String[] {scrollY, scrollSyncMethod, scrollSyncOffset, scrollSyncRatio,
						"" + System.currentTimeMillis()};
				bookPageDB.updateBookPage(loadedFromBookPageRowId, colsUpdate, values);
			}
			else {
				// Insert a brand new BookPage entry into the DB.
				String[] values = new String[] {
						displayedBookPageKey[0], displayedBookPageKey[1], displayedBookPageKey[2],
						panelHolder.getBook().getRelativePathFromAbsolute(displayedPage),
						scrollY, scrollSyncMethod, scrollSyncOffset, scrollSyncRatio,
						"" + System.currentTimeMillis()};

				bookPageDB.insertBookPage(colsInsert, values);
			}

			Log.d(LOG, String.format("%s: Saved to DB (id: %s, ScrollY: %s, "
					+ "ScrollSyncMethod: %s, ScrollSyncOffset: %s, ScrollSyncRatio: %s)",
					LOGID, loadedFromBookPageRowId, scrollY, scrollSyncMethod, scrollSyncOffset, scrollSyncRatio));
			//Toast.makeText(ReaderActivity.debugContext, "Saved page to DB", Toast.LENGTH_SHORT).show();

			displayedBookPageKey = null;
			loadedFromBookPageRowId = null;
		}
	}

	/**
	 * Loads scroll position and ScrollSync offset from the BookPage database.
	 * @param latestBookPage BookPage entry to load data from, or NULL so the method contacts database itself.
	 */
	public void loadBookPageFromDb(BookPage latestBookPage) {
		BookPage bookPage = null;

		// Only prepare displayedBookPageKey if enumState=books, because when saveBookPageToDb() gets
		// called, the enumState will be already changed to the new one. This way, we can test
		// whether displayedBookPageKey!=null to achieve the same effect.
		if (enumState == BookPanelState.books && panelHolder.getBook() != null) {

			// Compute the unique identifier of a book page: (bookFilename, bookTitle, pageFilename).
			displayedBookPageKey = new String[] {
					Func.fileNameFromPath(panelHolder.getBook().getFilePath()),
					panelHolder.getBook().getTitle(),
					Func.fileNameFromPath(displayedPage)};

			if (latestBookPage != null) {
				bookPage = latestBookPage;
			} else {
				// Load the BookPage from database if it wasn't provided.
				BookPageDB bookPageDB = BookPageDB.getInstance(governor.getActivity());
				bookPage = bookPageDB.findBookPage(
					displayedBookPageKey[0], displayedBookPageKey[1], displayedBookPageKey[2]);
			}

			// If the BookPage was found in the database.
			if (bookPage != null) {
				loadedFromBookPageRowId = bookPage.getId();
			}
		}

		// Load page only if this isn't the first time we're opening any page in this run of the app,
		// in which case we're loading the data from preferences, and loading from DB would be redundant.
		if ((latestBookPage != null || finishedRenderingContent.get()) && displayedBookPageKey != null) {

			// If the BookPage was found in the database, load the data to be used later!
			if (bookPage != null) {
				loadPositionY = bookPage.getScrollY();

				// Load ScrollSync data
				loadScrollSyncMethod = bookPage.getScrollSyncMethod();
				loadScrollSyncOffset = bookPage.getScrollSyncOffset();
				loadScrollSyncRatio = bookPage.getScrollSyncRatio();

				Log.d(LOG, String.format("%s: Loaded from DB (id: %s, ScrollY: %s, "
						+ "ScrollSyncMethod: %s, ScrollSyncOffset: %s, ScrollSyncRatio: %s)",
						LOGID, loadedFromBookPageRowId, loadPositionY,
						loadScrollSyncMethod, loadScrollSyncOffset, loadScrollSyncRatio));
			} else {
				// If the book page was not found in the database, reset the ScrollSync data in WebView.
				webView.resetScrollSync();

				Log.d(LOG, LOGID + ": NOT Loaded from DB, so webView.resetScrollSync instead.");
			}

			/*Toast.makeText(ReaderActivity.debugContext, "Loaded page from DB: "
					+ (bookPage != null ? "found" : "not found"), Toast.LENGTH_SHORT).show(); /**/
			/*Log.v(LOG, String.format("%s: Searching DB for BookPage with these values: (%s, %s, %s)", LOGID,
					Func.fileNameFromPath(panelHolder.getBook().getFilePath()),
					panelHolder.getBook().getTitle(),
					Func.fileNameFromPath(displayedPageFilename))); /**/
		}
	}

	/**
	 * Returns the scroll position as a fraction of the ContentHeight.
	 * Used when saving to the preferences or the database. Because next time, the page might be opened
	 * in a different sized WebView, so absolute values would be no good.
	 */
	private float getPositionYAsFloat() {
		return (float) webView.getScrollY() / (float) webView.getContentHeight();
	}

	/**
	 * This method gets called by our SelectionWebView when the WebView has definitively finished
	 * rendering its contents and getContentHeight() returns a new non-zero value.
	 *
	 * Warning: May not get called when two pages shorter than the WebView display area get opened after
	 *  each other. For details see SelectionWebView.onDraw() javadoc.
	 * This is, however, not an issue, because in such short pages, loading the scroll position
	 *  from preferences makes no sense anyway.
	 *
	 * Warning 2!: This method might very rarely get called twice, first time with a little premature
	 *  (shorter OR longer) getContentHeight(), and the second time with the correct getContentHeight().
	 * Thus it is important to do not null the load* fields here, so that in case the method gets called
	 *  the second time, it can recalculate the values and get it right!
	 * Nulling of load* values was therefore moved to loadPage/loadData when the user opens another page.
	 */
	public void onFinishedRenderingContent() {
		Log.d(LOG, LOGID + ".onFinishedRenderingContent, loadY: " + loadPositionY
				+ ", contentHeight: " + webView.getContentHeight());

		// Null out the animation, because it already happened.
		animate = false;

		// Load position from before if this is a page opening from before.
		if (loadPositionY != null) {
			webView.setScrollY(Math.round(loadPositionY * webView.getContentHeight()));
			Log.d(LOG, LOGID + ".onFinishedRenderingContent, set webView scrollY: " + webView.getScrollY());
		}

		// Load the scroll sync data
		if (loadScrollSyncMethod != null && loadScrollSyncOffset != null && loadScrollSyncRatio != null) {
			webView.initializeScrollSyncData(loadScrollSyncMethod, loadScrollSyncOffset, loadScrollSyncRatio);
		} else {
			if (firstRenderingAfterCreatingActivity) {
				// Load the scroll sync offset, etc.
				webView.loadStateWhenContentRendered();
				// Do not set this field to false here, because a second, more precise,
				// onFinishedRenderingContent call might come afterwards.
			}
		}

		//Log.d(LOG, LOGID + ".onFinishedRenderingContent: setting finishedRenderingContent = true");
		finishedRenderingContent.set(true);

		if (governor.isScrollSync() && enumState == BookPanelState.books) {
			BookPanel sister = panelHolder.getSisterBookPanel();

			// If this panel is the one that has finished rendering last.
			if (sister != null && sister.finishedRenderingContent.get()) {
				// Check if the Scroll Sync data in both WebViews are congruent.
				if (!webView.areScrollSyncDataCongruentWithSister()) {
					// If not, deactivate scroll sync.
					governor.setScrollSync(false, true);
					Toast.makeText(activity, R.string.Deactivated_Scroll_sync, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	/**
	 * Save state and content of the page.
	 */
	@Override
	public void saveState(Editor editor) {
		super.saveState(editor);
		if (enumState != null) {
			editor.putString("state"+panelPosition, enumState.name());
		}
		if (displayedPage != null) {
			editor.putString("displayedPage"+panelPosition, displayedPage);
		}
		if (enumState == BookPanelState.metadata) {
			editor.putString("displayedData"+panelPosition, displayedData);
		}

		// Save the position within the page.
		if (webView != null) {
			editor.putInt("positionX"+panelPosition, webView.getScrollX());
			editor.putFloat("positionY"+panelPosition, getPositionYAsFloat());
		}

		// Load the scroll sync offset, etc.
		if (webView != null) {
			webView.saveState(editor);
		}
	}

	/**
	 * Load the position within the page from before to be used when webView is instantiated.
	 */
	public void loadScrollPosition(SharedPreferences preferences) {
		loadPositionY = preferences.getFloat("positionY"+panelPosition, 0f);
	}

	/**
	 * Load state and content of the page.
	 * @param creatingActivity  Whether or not the activity is just being created.
	 */
	@Override
	public void loadState(SharedPreferences preferences, boolean creatingActivity) {
		Log.d(LOG, LOGID + ".loadState"); //loadPosX: " + loadPositionX + ", loadPosY: " + loadPositionY

		super.loadState(preferences, creatingActivity);
		try {
			enumState = BookPanelState.valueOf(preferences.getString("state"+panelPosition, BookPanelState.books.name()));
		} catch (IllegalArgumentException e) {
			enumState = BookPanelState.books;
		}

		// If this is one of the BookPanelStates that get closed by pressing the back button,
		// inform governor of that fact so it can close it properly.
		if (enumState == BookPanelState.notes || enumState == BookPanelState.metadata) {
			governor.notesDisplayedLastIn = panelHolder;
		}

		String page = preferences.getString("displayedPage"+panelPosition, "");

		if (enumState == BookPanelState.metadata) {
			loadData(preferences.getString("displayedData"+panelPosition, ""), page);
		} else {
			Log.d(LOG, LOGID + ".loadState() calling loadPage(" + page + ")");
			loadPage(page);
		}

		loadScrollPosition(preferences);

		if (creatingActivity) {
			// Set this so that WebView loads its state upon finishing rendering of content
			firstRenderingAfterCreatingActivity = true;
		}
	}

	/**
	 * Activity calls this when a runtime change happens in case we need to do something about it.
	 * Examples: Screen orientation changed, entered/exited fullscreen, etc.
	 */
	public void onRuntimeChange() {
		Log.d(LOG, LOGID + ".onOrientationChanged");

		prepareForReloadOfTheSamePage();
	}

	/**
	 * When the same page is going to get reloaded in this Book Panel for some reason
	 * (e.g. reattaching the panel, change of orientation, ...), we save the current state variables
	 * to be loaded after the reload in onFinishedRenderingContent().
	 */
	public void prepareForReloadOfTheSamePage() {
		Log.d(LOG, LOGID + ".prepareForSamePageReload");

		// Save the scroll variables so that if content is going to get re-rendered,
		// we load the proper values.
		loadPositionY = getPositionYAsFloat();
		if (webView != null) {
			loadScrollSyncMethod = webView.getScrollSyncMethod();
			loadScrollSyncOffset = webView.getScrollSyncOffsetAsFloat();
			loadScrollSyncRatio = webView.getScrollSyncRatio();
		}

	}

}
