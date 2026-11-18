package dev.no.name.bdadvance.modes;

import dev.no.name.bdadvance.modes.PracticePicker.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModeRouteTest {
    @Test
    @DisplayName("Practice: выбираем сервер, где больше свободных мест")
    void practiceMostFree() {
        List<Option> options = List.of(
                new Option(11, "Practice #1", List.of("", "Online: 85/175", "", "Click to play")),
                new Option(15, "Practice #2", List.of("", "Online: 30/175", "", "Click to play")));
        assertEquals(15, PracticePicker.pick(options, 11));
    }

    @Test
    @DisplayName("Practice: без онлайна в описании — запасной слот (#1)")
    void practiceFallback() {
        List<Option> options = List.of(new Option(11, "Practice #1", List.of("Click to play")),
                new Option(15, "Something", List.of("Online: 1/175")));
        assertEquals(11, PracticePicker.pick(options, 11));
        assertEquals(11, PracticePicker.pick(List.of(), 11));
    }

    @Test
    @DisplayName("Practice на русском клиенте: «Практис #N» и «Онлайн: x/y»")
    void practiceRussian() {
        List<Option> options = List.of(
                new Option(11, "Практис #1", List.of("", "Онлайн: 2/175", "", "Нажми, чтобы играть")),
                new Option(15, "Практис #2", List.of("", "Онлайн: 0/175", "", "Нажми, чтобы играть")));
        assertEquals(15, PracticePicker.pick(options, 11));
        assertEquals(true, PracticePicker.isRoom("Практис #2"));
        assertEquals(false, PracticePicker.isRoom("Назад"));
    }

    @Test
    @DisplayName("Practice: полный сервер не выбираем")
    void practiceSkipsFull() {
        List<Option> options = List.of(
                new Option(11, "Practice #1", List.of("Online: 175/175")),
                new Option(15, "Practice #2", List.of("Online: 170/175")));
        assertEquals(15, PracticePicker.pick(options, 11));
    }
}
