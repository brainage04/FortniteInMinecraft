package io.github.brainage04.fortniteinminecraft.server.item;

public final class CombatSettings {
    private static boolean preventBulletKnockback = true;

    private CombatSettings() {
    }

    public static boolean preventBulletKnockback() {
        return preventBulletKnockback;
    }

    public static void setPreventBulletKnockback(boolean prevent) {
        preventBulletKnockback = prevent;
    }
}
