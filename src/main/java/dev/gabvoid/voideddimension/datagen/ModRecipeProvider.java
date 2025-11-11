package dev.gabvoid.voideddimension.datagen;

import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider
{

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        List<ItemConvertible> FULL_BLOCK = List.of(ModBlocks.AMALGAMA_ORE_BLOCK, ModBlocks.AMALGAMA_BLOCK);

        offerReversibleCompactingRecipes(exporter, RecipeCategory.MISC, ModItems.AMALGAMA,RecipeCategory.DECORATIONS,ModBlocks.AMALGAMA_BLOCK);
    }
}
