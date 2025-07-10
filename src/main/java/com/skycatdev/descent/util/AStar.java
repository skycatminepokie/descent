package com.skycatdev.descent.util;

import com.mojang.datafixers.util.Pair;
import com.skycatdev.descent.DungeonPiece;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;

import java.util.*;
import java.util.stream.Stream;


/**
 * A (mostly) A* pathfinding algorithm. Allows for staircases that take up multiple cells.
 */
public class AStar {
    @Contract(mutates = "param1")
    protected static Collection<DungeonPiece> findPath(Collection<DungeonPiece> paths,
                                                       Collection<BlockBounds> dungeon, // Everywhere we can't intersect - both paths and rooms
                                                       DungeonPiece.Opening start,
                                                       DungeonPiece.Opening end,
                                                       Collection<DungeonPiece> pieces) throws NoSolutionException { // include all the rotations and mirrors
        // Setup
        BlockPos startCenter = start.center();
        BlockPos endCenter = end.center();
        int dist = startCenter.getManhattanDistance(endCenter);

        // Preconditions
        if (start.equals(end) || dist == 0) {
            return paths; // TODO: Logging
        }

        if (dist == 1) {
            if (start.direction().getOpposite().equals(end.direction())) {
                return paths;
            }
            throw new NoSolutionException(); // TODO: Logging
        }

        // Algorithm
        List<Node> open = new LinkedList<>();
        List<Node> closed = new LinkedList<>();
        open.add(new Node(start, null, 0, dist, null));

        while (!open.isEmpty()) {
            Node parent = open.stream()
                    .min(Comparator.comparingDouble(Node::pathLength))
                    .orElseThrow(() -> new RuntimeException("Open list was not empty, but its stream was?"));
            open.remove(parent); // TODO: This might be slow

            // Generate successors
            pieces.stream() // TODO: Parallelize?
                    .flatMap(piece -> piece.matchedWith(parent.opening())) // Find candidate openings
                    .filter(candidate -> dungeon.stream() // Find valid openings (don't intersect things)
                            .noneMatch(bounds -> bounds.intersects(candidate.piece().bounds()))) // TODO: Verify that this blocks all intersections
                    // TODO: This only does one placed piece, it doesn't work for multiple in series
                    .<Node>mapMulti((valid, nodeAdder) -> {

                        // Try finding an already-placed piece that has a matching opening
                        // If there is one, recurse, marking their piece as the VALID opening's piece and not checking this piece for a match
                        // That says "You can have this node iff you have that piece (and therefore the valid opening)"
                        // If there is no already-placed piece, add the valid opening's node

                        // Find an already-placed opening that fits and add that too (if it exists)
                        @Nullable DungeonPiece matchedPath = null;
                        for (DungeonPiece path : paths) {
                            @Nullable DungeonPiece.Opening connected = path.getConnected(valid.opening());
                            if (connected != null) {
                                matchedPath = path;
                                nodeAdder.accept(new Node(connected, // Turn them into nodes
                                        validNode, // Parent
                                        validNode.distFromStart() + validNode.opening().center().getManhattanDistance(valid.opening().center()), // dist from start
                                        connected.center().getManhattanDistance(endCenter), // heuristic TODO: May have to give a discount to longer rooms
                                        path));
                                break;
                            }
                        }
                        if (matchedPath != null) {
                            paths.remove(matchedPath);
                        }
                    })
                    .filter(node -> Stream.concat(open.stream(), closed.stream()) // Only keep faster ones
                            .anyMatch(other -> other.pathLength() < node.pathLength() &&
                                             // Pieces shouldn't be null, it's just the starter node that does that, and that's removed by now.
                                             Objects.requireNonNull(other.getPiece()).bounds().intersects(Objects.requireNonNull(node.getPiece()).bounds())))
                    .forEach(open::add);
            // TODO: allow reusing old paths, maybe with a slight discount

            closed.add(parent);
        }




        return paths;
    }

