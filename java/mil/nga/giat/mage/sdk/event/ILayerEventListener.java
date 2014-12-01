package mil.nga.giat.mage.sdk.event;

import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;

public interface ILayerEventListener extends IEventListener {

	public void onLayersCreated(final Collection<Layer> layers);

}
