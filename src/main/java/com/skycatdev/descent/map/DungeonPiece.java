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
    public static final String DUNGEON_MARKER = "dungeon";
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
    private final StackedMapTransform transform; // TODO reinstate, ABOVE JAVADOC FOR template IS WRONG UNTIL THIS IS FIXED
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

    protected static BlockBounds getRealBounds(MapTemplate template) {
        return Objects.requireNonNull(template.getMetadata().getFirstRegionBounds(DUNGEON_MARKER)); // TODO: no reqnonnull
    }

    protected static List<Opening> findOpenings(MapTemplate template, Identifier id, @Nullable MapTransform transform) {
        // TODO: avoid transforming, it's slow.
        // TODO: THIS IS BEING MEAN WHY IS THE BOUNDS BIG
        // TODO: OHMYGOSH IT'S THE CHUNK SIZE SO SUCCESS WAS HAPPENING ON FREAKING CHUNK BORDERS
        // TODO: I'M GONNA YELL NOW AHHHHHHH
        BlockBounds map = Objects.requireNonNull(template.getMetadata().getFirstRegionBounds(DUNGEON_MARKER)); // TODO: no reqnonnull
        Stream<BlockBounds> openingBounds = template.getMetadata().getRegionBounds(OPENING_MARKER);

        // TODO: Move this into a conditional initialization
        return openingBounds.<Opening>mapMulti((bounds, adder) -> {
                    BlockPos size = bounds.size();
                    // The size component that is 0 will be one block thick. The corresponding opening component (min or max):
                    // will match the min component of the map when the direction is negative
                    // otherwise, it MUST match the max component of the map (the direction is positive)
                    boolean added = false;
                    if (size.getX() == 0) {
                        if (bounds.min().getX() == map.min().getX()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.X, Direction.AxisDirection.NEGATIVE)));
                        }
                        if (bounds.min().getX() == map.max().getX()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.X, Direction.AxisDirection.POSITIVE)));
                        }
                    }
                    if (size.getY() == 0) {
                        if (bounds.min().getY() == map.min().getY()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Y, Direction.AxisDirection.NEGATIVE)));
                        }
                        if (bounds.min().getY() == map.max().getY()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Y, Direction.AxisDirection.POSITIVE)));
                        }
                    }
                    if (size.getZ() == 0) {
                        if (bounds.min().getZ() == map.min().getZ()) {
                            added = true;
                            adder.accept(new Opening(bounds, Direction.from(Direction.Axis.Z, Direction.AxisDirection.NEGATIVE)));
                        }
                        if (bounds.min().getZ() == map.max().getZ()) {
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
        return getRealBounds(template); // TODO Slow, for debug, cache this
    }

    public boolean isConnected(Opening opening) {
        for (Opening myOpening : openings) {
            if (opening.isConnected(myOpening)) return true;
        }
        return false;
    }

    public boolean isConnected(DungeonPiece other) {
        for (Opening opening : openings) {
            for (Opening otherOpening : openings) {
                if (opening.isConnected(otherOpening)) {
                    return true;
                }
            }
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
        return Objects.equals(bounds, that.bounds) && Objects.equals(openings, that.openings) && Objects.equals(template, that.template);
    }

    public boolean hasOpening(BlockPos size) {
        return openings().stream().anyMatch(opening -> size.equals(opening.bounds().size()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(bounds, openings, template);
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
               '}';
    }

    public DungeonPiece withTransform(MapTransform transform) {
        // TODO: OPENING'S BOUNDS ARE NOT BEING TRANSLATED PROPERLY
        return new DungeonPiece(template.transformed(transform), id);
    }

    public MapTemplate toTemplate() {
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
                .filter(opening -> opening.direction().equals(matchOpposite))
                .map(opening -> {
                    BlockPos diff = toMatch.bounds().min().subtract(opening.bounds().min()).offset(toMatch.direction());
                    // new transform for each opening - it'll shift differently if the opening is on the left than if it's on the right
                    MapTransform transform = MapTransform.translation(diff.getX(), diff.getY(), diff.getZ());
                    return new AStar.ProtoNode(opening.transformed(transform), this.withTransform(transform));
                });
    }

    public record Opening(BlockBounds bounds, Direction direction, BlockPos center) {
        public Opening(BlockBounds bounds, Direction direction) {
            this(bounds, direction, BlockPos.ofFloored(bounds.center()));
        }

        public Opening transformed(MapTransform transform) {
            BlockPos transformedPosInDir = transform.transformedPoint(center.offset(direction));
            BlockBounds transformedBounds = transform.transformedBounds(bounds);
            BlockPos transformedCenter = transform.transformedPoint(center);
            @Nullable Direction transformedDir = Direction.fromVector(transformedPosInDir.subtract(transformedCenter), null);
            if (transformedDir == null) {
                Descent.LOGGER.warn("Failed to transform the direction of an opening. Not sure what happened - this may affect dungeon generation.");
                transformedDir = direction;
            }

            return new Opening(transformedBounds, transformedDir, transformedCenter);
        }

        public boolean isConnected(Opening opening) {
            return opening.direction().getOpposite().equals(this.direction()) &&
                   opening.bounds().size().equals(this.bounds().size()) &&
                   opening.center().getManhattanDistance(this.center()) == 1;
        }
    }
}
