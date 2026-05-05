package dev.gabvoid.voideddimension.blocks.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CrumblyAbyssBlock extends Block {

    public CrumblyAbyssBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        // Al pisar, programamos el tick si no está ya programado
        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, 15); // 15 ticks = 0.75 segundos antes de romperse
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Se rompe el bloque principal y avisa a los bloques vecinos
        world.breakBlock(pos, false);
        
        // Propaga la reacción a bloques de este mismo tipo (Spleef encadenado)
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            
            if (neighborState.isOf(this)) {
                // Agregar algo de retraso aleatorio para la propagación
                world.scheduleBlockTick(neighborPos, this, random.nextBetween(5, 12));
            }
        }
    }
}

