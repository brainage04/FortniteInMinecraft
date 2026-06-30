package io.github.brainage04.fortniteinminecraft.network;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class FortnitePayloads {
    private static boolean registered;

    private FortnitePayloads() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        PayloadTypeRegistry.serverboundPlay().register(ClientActionPayload.TYPE, ClientActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EditModePayload.TYPE, EditModePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ResourceStatePayload.TYPE, ResourceStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BuildPreviewPayload.TYPE, BuildPreviewPayload.CODEC);
        registered = true;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FortniteInMinecraft.MOD_ID, path);
    }

    public enum ClientAction {
        PRIMARY,
        SECONDARY,
        RELOAD,
        EDIT,
        GLIDER_TOGGLE
    }

    public record ClientActionPayload(ClientAction action, boolean pressed) implements CustomPacketPayload {
        public static final Type<ClientActionPayload> TYPE = new Type<>(id("client_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientActionPayload> CODEC = StreamCodec.ofMember(
                ClientActionPayload::write,
                ClientActionPayload::read
        );

        public ClientActionPayload {
            if (action == null) {
                throw new IllegalArgumentException("action cannot be null");
            }
        }

        private static ClientActionPayload read(RegistryFriendlyByteBuf buffer) {
            return new ClientActionPayload(buffer.readEnum(ClientAction.class), buffer.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeEnum(action);
            buffer.writeBoolean(pressed);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record EditModePayload(boolean active) implements CustomPacketPayload {
        public static final Type<EditModePayload> TYPE = new Type<>(id("edit_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EditModePayload> CODEC = StreamCodec.ofMember(
                EditModePayload::write,
                EditModePayload::read
        );

        private static EditModePayload read(RegistryFriendlyByteBuf buffer) {
            return new EditModePayload(buffer.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBoolean(active);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ResourceStatePayload(
            int wood,
            int stone,
            int metal,
            int gold,
            int lightAmmo,
            int mediumAmmo,
            int shells,
            int heavyAmmo,
            int rockets,
            boolean infiniteMaterials,
            boolean infiniteAmmo
    ) implements CustomPacketPayload {
        public static final Type<ResourceStatePayload> TYPE = new Type<>(id("resource_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ResourceStatePayload> CODEC = StreamCodec.ofMember(
                ResourceStatePayload::write,
                ResourceStatePayload::read
        );

        private static ResourceStatePayload read(RegistryFriendlyByteBuf buffer) {
            return new ResourceStatePayload(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(wood);
            buffer.writeVarInt(stone);
            buffer.writeVarInt(metal);
            buffer.writeVarInt(gold);
            buffer.writeVarInt(lightAmmo);
            buffer.writeVarInt(mediumAmmo);
            buffer.writeVarInt(shells);
            buffer.writeVarInt(heavyAmmo);
            buffer.writeVarInt(rockets);
            buffer.writeBoolean(infiniteMaterials);
            buffer.writeBoolean(infiniteAmmo);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BuildPreviewPayload(
            boolean active,
            String dimension,
            int gridX,
            int gridY,
            int gridZ,
            PieceType pieceType,
            Orientation orientation,
            MaterialType material,
            boolean valid
    ) implements CustomPacketPayload {
        public static final Type<BuildPreviewPayload> TYPE = new Type<>(id("build_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuildPreviewPayload> CODEC = StreamCodec.ofMember(
                BuildPreviewPayload::write,
                BuildPreviewPayload::read
        );

        public BuildPreviewPayload {
            if (active) {
                if (dimension == null || dimension.isBlank()) {
                    throw new IllegalArgumentException("dimension cannot be blank for active previews");
                }
            } else {
                dimension = "";
                gridX = 0;
                gridY = 0;
                gridZ = 0;
                pieceType = PieceType.WALL;
                orientation = Orientation.NORTH;
                material = MaterialType.WOOD;
                valid = false;
            }
            if (pieceType == null) {
                throw new IllegalArgumentException("pieceType cannot be null");
            }
            if (orientation == null) {
                throw new IllegalArgumentException("orientation cannot be null");
            }
            if (material == null) {
                throw new IllegalArgumentException("material cannot be null");
            }
        }

        public static BuildPreviewPayload inactive() {
            return new BuildPreviewPayload(false, "", 0, 0, 0, PieceType.WALL, Orientation.NORTH, MaterialType.WOOD, false);
        }

        public static BuildPreviewPayload active(BuildSlot slot, MaterialType material, boolean valid) {
            return new BuildPreviewPayload(
                    true,
                    slot.gridPos().dimension(),
                    slot.gridPos().x(),
                    slot.gridPos().y(),
                    slot.gridPos().z(),
                    slot.pieceType(),
                    slot.orientation(),
                    material,
                    valid
            );
        }

        private static BuildPreviewPayload read(RegistryFriendlyByteBuf buffer) {
            boolean active = buffer.readBoolean();
            if (!active) {
                return inactive();
            }
            return new BuildPreviewPayload(
                    true,
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readEnum(PieceType.class),
                    buffer.readEnum(Orientation.class),
                    buffer.readEnum(MaterialType.class),
                    buffer.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBoolean(active);
            if (!active) {
                return;
            }
            buffer.writeUtf(dimension);
            buffer.writeVarInt(gridX);
            buffer.writeVarInt(gridY);
            buffer.writeVarInt(gridZ);
            buffer.writeEnum(pieceType);
            buffer.writeEnum(orientation);
            buffer.writeEnum(material);
            buffer.writeBoolean(valid);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
