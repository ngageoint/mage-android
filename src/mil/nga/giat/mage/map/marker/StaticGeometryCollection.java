package mil.nga.giat.mage.map.marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

public class StaticGeometryCollection {

	private Map<String, Collection<Marker>> featureMarkers = new HashMap<String, Collection<Marker>>();
	private Map<String, Collection<Polyline>> featurePolylines = new HashMap<String, Collection<Polyline>>();
	private Map<String, Collection<Polygon>> featurePolygons = new HashMap<String, Collection<Polygon>>();

	private Map<Polyline, String> featurePolylineDescriptions = new HashMap<Polyline, String>();
	private Map<Polygon, String> featurePolygonDescriptions = new HashMap<Polygon, String>();

	public Set<String> getLayers() {
		Set<String> layerIds = new HashSet<String>();
		layerIds.addAll(featureMarkers.keySet());
		layerIds.addAll(featurePolylines.keySet());
		layerIds.addAll(featurePolygons.keySet());
		return layerIds;
	}

	public Polygon addPolygon(String layerId, Polygon polygon, String popupHTML) {
		if (featurePolygons.get(layerId) == null) {
			featurePolygons.put(layerId, new ArrayList<Polygon>());
		}

		featurePolygons.get(layerId).add(polygon);
		featurePolygonDescriptions.put(polygon, popupHTML);
		return polygon;
	}

	public void addPolyline(String layerId, Polyline polyline, String popupHTML) {
		if (featurePolylines.get(layerId) == null) {
			featurePolylines.put(layerId, new ArrayList<Polyline>());
		}

		featurePolylines.get(layerId).add(polyline);
		featurePolylineDescriptions.put(polyline, popupHTML);
	}

	public void addMarker(String layerId, Marker marker) {
		if (featureMarkers.get(layerId) == null) {
			featureMarkers.put(layerId, new ArrayList<Marker>());
		}

		featureMarkers.get(layerId).add(marker);
	}

	public String getPopupHTML(Polyline polyline) {
		return featurePolylineDescriptions.get(polyline);
	}

	public String getPopupHTML(Polygon polygon) {
		return featurePolygonDescriptions.get(polygon);
	}

	public Collection<Polygon> getPolygons() {
		Collection<Polygon> polygons = new ArrayList<Polygon>();
		for (Map.Entry<String, Collection<Polygon>> entry : featurePolygons.entrySet()) {
			for (Polygon p : entry.getValue()) {
				polygons.add(p);
			}
		}
		return polygons;
	}

	public Collection<Polyline> getPolylines() {
		Collection<Polyline> polylines = new ArrayList<Polyline>();
		for (Map.Entry<String, Collection<Polyline>> entry : featurePolylines.entrySet()) {
			for (Polyline p : entry.getValue()) {
				polylines.add(p);
			}
		}
		return polylines;
	}

	public Collection<Marker> getMarkers() {
		Collection<Marker> markers = new ArrayList<Marker>();
		for (Map.Entry<String, Collection<Marker>> entry : featureMarkers.entrySet()) {
			for (Marker m : entry.getValue()) {
				markers.add(m);
			}
		}
		return markers;
	}

	public void removeLayer(String layerId) {
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

}
