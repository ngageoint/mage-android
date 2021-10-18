package mil.nga.giat.mage.sdk.event;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;

public interface ILayerEventListener extends IEventListener {

	void onLayerCreated(Layer layer);
	void onLayerUpdated(Layer layer);

}
