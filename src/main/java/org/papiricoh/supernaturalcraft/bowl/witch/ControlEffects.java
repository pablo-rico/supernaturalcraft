package org.papiricoh.supernaturalcraft.bowl.witch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Expulsion;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.BanishEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.Bindings;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.hex.CurseEffects;
import org.papiricoh.supernaturalcraft.hex.HexBags;
import org.papiricoh.supernaturalcraft.hex.HexHooks;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The bowl's control and protection spells (v0.21). Every one aimed at someone goes through {@link NameMagic} (finding them by
 * the personal component in the bowl) and, if it seizes them, {@link Maledictions} (bosses refused, PvP respected, Ward of the
 * Name and Mirror Hex honoured).
 */
public final class ControlEffects {

    private ControlEffects() {
    }

    public static void bootstrap() {
        BowlSpellEffect.register(BindByName.ID, BindByName.CODEC);
        BowlSpellEffect.register(BanishByName.ID, BanishByName.CODEC);
        BowlSpellEffect.register(Compel.ID, Compel.CODEC);
        BowlSpellEffect.register(BreakHex.ID, BreakHex.CODEC);
        BowlSpellEffect.register(NameWard.ID, NameWard.CODEC);
        BowlSpellEffect.register(Scry.ID, Scry.CODEC);
        BowlSpellEffect.register(Brew.ID, Brew.CODEC);
        BowlSpellEffect.register(FamiliarBond.ID, FamiliarBond.CODEC);
    }

    private static void tell(BowlCast cast, String key, ChatFormatting style, Object... args) {
        cast.caster().displayClientMessage(Component.translatable(key, args).withStyle(style), false);
    }

    private static @Nullable String needsTarget(BowlCast cast) {
        return cast.target().isEmpty() ? "message.supernaturalcraft.bowl.needs_target" : null;
    }

