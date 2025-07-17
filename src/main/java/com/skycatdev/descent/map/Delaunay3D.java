package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// Adapted from https://github.com/vazgriz/DungeonGenerator/blob/master/Assets/Scripts3D/Delaunay3D.cs
// See src/main/resources/third_party_licenses/zeni.txt and src/main/resources/third_party_licenses/vazgriz.txt
public class Delaunay3D {

    private static Set<Edge> triangulate(List<Vec3d> vertices) {
        // General idea: Make one big tetrahedron that encompasses it all
        // For each vertex, find all tetrahedra that contain it. Break those down into triangles.
        // Then, construct tetrahedra from those triangles and the vertex - we just split the big tetrahedron into four
        // smaller tetrahedra. Now we're left with a ton of tetrahedra, none of which contain one of the vertices.
        // Get rid of the tetrahedra that have the vertices of the big tetrahedron - now we don't have those points in
        // our graph. Finally, return all the unique edges.


        // Find bounds
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
        double deltaMax = Math.max(dx, Math.max(dy, dz)); // Biggest side of bounds

        // Pretty sure this is the tetrahedron that circumscribes the whole thing
        Vec3d p1 = new Vec3d(minX - 1, minY - 1, minZ - 1);
        Vec3d p2 = new Vec3d(maxX + deltaMax, minY - 1, minZ - 1);
        Vec3d p3 = new Vec3d(minX - 1, maxY + deltaMax, minZ -1);
        Vec3d p4 = new Vec3d(minX - 1, minY - 1, maxZ + deltaMax);

        List<Tetrahedron> tetrahedra = new LinkedList<>();
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

            // Compare each triangle to every other triangle. If they're too similar, get rid of them.
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

        HashSet<Edge> edgeSet = new HashSet<>();

        for (Tetrahedron t : tetrahedra) {
            edgeSet.add(new Edge(t.a, t.b));
            edgeSet.add(new Edge(t.b, t.c));
            edgeSet.add(new Edge(t.c, t.a));
            edgeSet.add(new Edge(t.d, t.a));
            edgeSet.add(new Edge(t.d, t.b));
            edgeSet.add(new Edge(t.d, t.c));
        }
        return edgeSet;
    }

    public static boolean almostEqual(Vec3d vec1, Vec3d vec2) {
        return vec1.subtract(vec2).squaredDistanceTo(0, 0, 0) < 0.01;
    }
}
