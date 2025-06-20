package com.skycatdev.descent.util;

import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Objects;

public class Triangle {
    protected Vec3d u;
    protected Vec3d v;
    protected Vec3d w;
    protected boolean isBad;

    public Triangle(Vec3d u, Vec3d v, Vec3d w) {
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public static boolean almostEqual(Triangle t1, Triangle t2) {
        boolean uu = Delaunay3D.almostEqual(t1.u, t2.u);
        boolean vv = Delaunay3D.almostEqual(t1.v, t2.v);
        boolean ww = Delaunay3D.almostEqual(t1.w, t2.w);
        if (uu && vv && ww) return true;
        boolean vw = Delaunay3D.almostEqual(t1.v, t2.w);
        boolean wv = Delaunay3D.almostEqual(t1.w, t2.v);
        if (uu && vw && wv) return true;
        boolean uv = Delaunay3D.almostEqual(t1.u, t2.v);
        boolean wu = Delaunay3D.almostEqual(t1.w, t2.u);
        if (uv && vw && wu) return true;
        boolean vu = Delaunay3D.almostEqual(t1.v, t2.u);
        if (uv && vu && ww) return true;
        boolean uw = Delaunay3D.almostEqual(t1.u, t2.w);
        if (uw && vu && wv) return true;
        return uw && vv && wu;
    }

    private static boolean almostEqualOrdered(List<Vec3d> t1, List<Vec3d> t2) {
        return Delaunay3D.almostEqual(t1.get(0), t2.get(0)) &&
               Delaunay3D.almostEqual(t1.get(1), t2.get(1)) &&
               Delaunay3D.almostEqual(t1.get(2), t2.get(2));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triangle triangle)) return false;
        return isBad == triangle.isBad && Objects.equals(u, triangle.u) && Objects.equals(v, triangle.v) && Objects.equals(w, triangle.w);
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v, w, isBad);
    }

    public Vec3d getU() {
        return u;
    }

    public void setU(Vec3d u) {
        this.u = u;
    }

    public Vec3d getV() {
        return v;
    }

    public void setV(Vec3d v) {
        this.v = v;
    }

    public Vec3d getW() {
        return w;
    }

    public void setW(Vec3d w) {
        this.w = w;
    }

    public boolean isBad() {
        return isBad;
    }

    public void setBad(boolean bad) {
        isBad = bad;
    }
}
