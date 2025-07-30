package com.skycatdev.descent.map;

import com.skycatdev.descent.utils.Utils;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NewAStar {
    public static Collection<DungeonPiece> generatePath(Collection<DungeonPiece> base,
                                                        Collection<Pair<DungeonPiece, DungeonPiece>> toConnect,
                                                        Collection<DungeonPiece> pieces,
                                                        Random random) throws NoSolutionException {
        Collection<DungeonPiece> paths = new HashSet<>();
        Collection<BlockBounds> dungeonBounds = base.stream()
                .map(DungeonPiece::dungeonBounds)
                .toList();
        for (Pair<DungeonPiece, DungeonPiece> connection : toConnect) {
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
        DungeonPiece.Opening entrance = Utils.randomFromList(start.openings(), random);
        DungeonPiece.Opening exit = Utils.randomFromList(end.openings(), random);

        open.add(new Node(null, start, entrance, 0, exit.center().getManhattanDistance(entrance.center())));

        while (!open.isEmpty()) {
            Node parent = open.stream()
                    .min(Comparator.comparingInt(Node::estPathLength))
                    .orElseThrow(() -> new ConcurrentModificationException("open set was not empty, but there was nothing in it?"));
            open.remove(parent);

            // Generate successors TODO parallelize
            var protos = pieces.stream().flatMap(piece -> parent.piece().openings().stream()
                            .filter(opening -> !opening.equals(parent.entrance()))
                            .flatMap(piece::matchedWith))
                    // Don't intersect already-placed pieces
                    .filter(proto -> base.parallelStream().noneMatch(baseBound -> proto.piece().dungeonBounds().intersects(baseBound)))
                    // Don't intersect ancestors
                    .filter(proto -> parent.streamPath().map(Node::piece)
                            .map(DungeonPiece::dungeonBounds)
                            .noneMatch(ancestorBounds -> proto.piece().dungeonBounds().intersects(ancestorBounds)))
                    // Iterate already placed
                    .<AStar.ProtoNode>mapMulti((proto, adder) -> traversePlaced(placedPaths, proto, adder, proto.piece()))
                    .toList();

            for (var proto : protos) {
                if (proto.piece().isConnected(exit)) { // Found a working path!
                    List<DungeonPiece> path = new LinkedList<>();
                    path.add(proto.piece());
                    parent.iterateUp().iterator().forEachRemaining(node -> path.add(node.piece()));
                    return path;
                }
            }

            protos.parallelStream()
                    // For each successor
                    .map(proto -> Node.fromProto(proto, parent, entrance, exit))
                    // If a node with the same pos in open is faster, skip
                    .filter(successor -> open.parallelStream()
                            .noneMatch(openNode -> openNode.estPathLength() < successor.estPathLength() && openNode.piece().equals(successor.piece())))
                    // If a node with the same pos in closed is faster, skip
                    .filter(successor -> closed.parallelStream()
                            .noneMatch(closedNode -> closedNode.estPathLength() < successor.estPathLength() && closedNode.piece().equals(successor.piece())))
                    .sequential()
                    // Add node to open
                    .forEach(open::add);
            // Parent goes on closed
            // TODO: We're double adding somewhere
            closed.add(parent);
        }

        throw new NoSolutionException("Ran out of options to check.");
    }

    private static void traversePlaced(Collection<DungeonPiece> placed, AStar.ProtoNode node, Consumer<AStar.ProtoNode> adder, DungeonPiece root) {
        // TODO: Fix bad heuristics
        if (placed.isEmpty()) {
            adder.accept(node);
            return;
        }
        boolean surrounded = true;

        for (DungeonPiece.Opening siblingOpening : node.piece().openings()) {
            if (siblingOpening.equals(node.opening())) continue;

            for (DungeonPiece piece : placed) {
                @Nullable DungeonPiece.Opening connected = piece.getConnected(siblingOpening);
                if (connected != null) { // If there's another piece to traverse
                    List<DungeonPiece> toSearch = new LinkedList<>(placed);
                    toSearch.remove(piece); // Don't loop back
                    traversePlaced(toSearch, new AStar.ProtoNode(connected, piece), adder, root);
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
            DungeonPiece.Opening entrance,
            int distFromStart,
            int heuristic,
            int estPathLength
    ) {

        private Node(@Nullable Node parent, DungeonPiece piece, DungeonPiece.Opening entrance, int distFromStart, int heuristic) {
            this(parent, piece, entrance, distFromStart, heuristic, distFromStart + heuristic);
        }

        public static Node fromProto(AStar.ProtoNode proto, @Nullable Node parent, DungeonPiece.Opening pathEntrance, DungeonPiece.Opening pathExit) {
            int distFromStart;
            DungeonPiece.Opening pieceEntrance = proto.opening();

            if (parent != null) {
                distFromStart = parent.distFromStart() + parent.entrance().center().getManhattanDistance(pieceEntrance.center());
            } else {
                distFromStart = pathEntrance.center().getManhattanDistance(pieceEntrance.center());
            }
            return new Node(parent,
                    proto.piece(),
                    pieceEntrance,
                    distFromStart,
                    pathEntrance.center().getManhattanDistance(pathExit.center()));
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

}
