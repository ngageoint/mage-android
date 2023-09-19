package mil.nga.giat.mage.sdk.event;

import mil.nga.giat.mage.database.model.layer.Layer;

public interface IStaticFeatureEventListener extends IEventListener {

	/**
	 * The layer that the features were added to.
	 * 
	 * @param layer
	 */
    void onStaticFeaturesCreated(final Layer layer);

}
