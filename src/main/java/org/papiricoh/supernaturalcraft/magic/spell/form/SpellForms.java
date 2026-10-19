package org.papiricoh.supernaturalcraft.magic.spell.form;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.entity.magic.SpellCarrier;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehavior;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehaviors;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/** The ways a spell can travel (four, and since v0.21 the Chain and the Glyph). */
public final class SpellForms {

    private SpellForms() {
    }

    /** A split Bolt's sigils each carry this share of the potency. */
    public static final float SPLIT_POTENCY = 0.6f, SPLIT_DEGREES = 8f, LINGER_POTENCY = 0.4f, CHAIN_RANGE = 4f;

    /** Touch, catalysed: the spell jumps on to the nearest other foes. */
    static void chain(SpellContext ctx, ResolvedSpell spell, Entity from) {
        int left = ctx.traits.touchChain();
        if (left <= 0) return;
        List<LivingEntity> near = new ArrayList<>(ctx.level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(CHAIN_RANGE),
                e -> e != from && e != ctx.caster && !ResolvedSpell.isFriend(ctx.caster, e)));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        Vec3 a = from.position().add(0, from.getBbHeight() / 2, 0);
        int hop = 0;
        for (LivingEntity e : near.subList(0, Math.min(left, near.size()))) {
            spell.applyTo(ctx, e);
            Vec3 b = e.position().add(0, e.getBbHeight() / 2, 0);
            MagicFx.chain(ctx.level, spell.school(), a, b, e, hop++, spell.impactIndex(), CHAIN_TICKS);
            a = b;
        }
    }

    /** How long a chain arc stays drawn. */
    public static final int CHAIN_TICKS = 10;
    /** How long a ward's pulse plays. */
    public static final int WARD_PULSE_TICKS = 20;

    public static DustParticleOptions dust(int color, float size) {
        return new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f), size);
    }

    /** Whatever is in arm's reach: an entity, else a block, else (for purely helpful spells) yourself. */
    public static final SpellBehavior.Form TOUCH = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.range = sigil.param("range", 4.0f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            LivingEntity caster = ctx.caster;
            Vec3 from = caster.getEyePosition();
            Vec3 to = from.add(caster.getLookAngle().scale(ctx.range));
            BlockHitResult block = ctx.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(caster, from, end,
                    caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(ctx.range)).inflate(1.0),
                    e -> e.isPickable() && !e.isSpectator(), ctx.range * ctx.range);
            Vec3 at;
            if (entity != null) {
                at = entity.getLocation();
                spell.applyTo(ctx, entity.getEntity());
                if (ctx.area > 0) splash(ctx, spell, at, entity.getEntity());
                chain(ctx, spell, entity.getEntity());
            } else if (block.getType() == HitResult.Type.BLOCK) {
                at = block.getLocation();
                spell.applyToBlock(ctx, block.getBlockPos(), block.getDirection());
                if (ctx.area > 0) splash(ctx, spell, at, null);
            } else if (spell.allBeneficial()) {
                at = caster.position().add(0, 1, 0);
                spell.applyTo(ctx, caster);
            } else {
                at = end;
            }
        }
    };

    /** A thrown sigil that flies straight and breaks on the first thing it meets. */
    public static final SpellBehavior.Form BOLT = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.range = sigil.param("speed", 1.6f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            int n = Math.max(1, ctx.traits.boltSplit());
            SpellContext each = ctx.copy();
            if (n > 1) each.potency *= SPLIT_POTENCY;
            each.area += ctx.traits.boltBurstArea();
            Vec3 look = ctx.caster.getLookAngle();
            for (int i = 0; i < n; i++) {
                double offset = Math.toRadians((i - (n - 1) / 2.0) * SPLIT_DEGREES);
                Vec3 dir = new Vec3(look.x * Math.cos(offset) - look.z * Math.sin(offset), look.y,
                        look.x * Math.sin(offset) + look.z * Math.cos(offset));
                SigilBolt bolt = new SigilBolt(ctx.level, ctx.caster, spell.spell(), each);
                bolt.setPos(ctx.caster.getX() + dir.x * 0.6, ctx.caster.getEyeY() - 0.15, ctx.caster.getZ() + dir.z * 0.6);
                bolt.shoot(dir.x, dir.y, dir.z, ctx.range, 0.0f);
                ctx.level.addFreshEntity(bolt);
                // One payload per bolt: the client flies the trail itself (v0.21).
                MagicFx.boltTrail(ctx.level, bolt, spell.school(), bolt.position(), bolt.position().add(dir.normalize()),
                        spell.impactIndex(), ctx.range, SigilBolt.MAX_AGE);
            }
        }
    };

    /** A pulse outward from the caster. */
    public static final SpellBehavior.Form BURST = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.area = sigil.param("radius", 3.5f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            Vec3 c = ctx.caster.position();
            splash(ctx, spell, c.add(0, 1, 0), null);
            spell.applyTo(ctx, ctx.caster);
            if (ctx.traits.burstLingerTicks() > 0) {
                WardEntity zone = WardEntity.lingering(ctx.level, ctx.caster,
                        SpellCarrier.of(spell.spell(), ctx).withPotency(ctx.potency * LINGER_POTENCY), ctx.traits.burstLingerTicks());
                zone.setPos(c.x, c.y, c.z);
                zone.setSchool(spell.school());
                ctx.level.addFreshEntity(zone);
                MagicFx.ward(ctx.level, spell.school(), zone, c, zone.getRadius(), MagicFxPayload.WARD_RAISED, WARD_PULSE_TICKS);
            }
        }
    };

    /** A circle drawn at your feet that keeps working for a while and stops hostile projectiles. */
    public static final SpellBehavior.Form WARD = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.area = sigil.param("radius", 3.0f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            float scale = ctx.traits.wardScale();
            int duration = Math.round(ctx.duration(Math.round(spell.form().param("duration", 200f))) * scale);
            ctx.area *= scale;
            WardEntity ward = new WardEntity(ctx.level, ctx.caster, spell.spell(), ctx, duration);
            ward.setPos(ctx.caster.getX(), ctx.caster.getY(), ctx.caster.getZ());
            ward.setSchool(spell.school());
            ctx.level.addFreshEntity(ward);
            MagicFx.ward(ctx.level, spell.school(), ward, ward.position(), ward.getRadius(), MagicFxPayload.WARD_RAISED, WARD_PULSE_TICKS);
        }
    };

    /**
     * The Chain form (v0.21): a bolt of force leaps to the first thing in its line (up to {@code range}), then on to up to
     * {@code hops} more, each the nearest not yet struck within {@code hop_range} (+ the cast's area) of the last, every hop
     * at {@code hop_decay} of the potency before it. Harmful spells leap between foes, purely helpful ones between friends.
     * One CHAIN_ARC per hop (hop 0 from the hand).
     */
    public static final SpellBehavior.Form CHAIN = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.range = sigil.param("range", 10.0f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            LivingEntity caster = ctx.caster;
            Vec3 from = caster.getEyePosition();
            Vec3 to = from.add(caster.getLookAngle().scale(ctx.range));
            BlockHitResult block = ctx.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, from, end,
                    caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(ctx.range)).inflate(1.0),
                    e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(), ctx.range * ctx.range);
            Vec3 hand = from.add(caster.getLookAngle().scale(0.6)).add(0, -0.35, 0);
            if (hit == null) {
                if (block.getType() == HitResult.Type.BLOCK) {
                    spell.applyToBlock(ctx, block.getBlockPos(), block.getDirection());
                    MagicFx.chain(ctx.level, spell.school(), hand, end, null, 0, spell.impactIndex(), CHAIN_TICKS);
                } else if (spell.allBeneficial()) {
                    spell.applyTo(ctx, caster);
                }
                return;
            }
            boolean friendly = spell.allBeneficial();
            int hops = Math.round(spell.form().param("hops", 3f));
            double hopRange = spell.form().param("hop_range", 5f) + ctx.area;
            float decay = spell.form().param("hop_decay", 0.8f);
            java.util.Set<Entity> struck = new java.util.HashSet<>();
            Entity current = hit.getEntity();
            Vec3 a = hand;
            SpellContext each = ctx;
            for (int hop = 0; current != null && hop <= hops; hop++) {
                struck.add(current);
                Vec3 b = current.position().add(0, current.getBbHeight() / 2, 0);
                MagicFx.chain(ctx.level, spell.school(), a, b, current, hop, spell.impactIndex(), CHAIN_TICKS);
                spell.applyTo(each, current);
                a = b;
                final Entity last = current;
                current = ctx.level.getEntitiesOfClass(LivingEntity.class, last.getBoundingBox().inflate(hopRange),
                                e -> e.isAlive() && !struck.contains(e) && e != caster && !e.isSpectator()
                                        && ResolvedSpell.isFriend(caster, e) == friendly && e.distanceToSqr(last) <= hopRange * hopRange)
                        .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(last))).orElse(null);
                if (current != null) {
                    SpellContext next = each.copy();
                    next.potency *= decay;
                    next.impactBudget = each.impactBudget;
                    next.lingered = each.lingered;
                    each = next;
                }
            }
        }
    };

    /**
     * The Glyph form (v0.21): plants a small sigil on the floor where you look (within {@code range}; at your feet if you look at
     * nothing). It lies quiet for {@code duration} ticks until something hostile to you steps on it, then the spell goes off on
     * whatever stands on it. At most {@link WardEntity#MAX_GLYPHS} per caster: the oldest gives way.
     */
    public static final SpellBehavior.Form GLYPH = new SpellBehavior.Form() {
        @Override
        public void setup(SpellContext ctx, SigilComponent sigil) {
            ctx.range = sigil.param("range", 6.0f);
        }

        @Override
        public void deliver(SpellContext ctx, ResolvedSpell spell) {
            LivingEntity caster = ctx.caster;
            Vec3 from = caster.getEyePosition();
            Vec3 to = from.add(caster.getLookAngle().scale(ctx.range));
            BlockHitResult block = ctx.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            Vec3 at = block.getType() == HitResult.Type.BLOCK && block.getDirection() == net.minecraft.core.Direction.UP
                    ? block.getLocation() : caster.position();
            int duration = ctx.duration(Math.round(spell.form().param("duration", 600f)));
            WardEntity.makeRoomForGlyph(ctx.level, caster);
            WardEntity glyph = WardEntity.glyph(ctx.level, caster, SpellCarrier.of(spell.spell(), ctx), duration);
            glyph.setPos(at.x, at.y, at.z);
            glyph.setSchool(spell.school());
            ctx.level.addFreshEntity(glyph);
            MagicFx.ward(ctx.level, spell.school(), glyph, at, glyph.getRadius(), MagicFxPayload.WARD_RAISED, WARD_PULSE_TICKS);
        }
    };

    /** Applies the spell to everything (except {@code skip}) within {@code ctx.area} of {@code at}. */
    public static void splash(SpellContext ctx, ResolvedSpell spell, Vec3 at, Entity skip) {
        double r = ctx.area;
        for (Entity e : ctx.level.getEntities(ctx.caster, new AABB(at, at).inflate(r))) {
            if (e != skip && e instanceof LivingEntity && e.position().distanceToSqr(at) <= r * r * 1.4) {
                spell.applyTo(ctx, e);
            }
        }
    }

    public static void registerAll() {
        SpellBehaviors.register(SupernaturalCraft.asResource("touch"), TOUCH);
        SpellBehaviors.register(SupernaturalCraft.asResource("bolt"), BOLT);
        SpellBehaviors.register(SupernaturalCraft.asResource("burst"), BURST);
        SpellBehaviors.register(SupernaturalCraft.asResource("ward"), WARD);
        SpellBehaviors.register(SupernaturalCraft.asResource("chain"), CHAIN);
        SpellBehaviors.register(SupernaturalCraft.asResource("glyph"), GLYPH);
    }
}
