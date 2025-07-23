package com.skycatdev.descent.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;

public class Utils {
    public static Vec3i copySign(Vec3i magnitude, Vec3d sign) {
        return new Vec3i(
                sign.getX() > 0 ? Math.abs(magnitude.getX()) : -Math.abs(magnitude.getX()),
                sign.getY() > 0 ? Math.abs(magnitude.getY()) : -Math.abs(magnitude.getY()),
                sign.getZ() > 0 ? Math.abs(magnitude.getZ()) : -Math.abs(magnitude.getZ())
        );
    }

    @Contract("_,_,_->param1")
    public static BlockPos.Mutable forceOutside(BlockPos.Mutable pos, BlockPos min, BlockPos max) {
        pos.set(forceOutside(pos.getX(), min.getX(), max.getX()),
                forceOutside(pos.getY(), min.getY(), max.getY()),
                forceOutside(pos.getZ(), min.getZ(), max.getZ()));
        return pos;
    }

    /**
     *
     * @param value The value to force outside the interval
     * @param lower The lower end of the interval to exclude (exclusive)
     * @param upper The upper end of the interval to exclude (exclusive)
     * @return {@code value} if it is not between {@code lower} and {@code upper} (exclusive). Otherwise, whichever bound is closer.
     */
    public static int forceOutside(int value, int lower, int upper) {
        if (value > lower && value < upper) {
            if (value - lower > upper - value) {
                return upper;
            }
            return lower;
        }
        return value;
    }

    public static Vec3d maxComponentsByAbs(Vec3d a, Vec3d b) {
        return new Vec3d(
                Math.abs(a.getX()) > Math.abs(b.getX()) ? a.getX() : b.getX(), // if (abs(a.x) > abs(b.x)) { x = a.x } else { x = b.x }
                Math.abs(a.getY()) > Math.abs(b.getY()) ? a.getY() : b.getY(),
                Math.abs(a.getZ()) > Math.abs(b.getZ()) ? a.getZ() : b.getZ()
        );
    }
}
