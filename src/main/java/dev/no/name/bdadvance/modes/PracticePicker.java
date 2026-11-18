package dev.no.name.bdadvance.modes;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PracticePicker {
    // язык клиента не важен: «Practice #1» / «Практис #1», «Online: 2/175» / «Онлайн: 2/175»
    private static final Pattern ONLINE = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern ROOM_NAME = Pattern.compile("#\\d+");

    public record Option(int slot, String name, List<String> lore) {
        public Option {
            lore = List.copyOf(lore);
        }
    }

    private PracticePicker() {
    }

    public static int pick(List<Option> options, int fallback) {
        int best = fallback;
        int bestFree = 0;
        for (Option option : options) {
            if (!isRoom(option.name())) {
                continue;
            }
            int free = freeSlots(option.lore());
            if (free > bestFree) {
                bestFree = free;
                best = option.slot();
            }
        }
        return best;
    }

    /** Комната Practice: в названии есть «#номер». */
    public static boolean isRoom(String name) {
        return ROOM_NAME.matcher(name).find();
    }

    private static int freeSlots(List<String> lore) {
        for (String line : lore) {
            Matcher matcher = ONLINE.matcher(line);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(2)) - Integer.parseInt(matcher.group(1));
            }
        }
        return 0;
    }
}
