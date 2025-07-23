package com.skycatdev.descent.map;

import org.jetbrains.annotations.Nullable;

import java.util.*;

// Adapted from https://github.com/vazgriz/DungeonGenerator/blob/master/Assets/Prim.cs
// See src/main/resources/third_party_licenses/vazgriz.txt

public class Prim {
    public static Set<Edge> minimumSpanningTree(Iterable<Edge> edges, DungeonPiece.Opening start) {
        Set<DungeonPiece.Opening> open = new HashSet<>();
        Set<DungeonPiece.Opening> closed = new HashSet<>();
        for (Edge edge : edges) {
            open.add(edge.u());
            open.add(edge.v());
        }

        closed.add(start);

        Set<Edge> results = new HashSet<>();

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
