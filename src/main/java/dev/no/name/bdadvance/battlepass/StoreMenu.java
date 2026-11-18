package dev.no.name.bdadvance.battlepass;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Магазин bloodstone (/donate): главная, кейсы, «Другое», товары ФФА. Раздел «Боевой пропуск» — свой экран
 * (ShopMenu). Страница узнаётся по ключу перевода в заголовке, кнопки — по ключам названий, а не по номерам
 * слотов: одна кнопка на сервере занимает несколько слотов (блок 3×2), берём первый
 */
public final class StoreMenu {
    private static final String TITLE_PREFIX = "bloodstone.inventoryview.";
    private static final String UI = "bloodstone.blooddonate.store.ui.";

    public enum Page {
        MAIN("store", "Магазин", "Донат-товары bloodstone"),
        CASES("cases_menu", "Кейсы", "Косметика для мини-игр и СМП"),
        OTHER("store_other", "Другое", "Размут, проходка на СМП и хост ККБ"),
        FFA("store_ffa", "ФФА", "Койны и личный бустер для ФФА");

        private final String key;
        private final String title;
        private final String subtitle;

        Page(String key, String title, String subtitle) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
        }

        public String title() {
            return title;
        }

        public String subtitle() {
            return subtitle;
        }
    }

    /** Что на странице: категории (переход), товары, «Назад» или «Закрыть», справка о бладиках. */
    public record View(Page page, List<StoreItem> categories, List<StoreItem> products, int backSlot, ItemStack info) {
        public View {
            categories = List.copyOf(categories);
            products = List.copyOf(products);
        }
    }

    private StoreMenu() {
    }

    /** Страница магазина по заголовку; null — это не наш магазин (или раздел БП со своим экраном). */
    public static Page page(Component title) {
        String key = MenuText.key(title);
        if (!key.startsWith(TITLE_PREFIX)) {
            return null;
        }
        String name = key.substring(TITLE_PREFIX.length());
        for (Page page : Page.values()) {
            // «Другое» на сервере — store_other_2: номер версии меню может меняться
            if (name.equals(page.key) || page == Page.OTHER && name.startsWith(page.key)) {
                return page;
            }
        }
        return null;
    }

    public static View read(Page page, AbstractContainerMenu menu) {
        List<StoreItem> categories = new ArrayList<>();
        List<StoreItem> products = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int backSlot = -1;
        ItemStack info = ItemStack.EMPTY;
        int size = Math.max(0, menu.slots.size() - 36);
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = MenuText.stack(menu, slot);
            String key = MenuText.key(stack.getHoverName());
            if (stack.isEmpty() || key.isEmpty() || !seen.add(key + "|" + MenuText.name(stack))) {
                continue;
            }
            if (key.equals(UI + "back.name") || key.equals(UI + "close.name")) {
                backSlot = slot;
            } else if (key.equals(UI + "info.name")) {
                info = stack;
            } else if (key.startsWith(UI + "category.")) {
                categories.add(StoreItem.read(slot, stack));
            } else {
                StoreItem item = StoreItem.read(slot, stack);
                if (item.price() > 0) {
                    products.add(item);
                }
            }
        }
        return new View(page, categories, products, backSlot, info);
    }
}
