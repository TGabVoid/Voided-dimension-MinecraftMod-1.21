package dev.gabvoid.voideddimension.items.custom;

import dev.gabvoid.voideddimension.blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgonizingGlowItem extends Item {
    private boolean isActive = false;
    private final Queue<BlockPos> blockPositions = new LinkedList<>();
    private final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();
    private static final int GUSANO_LONGITUD = 50;

    public AgonizingGlowItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient && !isActive) {
            isActive = true;

            // Aplica efectos al jugador
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, true, false));

            // Reproduce un sonido al activar
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0F, 1.0F);

            // Establece un cooldown de 20 segundos
            player.getItemCooldownManager().set(this, 400);

            // Inicia el temporizador de 10 segundos
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.schedule(() -> {
                isActive = false;

                // Restaura los bloques originales al desactivar
                while (!blockPositions.isEmpty()) {
                    BlockPos pos = blockPositions.poll();
                    BlockState originalState = originalBlocks.remove(pos);
                    if (originalState != null) {
                        world.setBlockState(pos, originalState);
                    }
                }

                // Reproduce un sonido al desactivar
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                scheduler.shutdown();
            }, 10, TimeUnit.SECONDS);
        }

        return TypedActionResult.success(player.getStackInHand(hand));
    }

    public void tick(World world, PlayerEntity player) {
        if (isActive && !world.isClient) {
            BlockPos playerPos = player.getBlockPos().down();

            float yaw = player.getYaw();
            double radians = Math.toRadians(yaw);
            int xOffset = (int) (-Math.sin(radians) * 2);
            int zOffset = (int) (Math.cos(radians) * 2);

            // Generamos más bloques por delante (8 bloques)
            BlockPos forward1 = playerPos.add(xOffset, 0, zOffset);
            BlockPos forward2 = playerPos.add(xOffset * 2, 0, zOffset * 2);
            BlockPos forward3 = playerPos.add(xOffset * 3, 0, zOffset * 3);



            if (blockPositions.isEmpty() || !blockPositions.peek().equals(playerPos)) {
                BlockPos[] positionsToUpdate = new BlockPos[]{
                        playerPos, forward1, forward2, forward3
                };

                for (BlockPos pos : positionsToUpdate) {
                    if (!blockPositions.contains(pos)) {
                        if (!originalBlocks.containsKey(pos)) {
                            originalBlocks.put(pos, world.getBlockState(pos));
                        }

                        boolean success = world.setBlockState(pos, ModBlocks.AGONIZING_LIGHT_TRAIL.getDefaultState());
                        if (success) {
                            blockPositions.add(pos);
                        }
                    }
                }

                while (blockPositions.size() > GUSANO_LONGITUD) {
                    BlockPos removedPos = blockPositions.poll();
                    BlockState originalState = originalBlocks.remove(removedPos);
                    if (originalState != null) {
                        world.setBlockState(removedPos, originalState);
                    }
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type)
    {
        if(Screen.hasShiftDown())
        {
            tooltip.add(Text.translatable("item.voided_dimension.agonizing_glow.shiftdown.tooltip1"));
            tooltip.add(Text.translatable("item.voided_dimension.agonizing_glow.shiftdown.tooltip2"));
        }
        else
        {
            tooltip.add(Text.translatable("item.voided_dimension.agonizing_glow.tooltip"));
        }

        super.appendTooltip(stack, context, tooltip, type);
    }
}