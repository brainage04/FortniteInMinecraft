package io.github.brainage04.fortniteinminecraft.server.world;

import com.mojang.math.Transformation;
import io.github.brainage04.fortniteinminecraft.mixin.DisplayAccessor;
import io.github.brainage04.fortniteinminecraft.mixin.TextDisplayAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class HitMarkerDisplays {
    static final double MAX_MARKER_DISTANCE_BLOCKS = 20.0D;
    static final int MARKER_LIFETIME_TICKS = 18;
    private static final double MARKER_RISE_PER_TICK = 0.035D;
    private static final float MARKER_SCALE = 0.032F;
    private static final int TRANSPARENT_BACKGROUND = 0;
    private static final int TEXT_LINE_WIDTH = 80;
    private static final EntityType<Display.TextDisplay> TEXT_DISPLAY_TYPE = textDisplayType();
    private static final List<ActiveMarker> ACTIVE_MARKERS = new ArrayList<>();
    private static boolean registered;

    private HitMarkerDisplays() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_LEVEL_TICK.register(HitMarkerDisplays::tickLevel);
        registered = true;
    }

    public static void show(ServerLevel level, ServerPlayer shooter, LivingEntity target, float damage) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(shooter, "shooter");
        Objects.requireNonNull(target, "target");
        if (damage <= 0.0F) {
            return;
        }

        Vec3 origin = markerPosition(shooter, target);
        Display.TextDisplay display = new Display.TextDisplay(TEXT_DISPLAY_TYPE, level);
        display.setNoGravity(true);
        display.setPos(origin.x(), origin.y(), origin.z());
        configureDisplay(display, Component.literal(format(damage)).withStyle(ChatFormatting.YELLOW));
        if (!level.addFreshEntity(display)) {
            display.discard();
            return;
        }
        ACTIVE_MARKERS.add(new ActiveMarker(level.dimension().identifier().toString(), display, origin, level.getGameTime()));
    }

    public static void clearAll() {
        ACTIVE_MARKERS.forEach(marker -> marker.display().discard());
        ACTIVE_MARKERS.clear();
    }

    static Vec3 clampedMarkerPosition(Vec3 shooterEye, Vec3 targetCenter) {
        Objects.requireNonNull(shooterEye, "shooterEye");
        Objects.requireNonNull(targetCenter, "targetCenter");
        Vec3 offset = targetCenter.subtract(shooterEye);
        double distance = offset.length();
        if (distance <= MAX_MARKER_DISTANCE_BLOCKS || distance <= 0.0D) {
            return targetCenter;
        }
        return shooterEye.add(offset.scale(MAX_MARKER_DISTANCE_BLOCKS / distance));
    }

    private static Vec3 markerPosition(ServerPlayer shooter, LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter().add(0.0D, target.getBbHeight() * 0.12D, 0.0D);
        return clampedMarkerPosition(shooter.getEyePosition(), targetCenter);
    }

    private static void configureDisplay(Display.TextDisplay display, Component text) {
        TextDisplayAccessor textAccessor = (TextDisplayAccessor) display;
        textAccessor.fortniteinminecraft$setText(text);
        textAccessor.fortniteinminecraft$setLineWidth(TEXT_LINE_WIDTH);
        textAccessor.fortniteinminecraft$setBackgroundColor(TRANSPARENT_BACKGROUND);
        textAccessor.fortniteinminecraft$setTextOpacity((byte) 255);
        textAccessor.fortniteinminecraft$setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));

        DisplayAccessor displayAccessor = (DisplayAccessor) display;
        displayAccessor.fortniteinminecraft$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        displayAccessor.fortniteinminecraft$setBrightnessOverride(Brightness.FULL_BRIGHT);
        displayAccessor.fortniteinminecraft$setViewRange(0.5F);
        displayAccessor.fortniteinminecraft$setShadowRadius(0.0F);
        displayAccessor.fortniteinminecraft$setShadowStrength(0.0F);
        displayAccessor.fortniteinminecraft$setWidth(1.0F);
        displayAccessor.fortniteinminecraft$setHeight(0.5F);
        displayAccessor.fortniteinminecraft$setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(MARKER_SCALE, MARKER_SCALE, MARKER_SCALE),
                new Quaternionf()
        ));
    }

    private static void tickLevel(ServerLevel level) {
        String dimension = level.dimension().identifier().toString();
        long tick = level.getGameTime();
        Iterator<ActiveMarker> iterator = ACTIVE_MARKERS.iterator();
        while (iterator.hasNext()) {
            ActiveMarker marker = iterator.next();
            if (!marker.dimension().equals(dimension)) {
                continue;
            }
            int age = (int) (tick - marker.spawnTick());
            if (age >= MARKER_LIFETIME_TICKS || marker.display().isRemoved()) {
                marker.display().discard();
                iterator.remove();
                continue;
            }
            marker.display().setPos(marker.origin().add(0.0D, age * MARKER_RISE_PER_TICK, 0.0D));
            ((TextDisplayAccessor) marker.display()).fortniteinminecraft$setTextOpacity(opacity(age));
        }
    }

    private static byte opacity(int age) {
        int fadeStart = MARKER_LIFETIME_TICKS / 2;
        if (age <= fadeStart) {
            return (byte) 255;
        }
        int fadeTicks = Math.max(1, MARKER_LIFETIME_TICKS - fadeStart);
        int remaining = Math.max(0, MARKER_LIFETIME_TICKS - age);
        return (byte) Math.round(255.0F * remaining / fadeTicks);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Display.TextDisplay> textDisplayType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("text_display"));
        if (type == null) {
            throw new IllegalStateException("missing minecraft:text_display entity type");
        }
        return (EntityType<Display.TextDisplay>) type;
    }

    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.1f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private record ActiveMarker(String dimension, Display.TextDisplay display, Vec3 origin, long spawnTick) {
        private ActiveMarker {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(display, "display");
            Objects.requireNonNull(origin, "origin");
        }
    }
}