    /**
     * Binding of the Name: whoever the bowl names, within {@link NameMagic#REACH} in this world, is rooted (TRAPPED) for
     * {@code duration} ticks. A boss is never held: one with its own answer to binding ({@link SpellHooks.Bindable}) is held
     * for a breath ({@link #BOSS_TICKS}), any other refuses it.
     */
    public record BindByName(int duration) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("bind_by_name");
        public static final int BOSS_TICKS = 40;
        public static final MapCodec<BindByName> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(20, 6000).optionalFieldOf("duration", 400).forGetter(BindByName::duration)
        ).apply(i, BindByName::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Nullable
        @Override
        public String precheck(BowlCast cast) {
            return needsTarget(cast);
        }

        @Override
        public boolean perform(BowlCast cast) {
            NameMagic.Lookup l = NameMagic.find(cast, NameMagic.REACH);
            if (!l.ok()) {
                NameMagic.explain(cast, l);
                return false;
            }
            LivingEntity victim = l.entity();
            if (NameMagic.boss(victim)) {
                if (victim instanceof SpellHooks.Bindable b && b.onBound(BOSS_TICKS)) {
                    cast.sendTo(NameMagic.chest(victim), victim);
                    return true;
                }
                NameMagic.refuse(cast, victim);
                return false;
            }
            return Maledictions.strike(cast, victim, v -> v.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, duration), cast.caster())).spent();
        }
    }

    /**
     * Banish by Name: the named, within {@link NameMagic#REACH} in this world, is sent away as the banishing rites would: a ghost
     * laid to rest, a demon or hound cast back into the dark ({@link BanishEffect#castOut}), a lesser angel sent back to Heaven
     * and an angel player flung far ({@link Expulsion#banish}), a demon player expelled. Never a boss, nor a crossroads demon (a
     * deal is broken only by its death), nor anything of this world.
     */
    public record BanishByName(float expelDamage) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("banish_by_name");
        public static final MapCodec<BanishByName> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.floatRange(0, 40).optionalFieldOf("expel_damage", 6f).forGetter(BanishByName::expelDamage)
        ).apply(i, BanishByName::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Nullable
        @Override
        public String precheck(BowlCast cast) {
            return needsTarget(cast);
        }

        /** Whether the banishing has any hold on {@code e}. */
        public static boolean banishable(LivingEntity e) {
            if (NameMagic.boss(e) || e instanceof CrossroadsDemonEntity) return false;
            if (e instanceof GhostEntity g) return !g.isFading();
            if (e instanceof ServerPlayer) return Kin.isDemon(e) || Kin.isAngel(e);
            return Bindings.isUnclean(e) || Kin.isAngel(e);
        }

        @Override
        public boolean perform(BowlCast cast) {
            NameMagic.Lookup l = NameMagic.find(cast, NameMagic.REACH);
            if (!l.ok()) {
                NameMagic.explain(cast, l);
                return false;
            }
            LivingEntity victim = l.entity();
            // v0.21 (P5): a demon riding a vessel (Abaddon in a knight; her own taglock or the knight's) is forced out.
            if (victim instanceof org.papiricoh.supernaturalcraft.magic.spell.SpellHooks.Expellable x
                    && x.onExpelled(cast.caster(), org.papiricoh.supernaturalcraft.magic.spell.SpellHooks.Expellable.NAME)) return true;
            if (!banishable(victim)) {
                if (NameMagic.boss(victim)) NameMagic.refuse(cast, victim);
                else tell(cast, "message.supernaturalcraft.witchcraft.of_this_world", ChatFormatting.GRAY, victim.getName());
                return false;
            }
            return Maledictions.strike(cast, victim, v -> banish(cast, v)).spent();
        }

        private void banish(BowlCast cast, LivingEntity v) {
            if (!banishable(v)) return;
            if (v instanceof GhostEntity g) g.layToRest();
            else if (v instanceof ServerPlayer sp && Kin.isDemon(sp)) Expulsion.expel(sp, cast.caster(), expelDamage);
            else if (Kin.isAngel(v)) Expulsion.banish(v, cast.caster());
            else if (v.level() instanceof ServerLevel level) BanishEffect.castOut(level, v);
        }
    }

    /**
     * Compel: a creature (the one the bowl names, within {@link NameMagic#REACH}; without a name, the nearest within
     * {@link #NEAR} of the bowl) follows its compeller and will not attack them for {@code duration} ticks; a player named is
     * drawn slowly toward the bowl for {@code player_duration} ticks. Never a boss.
     */
    public record Compel(int duration, int playerDuration) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("compel");
        public static final double NEAR = 8;
        public static final MapCodec<Compel> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(20, 12000).optionalFieldOf("duration", 1200).forGetter(Compel::duration),
                Codec.intRange(20, 1200).optionalFieldOf("player_duration", 100).forGetter(Compel::playerDuration)
        ).apply(i, Compel::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public boolean perform(BowlCast cast) {
            LivingEntity victim;
            if (cast.target().isPresent()) {
                NameMagic.Lookup l = NameMagic.find(cast, NameMagic.REACH);
                if (!l.ok()) {
                    NameMagic.explain(cast, l);
                    return false;
                }
                victim = l.entity();
            } else {
                Vec3 c = Vec3.atCenterOf(cast.bowl());
                victim = cast.level().getEntitiesOfClass(Mob.class, new AABB(cast.bowl()).inflate(NEAR),
                                m -> m.isAlive() && !NameMagic.boss(m) && m.distanceToSqr(c) <= NEAR * NEAR)
                        .stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(c))).orElse(null);
                if (victim == null) {
                    tell(cast, "message.supernaturalcraft.witchcraft.nothing_near", ChatFormatting.GRAY);
                    return false;
                }
            }
            return Maledictions.strike(cast, victim, v -> compel(cast, v)).spent();
        }

        private void compel(BowlCast cast, LivingEntity v) {
            var data = v.getPersistentData();
            if (v instanceof Player) {
                data.putLong(CurseEffects.Compelled.ANCHOR, cast.bowl().asLong());
                v.addEffect(new MobEffectInstance(AllMobEffects.COMPELLED, playerDuration), cast.caster());
                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, playerDuration, 0), cast.caster());
            } else {
                data.putUUID(CurseEffects.Compelled.COMPELLER, cast.caster().getUUID());
                v.addEffect(new MobEffectInstance(AllMobEffects.COMPELLED, duration), cast.caster());
                if (v instanceof Mob m && m.getTarget() == cast.caster()) m.setTarget(null);
            }
        }
    }

    /**
     * Break Hex: frees whoever the bowl names (or, without a name, the caster) of every curse the witchcraft lays (jinx,
     * bleeding, agony, rot, silence, frailty, compulsion, bad luck) and of whatever later systems hook in ({@link HexHooks}),
     * and burns the curse bags within {@code radius} of the bowl and of them, and those they carry.
     */
    public record BreakHex(double radius) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("break_hex");
        public static final MapCodec<BreakHex> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.doubleRange(1, 32).optionalFieldOf("radius", 8.0).forGetter(BreakHex::radius)
        ).apply(i, BreakHex::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        /** The curses it lifts. */
        public static List<net.minecraft.core.Holder<MobEffect>> curses() {
            return List.of(AllMobEffects.JINXED, AllMobEffects.BLEEDING, AllMobEffects.AGONY, AllMobEffects.ROT, AllMobEffects.SILENCED,
                    AllMobEffects.MARKED, AllMobEffects.COMPELLED, MobEffects.UNLUCK);
        }

        /** Lifts every curse from {@code e}. @return whether there was any */
        public static boolean free(LivingEntity e) {
            return free(e, null);
        }

        /** As {@link #free(LivingEntity)}, freed by {@code by} (v0.21 P3: a freed victim thanks them). */
        public static boolean free(LivingEntity e, @org.jetbrains.annotations.Nullable LivingEntity by) {
            boolean any = false;
            // The vanilla effects a curse laid beside its marker (CurseEffect#lay) go with it: Frailty's Weakness and Mining
            // Fatigue, Rot's Hunger and Poison. Only those that last no longer than the marker (laid with it, not a potion).
            any |= liftCompanions(e, AllMobEffects.MARKED, MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN);
            any |= liftCompanions(e, AllMobEffects.ROT, MobEffects.HUNGER, MobEffects.POISON);
            for (var c : curses()) any |= e.removeEffect(c);
            any |= HexHooks.breakHex(e, by);
            return any;
        }

        @SafeVarargs
        private static boolean liftCompanions(LivingEntity e, net.minecraft.core.Holder<MobEffect> marker,
                                              net.minecraft.core.Holder<MobEffect>... companions) {
            MobEffectInstance m = e.getEffect(marker);
            if (m == null) return false;
            boolean any = false;
            for (var c : companions) {
                MobEffectInstance i = e.getEffect(c);
                if (i != null && !i.isInfiniteDuration() && (m.isInfiniteDuration() || i.getDuration() <= m.getDuration() + 20)) {
                    any |= e.removeEffect(c);
                }
            }
            return any;
        }

        @Override
        public boolean perform(BowlCast cast) {
            LivingEntity who = cast.caster();
            if (cast.target().isPresent()) {
                NameMagic.Lookup l = NameMagic.find(cast, -1);
                if (!l.ok()) {
                    NameMagic.explain(cast, l);
                    return false;
                }
                who = l.entity();
            }
            boolean any = free(who, cast.caster());
            // v0.21 (P3): the hexed victims standing round the bowl are freed too.
            final LivingEntity named = who;
            for (var victim : cast.level().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity.class,
                    new net.minecraft.world.phys.AABB(cast.bowl()).inflate(radius), v -> v != named && v.isAlive())) {
                any |= HexHooks.breakHex(victim, cast.caster());
            }
            int burned = HexBags.burnNear(cast.level(), cast.bowl(), radius);
            if (who.level() instanceof ServerLevel wl) burned += HexBags.burnNear(wl, who.blockPosition(), radius);
            if (who instanceof Player p) burned += HexBags.burnCarried(p);
            if (!any && burned == 0) {
                tell(cast, "message.supernaturalcraft.witchcraft.no_hex", ChatFormatting.GRAY, who.getName());
                return false;
            }
            if (who.level() instanceof ServerLevel wl) {
                MagicFx.hex(wl, who, who.position().add(0, who.getBbHeight() / 2, 0), MagicFxPayload.HEX_PROTECT, 1.5f, Maledictions.FX_TICKS);
            }
            if (who != cast.caster()) cast.sendTo(NameMagic.chest(who), who);
            return true;
        }
    }

    /**
     * Ward of the Name ({@code ward}: NAME_WARDED, curses aimed at the bearer fail and hurt their caster) and Mirror Hex
     * ({@code mirror}: MIRRORED, the next curse goes back to its caster), on whoever the bowl names or else the caster, for
     * {@code duration} ticks.
     */
    public record NameWard(Kind kind, int duration) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("name_ward");

        public enum Kind implements StringRepresentable {
            WARD, MIRROR;

            public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        public static final MapCodec<NameWard> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(NameWard::kind),
                Codec.intRange(20, 72000).optionalFieldOf("duration", 12000).forGetter(NameWard::duration)
        ).apply(i, NameWard::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Override
        public boolean perform(BowlCast cast) {
            LivingEntity who = cast.caster();
            if (cast.target().isPresent()) {
                NameMagic.Lookup l = NameMagic.find(cast, -1);
                if (!l.ok()) {
                    NameMagic.explain(cast, l);
                    return false;
                }
                who = l.entity();
            }
            who.addEffect(new MobEffectInstance(kind == Kind.WARD ? AllMobEffects.NAME_WARDED : AllMobEffects.MIRRORED, duration, 0,
                    false, false, true), cast.caster());
            if (who.level() instanceof ServerLevel wl) {
                MagicFx.hex(wl, who, who.position().add(0, who.getBbHeight() / 2, 0), MagicFxPayload.HEX_PROTECT, 1.5f, Maledictions.FX_TICKS);
            }
            if (who != cast.caster()) cast.sendTo(NameMagic.chest(who), who);
            return true;
        }
    }

    /**
     * Scrying: the bowl's smoke shows the caster where the named is (in this world): roughly where, in what land, and it outlines
     * them (Glowing, seen by all, for {@code duration} ticks). A Ward of the Name hides its bearer.
     */
    public record Scry(int duration) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("scry");
        public static final MapCodec<Scry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(20, 6000).optionalFieldOf("duration", 600).forGetter(Scry::duration)
        ).apply(i, Scry::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Nullable
        @Override
        public String precheck(BowlCast cast) {
            return needsTarget(cast);
        }

        @Override
        public boolean perform(BowlCast cast) {
            NameMagic.Lookup l = NameMagic.find(cast, 0);
            if (!l.ok()) {
                NameMagic.explain(cast, l);
                return false;
            }
            LivingEntity seen = l.entity();
            if (seen != cast.caster() && seen.hasEffect(AllMobEffects.NAME_WARDED)) {
                MagicFx.hex(cast.level(), seen, NameMagic.chest(seen), MagicFxPayload.HEX_PROTECT, 1.2f, Maledictions.FX_TICKS);
                tell(cast, "message.supernaturalcraft.scrying.veiled", ChatFormatting.GRAY, seen.getName());
                return true;
            }
            seen.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, false));
            BlockPos at = seen.blockPosition();
            Component biome = seen.level().getBiome(at).unwrapKey()
                    .map(k -> (Component) Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()))
                    .orElse(Component.literal("?"));
            int x = at.getX() >> 4 << 4, z = at.getZ() >> 4 << 4;
            tell(cast, "message.supernaturalcraft.scrying.vision", ChatFormatting.DARK_PURPLE, seen.getName(), biome, x, at.getY(), z);
            cast.sendTo(NameMagic.chest(seen), seen);
            return true;
        }
    }

    /**
     * The bowl's distillates, which it alone makes ({@code bound_blood}: the poured blood, bound to its owner, as a vial of bound
     * blood; {@code hexed_ink}: Enochian ink steeped in the school of the {@code #supernaturalcraft:ink_steeps} ingredient: gold
     * nugget Enochian, blaze powder Infernal, amethyst Witchcraft, salt Hunter). The result pops out of the bowl.
     */
    public record Brew(Kind kind) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("brew");

        public enum Kind implements StringRepresentable {
            BOUND_BLOOD, HEXED_INK;

            public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        public static final MapCodec<Brew> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Kind.CODEC.fieldOf("kind").forGetter(Brew::kind)
        ).apply(i, Brew::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Nullable
        @Override
        public String precheck(BowlCast cast) {
            return switch (kind) {
                case BOUND_BLOOD -> cast.bloodOwner().isEmpty() ? "message.supernaturalcraft.brew.no_blood" : null;
                case HEXED_INK -> school(cast).isNone() ? "message.supernaturalcraft.brew.no_school" : null;
            };
        }

        /** The school the steeping ingredient gives (NONE without one). */
        public static org.papiricoh.supernaturalcraft.magic.school.School school(BowlCast cast) {
            for (net.minecraft.world.item.ItemStack s : cast.contents().stacks()) {
                var school = steepSchool(s);
                if (!school.isNone()) return school;
            }
            return org.papiricoh.supernaturalcraft.magic.school.School.NONE;
        }

        /** What school {@code stack} steeps ink in. */
        public static org.papiricoh.supernaturalcraft.magic.school.School steepSchool(net.minecraft.world.item.ItemStack stack) {
            if (stack.is(net.minecraft.world.item.Items.GOLD_NUGGET)) return org.papiricoh.supernaturalcraft.magic.school.School.ENOCHIAN;
            if (stack.is(net.minecraft.world.item.Items.BLAZE_POWDER)) return org.papiricoh.supernaturalcraft.magic.school.School.INFERNAL;
            if (stack.is(net.minecraft.world.item.Items.AMETHYST_SHARD)) return org.papiricoh.supernaturalcraft.magic.school.School.WITCHCRAFT;
            if (stack.is(org.papiricoh.supernaturalcraft.registry.AllItems.SALT.get())) return org.papiricoh.supernaturalcraft.magic.school.School.HUNTER;
            return org.papiricoh.supernaturalcraft.magic.school.School.NONE;
        }

        @Override
        public boolean perform(BowlCast cast) {
            net.minecraft.world.item.ItemStack made = switch (kind) {
                case BOUND_BLOOD -> cast.blood().map(d -> org.papiricoh.supernaturalcraft.bowl.spell.BoundBloodItem.of(d.owner().get(),
                        d.ownerName().orElse("?"))).orElse(net.minecraft.world.item.ItemStack.EMPTY);
                case HEXED_INK -> school(cast).isNone() ? net.minecraft.world.item.ItemStack.EMPTY
                        : org.papiricoh.supernaturalcraft.magic.item.HexedInkItem.of(school(cast));
            };
            if (made.isEmpty()) return false;
            Vec3 at = cast.surface().add(0, 0.6, 0);
            net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(cast.level(), at.x, at.y, at.z, made);
            item.setDeltaMovement(0, 0.15, 0);
            item.setDefaultPickUpDelay();
            cast.level().addFreshEntity(item);
            return true;
        }

        @Override
        public net.minecraft.world.item.ItemStack displayResult() {
            return new net.minecraft.world.item.ItemStack(kind == Kind.BOUND_BLOOD
                    ? org.papiricoh.supernaturalcraft.registry.AllItems.BOUND_BLOOD.get()
                    : org.papiricoh.supernaturalcraft.registry.AllItems.HEXED_INK.get());
        }
    }

    /**
     * Familiar Bond (Rowena's): the tamed cat or parrot the bowl names (yours, within {@code reach} of the bowl) becomes your
     * familiar: the {@code FAMILIAR} attachment names you, and the bond mends it and steadies it a while (Regeneration,
     * Resistance). The coven's familiars (P3) build on it.
     */
    public record FamiliarBond(double reach, int buffTicks) implements BowlSpellEffect {
        public static final ResourceLocation ID = SupernaturalCraft.asResource("familiar_bond");
        public static final MapCodec<FamiliarBond> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.doubleRange(1, 256).optionalFieldOf("reach", 64.0).forGetter(FamiliarBond::reach),
                Codec.intRange(20, 12000).optionalFieldOf("buff_ticks", 1200).forGetter(FamiliarBond::buffTicks)
        ).apply(i, FamiliarBond::new));

        @Override
        public ResourceLocation type() {
            return ID;
        }

        @Nullable
        @Override
        public String precheck(BowlCast cast) {
            return needsTarget(cast);
        }

        /** Whether {@code e} may become {@code witch}'s familiar: a cat or parrot they tamed. */
        public static boolean eligible(LivingEntity e, Player witch) {
            return (e instanceof net.minecraft.world.entity.animal.Cat || e instanceof net.minecraft.world.entity.animal.Parrot)
                    && e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame() && witch.getUUID().equals(pet.getOwnerUUID());
        }

        @Override
        public boolean perform(BowlCast cast) {
            NameMagic.Lookup l = NameMagic.find(cast, reach);
            if (!l.ok()) {
                NameMagic.explain(cast, l);
                return false;
            }
            LivingEntity pet = l.entity();
            if (!eligible(pet, cast.caster())) {
                tell(cast, "message.supernaturalcraft.familiar.not_yours", ChatFormatting.GRAY, pet.getName());
                return false;
            }
            pet.setData(org.papiricoh.supernaturalcraft.registry.AllAttachments.FAMILIAR, java.util.Optional.of(cast.caster().getUUID()));
            pet.heal(pet.getMaxHealth());
            pet.addEffect(new MobEffectInstance(MobEffects.REGENERATION, buffTicks, 0), cast.caster());
            pet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffTicks, 0), cast.caster());
            MagicFx.hex(cast.level(), pet, NameMagic.chest(pet), MagicFxPayload.HEX_PROTECT, 1.0f, Maledictions.FX_TICKS);
            cast.sendTo(NameMagic.chest(pet), pet);
            tell(cast, "message.supernaturalcraft.familiar.bound", ChatFormatting.DARK_PURPLE, pet.getName());
            return true;
        }
    }
}
