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

package cz.metaverse.android.bilingualreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.dialog.CloseOrHidePanelDialog;
import cz.metaverse.android.bilingualreader.dialog.DictionaryDialog;
import cz.metaverse.android.bilingualreader.dialog.InfotextBrowserDialog;
import cz.metaverse.android.bilingualreader.dialog.InfotextDialog;
import cz.metaverse.android.bilingualreader.dialog.LanguageChooserDialog;
import cz.metaverse.android.bilingualreader.dialog.PanelSizeDialog;
import cz.metaverse.android.bilingualreader.dialog.ScrollSyncDialog;
import cz.metaverse.android.bilingualreader.dialog.VisualOptionsDialog;
import cz.metaverse.android.bilingualreader.helper.DontShowAgain;
import cz.metaverse.android.bilingualreader.manager.Governor;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;

/**
 *
 * The main Activity of our application.
 * Here it all begins, here it all ends.
 *
 */
public class ReaderActivity extends Activity implements View.OnSystemUiVisibilityChangeListener {

	private static final String LOG = "ReaderActivity";

	public Governor governor;
	protected int bookSelector;
	protected int panelCount;

	// Navigation Drawer
	private DrawerLayout navigationDrawerLayout;
	private ActionBarDrawerToggle actionBarDrawerToggle;
	private Button[] drawerBookButton;
	private String[] drawerBookButtonText;
	private Button drawerHideOrReappearPanelButton;

	// Request codes so we know from which Activity we have just returned.
	public static final int ACTIVITY_RESULT_FILE_CHOOSER = 1;
	public static final int ACTIVITY_RESULT_SRS_DATABASE = 2;

	// Fields pertaining to full-screen mode
	private View decorView;
	private boolean fullscreenMode = false;

	// Used exclusively for debugging purposes (e.g. Displaying toasts without context)
	public static Context debugContext;	// TODO remove when no longer needed

	private boolean doNotLoadGovernorThisTimeInOnResume;


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
		drawerBookButton = new Button[Governor.N_PANELS];
		drawerBookButton[0] = (Button) findViewById(R.id.drawer_book_title_1_button);
		drawerBookButton[1] = (Button) findViewById(R.id.drawer_book_title_2_button);

		drawerHideOrReappearPanelButton = (Button) findViewById(R.id.drawer_hide_panel_button);

		// Restore the buttons text after runtime change
		if (savedInstanceState != null) {
			drawerBookButtonText = savedInstanceState.getStringArray("nps_drawerBookButtonText");
			restoreBookNamesInDrawer(drawerBookButtonText);
		}
		if (drawerBookButtonText == null) {
			drawerBookButtonText = new String[Governor.N_PANELS];
		}

		/* end of Navigation Drawer setup */


		if (savedInstanceState != null) {
			// When trying to use "getString(R.string.nonPersistentState_panelCount)" as key, the value is
			//  just NOT retrieved. The same if the key is too long, e.g. "nonPersistentState_panelCount".
			fullscreenMode = savedInstanceState.getBoolean("nps_fullscreenMode", false);
		}

		debugContext = getBaseContext();

		// Fullscreen: Reactivate if it was active before.
		if (fullscreenMode) {
			activateFullscreen(true);
		}

