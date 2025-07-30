package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PrimTest {
    @Test
    void testMst1() {
        Vec3d point = new Vec3d(0,0,0);
        Set<Edge> result = Prim.minimumSpanningTree(Set.of(), point);
        assertThat(result.isEmpty())
                .isTrue();
    }

    @Test
    void testMst2() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 0, 1);
        Edge edge = new Edge(a, b);
        Set<Edge> input = Set.of(edge);
        assertThat(Prim.minimumSpanningTree(input, a))
                .containsOnly(edge);
        assertThat(Prim.minimumSpanningTree(input, b))
                .containsOnly(edge);
    }

    @Test
    void testMst3() {
        Vec3d a = new Vec3d(0, 0, 0);
        Vec3d b = new Vec3d(0, 1, 2);
        Vec3d c = new Vec3d(0, 1, 3);
        Set<Edge> expected = Set.of(new Edge(a, b), new Edge(b, c));
        assertThat(Prim.minimumSpanningTree(expected, a))
                .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(Prim.minimumSpanningTree(expected, b))
                .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(Prim.minimumSpanningTree(expected, c))
                .containsExactlyInAnyOrderElementsOf(expected);
    }
}
