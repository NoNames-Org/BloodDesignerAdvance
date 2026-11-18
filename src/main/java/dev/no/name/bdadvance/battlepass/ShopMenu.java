package dev.no.name.bdadvance.battlepass;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Магазин bloodstone: главное меню и раздел «Боевой пропуск». Узнаём по ключам перевода в заголовке. */
public final class ShopMenu {
    private static final String STORE_KEY = "bloodstone.inventoryview.store";
    private static final String BATTLEPASS_KEY = "bloodstone.inventoryview.store_battlepass";
    /** В главном меню магазина кнопка раздела «Боевой пропуск». */
    public static final int STORE_BATTLEPASS_SLOT = 36;
    public static final int BACK_SLOT = 0;
    private static final int INFO_SLOT = 8;
    private static final int PREMIUM_SLOT = 9;
    private static final int BUNDLE_SLOT = 14;

    public record View(List<StoreItem> products, ItemStack info) {
        public View {
            products = List.copyOf(products);
        }
    }

    private ShopMenu() {
    }

    public static boolean isStore(Component title) {
        return MenuText.hasKey(title, STORE_KEY);
    }

    public static boolean isBattlePassStore(Component title) {
        return MenuText.hasKey(title, BATTLEPASS_KEY);
    }

    public static View read(AbstractContainerMenu menu) {
        List<StoreItem> products = new ArrayList<>();
        for (int slot : new int[]{PREMIUM_SLOT, BUNDLE_SLOT}) {
            ItemStack stack = MenuText.stack(menu, slot);
            if (!stack.isEmpty()) {
                products.add(StoreItem.read(slot, stack));
            }
        }
        return new View(products, MenuText.stack(menu, INFO_SLOT));
    }
}
