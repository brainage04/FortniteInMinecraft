package io.github.brainage04.fortniteinminecraft.server.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.item.FortniteRarity;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponDefinition;
import io.github.brainage04.fortniteinminecraft.core.item.WeaponStats;
import io.github.brainage04.fortniteinminecraft.server.player.MobilityItemInteractions;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ProjectileWeaponItem extends SimplePolymerItem {
    private static final double FORTNITE_TO_MINECRAFT_DAMAGE = 0.2D;

    private final WeaponDefinition definition;
    private final Item clientItem;
    private final float projectileSpeed;
    private final float inaccuracy;

    public ProjectileWeaponItem(
            WeaponDefinition definition,
            Item.Properties settings,
            Item clientItem,
            float projectileSpeed,
            float inaccuracy
    ) {
        super(settings, clientItem);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.clientItem = Objects.requireNonNull(clientItem, "clientItem");
        if (projectileSpeed <= 0.0F) {
            throw new IllegalArgumentException("projectileSpeed must be positive");
        }
        if (inaccuracy < 0.0F) {
            throw new IllegalArgumentException("inaccuracy cannot be negative");
        }
        this.projectileSpeed = projectileSpeed;
        this.inaccuracy = inaccuracy;
    }

    public WeaponDefinition definition() {
        return definition;
    }

    public float projectileSpeed() {
        return projectileSpeed;
    }

    static UseCooldown cooldownComponent(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return new UseCooldown(cooldownTicks(definition) / 20.0F, Optional.of(Identifier.fromNamespaceAndPath(
                FortniteInMinecraft.MOD_ID,
                definition.path()
        )));
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return clientItem;
    }

    @Override
    public void modifyBasePolymerItemStack(
            ItemStack out,
            ItemStack stack,
            PacketContext context,
            HolderLookup.Provider registries
    ) {
        out.set(DataComponents.ITEM_NAME, displayNameComponent());
        out.set(DataComponents.USE_COOLDOWN, cooldownComponent(definition));
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayNameComponent();
    }

    @Override
    public void modifyClientTooltip(List<Component> tooltip, ItemStack stack, PacketContext context) {
        WeaponStats stats = definition.stats();
        tooltip.add(Component.literal(definition.category().label() + " / " + definition.rarity().label()));
        tooltip.add(Component.literal("Projectile damage: " + format(stats.damage())
                + "; speed: " + format(projectileSpeed)));
        tooltip.add(Component.literal("Reload/cooldown: " + format(stats.reloadSeconds()) + "s; crit: "
                + format(stats.criticalMultiplier()) + "x"));
        tooltip.add(Component.literal("Sneak before firing to steady the scope."));
        tooltip.add(Component.literal("Source: " + definition.sourceStatRow()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (MobilityItemInteractions.isGliding(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal("Cannot fire while gliding."), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (serverPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        fireArrow(serverLevel, serverPlayer, stack, serverPlayer.isShiftKeyDown());
        int cooldownTicks = cooldownTicks(definition);
        serverPlayer.getCooldowns().addCooldown(stack, cooldownTicks);
        serverPlayer.sendSystemMessage(Component.literal("Fired " + definition.displayName() + "."), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private void fireArrow(ServerLevel level, ServerPlayer shooter, ItemStack weaponStack, boolean scoped) {
        Arrow arrow = new Arrow(level, shooter, new ItemStack(Items.ARROW), weaponStack.copy());
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(minecraftDamage(definition));
        arrow.setCritArrow(true);
        Vec3 look = shooter.getLookAngle();
        arrow.shoot(look.x(), look.y(), look.z(), projectileSpeed, scopedInaccuracy(inaccuracy, scoped));
        level.addFreshEntity(arrow);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
        Vec3 muzzle = WeaponItem.muzzlePosition(shooter, shooter.getEyePosition());
        level.sendParticles(ParticleTypes.CRIT, true, true, muzzle.x(), muzzle.y(), muzzle.z(), 4, 0.08D, 0.08D, 0.08D, 0.0D);
    }

    static int cooldownTicks(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        int fireTicks = WeaponItem.fireDelayTicks(definition);
        int reloadTicks = Math.max(1, (int) Math.round(definition.stats().reloadSeconds() * 20.0D));
        return Math.max(fireTicks, reloadTicks);
    }

    static float minecraftDamage(WeaponDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return (float) Math.max(1.0D, definition.stats().totalDamagePerShot() * FORTNITE_TO_MINECRAFT_DAMAGE);
    }

    static float scopedInaccuracy(float baseInaccuracy, boolean scoped) {
        if (baseInaccuracy < 0.0F) {
            throw new IllegalArgumentException("baseInaccuracy cannot be negative");
        }
        return scoped ? baseInaccuracy * 0.25F : baseInaccuracy;
    }

    private Component displayNameComponent() {
        return Component.literal(definition.displayName()).withStyle(rarityColor(definition.rarity()));
    }

    private static ChatFormatting rarityColor(FortniteRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }

    private static String format(double value) {
        if (Math.rint(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
