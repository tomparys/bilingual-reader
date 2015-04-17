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

import cz.metaverse.android.bilingualreader.dialog.ChangeCSSMenu;
import cz.metaverse.android.bilingualreader.dialog.LanguageChooser;
import cz.metaverse.android.bilingualreader.dialog.SetPanelSize;
import cz.metaverse.android.bilingualreader.manager.EpubNavigator;
import cz.metaverse.android.bilingualreader.panel.SplitPanel;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

	public EpubNavigator navigator;
	protected int bookSelector;
	protected int panelCount;
	protected String[] cssSettings;

	/**
	 * Called when the application gets started.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		navigator = new EpubNavigator(2, this);

		panelCount = 0;
		cssSettings = new String[8];

		// Load state from previous runs of the application. 
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		loadState(preferences);
		navigator.loadViews(preferences);

		// If there are no panels, start FileChooser.
		if (panelCount == 0) {
			bookSelector = 0;
			Intent goToChooser = new Intent(this, FileChooser.class);
			startActivityForResult(goToChooser, 0);
		}
	}

	/**
	 * Called when the app gets focus.
	 *  Either for the first time (after onCreate), or after losing it first (after onPause).
	 */
	protected void onResume() {
		super.onResume();

		// If panelCount is zero, we can be sure we're getting focus back,
		// 	because otherwise FileChooser intent would have been launched in onCreate.
		if (panelCount == 0) {
			// Load panels and books into them from before.
			SharedPreferences preferences = getPreferences(MODE_PRIVATE);
			navigator.loadViews(preferences);
		}
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
		saveState(editor);
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

	/**
	 * Called when menu is opened.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Called right before menu is displayed.
	 * 
	 * Makes visible only the relevant menu options.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If there are two books opened and parallel text isn't active.
		if (navigator.exactlyOneBookOpen() == false && navigator.isParallelTextOn() == false) {			
			menu.findItem(R.id.meta1).setVisible(true);
			menu.findItem(R.id.meta2).setVisible(true);
			menu.findItem(R.id.toc1).setVisible(true);
			menu.findItem(R.id.toc2).setVisible(true);
			menu.findItem(R.id.FirstParallel).setVisible(true);
			menu.findItem(R.id.SecondParallel).setVisible(true);
		}

		// If there are two books opened.
		if (navigator.exactlyOneBookOpen() == false) {
			menu.findItem(R.id.Synchronize).setVisible(true);
			menu.findItem(R.id.Align).setVisible(true);
			// menu.findItem(R.id.SyncScroll).setVisible(true);
			menu.findItem(R.id.StyleBook1).setVisible(true);
			menu.findItem(R.id.StyleBook2).setVisible(true);
			menu.findItem(R.id.firstAudio).setVisible(true);
			menu.findItem(R.id.secondAudio).setVisible(true);
		}

		// If only one book is opened but it's in parallel text mode.
		if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true) {
			menu.findItem(R.id.meta1).setVisible(false);
			menu.findItem(R.id.meta2).setVisible(false);
			menu.findItem(R.id.toc1).setVisible(false);
			menu.findItem(R.id.toc2).setVisible(false);
			menu.findItem(R.id.FirstParallel).setVisible(false);
			menu.findItem(R.id.SecondParallel).setVisible(false);
		}

		// If only one book is opened.
		if (navigator.exactlyOneBookOpen() == true) {
			menu.findItem(R.id.Synchronize).setVisible(false);
			menu.findItem(R.id.Align).setVisible(false);
			menu.findItem(R.id.SyncScroll).setVisible(false);
			menu.findItem(R.id.StyleBook1).setVisible(false);
			menu.findItem(R.id.StyleBook2).setVisible(false);
			menu.findItem(R.id.firstAudio).setVisible(false);
			menu.findItem(R.id.secondAudio).setVisible(false);
		}

		// If there is only one view, option "changeSizes" is not displayed.
		if (panelCount == 1)
			menu.findItem(R.id.changeSize).setVisible(false);
		else
			menu.findItem(R.id.changeSize).setVisible(true);

		return true;
	}

	/**
	 * Called when Menu option is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		// User wants to open a new book (possibly even a panel)
		case R.id.FirstEPUB:
			bookSelector = 0;
			Intent goToChooser1 = new Intent(this, FileChooser.class);
			goToChooser1.putExtra(getString(R.string.second), getString(R.string.time));
			startActivityForResult(goToChooser1, 0);
			return true;
			
		case R.id.SecondEPUB:
			bookSelector = 1;
			Intent goToChooser2 = new Intent(this, FileChooser.class);
			goToChooser2.putExtra(getString(R.string.second), getString(R.string.time));
			startActivityForResult(goToChooser2, 0);
			return true;

		// Parallel text
		case R.id.Parallel:
			if (navigator.exactlyOneBookOpen() == true
					|| navigator.isParallelTextOn() == true)
				chooseLanguage(0);
			return true;

		case R.id.FirstParallel:
			chooseLanguage(0);
			return true;
			
		case R.id.SecondParallel:
			if (navigator.exactlyOneBookOpen() == false)
				chooseLanguage(1);
			else
				errorMessage(getString(R.string.error_onlyOneBookOpen));
			return true;

		// Align/sync book 1 with book 2
		case R.id.Align1with2:
			try {
				boolean yes = navigator.synchronizeView(1, 0);
				if (!yes) {
					errorMessage(getString(R.string.error_onlyOneBookOpen));
				}
			} catch (Exception e) {
				errorMessage(getString(R.string.error_cannotSynchronize));
			}
			return true;

		// Align/sync book 2 with book 1
		case R.id.Align2with1:
			try {
				boolean ok = navigator.synchronizeView(0, 1);
				if (!ok) {
					errorMessage(getString(R.string.error_onlyOneBookOpen));
				}
			} catch (Exception e) {
				errorMessage(getString(R.string.error_cannotSynchronize));
			}
			return true;

		// Toggle sync views
		case R.id.Synchronize:
			boolean sync = navigator.flipSynchronizedReadingActive();
			if (!sync) {
				errorMessage(getString(R.string.error_onlyOneBookOpen));
			}
			return true;

		// Display metadata of the book
		case R.id.Metadata:
			if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true) {
				navigator.displayMetadata(0);
			} else {
			}
			return true;

		case R.id.meta1:
			if (!navigator.displayMetadata(0))
				errorMessage(getString(R.string.error_metadataNotFound));
			return true;

		case R.id.meta2:
			if (!navigator.displayMetadata(1))
				errorMessage(getString(R.string.error_metadataNotFound));
			return true;

		// Table of contents
		case R.id.tableOfContents:
			if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true) {
				navigator.displayTOC(0);
			}
			return true;

		case R.id.toc1:
			if (!navigator.displayTOC(0))
				errorMessage(getString(R.string.error_tocNotFound));
			return true;
			
		case R.id.toc2:
			if (navigator.displayTOC(1))
				errorMessage(getString(R.string.error_tocNotFound));
			return true;
			
		// Change relative size of panels
		case R.id.changeSize:
			try {
				// Display dialog to pick new relative size of the panels.
				DialogFragment newFragment = new SetPanelSize();
				newFragment.show(getFragmentManager(), "");
			} catch (Exception e) {
				errorMessage(getString(R.string.error_cannotChangeSizes));
			}
			return true;
		
		// Change style
		case R.id.Style:
			try {
				// Display the style dialog.
				if (navigator.exactlyOneBookOpen() == true) {
					DialogFragment newFragment = new ChangeCSSMenu();
					newFragment.show(getFragmentManager(), "");
					bookSelector = 0;
				}
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		case R.id.StyleBook1:
			try {
				// Display the style dialog.
				DialogFragment newFragment = new ChangeCSSMenu();
				newFragment.show(getFragmentManager(), "");
				bookSelector = 0;
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		case R.id.StyleBook2:
			try {
				// Display the style dialog.
				DialogFragment newFragment = new ChangeCSSMenu();
				newFragment.show(getFragmentManager(), "");
				bookSelector = 1;
			} catch (Exception e) {
				errorMessage(getString(R.string.error_CannotChangeStyle));
			}
			return true;

		/*
		 * TODO:
		 * case R.id.SyncScroll: syncScrollActivated = !syncScrollActivated;
		 * return true;
		 */

		// Audio shenanigans.
		case R.id.audio:
			if (navigator.exactlyOneBookOpen() == true) {
				if (!navigator.extractAudio(0)) {
					errorMessage(getString(R.string.no_audio));
				}
			}
			return true;
			
		case R.id.firstAudio:
			if (!navigator.extractAudio(0))
				errorMessage(getString(R.string.no_audio));
			return true;
			
		case R.id.secondAudio:
			if (!navigator.extractAudio(1))
				errorMessage(getString(R.string.no_audio));
			return true;
		
		// And finally, the default option.
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	
	// ---- Panels Manager
	
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
	}

	/**
	 * Choose language.
	 * @param book
	 */
	public void chooseLanguage(int book) {

		String[] languages;
		languages = navigator.getLanguagesInABook(book);

		// If there are just two languages, start parallel text mode with them.
		if (languages.length == 2) {
			startParallelText(book, 0, 1);
		
		// If there are more than 2 languages, show a dialog to pick the two languages
		// 	with which to start parallel text mode.
		} else if (languages.length > 0) {
			Bundle bundle = new Bundle();
			bundle.putInt(getString(R.string.tome), book);
			bundle.putStringArray(getString(R.string.lang), languages);

			LanguageChooser langChooser = new LanguageChooser();
			langChooser.setArguments(bundle);
			langChooser.show(getFragmentManager(), "");
		} else {
			errorMessage(getString(R.string.error_noOtherLanguages));
		}
	}

	/**
	 * Start parallel text mode
	 * @param book		book containing the parallel text
	 * @param first		first language to show
	 * @param second	second language to show
	 */
	public void startParallelText(int book, int first, int second) {
		navigator.parallelText(book, first, second);
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
	protected void saveState(Editor editor) {
		navigator.saveState(editor);
	}

	/**
	 * Load state of the application from before.
	 */
	protected void loadState(SharedPreferences preferences) {
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
