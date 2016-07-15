/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 27/06/2016.
 */

package cm.aptoide.pt.v8engine.fragment.implementations;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.trello.rxlifecycle.FragmentEvent;

import java.util.ArrayList;
import java.util.List;

import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.ExcludedUpdate;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.utils.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.fragment.GridRecyclerSwipeFragment;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.ExcludedUpdateDisplayable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by sithengineer on 21/06/16.
 */
public class ExcludedUpdatesFragment extends GridRecyclerSwipeFragment {

	private static final String TAG = ExcludedUpdatesFragment.class.getSimpleName();
	private TextView emptyData;
	private Subscription subscription;

	public ExcludedUpdatesFragment() {
	}

	public static ExcludedUpdatesFragment newInstance() {
		return new ExcludedUpdatesFragment();
	}

	@Override
	public void load(boolean refresh, Bundle savedInstanceState) {
		Logger.d(TAG, String.format("refresh excluded updates? %s", refresh ? "yes" : "no"));
		fetchExcludedUpdates();
	}

	@Override
	public void bindViews(View view) {
		super.bindViews(view);
		emptyData = (TextView) view.findViewById(R.id.empty_data);
		setHasOptionsMenu(true);
	}

	@Override
	public int getContentViewId() {
		return R.layout.fragment_with_toolbar;
	}

	@Override
	public void setupToolbar() {
		super.setupToolbar();
		if (toolbar != null) {
			ActionBar bar = ((AppCompatActivity) getActivity()).getSupportActionBar();
			bar.setDisplayHomeAsUpEnabled(true);
			bar.setTitle(R.string.excluded_updates);
		}
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_excluded_updates_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			getActivity().onBackPressed();
			return true;
		} else if (itemId == R.id.menu_restore_updates) {
			ShowMessage.asSnack(this, "TO DO: restore updates");
			return true;
		} else if (itemId == R.id.menu_select_all) {
			ShowMessage.asSnack(this, "TO DO: select all");
			return true;
		} else if (itemId == R.id.menu_select_none) {
			ShowMessage.asSnack(this, "TO DO: select none");
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void fetchExcludedUpdates() {
		subscription = Database.ExcludedUpdatesQ.getAll(realm)
				.asObservable()
				.compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(excludedUpdates -> {

					finishLoading();

					if (excludedUpdates == null || excludedUpdates.isEmpty()) {
						emptyData.setText(R.string.no_excluded_updates_msg);
						emptyData.setVisibility(View.VISIBLE);
					} else {

						emptyData.setVisibility(View.GONE);

						List<ExcludedUpdateDisplayable> displayables = new ArrayList<>();
						for (final ExcludedUpdate excludedUpdate : excludedUpdates) {
							displayables.add(new ExcludedUpdateDisplayable(excludedUpdate));
						}
						setDisplayables(displayables);
					}
				});
	}
}
