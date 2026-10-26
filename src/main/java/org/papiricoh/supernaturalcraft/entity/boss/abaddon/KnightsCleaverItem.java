package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.melee.GeoSwordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The Knights' Cleaver (v0.21): Abaddon's own blade, Tier IV and cursed (its profile; without the Mark it hungers like the
 * First Blade). A heavy GeckoLib cleaver whose veins pulse ({@code idle}). Used, it sends a wave of hellfire down the ground
 * before its wielder ({@link #cleave}: a lane {@link AbaddonBalance#CLEAVE_REACH} long, {@link AbaddonBalance#CLEAVE_DAMAGE}
 * scaled by its Ascension (its text, {@code tooltip.supernaturalcraft.weapon.knights_cleaver}, is shown by the weapon tooltips), more against a great enemy by the wielder's tier), every {@link AbaddonBalance#CLEAVE_COOLDOWN} ticks.
 */
public class KnightsCleaverItem extends GeoSwordItem {

    public KnightsCleaverItem(Tier tier, Properties properties) {
        super("knights_cleaver", tier, properties, List.of(), List.of(AbaddonAnimations.CLEAVER_CLEAVE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            cleave(sp, stack);
            play(sp, stack, AbaddonAnimations.CLEAVER_CLEAVE);
            sp.getCooldowns().addCooldown(this, AbaddonBalance.CLEAVE_COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The wave: everyone not a friend in the lane before {@code player} burns. @return whom it struck */
    public static List<LivingEntity> cleave(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 from = player.position();
        double reach = AbaddonBalance.CLEAVE_REACH, half = AbaddonBalance.CLEAVE_WIDTH / 2;
        float base = Ascension.weaponAbility(stack, AbaddonBalance.CLEAVE_DAMAGE);
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(reach + 1, 3, reach + 1))) {
            if (e == player || !e.isAlive() || ResolvedSpell.isFriend(player, e)) continue;
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            if (along < 0.3 || along > reach || across > half || Math.abs(e.getY() - from.y) > 2.5) continue;
            if (e.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, player), base)) {
                e.igniteForSeconds(AbaddonBalance.CLEAVE_FIRE_SECONDS);
                struck.add(e);
            }
        }
        for (int i = 1; i <= (int) reach; i++) {
            Vec3 p = from.add(dir.scale(i));
            level.sendParticles(AllParticles.HELLFIRE.get(), p.x, p.y + 0.2, p.z, 4, 0.3, 0.1, 0.3, 0.02);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        AbaddonFxPayload fx = new AbaddonFxPayload(player.getId(), AbaddonFxPayload.CLEAVE, Math.round(yaw), (int) reach, from, 10);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(player) < 64 * 64) Send.toPlayer(p, fx);
        level.playSound(null, player.blockPosition(), AllSounds.abaddon("sweep"), SoundSource.PLAYERS, 1.2f, 1.1f);
        return struck;
    }
}
