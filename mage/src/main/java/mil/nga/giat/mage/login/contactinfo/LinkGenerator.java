package mil.nga.giat.mage.login.contactinfo;

import android.content.SharedPreferences;
import android.net.Uri;

public class LinkGenerator {

    /**
     * Prevent construction
     */
    private LinkGenerator() {
    }

    public static String getEmailLink(SharedPreferences preferences, String statusMessage, String identifier, String strategy) {
        StringBuilder url = new StringBuilder();

        final String email = preferences.getString("gAdminContactInfoEmail", null);
        if (email != null) {
            EmailBuilder emailBuilder = new EmailBuilder(statusMessage, identifier, strategy);
            emailBuilder.build();

            url.append(Uri.fromParts("mailto", email, null).toString());
            url.append("?subject=" + emailBuilder.getSubject());
            url.append("&body=" + emailBuilder.getBody());
        }

        return url.toString();
    }

    public static String getPhoneLink(SharedPreferences preferences) {
        StringBuilder url = new StringBuilder();

        final String phone = preferences.getString("gAdminContactInfoPhone", null);
        if (phone != null) {
            url.append(Uri.fromParts("tel", phone, null).toString());
        }

        return url.toString();
    }
}
