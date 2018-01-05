package mil.nga.giat.mage.login;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import mil.nga.giat.mage.sdk.preferences.ServerApi;

/**
 * Created by wnewman on 1/3/18.
 */

public class ServerUrlFragment extends Fragment implements ServerApi.ServerApiListener {

    private ServerApi.ServerApiListener apiListener;

    private boolean validatingApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ServerApi.ServerApiListener) {
            apiListener = (ServerApi.ServerApiListener) context;
        } else {
            throw new IllegalStateException("Activity must implement the 'ServerApiListener' interfaces.");
        }
    }

    public void validateApi(String url) {
        ServerApi serverApi = new ServerApi(getActivity().getApplicationContext());
        serverApi.validateServerApi(url, this);
        validatingApi = true;
    }

    public boolean isValidatingApi() {
        return validatingApi;
    }

    @Override
    public void onApi(final boolean valid, final Exception error) {
        apiListener.onApi(valid, error);
        validatingApi = false;
    }
}
