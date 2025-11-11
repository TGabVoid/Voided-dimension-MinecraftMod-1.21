package dev.gabvoid.voideddimension.datagen;

import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider
{

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.AMALGAMA_BLOCK)
                .add(ModBlocks.AMALGAMA_ORE_BLOCK)
                .add(ModBlocks.AGONIZING_LIGHT_TRAIL);

        getOrCreateTagBuilder(ModTags.Blocks.VOID_ORES)
                .add(ModBlocks.AMALGAMA_ORE_BLOCK)
                .setReplace(false);
    }

}
