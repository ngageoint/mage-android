package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

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
import mil.nga.giat.mage.sdk.exceptions.LayerException;

/**
 * Provides map configuration driven settings that are available to the user.
 * Check mappreferences.xml for the configuration.
 *
 * @author newmanw
 */
public class MapPreferencesActivity extends AppCompatActivity {

	public static String LOG_NAME = MapPreferencesActivity.class.getName();

	public static final int TILE_OVERLAY_ACTIVITY = 100;
	public static final int ONLINE_MAPS_OVERLAY_ACTIVITY = 200;
	public static final String OVERLAY_EXTENDED_DATA_KEY = "overlay";

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

			findPreference(getString(R.string.onlineMapsKey)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(getActivity(), OnlineMapsPreferenceActivity.class);
					getActivity().startActivityForResult(intent, ONLINE_MAPS_OVERLAY_ACTIVITY);
					return true;
				}
			});

			Event event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();

			// TODO : Remove the below and rework OverlayPreference to have a 'entities' similar to a list preference, these would be the 'display values'
			OverlayPreference p = (OverlayPreference) findPreference(getResources().getString(R.string.tileOverlaysKey));
			try {
				List<CacheOverlay> overlays = new CacheOverlayFilter(getContext(), event).filter(CacheProvider.getInstance(getContext()).getCacheOverlays());
				Set<String> layerIds = p.getValues();
				Collection<String> values = new ArrayList<>(layerIds.size());

				for (CacheOverlay overlay : overlays) {
					for (CacheOverlay child : overlay.getChildren()) {
						String name = overlay.getName() + "-" + child.getName();
						if (layerIds.contains(name)) {
							values.add(name );
						}
					}
				}

				p.setSummary(StringUtils.join(values, "\n"));

				List<Layer> layers = LayerHelper.getInstance(getContext()).readByEvent(event, "GeoPackage");
				Collection<Layer> available = Collections2.filter(layers, new Predicate<Layer>() {
					@Override
					public boolean apply(Layer layer) {
						return !layer.isLoaded();
					}
				});

				if (available.isEmpty()) {
					p.setDownloadIcon(null);
				} else {
					Drawable icon = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.baseline_cloud_download_white_24)).mutate();
					DrawableCompat.setTintList(icon, AppCompatResources.getColorStateList(getContext(), R.color.download_icon));
					p.setDownloadIcon(icon);
				}

				//TODO load anything for online maps if required
			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem setting preference.", e);
			}
		}

		@Override
		public void onPause() {
			super.onPause();

			findPreference(getString(R.string.tileOverlaysKey)).setOnPreferenceClickListener(null);
			findPreference(getString(R.string.onlineMapsKey)).setOnPreferenceClickListener(null);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case TILE_OVERLAY_ACTIVITY: {
				if (resultCode == Activity.RESULT_OK) {
					OverlayPreference p = (OverlayPreference) preference.findPreference(getString(R.string.tileOverlaysKey));
					p.setValues(new HashSet<>(data.getStringArrayListExtra(OVERLAY_EXTENDED_DATA_KEY)));
				}
				break;
			}
			case ONLINE_MAPS_OVERLAY_ACTIVITY: {
				//TODO implement for online maps.  Fall through to default for now
			}
			default: {
				super.onActivityResult(requestCode, resultCode, data);
				break;
			}
		}
	}
}