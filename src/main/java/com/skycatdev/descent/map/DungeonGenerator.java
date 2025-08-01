package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import com.skycatdev.descent.config.MapConfig;
import com.skycatdev.descent.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.MapTransform;

import java.io.IOException;
import java.util.*;

public class DungeonGenerator {
    public static MapTemplate generate(MapConfig config, MinecraftServer server, Random random) throws IOException, NoSolutionException {
        List<DungeonPiece> pathPieces = loadPieces(config.paths(), server);
        List<DungeonPiece> rooms = new ArrayList<>();

        for (int i = 0; i < config.numberOfRooms() - 2; i++) { // -2 to account for start and end
            DungeonPiece room = loadPiece(config.rooms().get(random), server);
            Vec3i center = BlockPos.ofFloored(room.dungeonBounds().center());
            room.withTransform(MapTransform.translation(-center.getX(), -center.getY(), -center.getZ())); // Center the room
            rooms.add(room);
        }

        DungeonPiece start = loadPiece(config.starts().get(random), server);
        rooms.add(start); // Start
        rooms.add(loadPiece(config.ends().get(random), server)); // End

        steerRooms(config, random, rooms);

        // Possibly better than a lookup, but this will do
        Map<Vec3d, DungeonPiece> pieceLookup = new HashMap<>();
        List<Vec3d> centers = new LinkedList<>();

        for (DungeonPiece room : rooms) {
            Vec3d center = room.dungeonBounds().center();
            centers.add(center);
            pieceLookup.put(center, room);
        }


        Set<Edge> allEdges = Delaunay3D.triangulate(centers);
        Set<Edge> resultingEdges = Prim.minimumSpanningTree(allEdges, centers.get(random.nextBetween(0, centers.size() - 1)));

        for (Edge edge : allEdges) {
            // TODO: 10 is the constant that can be tweaked (chance of path being added back)
            if (random.nextBetween(0, 99) < 10) {
                resultingEdges.add(edge);
            }
        }

        List<Pair<DungeonPiece, DungeonPiece>> connections = new LinkedList<>();

        // TODO: Don't allow b -> a if we have a -> b
        for (Edge edge : resultingEdges) {
            connections.add(new Pair<>(pieceLookup.get(edge.u()), pieceLookup.get(edge.v())));
        }

        Collection<DungeonPiece> paths = NewAStar.generatePath(rooms, connections, pathPieces, random);

        {
            int i = 0; // TODO DEBUG ONLY
            StringBuilder sb = new StringBuilder("l=");
            for (DungeonPiece piece : paths) {
                Vec3d center = piece.dungeonBounds().center();
                System.out.printf("p_{%d}=(%d,%d,%d)\n", i, (int) Math.ceil(center.getX()), (int) Math.ceil(center.getY()), (int) Math.ceil(center.getZ()));
                sb.append("p_{");
                sb.append(i);
                sb.append("},");
                i++;
            }
            sb.deleteCharAt(sb.length() - 1);
            System.out.println(sb);
        }

        List<MapTemplate> templates = new ArrayList<>();

        int minY = Integer.MAX_VALUE;
        for (DungeonPiece room : rooms) {
            MapTemplate roomTemplate = room.toTemplate();
            minY = Math.min(roomTemplate.getBounds().min().getY(), minY);
            templates.add(roomTemplate);
        }

        for (DungeonPiece path : paths) {
            MapTemplate pathTemplate = path.toTemplate();
            minY = Math.min(pathTemplate.getBounds().min().getY(), minY);
            templates.add(path.toTemplate());
        }

        if (minY < 0) {
            Descent.LOGGER.info("Translating y by {}", minY); // TODO: Set to debug
            for (int i = 0; i < templates.size(); i++) {
                templates.set(i, templates.get(i).translated(0, -minY, 0));
            }
        }


        MapTemplate map = templates.getFirst();

        for (int i = 1; i < templates.size(); i++) {
            map = MapTemplate.merged(map, templates.get(i));
        }

        map.setBlockState(BlockPos.ORIGIN, Blocks.SPONGE.getDefaultState());
        return map;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Contract("_,_,_->param3")
    private static List<DungeonPiece> steerRooms(MapConfig config, Random random, List<DungeonPiece> rooms) {
        boolean keepSteering = true;

        while (keepSteering) {
            keepSteering = false;
            for (int i = 0; i < rooms.size(); i++) {
                DungeonPiece main = rooms.get(i);
                boolean move = false; // Whether this needs to move - handles when things are all zerod out, but still overlapping
                BlockPos.Mutable totalOverlap = new BlockPos.Mutable(0,0,0);

                for (int j = 0; j < rooms.size(); j++) {
                    if (i == j) continue;
                    BlockBounds otherBounds = rooms.get(j).dungeonBounds();
                    BlockBounds avoid = BlockBounds.of(
                            otherBounds.min().subtract(new Vec3i(config.minSeparationX(), config.minSeparationY(), config.minSeparationZ())),
                            otherBounds.max().add(new Vec3i(config.minSeparationX(), config.minSeparationY(), config.minSeparationZ()))
                    );
                    @Nullable BlockBounds intersect = main.dungeonBounds().intersection(avoid);
                    if (intersect != null) {
                        move = true;
                        BlockPos magnitude = intersect.size();
                        Vec3d centerDiff = main.dungeonBounds().center().subtract(avoid.center());
                        totalOverlap.move(Utils.copySign(magnitude, centerDiff));
                    }
                }

                if (move) {
                    keepSteering = true;
                    // TODO: Constants may need tweaking

                    // Move it further (double it) TODO May need tweaking
                    totalOverlap.move(totalOverlap);
                    // Don't move the piece super far
                    totalOverlap.clamp(Direction.Axis.X, -config.minSeparationX() * 5, config.minSeparationX() * 5);
                    totalOverlap.clamp(Direction.Axis.Y, -config.minSeparationY() * 5, config.minSeparationY() * 5);
                    totalOverlap.clamp(Direction.Axis.Z, -config.minSeparationZ() * 5, config.minSeparationZ() * 5);

                    // Move the piece
                    if (totalOverlap.getX() != 0 || totalOverlap.getY() != 0 || totalOverlap.getZ() != 0) {
                        rooms.set(i, main.withTransform(MapTransform.translation(totalOverlap.getX(), totalOverlap.getY(), totalOverlap.getZ())));
                    } else {
                        // We're overlapping things, but the forces are cancelling out. Add some randomness to keep moving.
                        // TODO: Constants may need tweaking
                        BlockPos.Mutable boundsSize = main.dungeonBounds().size().mutableCopy();
                        int steerX = Math.max(boundsSize.getX() / 4, 1);
                        int steerY = Math.max(boundsSize.getY() / 4, 1);
                        int steerZ = Math.max(boundsSize.getZ() / 4, 1);
                        rooms.set(i, main.withTransform(MapTransform.translation(random.nextBetween(-steerX, steerX),
                                        random.nextBetween(-steerY, steerY),
                                        random.nextBetween(-steerZ, steerZ)
                                ))
                        );
                    }
                }
            }
        }
        return rooms;
    }

    // TODO: Cache?
    protected static DungeonPiece loadPiece(Identifier id, MinecraftServer server) throws IOException {
        return new DungeonPiece(MapTemplateSerializer.loadFromResource(server, id), id);
    }

    // TODO: Cache? Parallelize? I'm not sure that I trust mc to handle parallelism though.
    protected static List<DungeonPiece> loadPieces(List<Identifier> ids, MinecraftServer server) throws IOException {
        List<DungeonPiece> pieces = new LinkedList<>();
        for (Identifier id : ids) {
            pieces.add(loadPiece(id, server));
        }
        return pieces;
    }

}
