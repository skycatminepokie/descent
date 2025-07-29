package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Delaunay3DTest {
    private static final Comparator<Vec3d> VEC3D_SORTER = Comparator.comparingDouble(Vec3d::getX)
            .thenComparingDouble(Vec3d::getY)
            .thenComparingDouble(Vec3d::getZ);
    private static final Comparator<Edge> EDGE_SORTER = Comparator.comparing(Edge::u, VEC3D_SORTER)
            .thenComparing(Edge::v, VEC3D_SORTER)
            .thenComparingDouble(Edge::length);


    @Test
    void testTriangulate0() {
        Assertions.assertTrue(Delaunay3D.triangulate(List.of()).isEmpty());
    }

    @Test
    void testTriangulate1() {
        Assertions.assertTrue(Delaunay3D.triangulate(List.of(new Vec3d(0,0,0))).isEmpty());
    }

    @Test
    void testTriangulate2() {
        Vec3d a = new Vec3d(0, 1, 2);
        Vec3d b = new Vec3d(3, 4, 5);
        Set<Edge> result = Delaunay3D.triangulate(List.of(a, b));
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.stream().anyMatch(edge -> edge.hasBoth(a, b)));
    }

    @Test
    void testTriangulate3() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(-1, -1, -1);
        Vec3d c = new Vec3d(0, -2, 0);
        Set<Edge> result = Delaunay3D.triangulate(List.of(a, b, c));
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.stream().anyMatch(edge -> edge.hasBoth(a, b)));
        Assertions.assertTrue(result.stream().anyMatch(edge -> edge.hasBoth(b, c)));
        Assertions.assertTrue(result.stream().anyMatch(edge -> edge.hasBoth(c, a)));
    }

    @Test
    void testTriangulate4() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 0, 2);
        Vec3d c = new Vec3d(0, 1, 0);
        Vec3d d = new Vec3d(2, 0, 0);
        Set<Edge> expected = Set.of(new Edge(a, b),
                new Edge(a, c),
                new Edge(a, d),
                new Edge(b, c),
                new Edge(b, d),
                new Edge(c, d));
        Set<Edge> actual = Delaunay3D.triangulate(List.of(a, b, c, d));
        Assertions.assertEquals(expected, actual, () -> "Failed. Expected: \n" + expected.stream().sorted(EDGE_SORTER).map(Edge::toString).collect(Collectors.joining("\n")) +
                                                    "\nGot:\n" +
                                                    actual.stream().sorted(EDGE_SORTER).map(Edge::toString).collect(Collectors.joining("\n")) +
                                                    '\n');
    }

    @Test
    void testTriangulate5() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 0, 2);
        Vec3d c = new Vec3d(0, 1, 0);
        Vec3d d = new Vec3d(2, 0, 0);
        Vec3d e = new Vec3d(1, 1, 1); // Point inside the bounding tetrahedron

        Set<Edge> expected = Set.of(
                // Original tetrahedron
                new Edge(a, b), new Edge(a, c), new Edge(a, d),
                new Edge(b, c), new Edge(b, d), new Edge(c, d),

                // New edges introduced by point e
                new Edge(a, e), new Edge(b, e), new Edge(c, e), new Edge(d, e)
        );

        List<Vec3d> input = List.of(a, b, c, d, e);
        Set<Edge> actual = Delaunay3D.triangulate(input);

        Assertions.assertEquals(expected, actual, () ->
                "Failed. Expected:\n" +
                expected.stream().sorted(EDGE_SORTER).map(Edge::toString).collect(Collectors.joining("\n")) +
                "\nGot:\n" +
                actual.stream().sorted(EDGE_SORTER).map(Edge::toString).collect(Collectors.joining("\n")) +
                '\n'
        );
    }

}
