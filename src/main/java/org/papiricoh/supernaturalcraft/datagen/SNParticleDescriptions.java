package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

public class SNParticleDescriptions extends ParticleDescriptionProvider {

    public SNParticleDescriptions(PackOutput output, ExistingFileHelper helper) {
        super(output, helper);
    }

    @Override
    protected void addDescriptions() {
        spriteSet(AllParticles.HELLFIRE.get(), SupernaturalCraft.asResource("hellfire"), 4, false);
        spriteSet(AllParticles.GRACE.get(), SupernaturalCraft.asResource("grace"), 4, false);
        spriteSet(AllParticles.SIGIL.get(), SupernaturalCraft.asResource("sigil"), 4, false);
        spriteSet(AllParticles.DEMON_SMOKE.get(), SupernaturalCraft.asResource("demon_smoke"), 4, false);
        spriteSet(AllParticles.FROST.get(), SupernaturalCraft.asResource("frost"), 2, false);
        spriteSet(AllParticles.ASH.get(), SupernaturalCraft.asResource("ash"), 2, false);
        spriteSet(AllParticles.YELLOW_SMOKE.get(), SupernaturalCraft.asResource("yellow_smoke"), 4, false);
        spriteSet(AllParticles.WHITE_LIGHT.get(), SupernaturalCraft.asResource("white_light"), 4, false);
        spriteSet(AllParticles.INK.get(), SupernaturalCraft.asResource("ink"), 4, false);
        spriteSet(AllParticles.PAGE.get(), SupernaturalCraft.asResource("page"), 4, false);
        spriteSet(AllParticles.BOWL_SMOKE.get(), SupernaturalCraft.asResource("bowl_smoke"), 4, false);
        spriteSet(AllParticles.VOID_MOTE.get(), SupernaturalCraft.asResource("void_mote"), 4, false);
        // v0.39: Godstiel's (one white sprite each, tinted by the client).
        spriteSet(AllParticles.PURGATORY_SOUL.get(), SupernaturalCraft.asResource("purgatory_soul"));
        spriteSet(AllParticles.GRACE_MOTE.get(), SupernaturalCraft.asResource("grace_mote"));
        spriteSet(AllParticles.GOO_DROP.get(), SupernaturalCraft.asResource("goo_drop"));
        spriteSet(AllParticles.SHADOW_FEATHER.get(), SupernaturalCraft.asResource("shadow_feather"));
        org.papiricoh.supernaturalcraft.datagen.chuck.ChuckAssetData.particles((type, name, count) ->
                spriteSet(type, SupernaturalCraft.asResource(name), count, false));
        org.papiricoh.supernaturalcraft.datagen.horsemen.HorsemenAssetData.particles((type, name, count) ->
                spriteSet(type, SupernaturalCraft.asResource(name), count, false));
        org.papiricoh.supernaturalcraft.datagen.michael.MichaelAssetData.particles((type, name, count) ->
                spriteSet(type, SupernaturalCraft.asResource(name), count, false));
        org.papiricoh.supernaturalcraft.datagen.magic.MagicAssetData.particles((type, name, count) ->
                spriteSet(type, SupernaturalCraft.asResource(name), count, false));
    }
}
