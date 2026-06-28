package io.github.brainage04.fortniteinminecraft.server.player;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Objects;

public final class PlayerMovementTuning {
    static final double STEP_HEIGHT_BONUS = 0.65D;
    static final double JUMP_STRENGTH_BONUS = 0.08D;
    static final double MOVEMENT_SPEED_MULTIPLIER = 0.35D;

    private static final Identifier STEP_HEIGHT_ID = id("build_step_height");
    private static final Identifier JUMP_STRENGTH_ID = id("build_jump_strength");
    private static final Identifier MOVEMENT_SPEED_ID = id("build_movement_speed");

    private PlayerMovementTuning() {
    }

    public static void apply(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        addOrUpdate(player.getAttribute(Attributes.STEP_HEIGHT), STEP_HEIGHT_ID, STEP_HEIGHT_BONUS, AttributeModifier.Operation.ADD_VALUE);
        addOrUpdate(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_STRENGTH_ID, JUMP_STRENGTH_BONUS, AttributeModifier.Operation.ADD_VALUE);
        addOrUpdate(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_ID, MOVEMENT_SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void clear(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        remove(player.getAttribute(Attributes.STEP_HEIGHT), STEP_HEIGHT_ID);
        remove(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_STRENGTH_ID);
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_ID);
    }

    private static void addOrUpdate(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute != null) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void remove(AttributeInstance attribute, Identifier id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path);
    }
}
