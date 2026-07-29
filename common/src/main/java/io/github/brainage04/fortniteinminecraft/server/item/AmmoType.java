package io.github.brainage04.fortniteinminecraft.server.item;

public enum AmmoType {
    LIGHT("Light Ammo"),
    MEDIUM("Medium Ammo"),
    SHELLS("Shells"),
    HEAVY("Heavy Ammo"),
    ROCKETS("Rockets");

    private final String label;

    AmmoType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
