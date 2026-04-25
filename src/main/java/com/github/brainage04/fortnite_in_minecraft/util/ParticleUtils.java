package com.github.brainage04.fortnite_in_minecraft.util;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;

import java.util.List;

public class ParticleUtils {
    public static <T extends ParticleEffect> void createParticles(ServerWorld world, List<ServerPlayerEntity> players, T type, Position pos, int amount, double speed) {
        for (ServerPlayerEntity player : players) {
            world.spawnParticles(
                    player,
                    type,
                    true,
                    true,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    amount,
                    0,
                    0,
                    0,
                    speed
            );
        }
    }

    public static <T extends ParticleEffect> void createParticles(ServerWorld world, List<ServerPlayerEntity> players, T type, BlockPos pos, int amount, double speed) {
        for (ServerPlayerEntity player : players) {
            world.spawnParticles(
                    player,
                    type,
                    true,
                    true,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    amount,
                    0,
                    0,
                    0,
                    speed
            );
        }
    }
}
