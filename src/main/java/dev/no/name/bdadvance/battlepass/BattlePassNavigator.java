package dev.no.name.bdadvance.battlepass;

import dev.no.name.afriends.compat.Compat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Переходы между меню bloodstone через предметы хотбара лобби: «Магазин» → раздел «Боевой пропуск»
 * (главное меню магазина при этом не показываем) и обратно в уровни БП.
 */
public final class BattlePassNavigator {
    private static final String STORE_ITEM_MODEL = "blooddonate:icons/store_menu";
    private static final String BATTLEPASS_ITEM_MODEL = "minecraft:icons/gold_medal";
    private static final int HOTBAR = 9;
    private static final long TIMEOUT_MS = 5000;

    /**
     * WAIT_ITEM: пока открыто меню БП, сервер подменяет инвентарь своими предметами, поэтому сначала
     * закрываем меню и ждём, когда в хотбаре снова появится настоящий предмет лобби.
     */
    private enum Step { IDLE, WAIT_ITEM, WAIT_STORE, CLICK_SECTION }

    private static Step step = Step.IDLE;
    private static String wantedItem = "";
    private static boolean wantStore;
    private static long deadline;
    private static int storeContainer = -1;
    /** Раздел «Боевой пропуск» открыли мы из уровней БП: «назад» из него ведёт в уровни, а не в магазин. */
    private static boolean shopFromLevels;

    private BattlePassNavigator() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    /** Открыть раздел покупки БП. В лобби для этого есть предмет «Магазин» в хотбаре. */
    public static void openShop() {
        startWithItem(STORE_ITEM_MODEL, true);
    }

    /** Вернуться в уровни БП (из квестов или магазина). */
    public static void openBattlePass() {
        startWithItem(BATTLEPASS_ITEM_MODEL, false);
    }

    private static void startWithItem(String model, boolean store) {
        Compat.closeContainer();
        wantedItem = model;
        wantStore = store;
        step = Step.WAIT_ITEM;
        deadline = now() + TIMEOUT_MS;
    }

    /**
     * Вызывается из подмены экранов: если мы сами открывали магазин, его главное меню не показываем,
     * а на следующем тике жмём раздел «Боевой пропуск».
     */
    public static boolean consumeStore(Component title, AbstractContainerMenu menu) {
        if (step != Step.WAIT_STORE || !ShopMenu.isStore(title)) {
            return false;
        }
        storeContainer = menu.containerId;
        step = Step.CLICK_SECTION;
        return true;
    }

    /** Открылся ли раздел покупки БП из уровней БП (а не из магазина). Сбрасывается при чтении. */
    public static boolean consumeShopFromLevels() {
        boolean fromLevels = shopFromLevels;
        shopFromLevels = false;
        return fromLevels;
    }

    private static void tick() {
        if (step == Step.IDLE) {
            return;
        }
        if (now() > deadline) {
            Step failed = step;
            step = Step.IDLE;
            fail(failed == Step.WAIT_ITEM
                    ? (wantStore ? "Магазин открывается только в лобби" : "Батл пасс открывается только в лобби")
                    : "Сервер не открыл магазин");
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (step == Step.WAIT_ITEM) {
            int slot = player.containerMenu == player.inventoryMenu ? findHotbar(wantedItem) : -1;
            if (slot >= 0) {
                useHotbar(slot);
                step = wantStore ? Step.WAIT_STORE : Step.IDLE;
                deadline = now() + TIMEOUT_MS;
            }
            return;
        }
        if (step != Step.CLICK_SECTION) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId == storeContainer && !MenuText.stack(menu, ShopMenu.STORE_BATTLEPASS_SLOT).isEmpty()) {
            Compat.clickSlot(menu.containerId, ShopMenu.STORE_BATTLEPASS_SLOT);
            shopFromLevels = true;
            step = Step.IDLE;
        }
    }

    private static int findHotbar(String model) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return -1;
        }
        for (int i = 0; i < HOTBAR; i++) {
            if (model.equals(MenuText.model(player.getInventory().getItem(i)))) {
                return i;
            }
        }
        return -1;
    }

    private static void useHotbar(int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        int previous = inventory.getSelectedSlot();
        inventory.setSelectedSlot(slot);
        minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        inventory.setSelectedSlot(previous);
    }

    private static void fail(String message) {
        Compat.localMessage(Component.literal("[Bloodstone] " + message).withStyle(ChatFormatting.RED));
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
