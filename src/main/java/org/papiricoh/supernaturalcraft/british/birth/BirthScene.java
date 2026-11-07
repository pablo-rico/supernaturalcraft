package org.papiricoh.supernaturalcraft.british.birth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.british.British;
import org.papiricoh.supernaturalcraft.british.BritishFxPayload;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehouseAnchors;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.nephilim.jack.JackCompanions;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Jack's birth (v0.41, season 12's end), staged in Kelly Kline's safe house once Dagon falls. Kelly lies in labour on her cot;
 * Lucifer, in President Rourke's body, walks in for his son; her light pours out and fills the room; he reaches for the child
 * and the light throws him back, through a rift torn open behind him, into another world. Kelly fades. Then every hunter owed
 * the birth moves on to {@link NephilimRules.Step#BORN}, the rift stays open ({@link ApocalypseWorld#openRift}), and Jack
 * joins them ({@link JackCompanions#arrive}).
 *
 * <p>One scene at a time, ticked by the server ({@link #tick}). A restart drops the scene, never the birth: whoever is still
 * owed it is given it without the scene ({@link #catchUp}).
 */
public final class BirthScene {

    /** {@code Nephilim.scenes} bit: this hunter saw the birth (bits 0-7 are this slice's). */
    public static final int SCENE_BIRTH = 1;
    /** The beats, in ticks from the start. */
    public static final int KELLY_SPEAKS = 15, LUCIFER_COMES = 50, LUCIFER_SPEAKS = 70, LIGHT = 95, REACH = 125, THROWN = 150,
            GONE = 168, FADE = 175, LENGTH = 240;
    private static final double RANGE = 72;

    private static @Nullable BirthScene current;

    private final ServerLevel level;
    private final List<ServerPlayer> hunters;
    private final Vec3 cot, rourkeAt, rift;
    private final KellyKlineEntity kelly;
    private @Nullable RourkeLuciferEntity lucifer;
    private int t;

    private BirthScene(ServerLevel level, List<ServerPlayer> hunters, Vec3 cot, Vec3 rourkeAt, Vec3 rift, KellyKlineEntity kelly) {
        this.level = level;
        this.hunters = hunters;
        this.cot = cot;
        this.rourkeAt = rourkeAt;
        this.rift = rift;
        this.kelly = kelly;
    }

    public static boolean active() {
        return current != null;
    }

    /** Whether {@code p} is owed the birth (on the trail, with Dagon down or about to be). */
    public static boolean owed(ServerPlayer p) {
        return Nephilims.step(p) == NephilimRules.Step.TRACING;
    }

    /**
     * Dagon has fallen at {@code at} (at the safe house {@code site}, or anywhere by command): the birth for whoever among
     * {@code hunters} is owed it. A rematch (nobody owed) plays nothing.
     */
    public static void after(ServerLevel level, @Nullable PlacedSite site, Vec3 at, List<ServerPlayer> hunters) {
        List<ServerPlayer> owed = new ArrayList<>();
        for (ServerPlayer p : hunters) if (owed(p)) owed.add(p);
        if (owed.isEmpty()) return;
        Vec3 cot = site != null ? site.floor(SafehouseAnchors.COT) : at.add(2, 0, 0);
        Vec3 rourke = site != null ? site.floor(SafehouseAnchors.ROURKE) : at.add(-2, 0, 2);
        Vec3 rift = site != null ? site.floor(SafehouseAnchors.RIFT) : at.add(-5, 0, 5);
        if (!start(level, owed, cot, rourke, rift)) {
            for (ServerPlayer p : owed) born(p, BlockPos.containing(cot));
            ApocalypseWorld.openRift(level, BlockPos.containing(rift), ApocalypseWorld.BIRTH_RIFT_TICKS);
        }
    }

    /** Stages the scene; @return whether it could */
    public static boolean start(ServerLevel level, List<ServerPlayer> hunters, Vec3 cot, Vec3 rourkeAt, Vec3 rift) {
        if (current != null) return false;
        KellyKlineEntity k = BritishRegistry.KELLY_KLINE.get().create(level);
        if (k == null) return false;
        place(k, cot, rourkeAt);
        k.stage(true);
        level.addFreshEntity(k);
        current = new BirthScene(level, List.copyOf(hunters), cot, rourkeAt, rift, k);
        CinematicLocks.play(k, SupernaturalCraft.asResource("jack_birth"), LENGTH, RANGE);
        BritishFxPayload.send(level, k.getId(), BritishFxPayload.TITLE, BritishFxPayload.WHO_BIRTH, 1, cot, 80);
        level.playSound(null, k.blockPosition(), BritishRegistry.sound("kelly_kline.labor"), SoundSource.NEUTRAL, 1.4f, 1f);
        return true;
    }

    private static void place(StoryNpcEntity npc, Vec3 at, Vec3 face) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        npc.moveTo(at.x, at.y, at.z, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
    }

    /** Once a tick. */
    public static void tick() {
        BirthScene s = current;
        if (s == null) return;
        if (!s.kelly.isAlive() && s.t < FADE) {
            s.finish();
            return;
        }
        s.step();
    }

    private void step() {
        t++;
        if (t == KELLY_SPEAKS) say("message.supernaturalcraft.birth.kelly_0", ChatFormatting.WHITE);
        if (t == LUCIFER_COMES) {
            RourkeLuciferEntity r = BritishRegistry.ROURKE_LUCIFER.get().create(level);
            if (r != null) {
                place(r, rourkeAt, cot);
                r.stage(true);
                level.addFreshEntity(r);
                lucifer = r;
            }
        }
        if (t == LUCIFER_SPEAKS) say("message.supernaturalcraft.birth.lucifer_0", ChatFormatting.RED);
        if (t == LIGHT) {
            kelly.triggerAnim("action", "glow");
            BritishFxPayload.send(level, kelly.getId(), BritishFxPayload.FLASH, 60, BritishFxPayload.LIGHT, cot.add(0, 1, 0), REACH - LIGHT + 40);
            level.playSound(null, kelly.blockPosition(), BritishRegistry.sound("kelly_kline.glow"), SoundSource.NEUTRAL, 2f, 1f);
            say("message.supernaturalcraft.birth.kelly_1", ChatFormatting.WHITE);
        }
        if (t > LIGHT && t < FADE && t % 4 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, cot.x, cot.y + 1, cot.z, 6, 0.8, 0.8, 0.8, 0.08);
        }
        if (t == REACH && lucifer != null && lucifer.isAlive()) {
            lucifer.triggerAnim("action", "reach");
            say("message.supernaturalcraft.birth.lucifer_1", ChatFormatting.RED);
        }
        if (t == THROWN && lucifer != null && lucifer.isAlive()) {
            Vec3 away = rift.subtract(lucifer.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(0, 0, -1);
            away = away.normalize().scale(1.4);
            lucifer.setDeltaMovement(away.x, 0.4, away.z);
            lucifer.hurtMarked = true;
            lucifer.leave();
            BritishFxPayload.send(level, lucifer.getId(), BritishFxPayload.FLASH, 30, BritishFxPayload.LIGHT, lucifer.position().add(0, 1, 0), 20);
            BritishFxPayload.send(level, -1, BritishFxPayload.RING, 25, BritishFxPayload.FORCE, rift, LENGTH - THROWN);
        }
        if (t == GONE) say("message.supernaturalcraft.birth.gone", ChatFormatting.GRAY);
        if (t == FADE) {
            kelly.triggerAnim("action", "fade");
            level.playSound(null, kelly.blockPosition(), BritishRegistry.sound("kelly_kline.cry"), SoundSource.NEUTRAL, 1.5f, 1.3f);
            say("message.supernaturalcraft.birth.kelly_2", ChatFormatting.WHITE);
        }
        if (t >= LENGTH) finish();
    }

    private void finish() {
        current = null;
        if (kelly.isAlive()) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, kelly.getX(), kelly.getY() + 0.8, kelly.getZ(), 30, 0.4, 0.4, 0.4, 0.02);
            kelly.discard();
        }
        if (lucifer != null && lucifer.isAlive()) lucifer.discard();
        ApocalypseWorld.openRift(level, BlockPos.containing(rift), ApocalypseWorld.BIRTH_RIFT_TICKS);
        for (ServerPlayer was : hunters) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(was.getUUID());
            if (p == null && !was.isRemoved()) p = was;
            if (p != null) born(p, BlockPos.containing(cot));
        }
        say("message.supernaturalcraft.birth.born", ChatFormatting.GOLD);
    }

    /** {@code p} moves on to BORN (once) and Jack comes to them at {@code at}. */
    static void born(ServerPlayer p, @Nullable BlockPos at) {
        if (!owed(p)) return;
        Nephilims.advance(p, NephilimRules.Step.BORN);
        Nephilims.update(p, n -> n.withSeen(SCENE_BIRTH));
        JackCompanions.arrive(p, at);
    }

    /**
     * A hunter still owed the birth after Dagon fell for them (the scene was dropped by a restart): they are given it now,
     * without the scene, and Jack comes to their side. @return whether it happened
     */
    public static boolean catchUp(ServerPlayer p) {
        if (current != null && current.hunters.stream().anyMatch(h -> h.getUUID().equals(p.getUUID()))) return false;
        if (!owed(p) || !BritishOrder.get(p).has(British.DAGON_BEATEN)) return false;
        born(p, null);
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.birth.born").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private void say(String key, ChatFormatting style) {
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(cot) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(style, ChatFormatting.ITALIC), false);
        }
    }

    /** Drops the scene (server stopping, commands): its hunters are owed the birth and get it by {@link #catchUp}. */
    public static void reset() {
        BirthScene s = current;
        current = null;
        if (s == null) return;
        if (s.kelly.isAlive()) s.kelly.discard();
        if (s.lucifer != null && s.lucifer.isAlive()) s.lucifer.discard();
    }
}
