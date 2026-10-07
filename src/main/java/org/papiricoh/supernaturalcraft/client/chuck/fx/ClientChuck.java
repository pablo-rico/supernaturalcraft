package org.papiricoh.supernaturalcraft.client.chuck.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.network.AuthorFxPayload;

/**
 * Client side of the Author's fight: every {@link AuthorFxPayload} lands here and is handed to what shows it — the
 * overlay (titles, narration, rules, cracks, credits, the snap's count), the HUD (rewritten names and hearts), the
 * world (frames, streaks, rising letters), the page shader (white-out) or the camera (gravity, shake). Also the one
 * client tick that drives them all.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientChuck {

    private ClientChuck() {
    }

    public static void handle(AuthorFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        switch (p.kind()) {
            case AuthorFxPayload.CHAPTER_TITLE -> ChuckOverlay.add(new ChuckOverlay.TitleCard(
                    Chapter.values()[Mth.clamp(p.arg(), 0, Chapter.values().length - 1)]));
            case AuthorFxPayload.ARENA_WAVE -> ChuckWorldFx.wave(p.point(), p.radius(), p.duration(), p.arg());
            case AuthorFxPayload.NARRATE -> ChuckOverlay.narrate(p.text(), p.duration());
            case AuthorFxPayload.SNAP_COUNT -> ChuckWorldFx.snap(p.point(), p.radius(), Math.max(1, p.duration()));
            case AuthorFxPayload.BACKSPACE -> {
                ChuckWorldFx.backspace(p.entity(), p.point());
                if (p.entity() == mc.player.getId()) {
                    ChuckOverlay.add(new ChuckOverlay.Smear());
                    ChuckCamera.shake(0.4f, 10);
                }
            }
            case AuthorFxPayload.FAKE_CREDITS -> ChuckOverlay.add(ChuckCredits.fake(p.duration()));
            case AuthorFxPayload.CREDITS -> ChuckOverlay.add(ChuckCredits.real(p.duration()));
            case AuthorFxPayload.HUD_REWRITE -> ChuckHud.rewrite(p.duration());
            case AuthorFxPayload.WHITE_OUT -> AuthorPageFx.whiteOut(p.arg() / 100f, p.duration());
            case AuthorFxPayload.CRACK -> {
                ChuckOverlay.add(new ChuckOverlay.Cracks(p.duration()));
                ChuckCamera.shake(0.9f, 18);
                ChuckWorldFx.crackBurst(p.point());
            }
            case AuthorFxPayload.RULE -> ChuckOverlay.rules(p.arg(), p.duration());
            case AuthorFxPayload.GRAVITY -> ChuckCamera.gravity(p.arg(), p.duration());
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ChuckOverlay.tick();
        ChuckHud.tick();
        ChuckWorldFx.tick();
        ChuckCamera.tick();
        AuthorPageFx.tick();
    }

    /** Leaving the world forgets everything mid-air (and stands the camera upright). */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ChuckOverlay.clear();
        ChuckWorldFx.clear();
        ChuckCamera.reset();
    }
}
