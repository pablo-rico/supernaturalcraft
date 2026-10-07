package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.WordJudge;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.WordJudge.Order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordJudgeTest {

    @Test
    void beStill() {
        assertFalse(WordJudge.disobeys(Order.BE_STILL, 0.2, 1.0, false, false), "a little sway is allowed");
        assertTrue(WordJudge.disobeys(Order.BE_STILL, 0.8, -1.0, true, false), "walking breaks it");
    }

    @Test
    void lookAway() {
        assertFalse(WordJudge.disobeys(Order.LOOK_AWAY, 5, 0.3, false, false), "moving and looking aside is fine");
        assertTrue(WordJudge.disobeys(Order.LOOK_AWAY, 0, 0.95, false, false), "looking at him breaks it");
    }

    @Test
    void kneelIsOnlyJudgedAtTheEnd() {
        assertFalse(WordJudge.disobeys(Order.KNEEL, 0, 1, false, false), "not yet");
        assertTrue(WordJudge.disobeys(Order.KNEEL, 0, 1, false, true), "standing at the end breaks it");
        assertFalse(WordJudge.disobeys(Order.KNEEL, 3, 1, true, true), "kneeling at the end keeps it");
    }

    @Test
    void neverTheSameOrderTwice() {
        for (Order last : Order.values()) {
            for (int roll = 0; roll < 9; roll++) assertNotEquals(last, WordJudge.next(last, roll));
        }
    }
}
