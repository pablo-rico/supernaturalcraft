package org.papiricoh.supernaturalcraft.hell;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.network.TormentPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Hell gets into you. Torment grows while you stay (faster on the Rack), fades once you leave, and
 * salt held in the off hand or a dash of holy water eases it. It is only ever seen and heard — red at
 * the edge of sight, whispers, shapes in the fog — never felt.
 */
public final class Torment {

    /** Per-second fade outside Hell, and while salt is held in Hell. */
    public static final float FADE_OUTSIDE = 0.02f, SALT_EASE = 0.006f;
    public static final float RACK_FACTOR = 1.6f;

    private Torment() {
    }

    /**
     * One second of Torment.
     *
     * @param perMinute growth per minute in Hell (config)
     */
    public static float next(float current, float perMinute, boolean inHell, boolean onTheRack, boolean salted) {
        float v = current;
        if (inHell) {
            v += perMinute / 60f * (onTheRack ? RACK_FACTOR : 1f);
            if (salted) v -= SALT_EASE;
        } else {
            v -= FADE_OUTSIDE;
        }
        return Math.max(0f, Math.min(1f, v));
    }

    public static float get(ServerPlayer p) {
        return p.getData(AllAttachments.TORMENT);
    }

    public static void set(ServerPlayer p, float value) {
        float before = get(p);
        float v = Math.max(0f, Math.min(1f, value));
        p.setData(AllAttachments.TORMENT, v);
        if (Math.abs(before - v) > 0.0001f || v == 0f && before != 0f) PacketDistributor.sendToPlayer(p, new TormentPayload(v));
    }

    /** Holy water eases it. */
    public static void soothe(ServerPlayer p, float amount) {
        if (get(p) > 0) set(p, get(p) - amount);
    }

    /** Once a second per player. */
    public static void tick(ServerPlayer p) {
        if (p.tickCount % 20 != 0 || p.isSpectator()) return;
        boolean inHell = HellDimension.isHell(p.level());
        float now = get(p);
        if (!inHell && now == 0f) return;
        boolean rack = inHell && p.level().getBiome(p.blockPosition()).is(HellDimension.THE_RACK);
        boolean salted = p.getOffhandItem().is(AllTags.Items.SALT);
        set(p, next(now, SNConfig.TORMENT_PER_MINUTE.get().floatValue(), inHell, rack, salted));
    }
}
