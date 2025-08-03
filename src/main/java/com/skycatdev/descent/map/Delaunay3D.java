package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;

import java.util.*;

// Adapted from https://github.com/vazgriz/DungeonGenerator/blob/master/Assets/Scripts3D/Delaunay3D.cs
// See src/main/resources/third_party_licenses/zeni.txt and src/main/resources/third_party_licenses/vazgriz.txt
public class Delaunay3D {

    /*
     This *might* be optimized further by making tetrahedra out of triangles and triangles out of edges.
     It would mean redundant checks for Tetrahedron's hasVertex (and then Triangle's hasVertex) unless something
     clever was done, but much less object instantiation. Not quite clever enough is checking all the vertices of all
     but one of the triangles - that's still 9 checks instead of 4, assuming the triangle was clever enough to check
     only 3.
    */
    public static Set<Edge> triangulate(List<Vec3d> vertices) {
        /*
         General idea: Make one big tetrahedron that encompasses it all.
         For each vertex, find all tetrahedra that contain it. Break those down into triangles.
         Then, construct tetrahedra from those triangles and the vertex - we just split the big tetrahedron into a ton
         of tetrahedra, none of which contain one of the vertices.
         Get rid of the *EDGES* (not tetrahedra) that have the vertices of the big tetrahedron - now we don't have those points in
         our graph. Finally, return all the unique edges.
        */

        if (vertices.isEmpty()) {
            return Set.of();
        }

        if (vertices.size() == 2) {
            return Set.of(new Edge(vertices.getFirst(), vertices.getLast()));
        }

        if (vertices.size() == 3) {
            return Set.of(new Edge(vertices.getFirst(), vertices.get(1)),
                    new Edge(vertices.get(1), vertices.getLast()),
                    new Edge(vertices.getLast(), vertices.getFirst()));
        }

        Tetrahedron enclosing = createSuperTetrahedron(vertices);

        HashSet<Edge> edges = uniqueEdgesOf(tetrahedralize(vertices, enclosing));
        edges.removeIf(edge -> edge.hasAny(enclosing.a(), enclosing.b(), enclosing.c(), enclosing.d()));
        return edges;
    }

    /**
     * Create a "super tetrahedron" that encloses all the given points
     */
    public static Tetrahedron createSuperTetrahedron(List<Vec3d> points) {
        // Find bounds
        double minX, minY, minZ;
        minX = minY = minZ = Double.MAX_VALUE;
        double maxX, maxY, maxZ;
        maxX = maxY = maxZ = Double.MIN_VALUE;

        for (Vec3d point : points) {
            if (point.getX() < minX) minX = point.getX();
            if (point.getX() > maxX) maxX = point.getX();
            if (point.getY() < minY) minY = point.getY();
            if (point.getY() > maxY) maxY = point.getY();
            if (point.getZ() < minZ) minZ = point.getZ();
            if (point.getZ() > maxZ) maxZ = point.getZ();
        }

        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;
        double deltaMax = Math.max(dx, Math.max(dy, dz)); // Biggest side of bounds TODO: need *2?

        return new Tetrahedron(new Vec3d(minX - deltaMax, minY - deltaMax, minZ - deltaMax),
                new Vec3d(maxX + deltaMax, minY - deltaMax, minZ - deltaMax),
                new Vec3d(minX - deltaMax, maxY + deltaMax, minZ - deltaMax),
                new Vec3d(minX - deltaMax, minY - deltaMax, maxZ + deltaMax));
    }

    /**
     * Creates a Delaunay tetrahedralization of the given vertices and an enclosing super-tetrahedron.
     * @param enclosing A super-tetrahedron that encloses all the vertices.
     * @return A tetrahedralization of the given vertices and enclosing super-tetrahedron.
     * @implSpec Does not remove tetrahedra that have points of the enclosing Tetrahedron.
     */
    public static List<Tetrahedron> tetrahedralize(Collection<Vec3d> vertices, Tetrahedron enclosing) {

        List<Tetrahedron> tetrahedra = new LinkedList<>();
        tetrahedra.add(enclosing);

        // For each vertex, find all tetrahedra that contain it
        for (Vec3d vertex : vertices) {
            List<Triangle> triangles = new LinkedList<>();

            // Break those down into triangles
            Collection<Tetrahedron> badTetrahedra = new LinkedList<>();
            for (Tetrahedron t : tetrahedra) {
                if (t.circumsphereContains(vertex)) {
                    badTetrahedra.add(t);
                    triangles.add(new Triangle(t.a(), t.b(), t.c()));
                    triangles.add(new Triangle(t.a(), t.b(), t.d()));
                    triangles.add(new Triangle(t.a(), t.c(), t.d()));
                    triangles.add(new Triangle(t.b(), t.c(), t.d()));
                }
            }
            tetrahedra.removeAll(badTetrahedra);

            // Compare each triangle to every other triangle. If they're too similar, get rid of them.
            for (int i = 0; i < triangles.size(); i++) {
                for (int j = i + 1; j < triangles.size(); j++) {
                    if (Triangle.almostEqual(triangles.get(i), triangles.get(j))) {
                        triangles.get(i).setBad(true);
                        triangles.get(j).setBad(true);
                    }
                }
            }

            triangles.removeIf(Triangle::isBad);

            // Construct tetrahedra from those triangles and the vertex (we just split the tetrahedra into many smaller ones)
            for (Triangle triangle : triangles) {
                tetrahedra.add(new Tetrahedron(triangle.getU(), triangle.getV(), triangle.getW(), vertex));
            }
        }

        return tetrahedra;
    }

    /**
     * Find all the unique edges of the given tetrahedra.
     */
    public static HashSet<Edge> uniqueEdgesOf(Collection<Tetrahedron> tetrahedra) {
        HashSet<Edge> edgeSet = new HashSet<>();

        for (Tetrahedron t : tetrahedra) {
            edgeSet.add(new Edge(t.a(), t.b()));
            edgeSet.add(new Edge(t.b(), t.c()));
            edgeSet.add(new Edge(t.c(), t.a()));
            edgeSet.add(new Edge(t.d(), t.a()));
            edgeSet.add(new Edge(t.d(), t.b()));
            edgeSet.add(new Edge(t.d(), t.c()));
        }
        return edgeSet;
    }

    public static boolean almostEqual(Vec3d vec1, Vec3d vec2) {
        return vec1.subtract(vec2).squaredDistanceTo(0, 0, 0) < 0.01;
    }

    /**
     * Checks that a collection of tetrahedra is Delaunay, meaning that it contains all vertices specified
     * and that each tetrahedron does not contain a point that it is not made of.
     */
    public static boolean isDelaunay(Collection<Tetrahedron> tetrahedra, Collection<Vec3d> vertices) {
        Set<Vec3d> unusedVertices = new HashSet<>(vertices);
        Set<Vec3d> usedVertices = new HashSet<>();
        for (Tetrahedron t : tetrahedra) {
            for (Vec3d vertex : vertices) {
                if (t.hasVertex(vertex)) {
                    unusedVertices.remove(vertex);
                    usedVertices.add(vertex);
                    continue;
                }
                if (t.circumsphereContains(vertex)) return false;
            }
        }
        return unusedVertices.isEmpty() && usedVertices.containsAll(vertices);
    }
}
