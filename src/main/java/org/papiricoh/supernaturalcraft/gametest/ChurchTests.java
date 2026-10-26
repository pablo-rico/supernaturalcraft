package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.ConsecratedGround;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.coven.church.ChurchPiece;
import org.papiricoh.supernaturalcraft.coven.church.ChurchStructure;
import org.papiricoh.supernaturalcraft.coven.church.ConsecrateChurchEffect;
import org.papiricoh.supernaturalcraft.coven.church.Sanctuaries;
import org.papiricoh.supernaturalcraft.coven.layout.ChurchLayout;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The abandoned church (v0.21 P3): it builds with its altar spot, crypt and loot; the rite Consecrate Church works only on the
 * ground of a real structure start (a church built by hand or by {@code /place structure} registers none), once per church —
 * the first rite leaves an Enochian page and grants {@code main/consecrated_church} — and leaves a sanctuary that drives
 * demons and spirits out. Each test registers its church's start by hand and takes it back (with its sanctuary and holy
 * ground) when it ends, so no later test at the same spot finds them.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class ChurchTests {

    /** The church's origin in the ARENA template (its floor centre). */
    private static final BlockPos ORIGIN = new BlockPos(24, 8, 26);
    private static final long SEED = 7L;
    private static final int DIR = 0;

    /** A church built in the test, its start (if registered) and what the chunks held before. */
    private record Church(ChurchPiece piece, @Nullable StructureStart start, Map<ChunkAccess, Object[]> before) {
        BlockPos altar() {
            return piece.altar();
        }
    }

    private static Church church(GameTestHelper helper, boolean register) {
        ServerLevel level = helper.getLevel();
        SNGameTests.floor(helper, 48, 48);
        BlockPos origin = helper.absolutePos(ORIGIN);
        ChurchPiece piece = new ChurchPiece(origin, DIR, SEED);
        BoundingBox box = PlanWriter.fullBox(origin, ChurchLayout.plan(SEED, DIR), ChurchLayout.HEIGHT);
        PlanWriter.load(level, box);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, new ChunkPos(origin), origin);
        if (!register) return new Church(piece, null, Map.of());
        // What the world generator would have left (CovenCommands.register): the start in its chunk, a reference in every chunk
        // the piece touches. What those chunks held before is kept to put back.
        ChunkPos home = new ChunkPos(origin);
        Map<ChunkAccess, Object[]> before = new HashMap<>();
        BoundingBox pb = piece.getBoundingBox();
        for (int cx = pb.minX() >> 4; cx <= pb.maxX() >> 4; cx++) {
            for (int cz = pb.minZ() >> 4; cz <= pb.maxZ() >> 4; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz);
                Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> refs = new HashMap<>();
                chunk.getAllReferences().forEach((k, v) -> refs.put(k, new it.unimi.dsi.fastutil.longs.LongOpenHashSet(v)));
                before.put(chunk, new Object[]{new HashMap<>(chunk.getAllStarts()), refs});
            }
        }
        StructureStart start = org.papiricoh.supernaturalcraft.coven.CovenCommands.register(level, ChurchStructure.KEY, piece, home);
        return new Church(piece, start, before);
    }

    /** Takes back the start, the sanctuary and the holy ground, and clears what the church and the test left about. */
    @SuppressWarnings("unchecked")
    private static void putBack(GameTestHelper helper, Church c) {
        ServerLevel level = helper.getLevel();
        for (var e : c.before().entrySet()) {
            e.getKey().setAllStarts((Map<Structure, StructureStart>) e.getValue()[0]);
            e.getKey().setAllReferences((Map<Structure, it.unimi.dsi.fastutil.longs.LongSet>) e.getValue()[1]);
        }
        BlockPos altar = c.altar();
        Sanctuaries.get(level).remove(altar);
        ConsecratedGround.get(level).remove(altar);
        AABB all = new AABB(helper.absolutePos(ORIGIN)).inflate(ChurchLayout.RADIUS + 12);
        for (Entity e : level.getEntitiesOfClass(Entity.class, all, e -> e instanceof Mob || e instanceof ItemEntity)) e.discard();
    }

    private static ConsecrateChurchEffect rite(ServerLevel level) {
        var holder = level.getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/consecrate_church"));
        if (holder.isEmpty() || !(holder.get().value() instanceof RitualRecipe r) || !(r.effect() instanceof ConsecrateChurchEffect e)) {
            throw new IllegalStateException("ritual/consecrate_church is missing or not a ConsecrateChurchEffect");
        }
        return e;
    }

    private static String key(@Nullable Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    private static List<ItemEntity> pages(ServerLevel level, BlockPos altar) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(altar).inflate(3), i -> i.getItem().is(AllItems.SIGIL_PAGE.get()));
    }

    @GameTest(template = SNGameTests.SMALL, batch = "church_recipe")
    public static void theRiteIsLoadedAndAffordable(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(SupernaturalCraft.asResource("ritual/consecrate_church"));
        helper.assertTrue(holder.isPresent() && holder.get().value() instanceof RitualRecipe r && r.manaCost() <= 175
                && r.effect() instanceof ConsecrateChurchEffect e && e.radius() == 16, "consecrate_church loaded, within the mana bar, radius 16");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "church_build", timeoutTicks = 100)
    public static void theChurchStandsWithItsAltarCryptAndChests(GameTestHelper helper) {
        Church c = church(helper, false);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(ORIGIN);
        BlockPos altar = c.altar();
        boolean ring = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ring &= level.getBlockState(altar.offset(dx, 0, dz)).isAir() && level.getBlockState(altar.offset(dx, -1, dz)).isSolid();
            }
        }
        boolean loot = true;
        for (int i = 0; i < ChurchLayout.CHESTS.size(); i++) {
            var at = ChurchLayout.at(ChurchLayout.CHESTS.get(i), DIR);
            var be = level.getBlockEntity(origin.offset(at.x(), at.y(), at.z()));
            loot &= be instanceof RandomizableContainerBlockEntity r && r.getLootTable() != null
                    && r.getLootTable().location().toString().equals(ChurchLayout.loot(i));
        }
        var crypt = ChurchLayout.at(ChurchLayout.CRYPT, DIR);
        boolean cryptOpen = level.getBlockState(origin.offset(crypt.x(), crypt.y(), crypt.z())).isAir();
        putBack(helper, c);
        helper.assertTrue(ring, "a clear, floored 3x3 ring round the altar spot for the small circle");
        helper.assertTrue(loot, "both chests hold the church's loot (the crypt's with the nun's bone)");
        helper.assertTrue(cryptOpen, "the crypt is open space");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "church_outside", timeoutTicks = 100)
    public static void outsideARealChurchTheRiteIsRefused(GameTestHelper helper) {
        // Built by hand (as /place structure or the coven command would): the stones are there, the structure is not.
        Church c = church(helper, false);
        ServerLevel level = helper.getLevel();
        ConsecrateChurchEffect rite = rite(level);
        String refused = key(rite.precheck(level, c.altar(), null));
        boolean performed = rite.perform(level, c.altar(), null);
        boolean anyPage = !pages(level, c.altar()).isEmpty();
        putBack(helper, c);
        helper.assertTrue("message.supernaturalcraft.church.not_a_church".equals(refused), "refused off a church's ground, got " + refused);
        helper.assertFalse(performed, "nothing happens");
        helper.assertFalse(anyPage, "and no page is left");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "church_rite", timeoutTicks = 100)
    public static void theFirstRiteInAChurchRewardsItOnce(GameTestHelper helper) {
        Church c = church(helper, true);
        ServerLevel level = helper.getLevel();
        ConsecrateChurchEffect rite = rite(level);
        BlockPos altar = c.altar();
        JournalTests.Witness w = CovenTests.witness(helper, ORIGIN.offset(0, 0, -8));
        String before = key(rite.precheck(level, altar, w));
        boolean first = rite.perform(level, altar, w);
        List<ItemEntity> page = pages(level, altar);
        ResourceLocation sigil = page.size() == 1 ? page.getFirst().getItem().get(AllDataComponents.SIGIL_PAGE) : null;
        SigilComponent sc = sigil == null ? null : level.registryAccess().registryOrThrow(SNRegistries.SIGIL).get(sigil);
        boolean awarded = AuthorWorld.done(w, ConsecrateChurchEffect.ADVANCEMENT);
        boolean sanctuary = Sanctuaries.get(level).consecrated(c.start().getChunkPos().toLong())
                && Sanctuaries.get(level).inside(Vec3.atCenterOf(altar.offset(10, 0, 0)));
        boolean holy = ConsecratedGround.holy(level, altar.offset(12, 0, 0));
        String after = key(rite.precheck(level, altar, w));
        boolean second = rite.perform(level, altar, w);
        int pagesNow = pages(level, altar).size();
        CovenTests.leave(helper, w);
        putBack(helper, c);
        helper.assertTrue("null".equals(before), "on a church's ground the rite may begin, got " + before);
        helper.assertTrue(first, "the rite is performed");
        helper.assertTrue(page.size() == 1, "the first rite leaves one sigil page on the altar, found " + page.size());
        helper.assertTrue(sc != null && sc.school() == School.ENOCHIAN && sc.tier() <= 3, "an Enochian page of tier 3 at most: " + sigil);
        helper.assertTrue(awarded, "main/consecrated_church");
        helper.assertTrue(sanctuary, "the church is a sanctuary round its altar");
        helper.assertTrue(holy, "and holy ground");
        helper.assertTrue("message.supernaturalcraft.church.already".equals(after), "a cleansed church refuses the rite, got " + after);
        helper.assertTrue(second && pagesNow == 1, "forced again, it rewards nothing more: " + pagesNow + " pages");
        helper.succeed();
    }

    @GameTest(template = SNGameTests.ARENA, batch = "church_sanctuary", timeoutTicks = 120)
    public static void aSanctuaryDrivesOutDemonsAndSpiritsButNotTheLiving(GameTestHelper helper) {
        Church c = church(helper, true);
        ServerLevel level = helper.getLevel();
        ConsecrateChurchEffect rite = rite(level);
        BlockPos altar = c.altar();
        rite.perform(level, altar, null);
        Mob demon = spawn(level, AllEntities.BLACK_EYED_DEMON.get(), altar.offset(1, 0, 0));
        Mob ghost = spawn(level, AllEntities.GHOST.get(), altar.offset(-1, 0, 0));
        Mob villager = spawn(level, EntityType.VILLAGER, altar.offset(0, 0, 1));
        villager.setNoAi(true);
        Entity lucifer = AllEntities.LUCIFER.get().create(level);
        helper.assertTrue(Sanctuaries.barred(demon) && Sanctuaries.barred(ghost), "demons and spirits are barred");
        helper.assertFalse(Sanctuaries.barred(villager) || Sanctuaries.barred(lucifer), "the living and the great ones are not");
        Vec3 centre = Vec3.atBottomCenterOf(altar);
        helper.succeedWhen(() -> {
            double d = Math.hypot(demon.getX() - centre.x, demon.getZ() - centre.z);
            double g = Math.hypot(ghost.getX() - centre.x, ghost.getZ() - centre.z);
            helper.assertTrue(d > rite.radius(), "the demon is still inside, " + d + " blocks from the altar");
            helper.assertTrue(g > rite.radius(), "the ghost is still inside, " + g + " blocks from the altar");
            helper.assertTrue(villager.position().distanceTo(centre) < 3, "the villager stays");
            putBack(helper, c);
        });
    }

    private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, BlockPos at) {
        T m = type.create(level);
        if (m == null) throw new IllegalStateException("cannot create " + type);
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
        return m;
    }
}
