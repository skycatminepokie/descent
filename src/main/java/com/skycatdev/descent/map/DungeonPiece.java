package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DungeonPiece {
    public static final String OPENING_MARKER = "opening";
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
    private final StackedMapTransform transform;
    /**
     * The id of the template
     */
    private final Identifier id;

    public DungeonPiece(MapTemplate template, Identifier id) {
        this(template, id, new StackedMapTransform());
    }

    public DungeonPiece(MapTemplate template, Identifier id, StackedMapTransform transform) {
        this(template.getBounds(), findOpenings(template, id, transform), template, transform, id);
    }

    /**
     *
     * @param bounds The bounds of the piece AFTER transforming
     * @param openings The openings of the piece AFTER transforming
     * @param template The template of the piece BEFORE transforming
     * @param transform The transform to apply to the template before placing
     */
    protected DungeonPiece(BlockBounds bounds, List<Opening> openings, MapTemplate template, StackedMapTransform transform, Identifier id) {
        this.bounds = bounds;
        this.openings = List.copyOf(openings);
        this.template = template;
        this.transform = transform;
        this.id = id;
    }

    protected static List<Opening> findOpenings(MapTemplate template, Identifier id, @Nullable MapTransform transform) {
        BlockBounds map = template.getBounds();
        Stream<BlockBounds> openingBounds = template.getMetadata().getRegionBounds(OPENING_MARKER);
        if (transform != null) {
            openingBounds = openingBounds.map(transform::transformedBounds);
            map = transform.transformedBounds(map);
        }
        BlockBounds finalMap = map;
        return openingBounds.<Opening>mapMulti((bounds, adder) -> {
                    BlockPos size = bounds.size();
                    // The size component that is 0 will be one block thick. The corresponding opening component (min or max):
                    // will match the min component of the map when the direction is negative
                    // otherwise, it MUST match the max component of the map (the direction is positive)
                    boolean added = false;
                    if (size.getX() == 0) {
                        if (bounds.min().getX() == finalMap.min().getX()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.X, Direction.AxisDirection.NEGATIVE)));
                        }
                        if (bounds.min().getX() == finalMap.max().getX()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.X, Direction.AxisDirection.POSITIVE)));
                        }
                    }
                    if (size.getY() == 0) {
                        if (bounds.min().getY() == finalMap.min().getY()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Y, Direction.AxisDirection.NEGATIVE)));
                        }
                        if (bounds.min().getY() == finalMap.max().getY()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Y, Direction.AxisDirection.POSITIVE)));
                        }
                    }
                    if (size.getZ() == 0) {
                        if (bounds.min().getZ() == finalMap.min().getZ()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Z, Direction.AxisDirection.NEGATIVE)));
                        }
                        if (bounds.min().getZ() == finalMap.max().getZ()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Z, Direction.AxisDirection.POSITIVE)));
                        }
                    }
                    if (!added) {
                        Descent.LOGGER.warn("Failed to find a matching opening for {}. This may affect dungeon generation.", id);
                    }
                })
                .toList();
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

        List<Opening> openings = findOpenings(template, id, transform);
        return new DungeonPiece(bounds, openings, template, this.transform.copyWith(transform), id);
    }

    public MapTemplate toTemplate() {
        return template.transformed(transform);
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
        public Opening(BlockBounds bounds, Direction direction) {
            this(bounds, direction, BlockPos.ofFloored(bounds.center()));
        }

        public boolean isConnected(Opening opening) {
            return opening.direction().getOpposite().equals(this.direction()) &&
                   opening.bounds().size().equals(this.bounds().size()) &&
                   opening.center().getManhattanDistance(this.center()) == 1;
        }

    }
}
