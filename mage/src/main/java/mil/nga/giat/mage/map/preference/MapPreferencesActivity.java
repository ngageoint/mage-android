package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayFilter;
import mil.nga.giat.mage.map.cache.CacheProvider;
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
public class MapPreferencesActivity extends AppCompatActivity {

	public static String LOG_NAME = MapPreferencesActivity.class.getName();

	public static final int TILE_OVERLAY_ACTIVITY = 100;
	public static final int ONLINE_LAYERS_OVERLAY_ACTIVITY = 200;

	private MapPreferenceFragment preference = new MapPreferenceFragment();

	public static class MapPreferenceFragment extends PreferenceFragmentCompat {

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			addPreferencesFromResource(R.xml.mappreferences);
		}

		@Override
		public void onResume() {
			super.onResume();

			findPreference(getString(R.string.tileOverlaysKey)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(getActivity(), TileOverlayPreferenceActivity.class);
					getActivity().startActivityForResult(intent, TILE_OVERLAY_ACTIVITY);
					return true;
				}
			});

			findPreference(getString(R.string.onlineLayersKey)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(getActivity(), OnlineLayersPreferenceActivity.class);
					getActivity().startActivityForResult(intent, ONLINE_LAYERS_OVERLAY_ACTIVITY);
					return true;
				}
			});

			Event event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();

			// TODO : Remove the below and rework OverlayPreference to have a 'entities' similar to a list preference, these would be the 'display values'
			try {
				List<Layer> layers = LayerHelper.getInstance(getContext()).readByEvent(event, "GeoPackage");
				layers.addAll(LayerHelper.getInstance(getContext()).readByEvent(event, "Feature"));
				layers.addAll(LayerHelper.getInstance(getContext()).readByEvent(event, "Imagery"));
				Collection<Layer> available = Collections2.filter(layers, new Predicate<Layer>() {
					@Override
					public boolean apply(Layer layer) {
						return !layer.isLoaded();
					}
				});

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
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}
}