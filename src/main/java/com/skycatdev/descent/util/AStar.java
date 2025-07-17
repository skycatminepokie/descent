package com.skycatdev.descent.util;

import com.mojang.datafixers.util.Pair;
import com.skycatdev.descent.DungeonPiece;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * A (mostly) A* pathfinding algorithm. Allows for staircases that take up multiple cells.
 */
public class AStar {
    protected static List<Node> findPath(Collection<DungeonPiece> paths,
                                                       Collection<BlockBounds> dungeon, // Everywhere we can't intersect - both paths and rooms
                                                       DungeonPiece.Opening start,
                                                       DungeonPiece.Opening end,
                                                       Collection<DungeonPiece> pieces,
                                                       Random random) throws NoSolutionException { // include all the rotations and mirrors
        // Setup
        BlockPos startCenter = start.center();
        BlockPos endCenter = end.center();
        int dist = startCenter.getManhattanDistance(endCenter);

        // Preconditions
        if (start.equals(end) || dist == 0) {
            return new ArrayList<>(); // TODO: Logging
        }

        if (dist == 1) {
            if (start.direction().getOpposite().equals(end.direction())) {
                return new ArrayList<>();
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
            open.remove(parent); // TODO: This might be slow. Use open/closed sets instead of lists?

            // Generate successors
            var nodesByOpening = pieces.stream() // TODO: Parallelize?
                    .flatMap(piece -> piece.matchedWith(parent.opening())) // Find candidate openings
                    .filter(candidate -> dungeon.stream() // Find valid openings (don't intersect things)
                            .noneMatch(bounds -> bounds.intersects(candidate.piece().bounds()))) // TODO: Verify that this blocks all intersections
                    .map(proto -> Node.fromProto(proto, parent, endCenter))
                    .<Node>mapMulti((valid, nodeAdder) -> traverseAlreadyPlaced(valid, paths, nodeAdder, endCenter))
                    .filter(node -> Stream.concat(open.stream(), closed.stream()) // Only keep faster ones
                            .noneMatch(other -> other.pathLength() < node.pathLength() && other.opening().equals(node.opening()))) // There are none at the same position with faster paths. TODO More random: if path lengths equal and openings equal, randomly choose whether to keep
                    // Make sure that two nodes with same opening but different lengths don't both end up on the open list
                    .collect(Collectors.groupingBy(Node::opening)); // Map of opening to a list of nodes that have that opening
            
            // Openings and nodes that have that opening
            for (Map.Entry<DungeonPiece.Opening, List<Node>> entry : nodesByOpening.entrySet()) {
                List<Node> nodes = entry.getValue();
                
                // Get only the fastest ones for this opening
                List<Node> fastestNodes = getFastest(nodes);

                if (entry.getKey().isConnected(end)) { // We've found node(s) next to the end!
                    var finishingNode = fastestNodes.get(random.nextInt(fastestNodes.size())); // Choose a random one
                    return finishingNode.path();
                }
                
                open.addAll(fastestNodes);
            }
                
            closed.add(parent);
        }
        throw new NoSolutionException(); // TODO: Logging
    }

    private static @NotNull List<Node> getFastest(List<Node> nodes) {
        List<Node> fastestNodes = new LinkedList<>();
        int fastest = Integer.MAX_VALUE;
        for (Node node : nodes) {
            if (node.pathLength() < fastest) {
                fastest = node.pathLength();
                fastestNodes.clear();
                fastestNodes.add(node);
            } else if (node.pathLength() == fastest) {
                fastestNodes.add(node);
            }
        }
        return fastestNodes;
    }

    /**
     *
     * @param prev The node to start searching from.
     * @param placed The placed pieces to search for connections through.
     * @param collector What to call when we've found a path through the placed pieces.
     * @param endCenter The center of the ending opening (used for distance estimation)
     */
    protected static void traverseAlreadyPlaced(Node prev, Collection<DungeonPiece> placed, Consumer<Node> collector, BlockPos endCenter) {
        // Try finding an already-placed piece that has a matching opening
        // If there is one, recurse. The next Nodes we make will have the root Node's piece
        // This signifies that we have access to those Nodes only if the root piece is placed.

        for (DungeonPiece piece : placed) {
            DungeonPiece.@Nullable Opening connected = piece.getConnected(prev.opening());
            if (connected != null) {
                // Found a place for a new Node! (piece that has a connected opening)

                Collection<DungeonPiece> piecesLeft = new LinkedList<>(placed);
                piecesLeft.remove(piece); // Don't check this one - we don't want a loop, and we already handle this one

                for (DungeonPiece.Opening candidate : piece.openings()) {
                    if (candidate != connected) { // Don't add the connected one - that's not a leaf
                        traverseAlreadyPlaced(new Node(candidate,
                                prev, // Parent
                                prev.distFromStart() + prev.opening().center().getManhattanDistance(candidate.center()), // dist from start
                                candidate.center().getManhattanDistance(endCenter), // heuristic TODO: May have to give a discount to longer rooms
                                prev.getPiece()), piecesLeft, collector, endCenter); // Use prev's piece to show that this node is valid only if the root piece is placed
                    }
                }
                return;
            }
            // Nothing connected, try next
        }

        // We can't find any already-placed pieces that match, so this is a leaf.
        collector.accept(prev);
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
        Collection<DungeonPiece> paths = new LinkedList<>();
        for (Pair<BlockBounds, BlockBounds> connection : toConnect) {
            findPath(paths, )
        }
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
         * 
         * @return A list containing this Node, its parent (if it has one), that parent's parent (if it has one), and so on.
         */
        public List<Node> path() {
            List<Node> ancestors = new LinkedList<>();
            @Nullable Node current = this;
            while (current != null) {
                ancestors.add(current);
                current = current.parent();
            }
            return ancestors;
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

        public static Node fromProto(ProtoNode proto, Node parent, BlockPos endCenter) {
            return new Node(proto.opening(),
                    parent, // Parent
                    parent.distFromStart() + parent.opening().center().getManhattanDistance(proto.opening().center()), // dist from start
                    proto.opening().center().getManhattanDistance(endCenter), // heuristic
                    proto.piece());
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
