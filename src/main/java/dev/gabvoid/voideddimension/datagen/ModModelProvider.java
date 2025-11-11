package dev.gabvoid.voideddimension.datagen;

import net.minecraft.data.client.TextureKey;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.Models;
import net.minecraft.data.client.TexturedModel;
import net.minecraft.data.client.TextureMap;
import net.minecraft.util.Identifier;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.AMALGAMA_ORE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.AMALGAMA_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.AGONIZING_LIGHT_TRAIL);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CURSE_STONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.CURSE_COBBLESTONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.GREY_STONE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.BLACK_STONE_BLOCK);

        blockStateModelGenerator.registerAxisRotated(ModBlocks.BLACK_BASE_BLOCK, TexturedModel.CUBE_COLUMN);
        blockStateModelGenerator.registerAxisRotated(ModBlocks.VOIDED_BLOCK, TexturedModel.CUBE_COLUMN);

        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.VOID_BLOCK);
    }
    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.AMALGAMA, Models.GENERATED);
        itemModelGenerator.register(ModItems.AGONIZING_GLOW, Models.GENERATED);
        itemModelGenerator.register(ModItems.BLACK_ROSEHIP, Models.GENERATED);
    }
}