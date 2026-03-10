package dev.gabvoid.voideddimension.blocks.custom;

import dev.gabvoid.voideddimension.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MixedPetalsBlock extends Block {
    public static final IntProperty WHITE_PETALS = IntProperty.of("white_petals", 0, 9);
    public static final IntProperty BLACK_PETALS = IntProperty.of("black_petals", 0, 9);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public MixedPetalsBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
            .with(WHITE_PETALS, 1)
            .with(BLACK_PETALS, 0)
            .with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WHITE_PETALS, BLACK_PETALS, FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        ItemStack stack = context.getStack();
        if (!stack.isOf(ModItems.WHITE_ROSE_PETAL) && !stack.isOf(ModItems.BLACK_ROSE_PETAL)) {
            return false;
        }

        int total = state.get(WHITE_PETALS) + state.get(BLACK_PETALS);
        return total < 9;
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return ActionResult.PASS;
        }

        int white = state.get(WHITE_PETALS);
        int black = state.get(BLACK_PETALS);
        int total = white + black;
        if (total >= 9) {
            return ActionResult.PASS;
        }

        if (stack.isOf(ModItems.WHITE_ROSE_PETAL)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(WHITE_PETALS, white + 1), 3);
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            }
            return ActionResult.CONSUME;
        }

        if (stack.isOf(ModItems.BLACK_ROSE_PETAL)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(BLACK_PETALS, black + 1), 3);
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
            }
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
