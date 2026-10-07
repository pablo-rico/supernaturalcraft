package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.author.AuthorNpcEntity;
import org.papiricoh.supernaturalcraft.author.AuthorRewards;
import org.papiricoh.supernaturalcraft.author.AuthorSavedData;
import org.papiricoh.supernaturalcraft.author.AuthorSite;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.author.CabinBuilder;
import org.papiricoh.supernaturalcraft.client.chuck.screen.AuthorDialogueScreen;
import org.papiricoh.supernaturalcraft.client.chuck.screen.ManuscriptScreen;
import org.papiricoh.supernaturalcraft.client.chuck.screen.TypewriterPageScreen;
import org.papiricoh.supernaturalcraft.network.AuthorDialoguePayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * {@code SN_PREVIEW=chuck_cabin}: the cabin built at 2000, 2000 (porch, inside, the desk), the typewriter's page, the
 * Author typing at his desk, the dialogue at GUI scales 2 and 3, the map, "The End", the Pen and the amulet in hand.
 * Shots: {@code sn_chuck_cabin_*.png}.
 */
final class ChuckCabinPreview {

    private static int t = -1;
    private static AuthorSite.Site site;
    private static Vec3 camFrom, camAt;

    private ChuckCabinPreview() {
    }

    /** @return true while this preview is running (it owns the tick) */
    static boolean tick(Minecraft mc) {
        if (!"chuck_cabin".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        int now = ++t;
        server.execute(() -> scene(server.getPlayerList().getPlayers().getFirst(), now));
        // Client: screens and GUI scale.
        if (now == 20) mc.options.hideGui = true;
        if (now == 150) mc.setScreen(new TypewriterPageScreen(List.of("azazel", "lilith", "lucifer", "broken_chorus"), false));
        if (now == 175) mc.setScreen(null);
        if (now == 240 || now == 280) {
            scale(mc, now == 240 ? 2 : 3);
            AuthorNpcEntity npc = mc.level.getEntitiesOfClass(AuthorNpcEntity.class, mc.player.getBoundingBox().inflate(30)).stream().findFirst().orElse(null);
            AuthorDialogueScreen s = new AuthorDialogueScreen(new AuthorDialoguePayload(npc == null ? 0 : npc.getId(), "hello",
                    List.of("who", "monsters", "lucifer", "amara", "winchesters", "story", "ready", "leave"), false));
            mc.setScreen(s);
        }
        if ((now == 255 || now == 295) && mc.screen instanceof AuthorDialogueScreen s) s.finishTyping();
        if (now == 270 || now == 310) mc.setScreen(null);
        if (now == 315) scale(mc, 2);
        if (now == 330) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (now == 400) {
            ItemStack book = mc.player.getMainHandItem();
            mc.setScreen(new ManuscriptScreen(book.get(AllDataComponents.MANUSCRIPT.get())));
        }
        if (now == 412 && mc.screen instanceof ManuscriptScreen m) m.setPage(1);
        if (now == 424) mc.setScreen(null);
        String shot = switch (now) {
            case 70 -> "porch";
            case 100 -> "inside";
            case 125 -> "desk";
            case 165 -> "page";
            case 225 -> "npc";
            case 262 -> "dialogue_s2";
            case 302 -> "dialogue_s3";
            case 390 -> "map";
            case 408 -> "manuscript_title";
            case 420 -> "manuscript";
            case 480 -> "pen";
            case 540 -> "amulet";
            default -> null;
        };
        if (shot != null) Screenshot.grab(mc.gameDirectory, "sn_chuck_cabin_" + shot + ".png", mc.getMainRenderTarget(), m -> {
        });
        if (now >= 550) mc.stop();
        return true;
    }

    private static void scale(Minecraft mc, int s) {
        mc.options.guiScale().set(s);
        mc.resizeDisplay();
    }

    private static void scene(ServerPlayer p, int now) {
        ServerLevel level = p.server.overworld();
        if (now == 1) {
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(level, 2000.5, 120, 2000.5, 0, 0);
            level.setDayTime(5000);
            level.setWeatherParameters(12000, 0, false, false);
        }
        if (now == 10) {
            BlockPos origin = CabinBuilder.originOn(level, 2000, 2000);
            CabinBuilder.placeAt(level, origin, 0);
            site = new AuthorSite.Site(origin, 0);
            AuthorSavedData.get(level).setCabin(origin, 0, true);
            AuthorSavedData.get(level).setSpellCast(false);
        }
        if (site == null || now < 12) return;
        if (camFrom != null && now % 5 == 0 && p.position().distanceToSqr(camFrom) > 0.25) view(p, camFrom, camAt);
        Vec3 c = Vec3.atBottomCenterOf(AuthorWorld.centre(site));
        if (now == 40) view(p, c.add(7, 3.5, 15), c.add(0, 1.5, 4));
        if (now == 85) view(p, c.add(-2.5, 1.6, 3.5), c.add(1, 1, -3));
        if (now == 110) view(p, c.add(1, 1.9, -1.2), c.add(1, 1.2, -4));
        if (now == 190) {
            AuthorSavedData.get(level).setSpellCast(true);
            AuthorWorld.tick(level, List.of(p));
            view(p, c.add(-1.5, 1.7, 0.5), c.add(1, 1.1, -3));
        }
        if (now == 320) {
            camFrom = null;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(level, c.x, c.y, c.z + 2, 180, 10);
            p.setItemInHand(InteractionHand.MAIN_HAND, AuthorWorld.map(level, site));
        }
        if (now == 395) p.setItemInHand(InteractionHand.MAIN_HAND, AuthorRewards.manuscript(p));
        if (now == 430) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.AUTHORS_PEN.get()));
        if (now == 490) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.SAMS_AMULET.get()));
        if (now == 545) {
            AuthorSavedData.get(level).restore(new net.minecraft.nbt.CompoundTag());
            for (AuthorNpcEntity n : level.getEntitiesOfClass(AuthorNpcEntity.class, p.getBoundingBox().inflate(40))) n.discard();
        }
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        camFrom = from;
        camAt = at;
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }
}
