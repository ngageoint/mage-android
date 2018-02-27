package mil.nga.giat.mage.event;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import mil.nga.giat.mage.sdk.fetch.EventsServerFetch;

/**
 * Created by wnewman on 2/26/18.
 */

public class EventsFetchFragment extends Fragment {

    private Context context;
    private EventsFetchListener eventsListener;
    private boolean fetching;
    private boolean fetched;

    public interface EventsFetchListener {
        void onEventsFetched(boolean status, Exception error);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;

        if (context instanceof EventsFetchListener) {
            eventsListener = (EventsFetchListener) context;
        } else {
            throw new IllegalStateException("Activity must implement the 'EventsFetchListener' interfaces.");
        }
    }

    public void loadEvents() {
        if (fetched) {
            eventsListener.onEventsFetched(true, null);
            return;
        }

        if (fetching) {
            return;
        }

        EventsServerFetch eventsServerFetch = new EventsServerFetch(context);
        eventsServerFetch.setEventFetchListener(new EventsServerFetch.EventsFetchListener() {
            @Override
            public void onEventsFetched(boolean status, Exception error) {
                eventsListener.onEventsFetched(status, error);

                fetching = false;
                if (status) {
                    fetched = true;
                }
            }
        });
        eventsServerFetch.execute();
        fetching = true;
    }
}
