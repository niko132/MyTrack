package de.mytrack.mytrackapp.spheric_geometry;

public class Vector3d {
    protected final double[] x;

    public Vector3d(double x1, double x2, double x3){
        x = new double[] {x1,x2,x3};
    }

    public Vector3d(Vector3d vector3d){ x = vector3d.x.clone(); }

    public double dot(Vector3d v){
        return x[0]*v.x[0] + x[1]*v.x[1] + x[2]*v.x[2];
    }

    public Vector3d cross(Vector3d v){
        return new Vector3d(
                x[1]*v.x[2] - x[2]*v.x[1],
                x[2]*v.x[0] - x[0]*v.x[2],
                x[0]*v.x[1] - x[1]*v.x[0]
        );
    }

    public double norm(){
        return Math.sqrt(x[0]* x[0]+ x[1]* x[1]+ x[2]* x[2]);
    }

    public void normalize(){
        // TODO: throw Exception when n == 0
        double n = norm();
        x[0] /= n;
        x[1] /= n;
        x[2] /= n;
    }

    public Vector3d add(Vector3d v){
        return new Vector3d(
                x[0] + v.x[0],
                x[1] + v.x[1],
                x[2] + v.x[2]
        );
    }

    public Vector3d mul(double a){
        return new Vector3d(
                a*x[0],
                a*x[1],
                a*x[2]
        );
    }
}

