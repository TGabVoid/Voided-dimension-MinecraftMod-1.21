package dev.gabvoid.voideddimension.blocks.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.LandingBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;

public class SafeBlock extends ParticleBlock implements LandingBlock {
    private final boolean isBouncy;
    private final float bounceVelocity;
    private final boolean preventFallDamage;

    public SafeBlock(Settings settings, ParticleEffect particle, boolean isBouncy, boolean preventFallDamage, float bounceVelocity) {
        super(settings, particle);
        this.isBouncy = isBouncy;
        this.preventFallDamage = preventFallDamage;
        this.bounceVelocity = bounceVelocity;
    }
    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (entity == null || world == null) {
            return;
        }

        if (isBouncy) {
            entity.setVelocity(entity.getVelocity().x, bounceVelocity, entity.getVelocity().z);
        }
    }


}
