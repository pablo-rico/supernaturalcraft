package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.network.GrandWitchFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.raphael.StormcallerItem;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.catalyst.CatalystItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Coven Staff (v0.21, P6): the Grand Witch's own, her first-victory spoil — a Witchcraft catalyst (Tier IV, its profile).
 * Held opposite a grimoire, Witchcraft sigils cost ×{@link GrandWitchBalance#STAFF_WITCHCRAFT_MANA} and strike
 * ×{@link GrandWitchBalance#STAFF_WITCHCRAFT_POTENCY}. Its own spell is the Coven's hex: the foe it points at (within
 * {@link GrandWitchBalance#STAFF_RANGE}) takes {@link GrandWitchBalance#STAFF_DAMAGE} (scaled by Ascension) and, unless it is a
 * great enemy or a player, the Curse of Frailty. GeckoLib ({@code geo/item/coven_staff.geo.json}: {@code idle}, {@code cast}),
 * its crystal lit by its glowmask.
 */
public class CovenStaffItem extends CatalystItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CovenStaffItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    protected float ownMana() {
        return 10f;
    }

    @Override
    protected int ownCooldown() {
        return GrandWitchBalance.STAFF_COOLDOWN;
    }

    @Override
    public float manaMultiplier(ItemStack stack, ResolvedSpell spell) {
        return spell.school() == School.WITCHCRAFT ? GrandWitchBalance.STAFF_WITCHCRAFT_MANA : 1f;
    }

    @Override
    public void shape(SpellContext ctx, ItemStack stack, ResolvedSpell spell) {
        if (spell.school() == School.WITCHCRAFT) ctx.potency *= GrandWitchBalance.STAFF_WITCHCRAFT_POTENCY;
    }

    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        LivingEntity foe = StormcallerItem.aim(player);
        if (foe == null || foe.distanceTo(player) > GrandWitchBalance.STAFF_RANGE) return false;
        hex(player, stack, foe);
        return true;
    }

    /** The Coven's hex on {@code foe}. */
    public static void hex(ServerPlayer player, ItemStack stack, LivingEntity foe) {
        ServerLevel level = player.serverLevel();
        float damage = Ascension.weaponAbility(stack, GrandWitchBalance.STAFF_DAMAGE);
        foe.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, player, player), damage);
        if (!(foe instanceof Player) && !foe.getType().is(AllTags.Entities.BOSSES)) {
            new CurseEffect(CurseEffect.Curse.FRAILTY, GrandWitchBalance.STAFF_CURSE_TICKS).lay(foe);
        }
        Vec3 from = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0, 0.3, 0);
        Vec3 at = foe.position().add(0, foe.getBbHeight() / 2, 0);
        MagicFx.impact(level, School.WITCHCRAFT, at, from, -1, 0f, foe, 20);
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 20, 0.3, 0.4, 0.3, 0.05);
        GrandWitchFxPayload fx = new GrandWitchFxPayload(player.getId(), GrandWitchFxPayload.STAFF, foe.getId(), 0, at, 16);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(player) < 64 * 64) Send.toPlayer(p, fx);
        level.playSound(null, player.blockPosition(), AllSounds.coven("grand_witch.crush"), SoundSource.PLAYERS, 1f, 1.3f);
        if (stack.getItem() instanceof CovenStaffItem item) item.play(player, stack, GrandWitchAnimations.STAFF_CAST);
    }

    public void play(LivingEntity holder, ItemStack stack, String clip) {
        if (holder.level() instanceof ServerLevel level) triggerAnim(holder, GeoItem.getOrAssignId(stack, level), "main", clip);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.coven_staff." + GrandWitchAnimations.STAFF_IDLE);
        controllers.add(new AnimationController<>(this, "main", 3, s -> s.setAndContinue(idle))
                .triggerableAnim(GrandWitchAnimations.STAFF_CAST, RawAnimation.begin().thenPlay("animation.coven_staff." + GrandWitchAnimations.STAFF_CAST)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
