package mil.nga.giat.mage.login;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.connectivity.NetworkChangeReceiver;
import mil.nga.giat.mage.sdk.event.IConnectivityEventListener;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.utils.UserUtility;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Alert banner at the top of the screen
 *
 * @author wiedemanns
 */
public class AlertBannerFragment extends Fragment implements IUserEventListener, IConnectivityEventListener {

	private static final String SESSION_EXPIRED = "Session expired. Click to re-login.";
	private static final String DISCONNECTED_MODE = "Disconnected mode.";
	private Activity mActivity;
	private View alertBanner;
	private Button alertBannerButton;
	private View.OnClickListener buttonOnClickListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		HttpClientManager.getInstance(getActivity().getApplicationContext()).addListener(this);
		NetworkChangeReceiver.getInstance().addListener(this);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_alert_banner, container, false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		alertBanner = getActivity().findViewById(R.id.alert_banner);
		alertBannerButton = (Button) getActivity().findViewById(R.id.alert_banner_button);
		buttonOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().finish();
				Intent intent = new Intent(getActivity().getApplicationContext(), LoginActivity.class);
				intent.putExtra("LOGOUT", true);
				startActivity(intent);
			}
		};

		if (!ConnectivityUtility.isOnline(getActivity().getApplicationContext())) {
			alertBannerButton.setText(DISCONNECTED_MODE);
			alertBannerButton.setOnClickListener(null);
			alertBanner.setVisibility(View.VISIBLE);
		} else if (UserUtility.getInstance(getActivity().getApplicationContext()).isTokenExpired()) {
			alertBannerButton.setText(SESSION_EXPIRED);
			alertBannerButton.setOnClickListener(buttonOnClickListener);
			alertBanner.setVisibility(View.VISIBLE);
		} else {
			alertBanner.setVisibility(View.GONE);
		}

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onTokenExpired() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				alertBannerButton.setText(SESSION_EXPIRED);
				alertBannerButton.setOnClickListener(buttonOnClickListener);
				alertBanner.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAllDisconnected() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				alertBannerButton.setText(DISCONNECTED_MODE);
				alertBannerButton.setOnClickListener(null);
				alertBanner.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void onAnyConnected() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (UserUtility.getInstance(mActivity.getApplicationContext()).isTokenExpired()) {
					alertBannerButton.setText("Connectivity restored. Click to re-login.");
					alertBannerButton.setOnClickListener(buttonOnClickListener);
					alertBanner.setVisibility(View.VISIBLE);
				} else {
					alertBannerButton.setOnClickListener(null);
					alertBanner.setVisibility(View.GONE);
				}
			}
		});
	}

	@Override
	public void onWifiConnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onWifiDisconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMobileDataConnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMobileDataDisconnected() {
		// TODO Auto-generated method stub

	}
}
