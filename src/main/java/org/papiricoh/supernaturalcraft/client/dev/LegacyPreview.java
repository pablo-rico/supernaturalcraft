package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.client.legacy.CaseBriefScreen;
import org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy;
import org.papiricoh.supernaturalcraft.client.legacy.HenryDialogueScreen;
import org.papiricoh.supernaturalcraft.client.legacy.LegacyOverlay;
import org.papiricoh.supernaturalcraft.client.legacy.LegacyToasts;
import org.papiricoh.supernaturalcraft.client.legacy.ResearchScreen;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.legacy.HenryDialogue;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerBuilder;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLayout;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchMenu;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchOffer;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Men of Letters' scenes for the dev preview (v0.17), well away from the origin. Screenshots land in
 * {@code runs/client/screenshots/sn_legacy_*.png}.
 * <ul>
 *   <li>{@code SN_PREVIEW=legacy_models}: Henry and the three monsters front, back and side, every clip at its moment, the werewolf
 *   in both forms (and turning), the vampire's fangs, the shapeshifter disguised (villager, player) and revealed, by night too.</li>
 *   <li>{@code SN_PREVIEW=legacy_bunker}: a bunker built on the spot, buried, and a walk through it: the hill, the door, then every
 *       room's spot from {@code BunkerLayout.views()} ({@code room_<name>}), then doll's-house views of each level from above.</li>
 *   <li>{@code SN_PREVIEW=legacy_research}: the research desk's screen with a board, the toasts, the rank's title card, Henry's
 *   card, a case brief, and the journal's Archive (hidden from an outsider, then a member's).</li>
 *   <li>{@code SN_PREVIEW=legacy_case}: a case handed out and its site visited.</li>
 * </ul>
 * {@code SN_LEGACY_ONLY=a,b} keeps only the beats whose names start so.
 */
final class LegacyPreview {

    private static final int STEP = 30;
    private static int t = -1;
    private static BlockPos ground;
    private static final List<Entity> shown = new ArrayList<>();

