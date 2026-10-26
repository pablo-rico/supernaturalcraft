package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A borrowed skin (v0.17 shapeshifters, v0.25 Leviathans): draws a creature as what it copies — {@code villager:<profession>}
 * (a vanilla villager of that profession) or {@code player:<uuid>} (that player, their own skin and name) — with a stand-in
 * body that follows its pose, kept while its disguise stays the same.
 */
public final class DisguiseMask {

    private DisguiseMask() {
    }

    private static final Map<LivingEntity, Mask> MASKS = new WeakHashMap<>();

    private record Mask(String disguise, LivingEntity body, int[] lastTick) {
    }

    /** Draws {@code e} as its disguise. @return false if the disguise cannot be made (draw the creature itself) */
    public static boolean render(LivingEntity e, String disguise, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        LivingEntity body = body(e, disguise);
        if (body == null) return false;
        EntityRenderer<? super LivingEntity> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(body);
        r.render(body, yaw, partialTick, pose, buffers, light);
        return true;
    }

    /** The disguise's body, following {@code e}'s pose; null if it cannot be made. */
    public static @Nullable LivingEntity body(LivingEntity e, String disguise) {
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

    private static @Nullable LivingEntity create(LivingEntity e, String disguise) {
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
            // A malformed disguise draws the creature as itself.
        }
        return null;
    }
}
