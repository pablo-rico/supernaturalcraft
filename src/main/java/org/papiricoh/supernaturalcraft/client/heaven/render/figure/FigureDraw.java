package org.papiricoh.supernaturalcraft.client.heaven.render.figure;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Draws a figure wherever its host stands (v0.18): a memory's figure or a training copy. Who it is decides how:
 * <ul>
 *   <li>{@code @owner}: the viewer's own skin on the player model ({@link SkinFigure});</li>
 *   <li>{@code @ally:dean|sam|castiel|bobby} and {@code @rival:n}: the mod's GeckoLib humans ({@link HumanFigure});</li>
 *   <li>an entity type id: that type's own renderer drawing a still client-only copy of it (one per type, cached; never added
 *   to the world), seated or crouched for the low poses, laid on its side when fallen;</li>
 *   <li>anything that cannot be drawn (an unknown id, a type that will not make a copy, a renderer that throws on one): a pale
 *   silhouette.</li>
 * </ul>
 * Each is washed in the figure's colour and lifted to its glow ({@link TintedBuffers}).
 */
public final class FigureDraw {

    private final SkinFigure skins;
    private final Map<EntityType<?>, Entity> dummies = new HashMap<>();
    private final Set<EntityType<?>> broken = new HashSet<>();
    private @Nullable ClientLevel dummiesLevel;

    public FigureDraw(EntityRendererProvider.Context ctx) {
        skins = new SkinFigure(ctx);
    }

    /**
     * @param host     the entity it is drawn for (its animation clock, and its walk if alive)
     * @param bodyYaw  the way it faces (entity yaw: 0 = south)
     */
    public void draw(Entity host, FigureSpec spec, float bodyYaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        draw(host, spec, null, bodyYaw, partial, ps, buffers, light);
    }

    /** As above; {@code @owner} wears the skin of {@code skinOf} (a hunter's copy) instead of the viewer's, if known. */
    public void draw(Entity host, FigureSpec spec, @Nullable java.util.UUID skinOf, float bodyYaw, float partial, PoseStack ps,
                     MultiBufferSource buffers, int light) {
        MultiBufferSource tinted = new TintedBuffers(buffers, spec.argb(), spec.glow());
        int lit = TintedBuffers.lift(light, spec.glow());
        FigurePose pose = FigurePose.of(spec.pose());
        LivingEntity living = host instanceof LivingEntity l ? l : null;
        float headYaw = living == null ? 0 : Mth.wrapDegrees(Mth.rotLerp(partial, living.yHeadRotO, living.yHeadRot) - bodyYaw);
        float headPitch = living == null ? 0 : Mth.lerp(partial, host.xRotO, host.getXRot());
        ps.pushPose();
        ps.scale(spec.scale(), spec.scale(), spec.scale());
        try {
            if (spec.owner()) {
                skins.draw(ps, tinted, skinOf == null ? SkinFigure.viewerSkin() : SkinFigure.skinOf(skinOf), pose, living, bodyYaw, headYaw, headPitch, spec.argb(), lit, partial);
                return;
            }
            HumanFigure.Look look = spec.ally() != null ? HumanFigure.named(spec.ally()) : spec.rival() >= 0 ? HumanFigure.rival(spec.rival()) : null;
            if (look != null) {
                float walkPos = living == null ? 0 : living.walkAnimation.position(partial);
                float walkSpeed = living == null ? 0 : living.walkAnimation.speed(partial);
                HumanFigure.of(host).draw(ps, tinted, look, pose, bodyYaw, -headYaw, -headPitch, walkPos, walkSpeed, spec.armed(),
                        spec.argb(), lit, partial);
                return;
            }
            if (!drawType(host, spec, pose, bodyYaw, partial, ps, tinted, lit)) silhouette(ps, tinted, pose, living, bodyYaw, lit, partial);
        } finally {
            ps.popPose();
        }
    }

