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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import cz.metaverse.android.bilingualreader.dialog.ChangeCSSDialog;
import cz.metaverse.android.bilingualreader.dialog.LanguageChooserDialog;
import cz.metaverse.android.bilingualreader.dialog.PanelSizeDialog;
import cz.metaverse.android.bilingualreader.dialog.SettingsDialog;
import cz.metaverse.android.bilingualreader.manager.EpubsNavigator;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;
import cz.metaverse.android.bilingualreader.sync.ParagraphPositions;

public class ReaderActivity extends Activity {

	public EpubsNavigator navigator;
	protected int bookSelector;
	protected int panelCount;
	protected String[] cssSettings;

	// Navigation Drawer
	private String[] navigationDrawerItemNames;
	private DrawerLayout navigationDrawerLayout;
	private ListView navigationDrawerListView;
	private ActionBarDrawerToggle actionBarDrawerToggle;
	private CharSequence actionBarTitle;

	// Used exclusively for debugging purposes (e.g. Displaying toasts without context)
	public static Context debugContext;	// TODO remove when no longer needed


	/**
	 * Called when the application gets started.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		 * Navigation Drawer setup
		 */
		navigationDrawerItemNames = getResources().getStringArray(R.array.Navigation_Drawer_Items);
		navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationDrawerListView = (ListView) findViewById(R.id.left_drawer);

		// Set the adapter for the navigation drawer's list view
		navigationDrawerListView.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, navigationDrawerItemNames));

		// Set the navigation drawer's list view's click listener
		navigationDrawerListView.setOnItemClickListener(new DrawerItemClickListener());

		actionBarTitle = getTitle();

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

		// Set the drawer toggle as the DrawerListener
		navigationDrawerLayout.setDrawerListener(actionBarDrawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		/* end of Navigation Drawer setup */


		// Setup logic variables
		navigator = EpubsNavigator.getSingleton(this);

		panelCount = 0;
		if (savedInstanceState != null) {
			// When trying to use "getString(R.string.nonPersistentState_panelCount)" as key, the value is
			//  just NOT retrieved. The same if the key is too long, e.g. "nonPersistentState_panelCount".
			cssSettings = savedInstanceState.getStringArray("nps_cssSettings");
		}
		if (cssSettings == null) {
			cssSettings = new String[8];
		}

		debugContext = getBaseContext();


		if (savedInstanceState != null) {
			// Activity is just being recreated because of runtime configuration change.
			// Readd panels to view.
			navigator.reAddPanels();
		} else {
			// Load the persistent state from previous runs of the application.
			// if this is the first time the application is starting.
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			loadStateOfNavigator(preferences);
			// Load panels.
			navigator.loadViews(preferences);
		}

		// If there are no panels, start FileChooser.
		if (panelCount == 0) {
			bookSelector = 0;
			Intent goToChooser = new Intent(this, FileChooserActivity.class);
			startActivityForResult(goToChooser, 0);
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
		outState.putStringArray("nps_cssSettings", cssSettings);

		// Remove current panels from view (they still exist, they just won't be attached
		//  to the FragmentManager and therefore won't be displayed.
		// They will be readded in in onResume() or they will be recreated and readded
		//  in onCreate() after a runtime configuration change.
		// This is *not* possible to move to onDestroy, because FragmentManager refuses to do
		//  anything after onSaveInstanceState, because it might cause potential problems when recreating
		//  the Activity from the saved state.
		navigator.removePanels();

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

		// Open the selected book in a given panel if all went well.
		if (resultCode == Activity.RESULT_OK) {
			String path = data.getStringExtra(getString(R.string.bpath));
			navigator.openBook(path, bookSelector);
		}
	}



	// ============================================================================================
	//		Navigation Drawer
	// ============================================================================================

	/**
	 * OnClickListener for the ListView of our navigation drawer
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Handle the pressed menu item.
			switch(position) {

			// Open book 1
			case 0:
				bookSelector = 0;
				Intent goToChooser1 = new Intent(ReaderActivity.this, FileChooserActivity.class);
				goToChooser1.putExtra(getString(R.string.second), getString(R.string.time));
				startActivityForResult(goToChooser1, 0);
				break;

			// Open book 2
			case 1:
				bookSelector = 1;
				Intent goToChooser2 = new Intent(ReaderActivity.this, FileChooserActivity.class);
				goToChooser2.putExtra(getString(R.string.second), getString(R.string.time));
				startActivityForResult(goToChooser2, 0);
				break;

			// SRS Database
			case 2:
				// TODO Open SRS database activity
				break;

			// Settings
			case 3:
				new SettingsDialog().show(getFragmentManager(), "settings_dialog");
				break;
			}

			// Highlight the selected item and close the drawer
			navigationDrawerListView.setItemChecked(position, true);
			navigationDrawerLayout.closeDrawer(navigationDrawerListView);
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
	 * Sets the text displayed in the Action bar on top of the activity.
	 */
	@Override
	public void setTitle(CharSequence title) {
		actionBarTitle = title;
		getActionBar().setTitle(actionBarTitle);

	}

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
			if (navigator.displayTOC(1))
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
		fragmentTransaction.add(R.id.MainLayout, p, p.getTag());
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
	 * Change the relative weight of the two panels.
	 * @param weight	weight of the first panel (0 to 1)
	 */
	public void changeViewsSize(float weight) {
		navigator.changeViewsSize(weight);
	}

	/**
	 * @return height of the display area
	 */
	public int getHeight() {
		LinearLayout main = (LinearLayout) findViewById(R.id.MainLayout);
		return main.getMeasuredHeight();
	}

	/**
	 * @return width of the display area
	 */
	public int getWidth() {
		LinearLayout main = (LinearLayout) findViewById(R.id.MainLayout);
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
