package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class Delaunay3DTest {
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
        Assertions.assertEquals(Set.of(new Edge(a, b),
                new Edge(a, c),
                new Edge(a, d),
                new Edge(b, c),
                new Edge(b, d),
                new Edge(c, d)), Delaunay3D.triangulate(List.of(a, b, c, d)));
    }
}
