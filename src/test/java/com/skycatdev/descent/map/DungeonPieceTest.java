package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.UnknownNullability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DungeonPieceTest {
    protected static @UnknownNullability DungeonPiece HALL_1_1_1;
    protected static @UnknownNullability DungeonPiece ROOM_3_3_3;

    @BeforeAll
    static void setup() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        MapTemplate hall_1_1_1 = MapTemplate.createEmpty();
        hall_1_1_1.setBlockState(BlockPos.ORIGIN, Blocks.SPONGE.getDefaultState());
        hall_1_1_1.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(BlockPos.ORIGIN));
        hall_1_1_1.getMetadata().addRegion(DungeonPiece.DUNGEON_MARKER, BlockBounds.ofBlock(BlockPos.ORIGIN));
        HALL_1_1_1 = new DungeonPiece(hall_1_1_1, Identifier.of(Descent.MOD_ID, "test/hall_1_1_1"));

        MapTemplate room_3_3_3 = MapTemplate.createEmpty();
        room_3_3_3.setBounds(BlockBounds.of(0, 0, 0, 2, 2, 2));
        room_3_3_3.getMetadata().addRegion(DungeonPiece.DUNGEON_MARKER, BlockBounds.of(0, 0, 0, 2, 2, 2));
        for (BlockPos blockPos : BlockPos.iterate(BlockPos.ORIGIN, new BlockPos(2, 2, 2))) {
            room_3_3_3.setBlockState(blockPos, Blocks.RED_WOOL.getDefaultState());
        }
        room_3_3_3.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(0, 1, 1)));
        room_3_3_3.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 0, 1)));
        room_3_3_3.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 1, 0)));
        room_3_3_3.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 1, 2)));
        room_3_3_3.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(1, 2, 1)));
        room_3_3_3.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(new BlockPos(2, 1, 1)));

        ROOM_3_3_3 = new DungeonPiece(room_3_3_3, Identifier.of(Descent.MOD_ID, "test/room_3_3_3"));
    }

    @Test
    void openings1_1_1() {
        List<DungeonPiece.Opening> expected = List.of(new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.UP),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.DOWN),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.NORTH),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.EAST),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.SOUTH),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.WEST));
        List<DungeonPiece.Opening> actual = HALL_1_1_1.openings();

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);

    }

}
