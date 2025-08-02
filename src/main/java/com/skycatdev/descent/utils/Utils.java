package com.skycatdev.descent.utils;

import com.skycatdev.descent.map.Edge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Contract;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

    public static String makeEdgeDump(Iterable<Edge> edges) {
        return makeEdgeDump(edges.iterator());
    }

    protected static String makeEdgeDump(Iterator<Edge> edges) {
        HashMap<Vec3d, Integer> points = new HashMap<>();
        StringBuilder pointsStr = new StringBuilder();
        StringBuilder edgeStr = new StringBuilder();
        int p = 0;
        int e = 0;
        while (edges.hasNext()) {
            Edge edge = edges.next();
            if (!points.containsKey(edge.u())) {
                points.put(edge.u(), p);
                pointsStr.append("p_{")
                        .append(p)
                        .append("}=")
                        .append(edge.u())
                        .append('\n');
                p++;
            }
            if (!points.containsKey(edge.v())) {
                points.put(edge.v(), p);
                pointsStr.append("p_{")
                        .append(p)
                        .append("}=")
                        .append(edge.v())
                        .append('\n');
                p++;
            }
            edgeStr.append("e_{")
                    .append(e)
                    .append("}=")
                    .append("p_{")
                    .append(points.get(edge.u()))
                    .append("}p_{")
                    .append(points.get(edge.v()))
                    .append("}\n");
        }
        return pointsStr.append(edgeStr)
                .deleteCharAt(pointsStr.length() - 1)
                .toString();
    }

    public static String makePointDump(Iterable<Vec3d> points) {
        return makePointDump(points.iterator());
    }

    public static String makePointDump(Iterator<Vec3d> points) {
        int i = 0;
        StringBuilder pointsStr = new StringBuilder();
        StringBuilder line = new StringBuilder("l=");

        while (points.hasNext()) {
            Vec3d center = points.next();
            pointsStr.append(String.format("p_{%d}=(%d,%d,%d)\n", i, (int) Math.ceil(center.getX()), (int) Math.ceil(center.getY()), (int) Math.ceil(center.getZ())));
            line.append("p_{");
            line.append(i);
            line.append("},");
            i++;
        }
        line.deleteCharAt(line.length() - 1);
        return String.format("%s%s", pointsStr, line);
    }

    public static Vec3d maxComponentsByAbs(Vec3d a, Vec3d b) {
        return new Vec3d(
                Math.abs(a.getX()) > Math.abs(b.getX()) ? a.getX() : b.getX(), // if (abs(a.x) > abs(b.x)) { x = a.x } else { x = b.x }
                Math.abs(a.getY()) > Math.abs(b.getY()) ? a.getY() : b.getY(),
                Math.abs(a.getZ()) > Math.abs(b.getZ()) ? a.getZ() : b.getZ()
        );
    }

    public static <T> T randomFromList(List<T> ts, Random random) {
        if (ts.isEmpty()) {
            throw new IllegalArgumentException("List must not be empty");
        }
        return ts.get(random.nextInt(ts.size()));
    }
}
