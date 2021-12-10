package de.mytrack.mytrackapp.spheric_geometry;

public class PolygonOnSphere {
    protected final PointOnSphere[] spheric_points;
    protected final Vector3d[] normals;

    public PolygonOnSphere(PointOnSphere[] spheric_points) throws Exception{
        this.spheric_points = spheric_points.clone();
        normals = new Vector3d[spheric_points.length];
        for(int i = 0; i< spheric_points.length-1; i++){
            normals[i] = normal(spheric_points[i], spheric_points[i+1]);
        }
        normals[spheric_points.length-1] = normal(spheric_points[spheric_points.length-1], spheric_points[0]);
        if(!is_convex()){
            throw new Exception("Polygon is not convex.");
        }
    }

    public double distance(PointOnSphere sp){
        double min_dist = sp.distance_to_line_segment(
                spheric_points[spheric_points.length-1],
                spheric_points[0]);
        double min_tmp;
        for(int i = 0; i < spheric_points.length-1; i++){
            min_tmp = sp.distance_to_line_segment(spheric_points[i], spheric_points[i+1]);
            if(min_tmp < min_dist){
                min_dist = min_tmp;
            }
        }
        if(includes(sp)){
            min_dist = -min_dist;
        }
        return min_dist;
    }

    public boolean is_convex(){
        for(int i = 1; i < spheric_points.length-1; i++){
            if(spheric_points[i].angle(spheric_points[i+1], spheric_points[i-1]) > Math.PI){
                return false;
            }
        }

        if(spheric_points[0].angle(
                spheric_points[1],
                spheric_points[spheric_points.length-1]
        ) > Math.PI){
            return false;
        }

        if(spheric_points[spheric_points.length-1].angle(
                spheric_points[0],
                spheric_points[spheric_points.length-2]
        ) > Math.PI){
            return false;
        }
        return true;
    }

    public boolean includes(PointOnSphere v) {
        for (Vector3d normal : normals) {
            if (normal.dot(v) < 0.0) {
                return false;
            }
        }
        return true;
    }

    private Vector3d normal(PointOnSphere v1, PointOnSphere v2) {
        // TODO: Exception when liner depended
        Vector3d normal = v1.cross(v2);
        normal.normalize();
        return normal;
    }
}
