package dev.gabvoid.voideddimension.mixin;

import dev.gabvoid.voideddimension.items.ModItems;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class VoidPassDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void voideddimension$blockVoidDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!source.isOf(DamageTypes.OUT_OF_WORLD)) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().getRegistryKey() != World.OVERWORLD) return;
        if (!player.getInventory().containsAny(item -> item.isOf(ModItems.VOID_PASS))) return;

        cir.setReturnValue(false);
        cir.cancel();
    }
}

