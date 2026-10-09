package org.papiricoh.supernaturalcraft.client.legacy.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A shapeshifter (v0.17). While it wears a skin ({@link ShapeshifterEntity#disguise()}) it is drawn as what it copies: a
 * vanilla villager of that profession, or the copied player (their own skin, and their name over its head). Found out
 * ({@link ShapeshifterEntity#revealed()}) it is its own plain man with the borrowed skin peeling away ({@code shed_skin*} bones)
 * and the silver flare in its eyes.
 */
public class ShapeshifterRenderer extends GeoEntityRenderer<ShapeshifterEntity> {

    static final String[] SHED = {"shed_skin", "shed_skin_body", "shed_skin_right_arm", "shed_skin_left_arm", "shed_skin_right_hand",
            "shed_skin_left_hand"};
    /** The stand-in each disguised shapeshifter is drawn as, kept while its disguise stays the same. */
    private static final Map<ShapeshifterEntity, Mask> MASKS = new WeakHashMap<>();

    private record Mask(String disguise, LivingEntity body, int[] lastTick) {
    }

    public ShapeshifterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LegacyGeo.Model<>("shapeshifter", "head"));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(ShapeshifterEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        String disguise = e.disguise();
        if (!e.revealed() && disguise != null && !disguise.isEmpty()) {
            LivingEntity body = mask(e, disguise);
            if (body != null) {
                EntityRenderer<? super LivingEntity> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(body);
                r.render(body, yaw, partialTick, pose, buffers, light);
                return;
            }
        }
        if (!LegacyGeo.ready("shapeshifter")) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, ShapeshifterEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            String clip = LegacyGeo.clip(e, e.getId(), "action");
            boolean shed = e.revealed() || clip.equals("revealed") || clip.equals("shed");
            for (String b : SHED) LegacyGeo.show(getGeoModel(), b, shed);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** The disguise's body, following the shapeshifter's pose; null if it cannot be made. */
    @Nullable
    private static LivingEntity mask(ShapeshifterEntity e, String disguise) {
        Mask m = MASKS.get(e);
        if (m == null || !m.disguise().equals(disguise) || m.body().level() != e.level()) {
            LivingEntity body = create(e, disguise);
            if (body == null) return null;
            m = new Mask(disguise, body, new int[]{-1});
            MASKS.put(e, m);
        }
        LivingEntity b = m.body();
        b.setPos(e.getX(), e.getY(), e.getZ());
        b.xo = e.xo;
        b.yo = e.yo;
        b.zo = e.zo;
        b.yBodyRot = e.yBodyRot;
        b.yBodyRotO = e.yBodyRotO;
        b.yHeadRot = e.yHeadRot;
        b.yHeadRotO = e.yHeadRotO;
        b.setXRot(e.getXRot());
        b.xRotO = e.xRotO;
        b.hurtTime = e.hurtTime;
        b.deathTime = e.deathTime;
        b.setOnGround(e.onGround());
        b.setInvisible(e.isInvisible());
        if (m.lastTick()[0] != e.tickCount) {
            m.lastTick()[0] = e.tickCount;
            b.tickCount = e.tickCount;
            b.walkAnimation.update(e.walkAnimation.speed(), 1.0f);
            b.swinging = e.swinging;
            b.attackAnim = e.attackAnim;
            b.oAttackAnim = e.oAttackAnim;
        }
        return b;
    }

    @Nullable
    private static LivingEntity create(ShapeshifterEntity e, String disguise) {
        try {
            int colon = disguise.indexOf(':');
            String kind = colon < 0 ? disguise : disguise.substring(0, colon), arg = colon < 0 ? "" : disguise.substring(colon + 1);
            if (kind.equals("villager")) {
                Villager v = EntityType.VILLAGER.create(e.level());
                if (v == null) return null;
                ResourceLocation id = ResourceLocation.tryParse(arg);
                VillagerProfession prof = id == null ? VillagerProfession.NONE : BuiltInRegistries.VILLAGER_PROFESSION.getOptional(id).orElse(VillagerProfession.NONE);
                v.setVillagerData(v.getVillagerData().setProfession(prof).setLevel(2));
                return v;
            }
            if (kind.equals("player") && e.level() instanceof net.minecraft.client.multiplayer.ClientLevel level) {
                UUID uuid = UUID.fromString(arg);
                var conn = Minecraft.getInstance().getConnection();
                PlayerInfo info = conn == null ? null : conn.getPlayerInfo(uuid);
                GameProfile profile = info != null ? info.getProfile() : new GameProfile(uuid, "Hunter");
                return new RemotePlayer(level, profile);
            }
        } catch (RuntimeException ignored) {
            // A malformed disguise draws the shapeshifter as itself.
        }
        return null;
    }
}
