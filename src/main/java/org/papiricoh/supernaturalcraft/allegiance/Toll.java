package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * What "Make me one of you" costs: {@link Allegiance#tollHearts} hearts the crossroads keeps, worn as a max-health
 * modifier (the pattern of {@code crossroads.Boons}). Re-applied on login and respawn; idempotent. The demon cure's last
 * night gives them back.
 */
public final class Toll {

    public static final ResourceLocation HEARTS = SupernaturalCraft.asResource("crossroads_toll");
    /** Hearts the crossroads keeps for a conversion. */
    public static final int CONVERT_HEARTS = 2;

    private Toll() {
    }

    public static void apply(Player player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        int hearts = Allegiances.get(player).tollHearts();
        if (hearts <= 0) {
            health.removeModifier(HEARTS);
        } else {
            health.addOrReplacePermanentModifier(new AttributeModifier(HEARTS, -hearts * 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
