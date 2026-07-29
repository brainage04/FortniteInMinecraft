package io.github.brainage04.fortniteinminecraft.server.player;

public final class GliderState {
    public static final double DEFAULT_MAX_FALL_SPEED = -0.125D;
    public static final double MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS = 35.0D;

    private long redeployUntilTick = -1L;
    private boolean deployed;

    public void enableRedeploy(long currentTick, long durationTicks) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("currentTick cannot be negative");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks cannot be negative");
        }
        redeployUntilTick = Math.max(redeployUntilTick, currentTick + durationTicks);
    }

    public boolean canRedeploy(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick cannot be negative");
        }
        return tick <= redeployUntilTick;
    }

    public boolean canDeploy(long tick, double groundDistanceBlocks) {
        if (groundDistanceBlocks < 0.0D) {
            throw new IllegalArgumentException("groundDistanceBlocks cannot be negative");
        }
        return canRedeploy(tick) || groundDistanceBlocks >= MIN_REDEPLOY_GROUND_DISTANCE_BLOCKS;
    }

    public boolean toggle(long tick, double groundDistanceBlocks) {
        if (deployed) {
            deployed = false;
            return true;
        }
        if (!canDeploy(tick, groundDistanceBlocks)) {
            return false;
        }
        deployed = true;
        return true;
    }

    public void land() {
        deployed = false;
        redeployUntilTick = -1L;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public double fallSpeed(double currentYVelocity) {
        return fallSpeed(currentYVelocity, DEFAULT_MAX_FALL_SPEED);
    }

    public static double fallSpeed(double currentYVelocity, double maxFallSpeed) {
        if (maxFallSpeed > 0.0D) {
            throw new IllegalArgumentException("maxFallSpeed must be zero or negative");
        }
        return Math.max(currentYVelocity, maxFallSpeed);
    }
}
