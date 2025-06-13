package com.skycatdev.descent;

import com.skycatdev.descent.config.DescentConfig;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Descent implements ModInitializer {
	public static final String MOD_ID = "descent";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		GameType.register(
				Identifier.of(MOD_ID, "descent"),
				DescentConfig.CODEC,
				DescentGame::open
		);
	}
}