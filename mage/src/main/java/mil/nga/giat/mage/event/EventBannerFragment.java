package mil.nga.giat.mage.event;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

/**
 *
 * Banner to hold event name
 *
 * @author wiedemanns
 */
public class EventBannerFragment extends Fragment {

	private TextView textView;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.event_header, container, false);

		textView = (TextView) rootView.findViewById(R.id.event_header_text);
		setText();

		return rootView;
	}

	private void setText() {
		Event event = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();
		String text = "";
		if (event != null) {
			text = event.getName();
		}

		Context context = getActivity();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		List<String> filters = new ArrayList<>();
		boolean favorites = preferences.getBoolean(context.getResources().getString(R.string.activeFavoritesFilterKey), false);
		if (favorites) {
			filters.add("Favorites");
		}

		boolean important = preferences.getBoolean(context.getResources().getString(R.string.activeImportantFilterKey), false);
		if (important) {
			filters.add("Important");
		}


		if (!filters.isEmpty()) {
			text += ", " + StringUtils.join(filters, " & ");
		}

		textView.setText(text);
	}

	public void refresh() {
		setText();
	}
}
