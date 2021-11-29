package mil.nga.giat.mage.map.marker;

import android.app.Activity;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.MapUtils;

public class StaticGeometryCollection {

	private static final String LOG_NAME = StaticGeometryCollection.class.getName();

	private Map<Long, Collection<Marker>> featureMarkers = new HashMap();
	private Map<Long, Collection<Polyline>> featurePolylines = new HashMap();
	private Map<Long, Collection<Polygon>> featurePolygons = new HashMap();

	private Map<Polyline, String> featurePolylineDescriptions = new HashMap();
	private Map<Polygon, String> featurePolygonDescriptions = new HashMap();

	public Set<Long> getLayers() {
		Set<Long> layerIds = new HashSet();
		layerIds.addAll(featureMarkers.keySet());
		layerIds.addAll(featurePolylines.keySet());
		layerIds.addAll(featurePolygons.keySet());
		return layerIds;
	}

	public Polygon addPolygon(Long layerId, Polygon polygon, String popupHTML) {
		if (featurePolygons.get(layerId) == null) {
			featurePolygons.put(layerId, new ArrayList());
		}

		featurePolygons.get(layerId).add(polygon);
		featurePolygonDescriptions.put(polygon, popupHTML);
		return polygon;
	}

	public void addPolyline(Long layerId, Polyline polyline, String popupHTML) {
		if (featurePolylines.get(layerId) == null) {
			featurePolylines.put(layerId, new ArrayList());
		}

		featurePolylines.get(layerId).add(polyline);
		featurePolylineDescriptions.put(polyline, popupHTML);
	}

	public void addMarker(Long layerId, Marker marker) {
		if (featureMarkers.get(layerId) == null) {
			featureMarkers.put(layerId, new ArrayList<Marker>());
		}

		featureMarkers.get(layerId).add(marker);
	}

	public void clear() {
		for (Collection<Marker> layer : featureMarkers.values()) {
			for (Marker marker : layer) {
				marker.remove();
			}
			layer.clear();
		}
		featureMarkers.clear();

		for (Collection<Polyline> layer : featurePolylines.values()) {
			for (Polyline polyline : layer) {
				polyline.remove();
			}
			layer.clear();
		}
		featurePolylines.clear();
		featurePolylineDescriptions.clear();

		for (Collection<Polygon> layer : featurePolygons.values()) {
			for (Polygon polygon : layer) {
				polygon.remove();
			}
			layer.clear();
		}
		featurePolygons.clear();
		featurePolygonDescriptions.clear();
	}

	public String getPopupHTML(Polyline polyline) {
		return featurePolylineDescriptions.get(polyline);
	}

	public String getPopupHTML(Polygon polygon) {
		return featurePolygonDescriptions.get(polygon);
	}

	public Collection<Polygon> getPolygons() {
		Collection<Polygon> polygons = new ArrayList();
		for (Map.Entry<Long, Collection<Polygon>> entry : featurePolygons.entrySet()) {
			for (Polygon p : entry.getValue()) {
				polygons.add(p);
			}
		}
		return polygons;
	}

	public Collection<Polyline> getPolylines() {
		Collection<Polyline> polylines = new ArrayList();
		for (Map.Entry<Long, Collection<Polyline>> entry : featurePolylines.entrySet()) {
			for (Polyline p : entry.getValue()) {
				polylines.add(p);
			}
		}
		return polylines;
	}

	public Collection<Marker> getMarkers() {
		Collection<Marker> markers = new ArrayList();
		for (Map.Entry<Long, Collection<Marker>> entry : featureMarkers.entrySet()) {
			for (Marker m : entry.getValue()) {
				markers.add(m);
			}
		}
		return markers;
	}

	public void removeLayer(Long layerId) {
		Collection<Marker> markers = featureMarkers.remove(layerId);
		if (markers != null) {
			for (Marker m : markers) {
				m.remove();
			}
		}

		Collection<Polyline> polylines = featurePolylines.remove(layerId);
		if (polylines != null) {
			for (Polyline p : polylines) {
				featurePolylineDescriptions.remove(p);
				p.remove();
			}
		}

		Collection<Polygon> polygons = featurePolygons.remove(layerId);
		if (polygons != null) {
			for (Polygon p : polygons) {
				featurePolygonDescriptions.remove(p);
				p.remove();
			}
		}
	}

	public void onMapClick(GoogleMap map, LatLng latLng, Activity activity) {

		// how many meters away form the click can the geometry be?
		double tolerance = MapUtils.lineTolerance(map);

		// find the 'closest' line or polygon to the click.
		for (Polyline p : getPolylines()) {
			if (PolyUtil.isLocationOnPath(latLng, p.getPoints(), true, tolerance)) {
				// found it open a info window
				Log.i(LOG_NAME, "static feature polyline clicked at: " + latLng.toString());

				View markerInfoWindow = LayoutInflater.from(activity).inflate(R.layout.static_feature_infowindow, null, false);
				WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.static_feature_infowindow_content));
				webView.loadData(getPopupHTML(p), "text/html; charset=UTF-8", null);
				new AlertDialog.Builder(activity).setView(markerInfoWindow).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
				return;
			}
		}

		for (Polygon p : getPolygons()) {
			if (PolyUtil.containsLocation(latLng, p.getPoints(), true)) {
				// found it open a info window
				Log.i(LOG_NAME, "static feature polygon clicked at: " + latLng.toString());

				View markerInfoWindow = LayoutInflater.from(activity).inflate(R.layout.static_feature_infowindow, null, false);
				WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.static_feature_infowindow_content));
				webView.loadData(getPopupHTML(p), "text/html; charset=UTF-8", null);
				new AlertDialog.Builder(activity).setView(markerInfoWindow).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
				return;
			}
		}
	}
}
