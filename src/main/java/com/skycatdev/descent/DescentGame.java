package com.skycatdev.descent;

import com.skycatdev.descent.config.DescentConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.Collection;

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

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(new TemplateChunkGenerator(context.server(), buildMapFromIds(context.server(), context.config().rooms())))
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

    private static MapTemplate buildMapFromIds(MinecraftServer server, Collection<Identifier> templates) {
        // Collect all templates
        return buildMapFromTemplates(server, templates.stream()
                .map(id -> {
                    try {
                        return MapTemplateSerializer.loadFromResource(server, id);
                    } catch (IOException e) {
                        throw new RuntimeException(e); // TODO: Maybe loudly fail instead of crashing
                    }
                })
                .toList());
    }

    private static MapTemplate buildMapFromTemplates(MinecraftServer server, Collection<MapTemplate> templates) {
        return null; // TODO
    }
}
