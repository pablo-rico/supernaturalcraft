package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.item.SpellScrollItem;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * The witchcraft's marks on a bearer (v0.21), drawn for every player whose effects this client knows (itself, in third person):
 * <ul>
 *   <li>MIRRORED: a small mirror-glyph orbiting at chest height, trailing two faint echoes;</li>
 *   <li>NAME_WARDED: now and then a faint magenta-silver shimmer over a shell round them (the full flicker when a curse is turned
 *   away is the server's HEX_PULSE protect);</li>
 *   <li>SILENCED: trying to cast a sigil from a grimoire or scroll stitches the view ({@link MagicOverlay#stitched()}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CurseAuras {

    private static final int MIRROR = 0xC9C2FF, WARD = 0xD86AC0, SILVER = 0xE8F0FF;
    private static long lastStitch;

    private CurseAuras() {
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getLevel().isClientSide() || event.getEntity() != mc.player) return;
        var item = event.getItemStack().getItem();
        if (!(item instanceof GrimoireItem || item instanceof SpellScrollItem)) return;
        if (!mc.player.hasEffect(AllMobEffects.SILENCED) || mc.player.getAbilities().instabuild) return;
        long now = mc.player.level().getGameTime();
        if (now - lastStitch < 8) return;
        lastStitch = now;
        MagicOverlay.stitched();
    }

    /** Whether anyone this client knows bears a mark to draw. */
    static boolean any(Minecraft mc) {
        if (mc.level == null) return false;
        for (Player p : mc.level.players()) {
            if (p.hasEffect(AllMobEffects.MIRRORED) || p.hasEffect(AllMobEffects.NAME_WARDED)) return true;
        }
        return false;
    }

    static void renderAll(FxDraw d, Minecraft mc) {
        if (mc.level == null) return;
        for (Player p : mc.level.players()) {
            boolean mirrored = p.hasEffect(AllMobEffects.MIRRORED), warded = p.hasEffect(AllMobEffects.NAME_WARDED);
            if (!mirrored && !warded) continue;
            if (p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (p.isInvisible() || !MagicFxManager.visible(mc, p.position(), 4)) continue;
            Vec3 f = p.getPosition(d.partial);
            d.floor(f.y);
            float t = d.now + p.getId() * 13;
            double chest = f.y + p.getBbHeight() * 0.62;
            if (mirrored) {
                for (int e = 2; e >= 0; e--) {
                    float ang = t * 0.09f - e * 0.22f;
                    double x = f.x + Mth.cos(ang) * 0.72, y = chest + Mth.sin(t * 0.05f - e * 0.1f) * 0.18, z = f.z + Mth.sin(ang) * 0.72;
                    float a = e == 0 ? 0.9f : e == 1 ? 0.35f : 0.15f;
                    if (e == 0) d.billboard(MagicTextures.castCircle(), x, y, z, 0.36f, t * 0.06f, MIRROR, 0.85f);
                    d.glowDisc(x, y, z, e == 0 ? 0.3f : 0.18f, e == 0 ? SILVER : MIRROR, 0.45f * a);
                }
            }
            if (warded) {
                float cyc = (t % 120f) / 120f;
                if (cyc < 0.12f) {
                    float k = cyc / 0.12f, a = Mth.sin(Mth.PI * k) * (0.6f + 0.4f * Mth.sin(t * 2.3f));
                    float rr = Math.max(0.85f, p.getBbHeight() * 0.62f);
                    d.sphere(f.x, f.y + p.getBbHeight() * 0.5, f.z, rr, FxColor.mix(WARD, SILVER, 0.5f), 0.07f * a);
                    d.ring(f.x, f.y + p.getBbHeight() * 0.5, f.z, Mth.sin(t * 0.05f), 1, Mth.cos(t * 0.05f), rr * 0.97f, rr * 1.03f, WARD,
                            0.45f * a, true);
                }
            }
        }
        d.floor(Double.NaN);
    }
}