    private record Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client, int length) {
        Beat(String name, BiConsumer<ServerPlayer, Vec3> server, Runnable client) {
            this(name, server, client, STEP);
        }
    }

    private static List<Beat> beats;
    private static int beatIndex, beatStart;

    private LegacyPreview() {
    }

    /** @return true while one of its scenes runs (the harness then skips the rest) */
    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (scene == null || !scene.startsWith("legacy_")) return false;
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return true;
        t++;
        if (t == 0) {
            beats = switch (scene) {
                case "legacy_bunker", "legacy_bunker_real" -> bunker(mc, scene.equals("legacy_bunker_real"));
                case "legacy_research" -> research(mc);
                case "legacy_case" -> cases(mc);
                default -> models(mc);
            };
            String only = System.getenv("SN_LEGACY_ONLY");
            if (only != null && !only.isBlank()) {
                List<String> keep = List.of(only.split(","));
                beats = beats.stream().filter(b -> keep.stream().anyMatch(k -> b.name.startsWith(k.trim()))).toList();
            }
            int x = scene.equals("legacy_bunker") ? 5200 : 3200;
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst(), x, 412));
            return true;
        }
        if (t < 30 || ground == null) return true;
        if (beatIndex >= beats.size()) {
            if (t - beatStart == 5) {
                mc.setScreen(null);
                server.execute(LegacyPreview::clearShown);
            }
            if (t - beatStart == 15) mc.stop();
            return true;
        }
        Beat b = beats.get(beatIndex);
        if (beatStart == 0) beatStart = t;
        int local = t - beatStart;
        Vec3 c = Vec3.atBottomCenterOf(ground);
        if (local == 0) server.execute(() -> b.server.accept(server.getPlayerList().getPlayers().getFirst(), c));
        if (local == 3 && b.client != null) b.client.run();
        if (local == b.length - 2) {
            Screenshot.grab(mc.gameDirectory, "sn_" + scene + "_" + b.name + ".png", mc.getMainRenderTarget(), m -> {
            });
        }
        if (local >= b.length) {
            beatIndex++;
            beatStart = t + 1;
        }
        return true;
    }

    // --- set-up and helpers --------------------------------------------------------------------------------------------

    private static void setUp(ServerPlayer p, int x, int z) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.getChunk(x >> 4, z >> 4);
        ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                level.setBlockAndUpdate(ground.offset(dx, -1, dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        p.setGameMode(GameType.SPECTATOR);
        Vec3 c = Vec3.atBottomCenterOf(ground);
        p.teleportTo(level, c.x, c.y + 2, c.z + 6, 180, 0);
    }

    private static void clearShown() {
        for (Entity e : shown) e.discard();
        shown.clear();
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, Vec3 at, float yaw) {
        T e = type.create(level);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        shown.add(e);
        field(e, "persistentNoAi", true);
        if (e instanceof org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity h) h.home(BlockPos.containing(at), yaw);
        return e;
    }

    /** Sets a plain field of a creature (its class or a parent's), quietly doing nothing if it is not there. */
    private static void field(Entity e, String name, Object value) {
        for (Class<?> c = e.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(e, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // a parent's, maybe
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                return;
            }
        }
    }

    /** The werewolf's form, held whatever the hour. */
    private static void wolf(Entity e, boolean wolf) {
        field(e, "forced", wolf ? 1 : 0);
        data(e, "WOLF", wolf);
    }

    /** Sets a synced field of a Men of Letters creature directly (the preview has no AI to set it). */
    @SuppressWarnings("unchecked")
    private static <V> void data(Entity e, String field, V value) {
        try {
            var f = e.getClass().getDeclaredField(field);
            f.setAccessible(true);
            e.getEntityData().set((EntityDataAccessor<V>) f.get(null), value);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.warn("preview: no {} on {}", field, e);
        }
    }

    private static void camera(Minecraft mc, CameraType type, boolean hud) {
        mc.execute(() -> {
            mc.options.setCameraType(type);
            mc.options.hideGui = !hud;
        });
    }

    private static void command(ServerPlayer p, String cmd) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
    }

    private static Mob creature(ServerLevel level, String id, Vec3 at) {
        EntityType<? extends Mob> type = switch (id) {
            case "henry_winchester" -> AllEntities.HENRY.get();
            case "vampire" -> AllEntities.VAMPIRE.get();
            case "werewolf" -> AllEntities.WEREWOLF.get();
            default -> AllEntities.SHAPESHIFTER.get();
        };
        return place(level, type, at, 0);
    }

    // --- SN_PREVIEW=legacy_models --------------------------------------------------------------------------------------

    private static List<Beat> models(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        Vec3 eye = new Vec3(0, 1.0, 0);
        Mob[] cur = new Mob[1];
        for (String id : LegacyAssets.CREATURES) {
            out.add(new Beat(id + "_front", (p, c) -> {
                clearShown();
                p.serverLevel().setDayTime(6000);
                cur[0] = creature(p.serverLevel(), id, c);
                if (id.equals("werewolf")) wolf(cur[0], false);
                view(p, c.add(0.2, -0.3, 1.25), c.add(eye));
            }, () -> camera(mc, CameraType.FIRST_PERSON, false)));
            out.add(new Beat(id + "_back", (p, c) -> view(p, c.add(-0.2, -0.3, -1.25), c.add(eye)), null));
            out.add(new Beat(id + "_side", (p, c) -> view(p, c.add(1.25, -0.3, 0.2), c.add(eye)), null));
            out.add(new Beat(id + "_face", (p, c) -> view(p, c.add(0.05, 0.1, 0.55), c.add(0, 1.65, 0)), null));
            if (id.equals("werewolf")) {
                for (String v : List.of("front", "back", "side", "night")) {
                    out.add(new Beat("werewolf_wolf_" + v, (p, c) -> {
                        if (v.equals("front")) {
                            clearShown();
                            cur[0] = creature(p.serverLevel(), id, c);
                        }
                        wolf(cur[0], true);
                        p.serverLevel().setDayTime(v.equals("night") ? 18000 : 6000);
                        Vec3 from = switch (v) {
                            case "back" -> c.add(-0.3, -0.1, -1.7);
                            case "side" -> c.add(1.7, -0.1, 0.2);
                            default -> c.add(0.3, -0.1, 1.7);
                        };
                        view(p, from, c.add(0, 1.2, 0));
                    }, null));
                }
            }
            if (id.equals("vampire")) {
                out.add(new Beat("vampire_fangs", (p, c) -> {
                    view(p, c.add(0.05, 0.1, 0.55), c.add(0, 1.65, 0));
                    ((software.bernie.geckolib.animatable.GeoEntity) cur[0]).triggerAnim("action", "fangs_out");
                }, null, 10));
                out.add(new Beat("vampire_night", (p, c) -> {
                    p.serverLevel().setDayTime(18000);
                    view(p, c.add(0.2, -0.3, 1.25), c.add(eye));
                }, null));
            }
            if (id.equals("shapeshifter")) {
                out.add(new Beat("shapeshifter_villager", (p, c) -> {
                    data(cur[0], "DISGUISE", "villager:minecraft:librarian");
                    view(p, c.add(0.2, -0.3, 1.25), c.add(eye));
                }, null));
                out.add(new Beat("shapeshifter_player", (p, c) -> {
                    // A spectator is drawn as a floating head: wear the hunter's face while they stand in creative.
                    p.setGameMode(GameType.CREATIVE);
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                    data(cur[0], "DISGUISE", "player:" + p.getUUID());
                    view(p, c.add(0.2, -0.3, 1.25), c.add(0, 1.0, 0));
                }, null));
                out.add(new Beat("shapeshifter_revealed", (p, c) -> {
                    p.setGameMode(GameType.SPECTATOR);
                    data(cur[0], "DISGUISE", "");
                    data(cur[0], "REVEALED", true);
                    view(p, c.add(0.2, -0.3, 1.25), c.add(eye));
                }, null));
                out.add(new Beat("shapeshifter_revealed_face", (p, c) -> view(p, c.add(0.05, 0.1, 0.55), c.add(0, 1.65, 0)), null));
                out.add(new Beat("shapeshifter_revealed_night", (p, c) -> {
                    p.serverLevel().setDayTime(18000);
                    view(p, c.add(0.2, -0.3, 1.25), c.add(eye));
                }, null));
            }
            // Every clip it plays once, from three-quarters, caught near its middle.
            for (String clip : LegacyAssets.triggered(id)) {
                boolean wolfClip = id.equals("werewolf") && !clip.startsWith("human");
                int at = clip.equals("decapitated") || clip.equals("shed") || clip.equals("rise") ? 34 : clip.equals("turn") ? 28 : 12;
                out.add(new Beat(id + "_" + clip, (p, c) -> {
                    clearShown();
                    p.serverLevel().setDayTime(6000);
                    cur[0] = creature(p.serverLevel(), id, c);
                    if (id.equals("werewolf")) wolf(cur[0], wolfClip && !clip.equals("turn"));
                    if (id.equals("shapeshifter")) data(cur[0], "REVEALED", true);
                    view(p, c.add(1.5, 0.0, 1.7), c.add(0, 1.0, 0));
                }, null, at + 6));
                out.add(new Beat(id + "_" + clip + "_go", (p, c) -> ((software.bernie.geckolib.animatable.GeoEntity) cur[0]).triggerAnim("action", clip), null, at));
            }
        }
        return out;
    }

    // --- SN_PREVIEW=legacy_bunker --------------------------------------------------------------------------------------

    private static BlockPos origin;
    private static int quarter;

    /** A local point of the plan (block units, cell c spanning [c, c+1)) in the world, after the bunker's turn. */
    private static Vec3 point(double x, double y, double z) {
        double cx = x - 0.5, cz = z - 0.5;
        double rx = switch (Math.floorMod(quarter, 4)) {
            case 1 -> -cz;
            case 2 -> -cx;
            case 3 -> cz;
            default -> cx;
        }, rz = switch (Math.floorMod(quarter, 4)) {
            case 1 -> cx;
            case 2 -> -cz;
            case 3 -> -cx;
            default -> cz;
        };
        return new Vec3(origin.getX() + rx + 0.5, origin.getY() + y, origin.getZ() + rz + 0.5);
    }

    private static List<Beat> bunker(Minecraft mc, boolean real) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("build", (p, c) -> {
            ServerLevel level = p.serverLevel();
            // The preview world is a thin superflat: the bunker is built 45 up on a plate of earth of its own, so it lies buried
            // as in a real world (its rooms are walled in rock) and the hill stands on ground.
            if (real) {
                // SN_PREVIEW=legacy_bunker_real: the bunker the world generated (a world copied from a server that loaded it).
                var site = org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLocator.of(level);
                origin = site.origin();
                quarter = site.rotation();
                Vec3 o = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.OUTSIDE));
                view(p, o.add(0, 30, 0), o);
                return;
            }
            quarter = 0;
            origin = ground.offset(0, 45, -20);
            for (int x = -64; x <= 64; x++) {
                for (int z = -96; z <= 40; z++) {
                    for (int y = -3; y <= 0; y++) {
                        BlockPos at = BunkerBuilder.at(origin, quarter, new int[]{x, y, z});
                        level.setBlock(at, (y == 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), 2 | 16);
                    }
                }
            }
            BunkerBuilder.placeAt(level, origin, 0);
            Vec3 o = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.OUTSIDE));
            view(p, point(BunkerLayout.OUTSIDE[0] + 6.5, BunkerLayout.OUTSIDE[1] + 4, BunkerLayout.OUTSIDE[2] + 9.5), o.add(0, 2, 0));
        }, () -> camera(mc, CameraType.FIRST_PERSON, false), real ? 200 : 80));
        out.add(new Beat("hill", (p, c) -> {
            Vec3 o = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.OUTSIDE));
            view(p, point(BunkerLayout.OUTSIDE[0] + 0.5 - 14, BunkerLayout.OUTSIDE[1] + 9, BunkerLayout.OUTSIDE[2] + 0.5 + 16), o.add(0, 3, 0));
        }, null));
        out.add(new Beat("door", (p, c) -> {
            Vec3 o = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.OUTSIDE));
            Vec3 d = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.DOOR));
            view(p, point(BunkerLayout.OUTSIDE[0] + 2, BunkerLayout.OUTSIDE[1] + 0.2, BunkerLayout.OUTSIDE[2] + 3), d.add(0, 2, 0));
        }, null));
        // Every room's spot, as the plan's parts list them.
        for (var v : BunkerLayout.views().entrySet()) {
            var s = v.getValue();
            out.add(new Beat("room_" + v.getKey(), (p, c) -> {
                Vec3 feet = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, new int[]{s.x(), s.y(), s.z()}));
                Vec3 look = point(s.lx() + 0.5, s.ly() + 0.5, s.lz() + 0.5);
                view(p, feet, look);
                if (v.getKey().equals("map_table") && shown.isEmpty()) {
                    Vec3 m = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.MAP_TABLE));
                    Mob h = place(p.serverLevel(), AllEntities.HENRY.get(), Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.HENRY)),
                            org.papiricoh.supernaturalcraft.legacy.bunker.BunkerWorld.henryYaw(quarter));
                    h.lookAt(EntityAnchorArgument.Anchor.EYES, m);
                }
            }, null, real ? 70 : 40));
        }
        if (real) {
            out.add(new Beat("hill_far", (p, c) -> {
                Vec3 o = Vec3.atBottomCenterOf(BunkerBuilder.at(origin, quarter, BunkerLayout.OUTSIDE));
                Vec3 out2 = point(BunkerLayout.OUTSIDE[0] + 0.5 - 10, BunkerLayout.OUTSIDE[1] + 8, BunkerLayout.OUTSIDE[2] + 0.5 + 22);
                view(p, out2, o.add(0, 3, 0));
            }, null, 80));
            return out;
        }
        // Doll's-house views: each level with everything above it cleared away, seen from high over the south-east.
        int[][] cuts = {{-4, 0}, {-12, 0}, {-21, 1}, {-31, 2}};
        String[] names = {"dollhouse_level1", "dollhouse_gallery", "dollhouse_level2", "dollhouse_level3"};
        for (int i = 0; i < cuts.length; i++) {
            int cut = cuts[i][0];
            out.add(new Beat(names[i], (p, c) -> {
                ServerLevel level = p.serverLevel();
                clearShown();
                for (int x = BunkerLayout.MIN_X; x <= BunkerLayout.MAX_X; x++) {
                    for (int z = BunkerLayout.MIN_Z; z <= BunkerLayout.MAX_Z; z++) {
                        for (int y = cut + 1; y <= BunkerLayout.CLEAR_TO; y++) {
                            BlockPos at = BunkerBuilder.at(origin, quarter, new int[]{x, y, z});
                            if (!level.getBlockState(at).isAir()) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16);
                        }
                    }
                }
                Vec3 o = Vec3.atLowerCornerOf(BunkerBuilder.at(origin, quarter, new int[]{0, cut, -30}));
                view(p, o.add(26, 34, 30), o.add(0, -3, -4));
            }, null, 60));
        }
        return out;
    }

    // --- SN_PREVIEW=legacy_research ------------------------------------------------------------------------------------

    private static List<Beat> research(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("archive_outsider", (p, c) -> {
            command(p, "supernatural legacy reset");
            view(p, c.add(0, 1, 3), c);
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            HunterBookScreen book = new HunterBookScreen(HunterBookScreen.Tab.JOURNAL);
            mc.setScreen(book);
            book.journal().openChapter(JournalChapter.BASICS);
        }, 40));
        out.add(new Beat("archive_member", (p, c) -> {
            command(p, "supernatural legacy rank 3");
            for (String topic : List.of("lore:order_history", "lore:bunker", "lore:henry", "boss:supernaturalcraft:lucifer", "formula:0",
                    "rite:0", "creature:minecraft:zombie", "creature:minecraft:zombie")) {
                command(p, "supernatural legacy research grant " + topic);
            }
        }, () -> {
            HunterBookScreen book = new HunterBookScreen(HunterBookScreen.Tab.ARCHIVE);
            mc.setScreen(book);
            book.archive().openShelf("order");
        }, 50));
        out.add(new Beat("archive_journal_tab", (p, c) -> {
        }, () -> {
            HunterBookScreen book = new HunterBookScreen(HunterBookScreen.Tab.JOURNAL);
            mc.setScreen(book);
            book.journal().openChapter(JournalChapter.BASICS);
        }, 30));
        for (String shelf : org.papiricoh.supernaturalcraft.client.book.archive.ArchiveSection.SHELVES) {
            out.add(new Beat("archive_shelf_" + shelf, (p, c) -> {
            }, () -> {
                HunterBookScreen book = new HunterBookScreen(HunterBookScreen.Tab.ARCHIVE);
                mc.setScreen(book);
                book.archive().openShelf(shelf);
            }, 25));
        }
        out.add(new Beat("archive_formula", (p, c) -> {
        }, () -> openArchive(mc, "archive_formula_0"), 40));
        out.add(new Beat("archive_rite", (p, c) -> {
        }, () -> openArchive(mc, "archive_rite_0"), 40));
        out.add(new Beat("archive_creature", (p, c) -> {
        }, () -> openArchive(mc, "archive_creature_minecraft_zombie"), 40));
        out.add(new Beat("archive_lore", (p, c) -> {
        }, () -> openArchive(mc, "archive_lore_bunker"), 40));
        out.add(new Beat("archive_boss", (p, c) -> {
        }, () -> openArchive(mc, "archive_boss_lucifer"), 40));
        out.add(new Beat("desk_screen", (p, c) -> {
        }, () -> {
            long now = mc.level.getGameTime();
            ClientLegacy.set(ClientLegacy.legacy(), ClientLegacy.archive().withSlots(List.of(
                    new org.papiricoh.supernaturalcraft.legacy.research.ResearchSlot("lore:the_key", now - 2400, now + 1300))));
            var inv = mc.player.getInventory();
            ResearchScreen s = new ResearchScreen(new ResearchMenu(0, inv), inv, Component.translatable("container.supernaturalcraft.research"));
            mc.setScreen(s);
            s.previewBoard(2, previewOffers());
        }, 40));
        out.add(new Beat("toasts", (p, c) -> {
        }, () -> {
            mc.setScreen(null);
            LegacyToasts.research("lore:bunker");
            LegacyToasts.research("creature:minecraft:zombie");
            LegacyToasts.caseClosed(0, true);
        }, 40));
        out.add(new Beat("rank_card", (p, c) -> {
        }, () -> LegacyOverlay.showNow(3), 30));
        Mob[] henry = new Mob[1];
        out.add(new Beat("henry_card", (p, c) -> {
            clearShown();
            henry[0] = place(p.serverLevel(), AllEntities.HENRY.get(), c.add(0, 0, 0), 0);
            view(p, c.add(0, 1, 3), c.add(0, 1.5, 0));
        }, () -> mc.execute(() -> {
            LegacyOverlay.clear();
            Entity h = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(8), e -> e.getType() == AllEntities.HENRY.get())
                    .stream().findFirst().orElse(null);
            HenryDialogueScreen s = new HenryDialogueScreen(h == null ? -1 : h.getId(), HenryDialogue.Stage.OFFER);
            mc.setScreen(s);
            s.previewLine(5);
        }), 40));
        out.add(new Beat("case_brief", (p, c) -> {
        }, () -> mc.setScreen(new CaseBriefScreen(new CaseFile(2, ground.offset(640, 0, -420), "barn",
                ResourceLocation.parse("supernaturalcraft:vampire"), "hostage", 2, 0, CaseFile.ACTIVE))), 40));
        return out;
    }

    private static void openArchive(Minecraft mc, String id) {
        HunterBookScreen book = new HunterBookScreen(HunterBookScreen.Tab.ARCHIVE);
        mc.setScreen(book);
        book.openEntry(org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource(id));
    }

    private static List<ResearchOffer> previewOffers() {
        List<ItemStack> notes = List.of(FieldNotesItem.stack("arcane", 3), new ItemStack(Items.PAPER, 2), new ItemStack(Items.INK_SAC));
        return List.of(
                new ResearchOffer("formula:1", 1, notes, 3600, "research.supernaturalcraft.topic.formula", List.of("2"), true),
                new ResearchOffer("rite:1", 2, notes, 5200, "research.supernaturalcraft.topic.rite", List.of("2"), false),
                new ResearchOffer("creature:minecraft:zombie", 1, List.of(FieldNotesItem.stack("creature:minecraft:zombie", 4),
                        new ItemStack(Items.PAPER)), 4300, "research.supernaturalcraft.topic.creature", List.of("#entity.minecraft.zombie", "2"), true),
                new ResearchOffer("boss:supernaturalcraft:azazel", 2, notes, 7200, "research.supernaturalcraft.topic.boss",
                        List.of("#entity.supernaturalcraft.azazel"), true),
                new ResearchOffer("lore:the_key", 2, List.of(FieldNotesItem.stack("place", 5)), 6000, "research.supernaturalcraft.topic.lore",
                        List.of("#research.supernaturalcraft.lore.the_key"), false),
                new ResearchOffer("artifact:42", 3, List.of(FieldNotesItem.stack("relic", 6), new ItemStack(AllItems.DEAD_MANS_BLOOD.get())),
                        9000, "research.supernaturalcraft.topic.artifact", List.of("#artifact.supernaturalcraft.form.mirror"), true));
    }

    // --- SN_PREVIEW=legacy_case ---------------------------------------------------------------------------------------

    private static List<Beat> cases(Minecraft mc) {
        List<Beat> out = new ArrayList<>();
        out.add(new Beat("brief", (p, c) -> {
            command(p, "supernatural legacy rank 2");
            command(p, "supernatural legacy case new");
        }, () -> {
            camera(mc, CameraType.FIRST_PERSON, true);
            var cases = ClientLegacy.legacy().cases();
            if (!cases.isEmpty()) ClientLegacy.openCaseBrief(cases.getLast().index());
        }, 50));
        out.add(new Beat("site", (p, c) -> {
            p.setGameMode(GameType.CREATIVE);
            command(p, "supernatural legacy case go");
        }, () -> mc.setScreen(null), 100));
        out.add(new Beat("site_above", (p, c) -> view(p, p.position().add(6, 8, 6), p.position()), null, 40));
        out.add(new Beat("site_night", (p, c) -> p.serverLevel().setDayTime(18000), null, 40));
        return out;
    }
}
