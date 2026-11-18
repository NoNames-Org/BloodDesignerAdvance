package dev.no.name.bdadvance.battlepass;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню уровней bloodstone (БП). Узнаём его по шрифтам ресурспака в заголовке, а не по тексту,
 * поэтому всё работает и на русском, и на английском клиенте.
 */
public final class BattlePassMenu {
    static final String MENU_FONT = "bloodlevels:menus";
    private static final String PROGRESS_FONT = "bloodlevels:menu_progress";
    /** После 100 уровня Premium-линии нет: заголовок с этим шрифтом, бесплатная линия на ряд ниже. */
    private static final String FREE_ONLY_FONT = "bloodlevels:menu_progress_free_only";
    private static final String CLAIMED_MODEL = "bloodlevels:claimed";
    private static final String CLAIMED_MARK = "✔";

    static final int INFO_SLOT = 8;
    static final int FREE_FIRST = 27;
    static final int FREE_ONLY_FIRST = 45;
    static final int PREMIUM_FIRST = 54;
    public static final int LEVELS_PER_PAGE = 9;
    static final int PROGRESS_SLOT = 74;
    public static final int QUESTS_SLOT = 78;
    public static final int PREV_SLOT = 81;
    public static final int NEXT_SLOT = 88;
    private static final int MIN_SLOTS = NEXT_SLOT + 1;

    /**
     * Одна награда: слот в серверном меню, предмет с сервера и разобранное состояние.
     * {@code column} — столбец 0..8 по номеру уровня: сервер не всегда кладёт награду в «свою» ячейку.
     */
    public record Reward(int slot, ItemStack stack, int level, int column, boolean claimed, boolean claimedIcon) {
    }

    /** Снимок меню на этом кадре. */
    public record View(int page, List<Reward> free, List<Reward> premium, boolean premiumEnded, BattlePassProgress progress,
                       ItemStack info, ItemStack progressItem, ItemStack quests, boolean hasPrev, boolean hasNext) {
        public View {
            free = List.copyOf(free);
            premium = List.copyOf(premium);
        }

        public int totalPages() {
            int max = progress.maxLevel();
            return max <= 0 ? 0 : (max + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE;
        }
    }

    private BattlePassMenu() {
    }

    public static boolean isBattlePass(Component title) {
        return MenuText.hasFont(title, PROGRESS_FONT);
    }

    public static int page(Component title) {
        return BattlePassText.page(MenuText.glyphs(title, MENU_FONT));
    }

    public static View read(AbstractContainerMenu menu, Component title) {
        if (menu.slots.size() < MIN_SLOTS) {
            return new View(page(title), List.of(), List.of(), false, BattlePassProgress.EMPTY,
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, false, false);
        }
        ItemStack progressItem = MenuText.stack(menu, PROGRESS_SLOT);
        int page = page(title);
        boolean freeOnly = MenuText.hasFont(title, FREE_ONLY_FONT);
        List<Reward> premium = freeOnly ? List.of() : line(menu, PREMIUM_FIRST, page);
        return new View(page, line(menu, freeOnly ? FREE_ONLY_FIRST : FREE_FIRST, page), premium,
                freeOnly || premium.size() < LEVELS_PER_PAGE,
                BattlePassText.progress(MenuText.loreJoined(progressItem)), MenuText.stack(menu, INFO_SLOT), progressItem,
                MenuText.stack(menu, QUESTS_SLOT), !MenuText.stack(menu, PREV_SLOT).isEmpty(),
                !MenuText.stack(menu, NEXT_SLOT).isEmpty());
    }

    private static List<Reward> line(AbstractContainerMenu menu, int first, int page) {
        int firstLevel = (page - 1) * LEVELS_PER_PAGE + 1;
        List<Reward> rewards = new ArrayList<>();
        for (int i = 0; i < LEVELS_PER_PAGE; i++) {
            int slot = first + i;
            ItemStack stack = MenuText.stack(menu, slot);
            if (stack.isEmpty()) {
                continue;
            }
            String name = MenuText.name(stack);
            boolean claimedIcon = CLAIMED_MODEL.equals(MenuText.model(stack));
            boolean claimed = claimedIcon || name.startsWith(CLAIMED_MARK);
            int level = BattlePassText.firstNumber(name);
            int column = level > 0 ? level - firstLevel : i;
            column = column >= 0 && column < LEVELS_PER_PAGE ? column : i;
            rewards.add(new Reward(slot, stack, level, column, claimed, claimedIcon));
        }
        return rewards;
    }
}
