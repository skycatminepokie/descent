package com.skycatdev.descent.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "xyz.nucleoid.map_templates.MapTransform$1", remap = false)
public interface TranslationMapTransformAccess {
    @Accessor(value = "val$x")
    int x();
    @Accessor(value = "val$y")
    int y();
    @Accessor(value = "val$z")
    int z();

}
