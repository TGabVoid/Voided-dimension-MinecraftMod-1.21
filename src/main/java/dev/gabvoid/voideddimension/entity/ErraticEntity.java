package dev.gabvoid.voideddimension.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ErraticEntity extends HostileEntity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLOATING = RawAnimation.begin().thenLoop("floating");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("shoot");
    private static final RawAnimation DASH_START = RawAnimation.begin().thenPlay("dash_start");
    private static final RawAnimation DASHING = RawAnimation.begin().thenLoop("dashing");

    private static final double DETECTION_RANGE = 28.0;
    private static final int SHOOT_COOLDOWN_TICKS = 30;
    private static final int DASH_COOLDOWN_TICKS = 140;
    private static final int DASH_STARTUP_TICKS = 10;
    private static final int DASH_MAX_TICKS = 22;
    private static final double DASH_SPEED = 1.55;
    private static final float PROJECTILE_DAMAGE = 5.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ErraticState state = ErraticState.FLOATING;
    private int stateTicks = 0;
    private int idleTicks = 0;
    private int shootCooldown = 20;
    private int dashCooldown = 70;
    private Vec3d dashDirection = Vec3d.ZERO;

    public ErraticEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.noClip = false;
        this.experiencePoints = 9;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, DETECTION_RANGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0);
    }

    @Override
    protected void initGoals() {
        // IA manual por estados.
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);

        if (this.getWorld().isClient) {
            return;
        }

        if (this.shootCooldown > 0) {
            this.shootCooldown--;
        }
        if (this.dashCooldown > 0) {
            this.dashCooldown--;
        }

        switch (this.state) {
            case FLOATING -> tickFloating();
            case SHOOTING -> tickShooting();
            case DASH_START -> tickDashStart();
            case DASHING -> tickDashing();
        }
    }

    private void tickFloating() {
        this.stateTicks++;
        applyHover(0.86);

        PlayerEntity target = this.getWorld().getClosestPlayer(this, DETECTION_RANGE);
        if (!isValidTarget(target)) {
            if (this.idleTicks <= 0 && this.random.nextInt(75) == 0) {
                this.idleTicks = 24;
            } else if (this.idleTicks > 0) {
                this.idleTicks--;
            }
            return;
        }

        lookAtEntity(target, 30.0f, 30.0f);
        this.bodyYaw = this.getYaw();
        this.headYaw = this.getYaw();

        // Dash raro: alrededor de 1/8 intentos cuando el cooldown termino.
        if (this.dashCooldown <= 0 && this.random.nextInt(8) == 0) {
            Vec3d toTarget = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0).subtract(this.getPos());
            if (toTarget.lengthSquared() > 1.0E-6) {
                this.dashDirection = toTarget.normalize();
                changeState(ErraticState.DASH_START);
                return;
            }
        }

        if (this.shootCooldown <= 0) {
            // Para disparar por detras, gira 180 grados antes de la animacion.
            float shootYaw = MathHelper.wrapDegrees(this.getYaw() + 180.0f);
            this.setYaw(shootYaw);
            this.bodyYaw = shootYaw;
            this.headYaw = shootYaw;
            changeState(ErraticState.SHOOTING);
        }
    }

    private void tickShooting() {
        this.stateTicks++;
        applyHover(0.74);

        if (this.stateTicks == 6) {
            fireRearShot();
        }

        if (this.stateTicks >= 14) {
            this.shootCooldown = SHOOT_COOLDOWN_TICKS;
            changeState(ErraticState.FLOATING);
        }
    }

    private void tickDashStart() {
        this.stateTicks++;
        this.setVelocity(this.getVelocity().multiply(0.65, 0.85, 0.65));
        this.velocityDirty = true;

        if (this.dashDirection.lengthSquared() > 1.0E-6) {
            faceDirection(this.dashDirection);
        }

        if (this.stateTicks >= DASH_STARTUP_TICKS) {
            this.setVelocity(this.dashDirection.multiply(DASH_SPEED));
            this.velocityDirty = true;
            changeState(ErraticState.DASHING);
        }
    }

    private void tickDashing() {
        this.stateTicks++;

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 1.0E-6) {
            this.setVelocity(velocity.normalize().multiply(DASH_SPEED));
            this.velocityDirty = true;
            faceDirection(this.getVelocity());
        }

        PlayerEntity hitPlayer = this.getWorld().getClosestPlayer(this, 1.1);
        if (isValidTarget(hitPlayer)) {
            DamageSource source = this.getDamageSources().mobAttack(this);
            hitPlayer.damage(source, (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
            this.dashCooldown = DASH_COOLDOWN_TICKS;
            changeState(ErraticState.FLOATING);
            return;
        }

        if (this.horizontalCollision || this.verticalCollision || this.isOnGround() || this.stateTicks >= DASH_MAX_TICKS) {
            this.dashCooldown = DASH_COOLDOWN_TICKS;
            changeState(ErraticState.FLOATING);
        }
    }

    private void fireRearShot() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d rearDir = Vec3d.fromPolar(0.0f, this.getYaw() + 180.0f).normalize();
        Vec3d start = this.getPos().add(0.0, this.getHeight() * 0.55, 0.0);

        PlayerEntity target = serverWorld.getClosestPlayer(this, DETECTION_RANGE);
        if (isValidTarget(target)) {
            Vec3d toTarget = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0).subtract(start);
            if (toTarget.lengthSquared() > 1.0E-6) {
                // 65% va guiado al target, 35% respeta la salida trasera para mantener identidad.
                rearDir = rearDir.multiply(0.35).add(toTarget.normalize().multiply(0.65)).normalize();
            }
        }

        for (int i = 0; i < 18; i++) {
            double speed = 0.2 + (this.random.nextDouble() * 0.18);
            Vec3d pVel = rearDir.multiply(speed).add(
                    (this.random.nextDouble() - 0.5) * 0.04,
                    (this.random.nextDouble() - 0.5) * 0.04,
                    (this.random.nextDouble() - 0.5) * 0.04
            );
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, start.x, start.y, start.z, 1, pVel.x, pVel.y, pVel.z, 0.0);
        }

        Vec3d end = start.add(rearDir.multiply(18.0));
        Box hitBox = new Box(end.x - 1.7, end.y - 1.7, end.z - 1.7, end.x + 1.7, end.y + 1.7, end.z + 1.7);
        PlayerEntity hit = serverWorld.getEntitiesByClass(PlayerEntity.class, hitBox, this::isValidTarget)
                .stream()
                .findFirst()
                .orElse(null);
        if (hit != null) {
            DamageSource source = this.getDamageSources().mobProjectile(this, this);
            hit.damage(source, PROJECTILE_DAMAGE);
        }
    }

    private void applyHover(double horizontalDamp) {
        Vec3d vel = this.getVelocity();
        double wave = Math.sin((this.age * 0.11) + (this.getId() * 0.45)) * 0.015;
        double yVel = MathHelper.clamp((vel.y * 0.85) + wave + (this.isOnGround() ? 0.08 : 0.0), -0.05, 0.08);
        this.setVelocity(vel.x * horizontalDamp, yVel, vel.z * horizontalDamp);
        this.velocityDirty = true;
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

    private void changeState(ErraticState nextState) {
        this.state = nextState;
        this.stateTicks = 0;
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
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "floating_base", 2, state -> {
            state.getController().setAnimationSpeed(0.65f);
            state.setAndContinue(FLOATING);
            return PlayState.CONTINUE;
        }));

        controllers.add(new AnimationController<>(this, "action", 2, state -> {
            return switch (this.state) {
                case DASH_START -> {
                    state.setAndContinue(DASH_START);
                    yield PlayState.CONTINUE;
                }
                case DASHING -> {
                    state.setAndContinue(DASHING);
                    yield PlayState.CONTINUE;
                }
                case SHOOTING -> {
                    state.setAndContinue(SHOOT);
                    yield PlayState.CONTINUE;
                }
                case FLOATING -> {
                    if (this.idleTicks > 0) {
                        state.setAndContinue(IDLE);
                        yield PlayState.CONTINUE;
                    }
                    yield PlayState.STOP;
                }
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private enum ErraticState {
        FLOATING,
        SHOOTING,
        DASH_START,
        DASHING
    }
}

