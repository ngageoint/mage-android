package mil.nga.giat.mage.event;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;

/**
 * Created by wnewman on 2/23/18.
 */

public class EventListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    static final int ITEM_TYPE_HEADER = 1;
    static final int ITEM_TYPE_EVENT = 2;

    private List<Event> events;
    private List<Event> filteredEvents = new ArrayList<>();
    private List<Event> recentEvents;
    private List<Event> filteredRecentEvents = new ArrayList<>();
    private OnEventClickListener listener;

    private static Predicate<Event> eventFilterPredicate(String text){
        final String lowerCaseText = text.toLowerCase();
        return new Predicate<Event>() {
            @Override
            public boolean apply(Event event) {
                return event.getName().toLowerCase().contains(lowerCaseText) ||
                        event.getDescription().toLowerCase().contains(lowerCaseText);
            }
        };
    }

    EventListAdapter(List<Event> events, List<Event> recentEvents, OnEventClickListener listener) {
        Collections.sort(events, new Ordering<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return e1.getName().toLowerCase().compareTo(e2.getName().toLowerCase());
            }
        });

        this.events = events;
        this.filteredEvents.addAll(events);
        this.recentEvents = recentEvents;
        this.filteredRecentEvents.addAll(recentEvents);
        this.listener = listener;
    }

    public void filter(String text) {
        filteredEvents.clear();
        filteredRecentEvents.clear();

        if (text.isEmpty()){
            filteredEvents.addAll(events);
            filteredRecentEvents.addAll(recentEvents);
        } else {
            filteredEvents = Lists.newArrayList(Iterables.filter(events, eventFilterPredicate(text)));
            filteredRecentEvents = Lists.newArrayList(Iterables.filter(recentEvents, eventFilterPredicate(text)));
        }

        notifyDataSetChanged();
    }

    private class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView sectionName;

        private SectionViewHolder(View view) {
            super(view);

            sectionName = (TextView) view.findViewById(R.id.section_name);
        }
    }

    private class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView description;

        private EventViewHolder(View view) {
            super(view);

            name = (TextView) view.findViewById(R.id.event_name);
            description = (TextView) view.findViewById(R.id.event_description);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_EVENT) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_list_item, parent, false);
            return new EventViewHolder(itemView);
        } else if (viewType == ITEM_TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_list_section_header, parent, false);
            return new SectionViewHolder(itemView);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EventViewHolder) {
            bindEventViewHolder((EventViewHolder) holder, position);
        } else if (holder instanceof SectionViewHolder) {
            bindSectionViewHolder((SectionViewHolder) holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return filteredEvents.size() + filteredRecentEvents.size() + 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == filteredRecentEvents.size() + 1) {
            return ITEM_TYPE_HEADER;
        } else {
            return ITEM_TYPE_EVENT;
        }
    }

    private void bindEventViewHolder(EventViewHolder holder, int position) {
        final Event event;
        if (position <= filteredRecentEvents.size()) {
            event = filteredRecentEvents.get(position - 1);
        } else {
            event = filteredEvents.get(position - filteredRecentEvents.size() - 2);
        }

        holder.name.setText(event.getName());

        if (StringUtils.isNoneBlank(event.getDescription())) {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(event.getDescription());
        } else {
            holder.description.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEventClick(event);
            }
        });
    }

    private void bindSectionViewHolder(SectionViewHolder holder, int position) {
        holder.sectionName.setText(position == 0 ? "Recent Events" : "Events");
    }
}
