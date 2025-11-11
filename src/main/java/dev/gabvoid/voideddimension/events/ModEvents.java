package dev.gabvoid.voideddimension.events;

import dev.gabvoid.voideddimension.items.custom.AgonizingGlowItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;

public class ModEvents
{
    public static void registerEvents() {

        ServerTickEvents.END_SERVER_TICK.register(server ->
        {
            for (PlayerEntity player : server.getPlayerManager().getPlayerList())


            {
                if (player.getMainHandStack().getItem() instanceof AgonizingGlowItem)

                {
                    ((AgonizingGlowItem) player.getMainHandStack().getItem()).tick(player.getWorld(), player);
                }
            }
        });
    }
}