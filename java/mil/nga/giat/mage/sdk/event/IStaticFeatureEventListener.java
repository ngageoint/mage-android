package mil.nga.giat.mage.sdk.event;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;

public interface IStaticFeatureEventListener extends IEventListener {

	/**
	 * The layer that the features were added to.
	 * 
	 * @param layer
	 */
	public void onStaticFeaturesCreated(final Layer layer);

}
