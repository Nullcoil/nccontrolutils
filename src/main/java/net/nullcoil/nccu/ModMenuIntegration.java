package net.nullcoil.nccu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.nullcoil.nccu.config.NCControlConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new Screen(Component.literal("NC Control Utils")) {
            @Override
            protected void init() {
                int x = this.width / 2 - 100;
                int y = this.height / 2 - 24;

                addRenderableWidget(Button.builder(
                        Component.literal(NCControlConfig.sprintMode.label()),
                        button -> {
                            NCControlConfig.sprintMode = NCControlConfig.sprintMode.next();
                            NCControlConfig.save();
                            button.setMessage(Component.literal(NCControlConfig.sprintMode.label()));
                        }
                ).bounds(x, y, 200, 20).build());

                addRenderableWidget(Button.builder(
                        Component.literal("Elytra Swap: " + (NCControlConfig.elytraSwap ? "ON" : "OFF")),
                        button -> {
                            NCControlConfig.elytraSwap = !NCControlConfig.elytraSwap;
                            NCControlConfig.save();
                            button.setMessage(Component.literal(
                                    "Elytra Swap: " + (NCControlConfig.elytraSwap ? "ON" : "OFF")));
                        }
                ).bounds(x, y + 24, 200, 20).build());

                addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                    if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
                }).bounds(x, y + 56, 200, 20).build());
            }
        };
    }
}