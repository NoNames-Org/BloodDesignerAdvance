package dev.no.name.bdadvance.battlepass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбор текстов меню БП без привязки к языку: берём только числа и коды глифов. */
public final class BattlePassText {
    /** Глифы номера страницы в шрифте bloodlevels:menus идут подряд с U+E600 (страница 1). */
    static final int PAGE_GLYPH_FIRST = 0xE600;
    static final int PAGE_GLYPH_LAST = 0xE6FF;
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private BattlePassText() {
    }

    /** Все целые числа по порядку: «Базовый уровень: 4 / 1000» → [4, 1000]. */
    public static List<Integer> numbers(String text) {
        List<Integer> result = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            try {
                result.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {
                // число длиннее int в описании не встречается; такое просто пропускаем
            }
        }
        return result;
    }

    /** Первое число в строке или -1: «✔ Уровень 12» и «Level 12» дают 12. */
    public static int firstNumber(String text) {
        List<Integer> found = numbers(text);
        return found.isEmpty() ? -1 : found.get(0);
    }

    /** Номер страницы по глифу из заголовка, или 1, если глифа нет. */
    public static int page(int[] menuGlyphs) {
        for (int codepoint : menuGlyphs) {
            if (codepoint >= PAGE_GLYPH_FIRST && codepoint <= PAGE_GLYPH_LAST) {
                return codepoint - PAGE_GLYPH_FIRST + 1;
            }
        }
        return 1;
    }

    /**
     * Прогресс из описания «ТВОЙ ПРОГРЕСС». Числа идут в фиксированном порядке на любом языке:
     * уровень, макс. уровень, опыт, опыт до уровня, осталось, затем то же для Premium (без «осталось»).
     */
    public static BattlePassProgress progress(String lore) {
        List<Integer> n = numbers(lore);
        return new BattlePassProgress(at(n, 0), at(n, 1), at(n, 2), at(n, 3), at(n, 5), at(n, 6), at(n, 7), at(n, 8));
    }

    /** Число из строки скорборда «Blood gems: 8§0§5§r»: убираем §-коды (в них тоже цифры) и берём последнее число. */
    public static int lastNumber(String scoreboardLine) {
        List<Integer> found = numbers(scoreboardLine.replaceAll("§.", ""));
        return found.isEmpty() ? -1 : found.get(found.size() - 1);
    }

    private static int at(List<Integer> values, int index) {
        return index < values.size() ? values.get(index) : 0;
    }
}
