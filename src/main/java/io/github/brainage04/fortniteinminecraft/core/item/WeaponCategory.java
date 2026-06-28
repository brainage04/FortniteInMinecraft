package io.github.brainage04.fortniteinminecraft.core.item;

public enum WeaponCategory {
    ASSAULT_RIFLE("Assault Rifle"),
    SHOTGUN("Shotgun"),
    SMG("SMG"),
    PISTOL("Pistol"),
    SNIPER("Sniper");

    private final String label;

    WeaponCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
