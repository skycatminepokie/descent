package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;

public record Tetrahedron(Vec3d a, Vec3d b, Vec3d c, Vec3d d, Sphere circumsphere) {

    public Tetrahedron(Vec3d a, Vec3d b, Vec3d c, Vec3d d) {
        this(a, b, c, d, Sphere.circumsphere(a, b, c, d));
    }

    public boolean hasVertex(Vec3d point) {
        return Delaunay3D.almostEqual(point, a) ||
               Delaunay3D.almostEqual(point, b) ||
               Delaunay3D.almostEqual(point, c) ||
               Delaunay3D.almostEqual(point, d);
    }

    public boolean circumsphereContains(Vec3d vec) {
        return vec.subtract(circumsphere.center()).squaredDistanceTo(0, 0, 0) <= circumsphere.radiusSquared();
    }

}
