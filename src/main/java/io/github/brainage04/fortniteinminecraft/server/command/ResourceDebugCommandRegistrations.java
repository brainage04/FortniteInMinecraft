package io.github.brainage04.fortniteinminecraft.server.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.server.item.AmmoType;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceState;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStateSync;
import io.github.brainage04.fortniteinminecraft.server.player.PlayerResourceStates;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceDebugDisplays;
import io.github.brainage04.fortniteinminecraft.server.world.TerrainResourceHarvest;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.label;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.materialArgument;
import static io.github.brainage04.fortniteinminecraft.server.command.CommandRegistrationHelpers.suggestEnum;

final class ResourceDebugCommandRegistrations {
    private static final int DEFAULT_LOCATE_RADIUS = 256;
    private static final int DEFAULT_DEBUG_RADIUS = 96;
    private static final int MAX_DEBUG_MARKERS = 128;
    private static final DynamicCommandExceptionType INVALID_AMMO = new DynamicCommandExceptionType(
            input -> Component.literal("Unknown ammo type: " + input));

    private ResourceDebugCommandRegistrations() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("resource")
                .then(Commands.literal("locate")
                        .executes(context -> locate(context, DEFAULT_LOCATE_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4096))
                                .executes(context -> locate(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("debug")
                        .executes(context -> showDebug(context, DEFAULT_DEBUG_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
                                .executes(context -> showDebug(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("clear_debug")
                        .executes(ResourceDebugCommandRegistrations::clearDebug))
                .then(materialCommand("material"))
                .then(materialCommand("materials"))
                .then(ammoCommand())
                .then(infiniteAlias("infinite_materials", true))
                .then(infiniteAlias("infinite_ammo", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> materialCommand(String literal) {
        return Commands.literal(literal)
                .then(Commands.literal("set")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .suggests((context, builder) -> suggestEnum(MaterialType.values(), builder))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, PlayerResourceState.MAX_MATERIAL))
                                        .executes(ResourceDebugCommandRegistrations::setMaterial))))
                .then(Commands.literal("add")
                        .then(Commands.argument("material", StringArgumentType.word())
                                .suggests((context, builder) -> suggestEnum(MaterialType.values(), builder))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, PlayerResourceState.MAX_MATERIAL))
                                        .executes(ResourceDebugCommandRegistrations::addMaterial))))
                .then(Commands.literal("clear")
                        .executes(ResourceDebugCommandRegistrations::clearMaterials)
                        .then(Commands.argument("material", StringArgumentType.word())
                                .suggests((context, builder) -> suggestEnum(MaterialType.values(), builder))
                                .executes(ResourceDebugCommandRegistrations::clearMaterial)))
                .then(Commands.literal("infinite")
                        .executes(ResourceDebugCommandRegistrations::toggleInfiniteMaterials)
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ResourceDebugCommandRegistrations::setInfiniteMaterials)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ammoCommand() {
        return Commands.literal("ammo")
                .then(Commands.literal("set")
                        .then(Commands.argument("ammo", StringArgumentType.word())
                                .suggests(ResourceDebugCommandRegistrations::suggestAmmo)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, PlayerResourceState.MAX_AMMO))
                                        .executes(ResourceDebugCommandRegistrations::setAmmo))))
                .then(Commands.literal("add")
                        .then(Commands.argument("ammo", StringArgumentType.word())
                                .suggests(ResourceDebugCommandRegistrations::suggestAmmo)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, PlayerResourceState.MAX_AMMO))
                                        .executes(ResourceDebugCommandRegistrations::addAmmo))))
                .then(Commands.literal("clear")
                        .executes(ResourceDebugCommandRegistrations::clearAmmo)
                        .then(Commands.argument("ammo", StringArgumentType.word())
                                .suggests(ResourceDebugCommandRegistrations::suggestAmmo)
                                .executes(ResourceDebugCommandRegistrations::clearAmmoType)))
                .then(Commands.literal("infinite")
                        .executes(ResourceDebugCommandRegistrations::toggleInfiniteAmmo)
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ResourceDebugCommandRegistrations::setInfiniteAmmo)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> infiniteAlias(String literal, boolean materials) {
        return Commands.literal(literal)
                .executes(context -> materials
                        ? toggleInfiniteMaterials(context)
                        : toggleInfiniteAmmo(context))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> materials
                                ? setInfiniteMaterials(context)
                                : setInfiniteAmmo(context)));
    }

    private static int locate(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        return TerrainResourceHarvest.nearest(level, origin, radius)
                .map(block -> {
                    context.getSource().sendSuccess(() -> Component.literal("Nearest harvestable terrain: "
                            + label(block.material())
                            + " at " + format(block.pos())
                            + " (" + Math.round(Math.sqrt(block.pos().distSqr(origin))) + " blocks, "
                            + block.health() + "/" + block.maxHealth() + " hp)."), false);
                    return 1;
                })
                .orElseGet(() -> {
                    context.getSource().sendFailure(Component.literal("No harvestable terrain within " + radius + " blocks."));
                    return 0;
                });
    }

    private static int showDebug(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        List<TerrainResourceHarvest.HarvestableBlock> nearby =
                TerrainResourceHarvest.harvestablesNear(level, origin, radius, MAX_DEBUG_MARKERS);
        int shown = TerrainResourceDebugDisplays.show(level, nearby);
        int finalShown = shown;
        context.getSource().sendSuccess(() -> Component.literal("Showing " + finalShown
                + " see-through terrain resource debug markers for "
                + (TerrainResourceDebugDisplays.DEFAULT_LIFETIME_TICKS / 20) + "s."), false);
        return shown;
    }

    private static int clearDebug(CommandContext<CommandSourceStack> context) {
        TerrainResourceDebugDisplays.clearAll();
        context.getSource().sendSuccess(() -> Component.literal("Cleared terrain resource debug markers."), false);
        return 1;
    }

    private static int setMaterial(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MaterialType material = materialArgument(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        PlayerResourceStates.stateFor(player).setMaterial(material, amount);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Set " + label(material) + " materials to " + amount + "."), true);
        return 1;
    }

    private static int addMaterial(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MaterialType material = materialArgument(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int accepted = PlayerResourceStates.stateFor(player).addMaterial(material, amount);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Added " + accepted + " " + label(material) + " materials."), true);
        return 1;
    }

    private static int clearMaterial(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MaterialType material = materialArgument(context);
        PlayerResourceStates.stateFor(player).clearMaterial(material);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Cleared " + label(material) + " materials."), true);
        return 1;
    }

    private static int clearMaterials(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerResourceStates.stateFor(player).clearMaterials();
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Cleared all material counts."), true);
        return 1;
    }

    private static int toggleInfiniteMaterials(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerResourceState state = PlayerResourceStates.stateFor(player);
        return setInfiniteMaterials(context, player, state, !state.infiniteMaterials());
    }

    private static int setInfiniteMaterials(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return setInfiniteMaterials(context, player, PlayerResourceStates.stateFor(player), BoolArgumentType.getBool(context, "enabled"));
    }

    private static int setInfiniteMaterials(
            CommandContext<CommandSourceStack> context,
            ServerPlayer player,
            PlayerResourceState state,
            boolean enabled
    ) {
        state.setInfiniteMaterials(enabled);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Infinite materials: " + enabled + "."), true);
        return enabled ? 1 : 0;
    }

    private static int setAmmo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        AmmoType type = ammoArgument(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        PlayerResourceStates.stateFor(player).setAmmo(type, amount);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Set " + label(type) + " ammo to " + amount + "."), true);
        return 1;
    }

    private static int addAmmo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        AmmoType type = ammoArgument(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int accepted = PlayerResourceStates.stateFor(player).addAmmo(type, amount);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Added " + accepted + " " + label(type) + " ammo."), true);
        return 1;
    }

    private static int clearAmmoType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        AmmoType type = ammoArgument(context);
        PlayerResourceStates.stateFor(player).clearAmmo(type);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Cleared " + label(type) + " ammo."), true);
        return 1;
    }

    private static int clearAmmo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerResourceStates.stateFor(player).clearAmmo();
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Cleared all ammo counts."), true);
        return 1;
    }

    private static int toggleInfiniteAmmo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerResourceState state = PlayerResourceStates.stateFor(player);
        return setInfiniteAmmo(context, player, state, !state.infiniteAmmo());
    }

    private static int setInfiniteAmmo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return setInfiniteAmmo(context, player, PlayerResourceStates.stateFor(player), BoolArgumentType.getBool(context, "enabled"));
    }

    private static int setInfiniteAmmo(
            CommandContext<CommandSourceStack> context,
            ServerPlayer player,
            PlayerResourceState state,
            boolean enabled
    ) {
        state.setInfiniteAmmo(enabled);
        PlayerResourceStateSync.send(player);
        context.getSource().sendSuccess(() -> Component.literal("Infinite ammo: " + enabled + "."), true);
        return enabled ? 1 : 0;
    }

    private static CompletableFuture<Suggestions> suggestAmmo(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (AmmoType type : AmmoType.values()) {
            builder.suggest(label(type));
            builder.suggest(ammoLabel(type));
        }
        return builder.buildFuture();
    }

    private static AmmoType ammoArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context, "ammo");
        String normalized = normalize(raw);
        for (AmmoType type : AmmoType.values()) {
            if (normalize(label(type)).equals(normalized) || normalize(ammoLabel(type)).equals(normalized)) {
                return type;
            }
        }
        throw INVALID_AMMO.create(raw);
    }

    private static String ammoLabel(AmmoType type) {
        return type.label().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
