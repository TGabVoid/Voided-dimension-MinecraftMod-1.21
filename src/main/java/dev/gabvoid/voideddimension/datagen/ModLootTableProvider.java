package dev.gabvoid.voideddimension.datagen;

import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider
{
    private final CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup;

    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
        this.registryLookup = registryLookup;
    }

    @Override
    public void generate() {
        RegistryWrapper<Enchantment> enchantmentWrapper = registryLookup.join().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> fortuneEntry = enchantmentWrapper.getOrThrow(Enchantments.FORTUNE);

        addDrop(ModBlocks.AMALGAMA_ORE_BLOCK, oreDrops(ModBlocks.AMALGAMA_ORE_BLOCK, ModItems.AMALGAMA)
                .apply(ApplyBonusLootFunction.oreDrops(fortuneEntry))
                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 3.0f))));
        addDrop(ModBlocks.AMALGAMA_BLOCK);
    }
}