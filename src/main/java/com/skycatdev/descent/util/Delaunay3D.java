package com.skycatdev.descent.util;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4d;
import org.joml.Vector4d;

import java.util.*;

// Adapted from https://github.com/vazgriz/DungeonGenerator/blob/master/Assets/Scripts3D/Delaunay3D.cs
// See src/main/resources/third_party_licenses/vazgriz.txt
public class Delaunay3D {

    protected List<Vec3d> vertices;
    protected List<Edge> edges;
    protected List<Triangle> triangles;
    protected List<Tetrahedron> tetrahedra;

    public Delaunay3D(List<Vec3d> vertices) {
        this.vertices = vertices;
        this.edges = new ArrayList<>();
        this.triangles = new ArrayList<>();
        this.tetrahedra = new ArrayList<>();
    }

    public static Delaunay3D triangulate(List<Vec3d> vertices) {
        Delaunay3D delaunay = new Delaunay3D(vertices);
        delaunay.triangulate();
        return delaunay;
    }

    public void triangulate() {
        double maxX, maxY, maxZ;
        double minX = maxX = vertices.getFirst().getX();
        double minY = maxY = vertices.getFirst().getY();
        double minZ = maxZ = vertices.getFirst().getZ();

        for (Vec3d vertex : vertices) {
            if (vertex.getX() < minX) minX = vertex.getX();
            if (vertex.getX() > maxX) maxX = vertex.getX();
            if (vertex.getY() < minY) minY = vertex.getY();
            if (vertex.getY() > maxY) maxY = vertex.getY();
            if (vertex.getZ() < minZ) minZ = vertex.getZ();
            if (vertex.getZ() > maxZ) maxZ = vertex.getZ();
        }

        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;
        double deltaMax = Math.max(dx, Math.max(dy, dz));

        Vec3d p1 = new Vec3d(minX - 1, minY - 1, minZ - 1);
        Vec3d p2 = new Vec3d(maxX + deltaMax, minY - 1, minZ - 1);
        Vec3d p3 = new Vec3d(minX - 1, maxY + deltaMax, minZ -1);
        Vec3d p4 = new Vec3d(minX - 1, minY - 1, maxZ + deltaMax);

        tetrahedra.add(new Tetrahedron(p1, p2, p3, p4));

        for (Vec3d vertex : vertices) {
            List<Triangle> triangles = new LinkedList<>();

            for (Tetrahedron t : tetrahedra) {
                if (t.circumsphereContains(vertex)) {
                    t.setBad(true);
                    triangles.add(new Triangle(t.a, t.b, t.c));
                    triangles.add(new Triangle(t.a, t.b, t.d));
                    triangles.add(new Triangle(t.a, t.c, t.d));
                    triangles.add(new Triangle(t.b, t.c, t.d));
                }
            }

            // Compare each triangle to every other triangle. If they're too similar, mark them as bad.
            for (int i = 0; i < triangles.size(); i++) {
                for (int j = i + 1; j < triangles.size(); j++) {
                    if (Triangle.almostEqual(triangles.get(i), triangles.get(j))) {
                        triangles.get(i).setBad(true);
                        triangles.get(j).setBad(true);
                    }
                }
            }

            tetrahedra.removeIf(Tetrahedron::isBad);
            triangles.removeIf(Triangle::isBad);

            for (Triangle triangle : triangles) {
                tetrahedra.add(new Tetrahedron(triangle.getU(), triangle.getV(), triangle.getW(), vertex));
            }
        }

        tetrahedra.removeIf(t -> t.containsVertex(p1) ||
                                 t.containsVertex(p2) ||
                                 t.containsVertex(p3) ||
                                 t.containsVertex(p4));

        HashSet<Triangle> triangleSet = new HashSet<>();
        HashSet<Edge> edgeSet = new HashSet<>();

        for (Tetrahedron t : tetrahedra) {
            Triangle abc = new Triangle(t.a, t.b, t.c);
            Triangle abd = new Triangle(t.a, t.b, t.d);
            Triangle acd = new Triangle(t.a, t.c, t.d);
            Triangle bcd = new Triangle(t.b, t.c, t.d);

            if (triangleSet.add(abc)) {
                triangles.add(abc);
            }

            if (triangleSet.add(abd)) {
                triangles.add(abd);
            }

            if (triangleSet.add(acd)) {
                triangles.add(acd);
            }

            if (triangleSet.add(bcd)) {
                triangles.add(bcd);
            }

            Edge ab = new Edge(t.a, t.b);
            Edge bc = new Edge(t.b, t.c);
            Edge ca = new Edge(t.c, t.a);
            Edge da = new Edge(t.d, t.a);
            Edge db = new Edge(t.d, t.b);
            Edge dc = new Edge(t.d, t.c);

            if (edgeSet.add(ab)) {
                edges.add(ab);
            }

            if (edgeSet.add(bc)) {
                edges.add(bc);
            }

            if (edgeSet.add(ca)) {
                edges.add(ca);
            }

            if (edgeSet.add(da)) {
                edges.add(da);
            }

            if (edgeSet.add(db)) {
                edges.add(db);
            }

            if (edgeSet.add(dc)) {
                edges.add(dc);
            }
        }
    }

    public List<Vec3d> getVertices() {
        return vertices;
    }

    public void setVertices(List<Vec3d> vertices) {
        this.vertices = vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public List<Triangle> getTriangles() {
        return triangles;
    }

    public void setTriangles(List<Triangle> triangles) {
        this.triangles = triangles;
    }

    public List<Tetrahedron> getTetrahedra() {
        return tetrahedra;
    }

    public void setTetrahedra(List<Tetrahedron> tetrahedra) {
        this.tetrahedra = tetrahedra;
    }

    public static class Tetrahedron {
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
            return almostEqual(point, a) ||
                   almostEqual(point, b) ||
                   almostEqual(point, c) ||
                   almostEqual(point, d);
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

    public static class Triangle {
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
            boolean vv = Delaunay3D.almostEqual(t1.v , t2.v);
            boolean ww = Delaunay3D.almostEqual(t1.w , t2.w);
            if (uu && vv && ww) return true;
            boolean vw = Delaunay3D.almostEqual(t1.v , t2.w);
            boolean wv = Delaunay3D.almostEqual(t1.w , t2.v);
            if (uu && vw && wv) return true;
            boolean uv = Delaunay3D.almostEqual(t1.u , t2.v);
            boolean wu = Delaunay3D.almostEqual(t1.w , t2.u);
            if (uv && vw && wu) return true;
            boolean vu = Delaunay3D.almostEqual(t1.v , t2.u);
            if (uv && vu && ww) return true;
            boolean uw = Delaunay3D.almostEqual(t1.u , t2.w);
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

    public static class Edge {
        protected Vec3d u;
        protected Vec3d v;

        public Edge(Vec3d u, Vec3d v) {
            this.u = u;
            this.v = v;
        }

        public Vec3d getU() {
            return u;
        }

        public void setU(Vec3d u) {
            this.u = u;
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

    public static boolean almostEqual(Vec3d vec1, Vec3d vec2) {
        return vec1.subtract(vec2).squaredDistanceTo(0, 0, 0) < 0.01;
    }
}
