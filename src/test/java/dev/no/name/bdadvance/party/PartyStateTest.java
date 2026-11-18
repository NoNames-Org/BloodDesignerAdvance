package dev.no.name.bdadvance.party;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyStateTest {
    @Test
    @DisplayName("info заполняет состав, роспуск очищает")
    void infoAndClear() {
        PartyState state = new PartyState();
        state.apply(new PartyEvent.Info("Fell", List.of("Fell", "Bob")), 0);
        assertTrue(state.inParty());
        assertTrue(state.isLeader("fell"));
        assertEquals(List.of("Fell", "Bob"), state.members());
        state.apply(new PartyEvent.Disbanded(), 1);
        assertFalse(state.inParty());
        assertEquals("Пати распущена", state.status());
    }

    @Test
    @DisplayName("входящее приглашение живёт минуту и сбрасывается при принятии")
    void incomingInviteExpires() {
        PartyState state = new PartyState();
        state.apply(new PartyEvent.IncomingInvite("Lexi"), 1_000);
        assertEquals("Lexi", state.incomingInvite(30_000));
        assertNull(state.incomingInvite(1_000 + 60_001));
        state.clearInvite();
        assertNull(state.incomingInvite(2_000));
    }

    @Test
    @DisplayName("статус по-русски для известных ответов")
    void statuses() {
        PartyState state = new PartyState();
        state.apply(new PartyEvent.Invited("Bob"), 0);
        assertEquals("Bob приглашён в пати", state.status());
        state.apply(new PartyEvent.InviteSelf(), 0);
        assertEquals("Нельзя пригласить себя", state.status());
        state.apply(new PartyEvent.Other("Bob joined the party!"), 0);
        assertEquals("Bob joined the party!", state.status());
    }
}
