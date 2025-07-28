package com.skycatdev.descent;

import com.skycatdev.descent.config.DescentConfig;
import com.skycatdev.descent.map.DungeonPiece;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameType;

import java.io.FileOutputStream;
import java.io.IOException;

public class Descent implements ModInitializer {
	public static final String MOD_ID = "descent";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> { // TODO: Remove
			{
				MapTemplate room = MapTemplate.createEmpty();
				int x = 2;
				int y = 2;
				int z = 2;
				room.setBounds(BlockBounds.of(0, 0, 0, x, y, z));
				room.getMetadata().addRegion(DungeonPiece.DUNGEON_MARKER, BlockBounds.of(0, 0, 0, x, y, z));
				for (BlockPos blockPos : BlockPos.iterate(BlockPos.ORIGIN, new BlockPos(x, y, z))) {
					room.setBlockState(blockPos, Blocks.RED_WOOL.getDefaultState());
				}
				room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(0, 1, 1)));
				room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 0, 1)));
				room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 1, 0)));
				room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 1, 2)));
				room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 2, 1)));
				room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(2, 1, 1)));

                try {
                    MapTemplateSerializer.saveTo(room,
							new FileOutputStream(server.getRunDirectory().resolve("test_room_3_3_3.nbt").toFile()),
							server.getOverworld().getRegistryManager());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
			{
				MapTemplate room = MapTemplate.createEmpty();
				int x = 4;
				int y = 4;
				int z = 4;
				room.setBounds(BlockBounds.of(0, 0, 0, x, y, z));
				room.getMetadata().addRegion(DungeonPiece.DUNGEON_MARKER, BlockBounds.of(0, 0, 0, x, y, z));
				for (BlockPos blockPos : BlockPos.iterate(BlockPos.ORIGIN, new BlockPos(x, y, z))) {
					room.setBlockState(blockPos, Blocks.RED_WOOL.getDefaultState());
				}
				BlockBounds[] openings = new BlockBounds[] {
						BlockBounds.of(1, 1, 0, x - 1, y - 1, 0),             // Front face (Z = 0)
						BlockBounds.of(1, 1, z, x - 1, y - 1, z),             // Back face (Z = z)
						BlockBounds.of(0, 1, 1, 0, y - 1, z - 1),             // Left face (X = 0)
						BlockBounds.of(x, 1, 1, x, y - 1, z - 1),             // Right face (X = x)
						BlockBounds.of(1, 0, 1, x - 1, 0, z - 1),             // Bottom face (Y = 0)
						BlockBounds.of(1, y, 1, x - 1, y, z - 1),             // Top face (Y = y)
				};

				for (BlockBounds opening : openings) {
					room.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, opening);
					for (BlockPos blockPos : BlockPos.iterate(opening.asBox())) {
						room.setBlockState(blockPos, Blocks.AIR.getDefaultState());
					}
				}
				try {
					MapTemplateSerializer.saveTo(room,
							new FileOutputStream(server.getRunDirectory().resolve("test_room_5_5_5.nbt").toFile()),
							server.getOverworld().getRegistryManager());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			{
				MapTemplate hallway = MapTemplate.createEmpty();
				hallway.setBounds(BlockBounds.ofBlock(BlockPos.ORIGIN));
				hallway.setBlockState(BlockPos.ORIGIN, Blocks.BLUE_WOOL.getDefaultState());

				hallway.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(BlockPos.ORIGIN));
				hallway.getMetadata().addRegion(DungeonPiece.DUNGEON_MARKER, BlockBounds.ofBlock(BlockPos.ORIGIN));
				try {
					MapTemplateSerializer.saveTo(hallway,
							new FileOutputStream(server.getRunDirectory().resolve("test_hall_1_1_1.nbt").toFile()),
							server.getOverworld().getRegistryManager());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

		});

		GameType.register(
				Identifier.of(MOD_ID, "descent"),
				DescentConfig.CODEC,
				DescentGame::open
		);
	}
}