    /** A pale stand-in: the player's shape in a washed-out skin. */
    public void silhouette(PoseStack ps, MultiBufferSource buffers, FigurePose pose, @Nullable LivingEntity living, float bodyYaw,
                           int light, float partial) {
        skins.draw(ps, new TintedBuffers(buffers, 0xA8F6EEDC, 12), DefaultPlayerSkin.get(net.minecraft.Util.NIL_UUID), pose, living, bodyYaw, 0, 0,
                0xA8F6EEDC, TintedBuffers.lift(light, 12), partial);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean drawType(Entity host, FigureSpec spec, FigurePose pose, float bodyYaw, float partial, PoseStack ps,
                             MultiBufferSource buffers, int light) {
        ResourceLocation id = ResourceLocation.tryParse(spec.figure());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return false;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == EntityType.PLAYER) {
            skins.draw(ps, buffers, SkinFigure.viewerSkin(), pose, null, bodyYaw, 0, 0, spec.argb(), light, partial);
            return true;
        }
        if (broken.contains(type)) return false;
        Entity dummy = dummy(type);
        if (dummy == null) return false;
        stage(dummy, host, spec, pose, bodyYaw, partial);
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
        PoseStack.Pose top = ps.last();
        ps.pushPose();
        try {
            if (pose.lying()) {
                // On its side, along the way it faced.
                float rad = bodyYaw * Mth.DEG_TO_RAD;
                ps.translate(0, dummy.getBbWidth() * 0.5f, 0);
                ps.mulPose(new Quaternionf().rotationAxis(Mth.HALF_PI, -Mth.sin(rad), 0, Mth.cos(rad)));
            }
            renderer.render(dummy, bodyYaw, partial, ps, buffers, light);
            return true;
        } catch (RuntimeException e) {
            broken.add(type);
            SupernaturalCraft.LOGGER.warn("Memory figure: {} cannot be drawn as a still copy ({}); a silhouette stands in for it", id, e.toString());
            return false;
        } finally {
            while (ps.last() != top && !ps.clear()) ps.popPose();
        }
    }

    private @Nullable Entity dummy(EntityType<?> type) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (level != dummiesLevel) {
            dummies.clear();
            dummiesLevel = level;
        }
        return dummies.computeIfAbsent(type, t -> {
            try {
                Entity e = t.create(level);
                if (e == null) broken.add(t);
                return e;
            } catch (RuntimeException ex) {
                broken.add(t);
                return null;
            }
        });
    }

    /** Puts the still copy in the figure's place, facing, age and pose. */
    private static void stage(Entity dummy, Entity host, FigureSpec spec, FigurePose pose, float bodyYaw, float partial) {
        dummy.setPos(host.getX(), host.getY(), host.getZ());
        dummy.xo = dummy.xOld = host.getX();
        dummy.yo = dummy.yOld = host.getY();
        dummy.zo = dummy.zOld = host.getZ();
        dummy.tickCount = host.tickCount;
        dummy.setYRot(bodyYaw);
        dummy.yRotO = bodyYaw;
        dummy.setXRot(0);
        dummy.xRotO = 0;
        boolean low = FigurePose.low(spec.pose());
        if (dummy instanceof LivingEntity l) {
            l.yBodyRot = l.yBodyRotO = bodyYaw;
            l.yHeadRot = l.yHeadRotO = bodyYaw;
            l.attackAnim = l.oAttackAnim = pose == FigurePose.STRIKE ? 0.45f : 0f;
            l.swinging = pose == FigurePose.STRIKE;
            l.hurtTime = 0;
            l.deathTime = 0;
        }
        if (dummy instanceof TamableAnimal pet) {
            pet.setInSittingPose(low);
            pet.setTame(true, false);
        } else if (!(dummy instanceof Player)) {
            dummy.setPose(low ? Pose.CROUCHING : Pose.STANDING);
            dummy.setShiftKeyDown(low);
        }
    }

    /** A turn of the pose stack about the vertical axis, for renderers that take the yaw from the pose. */
    static void face(PoseStack ps, float yaw) {
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
    }
}