		// Load the Governor and panels from persistent state if needed.
		loadGovernorAndPanelsIfNeeded(true);
		doNotLoadGovernorThisTimeInOnResume = true;
	}

	/**
	 * Loads the Governor and panels from persistent state memory if needed.
	 * @param creatingActivity  Whether or not the acitivity is just being created.
	 */
	private void loadGovernorAndPanelsIfNeeded(boolean creatingActivity) {
		// Setup logic variables
		// Load persistent state and create panels from before if needed.
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		governor = Governor.loadAndGetSingleton(this, preferences, creatingActivity);
		panelCount = governor.loadPanels(preferences, creatingActivity);
	}

	/**
	 * Saves the Governor and panels states into persistent memory.
	 */
	private void saveGovernorAndPanels() {
		// Save state in case the app gets killed.
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		Editor editor = preferences.edit();
		governor.saveState(editor);
		editor.commit();
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
		outState.putBoolean("nps_fullscreenMode", fullscreenMode);
		outState.putStringArray("nps_drawerBookButtonText", drawerBookButtonText);

		// In case it is ever needed, this is the best place to remove panels FragmentManager
		//  (i.e. remove them from view) before the activity gets destroyed and recreated.
		//  Just test for isFinishing() to see if it will get recreated.
		// This is *not* possible to move to onDestroy, because FragmentManager refuses to do
		//  anything after onSaveInstanceState, since it might cause potential problems when recreating
		//  the Activity from the saved state.

		super.onSaveInstanceState(outState);
	}

	/**
	 * Called when the app gets focus.
	 *  Either for the first time (after onCreate), or after losing it first (after onPause).
	 */
	@Override
	protected void onResume() {
		Log.d(LOG, "ReaderActivity.onResume");
		super.onResume();

		// If we have just loaded Governor in onCreate or onActivityResult, do nothing this time.
		if (doNotLoadGovernorThisTimeInOnResume) {
			doNotLoadGovernorThisTimeInOnResume = false;
		} else {
			// Load the Governor and panels from persistent state if needed.
			loadGovernorAndPanelsIfNeeded(false);
		}

		// If there are no panels, start FileChooser.
		if (panelCount == 0) {
			startOpenFileActivity(0);
		}

		// Invalidate options menu, because at the start the ebooks weren't loaded
		// and we didn't know whether they had bilingual support.
		invalidateOptionsMenu();

		Log.d(LOG, "ReaderActivity.onResume finished");
	}

	/**
	 * Called when the app loses focus.
	 * 	App might then get killed to free memory, or might continue when focus gets back (onResume).
	 */
	@Override
	protected void onPause() {
		Log.d(LOG, "ReaderActivity.onPause");
		super.onPause();

		saveGovernorAndPanels();
	}

	/**
	 * Called when the device configuration changes.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(LOG, LOG + ".onConfigurationChanged");
		super.onConfigurationChanged(newConfig);

		// Signal to the Governor that a runtime change has occurred.
		governor.onRuntimeChange();

		// Change the orientation of the PanelsLayout accordingly.
		LinearLayout panelsLayout = (LinearLayout) findViewById(R.id.PanelsLayout);
		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			panelsLayout.setOrientation(LinearLayout.VERTICAL);
		} else {
			panelsLayout.setOrientation(LinearLayout.HORIZONTAL);
		}

		// Set the divider again, because it needs to be redrawn due to the orientation change.
		panelsLayout.setDividerDrawable(ContextCompat.getDrawable(this, R.drawable.divider_panels));

		// Inform ActionBar Drawer Toggle of the change.
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Called when the FileChooser (or possibly other) Intent we launched sends back results.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(LOG, "ReaderActivity.onActivityResult");

		// If the user has changed orientation when away from this activity, the Drawer will be left opened.
		if (navigationDrawerLayout.isDrawerOpen(Gravity.START)) {
			navigationDrawerLayout.closeDrawer(Gravity.START);
		}

		// Load the Governor and panels from persistent state if needed.
		loadGovernorAndPanelsIfNeeded(false);
		doNotLoadGovernorThisTimeInOnResume = true;

		// If we have just returned from the FileChooserActivity.
		if (requestCode == ACTIVITY_RESULT_FILE_CHOOSER) {
			// Open the selected book in a given panel if all went well.
			if (resultCode == Activity.RESULT_OK) {
				Log.d(LOG, "ReaderActivity.Loading book to panel " + bookSelector);

				String path = data.getStringExtra(getString(R.string.bpath));
				governor.getPanelHolder(bookSelector).openBook(path);

				Log.d(LOG, "ReaderActivity.Loaded book to panel " + bookSelector);
			}
		}
	}

	/**
	 * Called when the user presses the Back key.
	 */
	@Override
	public void onBackPressed() {
		// If the navigation drawer is opened, close it.
		if (navigationDrawerLayout.isDrawerOpen(Gravity.START)) {
			navigationDrawerLayout.closeDrawer(Gravity.START);
		}
		// See if there is some PanelViewState.notes or PanelViewState.metadata panel to be closed,
		// if so, close it. If not, moveTaskToBack().
		else if (!governor.closeLastOpenedNotes()) {
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
			activateFullscreen(true);
		}
	}

	/**
	 * Activates full-screen mode - hides status bar, system navigation and the Action Bar.
	 */
	@SuppressLint("InlinedApi") // Android versions not supporting Immersive Fullscreen ignore unsupported flags.
	public void activateFullscreen(boolean setFlags) {

		// Hide action bar and activate fullscreen flags.
		fullscreenMode = true;
		getActionBar().hide();

		if (setFlags) {
			// Android 4.0 and before have a different mechanism for fullscreen:
			if (Build.VERSION.SDK_INT < 16) {
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
			// Android 4.1 JellyBean and newer:
			else {
				// Set basic fullscreen flags
				int flags = View.SYSTEM_UI_FLAG_FULLSCREEN;  // Hides the status bar

				// Set flags for devices with support for immersive fullscreen.
				if (Build.VERSION.SDK_INT >= 19) {
					// The last two flags make for a smoother transition between states.
					flags = flags
							| View.SYSTEM_UI_FLAG_IMMERSIVE  // Ensures that no touch events won't cancel fullscreen mode.
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  // Hides the system navigation
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  // Ensures more seamless transition.
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;  // Ensures more seamless transition.
				}

				decorView.setSystemUiVisibility(flags);
			}
		}

		// Signal to the Governor that a runtime change has occurred.
		governor.onRuntimeChange();
	}

	/**
	 * Deactivates full-screen mode - shows status bar, system navigation and the Action Bar.
	 */
	public void deactivateFullscreen() {
		fullscreenMode = false;
		getActionBar().show();

		// Android 4.0 and before have a different mechanism for fullscreen:
		if (Build.VERSION.SDK_INT < 16) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		// Android 4.1 JellyBean and newer:
		else {
			decorView.setSystemUiVisibility(0); // Setting no flags returns all to normal.
		}

		// Signal to the Governor that a runtime change has occurred.
		governor.onRuntimeChange();
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
			// The system bars are visible again.

			if (fullscreenMode) {
				// If we have not deactivated the fullscreen manually through deactivateFullscreen(),
				// but the fullscreen was exited by a swipe from the top or the bottom of the screen in -
				// only the fullscreen flag was cancelled, so cancel the rest as well,
				// so that the navigation drawer renders properly.
				deactivateFullscreen();
			}

			Log.d(LOG, LOG + ".onSystemUiVisibilityChange - fullscreen cancelled");
		}
		else {
			// The system bars are no longer visible.

			if (!fullscreenMode) {
				// Do everything but set the fullscreen flags, so we don't end up in a crazy loop
				// of switching fullscreen on and off.
				activateFullscreen(false);
			}

			Log.d(LOG, LOG + ".onSystemUiVisibilityChange - fullscreen entered");
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
				activateFullscreen(true);
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
			if (!governor.getPanelHolder(0).displayToC()) {
				errorMessage(getString(R.string.error_tocNotFound));
			}
			break;

		// Open book 1
		case R.id.drawer_open_book_1_button:
			startOpenFileActivity(0);
			break;

		// Book title 2 - open Table of Contents
		case R.id.drawer_book_title_2_button:
			// If two books are open or if we're reading a bilingual book
			if (!governor.exactlyOneBookOpen() || governor.isReadingBilingualEbook()) {
				// Open ToC of the second book
				if (!governor.getPanelHolder(1).displayToC()) {
					errorMessage(getString(R.string.error_tocNotFound));
				}
			} else {
				// If only one non-bilingual book is opened, act like open_book_2_button.
				startOpenFileActivity(1);
			}
			break;

		// Open book 2
		case R.id.drawer_open_book_2_button:
			startOpenFileActivity(1);
			break;

		// Close panel
		case R.id.drawer_close_panel_button:
			new CloseOrHidePanelDialog(true).show(getFragmentManager(), "close_or_hide_panel_dialog");
			break;

		// Hide panel / Reappear panel
		case R.id.drawer_hide_panel_button:
			if (governor.isAnyPanelHidden()) {
				// Reappear (un-hide) the hidden panel.
				governor.reappearPanel();

			} else if (governor.isOnlyOnePanelOpen()) {
				// Can't hide the only open panel.
				Toast.makeText(this, R.string.Cannot_hide_the_only_open_panel, Toast.LENGTH_SHORT).show();

			} else {
				// Open a HidePanelDialog.
				new CloseOrHidePanelDialog(false).show(getFragmentManager(), "close_or_hide_panel_dialog");
			}
			break;

		// SRS Database
		case R.id.drawer_SRS_database_button:
			startActivityForResult(new Intent(this, SRSDatabaseActivity.class), ACTIVITY_RESULT_SRS_DATABASE);
			break;

		// Dictionary
		case R.id.drawer_dictionary_button:
			openSettings();
			break;

		// Would You Like To Know More? - Info Texts
		case R.id.drawer_infotext_button:
			new InfotextBrowserDialog().show(getFragmentManager(), "InfotextBrowserDialog");
			break;

		// Visual Options
		case R.id.drawer_visual_options_button:
			new VisualOptionsDialog().show(getFragmentManager(), "InfotextBrowserDialog");
			break;

		// Exit
		case R.id.drawer_exit_button:
			finish();
			break;
		}

		navigationDrawerLayout.closeDrawer(Gravity.START);
	}

	/**
	 * Changes the "Hide panel" button in the drawer to "Reappear panel" and back.
	 */
	public void setDrawerHideOrReappearPanelButton(boolean hide) {
		if (hide) {
			drawerHideOrReappearPanelButton.setText(R.string.Hide_panel);
		} else {
			drawerHideOrReappearPanelButton.setText(R.string.Unhide_panel);
		}
	}

	/**
	 * Changes the name of the book displayed in the navigation drawer.
	 * @param panel	Panel of which the book name has changed.
	 * @param name	The new name to display for that panel.
	 */
	public void setBookNameInDrawer(int panel, String name) {
		if (drawerBookButton[panel] != null) {
			if (name != null) {
				drawerBookButton[panel].setText(name);
			} else {
				drawerBookButton[panel].setText(getString(
						panel == 0 ? R.string.Panel_1_empty : R.string.Panel_2_empty));
			}
		}
		drawerBookButtonText[panel] = name;
	}

	/**
	 * When panels get switched, we have to switch displayed book names as well.
	 */
	public void switchBookNamesInDrawer() {
		String temp = drawerBookButtonText[0];
		setBookNameInDrawer(0, drawerBookButtonText[1]);
		setBookNameInDrawer(1, temp);
	}

	/**
	 * Restores the names of the books after a runtime change re-creates the Activity
	 */
	private void restoreBookNamesInDrawer(String[] drawerBookButtonText) {
		for (int i = 0; i < Governor.N_PANELS; i++) {
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
		if (governor.exactlyOneBookOpen()) {

			// Exactly one book open
			menu.findItem(R.id.scroll_sync_menu_item).setVisible(false);
			menu.findItem(R.id.scroll_sync_options_menu_item).setVisible(false);
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
			menu.findItem(R.id.scroll_sync_menu_item).setVisible(true);
			menu.findItem(R.id.scroll_sync_options_menu_item).setVisible(true);
			menu.findItem(R.id.sync_chapters_menu_item).setVisible(true);

			// Submenus
			menu.findItem(R.id.style_1_menu_item).setVisible(true);
			menu.findItem(R.id.style_2_menu_item).setVisible(true);
			menu.findItem(R.id.audio_1_menu_item).setVisible(true);
			menu.findItem(R.id.audio_2_menu_item).setVisible(true);

			if (governor.isReadingBilingualEbook()) {
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

		if (governor.canExtractAudio()) {
			menu.findItem(R.id.audio_menu_item).setVisible(true);
		} else {
			menu.findItem(R.id.audio_menu_item).setVisible(false);
		}

		// If there is only one view, option "changeSizes" is not displayed.
		if (panelCount == 1)
			menu.findItem(R.id.change_size_menu_item).setVisible(false);
		else
			menu.findItem(R.id.change_size_menu_item).setVisible(true);

		// Deactivated items:
		menu.findItem(R.id.style_menu_item).setVisible(false);

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

		// Fullscreen
		case R.id.fullscreen_menu_item:
			// Switch to fullscreen
			switchFullscreen();

			// Build an info dialog.
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.Fullscreen));

			// Add OK and Cancel buttons
			builder.setPositiveButton(getString(R.string.OK), null);
			builder.setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					switchFullscreen();
				}
			});

			// Create the dialog
			builder.setMessage(R.string.fullscreen_dialog_info_notice);
			builder.create().show();
			return true;

		// Toggle Scroll Sync
		case R.id.scroll_sync_menu_item:
			Boolean newDefaultScrollSyncMethodSet = governor.flipScrollSync();

			if (governor.isScrollSync()) {
				// Display infotext if appropriate.
				InfotextDialog.showIfAppropriate(this, DontShowAgain.SCROLL_SYNC);

				if (newDefaultScrollSyncMethodSet != null && newDefaultScrollSyncMethodSet) {
					Toast.makeText(this, getString(R.string.Activated_Default_Scroll_sync_method),
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, getString(R.string.Activated_Scroll_sync), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this, getString(R.string.Deactivated_Scroll_sync), Toast.LENGTH_SHORT).show();
			}
			return true;

		// Scroll Sync Options
		case R.id.scroll_sync_options_menu_item:
			new ScrollSyncDialog().show(getFragmentManager(), "ScrollSyncDialog");
			return true;

		// Sync Chapters
		case R.id.sync_chapters_menu_item:
			if (!governor.flipChapterSync()) {
				errorMessage(getString(R.string.error_onlyOneBookOpen));
			}
			if (governor.isChapterSync()) {
				// Display infotext if appropriate.
				if (!InfotextDialog.showIfAppropriate(this, DontShowAgain.CHAPTER_SYNC)) {
					// If infotext was not shown, show at least a toast.
					Toast.makeText(this, getString(R.string.Activated_Chapter_sync), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this, getString(R.string.Deactivated_Chapter_sync), Toast.LENGTH_SHORT).show();
			}
			return true;

		// Bilingual ebook
		case R.id.bilingual_ebook_menu_item:
			if (governor.exactlyOneBookOpen() || governor.isReadingBilingualEbook()) {
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
			if (!governor.exactlyOneBookOpen()) {
				if (openBilingualBook(1)) {
					invalidateOptionsMenu();
				}
			} else {
				errorMessage(getString(R.string.error_onlyOneBookOpen));
			}
			return true;

		// Display metadata of the book
		case R.id.metadata_menu_item:
			if (governor.exactlyOneBookOpen() == true || governor.isReadingBilingualEbook() == true) {
				governor.getPanelHolder(0).displayMetadata();
			} else {
			}
			return true;

		case R.id.metadata_1_menu_item:
			if (!governor.getPanelHolder(0).displayMetadata())
				errorMessage(getString(R.string.error_metadataNotFound));
			return true;

		case R.id.metadata_2_menu_item:
			if (!governor.getPanelHolder(1).displayMetadata())
				errorMessage(getString(R.string.error_metadataNotFound));
			return true;

		// Table of contents
		case R.id.table_of_contents_menu_item:
			if (governor.exactlyOneBookOpen() == true || governor.isReadingBilingualEbook() == true) {
				governor.getPanelHolder(0).displayToC();
			}
			return true;

		case R.id.table_of_contents_1_menu_item:
			if (!governor.getPanelHolder(0).displayToC())
				errorMessage(getString(R.string.error_tocNotFound));
			return true;

		case R.id.table_of_contents_2_menu_item:
			if (!governor.getPanelHolder(1).displayToC())
				errorMessage(getString(R.string.error_tocNotFound));
			return true;

		// Change style -- deprecated, replaced by Visual Options button in the navigation drawer.
		case R.id.style_menu_item:
			try {
				// Display the style dialog.
				if (governor.exactlyOneBookOpen() == true) {
					DialogFragment newFragment = new VisualOptionsDialog();
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
				DialogFragment newFragment = new VisualOptionsDialog();
				newFragment.show(getFragmentManager(), "");
				bookSelector = 0;
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		case R.id.style_2_menu_item:
			try {
				// Display the style dialog.
				DialogFragment newFragment = new VisualOptionsDialog();
				newFragment.show(getFragmentManager(), "");
				bookSelector = 1;
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		// Audio shenanigans.
		case R.id.audio_menu_item:
			if (governor.exactlyOneBookOpen() == true) {
				if (!governor.getPanelHolder(0).extractAudio()) {
					errorMessage(getString(R.string.no_audio));
				}
			}
			return true;

		case R.id.audio_1_menu_item:
			if (!governor.getPanelHolder(0).extractAudio())
				errorMessage(getString(R.string.no_audio));
			return true;

		case R.id.audio_2_menu_item:
			if (!governor.getPanelHolder(1).extractAudio())
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
		Log.d(LOG, "ReaderActivity.addPanel");

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.add(R.id.PanelsLayout, p, p.getTag());
		fragmentTransaction.commit();

		panelCount++;

		// Rethinks what menu items to display
		invalidateOptionsMenu();
	}

	/**
	 * Remove panel, but don't close the application if no panel is left.
	 */
	public void removePanel(SplitPanel p) {
		Log.d(LOG, "ReaderActivity.removePanelWithoutClosing");

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.remove(p);
		fragmentTransaction.commit();

		panelCount--;

		// Rethinks what menu items to display
		invalidateOptionsMenu();
	}

	/**
	 * Remove panel and if there are no more, close the application.
	 */
	public void removePanelWithClosing(SplitPanel p) {
		Log.d(LOG, "ReaderActivity.removePanel");

		removePanel(p);

		// Close the app if there are no panels left
		if (panelCount <= 0) {
			finish();
		}
	}

	/**
	 * Detach panel from view.
	 */
	public void detachPanel(SplitPanel p) {
		Log.d(LOG, "ReaderActivity.detachPanel");

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.detach(p);
		fragmentTransaction.commit();

		panelCount--;
	}

	/**
	 * Re-attach a panel that has been detached previously.
	 */
	public void attachPanel(SplitPanel p) {
		Log.d(LOG, "ReaderActivity.attachPanel");

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.attach(p);
		fragmentTransaction.commit();

		panelCount++;
	}

	/**
	 * Hide panel from view.
	 */
	public void hidePanel(SplitPanel p) {
		Log.d(LOG, "ReaderActivity.hidePanel");

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        fragmentTransaction.hide(p);
		fragmentTransaction.commit();
	}

	/**
	 * Reappear panel after it has been hidden.
	 */
	public void showPanel(SplitPanel p) {
		Log.d(LOG, "ReaderActivity.showPanel");

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        fragmentTransaction.show(p);
		fragmentTransaction.commit();
	}



	// ============================================================================================
	//		Misc
	// ============================================================================================

	/**
	 * Opens another activity to select which EPUB file to open.
	 * @param panel  Panel into which to load the newly opened EPUB.
	 */
	private void startOpenFileActivity(int panel) {
		bookSelector = panel;
		Intent goToChooser = new Intent(this, RecentlyOpenedFilesActivity.class);
		startActivityForResult(goToChooser, ACTIVITY_RESULT_FILE_CHOOSER);
	}

	/**
	 * Opens the Settings dialog.
	 */
	public void openSettings() {
		new DictionaryDialog().show(getFragmentManager(), "settings_dialog");
	}

	/**
	 * Finds out if any of the open books contains multiple languages or not.
	 * @return		Truth
	 */
	public boolean anyBilingualEbook() {
		String[] languages;
		for (int i = 0; i < Governor.N_PANELS; i++) {
			languages = governor.getPanelHolder(i).getLanguagesInABook();

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
		languages = governor.getPanelHolder(book).getLanguagesInABook();

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
		governor.activateBilingualEbook(book, first, second);
	}

	/**
	 * Change the relative weight of the two panels, this change their relative size.
	 * @param weight	weight of the first panel (0 to 1)
	 */
	public void changePanelsWeight(float weight) {
		governor.changePanelsWeight(weight);
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
	 * Display an error message as an Android toast.
	 */
	public void errorMessage(String message) {
		Context context = getApplicationContext();
		Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		toast.show();
	}
}
