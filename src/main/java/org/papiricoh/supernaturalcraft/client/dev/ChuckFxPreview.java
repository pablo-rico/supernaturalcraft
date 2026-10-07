package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorNpcEntity;
import org.papiricoh.supernaturalcraft.client.chuck.fx.ClientChuck;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorHandEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorRules;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.FloatingWordEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.HunterAllyEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.InkEchoEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.TypewriterKeyEntity;
import org.papiricoh.supernaturalcraft.network.AuthorFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * The Author's models and client effects in the real renderer, far from the origin (2000, 2000).
 *
 * <p>{@code SN_PREVIEW=chuck_model}: the man in his three outfits (robe, flannel, suit) front, side and back; the light
 * in chapters 3-5 from far and near, its weak-point hitboxes shown against the ring nodes; the hands; keys, words,
 * pages and a node target; an ink echo of every boss; Dean, Sam and Castiel stepping out of the light.
 *
 * <p>{@code SN_PREVIEW=chuck_fx}: every {@link AuthorFxPayload} kind fired locally (GUI on) in front of the light,
 * with the page shader, the glitching boss bar and the HUD rewrite. Screenshots: {@code sn_chuck_model_*} and
 * {@code sn_chuck_fx_*}.
 */
final class ChuckFxPreview {

    private static final BlockPos BASE = new BlockPos(2000, 0, 2000);
    private static int t = -1;
    private static Vec3 ground;
    /** The owner every prop needs (props vanish without one), kept out of the shots unless it is the subject. */
    private static ChuckEntity owner;
    private static final List<Entity> staged = new ArrayList<>();
    private static ServerBossEvent bar;

    private ChuckFxPreview() {
    }

