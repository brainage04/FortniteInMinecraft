package io.github.brainage04.fortniteinminecraft.core.edit;

import io.github.brainage04.fortniteinminecraft.core.model.BlockOffset;
import io.github.brainage04.fortniteinminecraft.core.model.EditVariantId;
import io.github.brainage04.fortniteinminecraft.core.model.PieceType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class BuildEditGrids {
    private static final int WALL_COLUMNS = 3;
    private static final int WALL_ROWS = 3;
    private static final int FLAT_COLUMNS = 2;
    private static final int FLAT_ROWS = 2;

    private BuildEditGrids() {
    }

    public static int columns(PieceType pieceType) {
        Objects.requireNonNull(pieceType, "pieceType");
        return pieceType == PieceType.WALL ? WALL_COLUMNS : FLAT_COLUMNS;
    }

    public static int rows(PieceType pieceType) {
        Objects.requireNonNull(pieceType, "pieceType");
        return pieceType == PieceType.WALL ? WALL_ROWS : FLAT_ROWS;
    }

    public static int cellCount(PieceType pieceType) {
        return columns(pieceType) * rows(pieceType);
    }

    public static int validMask(PieceType pieceType) {
        return (1 << cellCount(pieceType)) - 1;
    }

    public static int bit(PieceType pieceType, EditGridCell cell) {
        Objects.requireNonNull(cell, "cell");
        int columns = columns(pieceType);
        if (cell.column() >= columns || cell.row() >= rows(pieceType)) {
            throw new IllegalArgumentException("cell outside edit grid");
        }
        return 1 << (cell.row() * columns + cell.column());
    }

    public static int toggle(PieceType pieceType, int mask, EditGridCell cell) {
        requireSupportedMask(pieceType, mask);
        return mask ^ bit(pieceType, cell);
    }

    public static boolean isSupportedMask(PieceType pieceType, int mask) {
        return (mask & ~validMask(pieceType)) == 0;
    }

    public static boolean isConfirmableMask(PieceType pieceType, int mask) {
        return isSupportedMask(pieceType, mask) && mask != validMask(pieceType);
    }

    public static EditVariantId variantFor(PieceType pieceType, int mask) {
        Objects.requireNonNull(pieceType, "pieceType");
        requireSupportedMask(pieceType, mask);
        if (mask == 0) {
            return EditVariantId.BASE;
        }
        return new EditVariantId(prefix(pieceType) + bits(pieceType, mask));
    }

    public static Optional<Integer> maskForVariant(PieceType pieceType, EditVariantId variant) {
        Objects.requireNonNull(pieceType, "pieceType");
        Objects.requireNonNull(variant, "variant");
        if (EditVariantId.BASE.equals(variant)) {
            return Optional.of(0);
        }

        String value = variant.value();
        String prefix = prefix(pieceType);
        if (value.startsWith(prefix)) {
            String bits = value.substring(prefix.length());
            int cellCount = cellCount(pieceType);
            if (bits.length() != cellCount) {
                return Optional.empty();
            }
            int mask = 0;
            for (int i = 0; i < bits.length(); i++) {
                char c = bits.charAt(i);
                if (c == '1') {
                    mask |= 1 << i;
                } else if (c != '0') {
                    return Optional.empty();
                }
            }
            return Optional.of(mask);
        }

        String legacy = pieceType.name().toLowerCase(Locale.ROOT) + "_edited";
        if (legacy.equals(value)) {
            return Optional.of(defaultLegacyMask(pieceType));
        }
        return Optional.empty();
    }

    public static Optional<EditGridCell> cellAtLocal(
            PieceType pieceType,
            double localX,
            double localY,
            double localZ,
            int tileSize,
            int wallHeight
    ) {
        Objects.requireNonNull(pieceType, "pieceType");
        if (tileSize <= 0 || wallHeight <= 0) {
            throw new IllegalArgumentException("tileSize and wallHeight must be positive");
        }
        if (pieceType == PieceType.WALL) {
            if (localX < 0.0D || localX >= tileSize || localY < 0.0D || localY >= wallHeight) {
                return Optional.empty();
            }
            int column = coordinateToCell(localX, tileSize, WALL_COLUMNS);
            int bottomRow = coordinateToCell(localY, wallHeight, WALL_ROWS);
            return Optional.of(new EditGridCell(column, WALL_ROWS - 1 - bottomRow));
        }

        if (localX < 0.0D || localX >= tileSize || localZ < 0.0D || localZ >= tileSize) {
            return Optional.empty();
        }
        return Optional.of(new EditGridCell(
                coordinateToCell(localX, tileSize, FLAT_COLUMNS),
                coordinateToCell(localZ, tileSize, FLAT_ROWS)
        ));
    }

    public static boolean keepsBlock(PieceType pieceType, EditVariantId variant, BlockOffset offset, int tileSize, int wallHeight) {
        Objects.requireNonNull(offset, "offset");
        Optional<Integer> mask = maskForVariant(pieceType, variant);
        if (mask.isEmpty() || mask.get() == 0) {
            return true;
        }
        int cellIndex = blockCellIndex(pieceType, offset, tileSize, wallHeight);
        return cellIndex < 0 || (mask.get() & (1 << cellIndex)) == 0;
    }

    public static String label(PieceType pieceType, int mask) {
        requireSupportedMask(pieceType, mask);
        if (mask == 0) {
            return "base";
        }
        if (pieceType == PieceType.WALL) {
            if (mask == bit(pieceType, new EditGridCell(1, 1))) {
                return "center window";
            }
            if (mask == (bit(pieceType, new EditGridCell(1, 1)) | bit(pieceType, new EditGridCell(1, 2)))) {
                return "center door";
            }
            int topRow = bit(pieceType, new EditGridCell(0, 0))
                    | bit(pieceType, new EditGridCell(1, 0))
                    | bit(pieceType, new EditGridCell(2, 0));
            if (mask == topRow) {
                return "half wall";
            }
            return "wall selection " + bits(pieceType, mask);
        }

        int selected = Integer.bitCount(mask);
        if (pieceType == PieceType.FLOOR) {
            if (selected == 1) {
                return "corner floor";
            }
            if (selected == 2) {
                if (isDiagonalFlatMask(mask)) {
                    return "diagonal floor";
                }
                return "half floor";
            }
            if (selected == 3) {
                return "floor opening";
            }
        }
        return pieceType.name().toLowerCase(Locale.ROOT) + " selection " + bits(pieceType, mask);
    }

    private static int blockCellIndex(PieceType pieceType, BlockOffset offset, int tileSize, int wallHeight) {
        if (pieceType == PieceType.WALL) {
            if (offset.x() < 0 || offset.x() >= tileSize || offset.y() < 0 || offset.y() >= wallHeight) {
                return -1;
            }
            int column = offset.x() * WALL_COLUMNS / tileSize;
            int bottomRow = offset.y() * WALL_ROWS / wallHeight;
            int row = WALL_ROWS - 1 - bottomRow;
            return row * WALL_COLUMNS + column;
        }
        if (offset.x() < 0 || offset.x() >= tileSize || offset.z() < 0 || offset.z() >= tileSize) {
            return -1;
        }
        int column = offset.x() * FLAT_COLUMNS / tileSize;
        int row = offset.z() * FLAT_ROWS / tileSize;
        return row * FLAT_COLUMNS + column;
    }

    private static int coordinateToCell(double value, int extent, int cells) {
        int cell = (int) Math.floor(value * cells / extent);
        return Math.clamp(cell, 0, cells - 1);
    }

    private static void requireSupportedMask(PieceType pieceType, int mask) {
        if (!isSupportedMask(pieceType, mask)) {
            throw new IllegalArgumentException("edit mask outside " + pieceType + " grid");
        }
    }

    private static String prefix(PieceType pieceType) {
        return pieceType.name().toLowerCase(Locale.ROOT) + "_edit_";
    }

    private static String bits(PieceType pieceType, int mask) {
        int cellCount = cellCount(pieceType);
        StringBuilder builder = new StringBuilder(cellCount);
        for (int i = 0; i < cellCount; i++) {
            builder.append((mask & (1 << i)) == 0 ? '0' : '1');
        }
        return builder.toString();
    }

    private static int defaultLegacyMask(PieceType pieceType) {
        return switch (pieceType) {
            case WALL -> bit(pieceType, new EditGridCell(1, 1));
            case FLOOR, ROOF, STAIR -> bit(pieceType, new EditGridCell(0, 0));
        };
    }

    private static boolean isDiagonalFlatMask(int mask) {
        return mask == 0b1001 || mask == 0b0110;
    }
}
