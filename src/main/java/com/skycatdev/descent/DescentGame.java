package com.skycatdev.descent;

import com.skycatdev.descent.config.DescentConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;

public class DescentGame {
    private final DescentConfig config;
    private final GameSpace gameSpace;
    private final ServerWorld world;

    public DescentGame(DescentConfig config, GameSpace gameSpace, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.world = world;
    }

    public static GameOpenProcedure open(GameOpenContext<DescentConfig> context) {
        DescentConfig config = context.config();
        //noinspection OptionalGetWithoutIsPresent
        ChunkGenerator generator = new DungeonChunkGenerator(context.server().getRegistryManager().getOptionalEntry(BiomeKeys.PLAINS).get());

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(generator)
                .setTimeOfDay(6000);

        return context.openWithWorld(worldConfig, (activity, world) -> {
            DescentGame game = new DescentGame(config, activity.getGameSpace(), world);

            activity.deny(GameRuleType.FALL_DAMAGE);
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, game::onAccept);
            activity.listen(GamePlayerEvents.JOIN, game::onJoin);
        });
    }

    private void onJoin(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
    }

    private JoinAcceptorResult onAccept(JoinAcceptor acceptor) {
        return acceptor.teleport(world, new Vec3d(0, 65, 0));
    }

}
