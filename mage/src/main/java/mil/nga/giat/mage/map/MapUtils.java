package mil.nga.giat.mage.map;

import com.google.android.gms.maps.GoogleMap;

import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;

/**
 * Map utilities
 */
public class MapUtils {

    /**
     * Get the map point to line distance tolerance
     *
     * @param map map
     * @return
     */
    public static double lineTolerance(GoogleMap map) {
        // how many meters away form the click can the geometry be?
        double circumferenceOfEarthInMeters = 2 * Math.PI * 6371000;
        double pixelSizeInMetersAtLatitude = (circumferenceOfEarthInMeters * Math.cos(map.getCameraPosition().target.latitude * (Math.PI / 180.0))) / Math.pow(2.0, map.getCameraPosition().zoom + 8.0);
        double tolerance = pixelSizeInMetersAtLatitude * Math.sqrt(2.0) * 10.0;
        return tolerance;
    }

    public static boolean polygonHasKinks(Polygon polygon) {
        for (LineString line1 : polygon.getRings()) {
            Point lastPoint = line1.getPoints().get(line1.numPoints() - 1);
            for (LineString line2 : polygon.getRings()) {
                for (int i = 0; i < line1.numPoints() - 1; i++) {
                    Point point1 = line1.getPoints().get(i);
                    Point nextPoint1 = line1.getPoints().get(i + 1);
                    for (int k = i; k < line2.numPoints() - 1; k++) {
                        Point point2 = line2.getPoints().get(k);
                        Point nextPoint2 = line2.getPoints().get(k + 1);
                        if (line1 != line2) {
                            continue;
                        }

                        if (Math.abs(i - k) == 1) {
                            continue;
                        }

                        if (i == 0 && k == line1.numPoints() - 2 && point1.getX() == lastPoint.getX() && point1.getY() == lastPoint.getY()) {
                            continue;
                        }

                        boolean intersects = intersects(point1, nextPoint1, point2, nextPoint2);

                        if (intersects) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean intersects(Point point1Start, Point point1End, Point point2Start, Point point2End) {
        double q =
                //Distance between the lines' starting rows times line2's horizontal length
                (point1Start.getY() - point2Start.getY()) * (point2End.getX() - point2Start.getX())
                        //Distance between the lines' starting columns times line2's vertical length
                        - (point1Start.getX() - point2Start.getX()) * (point2End.getY() - point2Start.getY());
        double d =
            //Line 1's horizontal length times line 2's vertical length
            (point1End.getX() - point1Start.getX()) * (point2End.getY() - point2Start.getY())
                    //Line 1's vertical length times line 2's horizontal length
                    - (point1End.getY() - point1Start.getY()) * (point2End.getX() - point2Start.getX());

        if (d == 0) {
            return false;
        }

        double r = q / d;

        q =
            //Distance between the lines' starting rows times line 1's horizontal length
            (point1Start.getY() - point2Start.getY()) * (point1End.getX() - point1Start.getX())
                    //Distance between the lines' starting columns times line 1's vertical length
                    - (point1Start.getX() - point2Start.getX()) * (point1End.getY() - point1Start.getY());

        double s = q / d;

        if( r < 0 || r > 1 || s < 0 || s > 1 ) {
            return false;
        }

        return true;
    }
}
