package mil.nga.giat.mage.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Point;

import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.FavoriteFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.filter.ImportantFilter;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

public class ObservationTask extends AsyncTask<Observation, Pair<MarkerOptions, Observation>, Void> {
    public enum Type {
        ADD, UPDATE, DELETE
    }

    private Context context;
    private Type type;
    private PointCollection<Observation> observationCollection;
    private Collection<Filter<?>> filters = new ArrayList<>();

    public ObservationTask(Context context, Type type, PointCollection<Observation> observationCollection) {
        this.context = context;
        this.type = type;
        this.observationCollection = observationCollection;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean important = preferences.getBoolean(context.getResources().getString(R.string.activeImportantFilterKey), false);
        if (important) {
            filters.add(new ImportantFilter(context));
        }

        boolean favorites = preferences.getBoolean(context.getResources().getString(R.string.activeFavoritesFilterKey), false);
        if (favorites) {
            filters.add(new FavoriteFilter(context));
        }
    }
    
    public void addFilter(Filter<?> filter) {
        if (filter == null) return;

        filters.add(filter);
    }

    @Override
    protected Void doInBackground(Observation... observations) {
        for (Observation o : observations) {
            boolean passesFilter = true;
            for (Filter filter : filters) {
                passesFilter = filter.passesFilter(o);
                if (!passesFilter) {
                    break;
                }
            }

            if (passesFilter) {
                Point point = (Point) o.getGeometry();
                MarkerOptions options = new MarkerOptions().position(new LatLng(point.getY(), point.getX())).icon(ObservationBitmapFactory.bitmapDescriptor(context, o));
                publishProgress(new Pair<>(options, o));
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Pair<MarkerOptions, Observation>... pairs) {
        switch (type) {
            case ADD: {
                observationCollection.add(pairs[0].first, pairs[0].second);
                break;
            }
            case UPDATE: {
                observationCollection.remove(pairs[0].second);
                observationCollection.add(pairs[0].first, pairs[0].second);
                break;
            }
            case DELETE: {
                observationCollection.remove(pairs[0].second);
                break;
            }
        }
    }
}