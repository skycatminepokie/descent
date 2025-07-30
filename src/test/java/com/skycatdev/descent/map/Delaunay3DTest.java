package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class Delaunay3DTest {

    @Test
    void testTriangulate0() {
        assertThat(Delaunay3D.triangulate(List.of()))
                .hasSize(0);
    }

    @Test
    void testTriangulate1() {
        assertThat(Delaunay3D.triangulate(List.of(new Vec3d(0,0,0))))
                .hasSize(0);
    }

    @Test
    void testTriangulate2() {
        Vec3d a = new Vec3d(0, 1, 2);
        Vec3d b = new Vec3d(3, 4, 5);
        Set<Edge> result = Delaunay3D.triangulate(List.of(a, b));
        Assertions.assertEquals(1, result.size());
        assertThat(result)
                .containsOnly(new Edge(a, b));
    }

    @Test
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
        assertThat(expected)
                .isEqualTo(actual);
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

        assertThat(expected)
                .isEqualTo(actual);
    }

}
