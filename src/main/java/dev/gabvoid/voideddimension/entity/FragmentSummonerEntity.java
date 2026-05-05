package dev.gabvoid.voideddimension.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FragmentSummonerEntity extends HostileEntity implements GeoEntity {
    private static final RawAnimation FLOATING = RawAnimation.begin().thenLoop("floating");

    private static final double DETECTION_RANGE = 22.0;
    private static final int BURST_COUNT = 5;
    private static final int BURST_INTERVAL_TICKS = 10; // 2 por segundo
    private static final int COOLDOWN_TICKS = 200; // 10s
    private static final int MAX_NEARBY_FRAGMENTS = 12;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int burstRemaining = 0;
    private int burstInterval = 0;
    private int cooldown = 0;

    public FragmentSummonerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.noClip = false;
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, DETECTION_RANGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void initGoals() {
        // IA controlada por tick manual.
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);

        // Mantiene posicion estable con flotacion muy suave.
        if (this.getVelocity().lengthSquared() > 1.0E-6) {
            this.setVelocity(this.getVelocity().multiply(0.6, 0.85, 0.6));
            this.velocityDirty = true;
        }

        if (this.getWorld().isClient) {
            return;
        }

        if (this.cooldown > 0) {
            this.cooldown--;
        }

        if (this.burstRemaining <= 0) {
            if (this.cooldown <= 0 && hasValidTarget()) {
                this.burstRemaining = BURST_COUNT;
                this.burstInterval = 0;
            }
            return;
        }

        if (this.burstInterval > 0) {
            this.burstInterval--;
            return;
        }

        if (spawnOneFragment()) {
            this.burstRemaining--;
        } else {
            // Evita bucles duros si el espacio esta bloqueado.
            this.burstRemaining = Math.max(0, this.burstRemaining - 1);
        }

        this.burstInterval = BURST_INTERVAL_TICKS;
        if (this.burstRemaining <= 0) {
            this.cooldown = COOLDOWN_TICKS;
        }
    }

    private boolean hasValidTarget() {
        PlayerEntity target = this.getWorld().getClosestPlayer(this, DETECTION_RANGE);
        return target != null && target.isAlive() && !target.isSpectator() && !target.isCreative();
    }

    private boolean spawnOneFragment() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }

        int nearby = serverWorld.getEntitiesByClass(WanderingFragmentEntity.class,
                this.getBoundingBox().expand(20.0), entity -> entity.isAlive()).size();
        if (nearby >= MAX_NEARBY_FRAGMENTS) {
            return false;
        }

        for (int i = 0; i < 8; i++) {
            double ox = (this.random.nextDouble() - 0.5) * 3.0;
            double oz = (this.random.nextDouble() - 0.5) * 3.0;
            double oy = 0.8 + this.random.nextDouble() * 1.4;
            Vec3d spawn = this.getPos().add(ox, oy, oz);

            if (!serverWorld.isAir(this.getBlockPos().add((int) Math.round(ox), (int) Math.floor(oy), (int) Math.round(oz)))) {
                continue;
            }

            WanderingFragmentEntity fragment = ModEntities.WANDERING_FRAGMENT.create(serverWorld);
            if (fragment == null) {
                return false;
            }

            fragment.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, this.random.nextFloat() * 360.0f, 0.0f);
            serverWorld.spawnEntity(fragment);
            return true;
        }

        return false;
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "floating", 2, state -> {
            state.getController().setAnimationSpeed(0.6f);
            state.setAndContinue(FLOATING);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
