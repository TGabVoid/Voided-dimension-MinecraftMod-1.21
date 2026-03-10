package dev.gabvoid.voideddimension.items.custom;

import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.blocks.custom.MixedPetalsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RosePetalItem extends BlockItem {
    private final boolean isWhite;

    public RosePetalItem(Block block, Settings settings, boolean isWhite) {
        super(block, settings);
        this.isWhite = isWhite;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isOf(ModBlocks.ROSE_PETALS)) {
            int white = state.get(MixedPetalsBlock.WHITE_PETALS);
            int black = state.get(MixedPetalsBlock.BLACK_PETALS);
            if (white + black >= 9) {
                return ActionResult.PASS;
            }

            BlockState newState = state.with(
                isWhite ? MixedPetalsBlock.WHITE_PETALS : MixedPetalsBlock.BLACK_PETALS,
                isWhite ? white + 1 : black + 1
            );

            if (!world.isClient) {
                world.setBlockState(pos, newState, 3);
                if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
                    context.getStack().decrement(1);
                }
            }

            return ActionResult.SUCCESS;
        }

        return super.useOnBlock(context);
    }

    @Override
    protected BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null || !state.isOf(ModBlocks.ROSE_PETALS)) {
            return state;
        }

        return state
            .with(MixedPetalsBlock.WHITE_PETALS, isWhite ? 1 : 0)
            .with(MixedPetalsBlock.BLACK_PETALS, isWhite ? 0 : 1);
    }
}
