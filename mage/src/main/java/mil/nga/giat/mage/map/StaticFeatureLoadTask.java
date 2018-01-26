package mil.nga.giat.mage.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.map.marker.StaticGeometryCollection;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;

public class StaticFeatureLoadTask extends AsyncTask<Layer, Object, Void> {

	private static final String LOG_NAME = StaticFeatureLoadTask.class.getName();

	private StaticGeometryCollection staticGeometryCollection;
	private GoogleMap map;
	private Context context;

	public StaticFeatureLoadTask(Context context, StaticGeometryCollection staticGeometryCollection, GoogleMap map) {
		this.context = context;
		this.staticGeometryCollection = staticGeometryCollection;
		this.map = map;
	}

	@Override
	protected Void doInBackground(Layer... layers) {
		Layer layer = layers[0];
		String layerId = layer.getId().toString();

		Log.d(LOG_NAME, "static feature layer: " + layer.getName() + " is enabled, it has " + layer.getStaticFeatures().size() + " features");

		for (StaticFeature feature : layer.getStaticFeatures()) {
			Geometry geometry = feature.getGeometry();
			Map<String, StaticFeatureProperty> properties = feature.getPropertiesMap();

			StringBuilder content = new StringBuilder();
			if (properties.get("name") != null) {
				content.append("<h5>").append(properties.get("name").getValue()).append("</h5>");
			}
			if (properties.get("description") != null) {
				content.append("<div>").append(properties.get("description").getValue()).append("</div>");
			}
			GeometryType type = geometry.getGeometryType();
			if (type == GeometryType.POINT) {
				Point point = (Point) geometry;
				MarkerOptions options = new MarkerOptions().position(new LatLng(point.getY(), point.getX())).snippet(content.toString());

				// check to see if there's an icon
				String iconPath = feature.getLocalPath();
				if (iconPath != null) {
					File iconFile = new File(iconPath);
					if (iconFile.exists()) {
						BitmapFactory.Options o = new BitmapFactory.Options();
						o.inDensity = 480;
						o.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
						try {
							Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(iconFile), null, o);
							if (bitmap != null) {
								options.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
							}
						} catch (FileNotFoundException fnfe) {
							Log.e(LOG_NAME, "Could not set icon.", fnfe);
						}
					}
				}

				publishProgress(new Object[] { options, layerId, content.toString() });
			} else if (type == GeometryType.LINESTRING) {
				PolylineOptions options = new PolylineOptions();

				StaticFeatureProperty property = properties.get("stylelinestylecolorrgb");
				if (property != null) {
					String color = property.getValue();
					options.color(Color.parseColor(color));
				}
				LineString lineString = (LineString) geometry;
				for(Point point: lineString.getPoints()){
					options.add(new LatLng(point.getY(), point.getX()));
				}
				publishProgress(new Object[] { options, layerId, content.toString() });
			} else if (type == GeometryType.POLYGON) {
				PolygonOptions options = new PolygonOptions();

				Integer color = null;
				StaticFeatureProperty property = properties.get("stylelinestylecolorrgb");
				if (property != null) {
					String colorProperty = property.getValue();
					color = Color.parseColor(colorProperty);
					options.strokeColor(color);
				} else {
					property = properties.get("stylepolystylecolorrgb");
					if (property != null) {
					    String colorProperty = property.getValue();
						color = Color.parseColor(colorProperty);
						options.strokeColor(color);
					}
				}
				
                property = properties.get("stylepolystylefill");
                if (property != null) {
                    String fill = property.getValue();
                    if ("1".equals(fill) && color != null) {
                        options.fillColor(color);
                    }
                }

				mil.nga.wkb.geom.Polygon polygon = (mil.nga.wkb.geom.Polygon) geometry;
				List<LineString> rings = polygon.getRings();
				LineString polygonLineString = rings.get(0);
				for (Point point : polygonLineString.getPoints()) {
					LatLng latLng = new LatLng(point.getY(), point.getX());
					options.add(latLng);
				}
				for (int i = 1; i < rings.size(); i++) {
					LineString hole = rings.get(i);
					List<LatLng> holeLatLngs = new ArrayList<>();
					for (Point point : hole.getPoints()) {
						LatLng latLng = new LatLng(point.getY(), point.getX());
						holeLatLngs.add(latLng);
					}
					options.addHole(holeLatLngs);
				}

				publishProgress(new Object[] { options, layerId, content.toString() });
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Object... para) {
		Object options = para[0];
		String layerId = para[1].toString();
		String content = para[2].toString();
		if (options instanceof MarkerOptions) {
			Marker m = map.addMarker((MarkerOptions) options);
			staticGeometryCollection.addMarker(layerId, m);
		} else if (options instanceof PolylineOptions) {
			Polyline p = map.addPolyline((PolylineOptions) options);
			staticGeometryCollection.addPolyline(layerId, p, content);
		} else if (options instanceof PolygonOptions) {
			Polygon p = map.addPolygon((PolygonOptions) options);
			staticGeometryCollection.addPolygon(layerId, p, content);
		}
	}
}