package io.github.brainage04.fortniteinminecraft.client;

import io.github.brainage04.hudrendererlib.config.core.CoreSettings;
import io.github.brainage04.hudrendererlib.config.core.ICoreSettingsContainer;
import io.github.brainage04.hudrendererlib.hud.core.CoreHudElement;
import io.github.brainage04.hudrendererlib.util.LayerInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Bridges Fortnite's fixed-layout HUD renderers into HudRendererLib's loader-neutral layer API. */
final class FortniteHudRendererElement implements CoreHudElement<FortniteHudRendererElement.Settings> {
    private final Settings settings;
    private final LayerInfo layerInfo;
    private final BiConsumer<GuiGraphicsExtractor, DeltaTracker> renderer;

    FortniteHudRendererElement(
            String name,
            Identifier anchor,
            boolean before,
            BiConsumer<GuiGraphicsExtractor, DeltaTracker> renderer
    ) {
        this.settings = new Settings(name);
        this.layerInfo = new LayerInfo(anchor, before);
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public Settings getElementConfig() {
        return settings;
    }

    @Override
    public LayerInfo getLayerInfo() {
        return layerInfo;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        renderer.accept(graphics, deltaTracker);
    }

    static final class Settings implements ICoreSettingsContainer {
        private CoreSettings coreSettings;

        private Settings(String name) {
            coreSettings = new CoreSettings(name, true, 0, 0, null);
        }

        @Override
        public CoreSettings getCoreSettings() {
            return coreSettings;
        }

        @Override
        public void setCoreSettings(CoreSettings coreSettings) {
            this.coreSettings = Objects.requireNonNull(coreSettings, "coreSettings");
        }
    }
}
