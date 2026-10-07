package org.papiricoh.supernaturalcraft.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseEvents;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseState;
import org.papiricoh.supernaturalcraft.weapon.curse.Curses;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Hungry weapons: they eat, they starve, they answer to one hand only. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class CurseTests {

    /** FakePlayers ignore all damage; these tests need a bearer who can bleed. */
    static final class Mortal extends FakePlayer {
        Mortal(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "sn-test-mortal"));
        }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return false;
        }
    }

    static ServerPlayer mortal(GameTestHelper helper, BlockPos at, ItemStack held) {
        Mortal p = new Mortal(helper.getLevel());
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = helper.absoluteVec(at.getBottomCenter());
        p.moveTo(v.x, v.y, v.z, 0, 0);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            var f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(p, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }

    /** Unticked players never pick up their weapon's attributes; apply them by hand. */
    static void armed(ServerPlayer p) {
        p.getMainHandItem().forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                (attr, mod) -> p.getAttribute(attr).addOrUpdateTransientModifier(mod));
        ArsenalTests.fullyCharged(p);
    }

    static ItemStack cursed(net.minecraft.world.item.Item item, int souls, int satiation, UUID owner) {
        ItemStack stack = new ItemStack(item);
        stack.set(AllDataComponents.CURSE, new CurseState(souls, satiation, Optional.of(owner), CurseState.FRESH.lastMarkTick()));
        return stack;
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void aKillFeedsTheFirstBlade(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 6), 180);
        z.setHealth(1f);
        ServerPlayer p = mortal(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.FIRST_BLADE.get()));
        ItemStack blade = p.getMainHandItem();
        blade.set(AllDataComponents.CURSE, cursed(AllItems.FIRST_BLADE.get(), 0, 50, p.getUUID()).get(AllDataComponents.CURSE));
        armed(p);
        p.attack(z);
        helper.assertTrue(z.isDeadOrDying(), "the zombie survived at " + z.getHealth() + "; strength " + p.getAttackStrengthScale(0.5f)
                + ", damage " + p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        CurseState s = Curses.state(blade);
        helper.assertTrue(s.souls() == CurseLevels.soulsFor(z.getMaxHealth()), "expected " + CurseLevels.soulsFor(z.getMaxHealth()) + " souls, have " + s.souls());
        helper.assertTrue(s.satiation() == 50 + CurseLevels.FEED_PER_KILL, "satiation " + s.satiation());
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void aStarvingBladeFeedsOnItsBearer(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        p.setHealth(20f);
        Curses.starve(p, new CurseState(0, 0, Optional.of(p.getUUID()), 0));
        helper.assertTrue(p.getHealth() < 20f, "the hunger drew no blood");
        helper.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "no weakness");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void theCodexPaysInBloodAndSanity(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = mortal(helper, new BlockPos(5, 1, 5), new ItemStack(AllItems.GRIMOIRE.get()));
        p.setItemInHand(InteractionHand.OFF_HAND, cursed(AllItems.WHISPERING_CODEX.get(), 0, 100, p.getUUID()));
        p.setHealth(20f);
        var arcana = ManaManager.get(p);
        arcana.setMana(100);
        var result = SpellCaster.cast(p, new Spell(Optional.of(SupernaturalCraft.asResource("bolt")),
                List.of(SupernaturalCraft.asResource("smite")), List.of(), ""), 1f);
        helper.assertTrue(result.success(), "cast failed: " + result);
        helper.assertTrue(arcana.mana() >= 99.9f, "the codex spent mana: " + arcana.mana());
        helper.assertTrue(p.getHealth() < 20f, "the codex took no blood");
        helper.assertTrue(arcana.sanity() < 100f, "the codex took no sanity");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void theFirstBladeRefusesAStranger(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        Zombie z = ArsenalTests.dummy(helper, new BlockPos(5, 1, 6), 180);
        ServerPlayer p = mortal(helper, new BlockPos(5, 1, 5), cursed(AllItems.FIRST_BLADE.get(), 0, 100, UUID.randomUUID()));
        armed(p);
        helper.assertTrue(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > 5, "the blade's damage was not applied");
        p.attack(z);
        float dealt = z.getMaxHealth() - z.getHealth();
        helper.assertTrue(dealt <= 1.01f, "a stranger dealt " + dealt);
        p.setHealth(20f);
        Curses.tickCarried(p, p.getMainHandItem());
        p.tickCount = 20;
        Curses.tickCarried(p, p.getMainHandItem());
        helper.assertTrue(p.getHealth() < 20f, "a stranger was not burned");
        p.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.MEDIUM)
    public static void theMarkKeepsAFedBearerAlive(GameTestHelper helper) {
        SNGameTests.floor(helper, 11, 11);
        ServerPlayer p = mortal(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND, cursed(AllItems.FIRST_BLADE.get(), 400, 90, p.getUUID()));
        var death = new LivingDeathEvent(p, p.damageSources().generic());
        CurseEvents.onDeath(death);
        helper.assertTrue(death.isCanceled(), "the Mark did not hold");
        helper.assertTrue(Math.abs(p.getHealth() - 1f) < 0.01f, "expected one heart point, have " + p.getHealth());
        var again = new LivingDeathEvent(p, p.damageSources().generic());
        CurseEvents.onDeath(again);
        helper.assertFalse(again.isCanceled(), "the Mark held twice in a row");
        p.discard();
        helper.succeed();
    }
}
