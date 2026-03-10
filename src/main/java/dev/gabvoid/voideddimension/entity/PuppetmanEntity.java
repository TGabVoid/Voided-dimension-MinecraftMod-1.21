package dev.gabvoid.voideddimension.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.AnimationController;

import java.util.Collections;
import java.util.List;

public class PuppetmanEntity extends HostileEntity implements GeoEntity {
    private static final String[] AUTO_ANIMS = new String[] {
        "IDLE",
        "pulgar",
        "indice",
        "medio",
        "anular",
        "menique"
    };
    private static final int AUTO_ANIM_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String forcedAnim = "none";
    private String currentAnim = AUTO_ANIMS[0];
    private int animIndex = 0;

    public PuppetmanEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!"none".equalsIgnoreCase(this.forcedAnim)) {
            return;
        }
        if ((this.age % AUTO_ANIM_TICKS) == 0) {
            this.animIndex = (this.animIndex + 1) % AUTO_ANIMS.length;
            this.currentAnim = AUTO_ANIMS[this.animIndex];
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (!"none".equalsIgnoreCase(forcedAnim)) {
                state.setAndContinue(RawAnimation.begin().thenLoop(forcedAnim));
            } else {
                state.setAndContinue(RawAnimation.begin().thenLoop(currentAnim));
            }
            return PlayState.CONTINUE;
        }));
    }

    public void setDebugAnim(String anim) {
        if (anim == null || anim.isBlank()) {
            this.forcedAnim = "none";
        } else if ("idle".equalsIgnoreCase(anim)) {
            this.forcedAnim = "IDLE";
        } else {
            this.forcedAnim = anim.toLowerCase();
        }
        if ("none".equalsIgnoreCase(this.forcedAnim)) {
            this.animIndex = 0;
            this.currentAnim = AUTO_ANIMS[0];
        }
        this.setAiDisabled(!"none".equalsIgnoreCase(this.forcedAnim));
    }

    public String getDebugAnim() {
        return this.forcedAnim;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public List<Box> getDebugHitboxesWorld() {
        return Collections.emptyList();
    }

    public BeamMode getBeamMode() {
        return BeamMode.NONE;
    }

    public enum BeamMode { NONE, LEFT, RIGHT, BOTH }
}
