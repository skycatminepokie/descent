package com.skycatdev.descent.config;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DescentConfig(MapConfig mapConfig) {
    public static final MapCodec<DescentConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MapConfig.CODEC.fieldOf("mapConfig").forGetter(DescentConfig::mapConfig)
    ).apply(instance, DescentConfig::new));
}
