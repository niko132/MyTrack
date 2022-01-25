package de.mytrack.mytrackapp.spheric_geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import de.mytrack.mytrackapp.data.Area;

public class PolygonOnEarth extends PolygonOnSphere{
    /***
     * Represents a polygon on the earth.
     *
     * Example to use:
     *
     * // 1. Create a list spheric_points
     * SphericPoint v1 = new SphericPoint(51.02984271047772, 13.771130377605864);
     * SphericPoint v2 = new SphericPoint(51.036859316475706, 13.778194367744018);
     * SphericPoint v3 = new SphericPoint(51.04596934484342, 13.75492065272037);
     * SphericPoint v4 = new SphericPoint(51.038907228783614, 13.747591093140711);
     * SphericPoint[] spheric_points = {v1, v2, v3, v4};
     *
     * // 2. Create PolygonOnEarth
     * PolygonOnEarth great_garden = null;
     * try {
     *     great_garden = new PolygonOnEarth(spheric_points);
     * } catch (Exception e) {
     *     e.printStackTrace();
     * }
     *
     * // NOTE: The order of the points has to be in mathematical rotation. (anti-clockwise)
     * // NOTE: The inner angles have to be <= 180 degree. (Means the polygon has to be convex.)
     * // NOTE: If the rotation is wrong or the polygon is not convex the constructor will throw an exception.
     *
     * // 3. get distance to another point
     * SphericPoint hsz = new SphericPoint(51.02877241082275, 13.73004645971309);
     * double distance_between_hsz_and_great_garden = great_garden.distance(hsz);
     *
     *  // NOTE: The distance is positive if the point is outside of the polygon and negative if inside.
     *  // NOTE: The unit is in kilometers.
     *
     *  // 4. test if the point is inside of the polygon
     *  boolean hsz_inside_great_garden = great_garden.includes(hsz);
     *
     *  // NOTE: This operation needs less calculation resources then the distance function.
     ***/

    protected final double EARTH_RADIUS_IN_KM = 6367.5;

    @Nullable
    public static PolygonOnEarth from(@NonNull Area area) {
        PointOnSphere[] points = PointOnSphere.from(area.points);

        try {
            return new PolygonOnEarth(points);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public static PolygonOnEarth[] from(@NonNull List<Area> areas) {
        PolygonOnEarth[] polygons = new PolygonOnEarth[areas.size()];
        for (int i = 0; i < areas.size(); i++) {
            polygons[i] = PolygonOnEarth.from(areas.get(i));
        }
        return polygons;
    }

    public PolygonOnEarth(PointOnSphere[] spheric_points) throws Exception {
        super(spheric_points);
    }

    public double distance(PointOnSphere sp){
        return EARTH_RADIUS_IN_KM * super.distance(sp);
    }
}

