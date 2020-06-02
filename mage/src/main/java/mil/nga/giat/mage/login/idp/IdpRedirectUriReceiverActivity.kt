package mil.nga.giat.mage.login.idp

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class IdpRedirectUriReceiverActivity: Activity() {

    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)

        // Handling the redirect in this way ensures that we can remove the browser tab
        // from the back stack.
        val uri = intent.data
        val intent = Intent(this, IdpLoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.data = uri

        startActivity(intent)

        finish()
    }

}