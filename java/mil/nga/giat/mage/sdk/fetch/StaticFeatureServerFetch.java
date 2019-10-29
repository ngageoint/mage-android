package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

public class StaticFeatureServerFetch extends AbstractServerFetch {

	public interface OnStaticLayersListener {
		void onStaticLayersLoaded(Collection<Layer> layers);
	}

	private LayerResource layerResource;

	public StaticFeatureServerFetch(Context context) {
		super(context);
		layerResource = new LayerResource(context);
	}

	private static final String LOG_NAME = StaticFeatureServerFetch.class.getName();
	private static final String FEATURE_TYPE = "Feature";

	private Boolean isCanceled = Boolean.FALSE;


	// TODO test that icons are pulled correctly
	public List<Layer> fetch(boolean deleteLocal, OnStaticLayersListener listener) {

		List<Layer> newLayers = new ArrayList<>();

		// if you are disconnect, skip this
		if(!ConnectivityUtility.isOnline(mContext) || LoginTaskFactory.getInstance(mContext).isLocalLogin()) {
			Log.d(LOG_NAME, "Disconnected, not pulling static layers.");
			return newLayers;
		}

		Event event = EventHelper.getInstance(mContext).getCurrentEvent();
		Log.d(LOG_NAME, "Pulling static layers for event " + event.getName());
		try {
			LayerHelper layerHelper = LayerHelper.getInstance(mContext);

			if (deleteLocal) {
				layerHelper.deleteAll(FEATURE_TYPE);
			}

			Collection<Layer> remoteLayers = layerResource.getLayers(event);

			// get local layers
			Collection<Layer> localLayers = layerHelper.readAll(FEATURE_TYPE);

			remoteLayers.removeAll(localLayers);

			for (Layer layer : remoteLayers) {
				layerHelper.create(layer);
			}

			newLayers.addAll(layerHelper.readAll(FEATURE_TYPE));

			if (listener != null) {
				listener.onStaticLayersLoaded(newLayers);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem creating layers.", e);
		}

		return newLayers;
	}

	public void load(OnStaticLayersListener listener, Layer layer) {
		try {
			if (!layer.isLoaded()) {
				StaticFeatureHelper staticFeatureHelper = StaticFeatureHelper.getInstance(mContext);
				try {
					Log.i(LOG_NAME, "Loading static features for layer " + layer.getName() + ".");

					Collection<StaticFeature> staticFeatures = layerResource.getFeatures(layer);

					// Pull down the icons
					Collection<String> failedIconUrls = new ArrayList<>();
					for (StaticFeature staticFeature : staticFeatures) {
						StaticFeatureProperty property = staticFeature.getPropertiesMap().get("styleiconstyleiconhref");
						if (property != null) {
							String iconUrlString = property.getValue();

							if (failedIconUrls.contains(iconUrlString)) {
								continue;
							}

							if (iconUrlString != null) {
								File iconFile = null;
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

									iconFile = new File(mContext.getFilesDir() + "/icons/staticfeatures", filename);
									if (!iconFile.exists()) {
										iconFile.getParentFile().mkdirs();
										iconFile.createNewFile();
										InputStream inputStream = layerResource.getFeatureIcon(iconUrlString);
										if (inputStream != null) {
											ByteStreams.copy(inputStream, new FileOutputStream(iconFile));
											staticFeature.setLocalPath(iconFile.getAbsolutePath());
										}
									} else {
										staticFeature.setLocalPath(iconFile.getAbsolutePath());
									}
								} catch (Exception e) {
									// this block should never flow exceptions up! Log for now.
									Log.w(LOG_NAME, "Could not get icon.", e);
									failedIconUrls.add(iconUrlString);
									if (iconFile != null && iconFile.exists()) {
										iconFile.delete();
									}
								}
							}
						}
					}

					layer = staticFeatureHelper.createAll(staticFeatures, layer);
					try {
						layer.setLoaded(true);
						DaoStore.getInstance(mContext).getLayerDao().update(layer);
					} catch (SQLException e) {
						throw new StaticFeatureException("Unable to update the layer to loaded: " + layer.getName());
					}

					Log.i(LOG_NAME, "Loaded static features for layer " + layer.getName());

					if(listener != null){
						List<Layer> layers = new ArrayList<>(1);
						layers.add(layer);
						listener.onStaticLayersLoaded(layers);
					}

				} catch (StaticFeatureException e) {
					Log.e(LOG_NAME, "Problem creating static features.", e);
				}
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem loading layers.", e);
		}
	}

	public void destroy() {
		isCanceled = true;
	}
}