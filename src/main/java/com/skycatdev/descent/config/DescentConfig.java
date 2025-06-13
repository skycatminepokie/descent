package com.skycatdev.descent.config;

import com.mojang.serialization.MapCodec;

public record DescentConfig() {
    public static final MapCodec<DescentConfig> CODEC = MapCodec.unit(DescentConfig::new);
}
