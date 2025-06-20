package com.skycatdev.descent.util;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4d;
import org.joml.Vector4d;

import java.util.Objects;

public class Tetrahedron {
    protected Vec3d a;
    protected Vec3d b;
    protected Vec3d c;
    protected Vec3d d;
    protected boolean isBad;
    protected Vec3d circumcenter;
    protected double circumradiusSquared;

    public Tetrahedron(Vec3d a, Vec3d b, Vec3d c, Vec3d d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        calculateCircumsphere();
    }

    private void calculateCircumsphere() {
        double a = new Matrix4d(
                new Vector4d(this.a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(this.a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(this.a.getZ(), b.getX(), c.getZ(), d.getZ()),
                new Vector4d(1, 1, 1, 1)
        ).determinant();

        double aSqr = this.a.squaredDistanceTo(0, 0, 0);
        double bSqr = this.b.squaredDistanceTo(0, 0, 0);
        double cSqr = this.c.squaredDistanceTo(0, 0, 0);
        double dSqr = this.d.squaredDistanceTo(0, 0, 0);

        double dx = new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(this.a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(this.a.getZ(), b.getZ(), c.getZ(), d.getZ()),
                new Vector4d(1, 1, 1, 1)
        ).determinant();

        double dy = -(new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(this.a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(this.a.getZ(), b.getZ(), c.getZ(), d.getZ()),
                new Vector4d(1, 1, 1, 1)
        ).determinant());

        double dz = new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(this.a.getX(), b.getX(), c.getX(), d.getX()),
                new Vector4d(this.a.getY(), b.getY(), c.getY(), d.getY()),
                new Vector4d(1, 1, 1, 1)
        ).determinant();

        double c = new Matrix4d(
                new Vector4d(aSqr, bSqr, cSqr, dSqr),
                new Vector4d(this.a.getX(), b.getX(), this.c.getX(), d.getX()),
                new Vector4d(this.a.getY(), b.getY(), this.c.getY(), d.getY()),
                new Vector4d(this.a.getZ(), b.getZ(), this.c.getZ(), d.getZ())
        ).determinant();

        circumcenter = new Vec3d(
                dx / (2 * a),
                dy / (2 * a),
                dz / (2 * a)
        );

        circumradiusSquared = ((dx * dx) + (dy * dy) + (dz * dz) - (4 * a * c)) / (4 * a * a);

    }

    public boolean containsVertex(Vec3d point) {
        return Delaunay3D.almostEqual(point, a) ||
               Delaunay3D.almostEqual(point, b) ||
               Delaunay3D.almostEqual(point, c) ||
               Delaunay3D.almostEqual(point, d);
    }

    public boolean circumsphereContains(Vec3d vec) {
        return vec.subtract(circumcenter).squaredDistanceTo(0, 0, 0) <= circumradiusSquared;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tetrahedron that)) return false;
        return isBad == that.isBad && Double.compare(circumradiusSquared, that.circumradiusSquared) == 0 && Objects.equals(a, that.a) && Objects.equals(b, that.b) && Objects.equals(c, that.c) && Objects.equals(d, that.d) && Objects.equals(circumcenter, that.circumcenter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c, d, isBad, circumcenter, circumradiusSquared);
    }

    public Vec3d getA() {
        return a;
    }

    public void setA(Vec3d a) {
        this.a = a;
    }

    public Vec3d getB() {
        return b;
    }

    public void setB(Vec3d b) {
        this.b = b;
    }

    public Vec3d getC() {
        return c;
    }

    public void setC(Vec3d c) {
        this.c = c;
    }

    public Vec3d getD() {
        return d;
    }

    public void setD(Vec3d d) {
        this.d = d;
    }

    public boolean isBad() {
        return isBad;
    }

    public void setBad(boolean bad) {
        isBad = bad;
    }

    public Vec3d getCircumcenter() {
        return circumcenter;
    }

    public void setCircumcenter(Vec3d circumcenter) {
        this.circumcenter = circumcenter;
    }

    public double getCircumradiusSquared() {
        return circumradiusSquared;
    }

    public void setCircumradiusSquared(double circumradiusSquared) {
        this.circumradiusSquared = circumradiusSquared;
    }
}
