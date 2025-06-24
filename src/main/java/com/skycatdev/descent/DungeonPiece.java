package com.skycatdev.descent;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class DungeonPiece {
    /**
     * Bounds, transformed using transform.
     */
    private final BlockBounds bounds;
    /**
     * Openings, transformed using transform.
     */
    private final List<Opening> openings;
    /**
     * Map, NOT yet transformed using transform. Make sure to do that before placing.
     * Don't mutate!
     */
    private final MapTemplate template;
    private final @Nullable MapTransform transform;


    public DungeonPiece(MapTemplate template) {
        this(template, null);
    }

    public DungeonPiece(MapTemplate template, @Nullable MapTransform transform) {
        this(template.getBounds(), findOpenings(template), template, transform);
    }

    protected DungeonPiece(BlockBounds bounds, List<Opening> openings, MapTemplate template, @Nullable MapTransform transform) {
        this.bounds = bounds;
        this.openings = List.copyOf(openings);
        this.template = template;
        this.transform = transform;
    }

    protected static List<Opening> findOpenings(MapTemplate template) {
        // Require all openings to be flat against the template's bounds
        return new ArrayList<>(); // TODO
    }

    public BlockBounds bounds() {
        return bounds;
    }

    /**
     * @return Whether the room connects the given Openings.
     */
    public boolean canConnect(Opening a, Opening b) {
        return openings().contains(a) && openings().contains(b);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DungeonPiece) obj;
        return Objects.equals(this.bounds, that.bounds) &&
               Objects.equals(this.openings, that.openings);
    }

    public boolean hasOpening(BlockPos size) {
        return openings().stream().anyMatch(opening -> size.equals(opening.bounds().size()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(bounds, openings);
    }

    public List<Opening> openings() {
        return openings;
    }

    @Override
    public String toString() {
        return "DungeonPiece[" +
               "bounds=" + bounds + ", " +
               "openings=" + openings + ']';
    }

    public DungeonPiece withTransform(MapTransform transform) {
        BlockBounds bounds = transform.transformedBounds(this.bounds);
        List<Opening> openings = this.openings.stream()
                .map(opening -> opening.transformed(transform, bounds))
                .toList();
        return new DungeonPiece(bounds, openings, template, transform);
    }

    public static final class Opening {
        private final BlockBounds bounds;
        private final Direction direction;

        /**
         * @param bounds    The bounds of the opening.
         * @param direction The direction in which another piece could be placed.
         */
        public Opening(BlockBounds bounds, Direction direction) {
            this.bounds = bounds;
            this.direction = direction;
        }

        public Opening(BlockBounds bounds, BlockBounds templateBounds) {
            this(bounds, dirFromBounds(bounds, templateBounds));
        }

        /**
         * Get the direction of an opening based on the bounds of the opening and the bounds of the map.
         * Openings must be one block thick and flush with a face of the map. The chosen opening direction
         * should be considered arbitrary if placed entirely on an edge (read: where two or more wall meet).
         * @param opening
         * @param map
         * @return
         */
        public static Direction dirFromBounds(BlockBounds opening, BlockBounds map) {
            BlockPos size = opening.size();
            // The size component that is 0 will be one block thick. The corresponding opening component (min or max):
            // will match the min component of the map when the direction is negative
            // otherwise, it MUST match the max component of the map (the direction is positive)
            if (size.getX() == 0) {
                if (opening.min().getX() == map.min().getX()) {
                    return Direction.from(Direction.Axis.X, Direction.AxisDirection.NEGATIVE);
                }
                if (opening.min().getX() == map.max().getX()) {
                    return Direction.from(Direction.Axis.X, Direction.AxisDirection.POSITIVE);
                }
            }
            if (size.getY() == 0) {
                return Direction.from(Direction.Axis.Y,
                        opening.min().getY() == map.min().getY() ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);

            }
            if (size.getZ() == 0) {
                return Direction.from(Direction.Axis.Z,
                        opening.min().getZ() == map.min().getZ() ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            }
            throw new IllegalArgumentException("At least one side of an opening must be 1 block thick and flush with the map's wall.");
        }

        public BlockBounds bounds() {
            return bounds;
        }

        public Direction direction() {
            return direction;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Opening) obj;
            return Objects.equals(this.bounds, that.bounds) &&
                   Objects.equals(this.direction, that.direction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bounds, direction);
        }

        @Override
        public String toString() {
            return "Opening[" +
                   "bounds=" + bounds + ", " +
                   "direction=" + direction + ']';
        }

        public Opening transformed(MapTransform transform, BlockBounds transformedTemplateBounds) {
            return new Opening(transform.transformedBounds(bounds), transformedTemplateBounds);
        }

    }
}
