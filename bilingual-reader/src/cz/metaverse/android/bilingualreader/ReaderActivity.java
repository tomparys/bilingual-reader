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

package cz.metaverse.android.bilingualreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.dialog.ChangeCSSDialog;
import cz.metaverse.android.bilingualreader.dialog.LanguageChooserDialog;
import cz.metaverse.android.bilingualreader.dialog.PanelSizeDialog;
import cz.metaverse.android.bilingualreader.dialog.SettingsDialog;
import cz.metaverse.android.bilingualreader.manager.PanelNavigator;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;
import cz.metaverse.android.bilingualreader.sync.ParagraphPositions;

/**
 *
 * The main Activity of our application.
 * Here it all begins, here it all ends.
 *
 */
public class ReaderActivity extends Activity implements View.OnSystemUiVisibilityChangeListener {

	public PanelNavigator navigator;
	protected int bookSelector;
	protected int panelCount;
	protected String[] cssSettings;

	// Navigation Drawer
	private DrawerLayout navigationDrawerLayout;
	private ActionBarDrawerToggle actionBarDrawerToggle;
	private Button[] drawerBookButton;
	private String[] drawerBookButtonText;

	// Request codes so we know from which Activity we have just returned.
	public static final int ACTIVITY_RESULT_FILE_CHOOSER = 1;
	public static final int ACTIVITY_RESULT_SRS_DATABASE = 2;

	// Fields pertaining to full-screen mode
	private View decorView;
	private boolean fullscreenMode = false;

	// Used exclusively for debugging purposes (e.g. Displaying toasts without context)
	public static Context debugContext;	// TODO remove when no longer needed


	/**
	 * Called when the application gets started.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize our ability to switch to fullscreen.
		decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(this);

		// Set the Activity title that will be displayed on the ActionBar (among other places).
		setTitle(R.string.action_bar_title);

		/*
		 * Navigation Drawer setup
		 */
		navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		// Extend the ActionBarDrawerToggle class
		actionBarDrawerToggle = new ActionBarDrawerToggle(
				this,				  /* host Activity */
				navigationDrawerLayout,		 /* DrawerLayout object */
				R.string.drawer_open,  /* "open drawer" description */
				R.string.drawer_close  /* "close drawer" description */
				) {

			/** Called when a drawer has settled in a completely closed state. */
			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				//getActionBar().setTitle(actionBarTitle);
			}

