package org.papiricoh.supernaturalcraft.client.colt;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.reward.colt.ColtReload;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

/**
 * The Colt's model, plus the bones posed in code after the animations: the cylinder turns a fifth
 * per chamber, rounds show only in loaded chambers, and the muzzle flash blooms for a moment.
 */
public class ColtModel extends DefaultedItemGeoModel<ColtItem> {

    /** Ticks the muzzle flash lives: it swells for {@link #FLASH_RISE} and dies away. */
    public static final float FLASH_TICKS = 2.2f, FLASH_RISE = 0.4f, FLASH_PEAK = 1.4f;

    public ColtModel() {
        super(SupernaturalCraft.asResource("the_colt"));
    }

    @Override
    public void setCustomAnimations(ColtItem animatable, long instanceId, AnimationState<ColtItem> state) {
        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        if (stack == null || Minecraft.getInstance().level == null) return;
        float partial = state.getPartialTick();
        double now = Minecraft.getInstance().level.getGameTime() + partial;
        int chamber = ColtItem.chamber(stack), ammo = ColtItem.rounds(stack);
        float angle = ColtCylinder.angle(instanceId, chamber, now);
        getBone("cylinder").ifPresent(b -> b.setRotZ(-angle * Mth.DEG_TO_RAD));
        for (int j = 0; j < ColtItem.CAPACITY; j++) {
            boolean shown = ColtReload.roundVisible(j, chamber, ammo, ColtItem.CAPACITY);
            getBone("round_" + j).ifPresent(b -> b.setHidden(!shown));
        }
        double since = ColtClient.sinceShot(instanceId, partial);
        float flash = flashScale(since);
        getBone("muzzle_flash").ifPresent(b -> {
            b.setHidden(flash <= 0.01f);
            b.updateScale(flash, flash, flash * 1.2f);
            // Each shot's flame is turned its own way.
            b.setRotZ((float) ((Double.hashCode(Math.floor(now - since)) & 0xFF) / 255.0 * Mth.TWO_PI));
        });
        boolean reloading = ColtItem.reloading(stack);
        getBone("loose_round").ifPresent(b -> b.setHidden(!reloading));
    }

    /** Muzzle flash size {@code ticks} after the shot: a fast bloom, a slower fade, then nothing. */
    public static float flashScale(double ticks) {
        if (ticks < 0 || ticks > FLASH_TICKS) return 0;
        if (ticks < FLASH_RISE) return (float) (FLASH_PEAK * ticks / FLASH_RISE);
        return (float) (FLASH_PEAK * (1 - (ticks - FLASH_RISE) / (FLASH_TICKS - FLASH_RISE)));
    }
}
