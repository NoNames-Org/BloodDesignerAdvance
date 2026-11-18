package dev.no.name.bdadvance.battlepass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlePassTextTest {
    @Test
    @DisplayName("номер уровня берётся из названия на любом языке")
    void levelFromName() {
        assertEquals(12, BattlePassText.firstNumber("✔ Уровень 12"));
        assertEquals(5, BattlePassText.firstNumber("Level 5"));
        assertEquals(-1, BattlePassText.firstNumber("Закрыть меню"));
    }

    @Test
    @DisplayName("страница — по глифу U+E600+n в заголовке")
    void pageFromGlyph() {
        assertEquals(1, BattlePassText.page(new int[]{0xE500, 0xE600, 0xE501}));
        assertEquals(4, BattlePassText.page(new int[]{0xE500, 0xE603, 0xE501}));
        assertEquals(1, BattlePassText.page(new int[]{0xE500, 0xE511}));
    }

    @Test
    @DisplayName("прогресс: числа в фиксированном порядке, Premium не активирован")
    void progressInactivePremium() {
        String lore = "Базовый уровень: 4 / 1000 35 / 448 XP До следующего: 413 XP Статус » не активирован "
                + "После покупки прокачивается с нуля. Premium: 0 / 100 0 / 0 XP";
        BattlePassProgress p = BattlePassText.progress(lore);
        assertEquals(new BattlePassProgress(4, 1000, 35, 448, 0, 100, 0, 0), p);
        assertFalse(p.premiumActive());
        assertEquals(35 / 448f, p.fill(), 1e-6);
    }

    @Test
    @DisplayName("баланс из строки скорборда: §-коды с цифрами не мешают")
    void balanceFromScoreboard() {
        assertEquals(8, BattlePassText.lastNumber("ぇBlood gems: 8§0§5§r"));
        assertEquals(1250, BattlePassText.lastNumber("ぇБладики: 1250§0§5§r"));
        assertEquals(-1, BattlePassText.lastNumber("§0§2§r"));
    }

    @Test
    @DisplayName("прогресс: активный Premium и неполное описание")
    void progressActiveAndShort() {
        BattlePassProgress active = BattlePassText.progress("Base level: 7 / 1000 10 / 500 XP Next: 490 XP Premium: 3 / 100 20 / 300 XP");
        assertTrue(active.premiumActive());
        assertEquals(3, active.premiumLevel());
        assertEquals(BattlePassProgress.EMPTY, BattlePassText.progress(""));
        assertEquals(List.of(1, 22), BattlePassText.numbers("a1b22"));
    }
}
