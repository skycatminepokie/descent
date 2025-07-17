package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Edge {
    protected Vec3d u;
    protected Vec3d v;
    protected double length;

    public Edge(Vec3d u, Vec3d v) {
        this.u = u;
        this.v = v;
        length = u.subtract(v).length();
    }

    public Vec3d getU() {
        return u;
    }

    public void setU(Vec3d u) {
        this.u = u;
    }

    public double getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Edge edge)) return false;
        return Objects.equals(u, edge.u) && Objects.equals(v, edge.v);
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v);
    }

    public Vec3d getV() {
        return v;
    }

    public void setV(Vec3d v) {
        this.v = v;
    }

    public static boolean almostEqual(Edge a, Edge b) {
        return (Delaunay3D.almostEqual(a.getU(), b.getU()) && Delaunay3D.almostEqual(a.getV(), b.getV())) ||
               (Delaunay3D.almostEqual(a.getU(), b.getV()) && Delaunay3D.almostEqual(a.getV(), b.getU()));
    }
}
