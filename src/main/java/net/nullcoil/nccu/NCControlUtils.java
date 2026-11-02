package net.nullcoil.nccu;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import net.nullcoil.nccu.config.NCControlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NCControlUtils implements ModInitializer {
	public static final String MOD_ID = "nccontrolutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Map<UUID, Boolean> lastOnGroundMap = new HashMap<>();

	@Override
	public void onInitialize() {
		NCControlConfig.load();
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
