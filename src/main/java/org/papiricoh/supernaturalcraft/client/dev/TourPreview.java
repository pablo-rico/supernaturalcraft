package org.papiricoh.supernaturalcraft.client.dev;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.author.AuthorSite;
import org.papiricoh.supernaturalcraft.cain.CainSavedData;
import org.papiricoh.supernaturalcraft.cain.CainSite;
import org.papiricoh.supernaturalcraft.cain.CainWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.ClientMagicFx;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSavedData;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaSite;
import org.papiricoh.supernaturalcraft.coven.rowena.RowenaWorld;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlotLayout;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.RoadhouseLayout;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.cage.CageBuilder;
import org.papiricoh.supernaturalcraft.hell.cage.CageController;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerBuilder;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLayout;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerLocator;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.network.RitualChargePayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@code SN_PREVIEW=tour}: the screenshot tour for the mod's pages, in a real world ({@code runClient -Ptour} opens
 * {@code sn_tour}, a world generated by {@code runServer}). Stop after stop it finds a structure the world generated (or
 * builds it on flat ground if none is near), or calls a boss onto flat ground and fights it with an invulnerable hunter,
 * forcing its phases; each shot lands in the screenshots folder as {@code tour_NN_stop_shot.png}. Boss shots keep only
 * the boss bars of the HUD (no hotbar, hand or chat); the rest hide the GUI. Chain {@code book} after it for the journal.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class TourPreview {

    static final String NAME = "tour";

    private static final int DUSK = 12600, NOON = 6000, MORNING = 1500, NIGHT = 13300;
    private static final CameraType FIRST = CameraType.FIRST_PERSON, BACK = CameraType.THIRD_PERSON_BACK,
            FRONT = CameraType.THIRD_PERSON_FRONT;

    private record Shot(int at, String name, CameraType camera, boolean hud) {
    }

    private static final class Stop {
        final String name;
        final int length;
        final BiConsumer<ServerPlayer, Integer> server;
        final List<Shot> shots = new ArrayList<>();
        @Nullable BiConsumer<Minecraft, Integer> client;
        volatile boolean failed;

        Stop(String name, int length, BiConsumer<ServerPlayer, Integer> server) {
            this.name = name;
            this.length = length;
            this.server = server;
        }

        Stop shot(int at, String name, CameraType camera, boolean hud) {
            shots.add(new Shot(at, name, camera, hud));
            return this;
        }

        Stop client(BiConsumer<Minecraft, Integer> c) {
            client = c;
            return this;
        }
    }

    private static List<Stop> stops;
    private static int index, t;
    /** The last tick the server has run: the client waits for it (chunk generation can stall the server for seconds). */
    private static volatile int done;
    /** True while the tour runs; with {@link #bossHud}, which HUD layers it lets through. */
    private static boolean active, bossHud;
    /** Flat, dry ground for arenas, rituals and flights, found once. */
    private static final List<BlockPos> spots = new ArrayList<>();
    private static int nextSpot;
    /** What this stop put in the world, discarded when it ends. */
    private static final List<Entity> spawned = new ArrayList<>();

    private TourPreview() {
    }

    // --- the client loop ---------------------------------------------------------------------------------------------

    static boolean tick(Minecraft mc) {
        if (!NAME.equals(DevPreview.env())) return false;
        MinecraftServer server = mc.getSingleplayerServer();
        // A grabbed mouse would turn the camera whenever someone touches it while the tour runs.
        if (mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
        if (stops == null) {
            stops = plan();
            active = true;
            mc.options.fov().set(70);
            mc.options.renderDistance().set(16);
            mc.options.gamma().set(1.0);
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer p = player(server);
                for (ServerLevel level : server.getAllLevels()) {
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                }
                p.getInventory().clearContent();
            });
            return true;
        }
        if (index >= stops.size()) {
            active = false;
            DevPreview.stop(mc);
            return true;
        }
        Stop s = stops.get(index);
        if (done < t) return true;
        if (t >= s.length) {
            index++;
            t = 0;
            done = 0;
            return true;
        }
        int now = ++t;
        if (now == 1) {
            SupernaturalCraft.LOGGER.info("[tour] stop {}/{}: {}", index + 1, stops.size(), s.name);
            forgetBossFx();
        }
        server.execute(() -> {
            try {
                if (!s.failed || now >= s.length) s.server.accept(player(server), now);
            } catch (RuntimeException e) {
                SupernaturalCraft.LOGGER.error("[tour] {} failed at tick {}", s.name, now, e);
                s.failed = true;
            } finally {
                done = now;
            }
        });
        if (s.client != null && !s.failed) s.client.accept(mc, now);
        for (Shot shot : s.shots) {
            if (now == shot.at - 6) {
                mc.options.setCameraType(shot.camera);
                bossHud = shot.hud;
                mc.options.hideGui = !shot.hud;
            }
            if (now == shot.at - 2) mc.getToasts().clear();
            // A cinematic takes the camera (and Lucifer's open with title cards): that candidate is skipped.
            boolean cinematic = org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.active()
                    || org.papiricoh.supernaturalcraft.client.cinematic.ClientCinematics.playing();
            if (now == shot.at && !s.failed && !cinematic) {
                String file = String.format("tour_%02d_%s_%s.png", index + 1, s.name, shot.name);
                Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), m -> {
                });
            }
        }
        return true;
    }

    /** The client keeps some of a fight's HUD (Gabriel's studio signs, Michael's overlay…) until its end message, which a discarded boss never sends. */
    private static void forgetBossFx() {
        org.papiricoh.supernaturalcraft.client.gabriel.ClientGabriel.clear();
        org.papiricoh.supernaturalcraft.client.raphael.ClientRaphael.clear();
        org.papiricoh.supernaturalcraft.client.michael.MichaelOverlay.clear();
        org.papiricoh.supernaturalcraft.client.abaddon.ClientAbaddonFx.clear();
        org.papiricoh.supernaturalcraft.client.witchboss.ClientGrandWitchFx.clear();
        org.papiricoh.supernaturalcraft.client.witchboss.ClientRowenaFx.clear();
        org.papiricoh.supernaturalcraft.client.cain.ClientCainFx.clear();
        org.papiricoh.supernaturalcraft.client.heaven.FormHud.clear();
        org.papiricoh.supernaturalcraft.client.heaven.DocketHud.clear();
        org.papiricoh.supernaturalcraft.client.heaven.Termination.clear();
        org.papiricoh.supernaturalcraft.client.heaven.QteOverlay.clear();
        org.papiricoh.supernaturalcraft.client.heaven.TrainingTest.clear();
    }

    /** Boss shots: only the boss bars and titles; the hunter's own HUD, hand and chat stay out of the picture. */
    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!active || !bossHud) return;
        ResourceLocation id = event.getName();
        if (HIDDEN_LAYERS.contains(id)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (active) event.setCanceled(true);
    }

    private static final Set<ResourceLocation> HIDDEN_LAYERS = Set.of(VanillaGuiLayers.HOTBAR, VanillaGuiLayers.CROSSHAIR,
            VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.JUMP_METER, VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.CHAT, VanillaGuiLayers.OVERLAY_MESSAGE, VanillaGuiLayers.EFFECTS,
            VanillaGuiLayers.SPECTATOR_TOOLTIP, VanillaGuiLayers.DEMO_OVERLAY, VanillaGuiLayers.SCOREBOARD_SIDEBAR,
            SupernaturalCraft.asResource("mana"), SupernaturalCraft.asResource("consumption"), SupernaturalCraft.asResource("colt"),
            SupernaturalCraft.asResource("torment"), SupernaturalCraft.asResource("allegiance"), SupernaturalCraft.asResource("mark_thirst"),
            SupernaturalCraft.asResource("michael_flight"), SupernaturalCraft.asResource("death_clock"));

    // --- the plan ----------------------------------------------------------------------------------------------------

    /** {@code SN_TOUR=places,bosses,rituals,trade} (any of them; all by default): which parts of the tour to play. */
    private static List<Stop> plan() {
        String parts = Optional.ofNullable(System.getenv("SN_TOUR")).orElse("places,bosses,rituals,trade");
        List<Stop> places = new ArrayList<>(), bosses = new ArrayList<>(), trade = new ArrayList<>();
        places(places);
        bosses(bosses);
        trade(trade);
        List<Stop> out = new ArrayList<>();
        if (parts.contains("places")) out.addAll(places);
        if (parts.contains("bosses")) out.addAll(bosses);
        if (parts.contains("rituals")) {
            out.add(ritual("infernal_ritual", AllBlocks.BLOOD_CHALK_LINE.get().defaultBlockState(), Blocks.BLACK_CANDLE, School.INFERNAL, NIGHT));
            out.add(ritual("enochian_ritual", AllBlocks.CHALK_LINE.get().defaultBlockState(), Blocks.WHITE_CANDLE, School.ENOCHIAN, DUSK));
        }
        if (parts.contains("trade")) out.addAll(trade);
        // SN_TOUR_ONLY=amara,cain: just those stops again (after a failed or ugly shot), out of whichever parts are on.
        String only = System.getenv("SN_TOUR_ONLY");
        if (only != null) {
            Set<String> names = Set.of(only.split(","));
            out.removeIf(stop -> !names.contains(stop.name));
        }
        return out;
    }

    private static void places(List<Stop> out) {
        out.add(structure("hymnal_spire", "hymnal_spire", DUSK, 0, null, "supernatural spire place", null));
        out.add(structure("crossroads", "crossroads", NIGHT, 0, null, "supernatural crossroads place",
                (level, c) -> stand(level, AllEntities.CROSSROADS_DEMON.get(), c.add(0, 0, 0))));
        out.add(structure("graveyard", "grave", NIGHT, 0, null, "supernatural grave place",
                (level, c) -> {
                    stand(level, AllEntities.GHOST.get(), c.add(1.5, 0, 1));
                    stand(level, AllEntities.GHOST.get(), c.add(-2.5, 0, -1.5));
                }));
        out.add(structure("abandoned_church", "abandoned_church", MORNING, 1, null, "supernatural coven church place", null));
        out.add(structure("witch_coven", "witch_coven", DUSK, 0, null, "supernatural coven covenstead place",
                (level, c) -> {
                    stand(level, AllEntities.HEDGE_WITCH.get(), c.add(2, 0, 1));
                    stand(level, AllEntities.COVEN_WITCH.get(), c.add(-2, 0, 0));
                    stand(level, AllEntities.HEDGE_WITCH.get(), c.add(0, 0, -2.5));
                }));
        out.add(structure("rowena_house", "rowena_house", NOON, 0, level -> {
            RowenaSite.Site site = RowenaSite.of(level);
            return RowenaWorld.ensureNow(level, RowenaSavedData.get(level), site);
        }, null, null));
        out.add(structure("cain_farm", "cain_farm", MORNING, 0, level -> {
            CainSite.Site site = CainSite.of(level);
            return CainWorld.ensureNow(level, CainSavedData.get(level), site);
        }, null, null));
        out.add(structure("author_cabin", "author_cabin", DUSK, 0, level -> AuthorSite.of(level).origin(), null, null));
        out.add(bunker());
        out.add(hell());
        out.add(heavenPlot());
        out.add(roadhouse());
    }

    /** Bosses, roughly in the order a hunter meets them. */
    private static void bosses(List<Stop> out) {
        out.add(duel("azazel", AllEntities.AZAZEL.get(), NIGHT, 0, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelSummoning.summon(l, a, p))));
        out.add(duel("lilith", AllEntities.LILITH.get(), NIGHT, 0, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithSummoning.summon(l, a, p))));
        out.add(duel("lucifer", AllEntities.LUCIFER.get(), DUSK, 0, 12, bossAt((l, a, p) -> LuciferSummoning.summon(l, a.below(), p))));
        for (HorsemanKind kind : HorsemanKind.values()) {
            EntityType<? extends Entity> type = BuiltInRegistries.ENTITY_TYPE.get(SupernaturalCraft.asResource(kind.name().toLowerCase(java.util.Locale.ROOT)));
            out.add(duel(kind.name().toLowerCase(java.util.Locale.ROOT), type, kind == HorsemanKind.DEATH ? NIGHT : NOON, 0, 11,
                    bossAt((l, a, p) -> HorsemenSummoning.summon(kind, l, a, p))));
        }
        out.add(duel("gabriel", AllEntities.GABRIEL.get(), NIGHT, 0, 9, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielSummoning.summon(l, a, p))));
        out.add(duel("raphael", AllEntities.RAPHAEL.get(), NOON, 2, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelSummoning.summon(l, a, p))));
        out.add(duel("michael", AllEntities.MICHAEL.get(), NOON, 0, 12, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelSummoning.summon(l, a, p))));
        out.add(duel("metatron", AllEntities.METATRON.get(), NOON, 0, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronSummoning.summon(l, a, p))));
        out.add(duel("broken_chorus", AllEntities.BROKEN_CHORUS.get(), NOON, 2, 16, bossAt(ChorusSummoning::summon)));
        out.add(duel("amara", AllEntities.AMARA.get(), NOON, 0, 15, bossAt((l, a, p) -> {
            Eclipses.end(l);
            Eclipses.begin(l, a, 24000);
            AmaraSummoning.summon(l, a, p);
        })));
        out.add(duel("lucifer_uncaged", AllEntities.LUCIFER_UNCAGED.get(), NOON, 0, 9, TourPreview::cage));
        out.add(duel("cain", AllEntities.CAIN.get(), MORNING, 0, 10, p -> {
            ServerLevel level = p.server.overworld();
            CainSite.Site site = CainSite.of(level);
            CainWorld.ensureNow(level, CainSavedData.get(level), site);
            BlockPos centre = CainWorld.arenaCentre(site);
            return new Arena(level, centre, (l, a, pl) -> CainWorld.summonNear(l, a, pl));
        }));
        out.add(duel("abaddon", AllEntities.ABADDON.get(), NIGHT, 0, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonSummoning.summon(l, a, p))));
        out.add(duel("rowena", AllEntities.ROWENA.get(), DUSK, 0, 10, p -> {
            ServerLevel level = p.server.overworld();
            RowenaSite.Site site = RowenaSite.of(level);
            RowenaWorld.ensureNow(level, RowenaSavedData.get(level), site);
            BlockPos centre = RowenaWorld.arenaCentre(site);
            return new Arena(level, centre, (l, a, pl) -> org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaSummoning.summon(l, a, pl));
        }));
        out.add(duel("grand_witch", AllEntities.GRAND_WITCH.get(), NIGHT, 0, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchSummoning.summon(l, a, p))));
        out.add(duel("naomi", AllEntities.NAOMI.get(), NOON, 0, 9, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiSummoning.summon(l, a, p))));
        out.add(duel("zachariah", AllEntities.ZACHARIAH.get(), NOON, 0, 9, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahSummoning.summon(l, a, p))));
        out.add(duel("chuck", AllEntities.CHUCK.get(), DUSK, 0, 11, bossAt((l, a, p) ->
                org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckSummoning.summon(l, a, p, false))));
    }

    /** The hunter's trade. */
    private static void trade(List<Stop> out) {
        out.add(hunt());
        out.add(colt());
        out.add(flight());
    }

    // --- structures --------------------------------------------------------------------------------------------------

    private interface Extras {
        void spawn(ServerLevel level, Vec3 centre);
    }

    /**
     * A structure the world generated: the nearest one ({@code hidden} null) or the one at a site the mod picks per world;
     * if there is none, {@code fallback} builds one on flat ground. Three shots: two wide diagonals and a low close one.
     */
    private static Stop structure(String name, String id, int time, int weather, @Nullable Function<ServerLevel, BlockPos> hidden,
                                  @Nullable String fallback, @Nullable Extras extras) {
        Vec3[][] views = new Vec3[3][];
        Stop s = new Stop(name, 570, (p, now) -> {
            ServerLevel level = p.server.overworld();
            if (now == 1) {
                clean(level);
                spectator(p);
                sky(level, time, weather);
                BoundingBox box = locate(level, id, hidden, p);
                if (box == null && fallback != null) {
                    BlockPos at = spot(level);
                    p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
                    command(p, fallback);
                    box = BoundingBox.fromCorners(at.offset(-12, 0, -12), at.offset(12, 14, 12));
                }
                if (box == null) throw new IllegalStateException("no " + id + " in this world");
                frame(level, box, views);
                warm(level, views[2][1], 9);
                if (extras != null) extras.spawn(level, views[2][1].multiply(1, 0, 1).add(0, ground(level, views[2][1]), 0));
                view(p, level, views[0][0], views[0][1]);
            }
            if (now == 300) view(p, level, views[1][0], views[1][1]);
            if (now == 430) view(p, level, views[2][0], views[2][1]);
            if (now == 570) discardSpawned();
        });
        return s.shot(290, "wide", FIRST, false).shot(420, "other_side", FIRST, false).shot(560, "close", FIRST, false);
    }

    /** The structure's box: from its start near {@code hidden}'s site, or the nearest one found like {@code /locate}. */
    private static @Nullable BoundingBox locate(ServerLevel level, String id, @Nullable Function<ServerLevel, BlockPos> hidden, ServerPlayer p) {
        var key = ResourceKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource(id));
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
        if (holder.isEmpty()) return null;
        BlockPos anchor;
        if (hidden != null) {
            anchor = hidden.apply(level);
        } else {
            Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.direct(holder.get()), level.getSharedSpawnPos(), 100, false);
            if (found == null) return null;
            anchor = found.getFirst();
        }
        Structure structure = holder.get().value();
        int cx = anchor.getX() >> 4, cz = anchor.getZ() >> 4;
        for (int r = 0; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    ChunkAccess chunk = level.getChunk(cx + dx, cz + dz, ChunkStatus.STRUCTURE_STARTS, true);
                    StructureStart start = chunk == null ? null : chunk.getStartForStructure(structure);
                    if (start != null && start.isValid()) return start.getBoundingBox();
                }
            }
        }
        // A site with no start of its own (built by hand): a box around the anchor.
        return hidden != null ? BoundingBox.fromCorners(anchor.offset(-16, -2, -16), anchor.offset(16, 14, 16)) : null;
    }

    /** Three views of a box: {from, at} wide from one corner, wide from the opposite one, low and close. */
    private static void frame(ServerLevel level, BoundingBox box, Vec3[][] views) {
        double cx = (box.minX() + box.maxX() + 1) / 2.0, cz = (box.minZ() + box.maxZ() + 1) / 2.0;
        int span = Math.max(box.getXSpan(), box.getZSpan());
        // The terrain around it (the box can reach well below ground, the heightmap at its centre lands on its roof).
        double terrain = Math.min(Math.min(ground(level, box.minX() - 3, box.minZ() - 3), ground(level, box.maxX() + 3, box.maxZ() + 3)),
                Math.min(ground(level, box.minX() - 3, box.maxZ() + 3), ground(level, box.maxX() + 3, box.minZ() - 3)));
        double tall = Math.max(6, box.maxY() + 1 - terrain);
        Vec3 look = new Vec3(cx, terrain + tall * 0.4, cz);
        double d = Math.max(span, tall) * 0.8 + 12;
        views[0] = new Vec3[]{above(level, look.add(d * 0.75, tall * 0.35 + d * 0.3, d * 0.65)), look};
        views[1] = new Vec3[]{above(level, look.add(-d * 0.7, tall * 0.25 + d * 0.22, -d * 0.7)), look};
        Vec3 near = new Vec3(cx, terrain, cz);
        views[2] = new Vec3[]{above(level, near.add(-d * 0.45, 2.2, d * 0.5)), near.add(0, tall * 0.35, 0)};
        SupernaturalCraft.LOGGER.info("[tour] box {} terrain {} tall {} views {} / {} / {}", box, terrain, tall,
                views[0][0], views[1][0], views[2][0]);
    }

    /** The bunker of the Men of Letters: its hill, then the War Table, the war room, the library and the garage. */
    private static Stop bunker() {
        String[] rooms = {"map_table", "war_room", "library", "garage"};
        BlockPos[] origin = new BlockPos[1];
        int[] quarter = new int[1];
        Stop s = new Stop("bunker", 200 + rooms.length * 170, (p, now) -> {
            ServerLevel level = p.server.overworld();
            if (now == 1) {
                clean(level);
                spectator(p);
                sky(level, MORNING, 0);
                var site = BunkerLocator.of(level);
                origin[0] = site.origin();
                quarter[0] = site.rotation();
                Vec3 o = Vec3.atBottomCenterOf(BunkerBuilder.at(origin[0], quarter[0], BunkerLayout.OUTSIDE));
                warm(level, o, 9);
                view(p, level, above(level, o.add(-14, 9, 16)), o.add(0, 3, 0));
            }
            for (int i = 0; i < rooms.length; i++) {
                if (now == 200 + i * 170) {
                    var v = BunkerLayout.views().get(rooms[i]);
                    if (v == null) continue;
                    Vec3 feet = Vec3.atBottomCenterOf(BunkerBuilder.at(origin[0], quarter[0], new int[]{v.x(), v.y(), v.z()}));
                    Vec3 look = Vec3.atCenterOf(BunkerBuilder.at(origin[0], quarter[0],
                            new int[]{(int) Math.floor(v.lx()), (int) Math.floor(v.ly()), (int) Math.floor(v.lz())}));
                    view(p, level, feet.add(0, 1.62, 0), look);
                }
            }
        });
        s.shot(190, "hill", FIRST, false);
        for (int i = 0; i < rooms.length; i++) s.shot(200 + i * 170 + 160, rooms[i], FIRST, false);
        return s;
    }

    /** Hell: the pit, then the Cage hanging in it. */
    private static Stop hell() {
        return new Stop("hell", 420, (p, now) -> {
            ServerLevel hell = p.server.getLevel(HellDimension.LEVEL);
            if (hell == null) throw new IllegalStateException("no Hell in this world");
            if (now == 1) {
                spectator(p);
                warm(hell, Vec3.ZERO, 7);
                CageBuilder.build(hell, CageBuilder.extent(), true);
                view(p, hell, new Vec3(86, 103, 0.5), new Vec3(0, 118, 0));
            }
            if (now == 210) view(p, hell, new Vec3(34, 76, 34), new Vec3(0, 92, 0));
        }).shot(200, "pit", FIRST, false).shot(410, "cage", FIRST, false);
    }

    /** A hunter's own Heaven, from the landing and from the air. */
    private static Stop heavenPlot() {
        Vec3[] centre = new Vec3[1];
        HeavenPlot[] plot = new HeavenPlot[1];
        return new Stop("heaven", 420, (p, now) -> {
            ServerLevel level = HeavenPlots.level(p.server);
            if (now == 1) {
                spectator(p);
                plot[0] = HeavenPlots.ensure(level, p);
                HeavenPlots.writeNow(level, plot[0]);
                centre[0] = Vec3.atBottomCenterOf(HeavenPlots.origin(level, plot[0]));
                Vec3 land = Vec3.atBottomCenterOf(HeavenPlots.at(level, plot[0], HeavenPlotLayout.LANDING));
                view(p, level, land.add(6, 5.6, 8), land.add(0, 1, -12));
            }
            if (now == 210) view(p, level, centre[0].add(70, 70, 70), centre[0]);
        }).shot(200, "landing", FIRST, false).shot(410, "overview", FIRST, false);
    }

    /** Ash's Roadhouse in Heaven, outside and with Ash behind the bar. */
    private static Stop roadhouse() {
        Vec3[] centre = new Vec3[1];
        Entity[] ash = new Entity[1];
        return new Stop("roadhouse", 420, (p, now) -> {
            ServerLevel level = HeavenPlots.level(p.server);
            if (now == 1) {
                spectator(p);
                HeavenPlot hub = HeavenPlots.ensureHub(level);
                HeavenPlots.writeNow(level, hub);
                ash[0] = Roadhouse.ensureAsh(level, hub);
                centre[0] = Vec3.atBottomCenterOf(HeavenPlots.origin(level, hub));
                Vec3 land = Vec3.atBottomCenterOf(HeavenPlots.at(level, hub, RoadhouseLayout.HUB_LANDING));
                view(p, level, land.add(10, 9.6, 18), centre[0].add(0, 3, 0));
            }
            if (now == 210 && ash[0] != null) view(p, level, ash[0].position().add(0, 1.8, 3), ash[0].position().add(0, 1.4, 0));
        }).shot(200, "outside", FIRST, false).shot(410, "ash", FIRST, false);
    }

    // --- bosses ------------------------------------------------------------------------------------------------------

    private interface Summon {
        void summon(ServerLevel level, BlockPos at, ServerPlayer p);
    }

    private record Arena(ServerLevel level, BlockPos at, Summon summon) {
    }

    /** A boss on the next flat spot of the Overworld. */
    private static Function<ServerPlayer, Arena> bossAt(Summon summon) {
        return p -> {
            ServerLevel level = p.server.overworld();
            return new Arena(level, spot(level), summon);
        };
    }

    /** Lucifer walks free: the Cage in Hell, opened. */
    private static Arena cage(ServerPlayer p) {
        ServerLevel hell = p.server.getLevel(HellDimension.LEVEL);
        if (hell == null) throw new IllegalStateException("no Hell in this world");
        CageBuilder.build(hell, CageBuilder.extent(), false);
        CageController.get(hell).set(hell, false);
        return new Arena(hell, CageLayout.ALTAR, (l, a, pl) -> org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedSummoning.summon(l, a, pl));
    }

    /**
     * A real fight: the hunter (invulnerable) stands near the boss and keeps it in view; the boss is pushed into its last
     * phase and photographed through the hunter's eyes.
     */
    private static Stop duel(String name, EntityType<? extends Entity> type, int time, int weather, double reach,
                             Function<ServerPlayer, Arena> where) {
        Arena[] arena = new Arena[1];
        Entity[] boss = new Entity[1];
        double distance = Math.max(6, reach * 0.7);
        Stop s = new Stop(name, 700, (p, now) -> {
            if (now == 1) {
                for (ServerLevel l : p.server.getAllLevels()) clean(l);
                arena[0] = where.apply(p);
                boss[0] = null;
                ServerLevel level = arena[0].level;
                warm(level, Vec3.atCenterOf(arena[0].at), 8);
                sky(level, time, weather);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.getAbilities().mayfly = true;
                // Under a ceiling (Hell) the hunter hovers: the pit below is a long way down.
                p.getAbilities().flying = level.dimensionType().hasCeiling();
                p.onUpdateAbilities();
                Vec3 c = Vec3.atBottomCenterOf(arena[0].at);
                p.teleportTo(level, c.x, c.y + 0.5, c.z - distance, 0, 5);
            }
            if (arena[0] == null) return;
            ServerLevel level = arena[0].level;
            if (now == 80) arena[0].summon.summon(level, arena[0].at, p);
            if (now >= 80 && now < 120 && boss[0] == null) {
                List<? extends Entity> found = level.getEntities(type, new AABB(arena[0].at).inflate(64), e -> true);
                if (!found.isEmpty()) boss[0] = found.getFirst();
            }
            if (now == 700) {
                if (boss[0] != null) boss[0].discard();
                clean(level);
                Eclipses.end(level);
                sky(level, NOON, 0);
                return;
            }
            Entity b = boss[0];
            if (b == null || b.isRemoved()) return;
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(20);
            if (now % 5 == 0) follow(p, b, distance);
            if (b instanceof LuciferEntity l && now == 200 && !FIRST_PHASE.contains(name) && l.maxPhase() > 1 && l.phase() < l.maxPhase()) {
                l.beginTransition(l.maxPhase());
            }
        });
        // Candidates through the hunter's eyes; the best one per boss is picked afterwards.
        for (int i = 0; i < 5; i++) s.shot(380 + i * 70, "fight_" + (char) ('a' + i), FIRST, true);
        return s;
    }

    /** Bosses photographed in their first phase: Naomi's last one leaves her out of sight. */
    private static final Set<String> FIRST_PHASE = Set.of("naomi");

    /** Keeps the hunter in range of the boss (charges and knockbacks throw them about), looking straight at it. */
    private static void follow(ServerPlayer p, Entity boss, double distance) {
        Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
        if (to.length() > distance * 1.5 || to.length() < distance * 0.4 || Math.abs(boss.getY() - p.getY()) > 12) {
            Vec3 d = to.lengthSqr() < 1e-3 ? new Vec3(0, 0, 1) : to.normalize();
            double x = boss.getX() - d.x * distance, z = boss.getZ() - d.z * distance;
            double y = p.serverLevel().dimensionType().hasCeiling() ? boss.getY()
                    : Math.max(boss.getY(), ground(p.serverLevel(), new Vec3(x, 0, z)));
            p.teleportTo(p.serverLevel(), x, y, z, p.getYRot(), p.getXRot());
            to = boss.position().subtract(p.position()).multiply(1, 0, 1);
        }
        p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(0, boss.getBbHeight() * 0.55, 0));
    }

    // --- the hunter's trade ------------------------------------------------------------------------------------------

    private static final List<String> CIRCLE = List.of("  #####  ", " #  c  # ", "# c   c #", "#       #", "#   A   #", "#       #",
            "# c   c #", " #  c  # ", "  #####  ");

    /** A ritual circle: chalk, lit candles, the altar charging in its school's colour. */
    private static Stop ritual(String name, net.minecraft.world.level.block.state.BlockState chalk, net.minecraft.world.level.block.Block candle,
                               School school, int time) {
        BlockPos[] at = new BlockPos[1];
        return new Stop(name, 330, (p, now) -> {
            ServerLevel level = p.server.overworld();
            if (now == 1) {
                clean(level);
                spectator(p);
                sky(level, time, 0);
                at[0] = spot(level);
                BlockPos c = at[0];
                // A level floor for the circle: the ground's own top block, and nothing standing on it.
                var floor = level.getBlockState(c.below());
                if (floor.isAir() || !floor.getFluidState().isEmpty()) floor = Blocks.GRASS_BLOCK.defaultBlockState();
                for (int dx = -7; dx <= 7; dx++) {
                    for (int dz = -7; dz <= 7; dz++) {
                        level.setBlockAndUpdate(c.offset(dx, -2, dz), Blocks.DIRT.defaultBlockState());
                        level.setBlockAndUpdate(c.offset(dx, -1, dz), floor);
                        for (int dy = 0; dy < 5; dy++) level.setBlockAndUpdate(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                }
                level.setBlockAndUpdate(c, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
                for (PatternGeometry.Cell cell : PatternGeometry.cells(CIRCLE)) {
                    BlockPos b = c.offset(cell.dx(), 0, cell.dz());
                    level.setBlockAndUpdate(b.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    level.setBlockAndUpdate(b, cell.symbol() == 'c' ? candle.defaultBlockState()
                            .setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true) : chalk);
                }
                Vec3 v = Vec3.atBottomCenterOf(c);
                view(p, level, v.add(-5, 4.2, 5.5), v.add(0, 0.4, 0));
            }
            if (now == 330) {
                for (PatternGeometry.Cell cell : PatternGeometry.cells(CIRCLE)) level.removeBlock(at[0].offset(cell.dx(), 0, cell.dz()), false);
                level.removeBlock(at[0], false);
            }
        }).client((mc, now) -> {
            if (now == 60 && at[0] != null) {
                List<Short> cells = new ArrayList<>();
                for (PatternGeometry.Cell c : PatternGeometry.cells(CIRCLE)) cells.add(RitualChargePayload.pack(c.dx(), c.dz()));
                ClientMagicFx.handleCharge(new RitualChargePayload(at[0], school, cells, 600, 0.3f));
            }
            if (now == 329 && at[0] != null) ClientMagicFx.handleCharge(RitualChargePayload.stop(at[0]));
        }).shot(300, "circle", FIRST, false);
    }

    /** The hunt: a demon held in a devil's trap, another behind a salt line, and a sigil spell on its way. */
    private static Stop hunt() {
        BlockPos[] at = new BlockPos[1];
        Spell lance = new Spell(Optional.of(SupernaturalCraft.asResource("bolt")),
                List.of(SupernaturalCraft.asResource("smite"), SupernaturalCraft.asResource("hellfire")),
                List.of(SupernaturalCraft.asResource("empower")), "Holy Lance");
        Stop s = new Stop("hunt", 420, (p, now) -> {
            ServerLevel level = p.server.overworld();
            if (now == 1) {
                clean(level);
                sky(level, DUSK, 0);
                at[0] = spot(level);
                BlockPos c = at[0];
                for (int part = 0; part < 9; part++) {
                    level.setBlockAndUpdate(c.offset(DevilsTrapBlock.dx(part), 0, 6 + DevilsTrapBlock.dz(part)),
                            AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(DevilsTrapBlock.PART, part));
                }
                for (int x = -4; x <= 4; x++) level.setBlockAndUpdate(c.offset(x, 0, 3), AllBlocks.SALT_LINE.get().defaultBlockState());
                Vec3 v = Vec3.atBottomCenterOf(c);
                Mob trapped = stand(level, AllEntities.BLACK_EYED_DEMON.get(), v.add(0, 0, 6));
                Mob other = stand(level, AllEntities.DEMON_OCCULTIST.get(), v.add(3.5, 0, 5));
                Mob third = stand(level, AllEntities.BLACK_EYED_DEMON.get(), v.add(-3.5, 0, 4.5));
                for (Mob m : List.of(trapped, other, third)) m.lookAt(EntityAnchorArgument.Anchor.EYES, v.add(0, 1.6, 0));
                p.setGameMode(GameType.CREATIVE);
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllItems.GRIMOIRE.get()));
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AllItems.ANGEL_BLADE.get()));
                p.teleportTo(level, v.x, v.y, v.z, 0, 8);
                p.lookAt(EntityAnchorArgument.Anchor.EYES, v.add(0.4, 1.4, 6));
            }
            if (now >= 150 && now % 60 == 30 && now < 400) {
                p.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(at[0]).add(0.4, 1.3, 6));
                SpellCaster.cast(p, lance, 1f);
            }
            if (now == 420) {
                for (int part = 0; part < 9; part++) level.removeBlock(at[0].offset(DevilsTrapBlock.dx(part), 0, 6 + DevilsTrapBlock.dz(part)), false);
                for (int x = -4; x <= 4; x++) level.removeBlock(at[0].offset(x, 0, 3), false);
                discardSpawned();
                p.getInventory().clearContent();
            }
        });
        return s.shot(120, "trap", BACK, false).shot(213, "spell_1", BACK, false).shot(276, "spell_2", BACK, false)
                .shot(337, "spell_front", FRONT, false).shot(395, "spell_3", BACK, false);
    }

    /** The Colt, in hand, before a demon. */
    private static Stop colt() {
        return new Stop("colt", 260, (p, now) -> {
            ServerLevel level = p.server.overworld();
            if (now == 1) {
                clean(level);
                sky(level, DUSK, 0);
                BlockPos c = spot(level);
                Vec3 v = Vec3.atBottomCenterOf(c);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                p.getInventory().selected = 0;
                p.getInventory().setItem(0, new ItemStack(AllItems.THE_COLT.get()));
                p.teleportTo(level, v.x, v.y, v.z, 0, 0);
                stand(level, AllEntities.BLACK_EYED_DEMON.get(), v.add(0, 0, 7)).lookAt(EntityAnchorArgument.Anchor.EYES, v.add(0, 1.6, 0));
                p.lookAt(EntityAnchorArgument.Anchor.EYES, v.add(0, 1.5, 7));
            }
            if (now == 260) {
                discardSpawned();
                p.getInventory().clearContent();
            }
        }).shot(150, "front", FRONT, false).shot(250, "shoulder", BACK, false);
    }

    /** An angel's wings over the land: the hunter flies off, seen from behind and from the side. */
    private static Stop flight() {
        return new Stop("flight", 320, (p, now) -> {
            ServerLevel level = p.server.overworld();
            if (now == 1) {
                clean(level);
                sky(level, MORNING, 0);
                Allegiance a = Allegiance.HUMAN.convert(Faction.ANGEL).withRank(2);
                Allegiances.set(p, a.withEssence(a.maxEssence()));
                BlockPos c = spot(level);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.teleportTo(level, c.getX() + 0.5, c.getY() + 18, c.getZ() + 0.5, -90, 10);
            }
            if (now == 320) {
                Allegiances.set(p, Allegiance.HUMAN);
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
        }).client((mc, now) -> {
            if (mc.player == null || now < 60 || now >= 318) return;
            // Looking along the flight, then off to its side so the camera behind sees the wings side on.
            boolean side = now > 200;
            mc.player.setYRot(side ? 0 : -90);
            mc.player.setXRot(10);
            mc.player.setDeltaMovement(new Vec3(0.6, 0.02, 0));
        }).shot(170, "behind", BACK, false).shot(300, "side", BACK, false);
    }

    // --- helpers -----------------------------------------------------------------------------------------------------

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void spectator(ServerPlayer p) {
        p.setGameMode(GameType.SPECTATOR);
    }

    /** 0 clear, 1 rain, 2 thunder. */
    private static void sky(ServerLevel level, int time, int weather) {
        level.setDayTime(time);
        if (weather == 0) level.setWeatherParameters(24000, 0, false, false);
        else level.setWeatherParameters(0, 24000, true, weather == 2);
    }

    /** Every arena in this level put back, and whatever the last fight left lying about. */
    private static void clean(ServerLevel level) {
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        discardSpawned();
        for (Player pl : level.players()) {
            for (Entity e : level.getEntities((Entity) null, pl.getBoundingBox().inflate(96), e -> !(e instanceof Player)
                    && (e instanceof ItemEntity || e instanceof ExperienceOrb
                    || BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(SupernaturalCraft.MODID)))) {
                e.discard();
            }
        }
    }

    private static void discardSpawned() {
        spawned.forEach(Entity::discard);
        spawned.clear();
    }

    private static void command(ServerPlayer p, String cmd) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
    }

    /** A still mob standing on the ground at {@code at}. */
    private static Mob stand(ServerLevel level, EntityType<? extends Mob> type, Vec3 at) {
        Mob m = type.create(level);
        if (m == null) throw new IllegalStateException("cannot create " + type);
        double y = ground(level, at);
        m.moveTo(at.x, y, at.z, 180, 0);
        m.setNoAi(true);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
        spawned.add(m);
        return m;
    }

    /** Places the camera (an eye at {@code from}) looking at {@code at}, in any dimension. */
    private static void view(ServerPlayer p, ServerLevel level, Vec3 from, Vec3 at) {
        double dx = at.x - from.x, dy = at.y - from.y, dz = at.z - from.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, from.x, from.y - p.getEyeHeight(), from.z, yaw, pitch);
    }

    private static double ground(ServerLevel level, Vec3 at) {
        return ground(level, (int) Math.floor(at.x), (int) Math.floor(at.z));
    }

    private static int ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /**
     * Generates the chunks around {@code at} now, on the server thread: a far teleport otherwise leaves the client looking at
     * the edge of the loaded world when the shot is taken.
     */
    private static void warm(ServerLevel level, Vec3 at, int radius) {
        int cx = (int) Math.floor(at.x) >> 4, cz = (int) Math.floor(at.z) >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) level.getChunk(cx + dx, cz + dz);
        }
    }

    /** {@code eye} lifted clear of the ground under it, so the camera never sits inside a hill. */
    private static Vec3 above(ServerLevel level, Vec3 eye) {
        return new Vec3(eye.x, Math.max(eye.y, ground(level, eye) + 3), eye.z);
    }

    /**
     * The next flat, open spot (the ground block above which an arena or a scene stands), found once and handed out in turn.
     * The search only asks the generator (biome and terrain noise), so it generates no chunks.
     */
    private static BlockPos spot(ServerLevel level) {
        if (spots.isEmpty()) {
            BlockPos spawn = level.getSharedSpawnPos();
            for (int i = 0; i < 1500 && spots.size() < 5; i++) {
                double a = i * 2.39996, r = 400 + i * 2;
                int x = spawn.getX() + (int) (Math.cos(a) * r), z = spawn.getZ() + (int) (Math.sin(a) * r);
                if (!flat(level, x, z)) continue;
                BlockPos found = new BlockPos(x, ground(level, x, z), z);
                if (spots.stream().anyMatch(s -> s.distSqr(found) < 240 * 240)) continue;
                spots.add(found);
            }
            if (spots.isEmpty()) spots.add(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
            SupernaturalCraft.LOGGER.info("[tour] flat spots: {}", spots);
        }
        return spots.get(nextSpot++ % spots.size());
    }

    private static final Set<ResourceKey<net.minecraft.world.level.biome.Biome>> OPEN = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS,
            Biomes.MEADOW, Biomes.SAVANNA, Biomes.DESERT, Biomes.SNOWY_PLAINS);

    /** An open biome (no forest), dry land, and terrain within 3 blocks of the centre's height on rings out to 26. */
    private static boolean flat(ServerLevel level, int x, int z) {
        var generator = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int y0 = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
        if (y0 <= level.getSeaLevel()) return false;
        var biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y0), QuartPos.fromBlock(z), random.sampler());
        if (OPEN.stream().noneMatch(biome::is)) return false;
        for (int r : new int[]{8, 16, 26}) {
            for (int k = 0; k < 12; k++) {
                double a = k * Math.PI / 6;
                int px = x + (int) (Math.cos(a) * r), pz = z + (int) (Math.sin(a) * r);
                int top = generator.getBaseHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                int floor = generator.getBaseHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                if (floor < top || Math.abs(top - y0) > 3) return false;
            }
        }
        return true;
    }
}
