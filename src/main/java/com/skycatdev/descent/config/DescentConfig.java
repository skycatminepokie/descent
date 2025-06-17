package com.skycatdev.descent.config;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.List;

public record DescentConfig(List<Identifier> rooms) {
    public static final MapCodec<DescentConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().fieldOf("rooms").forGetter(DescentConfig::rooms)
    ).apply(instance, DescentConfig::new));
}
