package dev.gabvoid.voideddimension.items;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ModFoodComponents
{
    public static final FoodComponent BLACK_ROSEHIP = new FoodComponent.Builder()
            .nutrition(3)
            .saturationModifier(0.25f)
            .statusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, 1), 1.0f)
            .build();
}
