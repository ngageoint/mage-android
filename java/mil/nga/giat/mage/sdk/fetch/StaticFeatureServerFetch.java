package mil.nga.giat.mage.sdk.fetch;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.ByteStreams;

public class StaticFeatureServerFetch extends AbstractServerFetch {

	public StaticFeatureServerFetch(Context context) {
		super(context);
	}

	private static final String LOG_NAME = StaticFeatureServerFetch.class.getName();

	private Boolean isCanceled = Boolean.FALSE;

	public void fetch() {
		fetch(false);
	}

	public void fetch(boolean force) {
		Editor sp = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

		StaticFeatureHelper staticFeatureHelper = StaticFeatureHelper.getInstance(mContext);
		LayerHelper layerHelper = LayerHelper.getInstance(mContext);

		// if you are disconnect, skip this
		if(!ConnectivityUtility.isOnline(mContext) || LoginTaskFactory.getInstance(mContext).isLocalLogin()) {
			Log.d(LOG_NAME, "Disconnected, not pulling static layers.");
			return;
		}

		Log.d(LOG_NAME, "Pulling static layers.");
		Collection<Layer> layers = MageServerGetRequests.getStaticLayers(mContext);
		try {
			if (force) {
				layerHelper.deleteAllStaticLayers();
			}

			layerHelper.createAll(layers);

			// FIXME : set a flag to not pull layers again


			// get ALL the layers
			layers = layerHelper.readAll();

			for (Layer layer : layers) {
				if (isCanceled) {
					break;
				}
				if (layer.getType().equalsIgnoreCase("external") && (force || !layer.isLoaded())) {
					try {
						Log.i(LOG_NAME, "Loading static features for layer " + layer.getName());

						Collection<StaticFeature> staticFeatures = MageServerGetRequests.getStaticFeatures(mContext, layer);

						DefaultHttpClient httpclient = HttpClientManager.getInstance(mContext).getHttpClient();

						// Pull down the icons
						for (StaticFeature staticFeature : staticFeatures) {
							StaticFeatureProperty property = staticFeature.getPropertiesMap().get("styleiconstyleiconhref");
							if (property != null) {
								String iconUrlString = property.getValue();
								if (iconUrlString != null) {
									HttpEntity entity = null;
									try {
										URL iconUrl = new URL(iconUrlString);
										String filename = iconUrl.getFile();
										// remove leading /
										if (filename != null) {
											filename = filename.trim();
											while (filename.startsWith("/")) {
												filename = filename.substring(1, filename.length());
											}
										}
										File iconFile = new File(mContext.getFilesDir() + "/icons/staticfeatures", filename);
										if (!iconFile.exists()) {
											iconFile.getParentFile().mkdirs();
											iconFile.createNewFile();
											HttpGet get = new HttpGet(iconUrl.toURI());
											HttpResponse response = httpclient.execute(get);
											if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
												entity = response.getEntity();
												ByteStreams.copy(entity.getContent(), new FileOutputStream(iconFile));
												staticFeature.setLocalPath(iconFile.getAbsolutePath());
											} else {
												entity = response.getEntity();
												String error = EntityUtils.toString(entity);
												Log.e(LOG_NAME, "Bad request.");
												Log.e(LOG_NAME, error);
											}
										} else {
											staticFeature.setLocalPath(iconFile.getAbsolutePath());
										}
									} catch (Exception e) {
										// this block should never flow exceptions up! Log for now.
										Log.w(LOG_NAME, "Could not get icon.", e);
										continue;
									} finally {
										try {
											if (entity != null) {
												entity.consumeContent();
											}
										} catch (Exception e) {
											Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
										}
									}
								}
							}
						}

						staticFeatureHelper.createAll(staticFeatures);

						layer.setLoaded(true);
						try {
							DaoStore.getInstance(mContext).getLayerDao().update(layer);
						} catch (SQLException e) {
							throw new StaticFeatureException("Unable to update the layer to loaded: " + layer.getName());
						}

						Log.i(LOG_NAME, "Loaded static features for layer " + layer.getName());

					} catch (StaticFeatureException e) {
						Log.e(LOG_NAME, "Problem creating static features.", e);
						continue;
					}
				}
			}
		} catch (LayerException e) {
			Log.e(LOG_NAME, "Problem creating layers.", e);
		}
	}

	public void destroy() {
		isCanceled = true;
	}
}