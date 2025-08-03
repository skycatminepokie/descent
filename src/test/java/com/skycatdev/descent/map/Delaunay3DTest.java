package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import com.skycatdev.descent.utils.Utils;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class Delaunay3DTest {
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testIsDelaunayEmpty() {
        assertThat(Delaunay3D.isDelaunay(Set.of(), Set.of()))
                .isTrue();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testIsDelaunayOne() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(1, 1, 1);
        Vec3d c = new Vec3d(0, 1, 0);
        Vec3d d = new Vec3d(0, 1, 1);
        assertThat(Delaunay3D.isDelaunay(Set.of(new Tetrahedron(a, b, c, d)), Set.of(a, b, c, d)))
                .isTrue();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testIsDelaunayMissingPoint() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(1, 1, 1);
        Vec3d c = new Vec3d(0, 1, 0);
        Vec3d d = new Vec3d(0, 1, 1);
        Vec3d e = new Vec3d(100, 100, 100);
        assertThat(Delaunay3D.isDelaunay(Set.of(new Tetrahedron(a, b, c, d)), Set.of(a, b, c, d, e)))
                .isFalse();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testIsDelaunayOverlaps() {
        Vec3d a = new Vec3d(0, 0, 1);
        Vec3d b = new Vec3d(0, 0, -1);
        Vec3d c = new Vec3d(0, 1, 0);
        Vec3d d = new Vec3d(1, 0, 0);
        Vec3d e = new Vec3d(0, 0, 0);
        assertThat(Delaunay3D.isDelaunay(Set.of(new Tetrahedron(a, b, c, d), new Tetrahedron(a, b, c, e)), Set.of(a, b, c, d, e)))
                .isFalse();
    }


    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testTriangulate0() {
        assertThat(Delaunay3D.triangulate(List.of()))
                .hasSize(0);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testTriangulate1() {
        assertThat(Delaunay3D.triangulate(List.of(new Vec3d(0,0,0))))
                .hasSize(0);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testTriangulate2() {
        Vec3d a = new Vec3d(0, 1, 2);
        Vec3d b = new Vec3d(3, 4, 5);
        Set<Edge> actual = Delaunay3D.triangulate(List.of(a, b));
        Assertions.assertEquals(1, actual.size());
        assertThat(actual)
                .containsOnly(new Edge(a, b));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testTriangulate3() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(-1, -1, -1);
        Vec3d c = new Vec3d(0, -2, 0);
        Set<Edge> result = Delaunay3D.triangulate(List.of(a, b, c));
        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains(new Edge(a, b)));
        Assertions.assertTrue(result.contains(new Edge(b, c)));
        Assertions.assertTrue(result.contains(new Edge(c, a)));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
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
        assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testTetrahedralize5() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 0, 2);
        Vec3d c = new Vec3d(0, 1, 0);
        Vec3d d = new Vec3d(2, 0, 0);
        Vec3d e = new Vec3d(1, 1, 1); // Point inside the bounding tetrahedron

        List<Vec3d> input = List.of(a, b, c, d, e);
        Tetrahedron superTetrahedron = Delaunay3D.createSuperTetrahedron(input);
        List<Tetrahedron> actual = Delaunay3D.tetrahedralize(input, superTetrahedron);

        Descent.LOGGER.info("Edge dump: \n{}", Utils.makeEdgeDump(Delaunay3D.uniqueEdgesOf(actual)));

        List<Vec3d> finalGraph = List.of(a, b, c, d, e, superTetrahedron.a(), superTetrahedron.b(), superTetrahedron.c(), superTetrahedron.d());
        assertThat(Delaunay3D.isDelaunay(actual, finalGraph))
                .isTrue();
    }

//    @Test
//    @Execution(ExecutionMode.CONCURRENT)
//    void testTriangulate6Strange() {
//        Vec3d point0 = new Vec3d(-2,-1,-3);
//        Vec3d point1 = new Vec3d(7,4,-5);
//        Vec3d point2 = new Vec3d(4,7,0);
//        Vec3d point3 = new Vec3d(-4,-5,3);
//        Vec3d point4 = new Vec3d(4,-2,6);
//        Vec3d point5 = new Vec3d(-3,4,-4);
//        List<Vec3d> points = List.of(point0, point1, point2, point3, point4, point5);
//        Set<Edge> expected = Set.of();
//
//        assertThat(Delaunay3D.tetrahedralize(points))
//                .satisfies()
//    }
}
