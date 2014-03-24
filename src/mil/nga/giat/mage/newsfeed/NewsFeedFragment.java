package mil.nga.giat.mage.newsfeed;

import mil.nga.giat.mage.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NewsFeedFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);

		TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
		dummyTextView.setText("News Feed");

		return rootView;
	}
}
