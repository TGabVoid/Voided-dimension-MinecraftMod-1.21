package dev.gabvoid.voideddimension.blocks;

import com.ibm.icu.message2.Mf2DataModel;
import com.mojang.serialization.MapCodec;
import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.blocks.custom.ParticleBlock;
import dev.gabvoid.voideddimension.blocks.custom.SafeBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.state.StateManager;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import java.util.List;

public class ModBlocks {

    public static final Block AMALGAMA_BLOCK = registerBlock(
            "amalgama_block",
            new Block(AbstractBlock.Settings.create()
                    .strength(4.0f, 6.0f)
                    .sounds(BlockSoundGroup.SCULK)
                    .requiresTool()
                    .luminance(state -> 6))
    );

    public static final Block AMALGAMA_ORE_BLOCK = registerBlock(
            "amalgama_ore_block",
            new ExperienceDroppingBlock(UniformIntProvider.create(830, 1200),
                    AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.ANCIENT_DEBRIS)
                            .requiresTool()
                            .strength(3.0F, .5f)
                            .luminance(state -> 4))
    );

    public static final Block AGONIZING_LIGHT_TRAIL = registerBlock(
            "agonizing_light_trail",
            new SafeBlock(
                    AbstractBlock.Settings.create()
                            .strength(0.5f, 0.5f)
                            .sounds(BlockSoundGroup.LANTERN)
                            .luminance(state -> 4)
                            .nonOpaque()
                            .slipperiness(0.30f),
                    ParticleTypes.POOF,
                    true,
                    true,
                    0.6f)
    );

    public static final Block VOID_BLOCK = registerBlock(
            "void_block",
            new HorizontalFacingBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.GRASS)
                    .strength(1.0F, 0.5f)) {
                @Override
                protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
                    return null; // O simplemente retorna null para el codec
                }

                @Override
                protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
                    builder.add(FACING);
                }

                @Override
                public BlockState getPlacementState(ItemPlacementContext ctx) {
                    return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
                }
            }
    );

    public static final Block CURSE_STONE_BLOCK = registerBlock(
            "curse_stone_block",
            new Block(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE)
                    .strength(2.0F, 0.5f))
    );

    public static final Block BLACK_BASE_BLOCK = registerBlock(
            "black_base_block",
            new PillarBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.BONE)
                    .requiresTool()
                    .strength(4.0F, 0.5f))
    );

    public static final Block VOIDED_BLOCK = registerBlock(
            "voided_block",
            new PillarBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.ANCIENT_DEBRIS)
                    .requiresTool()
                    .strength(9.0F, 0.5f))
    );

    public static final Block CURSE_COBBLESTONE_BLOCK = registerBlock(
            "curse_cobblestone_block",
            new Block(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.GILDED_BLACKSTONE)
                    .requiresTool()
                    .strength(3.0F, 0.5f))
    );

    public static final Block GREY_STONE_BLOCK = registerBlock(
            "grey_stone_block",
            new Block(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.GILDED_BLACKSTONE)
                    .requiresTool()
                    .strength(3.0F, 0.5f))
    );

    public static final Block BLACK_STONE_BLOCK = registerBlock(
            "black_stone_block",
            new Block(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.ANCIENT_DEBRIS)
                    .requiresTool()
                    .strength(3.0F, 0.5f))
    );

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(VoidedDimension.MOD_ID, name), block);
    }

    private static Block registerParticleBlock(String name, AbstractBlock.Settings settings, ParticleEffect particleEffect) {
        Block block = new ParticleBlock(settings, particleEffect);
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(VoidedDimension.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(VoidedDimension.MOD_ID, name),
                new BlockItem(block, new Item.Settings()) {
                    @Override
                    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
                        super.appendTooltip(stack, context, tooltip, options);
                    }
                });
    }

    public static void registerModBlocks() {
        VoidedDimension.LOGGER.info("Registering ModBlocks for " + VoidedDimension.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(AMALGAMA_BLOCK);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.add(AMALGAMA_ORE_BLOCK);
        });
    }
}