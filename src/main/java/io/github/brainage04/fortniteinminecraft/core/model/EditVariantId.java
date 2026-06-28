package io.github.brainage04.fortniteinminecraft.core.model;

import java.util.Locale;
import java.util.Objects;

public record EditVariantId(String value) {
    public static final EditVariantId BASE = new EditVariantId("base");

    public EditVariantId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("edit variant id cannot be blank");
        }
        value = value.toLowerCase(Locale.ROOT);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                continue;
            }
            throw new IllegalArgumentException("edit variant id contains unsupported character: " + c);
        }
    }
}
