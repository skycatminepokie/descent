package com.skycatdev.descent.mixin;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(targets = "xyz.nucleoid.map_templates.MapTransform$2")
public abstract class RotationMapTransformMixin {
    @Shadow @Final BlockPos val$pivot;

    @Shadow @Final BlockRotation val$rotation;

    @Shadow @Final BlockMirror val$mirror;

    @Override
    public int hashCode() {
        return Objects.hash(val$pivot, val$rotation, val$mirror);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RotationMapTransformMixin that)) return false;
        RotationMapTransformAccess reallyThat = (RotationMapTransformAccess) that;
        return Objects.equals(val$pivot, reallyThat.pivot()) && val$rotation == reallyThat.val$rotation() && val$mirror == reallyThat.val$mirror();
    }
}
