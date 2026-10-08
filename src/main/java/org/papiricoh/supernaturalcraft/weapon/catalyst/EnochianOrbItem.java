package org.papiricoh.supernaturalcraft.weapon.catalyst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.projectile.BossShard;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Tier II catalyst: a glass orb with an Enochian glyph suspended inside. Spells cost less and
 * recover faster, a Bolt splits into three, and on its own it looses a glyph that seeks its mark.
 */
public class EnochianOrbItem extends CatalystItem {

    public static final float MANA = 0.85f, COOLDOWN = 0.8f, GLYPH_DAMAGE = 5f;
    public static final int SPLIT = 3;

    public EnochianOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public float manaMultiplier(ItemStack stack, ResolvedSpell spell) {
        return MANA;
    }

    @Override
    public float cooldownMultiplier(ItemStack stack, ResolvedSpell spell) {
        return COOLDOWN;
    }

    @Override
    public void shape(SpellContext ctx, ItemStack stack, ResolvedSpell spell) {
        ctx.traits = ctx.traits.withBoltSplit(Math.max(ctx.traits.boltSplit(), SPLIT));
    }

    @Override
    protected float ownMana() {
        return 10f;
    }

    @Override
    protected int ownCooldown() {
        return 25;
    }

    @Override
    protected boolean ownSpell(ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        BossShard glyph = new BossShard(player.level(), player, BossShard.Kind.GLYPH,
                org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.scale(stack, GLYPH_DAMAGE), 0.08f, seek(player));
        glyph.setPos(player.getX() + look.x * 0.8, player.getEyeY() - 0.1, player.getZ() + look.z * 0.8);
        glyph.shoot(look.x, look.y, look.z, 0.9f, 0f);
        player.level().addFreshEntity(glyph);
        player.level().playSound(null, player.blockPosition(), AllSounds.SPELL_CAST.get(), SoundSource.PLAYERS, 0.7f, 1.5f);
        return true;
    }

    /** The closest hostile creature roughly where the player is looking. */
    private static @Nullable LivingEntity seek(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestScore = 0;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20),
                e -> e != player && e instanceof net.minecraft.world.entity.monster.Enemy)) {
            Vec3 to = e.getEyePosition().subtract(player.getEyePosition());
            double dot = to.normalize().dot(look);
            if (dot < 0.8) continue;
            double score = dot / Math.max(1, to.length());
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }
}
