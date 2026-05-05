package dev.gabvoid.voideddimension.items;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup
{

    public static final ItemGroup VOIDED_ITEMS = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(VoidedDimension.MOD_ID, "voided_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.AMALGAMA)) //icono del item group
                    .displayName(Text.translatable("itemGroup.voided_items"))
                    .entries(((displayContext, entries) ->
                    {
                        entries.add(new ItemStack(ModItems.AMALGAMA));
                        entries.add(new ItemStack(ModItems.AGONIZING_GLOW));
                        entries.add(new ItemStack(ModItems.BLACK_ROSEHIP));
                        entries.add(new ItemStack(ModItems.VOID_PASS));
                        entries.add(new ItemStack(ModItems.WHITE_ROSE_PETAL));
                        entries.add(new ItemStack(ModItems.BLACK_ROSE_PETAL));
                    } ))

                    .build());


    public static final ItemGroup VOIDED_BLOCKS = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(VoidedDimension.MOD_ID, "voided_blocks"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModBlocks.AMALGAMA_BLOCK)) //icono del block group
                    .displayName(Text.translatable("blockGroup.voided_blocks"))
                    .entries(((displayContext, entries) ->
                    {
                        entries.add(new ItemStack(ModBlocks.AMALGAMA_BLOCK));
                        entries.add(new ItemStack(ModBlocks.AMALGAMA_ORE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.AGONIZING_LIGHT_TRAIL));
                        entries.add(new ItemStack(ModBlocks.VOID_BLOCK));
                        entries.add(new ItemStack(ModBlocks.CURSE_STONE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.BLACK_BASE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.VOIDED_BLOCK));
                        entries.add(new ItemStack(ModBlocks.CURSE_COBBLESTONE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.GREY_STONE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.BLACK_STONE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.DUST_PLATE));
                        entries.add(new ItemStack(ModBlocks.FRACTURED_COBBLESTONE));
                        entries.add(new ItemStack(ModBlocks.STRESS_CRACK));
                        entries.add(new ItemStack(ModBlocks.FRACTURED_STONE));
                        entries.add(new ItemStack(ModBlocks.FRAGILE_BEDROCK));
                        entries.add(new ItemStack(ModBlocks.SAND_ASHE));
                        entries.add(new ItemStack(ModBlocks.DIRT_ASHE));
                        entries.add(new ItemStack(ModBlocks.ASHE));
                        entries.add(new ItemStack(ModBlocks.ABYSAL_FUSTE));
                        entries.add(new ItemStack(ModBlocks.FUSTE_CARCASA));
                        entries.add(new ItemStack(ModBlocks.ABYSS_VEIN));
                        entries.add(new ItemStack(ModBlocks.BONY_RACIM_BLOCK));
                        entries.add(new ItemStack(ModBlocks.BONY_RACIM));
                    } ))

                    .build());

    public static final ItemGroup VOIDED_NATURE = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(VoidedDimension.MOD_ID, "voided_nature"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModBlocks.ASHE))
                    .displayName(Text.translatable("blockGroup.voided_nature"))
                    .entries(((displayContext, entries) ->
                    {
                        entries.add(new ItemStack(ModBlocks.AMALGAMA_ORE_BLOCK));
                        entries.add(new ItemStack(ModBlocks.FRAGILE_BEDROCK));
                        entries.add(new ItemStack(ModBlocks.SAND_ASHE));
                        entries.add(new ItemStack(ModBlocks.DIRT_ASHE));
                        entries.add(new ItemStack(ModBlocks.ASHE));
                        entries.add(new ItemStack(ModBlocks.BONY_RACIM_BLOCK));
                        entries.add(new ItemStack(ModBlocks.BONY_RACIM));
                    } ))

                    .build());

    public static final ItemGroup VOIDED_CREATURES = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(VoidedDimension.MOD_ID, "voided_creatures"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.PUPPETMAN_SPAWN_EGG))
                    .displayName(Text.translatable("itemGroup.voided_creatures"))
                    .entries(((displayContext, entries) ->
                    {
                        entries.add(new ItemStack(ModItems.PUPPETMAN_SPAWN_EGG));
                        entries.add(new ItemStack(ModItems.WANDERING_FRAGMENT_SPAWN_EGG));
                        entries.add(new ItemStack(ModItems.FRAGMENT_SUMMONER_SPAWN_EGG));
                        entries.add(new ItemStack(ModItems.ERRATIC_SPAWN_EGG));
                    } ))

                    .build());

    public static void registerItemGroup()
    {
        VoidedDimension.LOGGER.info("Registering Mod Item Group" + VoidedDimension.MOD_ID);
    }


}
