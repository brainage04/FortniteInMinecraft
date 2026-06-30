package io.github.brainage04.fortniteinminecraft.server.world;

import com.mojang.math.Transformation;
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
    public static final double MAX_MARKER_DISTANCE_BLOCKS = 20.0D;
    static final int MARKER_LIFETIME_TICKS = 18;
    private static final double MARKER_RISE_PER_TICK = 0.035D;
    public static final float MIN_MARKER_SCALE = 0.05F;
    public static final float MAX_MARKER_SCALE = 8.0F;
    private static final float DEFAULT_MARKER_NEAR_SCALE = 2.0F;
    private static final float DEFAULT_MARKER_FAR_SCALE = 4.0F;
    private static final int TRANSPARENT_BACKGROUND = 0;
    private static final int TEXT_LINE_WIDTH = 80;
    private static final EntityType<Display.TextDisplay> TEXT_DISPLAY_TYPE = textDisplayType();
    private static final List<ActiveMarker> ACTIVE_MARKERS = new ArrayList<>();
    private static volatile ScaleModel scaleModel = ScaleModel.defaults();
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
        show(level, shooter, target, damage, false, target != null && target.getAbsorptionAmount() > 0.0F);
    }

    public static void show(
            ServerLevel level,
            ServerPlayer shooter,
            LivingEntity target,
            float damage,
            boolean headshot,
            boolean targetShielded
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(shooter, "shooter");
        Objects.requireNonNull(target, "target");
        if (damage <= 0.0F) {
            return;
        }

        Vec3 origin = markerPosition(shooter, target);
        float scale = scaleAtDistance(shooter.getEyePosition().distanceTo(origin));
        Display.TextDisplay display = new Display.TextDisplay(TEXT_DISPLAY_TYPE, level);
        display.setNoGravity(true);
        display.setPos(origin.x(), origin.y(), origin.z());
        configureDisplay(display, Component.literal(format(damage)).withStyle(markerColor(headshot, targetShielded)), scale);
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

    public static ScaleModel scaleModel() {
        return scaleModel;
    }

    public static ScaleModel setScaleModel(float nearScale, float farScale) {
        ScaleModel model = new ScaleModel(nearScale, farScale);
        scaleModel = model;
        return model;
    }

    public static ScaleModel resetScaleModel() {
        scaleModel = ScaleModel.defaults();
        return scaleModel;
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

    static ChatFormatting markerColor(boolean headshot, boolean targetShielded) {
        if (targetShielded) {
            return ChatFormatting.BLUE;
        }
        return headshot ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
    }

    private static void configureDisplay(Display.TextDisplay display, Component text, float markerScale) {
        display.setText(text);
        display.setLineWidth(TEXT_LINE_WIDTH);
        display.setBackgroundColor(TRANSPARENT_BACKGROUND);
        display.setTextOpacity((byte) 255);
        display.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));

        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        display.setBrightnessOverride(Brightness.FULL_BRIGHT);
        display.setViewRange(0.5F);
        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);
        display.setWidth(1.0F);
        display.setHeight(0.5F);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(markerScale, markerScale, markerScale),
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
            marker.display().setTextOpacity(opacity(age));
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


    static float scaleAtDistance(double distanceBlocks) {
        return scaleModel.scaleAtDistance(distanceBlocks);
    }

    public record ScaleModel(float nearScale, float farScale) {
        public ScaleModel {
            validateScale(nearScale, "nearScale");
            validateScale(farScale, "farScale");
        }

        public static ScaleModel defaults() {
            return new ScaleModel(DEFAULT_MARKER_NEAR_SCALE, DEFAULT_MARKER_FAR_SCALE);
        }

        public float scaleAtDistance(double distanceBlocks) {
            double clampedDistance = Double.isFinite(distanceBlocks)
                    ? Math.max(0.0D, Math.min(MAX_MARKER_DISTANCE_BLOCKS, distanceBlocks))
                    : MAX_MARKER_DISTANCE_BLOCKS;
            double progress = clampedDistance / MAX_MARKER_DISTANCE_BLOCKS;
            return (float) (nearScale + (farScale - nearScale) * progress);
        }

        private static void validateScale(float scale, String name) {
            if (!Float.isFinite(scale) || scale < MIN_MARKER_SCALE || scale > MAX_MARKER_SCALE) {
                throw new IllegalArgumentException(name + " must be between "
                        + MIN_MARKER_SCALE + " and " + MAX_MARKER_SCALE);
            }
        }
    }

    private record ActiveMarker(String dimension, Display.TextDisplay display, Vec3 origin, long spawnTick) {
        private ActiveMarker {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(display, "display");
            Objects.requireNonNull(origin, "origin");
        }
    }
}
