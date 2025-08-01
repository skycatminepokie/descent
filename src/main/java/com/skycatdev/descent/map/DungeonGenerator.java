package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import com.skycatdev.descent.config.MapConfig;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
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
            room = room.withTransform(MapTransform.translation(-center.getX(), -center.getY(), -center.getZ())); // Center the room
            rooms.add(room);
        }

        DungeonPiece start = loadPiece(config.starts().get(random), server);
        rooms.add(start); // Start
        rooms.add(loadPiece(config.ends().get(random), server)); // End

        steerRooms(config, random, rooms);

        // There's probably something better than a lookup, but this will do
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

        for (Edge edge : resultingEdges) {
            connections.add(new Pair<>(pieceLookup.get(edge.u()), pieceLookup.get(edge.v())));
        }

        Collection<DungeonPiece> paths = AStar.generatePath(rooms, connections, pathPieces, random);

        if (Descent.LOGGER.isTraceEnabled()) {
            int i = 0;
            StringBuilder points = new StringBuilder();
            StringBuilder line = new StringBuilder("l=");
            for (DungeonPiece piece : paths) {
                Vec3d center = piece.dungeonBounds().center();
                points.append(String.format("p_{%d}=(%d,%d,%d)\n", i, (int) Math.ceil(center.getX()), (int) Math.ceil(center.getY()), (int) Math.ceil(center.getZ())));
                line.append("p_{");
                line.append(i);
                line.append("},");
                i++;
            }
            line.deleteCharAt(line.length() - 1);
            Descent.LOGGER.trace("Path dump (centers):\n{}{}", points, line);
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
            Descent.LOGGER.debug("Translating y by {}", -minY);
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
        Descent.LOGGER.debug("Steering {} rooms", rooms.size());
        boolean keepSteering = true;

        while (keepSteering) {
            keepSteering = false;
            for (int i = 0; i < rooms.size(); i++) {
                DungeonPiece main = rooms.get(i);
                boolean move = false; // Whether this needs to move - handles when things are all zerod out, but still overlapping

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
                    }
                }

                if (move) {
                    keepSteering = true;
                    // TODO: Constants or random formula may need tweaking
                    int x = random.nextBetween(-config.minSeparationX() * 3, config.minSeparationX() * 3);
                    int y = random.nextBetween(-config.minSeparationY() * 3, config.minSeparationY() * 3);
                    int z = random.nextBetween(-config.minSeparationZ() * 3, config.minSeparationZ() * 3);
                    
                    rooms.set(i, main.withTransform(MapTransform.translation(x, y, z)));
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
