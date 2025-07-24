package com.skycatdev.descent;

import com.skycatdev.descent.config.DescentConfig;
import com.skycatdev.descent.map.DungeonGenerator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

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

        RuntimeWorldConfig worldConfig;
        try {
            worldConfig = new RuntimeWorldConfig()
                    .setGenerator(new TemplateChunkGenerator(context.server(), DungeonGenerator.generate(config.mapConfig(), context.server(), Random.create())))
//                    .setGenerator(new TemplateChunkGenerator(context.server(), MapTemplateSerializer.loadFrom(new FileInputStream(context.server().getRunDirectory().resolve("builtsponge.nbt").toFile()), context.server().getRegistryManager())))
                    .setTimeOfDay(6000);
//        } catch (IOException | NoSolutionException e) {
        } catch (Exception e) {
            throw new RuntimeException(e); // TODO: Don't crash
        }

        return context.openWithWorld(worldConfig, (activity, world) -> {
            DescentGame game = new DescentGame(config, activity.getGameSpace(), world);

            activity.deny(GameRuleType.FALL_DAMAGE);
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, game::onAccept);
            activity.listen(GamePlayerEvents.JOIN, game::onJoin);
        });
    }

    private void onJoin(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SPECTATOR); // TODO WRONG
    }

    private JoinAcceptorResult onAccept(JoinAcceptor acceptor) {
        return acceptor.teleport(world, new Vec3d(0, 0, 0)); // TODO tp to start
    }
}
