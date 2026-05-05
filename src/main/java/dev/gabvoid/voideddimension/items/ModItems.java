package dev.gabvoid.voideddimension.items;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.items.custom.AgonizingGlowItem;
import dev.gabvoid.voideddimension.items.custom.RosePetalItem;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import dev.gabvoid.voideddimension.entity.ModEntities;
import net.minecraft.item.SpawnEggItem;

public class ModItems
{
    public static Item AMALGAMA = registerItem("amalgama", new Item(new Item.Settings()));
    public static final Item AGONIZING_GLOW = registerItem("agonizing_glow", new AgonizingGlowItem(new Item.Settings()));
    public static final Item BLACK_ROSEHIP = registerItem("black_rosehip", new Item(new Item.Settings().food(ModFoodComponents.BLACK_ROSEHIP)));
    public static final Item VOID_PASS = registerItem("void_pass", new Item(new Item.Settings().maxCount(1)));
    public static final Item WHITE_ROSE_PETAL = registerItem("white_rose_petal", new RosePetalItem(ModBlocks.ROSE_PETALS, new Item.Settings(), true));
    public static final Item BLACK_ROSE_PETAL = registerItem("black_rose_petal", new RosePetalItem(ModBlocks.ROSE_PETALS, new Item.Settings(), false));

    // Spawn Egg Puppetman (colores base y manchas en formato RGB hex)
    public static final Item PUPPETMAN_SPAWN_EGG = registerItem(
            "puppetman_spawn_egg",
            new SpawnEggItem(ModEntities.PUPPETMAN, 0x2B2B2B, 0xB43B3B, new Item.Settings())
    );

    public static final Item WANDERING_FRAGMENT_SPAWN_EGG = registerItem(
            "wandering_fragment_spawn_egg",
            new SpawnEggItem(ModEntities.WANDERING_FRAGMENT, 0x2A2D35, 0x6F7480, new Item.Settings())
    );

    public static final Item FRAGMENT_SUMMONER_SPAWN_EGG = registerItem(
            "fragment_summoner_spawn_egg",
            new SpawnEggItem(ModEntities.FRAGMENT_SUMMONER, 0x2D2A31, 0x8B7D6A, new Item.Settings())
    );

    public static final Item ERRATIC_SPAWN_EGG = registerItem(
            "erratic_spawn_egg",
            new SpawnEggItem(ModEntities.ERRATIC, 0x28322B, 0x63C77A, new Item.Settings())
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(VoidedDimension.MOD_ID, name), item);
    }


    public static void registerModItems()
    {
        VoidedDimension.LOGGER.info("Registrando Mod Items" + VoidedDimension.MOD_ID);
//ITEMS GROUPS VANILLA
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(AMALGAMA);
            entries.add(BLACK_ROSEHIP);
            entries.add(VOID_PASS);
            entries.add(WHITE_ROSE_PETAL);
            entries.add(BLACK_ROSE_PETAL);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(BLACK_ROSEHIP);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(AGONIZING_GLOW);
            entries.add(VOID_PASS);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(PUPPETMAN_SPAWN_EGG);
            entries.add(WANDERING_FRAGMENT_SPAWN_EGG);
            entries.add(FRAGMENT_SUMMONER_SPAWN_EGG);
            entries.add(ERRATIC_SPAWN_EGG);
        });
    }
}
