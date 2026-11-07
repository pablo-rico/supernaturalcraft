package org.papiricoh.supernaturalcraft.client.s1214;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.apocalypse.ApocalypseBar;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;

import java.util.Map;

/**
 * The boss bars of the eight v0.41 bosses (seasons 12-14), one shared renderer on {@link ApocalypseBar}'s layout (as Alastair's
 * and Uriel's), each from its own sheet ({@code tools/artgen/s1214_gui_art.py}):
 * <ul>
 *   <li>Ketch: gunmetal, a belt of brass rounds runs out, muzzle smoke at its end; rounds hang below (whole, smoking, spent).</li>
 *   <li>Hess: tweed bound in leather, gilt; ink with gold flecks dripping; the Order's aquarian star per phase.</li>
 *   <li>Dagon: black lacquer split by fire; hellfire with a flame at its end; a burning eye per phase.</li>
 *   <li>Asmodeus: ivory and gold; liquid grace, gold sparks; a vial of grace per phase (full, glowing, shattered).</li>
 *   <li>AU Michael: blackened steel and silver insignia; dark gold light; a silver star per phase.</li>
 *   <li>Michael (vessel): charcoal, an ice-blue rule; blue-white grace; a feather of light per phase.</li>
 *   <li>Soulless Jack: black cracked with gold; gold light; a gold eye per phase.</li>
 *   <li>The Shadow: wet black tar with a violet sheen; tar dripping; a tendril per phase (whole, writhing, severed).</li>
 * </ul>
 *
 * <p><b>Key convention</b> (see {@link S1214Bars}): the boss names its {@code ServerBossEvent}
 * {@code Component.translatable("entity.supernaturalcraft.<rig>.bar.phase<n>")}, rig in {@link S1214Bars#RIGS}. The bar draws
 * the key's own text as the name, three emblems (the current phase's swings, the lost ones hang broken), and a burst of motes
 * when the boss is hit. It draws no title cards, so it simply stays hidden during a camera sequence
 * ({@link CameraDirector#active()}) and needs no {@code showDuringSequences}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class S1214Hud {

    private S1214Hud() {
    }

    /** Per rig: emblem height, edge offset (drip 0, flame/sparks -6), name colour, mote colours, gravity (+ falls, - rises). */
    static final Map<String, ApocalypseBar> BARS = Map.of(
            "ketch", bar("ketch", -6, 0xD8D4CC, 0xC8C8C8, 0xA98236, -0.6f),
            "hess", bar("hess", 0, 0xF2D257, 0x1C2A50, 0xD9A92B, 1f),
            "dagon", bar("dagon", -6, 0xFFB070, 0xFF9A26, 0xC02D06, -1f),
            "asmodeus", bar("asmodeus", -6, 0x4B3108, 0xFFF2A8, 0xD9A92B, -1f),
            "au_michael", bar("au_michael", -6, 0xDDE1E6, 0xF0D070, 0xA8781E, -1f),
            "michael_vessel", bar("michael_vessel", -6, 0xE2EEFA, 0xB4CCE6, 0x86A8CC, -0.8f),
            "soulless_jack", bar("soulless_jack", -6, 0xF2D260, 0xFFF0A8, 0xD4A830, -1f),
            "shadow", bar("shadow", 0, 0xB4A0D8, 0x1B1624, 0x56407A, 1f));

    private static ApocalypseBar bar(String rig, int edgeDy, int nameRgb, int moteA, int moteB, float gravity) {
        return new ApocalypseBar(SupernaturalCraft.asResource(S1214Bars.sheet(rig)), S1214Bars.phases(rig), 24, edgeDy, nameRgb, moteA, moteB, gravity);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        boolean inWorld = Minecraft.getInstance().level != null;
        for (ApocalypseBar b : BARS.values()) b.tick(inWorld);
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)) return;
        String rig = S1214Bars.rigOf(tc.getKey());
        if (rig == null) return;
        ApocalypseBar bar = BARS.get(rig);
        if (bar == null) return;
        event.setCanceled(true);
        event.setIncrement(ApocalypseBar.H + 34);
        if (Minecraft.getInstance().level == null || CameraDirector.active()) return;
        bar.draw(event.getGuiGraphics(), boss.getId(), boss.getName(), S1214Bars.phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1),
                event.getY(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
