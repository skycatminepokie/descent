package com.skycatdev.descent.config;

import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record MapConfig(Pool<Identifier> starts, Pool<Identifier> ends, @Unmodifiable List<Identifier> paths,
                        Pool<Identifier> rooms, @Range(from = 2, to = Integer.MAX_VALUE) int numberOfRooms,
                        @Range(from = 0, to = Integer.MAX_VALUE) int minSeparationX,
                        @Range(from = 0, to = Integer.MAX_VALUE) int minSeparationY,
                        @Range(from = 0, to = Integer.MAX_VALUE) int minSeparationZ) {
    public MapConfig {
        // We want immutable things
        paths = List.copyOf(paths);
    }
}
