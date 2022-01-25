package de.mytrack.mytrackapp.spheric_geometry;

import androidx.annotation.NonNull;

import java.util.List;

import de.mytrack.mytrackapp.data.AreaPoint;

public class PointOnSphere extends Vector3d{
    protected final double latitude;
    protected final double longitude;

    @NonNull
    public static PointOnSphere[] from(@NonNull List<AreaPoint> points) {
        PointOnSphere[] pointsOnSphere = new PointOnSphere[points.size()];

        for (int i = 0; i < points.size(); i++) {
            AreaPoint point = points.get(i);
            PointOnSphere pointOnSphere = new PointOnSphere(point.latitude, point.longitude);
            pointsOnSphere[i] = pointOnSphere;
        }

        return pointsOnSphere;
    }

    public PointOnSphere(double latitude, double longitude){
        super(
                Math.sin((Math.PI / 180) * latitude),
                Math.cos((Math.PI / 180) * latitude)*Math.cos((Math.PI / 180) * longitude),
                Math.cos((Math.PI / 180) * latitude)*Math.sin((Math.PI / 180) * longitude)
        );
        this.latitude = (Math.PI / 180) * latitude;
        this.longitude = (Math.PI / 180) * longitude;
    }

    public PointOnSphere(Vector3d vector3d){
        super(vector3d);
        normalize();

        latitude = (180/Math.PI) * Math.asin(x[0]);
        if(x[1] > 0.0){
            longitude = (180/Math.PI) * Math.atan(x[2]/x[1]);
        } else if(x[1] < 0.0 && x[2] >= 0.0){
            longitude = (180/Math.PI) * (Math.atan(x[2]/x[1]) + Math.PI);
        } else if(x[1] < 0.0 && x[2] < 0.0){
            longitude = (180/Math.PI) * (Math.atan(x[2]/x[1]) - Math.PI);
        } else if(x[1] == 0.0 && x[2] > 0.0){
            longitude = 90.0;
        } else if(x[1] == 0.0 && x[2] < 0.0){
            longitude = -90.0;
        } else{
            longitude = 0.0;
        }
    }

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public double distance(PointOnSphere sp){ return Math.acos(this.dot(sp)); }

    public PointOnSphere project_to_line(PointOnSphere sp1, PointOnSphere sp2){
        Vector3d normal = sp1.cross(sp2);
        normal.normalize();
        Vector3d projection = normal.mul(-this.dot(normal)).add(this);
        projection.normalize();
        return new PointOnSphere(projection);
    }

    public double angle(PointOnSphere sp1, PointOnSphere sp2){
        Vector3d v1 = this.cross(sp1).cross(this);
        Vector3d v2 = this.cross(sp2).cross(this);
        v1.normalize();
        v2.normalize();
        double angle = Math.acos(v1.dot(v2));
        if (v1.cross(v2).dot(this) < 0.0){
            angle = 2*Math.PI - angle;
        }
        return  angle;
    }

    public double distance_to_line_segment(PointOnSphere sp1, PointOnSphere sp2){
        PointOnSphere s = project_to_line(sp1, sp2);
        if (
                this.cross(sp1).dot(s) *
                this.cross(sp2).dot(s)
                > 0.0
        ){
            return Math.min(this.distance(sp1), this.distance(sp2));
        }
        return this.distance(s);
    }
}
