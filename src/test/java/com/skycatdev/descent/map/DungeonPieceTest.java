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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DungeonPieceTest {
    protected static @UnknownNullability DungeonPiece HALL_1_1_1;
    protected static @UnknownNullability DungeonPiece ROOM_3_3_3;
    protected static @UnknownNullability MapTemplate HALL_1_1_1_TEMPLATE;

    @BeforeAll
    static void setup() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        MapTemplate hall_1_1_1 = MapTemplate.createEmpty();
        hall_1_1_1.setBlockState(BlockPos.ORIGIN, Blocks.SPONGE.getDefaultState());
        hall_1_1_1.getMetadata().addRegion(DungeonPiece.OPENING_MARKER, BlockBounds.ofBlock(BlockPos.ORIGIN));
        hall_1_1_1.getMetadata().addRegion(DungeonPiece.DUNGEON_MARKER, BlockBounds.ofBlock(BlockPos.ORIGIN));
        HALL_1_1_1_TEMPLATE = hall_1_1_1;
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
    @Execution(ExecutionMode.CONCURRENT)
    void base1_1_1() {
        List<DungeonPiece.Opening> expected = List.of(new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.UP),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.DOWN),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.NORTH),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.EAST),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.SOUTH),
                new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.WEST));

        assertThat(HALL_1_1_1.openings()).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(HALL_1_1_1.dungeonBounds()).isEqualTo(BlockBounds.ofBlock(BlockPos.ORIGIN));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void translation1_1_1() {
        BlockBounds expectedBounds = BlockBounds.ofBlock(new BlockPos(1, 1, 1));
        List<DungeonPiece.Opening> expectedOpenings = List.of(new DungeonPiece.Opening(expectedBounds, Direction.UP),
                new DungeonPiece.Opening(expectedBounds, Direction.DOWN),
                new DungeonPiece.Opening(expectedBounds, Direction.NORTH),
                new DungeonPiece.Opening(expectedBounds, Direction.EAST),
                new DungeonPiece.Opening(expectedBounds, Direction.SOUTH),
                new DungeonPiece.Opening(expectedBounds, Direction.WEST));

        DungeonPiece translated = HALL_1_1_1.withTransform(MapTransform.translation(1, 1, 1));

        assertThat(translated)
                .isNotSameAs(HALL_1_1_1)
                .extracting(DungeonPiece::dungeonBounds) // Not same reference - withTransform should return new
                    .isEqualTo(expectedBounds);
        assertThat(translated.openings())
                .containsExactlyInAnyOrderElementsOf(expectedOpenings);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void notConnectedUp() {
        for (Direction otherDir : Direction.values()) {
            if (otherDir != Direction.DOWN) {
                assertThat(HALL_1_1_1.isConnected(new DungeonPiece.Opening(BlockBounds.ofBlock(new BlockPos(0, 1, 0)), otherDir)))
                        .isFalse();
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void connectedUp() {
        assertThat(HALL_1_1_1.isConnected(new DungeonPiece.Opening(BlockBounds.ofBlock(new BlockPos(0, 1, 0)), Direction.DOWN)))
                .isTrue();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void findOpenings1_1_1() {
        BlockBounds expectedBounds = BlockBounds.ofBlock(BlockPos.ORIGIN);
        List<DungeonPiece.Opening> expectedOpenings = List.of(new DungeonPiece.Opening(expectedBounds, Direction.UP),
                new DungeonPiece.Opening(expectedBounds, Direction.DOWN),
                new DungeonPiece.Opening(expectedBounds, Direction.NORTH),
                new DungeonPiece.Opening(expectedBounds, Direction.EAST),
                new DungeonPiece.Opening(expectedBounds, Direction.SOUTH),
                new DungeonPiece.Opening(expectedBounds, Direction.WEST));
        assertThat(DungeonPiece.findOpenings(HALL_1_1_1_TEMPLATE, Identifier.of(Descent.MOD_ID, "test/hall_1_1_1"), null))
                .containsExactlyInAnyOrderElementsOf(expectedOpenings);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void findOpenings1_1_1Transformed() {
        BlockBounds expectedBounds = BlockBounds.ofBlock(new BlockPos(1, 2, 3));
        List<DungeonPiece.Opening> expectedOpenings = List.of(new DungeonPiece.Opening(expectedBounds, Direction.UP),
                new DungeonPiece.Opening(expectedBounds, Direction.DOWN),
                new DungeonPiece.Opening(expectedBounds, Direction.NORTH),
                new DungeonPiece.Opening(expectedBounds, Direction.EAST),
                new DungeonPiece.Opening(expectedBounds, Direction.SOUTH),
                new DungeonPiece.Opening(expectedBounds, Direction.WEST));
        assertThat(DungeonPiece.findOpenings(HALL_1_1_1_TEMPLATE, Identifier.of(Descent.MOD_ID, "test/hall_1_1_1"), MapTransform.translation(1, 2, 3)))
                .containsExactlyInAnyOrderElementsOf(expectedOpenings);
    }

}
