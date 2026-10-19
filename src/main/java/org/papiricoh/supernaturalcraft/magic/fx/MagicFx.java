package org.papiricoh.supernaturalcraft.magic.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload.Kind;
import org.papiricoh.supernaturalcraft.network.MarkSyncPayload;
import org.papiricoh.supernaturalcraft.network.MasterySyncPayload;
import org.papiricoh.supernaturalcraft.network.RitualChargePayload;
import org.papiricoh.supernaturalcraft.network.Send;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Server side of the magic's effects (v0.21): builds and sends {@link MagicFxPayload}s, {@link RitualChargePayload}s and the
 * owner-only syncs, always through {@link Send}. Point effects reach players within {@link #RANGE} blocks; a bowl's sending
 * reaches those near either end. Every method is safe to call on a client level (it does nothing).
 */
public final class MagicFx {

    /** How far (blocks) a point effect is seen. */
    public static final double RANGE = 64;

    private MagicFx() {
    }

    // --- generic ---------------------------------------------------------------------------------------------------------

    /** Any payload, to players near its {@code pos}. */
    public static void send(ServerLevel level, MagicFxPayload payload) {
        Vec3 p = payload.pos();
        Send.near(level, null, p.x, p.y, p.z, RANGE, payload);
    }

    /** Any payload, to players near its {@code pos} or its {@code target} (each once). */
    public static void sendBothEnds(ServerLevel level, MagicFxPayload payload) {
        double r2 = RANGE * RANGE;
        for (ServerPlayer p : level.players()) {
            if (p instanceof FakePlayer) continue;
            if (p.distanceToSqr(payload.pos()) < r2 || p.distanceToSqr(payload.target()) < r2) Send.toPlayer(p, payload);
        }
    }

    private static int id(@Nullable Entity e) {
        return e == null ? -1 : e.getId();
    }

    // --- sigils ----------------------------------------------------------------------------------------------------------

    /** CAST_CIRCLE: a cast circle at the caster's hand ({@code feet} false) or feet, toward {@code look}. */
    public static void cast(ServerLevel level, Entity caster, School school, Vec3 pos, Vec3 look, int tier, boolean feet, int duration) {
        send(level, new MagicFxPayload(Kind.CAST_CIRCLE, school, caster.getId(), pos, look, tier, feet ? 1 : 0, duration));
    }

    /** BOLT_TRAIL: a sigil bolt sets off from {@code from} toward {@code heading}. */
    public static void boltTrail(ServerLevel level, Entity bolt, School school, Vec3 from, Vec3 heading, int impact, float speed,
                                 int maxTicks) {
        send(level, new MagicFxPayload(Kind.BOLT_TRAIL, school, bolt.getId(), from, heading, impact, speed, maxTicks));
    }

    /** IMPACT: an effect lands at {@code pos} (from {@code from}); {@code impact} indexes {@code MagicAssets.IMPACTS}. */
    public static void impact(ServerLevel level, School school, Vec3 pos, Vec3 from, int impact, float radius, @Nullable Entity hit,
                              int duration) {
        send(level, new MagicFxPayload(Kind.IMPACT, school, id(hit), pos, from, impact, radius, duration));
    }

    /** CHAIN_ARC: an arc jumps from {@code from} to {@code to} ({@code hop} 0 first). */
    public static void chain(ServerLevel level, School school, Vec3 from, Vec3 to, @Nullable Entity struck, int hop, int impact,
                             int duration) {
        send(level, new MagicFxPayload(Kind.CHAIN_ARC, school, id(struck), from, to, hop, impact, duration));
    }

    /** WARD_PULSE: a ward goes up, is struck or breaks ({@code MagicFxPayload.WARD_*}). */
    public static void ward(ServerLevel level, School school, @Nullable Entity ward, Vec3 centre, float radius, int state, int duration) {
        send(level, new MagicFxPayload(Kind.WARD_PULSE, school, id(ward), centre, centre, radius, state, duration));
    }

    // --- rituals ---------------------------------------------------------------------------------------------------------

    /**
     * A rite starts charging at {@code altar}: the circle's cells (any blocks at the altar's height; those outside the 13×13
     * window and the altar itself are left out), lit over {@code duration} ticks from {@code progress}.
     */
    public static void ritualCharge(ServerLevel level, BlockPos altar, School school, Collection<BlockPos> cells, int duration,
                                    float progress) {
        List<Short> packed = new ArrayList<>(Math.min(cells.size(), RitualChargePayload.MAX_CELLS));
        for (BlockPos c : cells) {
            int dx = c.getX() - altar.getX();
            int dz = c.getZ() - altar.getZ();
            if ((dx != 0 || dz != 0) && RitualChargePayload.fits(dx, dz) && packed.size() < RitualChargePayload.MAX_CELLS) {
                packed.add(RitualChargePayload.pack(dx, dz));
            }
        }
        Send.near(level, null, altar.getX() + 0.5, altar.getY() + 0.5, altar.getZ() + 0.5, RANGE,
                new RitualChargePayload(altar, school, packed, Math.max(1, duration), progress));
    }

    /** The charge at {@code altar} is over (completed, failed or broken). */
    public static void ritualStop(ServerLevel level, BlockPos altar) {
        Send.near(level, null, altar.getX() + 0.5, altar.getY() + 0.5, altar.getZ() + 0.5, RANGE, RitualChargePayload.stop(altar));
    }

    /** RITUAL_COMPLETE: the school's pillar and shockwave over {@code altar}. */
    public static void ritualComplete(ServerLevel level, BlockPos altar, School school, float pillarHeight, float waveRadius, int duration) {
        Vec3 top = Vec3.atCenterOf(altar).add(0, 0.5, 0);
        send(level, new MagicFxPayload(Kind.RITUAL_COMPLETE, school, -1, top, top, pillarHeight, waveRadius, duration));
    }

    /** RITUAL_FAIL: the circle implodes onto {@code altar}. */
    public static void ritualFail(ServerLevel level, BlockPos altar, School school, float radius, boolean breach, int duration) {
        Vec3 top = Vec3.atCenterOf(altar).add(0, 0.5, 0);
        send(level, new MagicFxPayload(Kind.RITUAL_FAIL, school, -1, top, top, radius, breach ? 1 : 0, duration));
    }

    /** SUMMON_RIFT: a rift opens at {@code base}, facing {@code facing}. */
    public static void rift(ServerLevel level, School school, Vec3 base, Vec3 facing, @Nullable Entity summoned, float height,
                            float width, int duration) {
        send(level, new MagicFxPayload(Kind.SUMMON_RIFT, school, id(summoned), base, facing, height, width, duration));
    }

    // --- the bowl --------------------------------------------------------------------------------------------------------

    /** BOWL_SEAL: the smoke over the bowl draws the spell's seal ({@code MagicAssets.SEALS} index). */
    public static void bowlSeal(ServerLevel level, School school, Vec3 bowlTop, @Nullable Entity caster, int seal, int duration) {
        send(level, new MagicFxPayload(Kind.BOWL_SEAL, school, id(caster), bowlTop, bowlTop, seal, 0, duration));
    }

    /** BOWL_SEND: the seal travels from the bowl to {@code destination} (seen near either end). */
    public static void bowlSend(ServerLevel level, School school, Vec3 bowlTop, Vec3 destination, @Nullable Entity target, int seal,
                                int travelTicks) {
        sendBothEnds(level, new MagicFxPayload(Kind.BOWL_SEND, school, id(target), bowlTop, destination, seal, 0, travelTicks));
    }

    /** BOWL_BACKLASH: the spell turns on the bowl. */
    public static void bowlBacklash(ServerLevel level, School school, Vec3 bowlTop, @Nullable Entity caster, float strength, int seal,
                                    int duration) {
        send(level, new MagicFxPayload(Kind.BOWL_BACKLASH, school, id(caster), bowlTop, bowlTop, strength, seal, duration));
    }

    // --- hexes, traps, the Mark --------------------------------------------------------------------------------------------

    /** HEX_PULSE: a curse takes hold, or a protection turns something away ({@code MagicFxPayload.HEX_*}). */
    public static void hex(ServerLevel level, @Nullable Entity hexed, Vec3 pos, int mode, float radius, int duration) {
        send(level, new MagicFxPayload(Kind.HEX_PULSE, School.WITCHCRAFT, id(hexed), pos, pos, mode, radius, duration));
    }

    /**
     * HEX_PULSE / {@code HEX_WARDS}: tells the players tracking {@code bearer} which protections (Mirror Hex, Ward of the Name)
     * guard them now; {@code ending}, if not null, is left out (it is being removed). Not to the bearer (their own client
     * reads its effects).
     */
    public static void wardState(LivingEntity bearer, @Nullable net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> ending) {
        if (!(bearer.level() instanceof ServerLevel)) return;
        int flags = 0;
        if (bearer.hasEffect(AllMobEffects.MIRRORED) && (ending == null || !ending.is(AllMobEffects.MIRRORED.getKey()))) flags |= MagicFxPayload.WARDS_MIRRORED;
        if (bearer.hasEffect(AllMobEffects.NAME_WARDED) && (ending == null || !ending.is(AllMobEffects.NAME_WARDED.getKey()))) flags |= MagicFxPayload.WARDS_NAME_WARDED;
        Vec3 p = bearer.position();
        Send.trackingEntity(bearer, new MagicFxPayload(Kind.HEX_PULSE, School.WITCHCRAFT, bearer.getId(), p, p, MagicFxPayload.HEX_WARDS,
                flags, org.papiricoh.supernaturalcraft.hex.CurseEffects.Shield.SHOWN_EVERY + 20));
    }

    /** TRAP_FLARE: a devil's trap reacts ({@code MagicFxPayload.TRAP_*}). */
    public static void trapFlare(ServerLevel level, Vec3 centre, @Nullable Entity caught, float radius, int state, int duration) {
        send(level, new MagicFxPayload(Kind.TRAP_FLARE, School.HUNTER, id(caught), centre, centre, radius, state, duration));
    }

    /** MARK_FLARE: the Mark of Cain flares on {@code marked} ({@code MagicFxPayload.MARK_*}). */
    public static void markFlare(ServerLevel level, Entity marked, float thirst, int state, int duration) {
        Vec3 p = marked.position();
        send(level, new MagicFxPayload(Kind.MARK_FLARE, School.INFERNAL, marked.getId(), p, p, thirst, state, duration));
    }

    /** TELEPORT (v0.21): {@code who} vanishes in smoke at {@code from} and reappears at {@code to} (seen near either end). */
    public static void teleport(ServerLevel level, Entity who, School school, Vec3 from, Vec3 to, int smoke, int duration) {
        sendBothEnds(level, new MagicFxPayload(Kind.TELEPORT, school, who.getId(), from, to, smoke, who.getBbHeight(), duration));
    }

    // --- owner-only syncs ------------------------------------------------------------------------------------------------

    /** The owner's Mark and thirst (call only when it changed, and on login/respawn/dimension change). */
    public static void syncMark(ServerPlayer player, boolean marked, float thirst) {
        if (player instanceof FakePlayer) return;
        Send.toPlayer(player, new MarkSyncPayload(marked, thirst));
    }

    /** The owner's mastery XP (call only when it changed, and on login/respawn/dimension change). */
    public static void syncMastery(ServerPlayer player, int enochian, int infernal, int witchcraft, int hunter) {
        if (player instanceof FakePlayer) return;
        Send.toPlayer(player, new MasterySyncPayload(enochian, infernal, witchcraft, hunter));
    }
}
