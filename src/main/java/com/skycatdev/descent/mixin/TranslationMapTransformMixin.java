package com.skycatdev.descent.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.Objects;

@Mixin(targets = "xyz.nucleoid.map_templates.MapTransform$1", remap = false)
public abstract class TranslationMapTransformMixin {
    @Shadow @Final int val$x;

    @Shadow @Final int val$y;

    @Shadow @Final
    int val$z;

    @Override
    public int hashCode() {
        return Objects.hash(val$x, val$y, val$z);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapTransform that)) return false;
        TranslationMapTransformAccess reallyThat = (TranslationMapTransformAccess) that;
        return val$x == reallyThat.x() && val$y == reallyThat.y() && val$z == reallyThat.z();
    }
}
