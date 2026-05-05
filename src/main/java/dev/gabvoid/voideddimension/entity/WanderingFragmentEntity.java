package dev.gabvoid.voideddimension.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WanderingFragmentEntity extends HostileEntity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLOATING = RawAnimation.begin().thenLoop("floating");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("death");

    private static final int ATTACK_WINDUP_TICKS = 30; // 1.5s segun anim.
    private static final int DEATH_ANIM_TICKS = 15;    // 0.75s segun anim.
    private static final int IDLE_CHANCE_ROLL = 90;
    private static final int IDLE_FULL_ANIM_TICKS = 90; // 4.5s segun anim.
    private static final double DETECTION_RANGE = 28.0;
    private static final double LAUNCH_SPEED = 2.9; // cercano a flecha full charge
    private static final double PLAYER_HIT_RADIUS = 0.15;
    private static final TrackedData<Integer> TRACKED_STATE = DataTracker.registerData(WanderingFragmentEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private FragmentState state = FragmentState.FLOATING;
    private int stateTicks = 0;
    private int idleTicks = 0;
    private Vec3d lockedLaunchDirection = Vec3d.ZERO;

    public WanderingFragmentEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 6;
        this.setNoGravity(true);
        this.noClip = false;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        // IA manual por estados: no pathfinding ni persecucion.
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TRACKED_STATE, FragmentState.FLOATING.id());
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);

        if (this.getWorld().isClient) {
            this.state = FragmentState.fromId(this.dataTracker.get(TRACKED_STATE));
            return;
        }

        switch (this.state) {
            case FLOATING -> tickFloating();
            case IDLE -> tickIdle();
            case ATTACK_WINDUP -> tickAttackWindup();
            case SPINNING_LAUNCH -> tickSpinningLaunch();
            case DYING -> tickDying();
        }
    }

    private void tickFloating() {
        this.stateTicks++;

        PlayerEntity target = this.getWorld().getClosestPlayer(this, DETECTION_RANGE);
        if (isValidTarget(target)) {
            startAttackOnTarget(target);
            return;
        }

        // Sin jugadores: leve flotacion y idle ocasional.
        this.setVelocity(this.getVelocity().multiply(0.8, 0.6, 0.8));
        if (this.idleTicks <= 0 && this.random.nextInt(IDLE_CHANCE_ROLL) == 0) {
            this.idleTicks = IDLE_FULL_ANIM_TICKS;
            changeState(FragmentState.IDLE);
        }
    }

    private void tickIdle() {
        this.stateTicks++;
        this.setVelocity(this.getVelocity().multiply(0.75, 0.6, 0.75));

        this.idleTicks--;
        if (this.idleTicks <= 0) {
            changeState(FragmentState.FLOATING);
        }
    }

    private void tickAttackWindup() {
        this.stateTicks++;
        this.setVelocity(Vec3d.ZERO);
        this.velocityDirty = true;

        if (this.lockedLaunchDirection.lengthSquared() > 1.0E-6) {
            faceDirection(this.lockedLaunchDirection);
        }

        if (this.stateTicks >= ATTACK_WINDUP_TICKS) {
            Vec3d dir = this.lockedLaunchDirection.lengthSquared() > 1.0E-6 ? this.lockedLaunchDirection : Vec3d.fromPolar(this.getPitch(), this.getYaw()).normalize();
            faceDirection(dir);
            this.setVelocity(dir.multiply(LAUNCH_SPEED));
            this.velocityDirty = true;
            changeState(FragmentState.SPINNING_LAUNCH);
        }
    }

    private void tickSpinningLaunch() {
        this.stateTicks++;

        // Se mantiene en trayectoria unica, sin perseguir ni recalcular.
        this.setVelocity(this.getVelocity().normalize().multiply(LAUNCH_SPEED));
        this.velocityDirty = true;

        PlayerEntity hitPlayer = this.getWorld().getClosestPlayer(this, 1.5);
        if (isValidTarget(hitPlayer)) {
            Box hitbox = this.getBoundingBox().expand(PLAYER_HIT_RADIUS);
            if (hitbox.intersects(hitPlayer.getBoundingBox())) {
                DamageSource source = this.getDamageSources().mobAttack(this);
                hitPlayer.damage(source, (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                startDying();
                return;
            }
        }

        if (this.horizontalCollision || this.verticalCollision || this.isOnGround()) {
            startDying();
        }
    }

    private void startDying() {
        if (this.state == FragmentState.DYING) {
            return;
        }
        this.setVelocity(Vec3d.ZERO);
        this.velocityDirty = true;
        changeState(FragmentState.DYING);
    }

    private void tickDying() {
        this.stateTicks++;
        this.setVelocity(Vec3d.ZERO);
        this.velocityDirty = true;

        if (this.stateTicks >= DEATH_ANIM_TICKS) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.POOF, this.getX(), this.getBodyY(0.5), this.getZ(), 18, 0.25, 0.25, 0.25, 0.02);
            }
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.6f, 1.3f);
            this.discard();
        }
    }

    private void changeState(FragmentState nextState) {
        this.state = nextState;
        this.stateTicks = 0;
        this.dataTracker.set(TRACKED_STATE, nextState.id());
    }

    private void startAttackOnTarget(PlayerEntity target) {
        Vec3d aim = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(this.getPos());
        if (aim.lengthSquared() <= 1.0E-6) {
            return;
        }
        this.lockedLaunchDirection = aim.normalize();
        faceDirection(this.lockedLaunchDirection);
        this.setVelocity(Vec3d.ZERO);
        this.velocityDirty = true;
        changeState(FragmentState.ATTACK_WINDUP);
    }

    private void faceDirection(Vec3d direction) {
        Vec3d dir = direction.normalize();
        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dir.y, horizontal)));
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.bodyYaw = yaw;
        this.headYaw = yaw;
    }

    private boolean isValidTarget(PlayerEntity player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative();
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
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            if (this.state == FragmentState.DYING) {
                state.setAndContinue(DEATH);
                return PlayState.CONTINUE;
            }
            if (this.state == FragmentState.ATTACK_WINDUP) {
                state.setAndContinue(ATTACK);
                return PlayState.CONTINUE;
            }
            if (this.state == FragmentState.IDLE) {
                state.setAndContinue(IDLE);
                return PlayState.CONTINUE;
            }
            state.setAndContinue(FLOATING);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private enum FragmentState {
        FLOATING(0),
        IDLE(1),
        ATTACK_WINDUP(2),
        SPINNING_LAUNCH(3),
        DYING(4);

        private final int id;

        FragmentState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static FragmentState fromId(int id) {
            for (FragmentState value : values()) {
                if (value.id == id) {
                    return value;
                }
            }
            return FLOATING;
        }
    }
}


