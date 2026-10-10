package org.papiricoh.supernaturalcraft.client.heaven.render.figure;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A humanoid figure drawn from one of the mod's GeckoLib rigs without an entity of its own (v0.18): Dean, Sam and Castiel (the
 * {@code hunter_*} allies of the Author's finale), Bobby and the rival hunters' looks. One instance per thing it stands in for
 * (a memory's figure, a training copy), so each keeps its own animation; the rig's idle (or walk, when it moves and the rig has
 * one) plays, and the pose ({@link FigurePose}), a walking swing and the head's turn are laid over it by code.
 */
public final class HumanFigure implements GeoAnimatable {

    /** A rig and its look: model and animation names, texture, and the rival hunters' variant (-1 for the allies). */
    public record Look(String geo, String texture, String anim, int variant) {
        public ResourceLocation model() {
            return GeoGuard.model(geo);
        }

        public ResourceLocation animation() {
            return GeoGuard.animation(anim);
        }

        public ResourceLocation textureLocation() {
            return SupernaturalCraft.asResource("textures/entity/" + texture + ".png");
        }

        public boolean ready() {
            return GeoGuard.ready(model(), animation());
        }

        /** The rival hunters' rig has shins; the allies' does not. */
        public boolean hasShins() {
            return variant >= 0;
        }
    }

    public static final Look DEAN = new Look("hunter_dean", "hunter_dean", "hunter_ally", -1),
            SAM = new Look("hunter_sam", "hunter_sam", "hunter_ally", -1),
            CASTIEL = new Look("hunter_castiel", "hunter_castiel", "hunter_ally", -1);

    /** A rival hunter's look {@code n} (0 cap and hood, 1 vest, 2 trench coat); Bobby wears the cap. */
    public static Look rival(int n) {
        return new Look("rival_hunter", "rival_hunter_" + Math.floorMod(n, 3), "rival_hunter", Math.floorMod(n, 3));
    }

    /** {@code dean|sam|castiel|bobby} (or a rival's {@code rival0..2}) → its look, null if unknown. */
    public static @Nullable Look named(String who) {
        return switch (who) {
            case "dean" -> DEAN;
            case "sam" -> SAM;
            case "castiel", "cas" -> CASTIEL;
            case "bobby" -> rival(0);
            case "rival0", "rival" -> rival(0);
            case "rival1" -> rival(1);
            case "rival2" -> rival(2);
            default -> null;
        };
    }

    private static final Map<Object, HumanFigure> FIGURES = new WeakHashMap<>();
    private static final Renderer RENDERER = new Renderer();
    /** Pieces only one rival look wears (as the rival hunters' renderer hides them). */
    private static final String[][] LOOK_ONLY = {{"cap", "hood"}, {"vest", "shirt_tails"},
            {"coat_tail_r", "coat_tail_l", "coat_back", "belt_tail"}};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Look look = DEAN;
    private FigurePose pose = FigurePose.STAND;
    private float walk, walkSpeed, headYaw, headPitch;
    private boolean moving, armed;
    private int colour = 0xFFFFFFFF;
    private boolean translucent;

    private HumanFigure() {
    }

    /** The figure standing in for {@code host} (made on first use). */
    public static HumanFigure of(Object host) {
        return FIGURES.computeIfAbsent(host, h -> new HumanFigure());
    }

    /**
     * Draws it with its feet at the pose's origin, facing {@code bodyYaw} (an entity's yaw: 0 = south).
     *
     * @param walkPos   how far its legs have walked (an entity's walk animation position), 0 if it stands
     * @param walkSpeed how fast (0-1)
     * @param headYaw   the head's turn from the body, degrees (GeckoLib's sense: positive to the figure's left)
     * @param headPitch the head's tilt, degrees (positive up)
     * @param armed     whether a hostile copy shows its machete
     * @param argb      colour and alpha it is drawn with (white = as painted)
     */
    public void draw(PoseStack ps, MultiBufferSource buffers, Look look, FigurePose pose, float bodyYaw, float headYaw, float headPitch,
                     float walkPos, float walkSpeed, boolean armed, int argb, int light, float partial) {
        Look use = look.ready() ? look : DEAN.ready() ? DEAN : null;
        if (use == null) return;
        this.look = use;
        this.pose = pose;
        this.walk = walkPos;
        this.walkSpeed = walkSpeed;
        this.moving = walkSpeed > 0.05f;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
        this.armed = armed;
        this.colour = argb;
        this.translucent = (argb >>> 24) < 250;
        ResourceLocation texture = use.textureLocation();
        if (!GeoGuard.exists(texture)) return;
        RenderType type = translucent ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
        ps.pushPose();
        boolean shins = use.hasShins();
        ps.translate(0, -(shins ? pose.sinkShins() : pose.sink()), 0);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - bodyYaw));
        if (pose.lying()) {
            // On its back, its head where its back was turned.
            ps.translate(0, 0.15, 0);
            ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
            ps.translate(0, -0.95, 0);
        }
        ps.translate(-0.5, -0.51, -0.5);
        RENDERER.render(ps, this, buffers, type, buffers.getBuffer(type), light, partial);
        ps.popPose();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            String prefix = "animation." + look.anim() + ".";
            boolean hasWalk = look.anim().equals("rival_hunter");
            return state.setAndContinue(RawAnimation.begin().thenLoop(prefix + (moving && hasWalk ? "walk" : "idle")));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return software.bernie.geckolib.util.RenderUtil.getCurrentTick();
    }

    // --- the shared renderer and model -------------------------------------------------------------------------------

    private static final class Renderer extends GeoObjectRenderer<HumanFigure> {
        Renderer() {
            super(new Model());
        }

        @Override
        public void preRender(PoseStack pose, HumanFigure f, BakedGeoModel model, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender && f.look.variant() >= 0) {
                GeoModel<HumanFigure> geo = getGeoModel();
                show(geo, "flask", false);
                show(geo, "shotgun", false);
                show(geo, "machete", f.armed);
                show(geo, "flask_belt", true);
                show(geo, "machete_sheathed", !f.armed);
                show(geo, "shotgun_slung", true);
                for (int l = 0; l < LOOK_ONLY.length; l++) for (String b : LOOK_ONLY[l]) show(geo, b, l == f.look.variant());
                for (GeoBone bone : model.topLevelBones()) variantBones(bone, f.look.variant());
            }
            super.preRender(pose, f, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        public Color getRenderColor(HumanFigure f, float partialTick, int light) {
            int c = f.colour;
            return Color.ofARGB(c >>> 24, (c >> 16) & 255, (c >> 8) & 255, c & 255);
        }

        private static void show(GeoModel<?> model, String bone, boolean visible) {
            model.getBone(bone).ifPresent(b -> {
                b.setHidden(!visible);
                b.setChildrenHidden(!visible);
            });
        }

        private static void variantBones(GeoBone bone, int variant) {
            String n = bone.getName();
            if (n.length() > 2 && n.charAt(0) == 'v' && Character.isDigit(n.charAt(1)) && n.charAt(2) == '_') {
                boolean on = n.charAt(1) - '0' == variant;
                bone.setHidden(!on);
                bone.setChildrenHidden(!on);
                if (!on) return;
            }
            for (GeoBone child : bone.getChildBones()) variantBones(child, variant);
        }
    }

    private static final class Model extends GeoModel<HumanFigure> {
        @Override
        public ResourceLocation getModelResource(HumanFigure f) {
            return f.look.model();
        }

        @Override
        public ResourceLocation getTextureResource(HumanFigure f) {
            return f.look.textureLocation();
        }

        @Override
        public ResourceLocation getAnimationResource(HumanFigure f) {
            return f.look.animation();
        }

        @Override
        public void setCustomAnimations(HumanFigure f, long instanceId, AnimationState<HumanFigure> state) {
            super.setCustomAnimations(f, instanceId, state);
            FigurePose p = f.pose;
            boolean shins = f.look.hasShins();
            add("body", p.body());
            add("head", new FigurePose.Rot(p.head().x() + f.headPitch * Mth.DEG_TO_RAD, p.head().y() + f.headYaw * Mth.DEG_TO_RAD,
                    p.head().z()));
            add("right_arm", p.rightArm());
            add("left_arm", p.leftArm());
            add("right_leg", p.rightLeg(shins));
            add("left_leg", p.leftLeg(shins));
            if (shins) {
                add("right_shin", p.rightShin());
                add("left_shin", p.leftShin());
            }
            // A walking swing when the rig has no walk of its own (and only while standing).
            if (f.moving && p == FigurePose.STAND && !f.look.anim().equals("rival_hunter")) {
                float s = Mth.cos(f.walk * 0.6662f) * 1.1f * Math.min(1, f.walkSpeed);
                add("right_leg", new FigurePose.Rot(s, 0, 0));
                add("left_leg", new FigurePose.Rot(-s, 0, 0));
                add("right_arm", new FigurePose.Rot(-s * 0.8f, 0, 0));
                add("left_arm", new FigurePose.Rot(s * 0.8f, 0, 0));
            }
        }

        private void add(String bone, FigurePose.Rot r) {
            if (r == null || r.none()) return;
            getBone(bone).ifPresent(b -> {
                b.setRotX(b.getRotX() + r.x());
                b.setRotY(b.getRotY() + r.y());
                b.setRotZ(b.getRotZ() + r.z());
            });
        }
    }
}
