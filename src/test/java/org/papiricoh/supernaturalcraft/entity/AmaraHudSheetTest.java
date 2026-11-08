package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraHudSheet;

import static org.junit.jupiter.api.Assertions.assertNull;

/** The HUD sheet's regions stay inside the sheet and never overlap (the art and the HUD share {@link AmaraHudSheet}). */
class AmaraHudSheetTest {

    @Test
    void layoutIsSound() {
        assertNull(AmaraHudSheet.problem());
    }
}
