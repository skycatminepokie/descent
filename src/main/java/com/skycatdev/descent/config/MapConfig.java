package com.skycatdev.descent.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Pool.createNonEmptyCodec(Identifier.CODEC).fieldOf("starts").forGetter(MapConfig::starts),
            Pool.createNonEmptyCodec(Identifier.CODEC).fieldOf("ends").forGetter(MapConfig::ends),
            Identifier.CODEC.listOf().fieldOf("paths").forGetter(MapConfig::paths),
            Pool.createNonEmptyCodec(Identifier.CODEC).fieldOf("rooms").forGetter(MapConfig::rooms),
            Codec.INT.fieldOf("numberOfRooms").forGetter(MapConfig::numberOfRooms),
            Codec.INT.fieldOf("minSeparationX").forGetter(MapConfig::minSeparationX),
            Codec.INT.fieldOf("minSeparationY").forGetter(MapConfig::minSeparationY),
            Codec.INT.fieldOf("minSeparationZ").forGetter(MapConfig::minSeparationZ)
    ).apply(instance, MapConfig::new));

    public MapConfig {
        //noinspection ConstantValue
        if (numberOfRooms < 2) {
            throw new IllegalArgumentException("numberOfRooms must be >= 2 (it needs a start and end)");
        }
        //noinspection ConstantValue
        if (minSeparationX < 0 || minSeparationY < 0 || minSeparationZ < 0) {
            throw new IllegalArgumentException("Minimum separation values must be >= 0");
        }
        // We want immutable things
        paths = List.copyOf(paths);
    }
}
