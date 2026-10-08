package org.papiricoh.supernaturalcraft.client.michael;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;
import org.papiricoh.supernaturalcraft.reward.michael.WingFlight;
import org.papiricoh.supernaturalcraft.reward.michael.WingStamina;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's side of Michael's fight: every {@link MichaelFxPayload} lands here and is handed to the HUD and effects.
 * It also keeps what lasts: the Heaven round you (its sky and fog), being worn (third-person camera, no control of your
 * own body), being pinned by the lance, who wears Heaven's mark, and the Seraph Wings' stamina mirrored for the HUD.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientMichael {

    /** Ticks the air of a new Heaven takes to settle. */
    private static final int HEAVEN_FADE = 60;

    private static int heaven = -1, heavenMichael = -1;
    private static long heavenSince, ticks;
    private static long possessedUntil = -1, pinnedUntil = -1;
    private static CameraType cameraBefore;
    private static final Map<Integer, Long> MARKED = new HashMap<>();
    private static WingStamina stamina;

    private ClientMichael() {
    }

    public static void handle(MichaelFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (p.kind()) {
            case MichaelFxPayload.TITLE -> MichaelOverlay.add(new MichaelOverlay.TitleCard(p.arg(), p.duration()));
            case MichaelFxPayload.ASK_YES -> {
                if (mc.screen == null || !(mc.screen instanceof VesselScreen)) mc.setScreen(new VesselScreen(p.entity(), p.duration()));
            }
            case MichaelFxPayload.POSSESSED -> {
                if (p.arg() == 1) possess(p.duration());
                else release();
            }
            case MichaelFxPayload.MARK -> MARKED.put(p.arg(), ticks + p.duration());
            case MichaelFxPayload.LANCE_PIN -> {
                if (p.arg() == mc.player.getId()) {
                    pinnedUntil = ticks + p.duration();
                    MichaelOverlay.add(new MichaelOverlay.Flash(14, 2, 0xFFFFFF, 0.5f));
                }
            }
            case MichaelFxPayload.ARENA_SHIFT -> {
                heaven = p.arg();
                heavenMichael = p.entity();
                heavenSince = ticks;
                MichaelOverlay.add(new MichaelOverlay.Flash(36, 10, 0xFFF6DD, 0.55f));
            }
            case MichaelFxPayload.TRANSFORM -> MichaelOverlay.add(new MichaelOverlay.Flash(p.duration(),
                    org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance.TRANSFORM_SWAP_TICKS, 0xFFFFFF, 0.95f));
            case MichaelFxPayload.FEATHER_BURST -> {
                if (mc.player.position().distanceToSqr(p.point()) < 20 * 20) MichaelOverlay.add(new MichaelOverlay.Feathers(p.arg() / 2));
            }
            case MichaelFxPayload.FAVOR -> MichaelOverlay.add(new MichaelOverlay.Edges(p.duration(), MichaelGui.GOLD, 0.35f));
            default -> {
            }
        }
    }

    // --- what lasts ----------------------------------------------------------------------------------------------

    /** The Heaven round the player (0 the Garden, 1 the War, 2 the Throne Room), or -1 outside Michael's fight. */
    public static int heaven() {
        return heaven;
    }

    /** How far the current Heaven's air has settled, 0..1. */
    public static float heavenFade(float partial) {
        return Mth.clamp((ticks - heavenSince + partial) / HEAVEN_FADE, 0f, 1f);
    }

    /** Preview hook: show Heaven {@code which} as if he had shifted it (-1 clears it). */
    public static void forceHeaven(int which) {
        heaven = which;
        heavenMichael = -1;
        heavenSince = ticks - HEAVEN_FADE;
    }

    public static boolean possessed() {
        return possessedUntil >= ticks;
    }

    public static boolean pinned() {
        return pinnedUntil >= ticks;
    }

    public static boolean marked(int entityId) {
        Long until = MARKED.get(entityId);
        return until != null && until >= ticks;
    }

    /** The wings' stamina as this client reckons it (null while the wings do not carry the player). */
    public static WingStamina stamina() {
        return stamina;
    }

    private static void possess(int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (!possessed()) cameraBefore = mc.options.getCameraType();
        possessedUntil = ticks + duration;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (mc.screen instanceof VesselScreen) mc.setScreen(null);
    }

    private static void release() {
        possessedUntil = -1;
        Minecraft mc = Minecraft.getInstance();
        if (cameraBefore != null) mc.options.setCameraType(cameraBefore);
        cameraBefore = null;
    }

    /** Preview hook: as if he wore the player for {@code ticks} (0 lets go). */
    public static void forcePossessed(int duration) {
        if (duration > 0) possess(duration);
        else release();
    }

    /** Preview hook: as if the player had Heaven's mark for {@code duration}. */
    public static void forceMark(int entityId, int duration) {
        MARKED.put(entityId, ticks + duration);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        MichaelOverlay.tick();
        MichaelHud.tick();
        if (mc.level == null || mc.player == null) {
            heaven = -1;
            MARKED.clear();
            stamina = null;
            if (possessedUntil >= 0) release();
            return;
        }
        if (possessedUntil >= 0 && !possessed()) release();
        // Heaven's air goes with him: once he is gone (or far), the sky is the world's again.
        if (heaven >= 0 && heavenMichael >= 0 && ticks % 20 == 0) {
            if (!(mc.level.getEntity(heavenMichael) instanceof MichaelEntity m) || !m.isAlive() || m.distanceToSqr(mc.player) > 96 * 96) {
                heaven = -1;
            }
        }
        MARKED.values().removeIf(until -> until < ticks);
        // The mark burns above every marked hunter's head.
        if (ticks % 4 == 0) {
            for (int id : MARKED.keySet()) {
                Entity e = mc.level.getEntity(id);
                if (e != null && (e != mc.player || !mc.options.getCameraType().isFirstPerson())) {
                    mc.level.addParticle(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), 0, 0.02, 0);
                }
            }
        }
        tickStamina(mc.player);
    }

    /** Mirrors the server's reckoning of the wings: flight drains, the ground refills (only while the wings carry you). */
    private static void tickStamina(Player player) {
        // The Seraph Wings on Michael's Grace, or an angel's own wings (v0.13: Allegiances.wingsGranted).
        boolean worn = WingFlight.wearingWings(player)
                || org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance.wingsGranted(player);
        boolean wings = !player.isCreative() && !player.isSpectator() && worn && (player.getAbilities().mayfly
                || stamina != null && stamina.exhausted());
        if (!wings) {
            stamina = null;
            return;
        }
        if (stamina == null) {
            int seconds;
            try {
                seconds = org.papiricoh.supernaturalcraft.SNConfig.GRACE_FLIGHT_SECONDS.get();
            } catch (IllegalStateException notLoaded) {
                seconds = 20;
            }
            stamina = new WingStamina(seconds);
        }
        stamina.tick(player.getAbilities().flying, player.onGround());
    }

    /** While he wears you, or the lance pins you, the body is not yours. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!possessed() && !pinned()) return;
        Input in = event.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!possessed()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }
}
