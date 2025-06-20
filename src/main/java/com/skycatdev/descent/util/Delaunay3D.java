package com.skycatdev.descent.util;

import net.minecraft.util.math.Vec3d;

import java.util.*;

// Adapted from https://github.com/vazgriz/DungeonGenerator/blob/master/Assets/Scripts3D/Delaunay3D.cs
// See src/main/resources/third_party_licenses/zeni.txt and src/main/resources/third_party_licenses/vazgriz.txt
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

    public static boolean almostEqual(Vec3d vec1, Vec3d vec2) {
        return vec1.subtract(vec2).squaredDistanceTo(0, 0, 0) < 0.01;
    }
}
