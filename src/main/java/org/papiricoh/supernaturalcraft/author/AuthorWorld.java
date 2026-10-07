package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.datagen.SNStructures;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.network.AuthorDialoguePayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMapDecorations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The Author's world on the server: making sure his cabin stands where the world says it does, putting him at his desk
 * once he has been found, the page in his typewriter, and the map that leads to him.
 */
public final class AuthorWorld {

    /** He sits at his desk while a hunter is this close to the cabin (and "Find the Author" has been cast). */
    public static final double NPC_RANGE = 48;
    /** The cabin is checked (and built, if the world never generated it) when a hunter comes this close. */
    public static final double CABIN_RANGE = 96;
    /** Where he stands to talk, beside his chair (local to the cabin). */
    static final int[] STAND = {0, 1, -2};
    /** The map's tint and name. */
    public static final int MAP_COLOR = 0x2B2B2B;
    public static final String MAP_NAME = "item.supernaturalcraft.author_map";
    /** The typewriter's page: no speaker, the bosses read so far as options, {@code rematch} = whether he is expected. */
    public static final int PAGE_NPC = -1;
    public static final String PAGE_NODE = "page";

    private AuthorWorld() {
    }

    // --- places ---------------------------------------------------------------------------------

    public static BlockPos chair(AuthorSite.Site site) {
        return CabinBuilder.at(site.origin(), site.rotation(), CabinLayout.CHAIR);
    }

    public static BlockPos typewriter(AuthorSite.Site site) {
        return CabinBuilder.at(site.origin(), site.rotation(), CabinLayout.TYPEWRITER);
    }

    /** The middle of the room: where the arena is centred and where he stands up as the test begins. */
    public static BlockPos centre(AuthorSite.Site site) {
        return CabinBuilder.at(site.origin(), site.rotation(), CabinLayout.CENTRE);
    }

    public static BlockPos stand(AuthorSite.Site site) {
        return CabinBuilder.at(site.origin(), site.rotation(), STAND);
    }

    /** The yaw of someone at his chair looking at the desk (local north). */
    public static float deskYaw(int rotation) {
        int[] d = CabinLayout.rotate(0, -1, rotation);
        return (float) Math.toDegrees(Math.atan2(-d[0], d[1]));
    }

    /** The cabin as {@link AuthorSavedData} knows it, if it does. */
    public static AuthorSite.Site site(ServerLevel level) {
        return AuthorSite.of(level);
    }

    // --- every second -------------------------------------------------------------------------------

    /** Once a second in the overworld. {@code players} are explicit so tests can pass their own. */
    public static void tick(ServerLevel level, List<? extends Player> players) {
        if (players.isEmpty()) return;
        AuthorSavedData data = AuthorSavedData.get(level);
        if (data.cabin() == null && !data.spellCast()) {
            // Nobody needs the site yet unless someone wanders out to the ring: don't sample the world for nothing.
            boolean onRing = players.stream().anyMatch(p -> {
                double d = Math.hypot(p.getX(), p.getZ());
                return d > CabinSite.MIN - CABIN_RANGE * 2 && d < CabinSite.MAX + CABIN_RANGE * 2;
            });
            if (!onRing) return;
        }
        AuthorSite.Site site = AuthorSite.of(level);
        ensureCabin(level, data, site, players);
        tickNpc(level, data, AuthorSite.of(level), players);
    }

