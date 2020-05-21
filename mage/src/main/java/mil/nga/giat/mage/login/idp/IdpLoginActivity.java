package mil.nga.giat.mage.login.idp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.FragmentActivity;

import com.google.gson.JsonObject;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.event.EventsActivity;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.login.SignupActivity;
import mil.nga.giat.mage.sdk.login.AbstractAccountTask;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.IdpLoginTask;
import mil.nga.giat.mage.sdk.login.IdpSignupTask;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;

/**
 * Created by wnewman on 10/14/15.
 */
public class IdpLoginActivity extends FragmentActivity implements AccountDelegate {

    public enum IdpType {
        SIGNIN,
        SIGINUP
    }

    private static final String LOG_NAME = IdpLoginActivity.class.getName();

    public static final String EXTRA_SERVER_URL = "EXTRA_SERVER_URL";
    public static final String EXTRA_IDP_TYPE = "EXTRA_IDP_TYPE";
    public static final String EXTRA_IDP_STRATEGY = "EXTRA_IDP_STRATEGY";

    private String serverURL;
    private String idpURL;
    private IdpType idpType;
    private String idpStrategy;

    private WebView webView;
    private View progress;
    private String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idp);

        serverURL = getIntent().getStringExtra(EXTRA_SERVER_URL);
        idpType = (IdpType) getIntent().getSerializableExtra(EXTRA_IDP_TYPE);
        idpStrategy = getIntent().getStringExtra(EXTRA_IDP_STRATEGY);
        idpURL = String.format("%s/auth/%s/signin", serverURL, idpStrategy);

        uuid = new DeviceUuidFactory(this).getDeviceUuid().toString();

        progress = findViewById(R.id.progress);

        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(this, "Android");
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.INVISIBLE);

                 if (url.contains("/callback")) {
                    webView.setVisibility(View.INVISIBLE);
                    webView.loadUrl("javascript:Android.getLogin(JSON.stringify(login));");
                }
            }
        });

        webView.loadUrl(idpURL);
    }

    @JavascriptInterface
    public void getLogin(String login) {
        AbstractAccountTask loginTask = idpType == IdpType.SIGNIN ?
                new IdpLoginTask(this, getApplicationContext()) :
                new IdpSignupTask(this, getApplicationContext());

        loginTask.execute(idpStrategy, uuid, login );
    }

    @Override
    public void finishAccount(AccountStatus accountStatus) {
        if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN)) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);

            Intent intent = showDisclaimer ?
                    new Intent(getApplicationContext(), DisclaimerActivity.class) :
                    new Intent(getApplicationContext(), EventsActivity.class);

            startActivity(intent);
            finish();
        } else if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_REGISTRATION)) {
            Intent intent = new Intent();
            intent.putExtra(LoginActivity.EXTRA_IDP_UNREGISTERED_DEVICE, true);
            setResult(RESULT_OK, intent);
            finish();
        } else if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_SIGNUP)) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        } else if (accountStatus.getStatus().equals(AccountStatus.Status.FAILED_SIGNUP)) {
            JsonObject json = accountStatus.getAccountInformation();
            String errorMessage = "Your account could not be created, please contact your MAGE administrator";

            if (json.has("errorMessage")) {
                errorMessage = json.get("errorMessage").getAsString();
            }

            Intent intent = new Intent();
            intent.putExtra(SignupActivity.EXTRA_IDP_ERROR, true);
            intent.putExtra(SignupActivity.EXTRA_IDP_ERROR_MESSAGE, errorMessage);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Intent intent = new Intent();
            intent.putExtra(LoginActivity.EXTRA_IDP_ERROR, true);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
