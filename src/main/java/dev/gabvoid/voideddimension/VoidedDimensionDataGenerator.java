package dev.gabvoid.voideddimension;

import dev.gabvoid.voideddimension.datagen.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class VoidedDimensionDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		// Crear un pack con el ID del mod
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		// Registrar todos los proveedores de datos
		pack.addProvider(ModBlockTagProvider::new);
		pack.addProvider((output, registriesFuture) -> new ModItemTagProvider(output, registriesFuture));
		pack.addProvider(ModLootTableProvider::new);
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModRecipeProvider::new);

	}
}