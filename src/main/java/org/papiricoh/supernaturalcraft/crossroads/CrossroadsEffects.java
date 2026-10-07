package org.papiricoh.supernaturalcraft.crossroads;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** The crossroads' bowl spells: calling the demon, and breaking a deal. */
public final class CrossroadsEffects {

    private CrossroadsEffects() {
    }

    public static void bootstrap() {
        BowlSpellEffect.register(SummonCrossroads.ID, SummonCrossroads.CODEC);
        BowlSpellEffect.register(BreakDeal.ID, BreakDeal.CODEC);
    }

    /** {@code supernaturalcraft:summon_crossroads}: the demon steps out of the smoke beside the bowl. */
    public record SummonCrossroads() implements BowlSpellEffect {

        public static final ResourceLocation ID = SupernaturalCraft.asResource("summon_crossroads");
        public static final MapCodec<SummonCrossroads> CODEC = MapCodec.unit(SummonCrossroads::new);

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable String precheck(BowlCast cast) {
            return Debts.get(cast.caster()).active() ? "message.supernaturalcraft.crossroads.already_bound" : null;
        }

        @Override
        public boolean perform(BowlCast cast) {
            ServerPlayer caster = cast.caster();
            Vec3 bowl = cast.surface();
            // Across the bowl from whoever called, facing them.
            Vec3 away = new Vec3(bowl.x - caster.getX(), 0, bowl.z - caster.getZ());
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            Vec3 at = Debts.floor(cast.level(), bowl.add(away.scale(1.8)));
            return CrossroadsDemonEntity.summon(cast.level(), at, caster) != null;
        }

        @Override
        public ItemStack displayResult() {
            return new ItemStack(AllItems.CROSSROADS_DEMON_SPAWN_EGG.get());
        }
    }

    /**
     * {@code supernaturalcraft:break_deal}: burn your own contract and the demon walks again, hostile.
     * Kill it before the debt is due and you are free.
     */
    public record BreakDeal() implements BowlSpellEffect {

        public static final ResourceLocation ID = SupernaturalCraft.asResource("break_deal");
        public static final MapCodec<BreakDeal> CODEC = MapCodec.unit(BreakDeal::new);

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public @Nullable String precheck(BowlCast cast) {
            String why = Debts.cannotBreak(cast.caster());
            if (why != null) return why;
            ItemStack contract = cast.find(AllItems.CROSSROADS_CONTRACT.get());
            ContractTerms terms = contract.get(AllDataComponents.CONTRACT.get());
            if (!contract.isEmpty() && (terms == null || !terms.owner().equals(cast.caster().getUUID()))) {
                return "message.supernaturalcraft.crossroads.not_your_contract";
            }
            return null;
        }

        @Override
        public boolean perform(BowlCast cast) {
            return precheck(cast) == null && Debts.breakDeal(cast.caster(), cast.surface());
        }
    }
}
