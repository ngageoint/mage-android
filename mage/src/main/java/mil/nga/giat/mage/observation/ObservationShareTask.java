package mil.nga.giat.mage.observation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import okhttp3.ResponseBody;

/**
 * Created by wnewman on 8/27/16.
 */
public class ObservationShareTask extends AsyncTask<Void, Integer, ArrayList<Uri>> {

    public interface OnShareableListener {
        void onShareable(ArrayList<Uri> uris);
    }

    private static final String LOG_NAME = ObservationShareTask.class.getName();

    Activity activity;
    OnShareableListener shareableListener;
    ProgressDialog progressDialog;
    Observation observation;

    public ObservationShareTask(Activity activity, Observation observation) {
        this.activity = activity;
        this.observation = observation;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (observation.getAttachments().size() > 0) {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Attaching file 1 of " + observation.getAttachments().size());
            progressDialog.show();
        }
    }

    @Override
    protected ArrayList<Uri> doInBackground(Void... params) {
        Attachment[] attachments = observation.getAttachments().toArray(new Attachment[observation.getAttachments().size()]);

        ArrayList<Uri> uris = new ArrayList<>();

        InputStream is = null;
        OutputStream os = null;

        for (int i = 0; i < attachments.length; i++) {
            publishProgress(i, 0);

            Attachment attachment = attachments[i];

            if (attachment.getLocalPath() != null && new File(attachment.getLocalPath()).exists()) {
                Uri uri = Uri.fromFile(new File(attachment.getLocalPath()));
                uris.add(uri);
                publishProgress(i, 100);
                continue;
            }

            File cacheDir = new File(activity.getCacheDir(), "attachments");
            cacheDir.mkdirs();

            String extension = Files.getFileExtension(attachment.getName());
            File file = new File(cacheDir, attachment.getId().toString() + "." + extension);
            Uri uri  = FileProvider.getUriForFile(activity, "mil.nga.giat.mage.fileprovider", file);
            if (file.exists()) {
                publishProgress(i, 100);
                uris.add(uri);
                continue;
            }

            try {
                ObservationResource observationResource = new ObservationResource(activity);
                ResponseBody response = observationResource.getAttachment(attachment);

                Long contentLength = response.contentLength();

                os = new FileOutputStream(file);

                byte data[] = new byte[1024];

                Long total = 0l;
                int count;
                is = response.byteStream();
                while ((count = is.read(data)) != -1) {
                    total += count;
                    publishProgress(i, ((Double) (100.0 * (total.doubleValue() / contentLength.doubleValue()))).intValue());
                    os.write(data, 0, count);
                }

                uris.add(uri);
            } catch (Exception e) {
                Log.e(LOG_NAME, "Problem downloading attachment.", e);
            } finally {
                Closeables.closeQuietly(is);

                if (os != null) {
                    try {
                        os.close();;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return uris;
    }

    protected void onProgressUpdate(Integer... progress) {
        Integer number = progress[0] + 1;
        Integer percentage = progress[1];

        progressDialog.setProgress(percentage);
        progressDialog.setMessage("Attaching file " + number + " of " + observation.getAttachments().size());
    }

    @Override
    protected void onPostExecute(ArrayList<Uri> uris) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        if (uris.size() != observation.getAttachments().size()) {
            Toast toast = Toast.makeText(activity, "One or more attachments failed to attach", Toast.LENGTH_LONG);
            toast.show();
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, observation.getEvent().getName() + " MAGE Observation");
        intent.putExtra(Intent.EXTRA_TEXT, observationText(observation));
        intent.putExtra(Intent.EXTRA_STREAM, uris);
        activity.startActivity(Intent.createChooser(intent, "Share Observation"));
    }


    private Spanned observationText(Observation observation) {
        // TODO when we turn this back on, make it work for multiple forms

//        JsonObject formJson = observation.getEvent().getForm();
//        Map<String, JsonObject> nameToField = new TreeMap<>();
//        JsonArray dynamicFormFields = formJson.get("fields").getAsJsonArray();
//        for (int i = 0; i < dynamicFormFields.size(); i++) {
//            JsonObject field = dynamicFormFields.get(i).getAsJsonObject();
//            String name = field.get("name").getAsString();
//            nameToField.put(name, field);
//        }

        StringBuilder builder = new StringBuilder();

//        try {
//            User user = UserHelper.getInstance(activity).read(observation.getUserId());
//            builder.append("<strong>Created by:</strong><br>")
//                    .append(user.getDisplayName())
//                    .append("<br><br>");
//        } catch (UserException e) {
//            Log.e(LOG_NAME, "Error reading user with id: " + observation.getUserId(), e);
//        }
//
//        builder.append("<strong>Date:</strong><br>")
//                .append(observation.getTimestamp())
//                .append("<br><br>");
//
//        Point point = GeometryUtils.getCentroid(observation.getGeometry());
//        builder.append("<strong>Latitude, Longitude:</strong><br>")
//                .append(point.getY()).append(", ").append(point.getX())
//                .append("<br><br>");
//
//        ObservationProperty type = observation.getPropertiesMap().get("type");
//        builder.append(propertyText(type, nameToField.get(type.getKey())));
//
//        JsonElement variantJson = formJson.get("variantField");
//        String variantField = null;
//        if (variantJson != null) {
//            variantField = variantJson.getAsString();
//            ObservationProperty variantProperty = observation.getPropertiesMap().get(variantField);
//            if (variantProperty != null) {
//                JsonObject field = nameToField.get(variantProperty.getKey());
//                builder.append(propertyText(variantProperty, field));
//            }
//        }
//
//        for (ObservationProperty property : observation.getProperties()) {
//            JsonObject field = nameToField.get(property.getKey());
//            if (field == null || "type".equals(property.getKey()) || "timestamp".equals(property.getKey()) || property.getKey().equals(variantField)) {
//                continue;
//            }
//
//            Serializable value = property.getValue();
//            if (value == null || (value instanceof String && (StringUtils.isEmpty((String) value))) ||(value instanceof Collection && ((Collection) value).isEmpty())) {
//                continue;
//            }
//
//            JsonElement archivedJson = field.get("archived");
//            if (archivedJson != null && archivedJson.getAsBoolean()) {
//                continue;
//            }
//
//            builder.append(propertyText(property, field));
//        }

        return Html.fromHtml(builder.toString());
    }

    private String propertyText(ObservationProperty property, JsonObject field) {
        String title = field.get("title").getAsString();
        return "<strong>" + title + "</strong>:<br>" + property.getValue() + "<br><br>";
    }
}
