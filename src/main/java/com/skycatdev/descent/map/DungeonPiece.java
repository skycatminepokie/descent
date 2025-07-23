package com.skycatdev.descent.map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
        this(template.getBounds(), findOpenings(template), template, null);
    }

    /**
     *
     * @param bounds The bounds of the piece AFTER transforming
     * @param openings The openings of the piece AFTER transforming
     * @param template The template of the piece BEFORE transforming
     * @param transform The transform to apply to the template before placing
     */
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

    public boolean isConnected(Opening opening) {
        for (Opening myOpening : openings) {
            if (opening.isConnected(myOpening)) return true;
        }
        return false;
    }

    public @Nullable Opening getConnected(Opening opening) {
        for (Opening myOpening : openings) {
            if (opening.isConnected(myOpening)) return myOpening;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DungeonPiece that)) return false;
        return Objects.equals(bounds, that.bounds) && Objects.equals(openings, that.openings) && Objects.equals(template, that.template) && Objects.equals(transform, that.transform);
    }

    public boolean hasOpening(BlockPos size) {
        return openings().stream().anyMatch(opening -> size.equals(opening.bounds().size()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(bounds, openings, template, transform);
    }

    public List<Opening> openings() {
        return openings;
    }

    @Override
    public String toString() {
        return "DungeonPiece{" +
               "bounds=" + bounds +
               ", openings=" + openings +
               ", template=" + template +
               ", transform=" + transform +
               '}';
    }

    public DungeonPiece withTransform(MapTransform transform) {
        BlockBounds bounds = transform.transformedBounds(this.bounds);
        List<Opening> openings = this.openings.stream()
                .map(opening -> opening.transformed(transform, bounds))
                .toList();
        return new DungeonPiece(bounds, openings, template, transform);
    }

    public MapTemplate toTemplate() {
        if (transform != null) {
            return template.transformed(transform);
        }
        return template;
    }

    /**
     * All the possible ways to match this piece's template to an opening by shifting
     * the piece. Must not account for rotations and mirrors.
     * @param toMatch The opening to match with
     * @return A collection of matching pieces, which are just this piece but transformed.
     */
    public Stream<AStar.ProtoNode> matchedWith(DungeonPiece.Opening toMatch) {
        BlockPos matchSize = toMatch.bounds().size();
        Direction matchOpposite = toMatch.direction().getOpposite();
        return openings.stream()
                .filter(opening -> opening.bounds().size().equals(matchSize))
                .filter(opening -> opening.direction().getOpposite().equals(matchOpposite))
                .map(opening -> {
                    BlockPos diff = toMatch.bounds().min().subtract(opening.bounds().min()).offset(toMatch.direction());
                    return new AStar.ProtoNode(opening, this.withTransform(MapTransform.translation(diff.getX(), diff.getY(), diff.getZ())));
                });
    }

    public record Opening(BlockBounds bounds, Direction direction, BlockPos center) {
        /**
         * @param bounds         The bounds of the opening.
         * @param templateBounds The bounds of the template this is placed in.
         */
        public Opening(BlockBounds bounds, BlockBounds templateBounds) {
            this(bounds, dirFromBounds(bounds, templateBounds), BlockPos.ofFloored(bounds.center()));
        }

        /**
         * Get the direction of an opening based on the bounds of the opening and the bounds of the map.
         * Openings must be one block thick and flush with a face of the map. The chosen opening direction
         * should be considered arbitrary if placed entirely on an edge (read: where two or more wall meet).
         *
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
                if (opening.min().getY() == map.min().getY()) {
                    return Direction.from(Direction.Axis.Y, Direction.AxisDirection.NEGATIVE);
                }
                if (opening.min().getY() == map.max().getY()) {
                    return Direction.from(Direction.Axis.Y, Direction.AxisDirection.POSITIVE);
                }
            }
            if (size.getZ() == 0) {
                if (opening.min().getZ() == map.min().getZ()) {
                    return Direction.from(Direction.Axis.Z, Direction.AxisDirection.NEGATIVE);
                }
                if (opening.min().getZ() == map.max().getZ()) {
                    return Direction.from(Direction.Axis.Z, Direction.AxisDirection.POSITIVE);
                }
            }
            throw new IllegalArgumentException("At least one side of an opening must be 1 block thick and flush with the map's wall.");
        }

        public Opening transformed(MapTransform transform, BlockBounds transformedTemplateBounds) {
            return new Opening(transform.transformedBounds(bounds), transformedTemplateBounds);
        }

        public boolean isConnected(Opening opening) {
            return opening.direction().getOpposite().equals(this.direction()) &&
                   opening.bounds().size().equals(this.bounds().size()) &&
                   opening.center().getManhattanDistance(this.center()) == 1;

        }

    }
}
