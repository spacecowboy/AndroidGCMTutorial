package com.nononsenseapps.linksgcm;

import java.util.Collection;
import java.util.HashMap;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import com.nononsenseapps.linksgcm.database.DatabaseHandler;
import com.nononsenseapps.linksgcm.database.LinkItem;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the
 * ListView with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class LinkFragment extends Fragment {

	/**
	 * The fragment's ListView/GridView.
	 */
	private AbsListView mListView;

	/**
	 * The Adapter which will be used to populate the ListView/GridView with
	 * Views.
	 */
	private CursorAdapter mAdapter;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public LinkFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_activated_2, null, new String[] {
						LinkItem.COL_URL, LinkItem.COL_TIMESTAMP }, new int[] {
						android.R.id.text1, android.R.id.text2 }, 0);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.link_fragment, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_link, container, false);

		// Set the adapter
		mListView = (AbsListView) view.findViewById(android.R.id.list);
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

		mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		// Set OnItemClickListener so we can be notified on item clicks
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) {
				final LinkItem linkItem = new LinkItem((Cursor) mAdapter.getItem(position));
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(linkItem.url));
				startActivity(i);
			}});
		
		mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			HashMap<Long, LinkItem> links = new HashMap<Long, LinkItem>();

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				// Here you can do something when items are
				// selected/de-selected,
				// such as update the title in the CAB
				if (checked) {
					links.put(id,
							new LinkItem((Cursor) mAdapter.getItem(position)));
				}
				else {
					links.remove(id);
				}
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				// Respond to clicks on the actions in the CAB
				switch (item.getItemId()) {
				case R.id.action_delete:
					deleteItems(links.values());
					mode.finish(); // Action picked, so close the CAB
					return true;
				default:
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// Inflate the menu for the CAB
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.link_fragment_context, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// Here you can make any necessary updates to the activity when
				// the CAB is removed. By default, selected items are
				// deselected/unchecked.
				links.clear();
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// Here you can perform updates to the CAB due to
				// an invalidate() request
				return false;
			}
		});

		// Load content
		getLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				return new CursorLoader(getActivity(), LinkItem.URI(),
						LinkItem.FIELDS, null, null, LinkItem.COL_TIMESTAMP
								+ " DESC");
			}

			@Override
			public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
				mAdapter.swapCursor(c);
			}

			@Override
			public void onLoaderReset(Loader<Cursor> arg0) {
				mAdapter.swapCursor(null);
			}
		});

		return view;
	}
	
	void deleteItems(Collection<LinkItem> items) {
		final DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
		for (LinkItem item: items) {
			db.deleteItem(item);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = false;
		switch (item.getItemId()) {
		case R.id.action_add:
			showAddDialog();
			break;
		}
		return result;
	}

	void showAddDialog() {
		DialogFragment dialog = new DialogAddLink();
		dialog.show(getFragmentManager(), "add_link");
	}
}
