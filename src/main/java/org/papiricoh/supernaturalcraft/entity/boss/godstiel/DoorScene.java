package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.network.GodstielFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.pact.CastielEntity;
import org.papiricoh.supernaturalcraft.pact.CrowleyKingEntity;
import org.papiricoh.supernaturalcraft.pact.LabSite;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.Pacts;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.UUID;

/**
 * The door to Purgatory (v0.39): the scene between the rite and the fight, staged in the hall of Crowley's lab. Crowley waits by
 * his cages, Castiel stands before the altar, the door opens in the air above it and the souls pour into Castiel; Crowley
 * protests, Castiel snaps his fingers and the King of Hell is gone; "I'm your new God" — and Godstiel stands where Castiel was.
 * One scene at a time, ticked by the server ({@link #tick}); a restart drops it (the rite can be worked again: the step is
 * only moved on when the fight begins).
 */
public final class DoorScene {

    /** The beats, in ticks from the start. */
    public static final int CROWLEY_SPEAKS = 20, SOULS = 50, BETRAYAL = 95, SNAP = 115, NEW_GOD = 130, LENGTH = GodstielCinematics.DOOR_TICKS;

    private static @Nullable DoorScene current;

    private final ServerLevel level;
    private final LabSite site;
    private final @Nullable UUID opener;
    private final CastielEntity castiel;
    private final @Nullable CrowleyKingEntity crowley;
    private final Vec3 door;
    private int t;

    private DoorScene(ServerLevel level, LabSite site, @Nullable UUID opener, CastielEntity castiel, @Nullable CrowleyKingEntity crowley) {
        this.level = level;
        this.site = site;
        this.opener = opener;
        this.castiel = castiel;
        this.crowley = crowley;
        this.door = site.floor(LabAnchors.DOOR);
    }

    public static boolean active() {
        return current != null;
    }

    /** Stages the scene; @return whether it could */
    public static boolean start(ServerLevel level, LabSite site, @Nullable ServerPlayer opener) {
        if (current != null) return false;
        Vec3 rite = site.floor(LabAnchors.RITE);
        CastielEntity cas = AllEntities.CASTIEL.get().create(level);
        if (cas == null) return false;
        Vec3 face = site.floor(LabAnchors.ENTRY);
        place(cas, rite.add(0, 0, 0), face);
        cas.stage(true);
        level.addFreshEntity(cas);
        CrowleyKingEntity cr = AllEntities.CROWLEY_KING.get().create(level);
        if (cr != null) {
            place(cr, site.floor(LabAnchors.CROWLEY), rite);
            cr.stage(true);
            level.addFreshEntity(cr);
        }
        current = new DoorScene(level, site, opener != null ? opener.getUUID() : null, cas, cr);
        GodstielCinematics.door(cas);
        Vec3 door = current.door;
        fx(level, door, new GodstielFxPayload(-1, GodstielFxPayload.DOOR, 1, 0, door, LENGTH));
        level.playSound(null, cas.blockPosition(), AllSounds.godstiel("radiance"), SoundSource.HOSTILE, 2f, 0.5f);
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
        DoorScene s = current;
        if (s == null) return;
        if (!s.castiel.isAlive()) {
            current = null;
            return;
        }
        s.step();
    }

    private void step() {
        t++;
        if (t == CROWLEY_SPEAKS) say("message.supernaturalcraft.pact.scene.crowley_0", ChatFormatting.DARK_RED);
        if (t == SOULS) {
            fx(level, door, new GodstielFxPayload(castiel.getId(), GodstielFxPayload.SOULS, 0, 0, door, BETRAYAL - SOULS + 20));
            castiel.triggerAnim("action", "turn_away");
            level.playSound(null, castiel.blockPosition(), AllSounds.godstiel("voices"), SoundSource.HOSTILE, 2f, 1.2f);
            say("message.supernaturalcraft.pact.scene.castiel_0", ChatFormatting.AQUA);
        }
        if (t > SOULS && t < BETRAYAL && t % 4 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, door.x, door.y, door.z, 6, 0.6, 0.6, 0.6, 0.05);
            level.sendParticles(AllParticles.GRACE.get(), castiel.getX(), castiel.getY() + 1.2, castiel.getZ(), 4, 0.3, 0.6, 0.3, 0.02);
        }
        if (t == BETRAYAL) say("message.supernaturalcraft.pact.scene.crowley_1", ChatFormatting.DARK_RED);
        if (t == SNAP) {
            castiel.triggerAnim("action", "talk");
            level.playSound(null, castiel.blockPosition(), AllSounds.godstiel("snap"), SoundSource.HOSTILE, 2f, 1f);
            if (crowley != null && crowley.isAlive()) {
                crowley.leave();
                say("message.supernaturalcraft.pact.scene.crowley_2", ChatFormatting.DARK_RED);
            }
        }
        if (t == NEW_GOD) say("message.supernaturalcraft.pact.scene.castiel_1", ChatFormatting.AQUA);
        if (t >= LENGTH) finish();
    }

    private void finish() {
        current = null;
        fx(level, door, new GodstielFxPayload(-1, GodstielFxPayload.DOOR, 0, 0, door, 20));
        Vec3 at = castiel.position();
        castiel.discard();
        if (crowley != null && crowley.isAlive()) crowley.discard();
        ServerPlayer p = opener != null && level.getServer().getPlayerList().getPlayer(opener) instanceof ServerPlayer sp ? sp : null;
        GodstielEntity boss = GodstielSummoning.begin(level, p, site, at, false);
        if (boss == null) return;
        // Everyone at the lab who was on the errand now knows what Castiel did.
        for (ServerPlayer q : level.players()) {
            if (q.distanceToSqr(at) < 48 * 48 && Pacts.step(q) == PactRules.Step.RITE_READY) Pacts.advance(q, PactRules.Step.BETRAYED);
        }
        if (p != null && Pacts.step(p) == PactRules.Step.RITE_READY) Pacts.advance(p, PactRules.Step.BETRAYED);
    }

    private void say(String key, ChatFormatting style) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(castiel) < 48 * 48) p.displayClientMessage(Component.translatable(key).withStyle(style, ChatFormatting.ITALIC), false);
        }
    }

    private static void fx(ServerLevel level, Vec3 at, GodstielFxPayload payload) {
        for (ServerPlayer p : level.players()) if (p.position().distanceToSqr(at) < 96 * 96) Send.toPlayer(p, payload);
    }

    /** Drops the scene (server stopping, commands). */
    public static void reset() {
        DoorScene s = current;
        current = null;
        if (s == null) return;
        if (s.castiel.isAlive()) s.castiel.discard();
        if (s.crowley != null && s.crowley.isAlive()) s.crowley.discard();
    }
}
