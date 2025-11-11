package dev.gabvoid.voideddimension.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PuppetmanEntity extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("IDLE");

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.setAndContinue(IDLE_ANIM);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static final double SCALE = 5.0; // factor de escala del modelo y hitboxes (antes 10)
    private static final double HEAD_Z_SHIFT_BLOCKS = -2.25; // ajuste de cabeza en bloques base

    @Override
    public float getTargetingMargin() {
        return 25.0F; // la mitad del anterior (10x->5x)
    }

    // ----------------- Hitboxes lógicas (5 zonas) -----------------
    private record LabeledBox(String name, Box box) {}

    private static Box boxFromOriginSize(double ox, double oy, double oz, double sx, double sy, double sz) {
        double minX = (ox / 16.0) * SCALE;
        double minY = (oy / 16.0) * SCALE;
        double minZ = (oz / 16.0) * SCALE;
        double maxX = ((ox + sx) / 16.0) * SCALE;
        double maxY = ((oy + sy) / 16.0) * SCALE;
        double maxZ = ((oz + sz) / 16.0) * SCALE;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static final double HEAD_Z_SHIFT = HEAD_Z_SHIFT_BLOCKS * SCALE; // aplicar escala

    private List<LabeledBox> computeWorldHitboxes() {
        // 5 cajas tomadas de puppetman.geo.json (hitbox): origin/size -> unidades de bloque escaladas
        Box topLocal = boxFromOriginSize(-25, 34, 20, 52, 45, 44).offset(0.0, 0.0, HEAD_Z_SHIFT);
        Box leftLongLocal = boxFromOriginSize(-33, 11, -17, 14, 14, 93);
        Box leftFrontLocal = boxFromOriginSize(-33, 11, -48, 20, 14, 31);
        Box rightFrontLocal = boxFromOriginSize(13, 11, -48, 20, 14, 31);
        Box rightLongLocal = boxFromOriginSize(19, 11, -17, 14, 14, 93);

        float yawRad = (float) Math.toRadians(-this.getYaw());

        List<LabeledBox> list = new ArrayList<>(5);
        list.add(new LabeledBox("top", rotateAndTranslate(topLocal, yawRad)));
        list.add(new LabeledBox("left_long", rotateAndTranslate(leftLongLocal, yawRad)));
        list.add(new LabeledBox("left_front", rotateAndTranslate(leftFrontLocal, yawRad)));
        list.add(new LabeledBox("right_front", rotateAndTranslate(rightFrontLocal, yawRad)));
        list.add(new LabeledBox("right_long", rotateAndTranslate(rightLongLocal, yawRad)));
        return list;
    }

    public List<Box> getDebugHitboxesWorld() {
        List<LabeledBox> lbs = computeWorldHitboxes();
        List<Box> out = new ArrayList<>(lbs.size());
        for (LabeledBox lb : lbs) out.add(lb.box());
        return out;
    }

    private Box rotateAndTranslate(Box local, float yawRad) {
        // Rotar 8 esquinas y tomar el AABB global
        Vec3d[] corners = new Vec3d[] {
                new Vec3d(local.minX, local.minY, local.minZ),
                new Vec3d(local.minX, local.minY, local.maxZ),
                new Vec3d(local.minX, local.maxY, local.minZ),
                new Vec3d(local.minX, local.maxY, local.maxZ),
                new Vec3d(local.maxX, local.minY, local.minZ),
                new Vec3d(local.maxX, local.minY, local.maxZ),
                new Vec3d(local.maxX, local.maxY, local.minZ),
                new Vec3d(local.maxX, local.maxY, local.maxZ)
        };
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3d c : corners) {
            double rx = c.x * cos - c.z * sin;
            double rz = c.x * sin + c.z * cos;
            double wx = this.getX() + rx;
            double wy = this.getY() + c.y;
            double wz = this.getZ() + rz;
            if (wx < minX) minX = wx; if (wy < minY) minY = wy; if (wz < minZ) minZ = wz;
            if (wx > maxX) maxX = wx; if (wy > maxY) maxY = wy; if (wz > maxZ) maxZ = wz;
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Permitir daño ambiental directo (fuego, caída, fuera del mundo, etc.)
        if (source.getAttacker() == null && source.getSource() == null) {
            return super.damage(source, amount);
        }

        Vec3d hitStart = null;
        Vec3d hitEnd = null;
        var attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player) {
            Vec3d eye = player.getEyePos();
            Vec3d look = player.getRotationVec(1.0F);
            hitStart = eye;
            hitEnd = eye.add(look.multiply(4.5));
        } else if (attacker != null) {
            // Atacante no jugador (otro mob): trazar del ojo del atacante al centro del Puppetman
            Vec3d eye = attacker.getEyePos();
            Vec3d target = this.getPos().add(0, this.getHeight() * 0.5, 0);
            hitStart = eye;
            hitEnd = target;
        }

        List<LabeledBox> boxes = computeWorldHitboxes();

        if (hitStart != null && hitEnd != null) {
            final Vec3d s = hitStart;
            final Vec3d e = hitEnd;
            boolean hit = boxes.stream().anyMatch(lb -> {
                Box b = lb.box().expand(0.01);
                return b.contains(s) || b.raycast(s, e).isPresent();
            });
            return hit && super.damage(source, amount);
        }

        // Proyectiles u otras fuentes con entidad fuente
        if (source.getSource() != null) {
            Vec3d p = source.getSource().getPos();
            boolean contains = boxes.stream().anyMatch(lb -> lb.box().contains(p));
            return contains && super.damage(source, amount);
        }

        // Fallback seguro
        return super.damage(source, amount);
    }

    private record RayHit(LabeledBox labeledBox, Optional<Vec3d> hit) {}

    // ----------------- Beam de prueba -----------------
    public enum BeamMode { NONE, LEFT, RIGHT, BOTH }

    /**
     * Modo de beam de prueba para visualización.
     * Cicla cada ~2s entre NONE -> LEFT -> RIGHT -> BOTH.
     * Reemplazar por chequeo de animaciones cuando estén listas.
     */
    public BeamMode getBeamMode() {
        int phase = (this.age / 100) % 4; // 40 ticks ~ 2s
        return switch (phase) {
            case 1 -> BeamMode.LEFT;
            case 2 -> BeamMode.RIGHT;
            case 3 -> BeamMode.BOTH;
            default -> BeamMode.NONE;
        };
    }
}
