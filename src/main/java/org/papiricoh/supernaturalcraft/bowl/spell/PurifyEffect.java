package org.papiricoh.supernaturalcraft.bowl.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Purification spell: within {@code radius} of the bowl (and always on the caster) every harmful
 * effect lifts (possession, jinxes, bleeding, marks: never a collected soul), minds steady (+40
 * sanity), possessed creatures are themselves again, curse bags burn and ghosts scatter.
 */
public record PurifyEffect(double radius) implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("purify");
    public static final float SANITY_RESTORED = 40f;

    public static final MapCodec<PurifyEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 32).optionalFieldOf("radius", 8.0).forGetter(PurifyEffect::radius)
    ).apply(i, PurifyEffect::new));

    @Override
    public ResourceLocation type() {
        return ID;
    }

    @Override
    public boolean perform(BowlCast cast) {
        ServerLevel level = cast.level();
        Vec3 center = cast.surface();
        AABB box = new AABB(cast.bowl()).inflate(radius);
        boolean any = false;

        // Players: the caster by reference (fake players are not in the level), and everyone near.
        Set<Player> players = new LinkedHashSet<>();
        players.add(cast.caster());
        for (Player p : level.getEntitiesOfClass(Player.class, box, p -> p.distanceToSqr(center) <= radius * radius)) players.add(p);
        for (Player p : players) any |= cleanse(p);

        // Possessed creatures are freed (they forget whoever they were turned on).
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof Player) && e.hasEffect(AllMobEffects.POSSESSED) && e.distanceToSqr(center) <= radius * radius)) {
            e.removeEffect(AllMobEffects.POSSESSED);
            if (e instanceof Mob mob) mob.setTarget(null);
            level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            any = true;
        }

        // Curse bags burn: hidden ones nearby, and any slipped into the caster's pockets.
        any |= org.papiricoh.supernaturalcraft.hex.HexBags.burnNear(level, cast.bowl(), radius) > 0;
        any |= org.papiricoh.supernaturalcraft.hex.HexBags.burnCarried(cast.caster()) > 0;

        // Ghosts scatter.
        for (GhostEntity g : level.getEntitiesOfClass(GhostEntity.class, box,
                g -> !g.isInert() && g.distanceToSqr(center) <= radius * radius)) {
            g.disperse();
            any = true;
        }

        if (any) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.5, center.z, 30, radius * 0.3, 0.6, radius * 0.3, 0.02);
        } else {
            cast.caster().displayClientMessage(Component.translatable("message.supernaturalcraft.purify.nothing").withStyle(ChatFormatting.GRAY), false);
        }
        return any;
    }

    /** Lifts {@code p}'s afflictions and steadies their mind. @return whether anything changed */
    public static boolean cleanse(Player p) {
        boolean any = false;
        List<Holder<MobEffect>> lift = new ArrayList<>();
        for (MobEffectInstance inst : p.getActiveEffects()) {
            Holder<MobEffect> effect = inst.getEffect();
            if (effect.is(AllMobEffects.SOULLESS.getId())) continue;
            if (effect.value().getCategory() == MobEffectCategory.HARMFUL || isAffliction(effect)) lift.add(effect);
        }
        for (Holder<MobEffect> effect : lift) any |= p.removeEffect(effect);
        if (p instanceof ServerPlayer) {
            ArcanaData data = ManaManager.get(p);
            float before = data.sanity();
            data.setSanity(before + SANITY_RESTORED);
            if (data.sanity() > before) any = true;
        }
        if (any && p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
        return any;
    }

    /** The mod's own afflictions, some of which are not filed as harmful (possession is neutral). */
    private static boolean isAffliction(Holder<MobEffect> effect) {
        return effect.is(AllMobEffects.POSSESSED.getId()) || effect.is(AllMobEffects.JINXED.getId())
                || effect.is(AllMobEffects.BLEEDING.getId()) || effect.is(AllMobEffects.MARKED.getId());
    }
}
