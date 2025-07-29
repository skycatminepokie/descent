package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class PrimTest {
    @Test
    void testMst1() {
        Vec3d point = new Vec3d(0,0,0);
        Set<Edge> result = Prim.minimumSpanningTree(Set.of(), point);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testMst2() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 0, 1);
        Set<Edge> expected = Set.of(new Edge(a,b));
        Assertions.assertEquals(expected, Prim.minimumSpanningTree(expected, a));
        Assertions.assertEquals(expected, Prim.minimumSpanningTree(expected, b));
    }

    @Test
    void testMst3() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 1, 2);
        Vec3d c = new Vec3d(0, 1, 3);
        Set<Edge> expected = Set.of(new Edge(a, b), new Edge(b, c));
        Assertions.assertEquals(expected, Prim.minimumSpanningTree(expected, a));
        Assertions.assertEquals(expected, Prim.minimumSpanningTree(expected, b));
        Assertions.assertEquals(expected, Prim.minimumSpanningTree(expected, c));
    }
}