			/** Called when a drawer has settled in a completely open state. */
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				//getActionBar().setTitle(navigationDrawerTitle);
			}
		};
		navigationDrawerLayout.setDrawerListener(actionBarDrawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Find the Book buttons that we will be updating with the names of the displayed books
		drawerBookButton = new Button[PanelNavigator.NUMBER_OF_PANELS];
		drawerBookButton[0] = (Button) findViewById(R.id.drawer_book_title_1_button);
		drawerBookButton[1] = (Button) findViewById(R.id.drawer_book_title_2_button);

		// Restore the buttons text after runtime change
		if (savedInstanceState != null) {
			drawerBookButtonText = savedInstanceState.getStringArray("nps_drawerBookButtonText");
			restoreBookNamesInDrawer(drawerBookButtonText);
		}
		if (drawerBookButtonText == null) {
			drawerBookButtonText = new String[PanelNavigator.NUMBER_OF_PANELS];
		}

		/* end of Navigation Drawer setup */


		// Setup logic variables
		navigator = PanelNavigator.getSingleton(this);

		panelCount = 0;
		if (savedInstanceState != null) {
			// When trying to use "getString(R.string.nonPersistentState_panelCount)" as key, the value is
			//  just NOT retrieved. The same if the key is too long, e.g. "nonPersistentState_panelCount".
			panelCount = savedInstanceState.getInt("nps_panelCount", 0);
			cssSettings = savedInstanceState.getStringArray("nps_cssSettings");
		}
		if (cssSettings == null) {
			cssSettings = new String[8];
		}

		debugContext = getBaseContext();


		// Load persistent state and create panels from before if needed. If the Activity is just being
		// recreated because of runtime configuration change, no need to do anything.
		if (savedInstanceState == null) {
			// Load the persistent state from previous runs of the application,
			// because this is the application is just starting.
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			loadStateOfNavigator(preferences);

			// Load panels.
			navigator.loadViews(preferences);
		}

		// If there are no panels, start FileChooser.
		if (panelCount == 0) {
			bookSelector = 0;
			Intent goToChooser = new Intent(this, FileChooserActivity.class);
			startActivityForResult(goToChooser, ACTIVITY_RESULT_FILE_CHOOSER);
		}
	}

	/**
	 * Called before placing the activity in a background state, such as
	 *   - when a new Activity opens up on top of it
	 *   - when a Runtime Change occurs (e.g. screen orientation change).
	 * The resulting Bundle is then passed to onCreate(Bundle) when the Activity gets back into focus.
	 *
	 * The bundle is used to save the _non-persistent_ application state, persistent state should be saved
	 * in onPause().
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Save non-persistent state:
		//  When trying to use "getString(R.string.nonPersistentState_panelCount)" as key, the value is
		//  just NOT retrieved. The same if the key is too long, e.g. "nonPersistentState_panelCount".
		outState.putInt("nps_panelCount", panelCount);
		outState.putStringArray("nps_cssSettings", cssSettings);
		outState.putStringArray("nps_drawerBookButtonText", drawerBookButtonText);

		// --- Removing panels is no longer necessary, because they are being retained through
		//	   the recreation of the Activity. Leaving just in case.
		// Remove current panels from view (they still exist, they just won't be attached
		//  to the FragmentManager and therefore won't be displayed.
		// They will be readded in in onResume() or they will be recreated and readded
		//  in onCreate() after a runtime configuration change.
		// This is *not* possible to move to onDestroy, because FragmentManager refuses to do
		//  anything after onSaveInstanceState, because it might cause potential problems when recreating
		//  the Activity from the saved state.
		//navigator.removePanels();

		super.onSaveInstanceState(outState);
	}

	/**
	 * Called when the app gets focus.
	 *  Either for the first time (after onCreate), or after losing it first (after onPause).
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// If panelCount is zero, we can be sure we're getting focus back,
		// 	because otherwise FileChooser intent would have been launched in onCreate.
		if (panelCount == 0) {
			// Load panels and books into them from before if necessary,
			// if not, just re-adds the panels to view.
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			navigator.loadViews(preferences);
		}

		// Invalidate options menu, because at the start the ebooks weren't loaded
		// and we didn't know whether they had bilingual support.
		invalidateOptionsMenu();
	}

	/**
	 * Called when the app loses focus.
	 * 	App might then get killed to free memory, or might continue when focus gets back (onResume).
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Save state in case the app gets killed.
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		saveStateOfNavigator(editor);
		editor.commit();
	}

	/**
	 * Called when the FileChooser (or possibly other) Intent we launched sends back results.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// If no panels present, load them from memory, if there were any before.
		if (panelCount == 0) {
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			navigator.loadViews(preferences);
		}

		// If we have just returned from the FileChooserActivity.
		if (requestCode == ACTIVITY_RESULT_FILE_CHOOSER) {
			// Open the selected book in a given panel if all went well.
			if (resultCode == Activity.RESULT_OK) {
				String path = data.getStringExtra(getString(R.string.bpath));
				navigator.openBook(path, bookSelector);
			}
		}
	}

	/**
	 * Called when the user presses the Back key.
	 */
	@Override
	public void onBackPressed() {
		// See if there is some PanelViewState.notes or PanelViewState.metadata panel to be closed,
		// if so, close it. If not, moveTaskToBack().
		if (!navigator.closeLastOpenedNotes()) {
			moveTaskToBack(true);
		}
	}



	// ============================================================================================
	//		Full-screen mode
	// ============================================================================================

	/**
	 * Switches between full-screen and normal mode.
	 */
	public void switchFullscreen() {
		if (fullscreenMode) {
			deactivateFullscreen();
		} else {
			activateFullscreen();
		}
	}

	/**
	 * Activates full-screen mode - hides status bar, system navigation and the Action Bar.
	 */
	@SuppressLint("InlinedApi") // Android versions not supporting Immersive Fullscreen ignore unsupported flags.
	public void activateFullscreen() {
		// Set basic fullscreen flags
		int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  // Hides the system navigation
				| View.SYSTEM_UI_FLAG_FULLSCREEN;  // Hides the status bar

		// Set flags for devices with support for immersive fullscreen.
		if (Build.VERSION.SDK_INT >= 19) {
			// The last two flags make for a smoother transition in android versions with IMMERSIVE fullscreen.
			flags = flags
					| View.SYSTEM_UI_FLAG_IMMERSIVE  // Ensures that no touch events won't cancel fullscreen mode.
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  // Ensures more seamless transition.
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;  // Ensures more seamless transition.
		}

		// Hide action bar and activate fullscreen flags.
		getActionBar().hide();
		decorView.setSystemUiVisibility(flags);
	}

	// This snippet shows the system bars. It does this by removing all the flags
	// except for the ones that make the content appear under the system bars.
	/**
	 * Deactivates full-screen mode - shows status bar, system navigation and the Action Bar.
	 */
	public void deactivateFullscreen() {
		getActionBar().show();
		decorView.setSystemUiVisibility(0); // Seting no flags returns all to normal.
	}

	/**
	 * Called when the visibility of the System UIs changes - when we enter or leave fullscreen mode.
	 */
	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		// Note that system bars will only be "visible" if none of the
		//  LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
		// But we're setting only the SYSTEM_UI_FLAG_FULLSCREEN flag, so we test for it.
		if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
			// The system bars are visible - show the action bar if it is hidden.
			fullscreenMode = false;
			if (!getActionBar().isShowing()) {
				getActionBar().show();
			}
		} else {
			// The system bars are NOT visible - hide the action bar if it is shown.
			fullscreenMode = true;
			if (getActionBar().isShowing()) {
				getActionBar().hide();
			}
		}
	}

	/**
	 * Called when we have gained or lost focus.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			if (fullscreenMode) {
				// If we have regained back focus and fullscreenImmersion was active before,
				// re-hide system UI.
				activateFullscreen();
			}
		}
	}



	// ============================================================================================
	//		Navigation Drawer
	// ============================================================================================

	/**
	 * Called when user presses any button inside the navigation drawer.
	 */
	public void onDrawerButtonClicked(View view) {
		// Handle the pressed navigation drawer button.
		switch(view.getId()) {

		// Book title 1 - open Table of Contents
		case R.id.drawer_book_title_1_button:
			if (!navigator.displayTOC(0)) {
				errorMessage(getString(R.string.error_tocNotFound));
			}
			break;

		// Open book 1
		case R.id.drawer_open_book_1_button:
			bookSelector = 0;
			Intent goToChooser1 = new Intent(ReaderActivity.this, FileChooserActivity.class);
			goToChooser1.putExtra(getString(R.string.second), getString(R.string.time));
			startActivityForResult(goToChooser1, ACTIVITY_RESULT_FILE_CHOOSER);
			break;

		// Book title 2 - open Table of Contents
		case R.id.drawer_book_title_2_button:
			// If two books are open or if we're reading a bilingual book
			if (!navigator.exactlyOneBookOpen() || navigator.isReadingBilingualEbook()) {
				// Open ToC of the second book
				if (!navigator.displayTOC(1)) {
					errorMessage(getString(R.string.error_tocNotFound));
				}
			} else {
				// If only one non-bilingual book is opened, act like open_book_2_button.
				bookSelector = 1;
				Intent goToChooser2 = new Intent(ReaderActivity.this, FileChooserActivity.class);
				goToChooser2.putExtra(getString(R.string.second), getString(R.string.time));
				startActivityForResult(goToChooser2, ACTIVITY_RESULT_FILE_CHOOSER);
			}
			break;

		// Open book 2
		case R.id.drawer_open_book_2_button:
			bookSelector = 1;
			Intent goToChooser2 = new Intent(ReaderActivity.this, FileChooserActivity.class);
			goToChooser2.putExtra(getString(R.string.second), getString(R.string.time));
			startActivityForResult(goToChooser2, ACTIVITY_RESULT_FILE_CHOOSER);
			break;

		// SRS Database
		case R.id.drawer_SRS_database_button:
			startActivityForResult(new Intent(this, SRSDatabaseActivity.class), ACTIVITY_RESULT_SRS_DATABASE);
			break;

		// Settings
		case R.id.drawer_settings_button:
			openSettings();
			break;

		// Exit
		case R.id.drawer_exit_button:
			finish();
			break;
		}

		navigationDrawerLayout.closeDrawer(Gravity.START);
	}

	/**
	 * Changes the name of the book displayed in the navigation drawer.
	 * @param panel	Panel of which the book name has changed.
	 * @param name	The new name to display for that panel.
	 */
	public void setBookNameInDrawer(int panel, String name) {
		if (drawerBookButton[panel] != null) {
			drawerBookButton[panel].setText(name);
		}
		drawerBookButtonText[panel] = name;
	}

	/**
	 * Restores the names of the books after a runtime change re-creates the Activity
	 */
	private void restoreBookNamesInDrawer(String[] drawerBookButtonText) {
		for (int i = 0; i < PanelNavigator.NUMBER_OF_PANELS; i++) {
			if (drawerBookButtonText[i] != null) {
				setBookNameInDrawer(i, drawerBookButtonText[i]);
			}
		}
	}

	/**
	 * Called when Activity start-up is complete
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Sync the toggle state after onRestoreInstanceState has occurred.
		actionBarDrawerToggle.syncState();
	}

	/**
	 * Called when the device configuration changes.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
	}



	// ============================================================================================
	//		Action Bar / options menu
	// ============================================================================================

	/**
	 * Called when menu is opened.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	/**
	 * Called right before menu is displayed.
	 *
	 * Makes visible only the relevant menu options.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (navigator.exactlyOneBookOpen()) {

			// Exactly one book open
			menu.findItem(R.id.sync_scroll_menu_item).setVisible(false);
			menu.findItem(R.id.sync_chapters_menu_item).setVisible(false);

			// Submenus
			menu.findItem(R.id.bilingual_ebook_1_menu_item).setVisible(false);
			menu.findItem(R.id.bilingual_ebook_2_menu_item).setVisible(false);
			menu.findItem(R.id.metadata_1_menu_item).setVisible(false);
			menu.findItem(R.id.metadata_2_menu_item).setVisible(false);
			menu.findItem(R.id.table_of_contents_1_menu_item).setVisible(false);
			menu.findItem(R.id.table_of_contents_2_menu_item).setVisible(false);
			menu.findItem(R.id.style_1_menu_item).setVisible(false);
			menu.findItem(R.id.style_2_menu_item).setVisible(false);
			menu.findItem(R.id.audio_1_menu_item).setVisible(false);
			menu.findItem(R.id.audio_2_menu_item).setVisible(false);

		} else {

			// Two books open
			menu.findItem(R.id.sync_scroll_menu_item).setVisible(true);
			menu.findItem(R.id.sync_chapters_menu_item).setVisible(true);

			// Submenus
			menu.findItem(R.id.style_1_menu_item).setVisible(true);
			menu.findItem(R.id.style_2_menu_item).setVisible(true);
			menu.findItem(R.id.audio_1_menu_item).setVisible(true);
			menu.findItem(R.id.audio_2_menu_item).setVisible(true);

			if (navigator.isReadingBilingualEbook()) {
				// Bilingual ebook open in both panels has only 1 ToC and 1 metadata.
				menu.findItem(R.id.bilingual_ebook_1_menu_item).setVisible(false);
				menu.findItem(R.id.bilingual_ebook_2_menu_item).setVisible(false);
				menu.findItem(R.id.metadata_1_menu_item).setVisible(false);
				menu.findItem(R.id.metadata_2_menu_item).setVisible(false);
				menu.findItem(R.id.table_of_contents_1_menu_item).setVisible(false);
				menu.findItem(R.id.table_of_contents_2_menu_item).setVisible(false);
			} else {
				menu.findItem(R.id.bilingual_ebook_1_menu_item).setVisible(true);
				menu.findItem(R.id.bilingual_ebook_2_menu_item).setVisible(true);
				menu.findItem(R.id.metadata_1_menu_item).setVisible(true);
				menu.findItem(R.id.metadata_2_menu_item).setVisible(true);
				menu.findItem(R.id.table_of_contents_1_menu_item).setVisible(true);
				menu.findItem(R.id.table_of_contents_2_menu_item).setVisible(true);
			}
		}

		if (anyBilingualEbook()) {
			menu.findItem(R.id.bilingual_ebook_menu_item).setVisible(true);
		} else {
			menu.findItem(R.id.bilingual_ebook_menu_item).setVisible(false);
		}

		if (navigator.canExtractAudio()) {
			menu.findItem(R.id.audio_menu_item).setVisible(true);
		} else {
			menu.findItem(R.id.audio_menu_item).setVisible(false);
		}

		// If there is only one view, option "changeSizes" is not displayed.
		if (panelCount == 1)
			menu.findItem(R.id.change_size_menu_item).setVisible(false);
		else
			menu.findItem(R.id.change_size_menu_item).setVisible(true);

		return true;
	}

	/**
	 * Called when Menu option is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns true, then it has handled
		//  the app icon touch event that opens/closes the navigation drawer.
		if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
		  return true;
		}

		// If a classic options-menu item was selected, close the navigation drawer if it was open.
		if (navigationDrawerLayout.isDrawerOpen(Gravity.START)) {
			navigationDrawerLayout.closeDrawer(Gravity.START);
		}


		// Now handle our menu items.
		switch (item.getItemId()) {

		// Change relative size of panels
		case R.id.change_size_menu_item:
			try {
				// Display dialog to pick new relative size of the panels.
				DialogFragment newFragment = new PanelSizeDialog();
				newFragment.show(getFragmentManager(), "");
			} catch (Exception e) {
				errorMessage(getString(R.string.error_cannotChangeSizes));
			}
			return true;

		// Sync Scroll
		case R.id.sync_scroll_menu_item:
			// TODO Make use of the information, not just compute it.
			Toast.makeText(this, "Computing paragraph positions.", Toast.LENGTH_SHORT).show();

			// Get position of each paragraph for proper synchronized scrolling.
			for (int panel = 0; panel < navigator.getNBooks(); panel++) {
				ParagraphPositions ppInstance = ParagraphPositions.instance(panel);
				if (!ppInstance.isActive()) {
					ppInstance.start(navigator.getBookPanel(panel),
							navigator.getBookPanel(panel).getViewedPage());
				}
			}
			return true;

		// Sync Chapters
		case R.id.sync_chapters_menu_item:
			boolean sync = navigator.flipSyncChapters();
			if (!sync) {
				errorMessage(getString(R.string.error_onlyOneBookOpen));
			}
			if (navigator.isSyncChapters()) {
				Toast.makeText(this, getString(R.string.activated_sync_chapters), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, getString(R.string.deactivated_sync_chapters), Toast.LENGTH_SHORT).show();
			}

			return true;

		// Bilingual ebook
		case R.id.bilingual_ebook_menu_item:
			if (navigator.exactlyOneBookOpen() || navigator.isReadingBilingualEbook()) {
				if (openBilingualBook(0)) {
					invalidateOptionsMenu();
				}
			}
			return true;

		case R.id.bilingual_ebook_1_menu_item:
			if (openBilingualBook(0)) {
				invalidateOptionsMenu();
			}
			return true;

		case R.id.bilingual_ebook_2_menu_item:
			if (!navigator.exactlyOneBookOpen()) {
				if (openBilingualBook(1)) {
					invalidateOptionsMenu();
				}
			} else {
				errorMessage(getString(R.string.error_onlyOneBookOpen));
			}
			return true;

		// Display metadata of the book
		case R.id.metadata_menu_item:
			if (navigator.exactlyOneBookOpen() == true || navigator.isReadingBilingualEbook() == true) {
				navigator.displayMetadata(0);
			} else {
			}
			return true;

		case R.id.metadata_1_menu_item:
			if (!navigator.displayMetadata(0))
				errorMessage(getString(R.string.error_metadataNotFound));
			return true;

		case R.id.metadata_2_menu_item:
			if (!navigator.displayMetadata(1))
				errorMessage(getString(R.string.error_metadataNotFound));
			return true;

		// Table of contents
		case R.id.table_of_contents_menu_item:
			if (navigator.exactlyOneBookOpen() == true || navigator.isReadingBilingualEbook() == true) {
				navigator.displayTOC(0);
			}
			return true;

		case R.id.table_of_contents_1_menu_item:
			if (!navigator.displayTOC(0))
				errorMessage(getString(R.string.error_tocNotFound));
			return true;

		case R.id.table_of_contents_2_menu_item:
			if (!navigator.displayTOC(1))
				errorMessage(getString(R.string.error_tocNotFound));
			return true;

		// Change style
		case R.id.style_menu_item:
			try {
				// Display the style dialog.
				if (navigator.exactlyOneBookOpen() == true) {
					DialogFragment newFragment = new ChangeCSSDialog();
					newFragment.show(getFragmentManager(), "");
					bookSelector = 0;
				}
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		case R.id.style_1_menu_item:
			try {
				// Display the style dialog.
				DialogFragment newFragment = new ChangeCSSDialog();
				newFragment.show(getFragmentManager(), "");
				bookSelector = 0;
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		case R.id.style_2_menu_item:
			try {
				// Display the style dialog.
				DialogFragment newFragment = new ChangeCSSDialog();
				newFragment.show(getFragmentManager(), "");
				bookSelector = 1;
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		// Audio shenanigans.
		case R.id.audio_menu_item:
			if (navigator.exactlyOneBookOpen() == true) {
				if (!navigator.extractAudio(0)) {
					errorMessage(getString(R.string.no_audio));
				}
			}
			return true;

		case R.id.audio_1_menu_item:
			if (!navigator.extractAudio(0))
				errorMessage(getString(R.string.no_audio));
			return true;

		case R.id.audio_2_menu_item:
			if (!navigator.extractAudio(1))
				errorMessage(getString(R.string.no_audio));
			return true;

		// And finally, the default option.
		default:
			return super.onOptionsItemSelected(item);
		}
	}



	// ============================================================================================
	//		Panels Manager
	// ============================================================================================

	/**
	 * Adds a panel to view for the first time.
	 * @param p SplitPanel instance
	 */
	public void addPanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.add(R.id.PanelsLayout, p, p.getTag());
		fragmentTransaction.commit();

		panelCount++;
	}

	/**
	 * Re-attach a panel that has been detached previously.
	 * @param p
	 */
	public void attachPanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.attach(p);
		fragmentTransaction.commit();

		panelCount++;

		// Rethinks what menu items to display
		invalidateOptionsMenu();
	}

	/**
	 * Detach panel from view.
	 * @param p
	 */
	public void detachPanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.detach(p);
		fragmentTransaction.commit();

		panelCount--;

		// Rethinks what menu items to display
		invalidateOptionsMenu();
	}

	/**
	 * Remove panel, but don't close the application.
	 * @param p
	 */
	public void removePanelWithoutClosing(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.remove(p);
		fragmentTransaction.commit();

		panelCount--;

		// Rethinks what menu items to display
		invalidateOptionsMenu();
	}

	/**
	 * Remove panel and if there are no more, end the application.
	 * @param p
	 */
	public void removePanel(SplitPanel p) {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.remove(p);
		fragmentTransaction.commit();
		panelCount--;

		// Close the app if there are no panels left
		if (panelCount <= 0) {
			finish();
		}

		// Rethinks what menu items to display
		invalidateOptionsMenu();
	}



	// ============================================================================================
	//		Misc
	// ============================================================================================

	/**
	 * Opens the Settings dialog.
	 */
	public void openSettings() {
		new SettingsDialog().show(getFragmentManager(), "settings_dialog");
	}

	/**
	 * Finds out if any of the open books contains multiple languages or not.
	 * @return		Truth
	 */
	public boolean anyBilingualEbook() {
		String[] languages;
		for (int i = 0; i < navigator.getNBooks(); i++) {
			languages = navigator.getLanguagesInABook(i);

			// If there are two or more languages, it is a multilingual ebook.
			if (languages != null && languages.length >= 2) {
				return true;
			}
		}
		// No multilingual book found.
		return false;
	}


	/**
	 * Open a bilingual book - if it contains more than 2 languages, let the user pick which he wants.
	 * @return  True if bilingual book was just opened,
	 * 			False if language chooser dialog was opened, or if this is not a bilingual book.
	 */
	public boolean openBilingualBook(int book) {

		String[] languages;
		languages = navigator.getLanguagesInABook(book);

		// If there are just two languages, start parallel text mode with them.
		if (languages.length == 2) {
			startParallelText(book, 0, 1);
			return true;

		// If there are more than 2 languages, show a dialog to pick the two languages
		// 	with which to start parallel text mode.
		} else if (languages.length > 0) {
			Bundle bundle = new Bundle();
			bundle.putInt(getString(R.string.tome), book);
			bundle.putStringArray(getString(R.string.lang), languages);

			LanguageChooserDialog langChooser = new LanguageChooserDialog();
			langChooser.setArguments(bundle);
			langChooser.show(getFragmentManager(), "");
			return false;
		} else {
			errorMessage(getString(R.string.error_noOtherLanguages));
			return false;
		}
	}

	/**
	 * Start parallel text mode
	 * @param book		book containing the parallel text
	 * @param first		first language to show
	 * @param second	second language to show
	 */
	public void startParallelText(int book, int first, int second) {
		navigator.activateBilingualEbook(book, first, second);
	}


	// ---- Change CSS Style section

	/**
	 * Activate the CSS settings after they have been filled in.
	 */
	public void setCSS() {
		navigator.changeCSS(bookSelector, cssSettings);
	}

	public void setBackColor(String my_backColor) {
		cssSettings[1] = my_backColor;
	}

	public void setColor(String my_color) {
		cssSettings[0] = my_color;
	}

	public void setFontType(String my_fontFamily) {
		cssSettings[2] = my_fontFamily;
	}

	public void setFontSize(String my_fontSize) {
		cssSettings[3] = my_fontSize;
	}

	public void setLineHeight(String my_lineHeight) {
		if (my_lineHeight != null)
			cssSettings[4] = my_lineHeight;
	}

	public void setAlign(String my_Align) {
		cssSettings[5] = my_Align;
	}

	public void setMarginLeft(String mLeft) {
		cssSettings[6] = mLeft;
	}

	public void setMarginRight(String mRight) {
		cssSettings[7] = mRight;
	}


	/**
	 * Change the relative weight of the two panels, this change their relative size.
	 * @param weight	weight of the first panel (0 to 1)
	 */
	public void changePanelsWeight(float weight) {
		navigator.changePanelsWeight(weight);
	}

	/**
	 * @return height of the display area
	 */
	public int getHeight() {
		LinearLayout main = (LinearLayout) findViewById(R.id.PanelsLayout);
		return main.getMeasuredHeight();
	}

	/**
	 * @return width of the display area
	 */
	public int getWidth() {
		LinearLayout main = (LinearLayout) findViewById(R.id.PanelsLayout);
		return main.getWidth();
	}

	/**
	 * Save state of the application.
	 */
	protected void saveStateOfNavigator(Editor editor) {
		navigator.saveState(editor);
	}

	/**
	 * Load state of the application from before.
	 */
	protected void loadStateOfNavigator(SharedPreferences preferences) {
		if (!navigator.loadState(preferences))
			errorMessage(getString(R.string.error_cannotLoadState));
	}

	/**
	 * Display an error message as an Android toast.
	 */
	public void errorMessage(String message) {
		Context context = getApplicationContext();
		Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.show();
	}
}
