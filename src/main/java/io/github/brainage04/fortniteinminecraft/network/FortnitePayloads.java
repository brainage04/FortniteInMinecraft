package io.github.brainage04.fortniteinminecraft.network;

import io.github.brainage04.fortniteinminecraft.FortniteInMinecraft;
import io.github.brainage04.fortniteinminecraft.core.model.BuildPieceState;
import io.github.brainage04.fortniteinminecraft.core.model.BuildSlot;
import io.github.brainage04.fortniteinminecraft.core.model.MaterialType;
import io.github.brainage04.fortniteinminecraft.core.model.Orientation;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;
import io.github.brainage04.fortniteinminecraft.platform.LoaderPlatform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class FortnitePayloads {
    private static boolean registered;

    private FortnitePayloads() {
    }

    public static void registerClientbound(LoaderPlatform platform) {
        if (registered) {
            return;
        }

        platform.registerClientboundPayload(EditModePayload.TYPE, EditModePayload.CODEC);
        platform.registerClientboundPayload(ResourceStatePayload.TYPE, ResourceStatePayload.CODEC);
        platform.registerClientboundPayload(BuildPreviewPayload.TYPE, BuildPreviewPayload.CODEC);
        platform.registerClientboundPayload(LootContainerProgressPayload.TYPE, LootContainerProgressPayload.CODEC);
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
        EDIT_RESET,
        SELECT_WALL,
        SELECT_FLOOR,
        SELECT_STAIR,
        SELECT_ROOF,
        DESELECT_BUILD,
        ROTATE_BUILD,
        REPAIR_BUILD,
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

    public record EditModePayload(
            boolean active,
            String dimension,
            int gridX,
            int gridY,
            int gridZ,
            PieceType pieceType,
            Orientation orientation,
            MaterialType material,
            int selectedMask
    ) implements CustomPacketPayload {
        public static final Type<EditModePayload> TYPE = new Type<>(id("edit_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EditModePayload> CODEC = StreamCodec.ofMember(
                EditModePayload::write,
                EditModePayload::read
        );

        public EditModePayload {
            if (active) {
                if (dimension == null || dimension.isBlank()) {
                    throw new IllegalArgumentException("dimension cannot be blank for active edit previews");
                }
            } else {
                dimension = "";
                gridX = 0;
                gridY = 0;
                gridZ = 0;
                pieceType = PieceType.WALL;
                orientation = Orientation.NORTH;
                material = MaterialType.WOOD;
                selectedMask = 0;
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

        public static EditModePayload inactive() {
            return new EditModePayload(false, "", 0, 0, 0, PieceType.WALL, Orientation.NORTH, MaterialType.WOOD, 0);
        }

        public static EditModePayload active(BuildPieceState piece, int selectedMask) {
            BuildSlot slot = piece.slot();
            return new EditModePayload(
                    true,
                    slot.gridPos().dimension(),
                    slot.gridPos().x(),
                    slot.gridPos().y(),
                    slot.gridPos().z(),
                    slot.pieceType(),
                    slot.orientation(),
                    piece.material(),
                    selectedMask
            );
        }

        private static EditModePayload read(RegistryFriendlyByteBuf buffer) {
            boolean active = buffer.readBoolean();
            if (!active) {
                return inactive();
            }
            return new EditModePayload(
                    true,
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readEnum(PieceType.class),
                    buffer.readEnum(Orientation.class),
                    buffer.readEnum(MaterialType.class),
                    buffer.readVarInt()
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
            buffer.writeVarInt(selectedMask);
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

    public record LootContainerProgressPayload(
            boolean active,
            String label,
            int elapsedTicks,
            int totalTicks
    ) implements CustomPacketPayload {
        public static final Type<LootContainerProgressPayload> TYPE = new Type<>(id("loot_container_progress"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LootContainerProgressPayload> CODEC = StreamCodec.ofMember(
                LootContainerProgressPayload::write,
                LootContainerProgressPayload::read
        );

        public LootContainerProgressPayload {
            if (active) {
                if (label == null || label.isBlank()) {
                    throw new IllegalArgumentException("label cannot be blank for active loot container progress");
                }
                if (totalTicks <= 0) {
                    throw new IllegalArgumentException("totalTicks must be positive for active loot container progress");
                }
                elapsedTicks = Math.clamp(elapsedTicks, 0, totalTicks);
            } else {
                label = "";
                elapsedTicks = 0;
                totalTicks = 0;
            }
        }

        public static LootContainerProgressPayload inactive() {
            return new LootContainerProgressPayload(false, "", 0, 0);
        }

        private static LootContainerProgressPayload read(RegistryFriendlyByteBuf buffer) {
            boolean active = buffer.readBoolean();
            if (!active) {
                return inactive();
            }
            return new LootContainerProgressPayload(true, buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBoolean(active);
            if (!active) {
                return;
            }
            buffer.writeUtf(label);
            buffer.writeVarInt(elapsedTicks);
            buffer.writeVarInt(totalTicks);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
