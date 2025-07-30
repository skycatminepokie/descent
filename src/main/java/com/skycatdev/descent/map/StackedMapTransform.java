package com.skycatdev.descent.map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StackedMapTransform implements MapTransform {
    protected final List<MapTransform> transforms;

    public StackedMapTransform(List<MapTransform> transforms) {
        // Make sure it's immutable
        this.transforms = List.copyOf(transforms);
    }

    public StackedMapTransform() {
        this.transforms = List.of();
    }

    public StackedMapTransform copyWith(MapTransform transform) {
        List<MapTransform> transformsCopy = new ArrayList<>(transforms);
        transformsCopy.add(transform);
        return new StackedMapTransform(transformsCopy);
    }

    @Override
    public BlockPos.Mutable transformPoint(BlockPos.Mutable mutablePos) {
        for (MapTransform transform : transforms) {
            transform.transformPoint(mutablePos);
        }

        return mutablePos;
    }

    @Override
    public Vec3d transformedPoint(Vec3d pos) {
        Vec3d ret = pos;
        for (MapTransform transform : transforms) {
            ret = transform.transformedPoint(ret);
        }

        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StackedMapTransform that)) return false;
        return Objects.equals(transforms, that.transforms);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(transforms);
    }
}
