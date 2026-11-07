package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealKind;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRules;
import org.papiricoh.supernaturalcraft.network.SealSitePayload;
import org.papiricoh.supernaturalcraft.network.SealStatePayload;

/**
 * What this client knows of the seals (v0.40, the Seals slice): the contract every client reader uses (sky, book, HUD).
 * Add helpers here rather than reading the payloads elsewhere.
 */
public final class ClientSeals {

    private static SealStatePayload state = SealStatePayload.NONE;
    private static SealSitePayload site = SealSitePayload.NONE;
    /** Game time the site was last heard of (it is dropped if the server falls silent). */
    private static long siteHeard;

    private ClientSeals() {
    }

    public static void handle(SealStatePayload payload) {
        state = payload;
    }

    public static void handle(SealSitePayload payload) {
        site = payload;
        var level = Minecraft.getInstance().level;
        siteHeard = level != null ? level.getGameTime() : 0;
    }

    public static SealStatePayload state() {
        return state;
    }

    /** The event this hunter is called to, or {@link SealSitePayload#NONE}. */
    public static SealSitePayload site() {
        var level = Minecraft.getInstance().level;
        if (site.active() && level != null && level.getGameTime() - siteHeard > 200) site = SealSitePayload.NONE;
        return site;
    }

    /** Whether this hunter is called to a seal now. */
    public static boolean called() {
        return site().active();
    }

    public static SealKind kind() {
        return SealKind.byOrdinal(site().kind());
    }

    public static BlockPos sitePos() {
        return site().pos();
    }

    /** Distance from {@code from} to the site (horizontal). */
    public static double distance(Vec3 from) {
        BlockPos p = site().pos();
        double dx = p.getX() + 0.5 - from.x, dz = p.getZ() + 0.5 - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean started() {
        return state.started();
    }

    public static int broken() {
        return state.broken();
    }

    public static int stage() {
        return state.stage();
    }

    public static boolean risen() {
        return state.risen();
    }

    public static boolean echo() {
        return state.echo();
    }

    public static int daysLeft() {
        return state.daysLeft();
    }

    /** How strongly the sky shows it (0-1). */
    public static float omens() {
        return SealRules.omenStrength(state.broken(), state.risen(), state.echo());
    }

    /** Whether {@code level} is where the omens show (the Overworld). */
    public static boolean shows(Level level) {
        return level != null && level.dimension() == Level.OVERWORLD && started();
    }

    /** Forgets everything (logout). */
    public static void reset() {
        state = SealStatePayload.NONE;
        site = SealSitePayload.NONE;
    }
}
