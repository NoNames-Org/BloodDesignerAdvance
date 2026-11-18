package dev.no.name.bdadvance.modes;

import dev.no.name.afriends.compat.Compat;
import dev.no.name.bdadvance.ServerText;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModeLauncher {
    private static final String NAVIGATOR_MODEL = "bloodstone:ui/hub_navigator";
    private static final String NAVIGATOR_NAME = "Mode selection";
    private static final String LEAVE_QUEUE_NAME = "Leave the queue";
    private static final String MAIN_MENU_MARK = "bloodstone:ui/mode_selection_label_mask";
    private static final int MAIN_MENU_SLOTS = 90;
    private static final int INFO_SLOT = 8;
    private static final int HOTBAR = 9;

    private static final int PRACTICE_FALLBACK = 11;
    private static final long HUB_TIMEOUT_MS = 15_000;

    private static final long HUB_SETTLE_MS = 1_500;

    private static final long REOPEN_MS = 2_000;
    private static final int MAX_OPEN_ATTEMPTS = 3;
    private static final long STEP_TIMEOUT_MS = 6_000;

    private static final long ANSWER_TIMEOUT_MS = 8_000;

    public enum Status { IDLE, GOING_HUB, OPENING, CHOOSING, WAITING, QUEUED, TRANSFERRED, ERROR }

    private static ModeLauncher active;

    private Status status = Status.IDLE;
    private String modeId;
    private ModeRoute route;
    private String error = "";
    private long deadline;
    private int clickedContainer = -1;

    private long navigatorSeenAt;
    private long lastOpenAt;
    private int openAttempts;

    private boolean hidingMenu;

    public void register() {
        active = this;
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            hidingMenu = false;
            if (status == Status.WAITING || status == Status.QUEUED) {
                finish(Status.TRANSFERRED);
            } else if (status == Status.OPENING || status == Status.CHOOSING) {
                deadline = now() + HUB_TIMEOUT_MS;
                status = Status.GOING_HUB;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            hidingMenu = false;
            status = Status.IDLE;
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                onServerMessage(message.getString());
            }
        });
    }

    public void launch(String id, ModeRoute modeRoute) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        modeId = id;
        route = modeRoute;
        error = "";
        clickedContainer = -1;
        navigatorSeenAt = 0;
        openAttempts = 0;
        if (modeRoute.isCommand()) {
            if (findHotbar(true) >= 0) {
                finish(Status.TRANSFERRED);
                return;
            }
            // после /hub ждать нечего: сервер переносит сам, а навигатору незачем висеть на «Подключаем…»
            sendCommand(modeRoute.command());
            finish(Status.TRANSFERRED);
            return;
        }
        if (findHotbar(true) < 0) {
            sendCommand("hub");
            status = Status.GOING_HUB;
            deadline = now() + HUB_TIMEOUT_MS;
        } else {
            openMenu();
        }
    }

    public void leaveQueue() {
        int slot = findHotbar(false);
        if (slot >= 0) {
            useHotbar(slot);
        }
    }

    public boolean isBusy() {
        return status == Status.GOING_HUB || status == Status.OPENING || status == Status.CHOOSING
                || status == Status.WAITING;
    }

    public Status status() {
        return status;
    }

    public String modeId() {
        return modeId;
    }

    public String error() {
        return error;
    }

    public static boolean hideScreen(Screen next) {
        return active != null && active.shouldHide(next);
    }

    public boolean shouldHide(Screen next) {
        boolean choosing = status == Status.OPENING || status == Status.CHOOSING || status == Status.WAITING;
        if (choosing && next instanceof AbstractContainerScreen<?>) {
            hidingMenu = true;
            return true;
        }
        if (next == null && hidingMenu) {
            hidingMenu = false;
            return true;
        }
        return false;
    }

    private void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || status == Status.IDLE || status == Status.QUEUED || status == Status.ERROR
                || status == Status.TRANSFERRED) {
            return;
        }
        if (now() > deadline) {
            fail(status == Status.GOING_HUB ? "Не удалось попасть в лобби" : "Сервер не ответил");
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        switch (status) {
            case GOING_HUB -> {
                if (findHotbar(true) < 0) {
                    navigatorSeenAt = 0;
                } else if (navigatorSeenAt == 0) {
                    navigatorSeenAt = now();
                } else if (now() - navigatorSeenAt >= HUB_SETTLE_MS) {
                    openMenu();
                }
            }
            case OPENING -> {
                if (!isMainMenu(menu)) {
                    if (now() - lastOpenAt >= REOPEN_MS && openAttempts < MAX_OPEN_ATTEMPTS) {
                        useNavigator();
                    }
                } else {
                    click(menu, route.mainSlot());
                    status = route.hasSubmenu() ? Status.CHOOSING : Status.WAITING;
                    deadline = now() + (route.hasSubmenu() ? STEP_TIMEOUT_MS : ANSWER_TIMEOUT_MS);
                }
            }
            case CHOOSING -> {
                if (menu.containerId != clickedContainer && isSubmenu(menu, route.submenu())) {
                    int slot = route.submenu() == ModeRoute.Submenu.PRACTICE
                            ? PracticePicker.pick(practiceOptions(menu), PRACTICE_FALLBACK)
                            : route.subSlot();
                    click(menu, slot);
                    status = Status.WAITING;
                    deadline = now() + ANSWER_TIMEOUT_MS;
                }
            }
            default -> {
            }
        }
    }

    private void openMenu() {
        if (findHotbar(true) < 0) {
            fail("Нет предмета «Mode selection» в хотбаре");
            return;
        }
        status = Status.OPENING;
        openAttempts = 0;
        deadline = now() + REOPEN_MS * MAX_OPEN_ATTEMPTS + STEP_TIMEOUT_MS;
        useNavigator();
    }

    private void useNavigator() {
        int slot = findHotbar(true);
        if (slot >= 0) {
            openAttempts++;
            lastOpenAt = now();
            useHotbar(slot);
        }
    }

    private void click(AbstractContainerMenu menu, int slot) {
        clickedContainer = menu.containerId;
        Compat.clickSlot(menu.containerId, slot);
    }

    private void onServerMessage(String plain) {
        String text = String.join(" ", ServerText.lines(plain));
        String lower = text.toLowerCase(Locale.ROOT);
        // сервер пишет на языке клиента, поэтому каждое сообщение знаем и на английском, и на русском
        if (lower.startsWith("you are now queued") || lower.startsWith("вы теперь в очереди")) {
            finish(Status.QUEUED);
        } else if (lower.startsWith("you left the queue") || lower.startsWith("вы покинули очередь")) {
            if (status == Status.QUEUED) {
                finish(Status.IDLE);
            }
        } else if ((lower.startsWith("unable to connect to") || lower.startsWith("не удалось подключиться"))
                && status == Status.WAITING) {
            fail(text);
        } else if (lower.startsWith("вы перенесены") && status == Status.WAITING) {
            finish(Status.TRANSFERRED);
        }
    }

    private void finish(Status result) {
        status = result;
        closeHiddenMenu();
    }

    private void fail(String message) {
        error = message;
        finish(Status.ERROR);
    }

    private void closeHiddenMenu() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu != player.inventoryMenu) {
            hidingMenu = true;
            player.closeContainer();
        }
        hidingMenu = false;
    }

    private static boolean isMainMenu(AbstractContainerMenu menu) {
        return menu.slots.size() == MAIN_MENU_SLOTS && menu.slots.stream()
                .anyMatch(slot -> MAIN_MENU_MARK.equals(model(slot.getItem())));
    }

    /**
     * Подменю после нашего клика (CHOOSING уже проверил, что окно новое). Надписи зависят от языка клиента
     * («Meetups» / «Митапы», «Practice #1» / «Практис #1»), а раскладка нет, поэтому названия не сверяем:
     * достаточно, что это не главное меню и оно уже прогрузилось.
     */
    private static boolean isSubmenu(AbstractContainerMenu menu, ModeRoute.Submenu submenu) {
        if (isMainMenu(menu)) {
            return false;
        }
        if (submenu == ModeRoute.Submenu.PRACTICE) {
            return menu.slots.stream().anyMatch(slot -> !(slot.container instanceof Inventory)
                    && PracticePicker.isRoom(name(slot.getItem())));
        }
        return menu.slots.size() > INFO_SLOT && !menu.getSlot(INFO_SLOT).getItem().isEmpty();
    }

    private static List<PracticePicker.Option> practiceOptions(AbstractContainerMenu menu) {
        List<PracticePicker.Option> options = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            ItemLore lore = stack.get(DataComponents.LORE);
            List<String> lines = lore == null ? List.of() : lore.lines().stream().map(Component::getString).toList();
            options.add(new PracticePicker.Option(slot.index, name(stack), lines));
        }
        return options;
    }

    private static int findHotbar(boolean navigator) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return -1;
        }
        for (int i = 0; i < HOTBAR; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            boolean match = navigator
                    ? NAVIGATOR_MODEL.equals(model(stack)) || NAVIGATOR_NAME.equals(name(stack))
                    : LEAVE_QUEUE_NAME.equals(name(stack));
            if (match) {
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

    private static void sendCommand(String command) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand(command);
        }
    }

    private static String model(ItemStack stack) {
        Object model = stack.get(DataComponents.ITEM_MODEL);
        return model == null ? "" : model.toString();
    }

    private static String name(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
