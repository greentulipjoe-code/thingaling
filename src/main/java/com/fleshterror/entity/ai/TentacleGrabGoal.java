package com.fleshterror.entity.ai;

import com.fleshterror.entity.FleshMonsterEntity;
import com.fleshterror.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Reaches tentacles out into nearby structures, wrenches a block free, and feeds on it.
 * Range and speed scale with the monster's current growth stage.
 */
public class TentacleGrabGoal extends Goal {

    private final FleshMonsterEntity monster;
    private int cooldown;
    private BlockPos targetBlock;
    private int grabTicks;

    private static final int GRAB_DURATION = 16; // ticks the tentacle takes to pull a block free

    public TentacleGrabGoal(FleshMonsterEntity monster) {
        this.monster = monster;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (monster.getTarget() != null) return false; // prioritize fighting players over snacking on walls
        if (cooldown-- > 0) return false;
        return findNearbyStructureBlock() != null;
    }

    @Override
    public void start() {
        this.targetBlock = findNearbyStructureBlock();
        this.grabTicks = 0;
        int baseCooldown = 60 - monster.getGrowthStage() * 8; // bigger = hungrier, less waiting
        this.cooldown = Math.max(20, baseCooldown);
    }

    @Override
    public boolean canContinueToUse() {
        return targetBlock != null && grabTicks < GRAB_DURATION && monster.getTarget() == null;
    }

    @Override
    public void tick() {
        if (targetBlock == null) return;

        monster.getLookControl().setLookAt(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);
        monster.getNavigation().moveTo(targetBlock.getX() + 0.5, targetBlock.getY(), targetBlock.getZ() + 0.5, 0.7D);

        grabTicks++;

        Level level = monster.level();
        if (level instanceof ServerLevel serverLevel) {
            double px = targetBlock.getX() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 0.6;
            double py = targetBlock.getY() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 0.6;
            double pz = targetBlock.getZ() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 0.6;
            // "tentacle" particle trail pulling toward the block
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, px, py, pz, 2, 0.05, 0.05, 0.05, 0.01);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);

            if (grabTicks == GRAB_DURATION) {
                BlockState state = serverLevel.getBlockState(targetBlock);
                if (!state.isAir()) {
                    serverLevel.destroyBlock(targetBlock, false);
                    serverLevel.playSound(null, targetBlock, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
                    serverLevel.playSound(null, targetBlock, SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.0f, 0.6f);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                            targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5,
                            1, 0.0, 0.0, 0.0, 0.0);
                    monster.addGrowth(1);
                }
            }
        }
    }

    @Override
    public void stop() {
        this.targetBlock = null;
        monster.getNavigation().stop();
    }

    private BlockPos findNearbyStructureBlock() {
        int radius = 5 + monster.getGrowthStage() * 4; // tentacles reach further as it grows
        BlockPos origin = monster.blockPosition();
        Level level = monster.level();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -3; dy <= radius / 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // sparse scan for performance: skip most interior points, check a shell-ish pattern
                    if (((dx + dy + dz) & 3) != 0) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModTags.GRABBABLE_STRUCTURE_BLOCKS)) {
                        double dist = origin.distSqr(pos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }
}
