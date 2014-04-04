package mil.nga.giat.mage.connection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.connectivity.NetworkChangeReceiver;
import mil.nga.giat.mage.sdk.event.connectivity.IConnectivityEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class ConnectionStatusFragment extends Fragment implements IConnectivityEventListener {
	private Boolean connected = true;
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.connection_status, container, false);
    }
	
    private Handler handler = new Handler();
    private Runnable fadeOutConnectedRunnable = new Runnable() {
    	public void run() {
    		fadeOutConnected();
    	}
    };
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		TextView t = (TextView)getActivity().findViewById(R.id.connection_status);
		
		t.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO: what does this connectivity status mean dialog
			}
		});
		
//		NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();
//		networkChangeReceiver.addListener(this);
//		
//		//set up initial connection state
//		connected = ConnectivityUtility.isOnline(getActivity().getApplicationContext());
//		
//		
//		// TODO set up the listener to just listen to the connectivity class
//		// don't show that we are connected if this is the first time and we are already connected
//		if (!connected) {
//			onConnectionChanged();
//		}
		super.onActivityCreated(savedInstanceState);
	}
	
	public void onConnectionChanged() {
		TextView t = (TextView)getActivity().findViewById(R.id.connection_status);
		getActivity().findViewById(R.id.connection_background).setVisibility(View.VISIBLE);
		Animation fadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), android.R.anim.fade_in);
		this.getView().setAnimation(fadeIn);
		Log.i("test", "connection changed to " + connected);
		if (connected) {
			t.setText(R.string.connected_mode);
			getActivity().findViewById(R.id.connection_background).setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
			handler.postDelayed(fadeOutConnectedRunnable, 2000);
		} else {
			t.setText(R.string.disconnected_mode);
			getActivity().findViewById(R.id.connection_background).setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
		}
	}
	
	public void fadeOutConnected() {
		Animation fadeOut = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), android.R.anim.fade_out);
		fadeOut.setAnimationListener(new Animation.AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				getActivity().findViewById(R.id.connection_background).setVisibility(View.GONE);
			}
		});
		getActivity().findViewById(R.id.connection_background).startAnimation(fadeOut);
	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAllDisconnected() {
		connected = false;
		onConnectionChanged();
	}

	@Override
	public void onAnyConnected() {
		connected = true;
		onConnectionChanged();
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
