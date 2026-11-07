package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

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
import org.papiricoh.supernaturalcraft.entity.boss.uriel.UrielAssets;

/**
 * Uriel's boss bar (v0.40), for his bar keys ({@code entity.supernaturalcraft.uriel.bar.phase<n>}): a band of scorched gold leaf on
 * dark bronze with grey feathers fanned at its ends, his name over a trough of molten gold light (his grace) that burns out to ash
 * from its end, sparks lifting off the burning edge; two grey wings hang under it, one per phase (the current one burning, the
 * lost one a black stump). Not drawn during a camera sequence. Sheet layout: {@code tools/artgen/uriel_art.py}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class UrielHud {

    static final ApocalypseBar BAR = new ApocalypseBar(SupernaturalCraft.asResource(UrielAssets.BAR_SHEET), 2, 24, -6,
            0xF4E2B0, 0xFFE07A, 0xFF8A30, -1f);

    private UrielHud() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        BAR.tick(Minecraft.getInstance().level != null);
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(UrielAssets.BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(ApocalypseBar.H + 26);
        if (Minecraft.getInstance().level == null || CameraDirector.active()) return;
        BAR.draw(event.getGuiGraphics(), boss.getId(), boss.getName(), ApocalypseBar.phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1),
                event.getY(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
