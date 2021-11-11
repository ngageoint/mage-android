package mil.nga.giat.mage.contact.utilities;

import android.content.SharedPreferences;
import android.net.Uri;

import java.net.URLEncoder;

public class LinkGenerator {

    /**
     * Prevent construction
     */
    private LinkGenerator() {
    }

    public static String getEmailLink(SharedPreferences preferences, String statusMessage, String identifier, String strategy) {
        StringBuilder url = new StringBuilder();

        final String email = preferences.getString("gContactinfoEmail", null);
        if (email != null && !email.isEmpty()) {
            EmailBuilder emailBuilder = new EmailBuilder(statusMessage, identifier, strategy);
            emailBuilder.build();

            url.append(Uri.fromParts("mailto", email, null).toString());
            try {
                url.append("?subject=" + URLEncoder.encode(emailBuilder.getSubject(), "UTF-8"));
                url.append("&body=" + URLEncoder.encode(emailBuilder.getBody(), "UTF-8"));
            } catch (Exception e) {

            }
        }

        return url.toString();
    }

    public static String getPhoneLink(SharedPreferences preferences) {
        StringBuilder url = new StringBuilder();

        final String phone = preferences.getString("gContactinfoPhone", null);
        if (phone != null && !phone.isEmpty()) {
            url.append(Uri.fromParts("tel", phone, null).toString());
        }

        return url.toString();
    }
}
