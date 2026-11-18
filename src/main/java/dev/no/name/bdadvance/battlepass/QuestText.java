package dev.no.name.bdadvance.battlepass;

import java.util.List;

/**
 * Разбор описания квеста без привязки к языку. Сервер шлёт строки в одном порядке:
 * тип, пусто, описание, пусто, «Прогресс » a / b», …, «Награда » +N XP», «Бонус серии » +N%».
 */
public final class QuestText {
    private static final String ARROW = "»";
    private static final String DONE_MARK = "✔";

    /** Квест в виде, готовом для отрисовки. Награда и бонус — текст после «»», как прислал сервер. */
    public record Quest(String title, String kind, String description, int progress, int goal,
                        String reward, String bonus, boolean done) {
        public float fill() {
            return goal <= 0 ? (done ? 1 : 0) : Math.max(0, Math.min(1, progress / (float) goal));
        }
    }

    private QuestText() {
    }

    public static Quest parse(String name, List<String> lore) {
        String title = name.startsWith(DONE_MARK) ? name.substring(DONE_MARK.length()).strip() : name.strip();
        String kind = line(lore, 0);
        String description = line(lore, 2);
        int progress = 0;
        int goal = 0;
        String reward = "";
        String bonus = "";
        int arrows = 0;
        for (String raw : lore) {
            int arrow = raw.indexOf(ARROW);
            if (arrow < 0) {
                continue;
            }
            String value = raw.substring(arrow + ARROW.length()).strip();
            switch (arrows++) {
                case 0 -> {
                    List<Integer> numbers = BattlePassText.numbers(value);
                    progress = numbers.isEmpty() ? 0 : numbers.get(0);
                    goal = numbers.size() > 1 ? numbers.get(1) : 0;
                }
                case 1 -> reward = value;
                case 2 -> bonus = value;
                default -> {
                }
            }
        }
        boolean done = name.startsWith(DONE_MARK) || goal > 0 && progress >= goal;
        return new Quest(title, kind, description, progress, goal, reward, bonus, done);
    }

    /** «Выполнено » 1 / 9» из описания кнопки «О квестах»: последние два числа. */
    public static int[] completed(String infoLore) {
        List<Integer> numbers = BattlePassText.numbers(infoLore);
        if (numbers.size() < 2) {
            return new int[]{0, 0};
        }
        return new int[]{numbers.get(numbers.size() - 2), numbers.get(numbers.size() - 1)};
    }

    private static String line(List<String> lore, int index) {
        return index < lore.size() ? lore.get(index).strip() : "";
    }
}
