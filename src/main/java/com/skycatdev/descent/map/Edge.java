package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;

public record Edge(Vec3d u, Vec3d v, double length) {

    public Edge(Vec3d u, Vec3d v) {
        this(u, v, u.subtract(v).length());
    }

    public double getLength() {
        return length;
    }

    public static boolean almostEqual(Edge a, Edge b) {
        return (Delaunay3D.almostEqual(a.u(), b.u()) && Delaunay3D.almostEqual(a.v(), b.v())) ||
               (Delaunay3D.almostEqual(a.u(), b.v()) && Delaunay3D.almostEqual(a.v(), b.u()));
    }

    public boolean has(Vec3d vec) {
        return u.equals(vec) || v.equals(vec);
    }

    public boolean hasAny(Vec3d... vecs) {
        for (Vec3d vec : vecs) {
            if (u.equals(vec) || v.equals(vec)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEquivalent(Edge other) {
        return this.equals(other) ||
               (other.u().equals(v()) &&
                other.v().equals(u()) &&
                other.length() == length());
    }

    public boolean hasBoth(Vec3d u, Vec3d v) {
        return (u.equals(u()) && v.equals(v())) ||
               (v.equals(u()) && u.equals(v()));
    }
}
