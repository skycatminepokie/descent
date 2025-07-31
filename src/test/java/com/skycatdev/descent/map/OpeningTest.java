package com.skycatdev.descent.map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTransform;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningTest { // TODO: Unconnected tests
    @Test
    void connectedEW() {
        DungeonPiece.Opening a = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.EAST);
        DungeonPiece.Opening b = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN.east()), Direction.WEST);
        assertThat(a.isConnected(b))
                .isTrue();
        assertThat(b.isConnected(a))
                .isTrue();
    }

    @Test
    void connectedUD() {
        DungeonPiece.Opening a = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.UP);
        DungeonPiece.Opening b = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN.up()), Direction.DOWN);
        assertThat(a.isConnected(b))
                .isTrue();
        assertThat(b.isConnected(a))
                .isTrue();
    }

    @Test
    void connectedNS() {
        DungeonPiece.Opening a = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.NORTH);
        DungeonPiece.Opening b = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN.north()), Direction.SOUTH);
        assertThat(a.isConnected(b))
                .isTrue();
        assertThat(b.isConnected(a))
                .isTrue();
    }

    @Test
    void transformedTranslate() {
        // TODO: maybe enumerate a bunch of things?
        DungeonPiece.Opening expected = new DungeonPiece.Opening(BlockBounds.ofBlock(new BlockPos(1, 2, 3)), Direction.UP);
        DungeonPiece.Opening actual = new DungeonPiece.Opening(BlockBounds.ofBlock(BlockPos.ORIGIN), Direction.UP).transformed(MapTransform.translation(1, 2, 3));
        assertThat(actual)
                .isEqualTo(expected);
    }
}
