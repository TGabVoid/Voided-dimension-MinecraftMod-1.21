package dev.gabvoid.voideddimension;

import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.items.ModItemGroup;
import dev.gabvoid.voideddimension.items.ModItems;
import net.fabricmc.api.ModInitializer;
import dev.gabvoid.voideddimension.events.ModEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.gabvoid.voideddimension.entity.ModEntities;
import dev.gabvoid.voideddimension.world.ModFeatures;

public class VoidedDimension implements ModInitializer {
    public static final String MOD_ID = "voideddimension";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModItemGroup.registerItemGroup();
        ModEvents.registerEvents();

        ModEntities.register();
        ModFeatures.register();

        LOGGER.info("VoidedDimension inicializado");
    }
}
