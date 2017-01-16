package mil.nga.giat.mage.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.FavoriteFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.filter.ImportantFilter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;

public class ObservationTask extends AsyncTask<Observation, Observation, Void> {
    public enum Type {
        ADD, UPDATE, DELETE
    }

    private Type type;
    private PointCollection<Observation> observationCollection;
    private Collection<Filter<?>> filters = new ArrayList<>();

    public ObservationTask(Context context, Type type, PointCollection<Observation> observationCollection) {
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
                    continue;
                }
            }

            if (passesFilter) {
                publishProgress(o);
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Observation... observations) {
        switch (type) {
            case ADD: {
                observationCollection.add(observations[0]);
                break;
            }
            case UPDATE: {
                observationCollection.remove(observations[0]);
                observationCollection.add(observations[0]);
                break;
            }
            case DELETE: {
                observationCollection.remove(observations[0]);
                break;
            }
        }
    }
}