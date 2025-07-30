package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
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
    /**
     *
     * @param paths Already placed paths
     * @param dungeon Everywhere we can't intersect - both paths and rooms
     * @param start The start of the path
     * @param end The end of the path
     * @param pieces The pieces we can use to make the path
     * @param random
     * @return
     * @throws NoSolutionException
     */
    protected static List<Node> findPath(Collection<DungeonPiece> paths,
                                         Collection<BlockBounds> dungeon,
                                         DungeonPiece start,
                                         DungeonPiece end,
                                         Collection<DungeonPiece> pieces,
                                         Random random) throws NoSolutionException {
        // Setup
        DungeonPiece.Opening startOpening;
        DungeonPiece.Opening endOpening;

        if (start.equals(end)) {
            if (start.openings().size() < 2) {
                return List.of();
            }
            int a = random.nextBetween(0, start.openings().size() - 1);
            int b = random.nextBetween(0, start.openings().size() - 1);
            while (b == a) {
                b = random.nextBetween(0, start.openings().size() - 1);
            }
            startOpening = start.openings().get(a);
            endOpening = start.openings().get(b);
        } else {
            startOpening = start.openings().get(random.nextBetween(0, start.openings().size() - 1));
            endOpening = end.openings().get(random.nextBetween(0, end.openings().size() - 1));
        }

        BlockPos startCenter = startOpening.center();
        BlockPos endCenter = endOpening.center();
        int dist = startCenter.getManhattanDistance(endCenter);

        // Preconditions
        if (dist == 1) {
            if (startOpening.direction().getOpposite().equals(endOpening.direction())) {
                return List.of();
            }
            // throw new NoSolutionException("Pieces are too close, and the openings are facing the wrong direction"); // TODO: Retry with diff openings
        }

        // Algorithm
        List<Node> open = new LinkedList<>();
        List<Node> closed = new LinkedList<>();
        traverseAlreadyPlaced(new Node(startOpening, null, 0, dist, null), paths, open::add, endCenter);

        while (!open.isEmpty()) {
            Node parent = open.stream()
                    .min(Comparator.comparingDouble(Node::pathLength))
                    .orElseThrow(() -> new ConcurrentModificationException("Open list was not empty, but its stream was?"));
            open.remove(parent); // TODO: This might be slow. Use open/closed sets instead of lists?

            // Generate successors
            var nodesByOpening = pieces.stream() // TODO: Parallelize?
                    .flatMap(piece -> piece.matchedWith(parent.opening())) // Find candidate nodes
                    .filter(candidate -> dungeon.stream() // Remove nodes with pieces that intersect the dungeon
                            .noneMatch(bounds -> bounds.intersects(candidate.piece().dungeonBounds()))) // TODO: Verify that this blocks all intersections
                    // Remove nodes with pieces that intersect their ancestors
                    .filter(candidate -> !parent.overlaps(candidate.piece().dungeonBounds()) && !parent.overlapsAncestors(candidate.piece().dungeonBounds()))
                    // We've got a piece and opening, now we need a ProtoNode for all its siblings (the next generation)
                    .<ProtoNode>mapMulti((valid, adder) -> {
                        for (DungeonPiece.Opening opening : valid.piece().openings()) {
                            if (!opening.equals(valid.opening())) {
                                adder.accept(new ProtoNode(opening, valid.piece()));
                            }
                        }
                    })
                    .map(proto -> Node.fromProto(proto, parent, endCenter))
                    .<Node>mapMulti((valid, nodeAdder) -> traverseAlreadyPlaced(valid, paths, nodeAdder, endCenter))
                    .filter(node -> Stream.concat(open.stream(), closed.stream()) // Only keep faster ones
                            .noneMatch(other -> other.pathLength() <= node.pathLength() && other.opening().equals(node.opening()))) // There are none at the same position with faster paths. TODO More random: if path lengths equal and openings equal, randomly choose whether to keep
                    // Make sure that two nodes with same opening but different lengths don't both end up on the open list
                    .collect(Collectors.groupingBy(Node::opening)); // Map of opening to a list of nodes that have that opening

            // Openings and nodes that have that opening
            for (Map.Entry<DungeonPiece.Opening, List<Node>> entry : nodesByOpening.entrySet()) {
                List<Node> nodes = entry.getValue();

                // Get only the fastest ones for this opening
                List<Node> fastestNodes = getFastest(nodes);

                if (entry.getKey().isConnected(endOpening)) { // We've found node(s) next to the end!
                    var finishingNode = fastestNodes.get(random.nextInt(fastestNodes.size())); // Choose a random one
                    return finishingNode.computePath();
                }
                Descent.LOGGER.info("Adding {} open nodes", fastestNodes.size()); // TODO: DEBUG ONLY
                open.addAll(fastestNodes);
            }
            closed.add(parent);
        }
        throw new NoSolutionException(); // TODO: Logging
    }

    /**
     * @param base      Places to not intersect.
     * @param toConnect Openings to connect.
     * @param pieces    The pieces that can be used to build a path.
     * @return The additional pieces that make up the paths.
     */
    public static Collection<DungeonPiece> generatePath(Collection<DungeonPiece> base,
                                                        Collection<Pair<DungeonPiece, DungeonPiece>> toConnect,
                                                        Collection<DungeonPiece> pieces,
                                                        Random random) throws NoSolutionException {
        Collection<DungeonPiece> paths = new LinkedList<>();
        Collection<BlockBounds> dungeonBounds = base.stream()
                .map(DungeonPiece::dungeonBounds)
                .toList();
        for (Pair<DungeonPiece, DungeonPiece> connection : toConnect) {
            for (Node node : findPath(paths, dungeonBounds, connection.getLeft(), connection.getRight(), pieces, random)) {
                if (node.piece() != null) { // The start
                    paths.add(node.piece());
                }
            }
        }
        return paths;
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
     * @param prev      The node to start searching from.
     * @param placed    The placed pieces to search for connections through.
     * @param collector What to call when we've found a path through the placed pieces.
     * @param endCenter The center of the ending opening (used for distance estimation)
     */
    protected static void traverseAlreadyPlaced(Node prev, Collection<DungeonPiece> placed, Consumer<Node> collector, BlockPos endCenter) {
        // Try finding an already-placed piece that has a matching opening
        // If there is one, recurse. The next Nodes we make will have the root Node's piece
        // This signifies that we have access to those Nodes only if the root piece is placed.

        // Find seed
//        for (DungeonPiece piece : placed) {
//            DungeonPiece.@Nullable Opening connected = piece.getConnected(prev.opening());
//            if (connected != null) {
//                // Find anything else connected to this
//                for (DungeonPiece.Opening opening : piece.openings()) {
//                    if (opening != connected) {
//
//                    }
//                }
//            }
//
//        }

        for (DungeonPiece piece : placed) {
            DungeonPiece.@Nullable Opening connected = piece.getConnected(prev.opening());
            if (connected != null) {
                // Found a place for a new Node! (piece that has a connected opening)

                Collection<DungeonPiece> piecesLeft = new LinkedList<>(placed);
                piecesLeft.remove(piece); // Don't check this one - we don't want a loop, and we already handle this one

                for (DungeonPiece.Opening candidate : piece.openings()) {
                    if (!candidate.equals(connected)) { // Don't add the connected one - that's not a leaf
                        traverseAlreadyPlaced(new Node(candidate,
                                prev, // Parent
                                prev.distFromStart() + prev.opening().center().getManhattanDistance(candidate.center()), // dist from start
                                candidate.center().getManhattanDistance(endCenter), // heuristic TODO: May have to give a discount to longer rooms
                                prev.piece()), piecesLeft, collector, endCenter); // Use prev's piece to show that this node is valid only if the root piece is placed
                    }
                }
                return;
            }
            // Nothing connected, try next
        }

        // We can't find any already-placed pieces that match, so this is a leaf.
        collector.accept(prev);
    }

    /**
     * @param opening       The Opening this Node is connected to.
     * @param parent        The Node's parent. If the parent is accessible, and this Node's piece is placed, this Node is accessible.
     * @param distFromStart The travelling distance from the start (g)
     * @param heuristic     An estimated travelling distance from the end (h)
     * @param piece         The piece that must be placed to access this node (assuming the parent is accessible)
     */
    public record Node(DungeonPiece.Opening opening, AStar.@Nullable Node parent, int distFromStart, int heuristic,
                       @Nullable DungeonPiece piece, int pathLength) {
        public Node(
                DungeonPiece.Opening opening,
                @Nullable Node parent,
                int distFromStart,
                int heuristic,
                @Nullable DungeonPiece piece
        ) {
            this(opening, parent, distFromStart, heuristic, piece, distFromStart + heuristic);
        }

        public static Node fromProto(ProtoNode proto, Node parent, BlockPos endCenter) {
            return new Node(proto.opening(),
                    parent, // Parent
                    parent.distFromStart() + parent.opening().center().getManhattanDistance(proto.opening().center()), // dist from start
                    proto.opening().center().getManhattanDistance(endCenter), // heuristic
                    proto.piece());
        }

        public boolean overlaps(BlockBounds bounds) {
            if (piece != null) {
                return piece.dungeonBounds().intersects(bounds);
            }
            return false;
        }

        public boolean overlapsAncestors(BlockBounds bounds) {
            @Nullable Node ancestor = parent;
            while (ancestor != null) {
                if (ancestor.overlaps(bounds)) return true;
                ancestor = ancestor.parent;
            }
            return false;
        }

        /**
         * @return A list containing this Node, its parent (if it has one), that parent's parent (if it has one), and so on.
         */
        public List<Node> computePath() {
            List<Node> ancestors = new LinkedList<>();
            @Nullable Node current = this;
            while (current != null) {
                ancestors.add(current);
                current = current.parent();
            }
            return ancestors;
        }
    }

    /**
     * @param opening
     * @param piece   The piece to place to reach the opening.
     * @implNote Note that you may have multiple pieces for each opening (multiple ProtoNodes).
     * This can happen when there is more than one piece that would allow access to the opening - two potential pieces may
     * overlap, or there may be more than one piece that connects to an already placed piece.
     */
    public record ProtoNode(DungeonPiece.Opening opening, DungeonPiece piece) {

    }

}
