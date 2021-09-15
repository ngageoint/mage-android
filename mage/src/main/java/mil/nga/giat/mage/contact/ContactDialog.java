package mil.nga.giat.mage.contact;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import mil.nga.giat.mage.contact.utilities.LinkGenerator;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;

public class ContactDialog {

    private final Context myContext;
    private final SharedPreferences myPreferences;
    private final String myTitle;
    private String myMessage;
    private String myId;
    private String myStrategy;

    public ContactDialog(Context context, SharedPreferences preferences, String title) {
        myContext = context;
        myPreferences = preferences;
        myTitle = title;
    }

    public void setMessage(String message) {
        myMessage = message;
    }

    public void setId(String id) {
        myId = id;
    }

    public void setStrategy(String strategy){
        myStrategy = strategy;
    }

    public void show() {
        final Spanned s = addLinks();
        final AlertDialog d = new AlertDialog.Builder(myContext)
                .setTitle(myTitle)
                .setMessage(s)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private Spanned addLinks() {
        if(myId == null) {
            myId = new DeviceUuidFactory(myContext).getDeviceUuid().toString();;
        }

        final String emailLink = LinkGenerator.getEmailLink(myPreferences, myMessage, myId, myStrategy);
        final String phoneLink = LinkGenerator.getPhoneLink(myPreferences);

        String message = myMessage;
        if (!emailLink.isEmpty() || !phoneLink.isEmpty()) {
            message += "<br /><br />";
            message += "You may contact your MAGE administrator via ";
            if (!emailLink.isEmpty()) {
                message += "<a href=" + emailLink + ">Email</a>";
            }
            if (!emailLink.isEmpty() && !phoneLink.isEmpty()) {
                message += " or ";
            }
            if (!phoneLink.isEmpty()) {
                message += "<a href=" + phoneLink + ">Phone</a>";
            }
            message += " for further assistance.";
        }

        final Spanned s = Html.fromHtml(message);

        return s;
    }
}
