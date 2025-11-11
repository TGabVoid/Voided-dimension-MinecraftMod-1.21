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
                    } ))

                    .build());

    public static void registerItemGroup()
    {
        VoidedDimension.LOGGER.info("Registering Mod Item Group" + VoidedDimension.MOD_ID);
    }


}
