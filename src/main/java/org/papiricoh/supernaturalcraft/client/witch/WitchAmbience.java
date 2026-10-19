package org.papiricoh.supernaturalcraft.client.witch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.magic.fx.ClientMagicFx;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxColor;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.entity.witch.WitchAssets;
import org.papiricoh.supernaturalcraft.entity.witch.WitchEntity;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The coven's small client-side touches (v0.21), for what the server does not send:
 * <ul>
 *   <li>a hexed villager trails faint magenta motes from its hex marks; when it starts to be freed, {@link HexLiftFx};</li>
 *   <li>a witch's hands shed motes while she casts, a magenta circle opens before her hands when her {@code hex} clip starts
 *   (a curse from afar: the server only marks the ground), and a channelling Coven Witch's motes rise from her raised hands;</li>
 *   <li>a familiar spotting the local player (its "You feel watched" on the action bar): eyes at the edges of the view
 *   ({@link MagicOverlay#watched}) and, close by the ear, a raven's caw or a cat's hiss.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class WitchAmbience {

    private static final double RANGE = 32;
    private static final String WATCHED = "message.supernaturalcraft.familiar.watched";
    private static final List<Entity> NEAR = new ArrayList<>();
    private static final Map<Entity, String> LAST_CLIP = new WeakHashMap<>();
    private static final Map<Entity, Boolean> FREEING = new WeakHashMap<>();

    private WitchAmbience() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            NEAR.clear();
            return;
        }
        if (mc.isPaused()) return;
        if (level.getGameTime() % 10 == 0) {
            NEAR.clear();
            NEAR.addAll(level.getEntitiesOfClass(Entity.class, new AABB(mc.player.blockPosition()).inflate(RANGE),
                    e -> e instanceof WitchEntity || e instanceof HexedVillagerEntity));
        }
        RandomSource r = level.random;
        for (Entity e : NEAR) {
            if (e.isRemoved() || e.isInvisible()) continue;
            if (e instanceof HexedVillagerEntity v) villager(level, v, r);
            else if (e instanceof WitchEntity w) witch(level, w, r);
        }
    }

    private static void villager(ClientLevel level, HexedVillagerEntity v, RandomSource r) {
        boolean freeing = v.freeing();
        if (freeing && !Boolean.TRUE.equals(FREEING.put(v, true))) MagicFxManager.add(new HexLiftFx(v));
        if (freeing) return;
        if (FxParticles.rate(r, 0.18f) > 0) {
            // The hex marks: on the face and the backs of the hands.
            float yaw = v.yBodyRot * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
            double y = v.getY() + v.getBbHeight() * (r.nextBoolean() ? 0.82 : 0.5);
            FxParticles.mote(level, r.nextBoolean() ? School.WITCHCRAFT.primary() : 0x5A1450, 0.7f, 30, v.getX() + fx * 0.25 + FxParticles.j(r) * 0.2,
                    y, v.getZ() + fz * 0.25 + FxParticles.j(r) * 0.2, 0, 0.012, 0);
        }
    }

    private static void witch(ClientLevel level, WitchEntity w, RandomSource r) {
        String clip = WitchGeo.clip(w, "action");
        String before = LAST_CLIP.put(w, clip);
        float yaw = w.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw), sx = -fz, sz = fx;
        Vec3 hands = new Vec3(w.getX() + fx * 0.45, w.getY() + 1.15, w.getZ() + fz * 0.45);
        if ("hex".equals(clip) && !"hex".equals(before)) {
            Vec3 look = hands.add(fx * 4, 0, fz * 4);
            ClientMagicFx.handle(new MagicFxPayload(MagicFxPayload.Kind.CAST_CIRCLE, School.WITCHCRAFT, w.getId(), hands, look, 2, 0,
                    WitchAssets.HIT_TICKS.get("hex")));
        }
        boolean channel = w instanceof CovenWitchEntity c && c.channeling();
        if (WitchRenderer.CASTING.contains(clip) && !"teleport".equals(clip) || channel) {
            for (int i = FxParticles.rate(r, channel ? 0.6f : 0.9f); i > 0; i--) {
                double side = r.nextBoolean() ? 0.32 : -0.32;
                double y = channel ? w.getY() + 2.1 : hands.y;
                double px = (channel ? w.getX() : hands.x) + sx * side, pz = (channel ? w.getZ() : hands.z) + sz * side;
                int rgb = r.nextBoolean() ? School.WITCHCRAFT.primary() : FxColor.mix(School.WITCHCRAFT.primary(), 0xFFFFFF, 0.5f);
                FxParticles.mote(level, rgb, 0.8f, 20, px + FxParticles.j(r) * 0.08, y + FxParticles.j(r) * 0.08, pz + FxParticles.j(r) * 0.08,
                        FxParticles.j(r) * 0.01, channel ? 0.06 : 0.02, FxParticles.j(r) * 0.01);
            }
        }
    }

    @SubscribeEvent
    public static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (!event.isOverlay() || !(event.getMessage().getContents() instanceof TranslatableContents tc) || !WATCHED.equals(tc.getKey())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        FamiliarEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (FamiliarEntity f : mc.level.getEntitiesOfClass(FamiliarEntity.class, mc.player.getBoundingBox().inflate(FamiliarEntity.SPY_RANGE + 4))) {
            double d = f.distanceToSqr(mc.player);
            if (d < best) {
                best = d;
                nearest = f;
            }
        }
        boolean raven = nearest != null && nearest.variant() == FamiliarEntity.Variant.RAVEN;
        MagicOverlay.watched(raven);
        Vec3 ear = mc.player.getEyePosition().add(mc.player.getViewVector(1).scale(-0.6));
        if (raven) {
            mc.level.playLocalSound(ear.x, ear.y, ear.z, AllSounds.coven("witch.raven_caw"), SoundSource.HOSTILE, 0.35f, 0.75f, false);
        } else {
            mc.level.playLocalSound(ear.x, ear.y, ear.z, SoundEvents.CAT_HISS, SoundSource.HOSTILE, 0.4f, 0.85f, false);
        }
    }
}
