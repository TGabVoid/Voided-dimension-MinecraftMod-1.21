package dev.gabvoid.voideddimension.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import dev.gabvoid.voideddimension.entity.PuppetmanEntity;

import java.util.Collection;

public class DebugPuppetAnimCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            literal("puppet")
                .then(
                    literal("anim")
                        .then(
                            argument("target", EntityArgumentType.entities())
                                .then(
                                    argument("anim", StringArgumentType.word())
                                        .executes(ctx -> execute(ctx, EntityArgumentType.getEntities(ctx, "target"), StringArgumentType.getString(ctx, "anim")))
                                )
                        )
                )
        );
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx, Collection<? extends Entity> targets, String anim) {
        for (Entity e : targets) {
            if (e instanceof PuppetmanEntity puppet) {
                puppet.setDebugAnim(anim);
                ctx.getSource().sendFeedback(() -> Text.literal("Set debug anim to " + anim + " for Puppetman"), true);
            } else {
                ctx.getSource().sendError(Text.literal("Entity " + e.getId() + " is not a Puppetman"));
            }
        }
        return 1;
    }
}
