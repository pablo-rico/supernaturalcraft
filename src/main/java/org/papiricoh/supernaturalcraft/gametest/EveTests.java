package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveBalance;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveEntity;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveSpoils;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryStanding;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryWorld;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatoryVampireEntity;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatoryWerewolfEntity;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRift;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRiftSavedData;
import org.papiricoh.supernaturalcraft.purgatory.rift.PurgatoryRifts;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Eve (v0.39), each test in its own batch with the arena cleaned first: her glade's arena; her phases one at a time (no blow skips
 * one); her lash's cone, the thorns' ring with its safe heart, the field's green circles; her children and the souls who answer her
 * call (put down, they go back into the earth); her spoils, the hunters let go, and the rift home.
 */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class EveTests {

    private static final BlockPos MID = new BlockPos(24, 1, 24);

    private static EveEntity eve(GameTestHelper helper) {
        BossTests.cleanup(helper);
        SNGameTests.floor(helper, 48, 48);
        return helper.spawn(AllEntities.EVE.get(), MID);
    }

    private static void still(GameTestHelper helper, EveEntity e) {
        helper.assertTrue(e.arena() != null && e.arena().theme() == ArenaTheme.GLADE, "her glade's arena opens round her");
        e.setNoAi(true);
        Vec3 v = helper.absoluteVec(Vec3.atBottomCenterOf(MID));
        e.teleportTo(v.x, v.y, v.z);
    }

    private static Mob dummy(GameTestHelper helper, Vec3 rel) {
        Mob m = helper.spawnWithNoFreeWill(EntityType.PIG, BlockPos.containing(rel));
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        m.setHealth(1000);
        Vec3 v = helper.absoluteVec(rel);
        m.moveTo(v.x, v.y, v.z, 0, 0);
        return m;
    }

    private static boolean struck(Mob m) {
        return m.getHealth() < 1000f;
    }

    private static Vec3 rel(double dx, double dz) {
        return new Vec3(MID.getX() + 0.5 + dx, 1, MID.getZ() + 0.5 + dz);
    }

    @GameTest(template = SNGameTests.ARENA, batch = "eve_phases", timeoutTicks = 400)
    public static void herPhasesComeOneAtATime(GameTestHelper helper) {
        EveEntity e = eve(helper);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(e.arena() != null && e.arena().theme() == ArenaTheme.GLADE, "her glade's arena opens round her");
            BossHealthGuard.set(e, e.getMaxHealth() * (EveBalance.threshold(1) + 0.01f));
            e.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            helper.assertTrue(Math.abs(e.getHealth() - e.getMaxHealth() * EveBalance.threshold(1)) < 0.01f,
                    "a huge blow stops at her first floor: " + e.getHealth() / e.getMaxHealth());
            helper.assertTrue(e.phase() == 2 && e.state() == LuciferEntity.TRANSITION, "and starts the Swarm, not a later phase");
            float before = e.getHealth();
            helper.assertFalse(e.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 30f), "untouchable while she changes");
            helper.assertTrue(e.getHealth() == before, "health kept through the change");
        });
        helper.runAfterDelay(3 + LuciferEntity.TRANSITION_TICKS + 5, () -> {
            helper.assertTrue(e.phase() == 2 && e.state() != LuciferEntity.TRANSITION, "the Swarm has begun");
            e.invulnerableTime = 0;
            e.hurt(AllDamageTypes.source(helper.getLevel(), AllDamageTypes.SMITE, null), 1e6f);
            helper.assertTrue(e.phase() == 3, "the next blow only reaches Mother's Call: " + e.phase());
            helper.assertTrue(Math.abs(e.getHealth() - e.getMaxHealth() * EveBalance.threshold(2)) < 0.01f, "at her second floor");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "eve_blows", timeoutTicks = 60)
    public static void herBlowsLandWhereTheyAreShown(GameTestHelper helper) {
        EveEntity e = eve(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, e);
            Mob front = dummy(helper, rel(0, 3)), behind = dummy(helper, rel(0, -3));
            EveAttacks.Lash lash = new EveAttacks.Lash();
            lash.onWindup(e, front);
            lash.onActive(e, front);
            helper.assertTrue(struck(front), "lashed in the cone");
            helper.assertFalse(struck(behind), "nobody behind her");

            Mob atFeet = dummy(helper, rel(1.2, 0.5)), inRing = dummy(helper, rel(5, 0)), beyond = dummy(helper, rel(12, 0));
            EveAttacks.ThornRing thorns = new EveAttacks.ThornRing();
            thorns.onWindup(e, inRing);
            thorns.onActive(e, inRing);
            helper.assertTrue(struck(inRing), "the thorns' ring strikes");
            helper.assertFalse(struck(atFeet), "her feet are safe");
            helper.assertFalse(struck(beyond), "and beyond the ring");

            EveAttacks.RootField field = new EveAttacks.RootField();
            field.onWindup(e, front);
            Vec3 safe = field.safeSpots().getFirst();
            Mob sheltered = dummy(helper, helper.relativeVec(safe));
            Mob exposed = dummy(helper, rel(-8, 8));
            exposed.setHealth(1000);
            boolean exposedSafe = field.safeSpots().stream().anyMatch(s -> s.distanceTo(exposed.position()) < EveBalance.FIELD_SAFE_RADIUS + 0.5);
            field.onActive(e, front);
            helper.assertFalse(struck(sheltered), "a green circle shelters");
            helper.assertTrue(exposedSafe || struck(exposed), "the rest of the glade erupts");
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "eve_children", timeoutTicks = 100)
    public static void herChildrenAndTheSoulsSheCalls(GameTestHelper helper) {
        EveEntity e = eve(helper);
        PurgatoryWerewolfEntity[] soul = new PurgatoryWerewolfEntity[1];
        PurgatoryVampireEntity[] child = new PurgatoryVampireEntity[1];
        helper.runAfterDelay(3, () -> {
            still(helper, e);
            Mob target = dummy(helper, rel(6, 0));
            Mob born = e.birth(helper.absoluteVec(rel(-4, 0)), target);
            helper.assertTrue(born instanceof PurgatoryVampireEntity, "her first child is a vampire's soul");
            child[0] = (PurgatoryVampireEntity) born;
            helper.assertTrue(born.getMaxHealth() == EveBalance.CHILD_HEALTH && e.minions().contains(born.getUUID()), "frail, and hers");
            soul[0] = helper.spawn(AllEntities.PURGATORY_WEREWOLF.get(), BlockPos.containing(rel(0, -8)));
            int answered = e.callSouls(target);
            helper.assertTrue(answered >= 1 && e.minions().contains(soul[0].getUUID()), "the soul answers her call");
            // Put down (a vampire's soul only goes down without a beheading), the child goes back into the earth.
            child[0].hurt(child[0].damageSources().generic(), 1e4f);
            helper.assertTrue(child[0].downed(), "the child is down");
            e.setNoAi(false);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(child[0].isRemoved(), "taken back into the earth");
            helper.assertTrue(e.children() >= 1, "the answered soul still fights for her");
            BossTests.cleanup(helper);
            soul[0].discard();
            helper.succeed();
        });
    }

    @GameTest(template = SNGameTests.ARENA, batch = "eve_spoils", timeoutTicks = 60)
    public static void herFallLetsTheHuntersGoAndOpensTheWayHome(GameTestHelper helper) {
        EveEntity e = eve(helper);
        helper.runAfterDelay(3, () -> {
            still(helper, e);
            ServerPlayer p = CurseTests.mortal(helper, MID.offset(3, 0, 3), ItemStack.EMPTY);
            p.setData(AllAttachments.PURGATORY, PurgatoryStanding.EMPTY.withVisited().withReturn(net.minecraft.world.level.Level.OVERWORLD,
                    helper.absoluteVec(rel(-10, -10))));
            helper.assertFalse(PurgatoryWorld.mayLeave(p), "held until she falls");
            helper.assertTrue(EveSpoils.share(p).stream().anyMatch(s -> s.is(AllItems.MOTHERS_HEARTWOOD.get())), "her Heartwood, the first time");
            PurgatoryRift rift = EveSpoils.openTheWayHome(helper.getLevel(), e, e.arena());
            EveSpoils.give(helper.getLevel(), e, List.<net.minecraft.world.entity.LivingEntity>of(p), null);
            helper.assertTrue(PurgatoryWorld.mayLeave(p) && PurgatoryWorld.standing(p).escaped(), "Purgatory lets the hunter go");
            boolean dropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(3),
                    i -> i.getItem().is(AllItems.MOTHERS_HEARTWOOD.get())).isEmpty();
            helper.assertTrue(dropped, "her Heartwood lies at the hunter's feet");
            helper.assertTrue(rift.kind() == PurgatoryRift.Kind.HOME && PurgatoryRiftSavedData.get(helper.getLevel()).at(rift.anchor().above()) != null,
                    "a rift home stands");
            var to = PurgatoryRifts.destination(helper.getLevel(), p, rift);
            helper.assertTrue(to != null && to.pos().distanceTo(helper.absoluteVec(rel(-10, -10))) < 0.01, "and leads where the hunter came from");
            for (PurgatoryRift r : List.copyOf(PurgatoryRiftSavedData.get(helper.getLevel()).all())) {
                PurgatoryRifts.collapse(helper.getLevel(), r);
                PurgatoryRiftSavedData.get(helper.getLevel()).remove(r);
            }
            BossTests.cleanup(helper);
            helper.succeed();
        });
    }
}
