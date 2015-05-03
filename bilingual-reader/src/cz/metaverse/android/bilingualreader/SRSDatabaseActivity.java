package cz.metaverse.android.bilingualreader;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import cz.metaverse.android.bilingualreader.db.SRSDatabaseTable;
import cz.metaverse.android.bilingualreader.dialog.AddToSRSDialog;
import cz.metaverse.android.bilingualreader.dialog.ExportSRSDialog;


/**
 *
 * Activity that displays and handles the SRS cards added to our Spaced Repetition Software (SRS) Database.
 *
 */
public class SRSDatabaseActivity extends ListActivity
		implements OnQueryTextListener, OnCloseListener, LoaderManager.LoaderCallbacks<Cursor>,
		MultiChoiceModeListener {

	// The TextView that's displayed with some message when the ListView is empty.
	TextView emptyTextView;

	// This is the Adapter being used to display the list's data.
	SimpleCursorAdapter cursorAdapter;

	// The SearchView for doing filtering.
	SearchView searchView;

	// If non-null, this is the current filter the user has provided in the SearchView.
	String currentFilter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_srs_database);

		emptyTextView = (TextView) findViewById(android.R.id.empty);

		// Sets the app icon as clickable.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Initialize the searching mechanism
		searchView = new MySearchView(this);
		searchView.setOnQueryTextListener(this);
		searchView.setOnCloseListener(this);
		searchView.setIconifiedByDefault(true);

		// Initialize long-click selecting on the ListView for deleting cards.
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(this);
		getListView().setLongClickable(true);

		// Create an empty adapter we will use to display the loaded data after they arrive.
		cursorAdapter = new SimpleCursorAdapter(this, R.layout.listview_row_srs, null,
				new String[] {SRSDatabaseTable.COL_WORD, SRSDatabaseTable.COL_DEFINITION},
				new int[] {android.R.id.text1, android.R.id.text2 }, 0);
		setListAdapter(cursorAdapter);

		// Start re/loading data in a background thread.
		reloadData();
	}

	/**
	 * Tells a LoaderManager to reload the ListView data on a background thread.
	 */
	public void reloadData() {
		// Restart the Loader - because we're not implementing database calls through a ContentProvider,
		//  the Loader will not be notified in case any changes are made to the content (database).
		//  Therefore we have to reload the data when we open this Activity, and after any change, manually.
		//  Thus we use restartLoader instead of initLoader.
		getLoaderManager().restartLoader(0, null, this);
	}

	/**
	 * Sets the text to be displayed when ListView is empty.
	 */
	private void setEmptyText(int textResource) {
		emptyTextView.setText(textResource);
	}

	/**
	 * Called when user (short) clicks on one of the ListView items.
	 * We start edit dialog for the clicked SRS card.
	 */
	@Override public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = cursorAdapter.getCursor();
		if (cursor != null) {
			cursor.moveToPosition(position);
			new AddToSRSDialog(this, id, cursor.getString(1), cursor.getString(2))
					.show(getFragmentManager(), "add_to_srs_dialog");
		}
	}


	// ============================================================================================
	//		Options menu
	// ============================================================================================

	/**
	 * Populates the main menu when called.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.srs_database_menu, menu);

		// Connect the SearchView class with the Search menu item..
		MenuItem item = menu.findItem(R.id.search_menu_item);
		item.setActionView(searchView);

		return true;
	}

	/**
	 * Called when a Options Menu item was selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		// The App icon in upper left corner
		case android.R.id.home:
			finish();
			return true;

		// Refresh
		case R.id.refresh_menu_item:
			reloadData();
			return true;

		// Add SRS card
		case R.id.add_SRS_card_menu_item:
			new AddToSRSDialog(this).show(getFragmentManager(), "add_to_srs_dialog");
			return true;

		// Sort alphabetically
		case R.id.sort_alphabetically_menu_item:
			SRSDatabaseTable.getInstance(this).sortAlphabetically(true);
			reloadData();
			return true;

		// Sort by last added
		case R.id.sort_by_last_added_menu_item:
			SRSDatabaseTable.getInstance(this).sortAlphabetically(false);
			reloadData();
			return true;

		// Export
		case R.id.export_menu_item:
			new ExportSRSDialog(this).show(getFragmentManager(), "export_srs_dialog");
			return true;

		default:
			return super.onOptionsItemSelected(item);

		}
	}

	/**
	 * Launch a prepared Intent.
	 * Here, because if we launch an Intent from a Dialog that was launched by another Dialog,
	 *  Android craps its pants.
	 */
	public void openShareIntent(Intent shareIntent) {
		startActivity(Intent.createChooser(shareIntent, getString(R.string.Share)));
	}


	// ============================================================================================
	//		Loading and receiving data - implementing LoaderManager.LoaderCallbacks<Cursor>
	// ============================================================================================

	/**
	 * This is called when a new Loader needs to be created. Loader is a class that handles
	 * the loading of data in a background thread for you.
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// Since we're implementing our own loadInBackground() method,
		// all but one of the parameters are irrelevant - null them!
		CursorLoader cursorLoader = new CursorLoader(this, null, null, null, null, null) {
				/**
				 * The main method that does the work in the background for us.
				 */
				@Override
				public Cursor loadInBackground() {
					// Search the database for all or filtered results, depending on what we want.
					SRSDatabaseTable dbt = SRSDatabaseTable.getInstance(SRSDatabaseActivity.this);

					if (currentFilter != null) {
						return dbt.getMatches(currentFilter);
					} else {
						return dbt.getAll();
					}
				}
			};
		return cursorLoader;
	}

	/**
	 * Called when the results that have been loading in the background get in.
	 */
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Put the new data into the adapter!
		// (The framework will take care of closing the old cursor once we return.)
		cursorAdapter.swapCursor(data);

		// If there are no results, update the TextView to display the relevant message.
		if (data == null || data.getCount() == 0) {
			if (currentFilter != null) {
				setEmptyText(R.string.No_SRS_cards_match_search);
			} else {
				setEmptyText(R.string.There_are_no_SRS_cards_yet);
			}
		}
	}

	/**
	 * This is called when the last Cursor provided to onLoadFinished()
	 * above is about to be closed.  We need to make sure we are no
	 * longer using it.
	 */
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		cursorAdapter.swapCursor(null);
	}


	// ============================================================================================
	//		Action Mode - when you long-click on one or more items, this menu pops up.
	// ============================================================================================

	@Override
	public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
		MenuInflater inflater = actionMode.getMenuInflater();
		inflater.inflate(R.menu.srs_item_selected_menu, menu);
		return true;
	}

	/**
	 * Called when menu gets invalidated so we can update it.
	 * We do nothing, menu stays the same.
	 */
	@Override
	public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
		return false;
	}

	/**
	 * Called when an ActionMode button was clicked.
	 */
	@Override
	public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
		switch (menuItem.getItemId()) {

		// Delete
		case R.id.delete_srs_card_menu_item:
			long[] ids = getListView().getCheckedItemIds();
			SRSDatabaseTable.getInstance(this).deleteCards(ids);
			reloadData();

			actionMode.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {}

	@Override
	public void onDestroyActionMode(ActionMode actionMode) {}


	// ============================================================================================
	//		Search
	// ============================================================================================

	/**
	 * Search: Called when the user changes the text in the search bar.
	 */
	@Override
	public boolean onQueryTextChange(String newText) {
		// "Compute" the new filter.
		String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

		// If the filter hasn't actually changed, do nothing.
		if (currentFilter == null && newFilter == null
				|| currentFilter != null && currentFilter.equals(newFilter)) {
			return true;
		}

		// Update the filter and reload data.
		currentFilter = newFilter;
		reloadData();
		return true;
	}

	/**
	 * Search: Called when the user presses submit. We update on the fly, therefore, we do nothing.
	 */
	@Override
	public boolean onQueryTextSubmit(String query) {
		return true;
	}

	/**
	 * Search: Called when the search bar gets closed.
	 */
	@Override
	public boolean onClose() {
		// If there was some text in the search bar delete it so it doesn't reappear next time.
		if (!TextUtils.isEmpty(searchView.getQuery())) {
			searchView.setQuery(null, true);
		}
		return true;
	}

	/**
	 *
	 * Only a light customization of the SearchView class.
	 *
	 */
	private static class MySearchView extends SearchView {
		public MySearchView(Context context) {
			super(context);
		}

		/**
		 * The normal SearchView doesn't clear its search text when
		 * collapsed, so we will do this for it.
		 */
		@Override
		public void onActionViewCollapsed() {
			setQuery("", false);
			super.onActionViewCollapsed();
		}
	}
}
