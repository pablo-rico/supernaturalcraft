package org.papiricoh.supernaturalcraft.client.heaven;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.client.book.memories.IsoThumbnail;
import org.papiricoh.supernaturalcraft.client.heaven.render.figure.FigurePose;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The client's pure pieces of v0.18: the docket's revision, the scene thumbnails, the figure poses. */
class HeavenClientPureTest {

    @Test
    void docketRevisionStrikesThenReplaces() {
        DocketView d = new DocketView();
        d.foretell("paper_storm, stamp ,precedent");
        assertEquals(List.of("paper_storm", "stamp", "precedent"), d.entries().stream().map(DocketView.Entry::id).toList());
        d.revise(1, "shuffle", 3);
        assertTrue(d.entries().get(1).striking());
        assertEquals("stamp", d.entries().get(1).id());
        d.tick();
        d.tick();
        assertTrue(d.entries().get(1).striking());
        d.tick();
        DocketView.Entry e = d.entries().get(1);
        assertFalse(e.striking());
        assertTrue(e.revised());
        assertEquals("shuffle", e.id());
        d.revise(7, "x", 5);
        assertEquals(3, d.entries().size());
        d.foretell("");
        assertTrue(d.empty());
    }

    @Test
    void thumbnailDrawsTopsAndSides() {
        assertNull(IsoThumbnail.draw(List.of()));
        IsoThumbnail.Image one = IsoThumbnail.draw(List.of(new IsoThumbnail.Voxel(0, 0, 0, 0x808080)));
        assertNotNull(one);
        assertEquals(IsoThumbnail.TILE + 1, one.width());
        // Top lighter than the left side, the left lighter than the right.
        int top = one.at(1, 0) & 0xFF, left = one.at(0, IsoThumbnail.TILE / 2) & 0xFF, right = one.at(IsoThumbnail.TILE - 1, IsoThumbnail.TILE / 2) & 0xFF;
        assertTrue(top > left && left > right, top + " " + left + " " + right);
        // A block under another shows no top: a stack of two is one picture taller.
        IsoThumbnail.Image two = IsoThumbnail.draw(List.of(new IsoThumbnail.Voxel(0, 0, 0, 0x808080), new IsoThumbnail.Voxel(0, 1, 0, 0x808080)));
        assertEquals(one.height() + IsoThumbnail.STEP, two.height());
    }

    @Test
    void posesByName() {
        assertSame(FigurePose.KNEEL, FigurePose.of("Kneel"));
        assertSame(FigurePose.STAND, FigurePose.of("no_such_pose"));
        assertSame(FigurePose.STAND, FigurePose.of(null));
        assertTrue(FigurePose.FALLEN.lying());
        assertTrue(FigurePose.low("sit"));
        assertFalse(FigurePose.low("strike"));
        // With shins the kneeling thigh stays upright; without, the whole leg folds back.
        assertTrue(FigurePose.KNEEL.rightLeg(true).x() > -0.5f);
        assertTrue(FigurePose.KNEEL.rightLeg(false).x() < -1f);
        assertEquals(-FigurePose.STRIKE.rightArm().x(), FigurePose.vanillaX(FigurePose.STRIKE.rightArm()));
    }
}
