package dev.no.name.bdadvance.battlepass;

import dev.no.name.afriends.compat.Compat;
import dev.no.name.bdadvance.AdvanceFeatures;
import dev.no.name.bdadvance.modes.BloodstoneServer;
import dev.no.name.bdadvance.battlepass.gui.BattlePassScreen;
import dev.no.name.bdadvance.battlepass.gui.QuestsScreen;
import dev.no.name.bdadvance.battlepass.gui.ShopScreen;
import dev.no.name.bdadvance.battlepass.gui.StoreScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/** Подмена серверных меню БП, квестов, магазина и покупки БП своими экранами. Вызывается из миксина на открытие экрана. */
public final class BattlePassHook {
    private BattlePassHook() {
    }

    /**
     * Что открыть вместо {@code next}. Возвращает сам {@code next}, если трогать нечего,
     * {@code null}, если серверное меню нужно просто не показывать (наш экран уже открыт и подхватил
     * новую страницу, или это промежуточное меню магазина), или наш новый экран.
     */
    public static Screen replace(Screen next) {
        if (!(next instanceof AbstractContainerScreen<?> container) || !BloodstoneServer.isConnected()) {
            return next;
        }
        Component title = container.getTitle();
        if (BattlePassNavigator.consumeStore(title, container.getMenu())) {
            return null;
        }
        Screen current = Compat.currentScreen();
        StoreMenu.Page storePage = AdvanceFeatures.enabled(AdvanceFeatures.Feature.STORE) ? StoreMenu.page(title) : null;
        if (storePage != null) {
            if (current instanceof StoreScreen screen) {
                screen.follow(storePage);
                return null;
            }
            return new StoreScreen(storePage);
        }
        if (!AdvanceFeatures.enabled(AdvanceFeatures.Feature.BATTLEPASS)) {
            return next;
        }
        if (BattlePassMenu.isBattlePass(title)) {
            if (current instanceof BattlePassScreen screen) {
                screen.follow(title);
                return null;
            }
            if (BattlePassScreen.consumeStaleReopen()) {
                // игрок уже закрыл меню: не показываем запоздалую страницу и закрываем её на сервере
                Minecraft.getInstance().execute(Compat::closeContainer);
                return null;
            }
            return new BattlePassScreen(title);
        }
        if (QuestMenu.isQuests(title)) {
            return current instanceof QuestsScreen ? null : new QuestsScreen();
        }
        if (ShopMenu.isBattlePassStore(title)) {
            return current instanceof ShopScreen ? null : new ShopScreen(BattlePassNavigator.consumeShopFromLevels());
        }
        return next;
    }
}
