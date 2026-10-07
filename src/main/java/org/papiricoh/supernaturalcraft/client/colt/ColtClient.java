package org.papiricoh.supernaturalcraft.client.colt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.ColtActionPayload;
import org.papiricoh.supernaturalcraft.network.ColtInputPayload;
import org.papiricoh.supernaturalcraft.network.ColtShotPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.colt.ColtAnimations;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;
import org.papiricoh.supernaturalcraft.reward.colt.ColtShot;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimationController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Everything the client does with the Colt: plays its animations and sounds when told (or, for
 * the local shooter, at once, predicting the server), keeps the timings the renderer reads, and
 * inspects the gun on its own when its holder has stood still long enough.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ColtClient {

    /** Ticks standing still with the Colt drawn before its holder looks it over. */
    public static final int AUTO_INSPECT = 600;
    /** How long after a shot the holder keeps the gun raised (third person). */
    public static final int AIM_HOLD = 40;

    private record Timed(long at, int player, SoundEvent sound, float volume, float pitch) {
    }

    private static final List<Timed> SOUNDS = new ArrayList<>();
    /** Per player: when they last fired. */
    private static final Map<Integer, Long> LAST_SHOT = new HashMap<>();
    /** Per gun (GeckoLib id): when it last fired, and when it began an inspection. */
    private static final Map<Long, Long> GUN_SHOT = new HashMap<>(), GUN_INSPECT = new HashMap<>();
    private static int predicted;
    private static long predictedAt;
    private static int idleTicks;
    private static Vec3 lastPos = Vec3.ZERO;
    private static float lastYaw, lastPitch;

    private ColtClient() {
    }

    private static long now() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    // --- shots ----------------------------------------------------------------------------

    /** The local player pulled the trigger and the gun will fire: play it now, the server will agree. */
    public static void predictFire(Player player, ItemStack stack) {
        boolean last = ColtItem.rounds(stack) <= 1 && !player.getAbilities().instabuild;
        predicted++;
        predictedAt = now();
        fired(player, GeoItem.getId(stack), last, true);
    }

    public static void onShot(ColtShotPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity shooter = level.getEntity(payload.shooter());
        boolean mine = mc.player != null && payload.shooter() == mc.player.getId();
        boolean wasPredicted = mine && predicted > 0;
        if (wasPredicted) predicted--;
        if (shooter instanceof Player p && !wasPredicted) fired(p, payload.geoId(), payload.lastRound(), mine);
        Vec3 from = shooter instanceof Player p ? ColtFx.muzzle(p, 1) : payload.end();
        ColtFx.tracer(from, payload.end());
        ColtShot.Outcome outcome = ColtShot.Outcome.values()[Math.floorMod(payload.outcome(), ColtShot.Outcome.values().length)];
        Entity target = payload.target() > 0 ? level.getEntity(payload.target() - 1) : null;
        switch (outcome) {
            case BLOCK -> ColtFx.impact(payload.end(), from, false);
            case BOSS, OTHER, DEFLECTED -> ColtFx.impact(payload.end(), from, true);
            case EXECUTED -> {
                ColtFx.execution(target, payload.end());
                Vec3 at = target != null ? target.position() : payload.end();
                level.playLocalSound(at.x, at.y, at.z, AllSounds.COLT_EXECUTE.get(), SoundSource.PLAYERS, 1.2f, 0.9f, false);
                level.playLocalSound(at.x, at.y, at.z, AllSounds.COLT_EXECUTE_ZAP.get(), SoundSource.PLAYERS, 1f, 1f, false);
            }
            default -> {
            }
        }
    }

    /** The gun goes off in {@code player}'s hand: animation, blast, smoke, and the kick if it is ours. */
    private static void fired(Player player, long geoId, boolean last, boolean local) {
        long now = now();
        LAST_SHOT.put(player.getId(), now);
        GUN_SHOT.put(geoId, now);
        GUN_INSPECT.remove(geoId);
        cancelSounds(player.getId());
        trigger(geoId, last ? ColtAnimations.FIRE_LAST : ColtAnimations.FIRE);
        ColtPlayerAnims.get().fire(player);
        play(player, AllSounds.COLT_SHOT.get(), 1.6f, 0.95f + player.getRandom().nextFloat() * 0.1f);
        play(player, AllSounds.COLT_SHOT_BODY.get(), 1.2f, 1f);
        play(player, AllSounds.COLT_SHOT_TAIL.get(), 1f, 1f);
        if (!last) schedule(now + 7, player, AllSounds.COLT_COCK.get(), 0.7f, 1f);
        ColtFx.muzzleBlast(player);
        if (local && player == Minecraft.getInstance().player) {
            ColtRecoil.kick();
            ColtOverlay.flash();
            idleTicks = 0;
        }
    }

    // --- other actions --------------------------------------------------------------------

    public static void onAction(ColtActionPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(payload.player()) instanceof Player p)) return;
        long now = now(), geo = payload.geoId();
        if (p == Minecraft.getInstance().player) idleTicks = 0;
        switch (payload.action()) {
            case ColtActionPayload.DRY_FIRE -> {
                cancelSounds(p.getId());
                trigger(geo, ColtAnimations.DRY_FIRE);
                ColtPlayerAnims.get().dryFire(p);
                play(p, AllSounds.COLT_CLICK.get(), 0.9f, 1f);
            }
            case ColtActionPayload.RELOAD -> {
                int n = Math.max(1, Math.min(ColtItem.CAPACITY, payload.arg()));
                cancelSounds(p.getId());
                GUN_INSPECT.remove(geo);
                trigger(geo, ColtAnimations.reload(n));
                ColtPlayerAnims.get().reload(p, n);
                schedule(now + 4, p, AllSounds.COLT_LEVER.get(), 0.6f, 1.1f);
                for (int k = 0; k < n; k++) {
                    schedule(now + ColtReload.insertTick(k) - 2, p, AllSounds.COLT_RELOAD.get(), 0.7f, 1f + k * 0.04f);
                    schedule(now + ColtReload.insertTick(k), p, AllSounds.COLT_LEVER.get(), 0.4f, 1.4f);
                }
                schedule(now + ColtReload.total(n) - 3, p, AllSounds.COLT_COCK.get(), 0.8f, 0.95f);
            }
            case ColtActionPayload.RELOAD_ABORT -> {
                cancelSounds(p.getId());
                trigger(geo, ColtAnimations.RELOAD_ABORT);
                ColtPlayerAnims.get().abort(p);
                schedule(now + 4, p, AllSounds.COLT_COCK.get(), 0.7f, 1f);
            }
            case ColtActionPayload.INSPECT -> {
                cancelSounds(p.getId());
                GUN_INSPECT.put(geo, now);
                ItemStack held = p.getMainHandItem();
                trigger(geo, held.getItem() instanceof ColtItem && ColtItem.rounds(held) == 0
                        ? ColtAnimations.INSPECT_EMPTY : ColtAnimations.INSPECT);
                ColtPlayerAnims.get().inspect(p);
                schedule(now + 24, p, AllSounds.COLT_SPIN.get(), 0.5f, 1f);
                schedule(now + 32, p, AllSounds.COLT_SPIN.get(), 0.4f, 1.2f);
                schedule(now + 44, p, AllSounds.COLT_COCK.get(), 0.7f, 1f);
            }
            default -> {
            }
        }
    }

    /** Restarts one of the gun's animations, even if it is the one already playing. */
    public static void trigger(long geoId, String name) {
        if (geoId == Long.MAX_VALUE) return;
        ColtItem colt = AllItems.THE_COLT.get();
        AnimationController<?> controller = colt.getAnimatableInstanceCache().getManagerForId(geoId).getAnimationControllers().get("main");
        if (controller == null) return;
        controller.forceAnimationReset();
        controller.tryTriggerAnimation(name);
    }

    // --- timings the renderer and HUD read ------------------------------------------------

    /** Ticks since this gun last fired (large if never). */
    public static double sinceShot(long geoId, float partial) {
        Long t = GUN_SHOT.get(geoId);
        return t == null ? 1e9 : now() - t + partial;
    }

    /** Ticks since this gun's inspection began (large if none is running). */
    public static double sinceInspect(long geoId, float partial) {
        Long t = GUN_INSPECT.get(geoId);
        return t == null ? 1e9 : now() - t + partial;
    }

    /** Ticks since {@code player} last fired (large if never). */
    public static long sincePlayerShot(Player player) {
        Long t = LAST_SHOT.get(player.getId());
        return t == null ? Long.MAX_VALUE / 2 : now() - t;
    }

    // --- sounds ---------------------------------------------------------------------------

    private static void play(Player p, SoundEvent sound, float volume, float pitch) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) level.playLocalSound(p.getX(), p.getEyeY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static void schedule(long at, Player p, SoundEvent sound, float volume, float pitch) {
        SOUNDS.add(new Timed(at, p.getId(), sound, volume, pitch));
    }

    private static void cancelSounds(int player) {
        SOUNDS.removeIf(t -> t.player == player);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            SOUNDS.clear();
            LAST_SHOT.clear();
            GUN_SHOT.clear();
            GUN_INSPECT.clear();
            predicted = 0;
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Timed> it = SOUNDS.iterator(); it.hasNext(); ) {
            Timed t = it.next();
            if (now < t.at) continue;
            it.remove();
            if (now - t.at < 10 && level.getEntity(t.player) instanceof Player p) play(p, t.sound, t.volume, t.pitch);
        }
        // A prediction the server never answered (it disagreed) is forgotten after a moment.
        if (predicted > 0 && now - predictedAt > 40) predicted = 0;
        GUN_INSPECT.values().removeIf(t -> now - t > 60);
        autoInspect(mc.player);
    }

    private static void autoInspect(LocalPlayer player) {
        if (player == null || !(player.getMainHandItem().getItem() instanceof ColtItem) || ColtItem.reloading(player.getMainHandItem())
                || Minecraft.getInstance().screen != null) {
            idleTicks = 0;
            return;
        }
        boolean still = player.position().distanceToSqr(lastPos) < 1e-4 && Math.abs(player.getYRot() - lastYaw) < 0.5f
                && Math.abs(player.getXRot() - lastPitch) < 0.5f;
        lastPos = player.position();
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
        if (!still) {
            idleTicks = 0;
            return;
        }
        if (++idleTicks >= AUTO_INSPECT) {
            idleTicks = 0;
            PacketDistributor.sendToServer(new ColtInputPayload(ColtInputPayload.INSPECT));
        }
    }

    /** The reload / inspect keys, with the Colt in the main hand. */
    public static void key(byte action) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof ColtItem)) return;
        idleTicks = 0;
        PacketDistributor.sendToServer(new ColtInputPayload(action));
    }
}
