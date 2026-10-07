package org.papiricoh.supernaturalcraft.gametest;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The cast pipeline end to end: resolution, costs, reagents, and what effects actually do. */
@GameTestHolder(SupernaturalCraft.MODID)
@PrefixGameTestTemplate(false)
public class MagicTests {

    private static ResourceLocation id(String path) {
        return SupernaturalCraft.asResource(path);
    }

    private static Spell spell(String form, String... effects) {
        return new Spell(Optional.of(id(form)), java.util.Arrays.stream(effects).map(MagicTests::id).toList(), List.of(), "");
    }

    private static ServerPlayer caster(GameTestHelper helper, BlockPos at, LivingEntity facing) {
        // A FakePlayer never logs in, so no mod tries to sync anything over a real connection.
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sn-test-caster"));
        player.setGameMode(GameType.SURVIVAL);
        var pos = helper.absoluteVec(at.getCenter()).subtract(0, 0.5, 0);
        player.moveTo(pos.x, pos.y, pos.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, facing.getEyePosition());
        ManaManager.get(player).setMana(100);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer player) {
        player.discard();
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void everyShippedSigilResolves(GameTestHelper helper) {
        var sigils = helper.getLevel().registryAccess().registryOrThrow(SNRegistries.SIGIL);
        helper.assertTrue(sigils.size() >= 16, "expected the 16 shipped sigils, found " + sigils.size());
        for (String effect : List.of("smite", "hellfire", "frost", "exorcise", "bind", "mend", "repel", "reveal")) {
            for (String form : List.of("touch", "bolt", "burst", "ward")) {
                helper.assertTrue(ResolvedSpell.resolve(helper.getLevel().registryAccess(), spell(form, effect)) != null,
                        form + "+" + effect + " did not resolve");
            }
        }
        helper.succeed();
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void smiteTouchHurtsUndeadAndCostsMana(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        ServerPlayer player = caster(helper, new BlockPos(2, 1, 1), zombie);
        float before = zombie.getHealth();
        SpellCaster.Result result = SpellCaster.cast(player, spell("touch", "smite"), 1f);
        helper.assertTrue(result.success(), "cast failed: " + result);
        helper.assertTrue(before - zombie.getHealth() >= 11.9f, "smite should deal 12 to undead, dealt " + (before - zombie.getHealth()));
        helper.assertTrue(Math.abs(ManaManager.get(player).mana() - 82f) < 0.01f, "expected 82 mana left, have " + ManaManager.get(player).mana());
        done(helper, player);
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void notEnoughManaDoesNothing(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        ServerPlayer player = caster(helper, new BlockPos(2, 1, 1), zombie);
        ManaManager.get(player).setMana(5);
        SpellCaster.Result result = SpellCaster.cast(player, spell("touch", "smite"), 1f);
        helper.assertTrue(result == SpellCaster.Result.NO_MANA, "expected NO_MANA, got " + result);
        helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "zombie was hurt by a spell that wasn't paid for");
        done(helper, player);
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void reagentsAreRequiredAndConsumed(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        ServerPlayer player = caster(helper, new BlockPos(2, 1, 1), zombie);
        SpellCaster.Result without = SpellCaster.cast(player, spell("touch", "hellfire"), 1f);
        helper.assertTrue(without == SpellCaster.Result.NO_REAGENTS, "hellfire without sulfur should fail, got " + without);
        player.getInventory().add(new ItemStack(AllItems.SULFUR.get(), 2));
        SpellCaster.Result with = SpellCaster.cast(player, spell("touch", "hellfire"), 1f);
        helper.assertTrue(with.success(), "hellfire with sulfur failed: " + with);
        helper.assertTrue(player.getInventory().countItem(AllItems.SULFUR.get()) == 1, "one sulfur should have burned");
        helper.assertTrue(zombie.isOnFire(), "hellfire should set the target alight");
        done(helper, player);
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void exorcisingATrappedDemonLeavesAnEmber(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity demon = helper.spawnWithNoFreeWill(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 3));
        demon.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, 200));
        ServerPlayer player = caster(helper, new BlockPos(2, 1, 1), demon);
        player.getInventory().add(new ItemStack(AllItems.SALT.get(), 1));
        SpellCaster.Result result = SpellCaster.cast(player, spell("touch", "exorcise"), 1f);
        helper.assertTrue(result.success(), "exorcise failed: " + result);
        helper.assertTrue(!demon.isAlive(), "trapped demon survived an exorcism");
        boolean ember = !helper.getLevel().getEntitiesOfClass(ItemEntity.class, demon.getBoundingBox().inflate(2),
                e -> e.getItem().is(AllItems.HELLFIRE_EMBER.get())).isEmpty();
        helper.assertTrue(ember, "no hellfire ember left behind");
        done(helper, player);
    }

    @GameTest(template = SNGameTests.SMALL)
    public static void bindTrapsADemon(GameTestHelper helper) {
        SNGameTests.floor(helper, 5, 5);
        LivingEntity demon = helper.spawnWithNoFreeWill(AllEntities.BLACK_EYED_DEMON.get(), new BlockPos(2, 1, 3));
        ServerPlayer player = caster(helper, new BlockPos(2, 1, 1), demon);
        player.getInventory().add(new ItemStack(AllItems.DEMON_BLOOD.get(), 1));
        helper.assertTrue(SpellCaster.cast(player, spell("touch", "bind"), 1f).success(), "bind failed");
        helper.assertTrue(demon.hasEffect(AllMobEffects.TRAPPED), "bind did not trap the demon");
        done(helper, player);
    }
}
