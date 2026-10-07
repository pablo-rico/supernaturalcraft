package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.NarrationJudge;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.NarrationJudge.Order;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.NarrationJudge.Verdict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NarrationJudgeTest {

    @Test
    void runAndStandStillAreEachOthersContradiction() {
        assertEquals(Verdict.OBEYED, NarrationJudge.judge(Order.RUN, 5, 0, false, false), "running when he says run obeys");
        assertEquals(Verdict.CONTRADICTED, NarrationJudge.judge(Order.RUN, 0.2, 0, false, false), "standing still contradicts run");
        assertEquals(Verdict.NEITHER, NarrationJudge.judge(Order.RUN, 1.5, 0, false, false), "a few steps is neither");
        assertEquals(Verdict.OBEYED, NarrationJudge.judge(Order.STAND_STILL, 0.1, 0, false, false));
        assertEquals(Verdict.CONTRADICTED, NarrationJudge.judge(Order.STAND_STILL, 4, 0, false, false), "running contradicts stand still");
    }

    @Test
    void lookingAwayIsContradictedByLookingAtHim() {
        assertEquals(Verdict.CONTRADICTED, NarrationJudge.judge(Order.LOOK_AWAY, 0, 0.95, false, false));
        assertEquals(Verdict.OBEYED, NarrationJudge.judge(Order.LOOK_AWAY, 0, 0.2, false, false));
    }

    @Test
    void jumpAndKneelAreEachOthersContradiction() {
        assertEquals(Verdict.OBEYED, NarrationJudge.judge(Order.JUMP, 0, 0, true, false));
        assertEquals(Verdict.CONTRADICTED, NarrationJudge.judge(Order.JUMP, 0, 0, false, true), "kneeling contradicts jump");
        assertEquals(Verdict.NEITHER, NarrationJudge.judge(Order.JUMP, 0, 0, false, false));
        assertEquals(Verdict.OBEYED, NarrationJudge.judge(Order.KNEEL, 0, 0, false, true));
        assertEquals(Verdict.CONTRADICTED, NarrationJudge.judge(Order.KNEEL, 0, 0, true, false), "jumping contradicts kneel");
        assertEquals(Verdict.OBEYED, NarrationJudge.judge(Order.KNEEL, 0, 0, true, true), "kneeling at the end still obeys");
    }

    @Test
    void neverTheSameLineTwice() {
        for (Order last : Order.values()) {
            for (int roll = -3; roll < 12; roll++) assertNotEquals(last, NarrationJudge.next(last, roll));
        }
    }

    @Test
    void ordersByOrdinal() {
        assertNull(Order.of(-1));
        assertEquals(Order.KNEEL, Order.of(Order.KNEEL.ordinal()));
        assertEquals("narration.supernaturalcraft.chuck.order.stand_still", NarrationJudge.lineKey(Order.STAND_STILL));
    }
}
