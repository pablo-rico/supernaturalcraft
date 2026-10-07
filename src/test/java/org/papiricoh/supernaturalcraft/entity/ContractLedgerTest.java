package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.ContractLedger;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractLedgerTest {

    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID();

    @Test
    void oneContractPerHunter() {
        ContractLedger l = new ContractLedger();
        assertTrue(l.sign(a, 0, 100));
        assertFalse(l.sign(a, 10, 100), "signed twice");
        assertTrue(l.sign(b, 0, 100));
        assertEquals(2, l.size());
    }

    @Test
    void woundsBurnEveryOpenContract() {
        ContractLedger l = new ContractLedger();
        l.sign(a, 0, 100);
        l.sign(b, 0, 100);
        assertTrue(l.pay(20, 30).isEmpty(), "not enough yet");
        List<UUID> burned = l.pay(10, 30);
        assertEquals(2, burned.size());
        assertEquals(0, l.size());
    }

    @Test
    void anUnpaidContractComesDue() {
        ContractLedger l = new ContractLedger();
        l.sign(a, 0, 100);
        l.pay(10, 30);
        assertTrue(l.due(99).isEmpty());
        assertEquals(1, l.remaining(a, 99));
        assertEquals(List.of(a), l.due(100));
        assertFalse(l.has(a), "a contract that came due stays closed");
    }

    @Test
    void restoreKeepsWhatWasPaid() {
        ContractLedger l = new ContractLedger();
        l.restore(a, 200, 25);
        assertEquals(List.of(a), l.pay(5, 30));
    }
}
