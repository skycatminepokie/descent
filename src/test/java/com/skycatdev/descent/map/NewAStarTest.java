package com.skycatdev.descent.map;

import com.skycatdev.descent.Descent;
import com.skycatdev.descent.utils.Utils;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.UnknownNullability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTransform;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;

public class NewAStarTest {
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
    @Timeout(10)
    void testAdjacent() throws NoSolutionException {
        @UnknownNullability DungeonPiece start = ROOM_3_3_3;
        DungeonPiece end = ROOM_3_3_3.withTransform(MapTransform.translation(0, 0, 3));
        List<DungeonPiece> base = List.of(start, end);
        List<Pair<DungeonPiece, DungeonPiece>> toConnect = List.of(new Pair<>(start, end));
        List<DungeonPiece> pieces = List.of();
        Random random = Random.create(0);
        DungeonPiece.Opening entrance = start.openings().stream().filter(o -> o.direction().equals(Direction.SOUTH))
                .findFirst()
                .orElseThrow();
        DungeonPiece.Opening exit = end.openings().stream().filter(o -> o.direction().equals(Direction.NORTH))
                .findFirst()
                .orElseThrow();


        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.randomFromList(anyList(), any()))
                    .thenReturn(entrance, exit);
            Collection<DungeonPiece> actual = NewAStar.generatePath(base, toConnect, pieces, random);
            assertThat(actual)
                    .hasSize(0);
        }
    }

    @Test
    @Timeout(10)
    void testOneAway() throws NoSolutionException {
        @UnknownNullability DungeonPiece start = ROOM_3_3_3;
        DungeonPiece end = ROOM_3_3_3.withTransform(MapTransform.translation(0, 0, 4));
        List<DungeonPiece> base = List.of(start, end);
        List<Pair<DungeonPiece, DungeonPiece>> toConnect = List.of(new Pair<>(start, end));
        List<DungeonPiece> pieces = List.of(HALL_1_1_1);
        Random random = Random.create(0);
        DungeonPiece.Opening entrance = start.openings().stream().filter(o -> o.direction().equals(Direction.SOUTH))
                .findFirst()
                .orElseThrow();
        DungeonPiece.Opening exit = end.openings().stream().filter(o -> o.direction().equals(Direction.NORTH))
                .findFirst()
                .orElseThrow();
        Collection<DungeonPiece> expected = List.of(HALL_1_1_1.withTransform(MapTransform.translation(1, 1, 3)));


        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.randomFromList(anyList(), any()))
                    .thenReturn(entrance, exit);
            Collection<DungeonPiece> actual = NewAStar.generatePath(base, toConnect, pieces, random);
            assertThat(actual)
                    .containsExactlyInAnyOrderElementsOf(expected);
        }
    }
}
