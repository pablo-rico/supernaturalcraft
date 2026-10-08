package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.client.allegiance.AllegianceDialogueScreen;
import org.papiricoh.supernaturalcraft.client.allegiance.AllegianceFx;
import org.papiricoh.supernaturalcraft.client.allegiance.AllegianceHud;
import org.papiricoh.supernaturalcraft.client.allegiance.ClientPowers;
import org.papiricoh.supernaturalcraft.client.allegiance.PowerWheelScreen;
import org.papiricoh.supernaturalcraft.crossroads.client.DealScreen;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.network.DealOfferPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code SN_PREVIEW=allegiance}: v0.13 in the real renderer, well away from the origin. An angel at ranks I–IV and a demon at
 * ranks I–IV in third person from the front and the back (wings, eyes, the King's regalia), the true form, the power wheel
 * of each side, the HUD (emblem, ring, selected power, a refusal, Angel Radio marks), Heaven's messenger and his dialogue,
 * the three rival hunters, the deal's "Bind my soul", and an ascension's camera with its title card. Screenshots:
 * {@code runs/client/screenshots/sn_allegiance_*.png}.
 */
final class AllegiancePreview {

    private static final int STEP = 40;
    private static int t = -1;
    private static BlockPos ground;
    private static final List<Entity> shown = new ArrayList<>();

    /** One moment: what the server sets up, what the client does, and the picture's name (taken at the end). */
    private record Beat(String name, java.util.function.BiConsumer<ServerPlayer, Vec3> server, Runnable client) {
    }

    private static List<Beat> beats;

    private AllegiancePreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"allegiance".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        t++;
        if (t == 0) {
            beats = plan(mc);
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst()));
            return true;
        }
        if (t < 20 || ground == null) return true;
        int i = (t - 20) / STEP, local = (t - 20) % STEP;
        if (i >= beats.size()) {
            if (local == 5) {
                mc.setScreen(null);
                server.execute(() -> {
                    clearShown();
                    ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                    Allegiances.set(p, Allegiance.HUMAN);
                });
            }
            if (local == 15) mc.stop();
            return true;
        }
        Beat b = beats.get(i);
        Vec3 c = Vec3.atBottomCenterOf(ground);
        if (local == 0) {
            server.execute(() -> b.server.accept(server.getPlayerList().getPlayers().getFirst(), c));
        }
        if (local == 4 && b.client != null) b.client.run();
        if (local == STEP - 2) {
            Screenshot.grab(mc.gameDirectory, "sn_allegiance_" + b.name + ".png", mc.getMainRenderTarget(), m -> {
            });
        }
        return true;
    }

    private static void setUp(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(1020 >> 4, 412 >> 4);
        ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(1020, 0, 412));
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = true;
        p.onUpdateAbilities();
        Vec3 c = Vec3.atBottomCenterOf(ground);
        p.teleportTo(level, c.x, c.y, c.z, 0, 0);
    }

    private static void clearShown() {
        for (Entity e : shown) e.discard();
        shown.clear();
    }

    /** Puts the player on the spot facing south, with a side and rank, display flags and full essence share. */
    private static void become(ServerPlayer p, Vec3 c, Faction f, int rank, int flags, float share) {
        Allegiance a = Allegiance.HUMAN.convert(f).withRank(rank);
        a = a.withEssence(Ranks.maxEssence(f, rank) * share);
        Allegiances.set(p, a);
        for (int flag : new int[]{Allegiances.EYES, Allegiances.TRUE_FORM, Allegiances.SUPPRESSED, Allegiances.SMOKE, Allegiances.POSSESSING}) {
            Allegiances.setFlag(p, flag, (flags & flag) != 0);
        }
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.teleportTo(p.serverLevel(), c.x, c.y, c.z, 0, 0);
        p.setYHeadRot(0);
        p.setYBodyRot(0);
    }

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    private static <T extends Mob> T place(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        shown.add(e);
        return e;
    }

    private static List<Beat> plan(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        // --- the sides on the player, front and back -------------------------------------------------------------------
        for (Faction f : new Faction[]{Faction.ANGEL, Faction.DEMON}) {
            for (int rank = 1; rank <= 4; rank++) {
                int r = rank;
                // Eyes show while powers are used: on for every demon shot, and for the Lesser Angel (whose wings show only then).
                int flags = f == Faction.DEMON || r == 1 ? Allegiances.EYES : 0;
                String id = f.getSerializedName() + "_" + r;
                out.add(new Beat(id + "_front", (p, c) -> {
                    clearShown();
                    become(p, c, f, r, flags, 0.7f);
                }, () -> camera(mc, CameraType.THIRD_PERSON_FRONT, false)));
                out.add(new Beat(id + "_back", (p, c) -> {
                }, () -> camera(mc, CameraType.THIRD_PERSON_BACK, false)));
            }
        }
        // The Seraph flying (wings spread), from behind.
        out.add(new Beat("angel_2_flying", (p, c) -> {
            become(p, c.add(0, 3, 0), Faction.ANGEL, 2, 0, 0.7f);
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }, () -> camera(mc, CameraType.THIRD_PERSON_BACK, false)));
        // The wings' clips from behind, for the art: rest, folded, opening (shadow and light).
        for (String clip : new String[]{"rest", "fold", "open"}) {
            for (int r : new int[]{2, 3}) {
                out.add(new Beat("wings_" + clip + "_" + (r == 2 ? "shadow" : "light"), (p, c) -> become(p, c, Faction.ANGEL, r, 0, 0.7f), () -> {
                    camera(mc, CameraType.THIRD_PERSON_BACK, false);
                    org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceLayer.forcedClip = clip;
                }));
            }
        }
        out.add(new Beat("wings_released", (p, c) -> {
        }, () -> org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceLayer.forcedClip = null));
        // An archangel's true form.
        out.add(new Beat("true_form", (p, c) -> become(p, c, Faction.ANGEL, 3, Allegiances.TRUE_FORM, 0.4f), () -> {
            camera(mc, CameraType.THIRD_PERSON_FRONT, false);
            if (mc.player != null) AllegianceFx.handle(new AllegianceFxPayload(mc.player.getId(), AllegianceFxPayload.TRUE_FORM, 0, 0,
                    mc.player.position(), 200));
        }));

        // --- the wheel and the HUD -------------------------------------------------------------------------------------
        out.add(new Beat("wheel_angel", (p, c) -> become(p, c, Faction.ANGEL, 4, 0, 0.55f), () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            mc.execute(() -> {
                ClientPowers.reset();
                ClientPowers.forceCooldown(Power.SMITE, 200, 300);
                PowerWheelScreen s = new PowerWheelScreen(null);
                mc.setScreen(s);
                s.hover(2);
            });
        }));
        out.add(new Beat("wheel_demon", (p, c) -> become(p, c, Faction.DEMON, 4, 0, 0.3f), () -> mc.execute(() -> {
            ClientPowers.reset();
            ClientPowers.forceCooldown(Power.POSSESS, 400, 900);
            PowerWheelScreen s = new PowerWheelScreen(null);
            mc.setScreen(s);
            s.hover(1);
        })));
        out.add(new Beat("hud_angel", (p, c) -> become(p, c, Faction.ANGEL, 2, 0, 0.62f), () -> mc.execute(() -> {
            mc.setScreen(null);
            ClientPowers.reset();
            ClientPowers.select(Power.SMITE);
            ClientPowers.forceCooldown(Power.SMITE, 150, 300);
            AllegianceHud.deny(ClientPowers.denial(org.papiricoh.supernaturalcraft.allegiance.power.PowerRules.Verdict.COOLING_DOWN.ordinal()));
            if (mc.player != null) {
                // The Angel Radio: two demons and a boss whispering from around.
                Vec3 at = mc.player.position();
                AllegianceHud.ping(at.add(20, 0, 30), 0, 400);
                AllegianceHud.ping(at.add(-40, 0, -10), 0, 400);
                AllegianceHud.ping(at.add(-6, 0, 80), 1, 400);
            }
        })));
        out.add(new Beat("hud_demon_suppressed", (p, c) -> {
            become(p, c, Faction.DEMON, 3, Allegiances.SUPPRESSED, 0.2f);
        }, () -> mc.execute(() -> {
            ClientPowers.reset();
            if (mc.player != null) AllegianceFx.handle(new AllegianceFxPayload(mc.player.getId(), AllegianceFxPayload.SUPPRESSED, 1, 0, Vec3.ZERO, 0));
        })));

        // --- the messenger and his dialogue ---------------------------------------------------------------------------
        // He comes as he would at dawn (his own entrance, then the real talk through the shared dialogue).
        out.add(new Beat("messenger", (p, c) -> {
            become(p, c, Faction.HUMAN, 0, 0, 0);
            Allegiances.set(p, Allegiance.HUMAN);
            var m = org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity.visit(p);
            if (m != null) shown.add(m);
        }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
        out.add(new Beat("messenger_greeting", (p, c) -> {
        }, () -> mc.execute(() -> {
            mc.options.hideGui = false;
            if (mc.screen instanceof AllegianceDialogueScreen s) s.finishTyping();
        })));
        out.add(new Beat("messenger_listen", (p, c) -> {
        }, () -> mc.execute(() -> {
            // "Listen": he makes his offer.
            if (mc.screen instanceof AllegianceDialogueScreen s) s.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_1, 0, 0);
        })));
        out.add(new Beat("messenger_offer", (p, c) -> {
        }, () -> mc.execute(() -> {
            if (mc.screen instanceof AllegianceDialogueScreen s) s.finishTyping();
        })));

        // --- the rival hunters ----------------------------------------------------------------------------------------
        out.add(new Beat("rival_hunters", (p, c) -> {
            clearShown();
            for (int v = 0; v < 3; v++) {
                RivalHunterEntity h = place(p.serverLevel(), AllEntities.RIVAL_HUNTER.get(), c.add(-1.6 + v * 1.6, 0, 3.5), 180);
                h.setVariant(v);
            }
        }, () -> mc.execute(() -> {
            mc.setScreen(null);
            mc.options.hideGui = true;
        })));
        out.add(new Beat("rival_hunters_side", (p, c) -> {
            p.teleportTo(p.serverLevel(), c.x + 3.5, c.y, c.z + 3.5, 90, 5);
        }, null));
        // At a distance, as they are met: 8 and 16 blocks.
        for (int d : new int[]{8, 16}) {
            out.add(new Beat("rival_hunters_" + d, (p, c) -> p.teleportTo(p.serverLevel(), c.x, c.y, c.z + 3.5 - d, 0, 3), null));
        }

        // --- the crossroads' fine print -------------------------------------------------------------------------------
        out.add(new Beat("deal_bind_soul", (p, c) -> {
            clearShown();
            p.teleportTo(p.serverLevel(), c.x, c.y, c.z, 0, 0);
            place(p.serverLevel(), AllEntities.CROSSROADS_DEMON.get(), c.add(0, 0, 3), 180);
        }, () -> mc.execute(() -> {
            mc.options.hideGui = false;
            int demon = shown.isEmpty() ? -1 : shown.getFirst().getId();
            DealScreen s = new DealScreen(new DealOfferPayload(demon, List.of("upgrade", "upgrade", "knowledge", "convert"),
                    List.of(0, 1, 0, 0), List.of(5, 5, 10, 0), true), 1);
            mc.setScreen(s);
            s.tickSoul(true);
        })));

        // --- an ascension: the camera, then the card ------------------------------------------------------------------
        out.add(new Beat("ascension_start", (p, c) -> {
            clearShown();
            become(p, c, Faction.ANGEL, 2, 0, 0.5f);
        }, () -> mc.execute(() -> {
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (mc.player != null) AllegianceFx.handle(new AllegianceFxPayload(mc.player.getId(), AllegianceFxPayload.ASCENSION,
                    Faction.ANGEL.ordinal(), 2, mc.player.position(), 0));
        })));
        out.add(new Beat("ascension_mid", (p, c) -> {
        }, null));
        out.add(new Beat("ascension_card", (p, c) -> {
        }, null));
        out.add(new Beat("ascension_demon", (p, c) -> become(p, c, Faction.DEMON, 4, Allegiances.EYES, 1), () -> mc.execute(() -> {
            if (mc.player != null) AllegianceFx.handle(new AllegianceFxPayload(mc.player.getId(), AllegianceFxPayload.ASCENSION,
                    Faction.DEMON.ordinal(), 4, mc.player.position(), 0));
        })));
        out.add(new Beat("ascension_demon_card", (p, c) -> {
        }, null));
        out.add(new Beat("ascension_demon_card2", (p, c) -> {
        }, null));
        return out;
    }
}
