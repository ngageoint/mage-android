package mil.nga.giat.mage.help;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mil.nga.giat.mage.BuildConfig;
import mil.nga.giat.mage.R;

public class HelpActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_help);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle("MAGE");
		setSupportActionBar(toolbar);

		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.addTab(tabLayout.newTab().setText("About"));
		tabLayout.addTab(tabLayout.newTab().setText("Acknowledgements"));
		tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		List<Fragment> fragments = new ArrayList<>(Arrays.asList(new AboutFragment(), new AcknowledgementsFragment()));

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);
		if (showDisclaimer) {
			fragments.add(1, new DisclaimerFragment());
			tabLayout.addTab(tabLayout.newTab().setText("Disclaimer"), 1);
		}

		final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

		PagerAdapter adapter = new HelpPagerAdapter(getSupportFragmentManager(), fragments);
		viewPager.setAdapter(adapter);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				viewPager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {

			}
		});
	}

	public static class AboutFragment extends Fragment implements View.OnClickListener {
		private int clicks = 0;
		private TextView version;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_help_about, container, false);

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

			TextView serverUrl  = (TextView) view.findViewById(R.id.server_url);
			serverUrl.setText(sharedPreferences.getString(getString(R.string.serverURLKey), ""));

			version  = (TextView) view.findViewById(R.id.version);
			version.setText(BuildConfig.VERSION_NAME);

			view.findViewById(R.id.version_layout).setOnClickListener(this);

			return view;
		}

		@Override
		public void onClick(View v) {
			clicks++;
			if (clicks == 5) {
				version.setText(String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
			}
		}
	}

	public static class DisclaimerFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_help_disclaimer, container, false);

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
			String disclaimerText = sharedPreferences.getString(getString(R.string.serverDisclaimerText), null);
			((TextView) view.findViewById(R.id.disclaimer_text)).setText(disclaimerText);

			return view;
		}
	}

	public static class AcknowledgementsFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.fragment_help_acknowledgement, container, false);
		}
	}

	public static class HelpPagerAdapter extends FragmentPagerAdapter {
		List<Fragment> fragments;

		public HelpPagerAdapter(FragmentManager fragmentManager, List<Fragment> fragments) {
			super(fragmentManager);
			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}
	}

}
