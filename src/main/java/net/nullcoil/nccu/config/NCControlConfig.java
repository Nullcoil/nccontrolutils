package net.nullcoil.nccu.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.nullcoil.nccu.NCControlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NCControlConfig {
    public enum SprintMode {
        HOLD_TO_START,
        HOLD,
        TOGGLE;

        public SprintMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String label() {
            return switch (this) {
                case HOLD_TO_START -> "Sprint: Hold To Start";
                case HOLD -> "Sprint: Hold";
                case TOGGLE -> "Sprint: Toggle";
            };
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nccontrolutils.json");

    public static SprintMode sprintMode = SprintMode.HOLD_TO_START;
    public static boolean elytraSwap = true;

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try {
            Data data = GSON.fromJson(Files.readString(PATH), Data.class);
            if (data == null) return;
            if (data.sprintMode != null) sprintMode = data.sprintMode;
            elytraSwap = data.elytraSwap;
        } catch (Exception e) {
            NCControlUtils.LOGGER.error("Failed to load config", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(new Data(sprintMode, elytraSwap)));
        } catch (IOException e) {
            NCControlUtils.LOGGER.error("Failed to save config", e);
        }
    }

    private static class Data {
        SprintMode sprintMode = SprintMode.HOLD_TO_START;
        boolean elytraSwap = true;

        Data() {}

        Data(SprintMode sprintMode, boolean elytraSwap) {
            this.sprintMode = sprintMode;
            this.elytraSwap = elytraSwap;
        }
    }
}