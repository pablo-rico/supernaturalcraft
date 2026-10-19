package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload.Kind;
import org.papiricoh.supernaturalcraft.network.RitualChargePayload;

/**
 * Client end of the magic's effects (v0.21): plays each {@link MagicFxPayload} and each ritual charge ({@link RitualChargePayload})
 * through {@link MagicFxManager}. Referenced from {@code SNNetworking} only through lambdas; the previews call it with payloads of
 * their own.
 *
 * <table>
 *   <caption>What each kind becomes</caption>
 *   <tr><td>CAST_CIRCLE</td><td>{@link CastCircleFx}: a turning ring of glyphs at the hand or the feet</td></tr>
 *   <tr><td>BOLT_TRAIL</td><td>{@link BoltTrails}: the bolt's school and impact for {@link SigilBoltRenderer} and its trail</td></tr>
 *   <tr><td>IMPACT</td><td>{@link ImpactFx}: each effect's own signature</td></tr>
 *   <tr><td>CHAIN_ARC</td><td>{@link ChainArcFx}: forking lightning between targets</td></tr>
 *   <tr><td>WARD_PULSE</td><td>{@link WardPulseFx}: the dome raised, struck or broken; on a Coven Witch, her bubble
 *   ({@link ShieldBubbles})</td></tr>
 *   <tr><td>RITUAL_COMPLETE / FAIL</td><td>{@link RitualBurstFx}: the pillar and shockwave, or the implosion</td></tr>
 *   <tr><td>SUMMON_RIFT</td><td>{@link SummonRiftFx}: a tear in the air</td></tr>
 *   <tr><td>BOWL_SEAL / SEND / BACKLASH</td><td>{@link BowlFx}: the seal written, flown, or burst</td></tr>
 *   <tr><td>HEX_PULSE / TRAP_FLARE / MARK_FLARE</td><td>{@link SignFx}</td></tr>
 *   <tr><td>TELEPORT</td><td>{@link TeleportSmokeFx}: a smoke blink (and the one who blinks hidden while it covers them)</td></tr>
 * </table>
 */
public final class ClientMagicFx {

    private ClientMagicFx() {
    }

    /** One moment of magic; see {@link MagicFxPayload} for the kinds and their arguments. */
    public static void handle(MagicFxPayload p) {
        if (Minecraft.getInstance().level == null) return;
        switch (p.kind()) {
            case Kind.CAST_CIRCLE -> MagicFxManager.add(new CastCircleFx(p));
            case Kind.BOLT_TRAIL -> BoltTrails.start(p);
            case Kind.IMPACT -> MagicFxManager.add(new ImpactFx(p));
            case Kind.CHAIN_ARC -> MagicFxManager.add(new ChainArcFx(p));
            case Kind.WARD_PULSE -> {
                if (!ShieldBubbles.pulse(p)) MagicFxManager.add(new WardPulseFx(p));
            }
            case Kind.RITUAL_COMPLETE -> MagicFxManager.add(new RitualBurstFx(p, true));
            case Kind.RITUAL_FAIL -> MagicFxManager.add(new RitualBurstFx(p, false));
            case Kind.SUMMON_RIFT -> MagicFxManager.add(new SummonRiftFx(p));
            case Kind.BOWL_SEAL -> MagicFxManager.add(new BowlFx.Seal(p));
            case Kind.BOWL_SEND -> MagicFxManager.add(new BowlFx.Send(p));
            case Kind.BOWL_BACKLASH -> MagicFxManager.add(new BowlFx.Backlash(p));
            case Kind.HEX_PULSE -> MagicFxManager.add(new SignFx.Hex(p));
            case Kind.TRAP_FLARE -> MagicFxManager.add(new SignFx.Trap(p));
            case Kind.MARK_FLARE -> MagicFxManager.add(new SignFx.Mark(p));
            case Kind.TELEPORT -> {
                // The one who blinks is hidden until the smoke puffs out where they appear.
                org.papiricoh.supernaturalcraft.client.witch.WitchGeo.vanish(p.entity(), TeleportSmokeFx.hiddenTicks(p.duration()));
                MagicFxManager.add(new TeleportSmokeFx(p));
            }
            default -> {
                // A kind from a newer server: nothing to show.
            }
        }
    }

    /** A rite starts charging at an altar, or stops ({@link RitualChargePayload#isStop()}). */
    public static void handleCharge(RitualChargePayload payload) {
        if (Minecraft.getInstance().level == null) return;
        RitualChargeFx.handle(payload);
    }
}
