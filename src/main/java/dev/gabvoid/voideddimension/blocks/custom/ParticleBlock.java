package dev.gabvoid.voideddimension.blocks.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ParticleBlock extends Block {
    private final ParticleEffect particleEffect;

    public ParticleBlock(Settings settings, ParticleEffect particleEffect) {
        super(settings);
        this.particleEffect = particleEffect;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);

        // Genera partículas alrededor del bloque
        for (int i = 0; i < 1; i++) { // Ajusta la cantidad de partículas
            double offsetX = random.nextDouble();
            double offsetY = random.nextDouble();
            double offsetZ = random.nextDouble();
            world.addParticle(particleEffect, pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ, 0, 0, 0);
        }
    }
}