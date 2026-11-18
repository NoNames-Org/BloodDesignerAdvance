package dev.no.name.bdadvance.modes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModesLogicTest {
    @Test
    @DisplayName("Bloodstone: основной и extra-адрес, с портом и без, в любом регистре")
    void bloodstoneAddresses() {
        assertTrue(BloodstoneServer.matches("bloodstone.boo"));
        assertTrue(BloodstoneServer.matches("Bloodstone.boo:25565"));
        assertTrue(BloodstoneServer.matches("extra.bloodstone.boo"));
        assertTrue(BloodstoneServer.matches(" EXTRA.bloodstone.boo:25566 "));
        assertFalse(BloodstoneServer.matches("bloodstone.boo.evil.com"));
        assertFalse(BloodstoneServer.matches("notbloodstone.boo"));
        assertFalse(BloodstoneServer.matches("play.hypixel.net"));
        assertFalse(BloodstoneServer.matches(null));
    }

    @Test
    @DisplayName("избранное: переключение и сохранение в JSON")
    void favoritesToggleAndJson() {
        ModeFavorites favorites = new ModeFavorites();
        assertTrue(favorites.toggle("ffa_smp"));
        assertTrue(favorites.toggle("mace"));
        assertFalse(favorites.toggle("mace"));
        assertTrue(favorites.contains("ffa_smp"));

        ModeFavorites loaded = new ModeFavorites();
        loaded.fromJson(favorites.toJson());
        assertEquals(Set.of("ffa_smp"), loaded.all());
    }

    @Test
    @DisplayName("избранные режимы встают впереди, порядок внутри групп сохраняется")
    void favoritesFirst() {
        ModeFavorites favorites = new ModeFavorites();
        favorites.toggle("practice");
        favorites.toggle("smp");
        List<String> modes = List.of("ffa_smp", "smp", "meetups", "practice", "mace");
        assertEquals(List.of("smp", "practice", "ffa_smp", "meetups", "mace"), favorites.order(modes, id -> id));
    }
}
