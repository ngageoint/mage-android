package mil.nga.giat.mage.map;

import java.util.Map;

import mil.nga.giat.mage.map.marker.StaticGeometryCollection;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class StaticFeatureLoadTask extends AsyncTask<Layer, Object, Void> {

	private static final String LOG_NAME = StaticFeatureLoadTask.class.getName();

	private StaticGeometryCollection staticGeometryCollection;
	private GoogleMap map;

	public StaticFeatureLoadTask(StaticGeometryCollection staticGeometryCollection, GoogleMap map) {
		this.staticGeometryCollection = staticGeometryCollection;
		this.map = map;
	}

	@Override
	protected Void doInBackground(Layer... layers) {
		Layer layer = layers[0];
		String layerId = layer.getId().toString();

		Log.d(LOG_NAME, "static feature layer: " + layer.getName() + " is enabled, it has " + layer.getStaticFeatures().size() + " features");

		for (StaticFeature feature : layer.getStaticFeatures()) {
			Geometry geometry = feature.getStaticFeatureGeometry().getGeometry();
			Map<String, StaticFeatureProperty> properties = feature.getPropertiesMap();
			String description = properties.get("description") != null ? properties.get("description").getValue() : "Unknown";
			String type = geometry.getGeometryType();
			if (type.equals("Point")) {
				MarkerOptions options = new MarkerOptions().position(new LatLng(geometry.getCoordinate().y, geometry.getCoordinate().x)).title(layer.getName()).snippet(description);
				publishProgress(new Object[] { options, layerId, description });
			} else if (type.equals("LineString")) {
				PolylineOptions options = new PolylineOptions();
				
				StaticFeatureProperty property = properties.get("stylelinestylecolorrgb");
				if (property != null) {
				    String color = property.getValue();
				    options.color(Color.parseColor(color));
				}
				for (Coordinate coordinate : geometry.getCoordinates()) {
					options.add(new LatLng(coordinate.y, coordinate.x));
				}
				publishProgress(new Object[] { options, layerId, description });
			} else if (type.equals("Polygon")) {
				PolygonOptions options = new PolygonOptions();
				
				StaticFeatureProperty property = properties.get("stylepolystylecolorrgb");
				if (property != null) {
				    String color = property.getValue();
				    int c = Color.parseColor(color);				    
				    options.fillColor(c).strokeColor(c);
				}
				
				for (Coordinate coordinate : geometry.getCoordinates()) {
					options.add(new LatLng(coordinate.y, coordinate.x));
				}
				publishProgress(new Object[] { options, layerId, description });
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... para) {
		Object options = para[0];
		String layerId = para[1].toString();
		String description = para[2].toString();
		if (options instanceof MarkerOptions) {
			Marker m = map.addMarker((MarkerOptions) options);
			staticGeometryCollection.addMarker(layerId, m);
		} else if (options instanceof PolylineOptions) {
			Polyline p = map.addPolyline((PolylineOptions) options);
			staticGeometryCollection.addPolyline(layerId, p, description);
		} else if (options instanceof PolygonOptions) {
			Polygon p = map.addPolygon((PolygonOptions) options);
			staticGeometryCollection.addPolygon(layerId, p, description);
		}
	}
}