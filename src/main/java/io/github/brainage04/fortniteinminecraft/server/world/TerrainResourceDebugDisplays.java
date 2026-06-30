package io.github.brainage04.fortniteinminecraft.server.world;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class TerrainResourceDebugDisplays {
    public static final int DEFAULT_LIFETIME_TICKS = 200;
    private static final int TRANSPARENT_BACKGROUND = 0;
    private static final int TEXT_LINE_WIDTH = 120;
    private static final float DISPLAY_SCALE = 0.45F;
    private static final EntityType<Display.TextDisplay> TEXT_DISPLAY_TYPE = textDisplayType();
    private static final List<ActiveDisplay> ACTIVE_DISPLAYS = new ArrayList<>();
    private static boolean registered;

    private TerrainResourceDebugDisplays() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(TerrainResourceDebugDisplays::tickLevel);
        registered = true;
    }

    public static int show(ServerLevel level, Collection<TerrainResourceHarvest.HarvestableBlock> blocks) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(blocks, "blocks");
        int count = 0;
        long expireTick = level.getGameTime() + DEFAULT_LIFETIME_TICKS;
        String dimension = level.dimension().identifier().toString();
        for (TerrainResourceHarvest.HarvestableBlock block : blocks) {
            Display.TextDisplay display = new Display.TextDisplay(TEXT_DISPLAY_TYPE, level);
            display.setNoGravity(true);
            display.setGlowingTag(true);
            display.setPos(block.pos().getX() + 0.5D, block.pos().getY() + 1.35D, block.pos().getZ() + 0.5D);
            configure(display, label(block));
            if (!level.addFreshEntity(display)) {
                display.discard();
                continue;
            }
            ACTIVE_DISPLAYS.add(new ActiveDisplay(dimension, display, expireTick));
            count++;
        }
        return count;
    }

    public static void clearAll() {
        ACTIVE_DISPLAYS.forEach(display -> display.display().discard());
        ACTIVE_DISPLAYS.clear();
    }

    private static Component label(TerrainResourceHarvest.HarvestableBlock block) {
        ChatFormatting color = switch (block.material()) {
            case WOOD -> ChatFormatting.GOLD;
            case STONE -> ChatFormatting.GRAY;
            case METAL -> ChatFormatting.AQUA;
        };
        return Component.literal(block.material().name().toLowerCase(java.util.Locale.ROOT)
                + " " + block.health() + "/" + block.maxHealth() + "hp").withStyle(color);
    }

    private static void configure(Display.TextDisplay display, Component text) {
        display.setText(text);
        display.setLineWidth(TEXT_LINE_WIDTH);
        display.setBackgroundColor(TRANSPARENT_BACKGROUND);
        display.setTextOpacity((byte) 255);
        display.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));
        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        display.setBrightnessOverride(Brightness.FULL_BRIGHT);
        display.setViewRange(1.0F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setWidth(2.0F);
        display.setHeight(0.5F);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                new Quaternionf()
        ));
    }

    private static void tickLevel(ServerLevel level) {
        String dimension = level.dimension().identifier().toString();
        long tick = level.getGameTime();
        Iterator<ActiveDisplay> iterator = ACTIVE_DISPLAYS.iterator();
        while (iterator.hasNext()) {
            ActiveDisplay display = iterator.next();
            if (!display.dimension().equals(dimension)) {
                continue;
            }
            if (tick >= display.expireTick() || display.display().isRemoved()) {
                display.display().discard();
                iterator.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.TextDisplay> textDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("text_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:text_display entity type");
        }
        return (EntityType<Display.TextDisplay>) type;
    }

    private record ActiveDisplay(String dimension, Display.TextDisplay display, long expireTick) {
        private ActiveDisplay {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(display, "display");
        }
    }
}
