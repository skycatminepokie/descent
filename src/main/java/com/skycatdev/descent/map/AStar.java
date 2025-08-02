package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import com.skycatdev.descent.utils.Utils;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AStar {
    public static Collection<DungeonPiece> generatePath(Collection<DungeonPiece> base,
                                                        Collection<Pair<DungeonPiece, DungeonPiece>> toConnect,
                                                        Collection<DungeonPiece> pieces,
                                                        Random random) throws NoSolutionException {
        Collection<DungeonPiece> paths = new HashSet<>();
        Collection<BlockBounds> dungeonBounds = base.stream()
                .map(DungeonPiece::dungeonBounds)
                .toList();
        for (Pair<DungeonPiece, DungeonPiece> connection : toConnect) {
            Descent.LOGGER.debug("Generating path...");
            paths.addAll(generatePath(dungeonBounds, connection.getLeft(), connection.getRight(), pieces, paths, random));
        }
        return paths;
    }

    private static Collection<DungeonPiece> generatePath(Collection<BlockBounds> base,
                                                         DungeonPiece start,
                                                         DungeonPiece end,
                                                         Collection<DungeonPiece> pieces,
                                                         Collection<DungeonPiece> placedPaths,
                                                         Random random) throws NoSolutionException {
        Collection<Node> open = new HashSet<>();
        Collection<Node> closed = new HashSet<>();

        if (start.equals(end)) {
            throw new NotImplementedException(); // TODO
        }

        // Decide on locations to match
        DungeonPiece.Opening entrance = Utils.randomFromList(List.copyOf(start.openings()), random);
        DungeonPiece.Opening exit = Utils.randomFromList(List.copyOf(end.openings()), random);

        if (entrance.isConnected(exit)) {
            return List.of();
        }

        open.add(new Node(null, start, null, 0, exit.center().getManhattanDistance(entrance.center())));

        while (!open.isEmpty()) {
            Descent.LOGGER.trace("Searching {} nodes.", open.size());

            Node parent = open.stream()
                    .min(Comparator.comparingInt(Node::estPathLength))
                    .orElseThrow(() -> new ConcurrentModificationException("open set was not empty, but there was nothing in it?"));
            open.remove(parent);

            // Find already placed pieces and add them to the open list as needed. Don't check for overlap of course.
            List<Node> placedNodes = streamNodeOpenings(parent, entrance).<Node>mapMulti((opening, adder) -> {
                for (DungeonPiece placedPath : placedPaths) {
                    DungeonPiece.@Nullable Opening connected = placedPath.getConnected(opening);
                    if (connected != null) {
                        adder.accept(Node.calculate(opening, placedPath, parent, entrance, exit));
                    }
                }
            })
                    // Filter out ones we've already searched
                    .filter(successor -> closed.stream().noneMatch(closedNode -> successor.piece().equivalentTo(closedNode.piece())))
                    .sequential()
                    .toList();
            for (Node placedNode : placedNodes) {
                if (placedNode.piece().isConnected(exit)) {
                    List<DungeonPiece> path = new LinkedList<>();
                    placedNode.iterateUp().iterator().forEachRemaining(node -> path.add(node.piece()));
                    path.remove(start); // Could've avoided this by making it null for the root node, but that was gonna be annoying
                    return path;
                }
            }
            open.addAll(placedNodes);

            // Generate successors
            var protos = streamNodeOpenings(parent, entrance).flatMap(opening -> pieces.stream().flatMap(piece -> piece.matchedWith(opening))) // Place all possible pieces next to each opening
                    // Don't intersect already-placed pieces
                    .filter(proto -> base.parallelStream().noneMatch(baseBound -> proto.piece().dungeonBounds().intersects(baseBound)))
                    // Don't intersect ancestors
                    .filter(proto -> parent.streamPath().map(Node::piece)
                            .map(DungeonPiece::dungeonBounds)
                            .noneMatch(ancestorBounds -> proto.piece().dungeonBounds().intersects(ancestorBounds)))
                    .toList();

            for (var proto : protos) {
                if (proto.piece().isConnected(exit)) { // Found a working path!
                    List<DungeonPiece> path = new LinkedList<>();
                    path.add(proto.piece());
                    parent.iterateUp().iterator().forEachRemaining(node -> path.add(node.piece()));
                    path.remove(start); // Could've avoided this by making it null for the root node, but that was gonna be annoying
                    return path;
                }

                // Skip if in closed (already searched this)
                boolean skip = false;
                for (Node closedNode : closed) {
                    if (proto.piece().equivalentTo(closedNode.piece())) {
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;

                Node successor = Node.fromProto(proto, parent, entrance, exit);
                replaceSlower(open, successor);
            }
            // Parent goes on closed
            Descent.LOGGER.trace("Searched a node");
            closed.add(parent);
        }

        throw new NoSolutionException("Ran out of options to check.");
    }

    private static void addAlreadyPlaced(Collection<DungeonPiece> placedPaths, DungeonPiece.Opening opening, Node parent, Consumer<Node> adder, DungeonPiece.Opening pathEntrance, DungeonPiece.Opening pathExit) {
        for (DungeonPiece placedPath : placedPaths) {
            DungeonPiece.@Nullable Opening connected = placedPath.getConnected(opening);
            if (connected != null) {
                adder.accept(Node.calculate(opening, placedPath, parent, pathEntrance, pathExit));
            }
        }
    }

    private static @NotNull Stream<DungeonPiece.Opening> streamNodeOpenings(Node parent, DungeonPiece.Opening pathEntrance) {
        if (parent.entrance() == null) { // First piece, only one valid opening
            return Stream.of(pathEntrance);
        } else { // Any other piece, (openings - 1) valid openings
            return parent.piece().openings().parallelStream().filter(opening -> !opening.equals(parent.entrance()));
        }
    }

    private static void replaceSlower(Collection<Node> replaceFrom, Node successor) {
        HashSet<Node> toRemove = new HashSet<>();
        boolean shouldAdd = true;

        for (Node node : replaceFrom) {
            if (successor.piece().equivalentTo(node.piece())) {
                if (successor.estPathLength() < node.estPathLength()) { // Current is faster
                    toRemove.add(node);
                } else { // At least one is faster
                    shouldAdd = false;
                    break;
                }
            }
        }

        replaceFrom.removeAll(toRemove);

        if (shouldAdd) {
            replaceFrom.add(successor);
        }
    }

    protected static void addAlreadyPlaced(Collection<DungeonPiece> placed, ProtoNode node, Consumer<ProtoNode> adder) {
        // TODO: More accurate heuristics? it will be shorter, so we could just call that our "reused path" bonus.
        if (placed.isEmpty()) {
            adder.accept(node);
            return;
        }
        boolean surrounded = true;

        for (DungeonPiece.Opening siblingOpening : node.piece().openings()) {
            if (siblingOpening.equals(node.opening())) continue;

            for (DungeonPiece piece : placed) {
                @Nullable DungeonPiece.Opening connected = piece.getConnected(siblingOpening);
                if (connected != null) { // Found a connecting piece that's placed
                    adder.accept(new ProtoNode(connected, piece));
                } else {
                    surrounded = false;
                }
            }
        }

        if (!surrounded) {
            adder.accept(node);
        }
    }

    private record Node(
            @Nullable Node parent,
            DungeonPiece piece,
            @Nullable DungeonPiece.Opening entrance,
            int distFromStart,
            int heuristic,
            int estPathLength
    ) {

        private Node(@Nullable Node parent, DungeonPiece piece, @Nullable DungeonPiece.Opening entrance, int distFromStart, int heuristic) {
            this(parent, piece, entrance, distFromStart, heuristic, distFromStart + heuristic);
        }

        public static Node calculate(DungeonPiece.Opening opening, DungeonPiece piece, Node parent, DungeonPiece.Opening pathEntrance, DungeonPiece.Opening pathExit) {
            DungeonPiece.Opening parentEntrance = parent.entrance() != null ? parent.entrance() : pathEntrance;

            int distFromStart = parent.distFromStart() + opening.center().getManhattanDistance(parentEntrance.center());
            int heuristic = parent.estPathLength() + opening.center().getManhattanDistance(pathExit.center());

            return new Node(parent,
                    piece,
                    opening,
                    distFromStart,
                    heuristic);
        }

        public static Node fromProto(ProtoNode proto, Node parent, DungeonPiece.Opening pathEntrance, DungeonPiece.Opening pathExit) {
            return calculate(proto.opening(), proto.piece(), parent, pathEntrance, pathExit);
        }

        public Stream<Node> streamPath() {
            return StreamSupport.stream(iterateUp().spliterator(), false);
        }

        public Iterable<Node> iterateUp() {
            return () -> new Iterator<>() {
                @Nullable Node current = Node.this;
                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public Node next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    Node ret = current;
                    current = current.parent();
                    return ret;
                }
            };
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
