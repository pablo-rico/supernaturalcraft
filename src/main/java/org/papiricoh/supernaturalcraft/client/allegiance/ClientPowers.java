package org.papiricoh.supernaturalcraft.client.allegiance;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.network.CastPowerPayload;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The local player's powers: which one the wheel selected, and a local estimate of each one's cooldown (the server's
 * cooldown multiplier is not known here; a {@code DENIED} from the server corrects it). Casting sends a
 * {@link CastPowerPayload} with whatever is under the crosshair; the server checks everything again.
 */
public final class ClientPowers {

    private static Power selected;
    /** Client game time each power is estimated ready again. */
    private static final Map<Power, Long> READY_AT = new EnumMap<>(Power.class);
    /** When each power's cooldown started (for the sweep). */
    private static final Map<Power, Long> COOLING_FROM = new EnumMap<>(Power.class);
    private static Power lastCast;

    private ClientPowers() {
    }

    public static Allegiance mine() {
        Player p = Minecraft.getInstance().player;
        return p == null ? Allegiance.HUMAN : Allegiances.get(p);
    }

    /** The wheel's powers now (actives of the local player's side and rank). */
    public static List<Power> wheel() {
        Allegiance a = mine();
        return Power.wheel(a.faction(), a.rank());
    }

    /** The selected power, if the player still has it (otherwise the wheel's first). */
    public static Power selected() {
        List<Power> wheel = wheel();
        if (wheel.isEmpty()) return null;
        if (selected == null || !wheel.contains(selected)) selected = wheel.getFirst();
        return selected;
    }

    public static void select(Power p) {
        selected = p;
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** The cooldown multiplier when it can be read (single player); 1 otherwise. */
    private static double cooldownMultiplier() {
        try {
            return org.papiricoh.supernaturalcraft.SNConfig.POWER_COOLDOWN_MULTIPLIER.get();
        } catch (RuntimeException notLoaded) {
            return 1;
        }
    }

    /** What a power costs as far as this client can tell (the server's multiplier in single player). */
    public static int cost(Power p) {
        try {
            return (int) Math.round(p.cost * org.papiricoh.supernaturalcraft.SNConfig.POWER_COST_MULTIPLIER.get());
        } catch (RuntimeException notLoaded) {
            return p.cost;
        }
    }

    public static int cooldownLeft(Power p) {
        Long at = READY_AT.get(p);
        return at == null ? 0 : (int) Math.max(0, at - now());
    }

    /** 1 just cast … 0 ready. */
    public static float cooldownShare(Power p, float partial) {
        Long at = READY_AT.get(p), from = COOLING_FROM.get(p);
        if (at == null || from == null || at <= from) return 0;
        float t = now() + partial;
        return Math.max(0, Math.min(1, (at - t) / (float) (at - from)));
    }

    /** What the local reckoning says about casting {@code p} now (the server has the last word). */
    public static PowerRules.Verdict verdict(Power p) {
        Allegiance a = mine();
        PowerRules.Verdict v = PowerRules.canCast(a.withEssence(Float.MAX_VALUE), p, cooldownLeft(p), ClientAllegiance.suppressed());
        return v == PowerRules.Verdict.OK && a.essence() < cost(p) ? PowerRules.Verdict.NO_ESSENCE : v;
    }

    /** Key B: cast the selected power at what is under the crosshair. */
    public static void castSelected() {
        Power p = selected();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (p == null) {
            AllegianceHud.deny(Component.translatable("hud.supernaturalcraft.allegiance.no_powers"));
            return;
        }
        PowerRules.Verdict v = verdict(p);
        if (v == PowerRules.Verdict.COOLING_DOWN || v == PowerRules.Verdict.NO_ESSENCE || v == PowerRules.Verdict.SUPPRESSED) {
            // Known here already: say so without asking.
            AllegianceHud.deny(denial(v.ordinal()));
            return;
        }
        int target = -1;
        if (mc.hitResult instanceof EntityHitResult hit) target = hit.getEntity().getId();
        else if (mc.crosshairPickEntity != null) target = mc.crosshairPickEntity.getId();
        PacketDistributor.sendToServer(new CastPowerPayload(p.ordinal(), target));
        lastCast = p;
        startCooldown(p);
    }

    static void startCooldown(Power p) {
        if (p.cooldown <= 0) return;
        long t = now();
        COOLING_FROM.put(p, t);
        READY_AT.put(p, t + Math.round(p.cooldown * cooldownMultiplier()));
    }

    /** The server cast {@code p} for us: the estimate starts over from now. */
    static void confirmed(Power p) {
        startCooldown(p);
    }

    /** The server refused the last cast: forget its estimate unless it says the power is cooling down. */
    static void denied(int verdict) {
        if (lastCast != null && verdict != PowerRules.Verdict.COOLING_DOWN.ordinal()) {
            READY_AT.remove(lastCast);
            COOLING_FROM.remove(lastCast);
        }
        AllegianceHud.deny(denial(verdict));
    }

    public static Component denial(int verdict) {
        PowerRules.Verdict[] all = PowerRules.Verdict.values();
        String id = verdict >= 0 && verdict < all.length ? all[verdict].name().toLowerCase(java.util.Locale.ROOT) : "no";
        return Component.translatable("hud.supernaturalcraft.allegiance.denied." + id);
    }

    /** Preview hook. */
    public static void forceCooldown(Power p, int ticks, int total) {
        long t = now();
        COOLING_FROM.put(p, t - (total - ticks));
        READY_AT.put(p, t + ticks);
    }

    public static void reset() {
        READY_AT.clear();
        COOLING_FROM.clear();
        selected = null;
        lastCast = null;
    }

    static boolean isMe(Entity e) {
        return e != null && e == Minecraft.getInstance().player;
    }
}
