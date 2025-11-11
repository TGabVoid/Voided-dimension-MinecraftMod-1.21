package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.entity.PuppetmanEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PuppetmanRenderer extends GeoEntityRenderer<PuppetmanEntity> {
    private static final boolean DEBUG_HITBOX = true;
    private static final float SCALE = 5.0f;

    // Textura del hilo/beam (colócala en assets/voideddimension/textures/effect/puppet_thread.png)
    private static final Identifier BEAM_TEXTURE = Identifier.of("voideddimension", "textures/effect/puppet_thread.png");

    // Offsets locales aproximados de las manos respecto al origen del modelo (en bloques, luego multiplicamos por SCALE)
    private static final Vec3d LEFT_HAND_LOCAL = new Vec3d(-0.9, 1.85, 0.9);
    private static final Vec3d RIGHT_HAND_LOCAL = new Vec3d(0.9, 1.85, 0.9);

    public PuppetmanRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new PuppetmanModel());
        this.shadowRadius = 0.4f * SCALE;
    }

    @Override
    public void render(PuppetmanEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(SCALE, SCALE, SCALE);
        super.render(entity, entityYaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();

        // Beams visuales (test): se activan según modo calculado en la entidad
        renderBeams(entity, tickDelta, matrices, vertexConsumers, light);

        if (DEBUG_HITBOX) {
            var boxes = entity.getDebugHitboxesWorld();
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getLines());
            for (Box wb : boxes) {
                Box rel = wb.offset(-entity.getX(), -entity.getY(), -entity.getZ());
                WorldRenderer.drawBox(matrices, vc, rel, 1.0f, 0.1f, 0.1f, 1.0f);
            }
        }
    }

    private void renderBeams(PuppetmanEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Determinar objetivo: jugador objetivo o el más cercano dentro de rango
        LivingEntity target = null;
        if (entity.getTarget() instanceof LivingEntity le) {
            target = le;
        } else {
            PlayerEntity closest = entity.getWorld().getClosestPlayer(entity, 64.0);
            if (closest != null && entity.canSee(closest)) target = closest;
        }
        if (target == null) return;

        // ¿Está activo el beam en este tick? (test: lo decide la entidad)
        PuppetmanEntity.BeamMode mode = entity.getBeamMode();
        if (mode == PuppetmanEntity.BeamMode.NONE) return;

        // Posiciones base de las manos en espacio mundial
        double yawRad = Math.toRadians(-entity.getYaw());
        Vec3d leftWorld = localToWorld(entity, LEFT_HAND_LOCAL.multiply(SCALE), yawRad);
        Vec3d rightWorld = localToWorld(entity, RIGHT_HAND_LOCAL.multiply(SCALE), yawRad);

        // Punto objetivo (pecho del jugador)
        Vec3d targetWorld = target.getPos().add(0, target.getStandingEyeHeight() * 0.6, 0);

        // Parámetros visuales
        float baseThickness = 0.025f * SCALE; // beam delgado
        int beamsPerHand = 1; // solo 1 beam por mano
        float time = (entity.age + tickDelta);
        float scrollSpeed = 0.6f; // velocidad de desplazamiento UV
        float vScale = 0.5f; // controla el número de repeticiones por bloque (más alto = textura más comprimida)
        int alpha = 200; // 0..255

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(BEAM_TEXTURE));

        Vec3d entityPos = entity.getPos();

        // Renderizar por mano según el modo
        if (mode == PuppetmanEntity.BeamMode.LEFT || mode == PuppetmanEntity.BeamMode.BOTH) {
            renderSingleBeam(matrices, vc, light, entityPos, leftWorld, targetWorld, baseThickness, time * scrollSpeed, vScale, alpha);
        }
        if (mode == PuppetmanEntity.BeamMode.RIGHT || mode == PuppetmanEntity.BeamMode.BOTH) {
            renderSingleBeam(matrices, vc, light, entityPos, rightWorld, targetWorld, baseThickness, time * scrollSpeed, vScale, alpha);
        }
    }

    // Nuevo método: solo un beam recto, sin animación ni spread
    private void renderSingleBeam(MatrixStack matrices, VertexConsumer vc, int light, Vec3d entityPos, Vec3d originWorld, Vec3d targetWorld,
                                  float baseThickness, float uOffset, float vScale, int alpha) {
        drawBeam(matrices, vc, light, entityPos, originWorld, targetWorld, baseThickness, uOffset, vScale, alpha);
    }

    private static Vec3d localToWorld(PuppetmanEntity entity, Vec3d local, double yawRad) {
        // rotación en Y + traslación por posición de la entidad
        double cx = local.x * Math.cos(yawRad) - local.z * Math.sin(yawRad);
        double cz = local.x * Math.sin(yawRad) + local.z * Math.cos(yawRad);
        return new Vec3d(entity.getX() + cx, entity.getY() + local.y, entity.getZ() + cz);
    }

    private void renderMultiBeams(MatrixStack matrices, VertexConsumer vc, int light, Vec3d entityPos, Vec3d originWorld, Vec3d targetWorld,
                                  int count, float baseThickness, float spread, float time, float scrollSpeed, float vScale, int alpha) {
        for (int i = 0; i < count; i++) {
            double ang = (i / (double) count) * Math.PI * 2.0 + time * 0.15; // ligera rotación temporal
            double dx = Math.cos(ang) * spread;
            double dz = Math.sin(ang) * spread;
            Vec3d o = originWorld.add(dx, 0, dz);
            drawBeam(matrices, vc, light, entityPos, o, targetWorld, baseThickness * (0.9f + 0.2f * (i % 2)), time * scrollSpeed, vScale, alpha);
        }
    }

    private void drawBeam(MatrixStack matrices, VertexConsumer vc, int light, Vec3d entityPos, Vec3d originWorld, Vec3d targetWorld,
                          float thickness, float uOffset, float vScale, int alpha) {
        Vec3d dir = targetWorld.subtract(originWorld);
        double len = dir.length();
        if (len < 0.05) return;

        // Obtener la dirección de la cámara para el billboard
        Vec3d camPos = this.dispatcher.camera.getPos();
        Vec3d beamCenter = originWorld.add(dir.multiply(0.5));
        Vec3d toCam = camPos.subtract(beamCenter).normalize();
        Vec3d beamDir = dir.normalize();
        Vec3d side = beamDir.crossProduct(toCam).normalize().multiply(thickness * 0.5);

        // Calcular los 4 vértices del quad orientado hacia la cámara
        Vec3d p0 = originWorld.add(side);
        Vec3d p1 = originWorld.subtract(side);
        Vec3d p2 = targetWorld.subtract(side);
        Vec3d p3 = targetWorld.add(side);

        // UVs: u se desplaza con el tiempo, v según la longitud
        float u0 = uOffset;
        float u1 = uOffset + 1.0f;
        float v0 = 0.0f;
        float v1 = (float) (len * vScale);

        // color blanco con alpha
        float r = 1f, g = 1f, bcol = 1f, aCol = (alpha & 0xFF) / 255f;
        var entry = matrices.peek();
        // Convertir a coordenadas relativas a la entidad
        Vec3d rel0 = p0.subtract(entityPos);
        Vec3d rel1 = p1.subtract(entityPos);
        Vec3d rel2 = p2.subtract(entityPos);
        Vec3d rel3 = p3.subtract(entityPos);

        vc.vertex(entry.getPositionMatrix(), (float) rel0.x, (float) rel0.y, (float) rel0.z)
                .color(r, g, bcol, aCol)
                .texture(u0, v0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 1, 0);
        vc.vertex(entry.getPositionMatrix(), (float) rel1.x, (float) rel1.y, (float) rel1.z)
                .color(r, g, bcol, aCol)
                .texture(u0, v1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 1, 0);
        vc.vertex(entry.getPositionMatrix(), (float) rel2.x, (float) rel2.y, (float) rel2.z)
                .color(r, g, bcol, aCol)
                .texture(u1, v1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 1, 0);
        vc.vertex(entry.getPositionMatrix(), (float) rel3.x, (float) rel3.y, (float) rel3.z)
                .color(r, g, bcol, aCol)
                .texture(u1, v0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 1, 0);
    }
}
