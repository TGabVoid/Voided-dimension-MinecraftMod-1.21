package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.blocks.custom.MixedPetalsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class RosePetalsFeature extends Feature<DefaultFeatureConfig> {
    private static final float PLACE_CHANCE = 0.8f;

    public RosePetalsFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = context.getRandom();

        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        BlockPos groundPos = pos.down();
        BlockState ground = world.getBlockState(groundPos);
        if (!ground.isOf(ModBlocks.DIRT_ASHE) && !ground.isOf(ModBlocks.SAND_ASHE)) {
            return false;
        }

        if (random.nextFloat() > PLACE_CHANCE) {
            return false;
        }

        int total = rollTotalPetals(random);
        int roll = random.nextInt(100);
        int white;
        int black;

        if (roll < 25) {
            white = total;
            black = 0;
        } else if (roll < 50) {
            white = 0;
            black = total;
        } else {
            if (total < 2) {
                total = 2;
            }
            white = 1 + random.nextInt(total - 1);
            black = total - white;
        }

        Direction facing = Direction.Type.HORIZONTAL.random(random);

        BlockState petals = ModBlocks.ROSE_PETALS.getDefaultState()
            .with(MixedPetalsBlock.WHITE_PETALS, white)
            .with(MixedPetalsBlock.BLACK_PETALS, black)
            .with(MixedPetalsBlock.FACING, facing);

        if (!petals.canPlaceAt(world, pos)) {
            return false;
        }

        world.setBlockState(pos, petals, Block.NOTIFY_ALL);
        return true;
    }

    private int rollTotalPetals(Random random) {
        int roll = random.nextInt(100);
        if (roll < 3) return 1;
        if (roll < 6) return 2;
        if (roll < 9) return 3;
        if (roll < 12) return 4;
        if (roll < 22) return 5;
        if (roll < 36) return 6;
        if (roll < 56) return 7;
        if (roll < 78) return 8;
        return 9;
    }
}
