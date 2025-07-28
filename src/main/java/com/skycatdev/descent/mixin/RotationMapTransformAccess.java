package com.skycatdev.descent.mixin;


import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "xyz.nucleoid.map_templates.MapTransform$2")
public interface RotationMapTransformAccess {
    @Accessor(value = "val$pivot")
    BlockPos pivot();

    @Accessor(value = "val$rotation")
    BlockRotation val$rotation();

    @Accessor(value = "val$mirror")
    BlockMirror val$mirror();
}
