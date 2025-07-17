package com.skycatdev.descent.map;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Adapted from https://github.com/vazgriz/DungeonGenerator/blob/master/Assets/Prim.cs
// See src/main/resources/third_party_licenses/vazgriz.txt

public class Prim {
    public static List<Edge> minimumSpanningTree(List<Edge> edges, Vec3d start) {
        Set<Vec3d> open = new HashSet<>();
        Set<Vec3d> closed = new HashSet<>();
        for (Edge edge : edges) {
            open.add(edge.u());
            open.add(edge.v());
        }

        closed.add(start);

        List<Edge> results = new ArrayList<>();

        while (!open.isEmpty()) {
            @Nullable Edge chosen = null;
            double minWeight = Float.POSITIVE_INFINITY;

            for (Edge edge : edges) {
                if (!closed.contains(edge.u()) ^ !closed.contains(edge.v())) {
                    if (edge.getLength() < minWeight) {
                        chosen = edge;
                        minWeight = edge.getLength();
                    }
                }
            }

            if (chosen == null) break;
            results.add(chosen);
            open.remove(chosen.u());
            open.remove(chosen.v());
            closed.add(chosen.u());
            closed.add(chosen.v());
        }

        return results;
    }
}
