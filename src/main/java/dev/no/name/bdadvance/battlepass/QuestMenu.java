package dev.no.name.bdadvance.battlepass;

import dev.no.name.bdadvance.battlepass.QuestText.Quest;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Меню квестов bloodstone. В заголовке тот же шрифт bloodlevels:menus с глифом U+E511.
 * Квесты лежат рядами: каждый ряд — своя группа (ежедневные и еженедельные в сундуке,
 * ежемесячные в слотах инвентаря).
 */
public final class QuestMenu {
    private static final int QUESTS_GLYPH = 0xE511;
    private static final int CLOSE_SLOT = 0;
    private static final int INFO_SLOT = 8;
    private static final int ROW = 9;

    /** Квест и слот, по которому он лежит в серверном меню. */
    public record Entry(int slot, ItemStack stack, Quest quest) {
    }

    /** Группа квестов одного ряда. Название берём из первой строки описания, как прислал сервер. */
    public record Group(String title, List<Entry> entries) {
        public Group {
            entries = List.copyOf(entries);
        }
    }

    public record View(List<Group> groups, ItemStack info, int done, int total) {
        public View {
            groups = List.copyOf(groups);
        }
    }

    private static volatile List<Quest> lastSeen = List.of();

    private QuestMenu() {
    }

    public static boolean isQuests(Component title) {
        for (int glyph : MenuText.glyphs(title, BattlePassMenu.MENU_FONT)) {
            if (glyph == QUESTS_GLYPH) {
                return true;
            }
        }
        return false;
    }

    /** Последние увиденные квесты: показываем их на панели в БП, где своих данных о квестах нет. */
    public static List<Quest> lastSeen() {
        return lastSeen;
    }

    public static View read(AbstractContainerMenu menu) {
        Map<Integer, List<Entry>> rows = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || slot.index == CLOSE_SLOT || slot.index == INFO_SLOT
                    || MenuText.loreLines(stack).isEmpty()) {
                continue;
            }
            // ежемесячные сервер кладёт в слоты инвентаря; квестом считаем только то, где есть прогресс и награда
            Quest quest = QuestText.parse(MenuText.name(stack), MenuText.loreStrings(stack));
            if (quest.goal() <= 0 || quest.reward().isEmpty()) {
                continue;
            }
            rows.computeIfAbsent(slot.index / ROW, row -> new ArrayList<>()).add(new Entry(slot.index, stack, quest));
        }
        List<Group> groups = new ArrayList<>();
        List<Quest> all = new ArrayList<>();
        for (List<Entry> entries : rows.values()) {
            groups.add(new Group(groupTitle(entries.get(0).quest().kind()), entries));
            entries.forEach(entry -> all.add(entry.quest()));
        }
        if (!all.isEmpty()) {
            lastSeen = List.copyOf(all);
        }
        ItemStack info = MenuText.stack(menu, INFO_SLOT);
        int[] completed = QuestText.completed(MenuText.loreJoined(info));
        return new View(groups, info, completed[0], completed[1]);
    }

    public static int closeSlot() {
        return CLOSE_SLOT;
    }

    /** «Ежедневное задание» → «Ежедневное»: короткая подпись группы; если слово одно — как есть. */
    private static String groupTitle(String kind) {
        int space = kind.indexOf(' ');
        return space > 0 ? kind.substring(0, space) : kind;
    }
}
