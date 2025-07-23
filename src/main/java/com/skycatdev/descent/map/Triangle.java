package com.skycatdev.descent.map;

import java.util.Objects;

public class Triangle {
    protected DungeonPiece.Opening u;
    protected DungeonPiece.Opening v;
    protected DungeonPiece.Opening w;
    protected boolean isBad; // TODO: get rid of this, this is stinky :(

    public Triangle(DungeonPiece.Opening u, DungeonPiece.Opening v, DungeonPiece.Opening w) {
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public static boolean congruent(Triangle t1, Triangle t2) {
        boolean uu = t1.getU().equals(t2.getU());
        boolean vv = t1.v.equals(t2.v);
        boolean ww = t1.w.equals(t2.w);
        if (uu && vv && ww) return true;
        boolean vw = t1.v.equals(t2.w);
        boolean wv = t1.w.equals(t2.v);
        if (uu && vw && wv) return true;
        boolean uv = t1.u.equals(t2.v);
        boolean wu = t1.w.equals(t2.u);
        if (uv && vw && wu) return true;
        boolean vu = t1.v.equals(t2.u);
        if (uv && vu && ww) return true;
        boolean uw = t1.u.equals(t2.w);
        if (uw && vu && wv) return true;
        return uw && vv && wu;
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

    public DungeonPiece.Opening getU() {
        return u;
    }

    public void setU(DungeonPiece.Opening u) {
        this.u = u;
    }

    public DungeonPiece.Opening getV() {
        return v;
    }

    public void setV(DungeonPiece.Opening v) {
        this.v = v;
    }

    public DungeonPiece.Opening getW() {
        return w;
    }

    public void setW(DungeonPiece.Opening w) {
        this.w = w;
    }

    public boolean isBad() {
        return isBad;
    }

    public void setBad(boolean bad) {
        isBad = bad;
    }
}
