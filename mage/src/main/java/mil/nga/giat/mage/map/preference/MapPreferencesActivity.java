package mil.nga.giat.mage.map.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.common.collect.Collections2;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerAppCompatActivity;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.data.feed.Feed;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

/**
 * Provides map configuration driven settings that are available to the user.
 * Check mappreferences.xml for the configuration.
 *
 * @author newmanw
 */
public class MapPreferencesActivity extends DaggerAppCompatActivity {

	public static String LOG_NAME = MapPreferencesActivity.class.getName();

	public static final int TILE_OVERLAY_ACTIVITY = 100;
	public static final int ONLINE_LAYERS_OVERLAY_ACTIVITY = 200;

	private MapPreferenceFragment preference = new MapPreferenceFragment();

	public static class MapPreferenceFragment extends PreferenceFragmentCompat implements HasAndroidInjector {

		@Inject
		DispatchingAndroidInjector<Object> androidInjector;

		@Inject
		protected ViewModelProvider.Factory viewModelFactory;
		private MapPreferencesViewModel viewModel;

		@Inject
		protected SharedPreferences preferences;

		@Inject
		protected MapLayerPreferences mapLayerPreferences;

		private Event event;

		@Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapPreferencesViewModel.class);
			viewModel.getFeeds().observe(this, this::onFeeds);
		}

		@Override
		public void onAttach(Context context) {
			AndroidSupportInjection.inject(this);
			super.onAttach(context);
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.mappreferences);
		}

		@Override
		public void onResume() {
			super.onResume();

			event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();
			viewModel.setEvent(event.getRemoteId());

			findPreference(getString(R.string.tileOverlaysKey)).setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(getActivity(), TileOverlayPreferenceActivity.class);
				getActivity().startActivityForResult(intent, TILE_OVERLAY_ACTIVITY);
				return true;
			});

			findPreference(getString(R.string.onlineLayersKey)).setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(getActivity(), OnlineLayersPreferenceActivity.class);
				getActivity().startActivityForResult(intent, ONLINE_LAYERS_OVERLAY_ACTIVITY);
				return true;
			});

			// TODO : Remove the below and rework OverlayPreference to have a 'entities' similar to a list preference, these would be the 'display values'
			try {
				List<Layer> layers = LayerHelper.getInstance(getContext()).readByEvent(event, "GeoPackage");
				layers.addAll(LayerHelper.getInstance(getContext()).readByEvent(event, "Feature"));
				layers.addAll(LayerHelper.getInstance(getContext()).readByEvent(event, "Imagery"));
				Collection<Layer> available = Collections2.filter(layers, layer -> !layer.isLoaded());

				OverlayPreference p = (OverlayPreference) findPreference(getResources().getString(R.string.tileOverlaysKey));
				p.setAvailableDownloads(!available.isEmpty());
			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem setting preference.", e);
			}
		}

		@Override
		public void onPause() {
			super.onPause();

			findPreference(getString(R.string.tileOverlaysKey)).setOnPreferenceClickListener(null);
			findPreference(getString(R.string.onlineLayersKey)).setOnPreferenceClickListener(null);
		}

		@Override
		public AndroidInjector<Object> androidInjector() {
			return androidInjector;
		}

		private void onFeeds(List<Feed> feeds) {
			PreferenceScreen screen = getPreferenceScreen();
			Set<String> enabledFeeds = mapLayerPreferences.getEnabledFeeds(event.getRemoteId());

			for (Feed feed : feeds) {
				PreferenceCategory category = (PreferenceCategory) screen.getPreference(screen.getPreferenceCount() - 1);
				SwitchPreferenceCompat feedPreference = mapLayerPreferences.mapFeedPreference(feed, getActivity(), enabledFeeds.contains(feed.getId()));
				feedPreference.setOnPreferenceClickListener( preference -> {
					onFeedClick(feed, feedPreference.isChecked());
					return true;
				});
				category.addPreference(feedPreference);
			}
		}

		private void onFeedClick(Feed feed, boolean on) {
			Set<String> feeds = mapLayerPreferences.getEnabledFeeds(event.getRemoteId());
			if (on) {
				feeds.add(feed.getId());
			} else {
				feeds.remove(feed.getId());
			}

			mapLayerPreferences.setEnabledFeeds(event.getRemoteId(), feeds);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}
}