    /** @return true while this preview is running (it owns the tick) */
    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        boolean model = "chuck_model".equals(scene);
        if (!model && !"chuck_fx".equals(scene)) return false;
        var server = mc.getSingleplayerServer();
        int now = ++t;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            if (model) modelScene(mc, p, now);
            else fxScene(mc, p, now);
        });
        if (model) modelClient(mc, now);
        else fxClient(mc, now);
        return true;
    }

    // --- shared ------------------------------------------------------------------------------------------------------

    private static void setUp(ServerPlayer p, GameType mode) {
        ServerLevel level = p.serverLevel();
        p.setGameMode(mode);
        p.getAbilities().invulnerable = true;
        p.onUpdateAbilities();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        p.teleportTo(level, BASE.getX() + 0.5, 140, BASE.getZ() + 0.5, 0, 0);
    }

    private static Vec3 ground(ServerLevel level) {
        level.getChunk(BASE.getX() >> 4, BASE.getZ() >> 4);
        return Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BASE));
    }

    private static Vec3 at(double dx, double dy, double dz) {
        return ground.add(dx, dy, dz);
    }

    private static <T extends Entity> T spawn(ServerLevel level, EntityType<T> type, Vec3 pos, float yaw) {
        T e = type.create(level);
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0);
        e.setYRot(yaw);
        if (e instanceof net.minecraft.world.entity.LivingEntity l) {
            l.setYBodyRot(yaw);
            l.setYHeadRot(yaw);
        }
        if (e instanceof net.minecraft.world.entity.Mob m) {
            m.setNoAi(true);
            m.setPersistenceRequired();
        }
        level.addFreshEntity(e);
        staged.add(e);
        return e;
    }

    private static ChuckEntity chuck(ServerLevel level, Vec3 pos, int phase, float yaw) {
        ChuckEntity c = spawn(level, AllEntities.CHUCK.get(), pos, yaw);
        c.forceLook(phase);
        return c;
    }

    private static void clear() {
        staged.forEach(e -> {
            if (e != owner) e.discard();
        });
        staged.removeIf(e -> e != owner);
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 look) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, look);
    }

    /** Calls a protected setter of the boss (the script's cracks, the blank page): a preview may, the game may not. */
    private static void poke(ChuckEntity c, String method, Class<?> type, Object value) {
        try {
            var m = ChuckEntity.class.getDeclaredMethod(method, type);
            m.setAccessible(true);
            m.invoke(c, value);
        } catch (ReflectiveOperationException e) {
            SupernaturalCraft.LOGGER.warn("Preview could not call ChuckEntity.{}", method, e);
        }
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_chuck_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }

    // --- chuck_model ---------------------------------------------------------------------------------------------------

    private static ChuckEntity light;

    private static void modelScene(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) setUp(p, GameType.SPECTATOR);
        if (now == 30) {
            ground = ground(level);
            owner = chuck(level, at(0, 0, -60), 1, 0);
        }
        if (ground == null) return;
        if (light != null && !light.isRemoved()) light.placeNodes(level, level.getGameTime());
        // The man: robe (at home), flannel (Eden), suit (Hell).
        if (now == 40) {
            spawn(level, AllEntities.AUTHOR_NPC.get(), at(-2.2, 0, 0), 0);
            chuck(level, at(0, 0, 0), 1, 0);
            chuck(level, at(2.2, 0, 0), 2, 0);
            view(p, at(0, 1.6, 5.5), at(0, 1.1, 0));
        }
        if (now == 100) view(p, at(6, 1.8, 1), at(0, 1.1, 0));
        if (now == 140) view(p, at(0.5, 2.2, -5.5), at(0, 1.1, 0));
        if (now == 175) view(p, at(0, 1.7, 2.2), at(0, 1.55, 0));
        // The light: chapter 3 from far, near; its nodes against their hitboxes; chapters 4 and 5.
        if (now == 210) {
            clear();
            light = chuck(level, at(0, 0, 0), 3, 0);
            view(p, at(0, 10, 34), at(0, 8, 0));
        }
        if (now == 260) view(p, at(18, 14, 26), at(0, 8, 0));
        if (now == 300) view(p, at(0, 9, 13), at(0, 8.5, 0));
        if (now == 330) light.forceLook(4);
        if (now == 340) view(p, at(0, 12, 26), at(0, 8.5, 0));
        if (now == 420) view(p, at(20, 12, 10), at(0, 8.5, 0));
        if (now == 450) {
            poke(light, "setScriptBroken", float.class, 0.6f);
            poke(light, "setWindowOpen", boolean.class, true);
        }
        if (now == 500) {
            light.forceLook(5);
            for (boolean left : new boolean[]{false, true}) {
                AuthorHandEntity h = spawn(level, AllEntities.AUTHOR_HAND.get(), at(left ? 9 : -9, 7, 2), 0);
                h.setLeft(left);
                h.bind(light);
            }
            view(p, at(0, 10, 30), at(0, 8, 0));
        }
        if (now == 560) view(p, at(-14, 9, 14), at(-6, 7, 0));
        // Props: keys, words, pages, a node.
        if (now == 600) {
            clear();
            light = null;
            int i = 0;
            for (char ch : "SAM".toCharArray()) {
                TypewriterKeyEntity k = spawn(level, AllEntities.TYPEWRITER_KEY.get(), at(-6 + i * 2.5, 0.6, 0), 20 * i);
                k.setLetter(ch);
                k.drop(owner, at(-6 + i * 2.5, 0.1, 0), 400);
                i++;
            }
            FloatingWordEntity w1 = spawn(level, AllEntities.FLOATING_WORD.get(), at(1, 2.5, 0), 0);
            w1.setText("AND THEN");
            w1.setSize(1.2f);
            w1.bind(owner, 0, 600);
            FloatingWordEntity w2 = spawn(level, AllEntities.FLOATING_WORD.get(), at(-4, 4.5, -2), 0);
            w2.setText("THE END");
            w2.setSize(2f);
            w2.setColor(0x8A1A10);
            w2.bind(owner, 0, 600);
            for (int k = 0; k < 2; k++) {
                AuthorTargetEntity page = spawn(level, AllEntities.AUTHOR_TARGET.get(), at(4 + k * 2.5, 1, 0), 0);
                page.setKind(AuthorTargetEntity.PAGE);
                page.bind(owner, k);
                page.setShielded(k == 1);
                page.setCracks(k == 1 ? 0.6f : 0.2f);
            }
            AuthorTargetEntity node = spawn(level, AllEntities.AUTHOR_TARGET.get(), at(9.5, 1, 0), 0);
            node.setKind(AuthorTargetEntity.NODE);
            node.bind(owner, 20);
            node.setShielded(true);
            node.setCracks(0.5f);
            view(p, at(1, 3.5, 11), at(1, 1.5, 0));
        }
        if (now == 650) view(p, at(7, 2.5, 5), at(6, 1.5, 0));
        // An ink echo of every boss.
        if (now == 680) {
            clear();
            String[] bosses = {"azazel", "lilith", "lucifer", "metatron", "lucifer_uncaged", "amara", "broken_chorus"};
            for (int i = 0; i < bosses.length; i++) {
                InkEchoEntity e = spawn(level, AllEntities.INK_ECHO.get(), at(-15 + i * 5, 0, 0), 0);
                e.setBoss(bosses[i]);
                e.bind(owner);
            }
            view(p, at(0, 6, 22), at(0, 2.5, 0));
        }
        if (now == 730) view(p, at(-9, 2.5, 9), at(-9, 1.4, 0));
        if (now == 760) view(p, at(8, 4, 13), at(8, 2.5, 0));
        // The brothers and the angel, stepping out of the light toward the Author.
        if (now == 790) {
            clear();
            owner.teleportTo(at(0, 0, -6).x, at(0, 0, -6).y, at(0, 0, -6).z);
            for (byte who = 0; who < 3; who++) {
                HunterAllyEntity a = spawn(level, AllEntities.HUNTER_ALLY.get(), at(-1.6 + who * 1.6, 0, 0), 180);
                a.setWho(who);
                a.bind(owner);
            }
            view(p, at(0.6, 1.7, 5), at(0, 1.1, 0));
        }
        if (now == 860) view(p, at(-4, 1.8, 3.5), at(0, 1.1, 0));
        if (now == 900) {
            clear();
            owner.discard();
        }
    }

    private static void modelClient(Minecraft mc, int now) {
        if (now == 2) mc.options.hideGui = true;
        // Keep the node targets exactly where the bones are this frame's tick, so the boxes can be compared.
        if (mc.level != null && now > 210 && now < 600) {
            for (ChuckEntity c : mc.level.getEntitiesOfClass(ChuckEntity.class, mc.player.getBoundingBox().inflate(80))) {
                if (!c.divine()) continue;
                for (AuthorTargetEntity n : mc.level.getEntitiesOfClass(AuthorTargetEntity.class, c.getBoundingBox().inflate(16, 24, 16))) {
                    if (n.kind() != AuthorTargetEntity.NODE) continue;
                    Vec3 v = c.nodeAt(n.ring(), n.node(), mc.level.getGameTime());
                    n.setPos(v);
                    n.xo = n.xOld = v.x;
                    n.yo = n.yOld = v.y;
                    n.zo = n.zOld = v.z;
                }
            }
        }
        if (now == 395 || now == 412) mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
        if (now == 405 || now == 420) mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
        String shot = switch (now) {
            case 95 -> "model_men_front";
            case 135 -> "model_men_side";
            case 170 -> "model_men_back";
            case 205 -> "model_men_faces";
            case 255 -> "model_light3_far";
            case 295 -> "model_light3_quarter";
            case 328 -> "model_light3_near";
            case 390 -> "model_light4";
            case 400 -> "model_light4_hitboxes";
            case 417 -> "model_light4_hitboxes_side";
            case 445 -> "model_light4_side";
            case 495 -> "model_light4_cracked";
            case 555 -> "model_light5_hands";
            case 595 -> "model_hands_near";
            case 645 -> "model_props";
            case 675 -> "model_props_near";
            case 725 -> "model_echoes";
            case 755 -> "model_echoes_left";
            case 785 -> "model_echoes_right";
            case 805 -> "model_allies_appear";
            case 855 -> "model_allies";
            case 895 -> "model_allies_side";
            default -> null;
        };
        if (shot != null) grab(mc, shot);
        if (now >= 905) mc.stop();
    }

    // --- chuck_fx ----------------------------------------------------------------------------------------------------

    private static void fxScene(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) setUp(p, GameType.SURVIVAL);
        if (now == 30) {
            ground = ground(level);
            owner = chuck(level, at(0, 0, -24), 3, 0);
            p.teleportTo(level, ground.x, ground.y, ground.z, 180, -8);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
            p.getInventory().setItem(1, new ItemStack(Items.BREAD, 12));
            p.getInventory().setItem(2, new ItemStack(Items.TORCH, 32));
            p.getInventory().setItem(3, new ItemStack(Items.BOW));
            p.getInventory().selected = 0;
            p.setHealth(14);
            bar = new ServerBossEvent(Component.translatable("entity.supernaturalcraft.chuck.bar.storm"), BossEvent.BossBarColor.WHITE,
                    BossEvent.BossBarOverlay.NOTCHED_10);
            bar.setProgress(0.62f);
            bar.addPlayer(p);
        }
        if (ground == null) return;
        if (now > 30 && now < 280) p.lookAt(EntityAnchorArgument.Anchor.EYES, owner.position().add(0, 8, 0));
        if (now == 280) p.lookAt(EntityAnchorArgument.Anchor.EYES, at(0, 0, -6));
        if (now == 500) p.lookAt(EntityAnchorArgument.Anchor.EYES, owner.position().add(0, 8, 0));
        if (now == 2080 && bar != null) bar.removeAllPlayers();
        if (now == 2085) owner.discard();
    }

    private static void fire(Minecraft mc, byte kind, int arg, Vec3 point, float radius, int duration, String text) {
        int id = kind == AuthorFxPayload.BACKSPACE ? mc.player.getId() : 0;
        ClientChuck.handle(new AuthorFxPayload(id, kind, arg, point, radius, duration, text));
    }

    private static void fxClient(Minecraft mc, int now) {
        if (now == 2) mc.options.hideGui = false;
        if (mc.player == null || ground == null) return;
        Vec3 me = mc.player.position();
        Vec3 core = me.add(0, 8, -24);
        switch (now) {
            case 60 -> fire(mc, AuthorFxPayload.CHAPTER_TITLE, 2, me, 0, 0, "");
            case 160 -> fire(mc, AuthorFxPayload.NARRATE, 0, me, 0, 110, "rule.supernaturalcraft.light_hurts");
            case 230 -> fire(mc, AuthorFxPayload.RULE, AuthorRules.WATER_BURNS | AuthorRules.FLOOR_LAVA, me, 0, 200, "");
            case 285 -> fire(mc, AuthorFxPayload.SNAP_COUNT, 0, me.add(0, 0, -3), 5, 100, "");
            case 390 -> fire(mc, AuthorFxPayload.BACKSPACE, 0, me.add(6, 0, -8), 0, 0, "");
            case 420 -> fire(mc, AuthorFxPayload.HUD_REWRITE, 0, me, 0, 160, "");
            case 500 -> fire(mc, AuthorFxPayload.CRACK, 0, core, 0, 60, "");
            case 560 -> fire(mc, AuthorFxPayload.GRAVITY, ChuckEntity.GRAVITY_INVERTED, me, 0, 120, "");
            case 700 -> fire(mc, AuthorFxPayload.ARENA_WAVE, 2, me.add(0, 0, -10), 26, 60, "");
            case 780 -> fire(mc, AuthorFxPayload.WHITE_OUT, 90, me, 0, 40, "");
            case 850 -> fire(mc, AuthorFxPayload.WHITE_OUT, 0, me, 0, 20, "");
            case 880 -> fire(mc, AuthorFxPayload.FAKE_CREDITS, 0, me, 0, 200, "");
            case 1170 -> fire(mc, AuthorFxPayload.CREDITS, 0, me, 0, 900, "");
            default -> {
            }
        }
        String shot = switch (now) {
            case 55 -> "fx_page_bossbar";
            case 95 -> "fx_title_typing";
            case 140 -> "fx_title";
            case 205 -> "fx_narrate";
            case 255 -> "fx_rule";
            case 320 -> "fx_snap";
            case 394 -> "fx_backspace";
            case 402 -> "fx_backspace_late";
            case 445 -> "fx_hud_rewrite";
            case 503 -> "fx_crack";
            case 515 -> "fx_crack_late";
            case 600 -> "fx_gravity_inverted";
            case 730 -> "fx_arena_wave";
            case 840 -> "fx_white_out";
            case 990 -> "fx_fake_credits";
            case 1092 -> "fx_not_like_this";
            case 1118 -> "fx_tear";
            case 1400 -> "fx_credits";
            case 2010 -> "fx_carry_on";
            default -> null;
        };
        if (shot != null) grab(mc, shot);
        if (now >= 2090) mc.stop();
    }
}
