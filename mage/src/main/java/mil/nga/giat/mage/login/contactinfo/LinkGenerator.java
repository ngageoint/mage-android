package mil.nga.giat.mage.login.contactinfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class LinkGenerator {

    /**
     * Prevent construction
     */
    private LinkGenerator() {
    }

    public static Intent getEmailLink(SharedPreferences preferences, String statusMessage, String identifier, String strategy) {
        Intent emailIntent = null;

        final String email = preferences.getString("gAdminContactInfoEmail", null);
        if (email != null) {
            EmailBuilder emailBuilder = new EmailBuilder(statusMessage, identifier, strategy);
            emailBuilder.build();

            emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailBuilder.getSubject());
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailBuilder.getBody());
        }

        return emailIntent;
    }

    public static Intent getPhoneLink(SharedPreferences preferences) {
        Intent phoneIntent = null;

        final String phone = preferences.getString("gAdminContactInfoPhone", null);
        if (phone != null) {
            phoneIntent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
        }

        return phoneIntent;
    }
}