    /** If a hunter is near and the cabin is not known to stand, find it (worldgen) or build it (older worlds). */
    public static void ensureCabin(ServerLevel level, AuthorSavedData data, AuthorSite.Site site, List<? extends Player> players) {
        if (data.built()) return;
        Vec3 c = Vec3.atCenterOf(site.origin());
        boolean near = players.stream().anyMatch(p -> horizontal(p.position(), c) < CABIN_RANGE);
        if (!near) return;
        BlockPos tw = typewriter(site);
        BoundingBox2 box = new BoundingBox2(CabinBuilder.box(site.origin(), site.rotation()));
        if (!box.loaded(level)) return;
        if (level.getBlockState(tw).is(AllBlocks.TYPEWRITER.get())) {
            data.setCabin(site.origin(), site.rotation(), true);
            return;
        }
        // The world generator's start, if it made one, knows exactly where the cabin went.
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SNStructures.AUTHOR_CABIN);
        if (structure != null) {
            StructureStart start = level.getChunk(site.origin()).getStartForStructure(structure);
            if (start != null && start.isValid()) {
                for (StructurePiece piece : start.getPieces()) {
                    if (piece instanceof CabinPiece cp) {
                        data.setCabin(cp.origin(), cp.rotation(), true);
                        return;
                    }
                }
            }
        }
        // An older world (or one without structures): the Author moves in now.
        BlockPos origin = CabinBuilder.originOn(level, site.origin().getX(), site.origin().getZ());
        CabinBuilder.placeAt(level, origin, site.rotation());
        data.setCabin(origin, site.rotation(), true);
        SupernaturalCraft.LOGGER.info("The Author's cabin was built at {}", origin.toShortString());
    }

    /** He is at his desk while hunters are near, after the spell, outside any fight; never two of him. */
    public static void tickNpc(ServerLevel level, AuthorSavedData data, AuthorSite.Site site, List<? extends Player> players) {
        if (!data.spellCast() || !data.built()) return;
        BlockPos chair = chair(site);
        Vec3 c = Vec3.atBottomCenterOf(centre(site));
        List<AuthorNpcEntity> npcs = level.getEntitiesOfClass(AuthorNpcEntity.class, new AABB(chair).inflate(64));
        if (fightAt(level, c)) {
            for (AuthorNpcEntity n : npcs) n.discard();
            return;
        }
        for (int i = 1; i < npcs.size(); i++) npcs.get(i).discard();
        if (!npcs.isEmpty()) return;
        boolean near = players.stream().anyMatch(p -> p.isAlive() && !p.isSpectator() && p.position().distanceTo(c) < NPC_RANGE);
        if (!near || !level.isPositionEntityTicking(chair)) return;
        spawnNpc(level, site);
    }

    public static AuthorNpcEntity spawnNpc(ServerLevel level, AuthorSite.Site site) {
        AuthorNpcEntity npc = AllEntities.AUTHOR_NPC.get().create(level);
        if (npc == null) return null;
        npc.home(chair(site), stand(site), deskYaw(site.rotation()));
        level.addFreshEntity(npc);
        return npc;
    }

    /** Whether his test is being fought (or its arena is still being put back) around {@code at}. */
    public static boolean fightAt(ServerLevel level, Vec3 at) {
        for (ArenaController a : ArenaSavedData.get(level).all()) {
            if (a.status() != ArenaController.Status.CLOSED && a.horizontalDistance(at) <= a.radius() + 16) return true;
        }
        return !level.getEntitiesOfClass(ChuckEntity.class, new AABB(BlockPos.containing(at)).inflate(128)).isEmpty();
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    /** Whether all the chunks of a box are loaded (nothing gets generated to answer). */
    private record BoundingBox2(net.minecraft.world.level.levelgen.structure.BoundingBox box) {
        boolean loaded(ServerLevel level) {
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) if (!level.hasChunk(cx, cz)) return false;
            }
            return true;
        }
    }

    // --- the hunter's progress ------------------------------------------------------------------------

    /** Whether {@code player} has done the mod's advancement {@code path} (e.g. {@code main/dawn}). */
    public static boolean done(ServerPlayer player, String path) {
        if (player.getServer() == null) return false;
        AdvancementHolder adv = player.getServer().getAdvancements().get(SupernaturalCraft.asResource(path));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    public static Predicate<String> beaten(ServerPlayer player) {
        return path -> done(player, path);
    }

    /** Every great enemy before him beaten: what "Find the Author" asks. */
    public static boolean readyForHim(ServerPlayer player) {
        return BossProgression.allBeforeChuck(beaten(player));
    }

    /** The bosses {@code player} has beaten, by id ({@link BossProgression.Boss#id()}), in the order of the road. */
    public static List<String> beatenIds(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) if (done(player, b.advancement)) out.add(b.id());
        return out;
    }

    // --- the page and the map -------------------------------------------------------------------------

    /** The half-typed page in the typewriter: what the reader has done so far, and a hint of what comes. */
    public static void readPage(ServerPlayer player) {
        boolean expected = AuthorSavedData.get(player.serverLevel()).spellCast();
        PacketDistributor.sendToPlayer(player, new AuthorDialoguePayload(PAGE_NPC, PAGE_NODE, beatenIds(player), expected));
    }

    /** An explorer's map of the cabin, marked with his sign. */
    public static ItemStack map(ServerLevel level, AuthorSite.Site site) {
        BlockPos target = centre(site);
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, "author", AllMapDecorations.AUTHOR_CABIN);
        map.set(DataComponents.ITEM_NAME, Component.translatable(MAP_NAME));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(MAP_COLOR));
        return map;
    }

    /** The empty map JEI and the journal show. */
    public static ItemStack displayMap() {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.ITEM_NAME, Component.translatable(MAP_NAME));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(MAP_COLOR));
        return map;
    }

    static Component gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }
}
