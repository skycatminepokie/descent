package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class Delaunay3DTest {
    @Test
    void testTriangulate0() {
        Assertions.assertThrows(NoSuchElementException.class, () -> Delaunay3D.triangulate(List.of()));
    }

    @Test
    void testTriangulate1() {
        Assertions.assertEquals(0, Delaunay3D.triangulate(List.of(new Vec3d(0,0,0))).size());
    }

    @Test
    void testTriangulate2() {
        Vec3d a = new Vec3d(0, 1, 2);
        Vec3d b = new Vec3d(3, 4, 5);
        Set<Edge> result = Delaunay3D.triangulate(List.of(a, b));
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.stream().anyMatch(edge -> edge.isEquivalent(new Edge(a, b))));
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
}
