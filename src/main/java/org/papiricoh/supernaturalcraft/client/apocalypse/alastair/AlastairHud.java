package org.papiricoh.supernaturalcraft.client.apocalypse.alastair;

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
import org.papiricoh.supernaturalcraft.entity.boss.alastair.AlastairAssets;

/**
 * Alastair's boss bar (v0.40), for his bar keys ({@code entity.supernaturalcraft.alastair.bar.phase<n>}): a plate of rusted black
 * iron, his name over a trough where a chain soaked in blood runs out as he loses health, blood dripping from its cut end; three
 * rusty hooks hang under the plate, one per phase (the current one bloody and swinging, those he has lost snapped off their
 * chains). Not drawn during a camera sequence. Sheet layout: {@code tools/artgen/alastair_gui_art.py}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AlastairHud {

    static final ApocalypseBar BAR = new ApocalypseBar(SupernaturalCraft.asResource(AlastairAssets.BAR_SHEET), 3, 32, 0,
            0xD8D0C2, 0x8E1212, 0x5E0A0C, 1f);

    private AlastairHud() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        BAR.tick(Minecraft.getInstance().level != null);
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(AlastairAssets.BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(ApocalypseBar.H + 34);
        if (Minecraft.getInstance().level == null || CameraDirector.active()) return;
        BAR.draw(event.getGuiGraphics(), boss.getId(), boss.getName(), ApocalypseBar.phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1),
                event.getY(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