    protected static Collection<ProtoNode> traverseAlreadyPlaced(ProtoNode proto, Collection<DungeonPiece> placed) {
        // Try finding an already-placed piece that has a matching opening
        Collection<ProtoNode> leaves = new LinkedList<>();
        for (DungeonPiece piece : placed) {
            DungeonPiece.@Nullable Opening connected = piece.getConnected(proto.opening());
            if (connected != null) {
                // If there is one, recurse. The next ProtoNodes we make will have the root ProtoNode's piece
                // This signifies that we have access to those ProtoNodes only if the root piece is placed.
                Collection<DungeonPiece> piecesLeft = new LinkedList<>(placed);
                for (DungeonPiece.Opening opening : piece.openings()) {
                    if (opening != connected) {
                        leaves.addAll(traverseAlreadyPlaced(new ProtoNode(opening, proto.piece()), piecesLeft));
                    }
                }
                return leaves;
            }
        }

        // We can't find any already-placed pieces that match, so there's no more nodes to consider.
        leaves.add(proto);
        return leaves;
    }

    protected static <T> T randomFromMax(Map<Integer, List<T>> map, Random random) {
        List<T> list = map.get(map.keySet().stream().max(Integer::compare).orElseThrow(() -> new IllegalArgumentException("Map must not be empty")));
        return list.get(random.nextInt(list.size()));
    }

    /**
     * @param base      Places to not intersect. Openings in these pieces may be opened.
     * @param toConnect Openings to connect. These will be opened.
     * @param pieces    The pieces that can be used to build a path.
     * @param cellSize  The size of a single dungeon cell. Make sure there's a way to
     *                  connect every combination of directions with a cell
     *                  of this size.
     * @return The additional pieces that make up the path.
     */
    public static Collection<DungeonPiece> getPaths(Collection<DungeonPiece> base,
                                                    Collection<Pair<BlockBounds, BlockBounds>> toConnect,
                                                    Collection<DungeonPiece> pieces,
                                                    int cellSize) {
        // TODO
        return null;
    }

    public static final class Node {
        private final DungeonPiece.Opening opening;
        private final int distFromStart;
        private final int heuristic;
        private final int pathLength;
        private @Nullable Node parent;
        private final @Nullable DungeonPiece piece;

        public @Nullable DungeonPiece getPiece() {
            return piece;
        }

        /**
         * @param opening
         * @param parent
         * @param distFromStart The travelling distance from the start (g)
         * @param heuristic     An estimated travelling distance from the end (h)
         * @param piece
         */
        public Node(
                DungeonPiece.Opening opening,
                @Nullable Node parent,
                int distFromStart,
                int heuristic,
                @Nullable DungeonPiece piece
        ) {
            this.opening = opening;
            this.parent = parent;
            this.distFromStart = distFromStart;
            this.heuristic = heuristic;
            this.piece = piece;
            this.pathLength = distFromStart + heuristic;
        }

        public Node(DungeonPiece.Opening opening, int distFromStart, int heuristic, @Nullable DungeonPiece piece) {
            this(opening, null, distFromStart, heuristic, piece);
        }

        public int distFromStart() {
            return distFromStart;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Node) obj;
            return Objects.equals(this.opening, that.opening) &&
                   Objects.equals(this.parent, that.parent) &&
                   this.distFromStart == that.distFromStart &&
                   this.heuristic == that.heuristic &&
                   this.pathLength == that.pathLength;
        }

        @Override
        public int hashCode() {
            return Objects.hash(opening, parent, distFromStart, heuristic, pathLength);
        }

        public int heuristic() {
            return heuristic;
        }

        public DungeonPiece.Opening opening() {
            return opening;
        }

        public @Nullable Node parent() {
            return parent;
        }

        public int pathLength() {
            return pathLength;
        }

        public void setParent(Node node) {
            this.parent = node;
        }

        @Override
        public String toString() {
            return "Node[" +
                   "opening=" + opening + ", " +
                   "parent=" + parent + ", " +
                   "distFromStart=" + distFromStart + ", " +
                   "heuristic=" + heuristic + ", " +
                   "pathLength=" + pathLength + ']';
        }


    }

    /**
     *
     * @param opening
     * @param piece The piece to place to reach the opening.
     * @implNote Note that you may have multiple pieces for each opening (multiple ProtoNodes).
     * This can happen when there is more than one piece that would allow access to the opening - two potential pieces may
     * overlap, or there may be more than one piece that connects to an already placed piece.
     */
    public record ProtoNode(DungeonPiece.Opening opening, DungeonPiece piece) {

    }

}
