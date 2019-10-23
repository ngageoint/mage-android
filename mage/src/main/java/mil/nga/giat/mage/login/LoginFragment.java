package mil.nga.giat.mage.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.login.AbstractAccountTask;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;
import mil.nga.giat.mage.sdk.preferences.ServerApi;

/**
 * Created by wnewman on 1/3/18.
 */

public class LoginFragment extends Fragment implements AccountDelegate {

    public interface LoginListener {
        void onApi(boolean valid);
        void onAuthentication(AccountStatus accountStatus);
    }

    private Context context;
    private LoginListener loginListener;
    private AbstractAccountTask loginTask;

    private boolean validatingApi;
    private boolean authenticating;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancel();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;

        if (context instanceof LoginListener) {
            loginListener = (LoginListener) context;
        } else {
            throw new IllegalStateException("Activity must implement the 'LoginListener' interfaces.");
        }

        // Validate server api
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String serverURL = sharedPreferences.getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue));
        validateApi(serverURL);
    }

    private void validateApi(String url) {
        if (StringUtils.isEmpty(url)) {
            return;
        }

        ServerApi serverApi = new ServerApi(context);
        serverApi.validateServerApi(url, new ServerApi.ServerApiListener() {
            @Override
            public void onApi(boolean valid, Exception error) {
                validatingApi = false;
                loginListener.onApi(valid);
            }
        });
        validatingApi = true;
    }

    public void authenticate(String[] credentialsArray) {
        if (!authenticating) {
            loginTask = LoginTaskFactory.getInstance(context).getLoginTask(this, context);
            loginTask.execute(credentialsArray);
            authenticating = true;
        }
    }

    public void cancel() {
        if (authenticating) {
            loginTask.cancel(false);
            loginTask = null;
            authenticating = false;
        }
    }

    public boolean isValidatingApi() {
        return validatingApi;
    }

    public boolean isAuthenticating() {
        return authenticating;
    }

    @Override
    public void finishAccount(final AccountStatus accountStatus) {
        authenticating = false;
        loginListener.onAuthentication(accountStatus);
    }
}
