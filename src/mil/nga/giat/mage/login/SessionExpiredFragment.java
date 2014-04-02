package mil.nga.giat.mage.login;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class SessionExpiredFragment extends Fragment {
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.session_expired, container, false);
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Button b = (Button)getActivity().findViewById(R.id.session_btn);
		b.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getActivity().finish();
				startActivity(new Intent(getActivity().getApplicationContext(), LoginActivity.class));
			}
		});
		
		if (UserUtility.getInstance(getActivity().getApplicationContext()).isTokenExpired()) {
			b.setVisibility(View.VISIBLE);
		} else {
			b.setVisibility(View.GONE);
		}
		
		super.onActivityCreated(savedInstanceState);
	}
}
