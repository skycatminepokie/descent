package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4d;
import org.joml.Vector4d;

public record Sphere(Vec3d center, double radiusSquared) {
    public static Sphere circumsphere(Vec3d a, Vec3d b, Vec3d c, Vec3d d) {
        double det1 = new Matrix4d(
                new Vector4d(a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(a.getZ(), b.getX(), c.getZ(), d.getZ()),
                new Vector4d(1, 1, 1, 1)
        ).determinant();

        double aSqr = a.squaredDistanceTo(0, 0, 0);
        double bSqr = b.squaredDistanceTo(0, 0, 0);
        double cSqr = c.squaredDistanceTo(0, 0, 0);
        double dSqr = d.squaredDistanceTo(0, 0, 0);

        double dx = new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(a.getZ(), b.getZ(), c.getZ(), d.getZ()),
                new Vector4d(1, 1, 1, 1)
        ).determinant();

        double dy = -(new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(a.getZ(), b.getZ(), c.getZ(), d.getZ()),
                new Vector4d(1, 1, 1, 1)
        ).determinant());

        double dz = new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(1, 1, 1, 1)
        ).determinant();

        double det2 = new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(a.getZ(), b.getZ(), c.getZ(), d.getZ())
        ).determinant();

        var circumcenter = new Vec3d(
                dx / (2 * det1),
                dy / (2 * det1),
                dz / (2 * det1)
        );

        var circumradiusSquared = ((dx * dx) + (dy * dy) + (dz * dz) - (4 * det1 * det2)) / (4 * det1 * det1);
        return new Sphere(circumcenter, circumradiusSquared);
    }
}
