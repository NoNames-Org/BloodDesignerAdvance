package dev.no.name.bdadvance.battlepass;

import dev.no.name.bdadvance.battlepass.QuestText.Quest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTextTest {
    @Test
    @DisplayName("квест в процессе: описание, прогресс, награда и бонус серии")
    void inProgress() {
        Quest quest = QuestText.parse("Выживание", List.of("Ежедневное задание", "", "Выживи 15 минут в обычном КБ.", "",
                "Прогресс » 3 / 15", "", "[[[[[[[[[ 20%", "", "Награда » +100 XP", "Бонус серии » +0%"));
        assertEquals("Выживание", quest.title());
        assertEquals("Ежедневное задание", quest.kind());
        assertEquals("Выживи 15 минут в обычном КБ.", quest.description());
        assertEquals(3, quest.progress());
        assertEquals(15, quest.goal());
        assertEquals("+100 XP", quest.reward());
        assertEquals("+0%", quest.bonus());
        assertFalse(quest.done());
        assertEquals(0.2f, quest.fill(), 1e-6);
    }

    @Test
    @DisplayName("выполненный квест: галочка убирается из названия, на английском тоже")
    void doneAndEnglish() {
        Quest quest = QuestText.parse("✔ Double hit", List.of("Weekly quest", "", "Kill two players in one round.", "",
                "Progress » 2 / 2", "", "[[[ 100%", "", "Reward » +500 XP", "Streak bonus » +0%", "✔ Done · XP granted"));
        assertEquals("Double hit", quest.title());
        assertTrue(quest.done());
        assertEquals("+500 XP", quest.reward());
        assertEquals(1f, quest.fill(), 1e-6);
    }

    @Test
    @DisplayName("«Выполнено » 1 / 9» из описания кнопки «О квестах»")
    void completedCounter() {
        assertArrayEquals(new int[]{1, 9}, QuestText.completed("Ежедневные • новые каждый день Выполнено » 1 / 9 Опыт начисляется автоматически."));
        assertArrayEquals(new int[]{0, 0}, QuestText.completed("нет чисел"));
    }
}
