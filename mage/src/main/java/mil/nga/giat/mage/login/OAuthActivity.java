package mil.nga.giat.mage.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.text.DateFormat;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.disclaimer.DisclaimerActivity;
import mil.nga.giat.mage.event.EventActivity;
import mil.nga.giat.mage.sdk.login.AccountDelegate;
import mil.nga.giat.mage.sdk.login.AccountStatus;
import mil.nga.giat.mage.sdk.login.OAuthLoginTask;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;

/**
 * Created by wnewman on 10/14/15.
 */
public class OAuthActivity extends FragmentActivity implements AccountDelegate {

    private static final String LOG_NAME = OAuthActivity.class.getName();

    public static final String EXTRA_SERVER_URL = "EXTRA_SERVER_URL";

    private DateFormat iso8601Format = DateFormatFactory.ISO8601();

    private String serverURL;
    private WebView webView;
    private View progress;
    private String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth);

        serverURL = getIntent().getStringExtra(EXTRA_SERVER_URL);
        uuid = new DeviceUuidFactory(this).getDeviceUuid().toString();

        progress = findViewById(R.id.progress);

        webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(this, "Android");
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.INVISIBLE);

                if (url.startsWith(serverURL + "/auth/google/callback")) {
                    webView.setVisibility(View.INVISIBLE);
                    webView.loadUrl("javascript:Android.getLogin(JSON.stringify(login));");
                }
            }
        });

        webView.loadUrl(serverURL + "/auth/google/signin?uid=" + uuid);
    }

    @JavascriptInterface
    public void getLogin(String login) {
        OAuthLoginTask loginTask = new OAuthLoginTask(this, getApplicationContext());
        loginTask.execute(new String[]{ serverURL, login });
    }

    @Override
    public void finishAccount(AccountStatus accountStatus) {
        if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN)) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean showDisclaimer = sharedPreferences.getBoolean(getString(R.string.serverDisclaimerShow), false);

            Intent intent = showDisclaimer ?
                    new Intent(getApplicationContext(), DisclaimerActivity.class) :
                    new Intent(getApplicationContext(), EventActivity.class);

            startActivity(intent);
            finish();
        } else if (accountStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_REGISTRATION)) {
            Intent intent = new Intent();
            intent.putExtra(LoginActivity.EXTRA_OAUTH_UNREGISTERED_DEVICE, true);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Intent intent = new Intent();
            intent.putExtra(LoginActivity.EXTRA_OAUTH_ERROR, true);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
