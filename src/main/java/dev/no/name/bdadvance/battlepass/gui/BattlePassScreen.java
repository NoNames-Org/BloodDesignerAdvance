package dev.no.name.bdadvance.battlepass.gui;

import dev.no.name.bdadvance.BdAdvance;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.bdadvance.battlepass.BattlePassMenu;
import dev.no.name.bdadvance.battlepass.BattlePassMenu.Reward;
import dev.no.name.bdadvance.battlepass.BattlePassMenu.View;
import dev.no.name.bdadvance.battlepass.BattlePassNavigator;
import dev.no.name.bdadvance.battlepass.BattlePassProgress;
import dev.no.name.bdadvance.battlepass.BloodBalance;
import dev.no.name.bdadvance.battlepass.QuestMenu;
import dev.no.name.bdadvance.battlepass.QuestText.Quest;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.afriends.gui.animation.Easing;
import dev.no.name.afriends.gui.animation.Motion;
import dev.no.name.bdadvance.battlepass.gui.MenuUi.ButtonStyle;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.afriends.gui.render.text.TextStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Свой экран уровней БП: баннер с картинкой (заголовок, уровень, Premium, задания поверх),
 * полоса уровня, две линии наград, страницы.
 */
public class BattlePassScreen extends MenuScreenBase {
    private static final int WIDTH = 538;
    private static final int GAP = 8;
    private static final int TOP_HEIGHT = 172;
    private static final Identifier BANNER = Identifier.fromNamespaceAndPath(BdAdvance.MOD_ID,
            "textures/gui/battlepass_banner.png");
    private static final int QUEST_PANEL_WIDTH = 186;
    private static final int QUEST_ROW_HEIGHT = 36;
    private static final int QUEST_HEADER_HEIGHT = 22;
    private static final int PREMIUM_CARD_WIDTH = 236;
    private static final int BUY_WIDTH = 64;
    private static final int LINE = 0xFF3A1A20;
    private static final int LEVEL_HEIGHT = 28;
    private static final int LABEL_WIDTH = 25;
    private static final int COLUMNS = BattlePassMenu.LEVELS_PER_PAGE;
    private static final int CELL_WIDTH = (WIDTH - LABEL_WIDTH) / COLUMNS;
    private static final int ROW_HEIGHT = 74;
    private static final int PAGER_HEIGHT = 22;
    private static final int HEIGHT = TOP_HEIGHT + GAP + LEVEL_HEIGHT + GAP + ROW_HEIGHT * 2 + GAP + PAGER_HEIGHT;
    private static final int PAD = 12;
    private static final int PAGE_BUTTONS = 9;
    private static final int PAGE_BUTTON = 22;
    private static final int PAGE_GAP = 5;
    private static final long PULSE_PERIOD = 1400;
    private static final long PAGE_STEP_TIMEOUT_MS = 1500;
    private static final int PREMIUM_CARD_HEIGHT = 56;

    /**
     * Цель листания живёт вне экрана: при переходе на страницу сервер может закрыть меню и открыть новое,
     * и тогда наш экран создаётся заново. Без этого переход «1 → 3» останавливался на 2.
     */
    private static int targetPage;
    private static int steppedFrom = -1;
    private static long steppedAt;
    private static long targetSetAt;
    private static final long TARGET_LIFETIME_MS = 15_000;
    /** Когда игрок закрыл меню посреди листания: ответ сервера на последний шаг ещё может прийти. */
    private static long closedWhilePagingAt;

    private Component serverTitle;
    /** Прокрутка списка заданий на баннере, в пикселях холста. */
    private float questScroll;

    public BattlePassScreen(Component serverTitle) {
        super("Батл пасс");
        this.serverTitle = serverTitle;
        if (Motion.now() - targetSetAt > TARGET_LIFETIME_MS) {
            targetPage = 0;
        }
    }

    /**
     * Игрок закрыл меню сам: листание отменяем, чтобы при следующем открытии оно не продолжилось.
     * Если шаг уже отправлен, сервер вот-вот откроет следующую страницу — {@link #consumeStaleReopen()} её погасит.
     */
    @Override
    public void onClose() {
        if (targetPage > 0 && Motion.now() - steppedAt < PAGE_STEP_TIMEOUT_MS) {
            closedWhilePagingAt = Motion.now();
        }
        targetPage = 0;
        steppedFrom = -1;
        super.onClose();
    }

    /**
     * Меню БП открылось сразу после того, как игрок закрыл его посреди листания: это ответ сервера
     * на последний шаг, а не новое открытие. Срабатывает один раз.
     */
    public static boolean consumeStaleReopen() {
        boolean stale = closedWhilePagingAt > 0 && Motion.now() - closedWhilePagingAt < PAGE_STEP_TIMEOUT_MS;
        closedWhilePagingAt = 0;
        return stale;
    }

    /** Сервер открыл следующую страницу: остаёмся на этом экране и просто берём новый заголовок. */
    public void follow(Component title) {
        serverTitle = title;
    }

    @Override
    protected int canvasWidth() {
        return WIDTH;
    }

    @Override
    protected int canvasHeight() {
        return HEIGHT;
    }

    private View view() {
        AbstractContainerMenu menu = menu();
        return menu == null ? null : BattlePassMenu.read(menu, serverTitle);
    }

    // ---------- раскладка ----------

    private int levelTop() {
        return top + TOP_HEIGHT + GAP;
    }

    private int gridTop() {
        return levelTop() + LEVEL_HEIGHT + GAP;
    }

    private int rowTop(boolean premium) {
        return gridTop() + (premium ? ROW_HEIGHT : 0);
    }

    private int cellX(int column) {
        return left + LABEL_WIDTH + column * CELL_WIDTH;
    }

    private int pagerTop() {
        return gridTop() + ROW_HEIGHT * 2 + GAP;
    }

    // панель заданий лежит на баннере справа
    private int questX() {
        return left + WIDTH - PAD - QUEST_PANEL_WIDTH;
    }

    private int questTop() {
        return top + PAD;
    }

    private int questHeight() {
        return TOP_HEIGHT - PAD * 2;
    }

    private int questListTop() {
        return questTop() + QUEST_HEADER_HEIGHT;
    }

    private int questListBottom() {
        return allQuestsY() - 6;
    }

    private int premiumCardY() {
        return top + TOP_HEIGHT - PAD - PREMIUM_CARD_HEIGHT;
    }

    private int buyX() {
        return left + PAD + PREMIUM_CARD_WIDTH - 10 - BUY_WIDTH;
    }

    private int buyY() {
        return premiumCardY() + PREMIUM_CARD_HEIGHT - 10 - MenuUi.BUTTON_HEIGHT;
    }

    private int allQuestsY() {
        return questTop() + questHeight() - 8 - MenuUi.BUTTON_HEIGHT;
    }

    // крестик и баланс — у левого края панели заданий
    private int closeX() {
        return questX() - 8 - MenuUi.BUTTON_HEIGHT;
    }

    // ---------- отрисовка ----------

    @Override
    protected void drawCanvas(Gfx g, int mouseX, int mouseY) {
        View view = view();
        if (view == null) {
            return;
        }
        stepTowardsTarget(view);
        drawBanner(g, view, mouseX, mouseY);
        drawQuestPanel(g, mouseX, mouseY);
        drawLevel(g, view.progress());
        drawGrid(g, view, mouseX, mouseY);
        drawPager(g, view, mouseX, mouseY);
        g.nextStratum();
        drawItems(g, view);
        g.nextStratum();
        drawCellBadges(g, view);
    }

    private void drawBanner(Gfx g, View view, int mouseX, int mouseY) {
        MenuUi.pixelBanner(g, BANNER, left, top, WIDTH, TOP_HEIGHT);
        MenuUi.display(g, "БАТЛ ПАСС", left + PAD + 2, top + PAD + 2, 2.4f, Theme.TEXT);
        BattlePassProgress progress = view.progress();
        Draw.text(g, "Твой уровень", left + PAD + 3, top + 48, Colors.fade(Theme.TEXT, 0.75f));
        String level = String.valueOf(progress.level());
        MenuUi.display(g, level, left + PAD + 2, top + 60, 1.8f, Theme.TEXT);
        if (progress.maxLevel() > 0) {
            Draw.text(g, "/ " + progress.maxLevel(), left + PAD + 8 + MenuUi.displayWidth(level, 1.8f),
                    top + 68, Colors.fade(Theme.TEXT, 0.6f));
        }
        boolean closeHovered = inside(mouseX, mouseY, closeX(), top + PAD, MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, closeX(), top + PAD, MenuUi.BUTTON_HEIGHT, Icon.CLOSE, null, ButtonStyle.OUTLINE, true, closeHovered);
        MenuUi.balance(g, closeX() - 6, top + PAD, BloodBalance.current());
        drawPremiumCard(g, progress, mouseX, mouseY);
    }

    private void drawPremiumCard(Gfx g, BattlePassProgress progress, int mouseX, int mouseY) {
        int x = left + PAD;
        int y = premiumCardY();
        int w = PREMIUM_CARD_WIDTH;
        Draw.gradient(g, x, y, w, PREMIUM_CARD_HEIGHT, Theme.RADIUS + 2, 0xD9781220, 0xD928080E);
        Draw.border(g, x, y, w, PREMIUM_CARD_HEIGHT, Theme.RADIUS + 2, Colors.fade(MenuUi.GOLD, 0.5f));
        Draw.icon(g, Icon.CROWN, x + 10, y + 9, Theme.ICON_SMALL, MenuUi.GOLD);
        Draw.text(g, "ПРЕМИУМ", x + 22, y + 9, MenuUi.GOLD, TextStyle.DISPLAY);
        if (progress.premiumActive()) {
            float badgeX = x + 28 + Draw.width("ПРЕМИУМ", TextStyle.DISPLAY);
            float badgeW = Draw.width("АКТИВЕН", TextStyle.SMALL) + 8;
            Draw.box(g, badgeX, y + 8, badgeW, 10, 5, Colors.fade(MenuUi.GREEN, 0.15f), MenuUi.GREEN);
            Draw.text(g, "АКТИВЕН", badgeX + 4, y + 10, MenuUi.GREEN, TextStyle.SMALL);
            Draw.text(g, "Уровень Premium " + progress.premiumLevel(), x + 10, y + 28, Theme.TEXT, TextStyle.BODY_MEDIUM);
            String xp = progress.premiumXp() + " / " + progress.premiumXpNeeded();
            Draw.text(g, xp, x + w - 10 - Draw.width(xp), y + 28, MenuUi.GOLD);
            MenuUi.progressBar(g, x + 10, y + 42, w - 20, 5, progress.premiumFill(), MenuUi.GOLD);
            return;
        }
        Draw.text(g, "Вторая линия наград", x + 10, y + 26, Colors.fade(Theme.TEXT, 0.9f));
        Draw.text(g, "до 100 уровня", x + 10, y + 37, Colors.fade(Theme.TEXT, 0.9f));
        boolean hovered = inside(mouseX, mouseY, buyX(), buyY(), BUY_WIDTH, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, buyX(), buyY(), BUY_WIDTH, Icon.CROWN, "Купить", ButtonStyle.GOLD, true, hovered);
    }

    private void drawQuestPanel(Gfx g, int mouseX, int mouseY) {
        int x = questX();
        int y = questTop();
        int pad = 10;
        int w = QUEST_PANEL_WIDTH - pad * 2;
        Draw.roundedRect(g, x, y, QUEST_PANEL_WIDTH, questHeight(), Theme.PANEL_RADIUS, 0xE0160A0D);
        Draw.border(g, x, y, QUEST_PANEL_WIDTH, questHeight(), Theme.PANEL_RADIUS, LINE);
        Draw.text(g, "Задания", x + pad, y + 8, Theme.TEXT, TextStyle.BODY_MEDIUM);
        List<Quest> quests = sortedQuests();
        if (!quests.isEmpty()) {
            long done = quests.stream().filter(Quest::done).count();
            String summary = "выполнено " + done + " из " + quests.size();
            Draw.text(g, summary, x + QUEST_PANEL_WIDTH - pad - Draw.width(summary, TextStyle.SMALL), y + 9, MenuUi.GREEN,
                    TextStyle.SMALL);
        }
        Draw.rect(g, x + 1, questListTop() - 1, QUEST_PANEL_WIDTH - 2, 1, LINE);
        if (quests.isEmpty()) {
            int hintY = questListTop() + 8;
            for (String line : MenuUi.wrap("Открой «Все задания», и здесь появится твой прогресс.", w, 3, TextStyle.BODY)) {
                Draw.text(g, line, x + pad, hintY, Theme.TEXT_DIM, TextStyle.BODY);
                hintY += 11;
            }
        } else {
            questScroll = Math.max(0, Math.min(questScroll, maxQuestScroll(quests.size())));
            g.enableScissor(x, questListTop(), x + QUEST_PANEL_WIDTH, questListBottom());
            float rowY = questListTop() + 6 - questScroll;
            for (Quest quest : quests) {
                if (rowY + QUEST_ROW_HEIGHT > questListTop() && rowY < questListBottom()) {
                    drawQuest(g, quest, x + pad, rowY, w);
                }
                rowY += QUEST_ROW_HEIGHT;
            }
            g.disableScissor();
        }
        boolean hovered = inside(mouseX, mouseY, x + pad, allQuestsY(), w, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, x + pad, allQuestsY(), w, Icon.LIST, "Все задания", ButtonStyle.LIGHT, true, hovered);
    }

    // одна строка задания: группа, название, полоса, награда и бонус серии, счётчик
    private static void drawQuest(Gfx g, Quest quest, float x, float y, int w) {
        Draw.text(g, Draw.trim(quest.kind().toUpperCase(Locale.ROOT), w, TextStyle.SMALL), x, y, Theme.TEXT_FAINT,
                TextStyle.SMALL);
        String count = quest.progress() + "/" + quest.goal();
        Draw.text(g, Draw.trim(quest.title(), w - (int) Draw.width(count) - 6), x, y + 8,
                quest.done() ? MenuUi.GREEN : Theme.TEXT);
        Draw.text(g, count, x + w - Draw.width(count), y + 8, Theme.TEXT_SOFT);
        MenuUi.progressBar(g, x, y + 19, w, 3, quest.fill(), quest.done() ? MenuUi.GREEN : MenuUi.BLOOD);
        Draw.text(g, quest.reward(), x, y + 25, MenuUi.GOLD, TextStyle.SMALL);
        if (!quest.bonus().isEmpty()) {
            Draw.text(g, "серия " + quest.bonus(), x + Draw.width(quest.reward(), TextStyle.SMALL) + 8, y + 25,
                    Theme.TEXT_DIM, TextStyle.SMALL);
        }
    }

    private float maxQuestScroll(int count) {
        return Math.max(0, count * QUEST_ROW_HEIGHT + 6 - (questListBottom() - questListTop()));
    }

    /** Задания из последнего открытия меню квестов: сначала невыполненные. */
    private static List<Quest> sortedQuests() {
        List<Quest> quests = new ArrayList<>(QuestMenu.lastSeen());
        quests.sort(Comparator.comparing(Quest::done));
        return quests;
    }

    private void drawLevel(Gfx g, BattlePassProgress progress) {
        int y = levelTop();
        MenuUi.panel(g, left, y, WIDTH, LEVEL_HEIGHT);
        int barX = left + 8;
        int barW = WIDTH - 16;
        int barH = 16;
        int barY = y + (LEVEL_HEIGHT - barH) / 2;
        Draw.roundedRect(g, barX, barY, barW, barH, Theme.RADIUS, 0x66000000);
        String label = "Уровень " + progress.level();
        float pill = Draw.width(label, TextStyle.BODY_MEDIUM) + 16;
        float fillW = Math.max(pill, pill + (barW - pill) * progress.fill());
        Draw.gradientHorizontal(g, barX, barY, fillW, barH, Theme.RADIUS, MenuUi.BLOOD_DEEP, MenuUi.BLOOD);
        Draw.text(g, label, barX + 8, barY + (barH - 8) / 2f, Theme.TEXT, TextStyle.BODY_MEDIUM);
        String xp = "Опыт " + progress.xp() + " / " + progress.xpNeeded();
        Draw.text(g, xp, barX + barW - 8 - Draw.width(xp), barY + (barH - 8) / 2f, Theme.TEXT);
    }

    private void drawGrid(Gfx g, View view, int mouseX, int mouseY) {
        int y = gridTop();
        MenuUi.panel(g, left, y, WIDTH, ROW_HEIGHT * 2);
        Draw.roundedRect(g, left + 1, rowTop(true), WIDTH - 2, ROW_HEIGHT - 1, Theme.PANEL_RADIUS - 1,
                Colors.fade(MenuUi.WINE, 0.7f));
        Draw.rect(g, left + 1, y + 1, LABEL_WIDTH - 1, ROW_HEIGHT - 1, 0x33000000);
        Draw.rect(g, left + 1, rowTop(true), WIDTH - 2, 1, Theme.BORDER_HOVER);
        Draw.rect(g, left + LABEL_WIDTH, y + 1, 1, ROW_HEIGHT * 2 - 2, Theme.BORDER_HOVER);
        MenuUi.verticalText(g, "Обычная", left + LABEL_WIDTH / 2f, rowTop(false) + ROW_HEIGHT / 2f, Theme.TEXT_SOFT,
                TextStyle.BODY_MEDIUM);
        MenuUi.verticalText(g, "Премиум", left + LABEL_WIDTH / 2f, rowTop(true) + ROW_HEIGHT / 2f, MenuUi.GOLD,
                TextStyle.BODY_MEDIUM);
        for (int column = 1; column < COLUMNS; column++) {
            for (boolean premium : new boolean[]{false, true}) {
                Draw.rect(g, cellX(column), rowTop(premium) + 6, 1, ROW_HEIGHT - 12, Colors.fade(Theme.BORDER_HOVER, 0.7f));
            }
        }
        drawRow(g, view.free(), false, view.progress(), mouseX, mouseY);
        drawRow(g, view.premium(), true, view.progress(), mouseX, mouseY);
        if (view.premiumEnded() && view.premium().isEmpty() && !view.free().isEmpty()) {
            String ended = "Premium-линия закончилась на 100 уровне";
            float w = Draw.width(ended) + 20;
            float cx = left + LABEL_WIDTH + (WIDTH - LABEL_WIDTH) / 2f;
            Draw.roundedRect(g, cx - w / 2f, rowTop(true) + ROW_HEIGHT / 2f - 9, w, 18, 9, 0x66000000);
            Draw.text(g, ended, cx - w / 2f + 10, rowTop(true) + ROW_HEIGHT / 2f - 4, MenuUi.GOLD);
        }
    }

    private void drawRow(Gfx g, List<Reward> rewards, boolean premium, BattlePassProgress progress, int mouseX, int mouseY) {
        int rowY = rowTop(premium);
        float pulse = Easing.pulse(Motion.now(), PULSE_PERIOD);
        for (int i = 0; i < rewards.size() && i < COLUMNS; i++) {
            Reward reward = rewards.get(i);
            int x = cellX(reward.column());
            boolean hovered = inside(mouseX, mouseY, x, rowY, CELL_WIDTH, ROW_HEIGHT);
            boolean available = isAvailable(reward, premium, progress);
            if (hovered) {
                Draw.roundedRect(g, x + 2, rowY + 2, CELL_WIDTH - 4, ROW_HEIGHT - 4, Theme.RADIUS, Colors.fade(Theme.TEXT, 0.05f));
            }
            if (available) {
                int accent = premium ? MenuUi.GOLD : Theme.TEXT;
                Draw.border(g, x + 2, rowY + 2, CELL_WIDTH - 4, ROW_HEIGHT - 4, Theme.RADIUS,
                        Colors.fade(accent, 0.35f + 0.45f * pulse));
                float pillW = Draw.width("Забрать", TextStyle.BODY) + 10;
                Draw.roundedRect(g, x + (CELL_WIDTH - pillW) / 2f, rowY + ROW_HEIGHT - 17, pillW, 12, 6, accent);
                Draw.text(g, "Забрать", x + (CELL_WIDTH - pillW) / 2f + 5, rowY + ROW_HEIGHT - 15, 0xFF1A0A0D, TextStyle.BODY);
            }
            if (reward.level() > 0 && !premium) {
                boolean current = reward.level() == progress.level();
                Draw.text(g, String.valueOf(reward.level()), x + 5, rowY + 5, current ? Theme.TEXT : Theme.TEXT_DIM,
                        TextStyle.BODY_MEDIUM);
            }
        }
    }

    private void drawItems(Gfx g, View view) {
        drawRowItems(g, view.free(), false);
        drawRowItems(g, view.premium(), true);
    }

    private void drawRowItems(Gfx g, List<Reward> rewards, boolean premium) {
        int rowY = rowTop(premium);
        float size = 16 * 2.25f;
        for (int i = 0; i < rewards.size() && i < COLUMNS; i++) {
            MenuUi.item(g, minecraft.font, rewards.get(i).stack(), cellX(rewards.get(i).column()) + (CELL_WIDTH - size) / 2f,
                    rowY + (ROW_HEIGHT - size) / 2f + 2, 2.25f);
        }
    }

    private void drawCellBadges(Gfx g, View view) {
        drawRowBadges(g, view.free(), false, view.progress());
        drawRowBadges(g, view.premium(), true, view.progress());
    }

    private void drawRowBadges(Gfx g, List<Reward> rewards, boolean premium, BattlePassProgress progress) {
        int rowY = rowTop(premium);
        for (int i = 0; i < rewards.size() && i < COLUMNS; i++) {
            Reward reward = rewards.get(i);
            int x = cellX(reward.column());
            float badgeX = x + CELL_WIDTH - Theme.ICON_SMALL - 5;
            if (reward.claimed()) {
                Draw.roundedRect(g, x + 2, rowY + 2, CELL_WIDTH - 4, ROW_HEIGHT - 4, Theme.RADIUS, 0x59000000);
                if (!reward.claimedIcon()) {
                    Draw.icon(g, Icon.CHECK, badgeX, rowY + 6, Theme.ICON_SMALL, MenuUi.GREEN);
                }
            } else if (isLocked(reward, premium, progress)) {
                Draw.roundedRect(g, x + 2, rowY + 2, CELL_WIDTH - 4, ROW_HEIGHT - 4, Theme.RADIUS, 0x38000000);
                Draw.icon(g, premium && !progress.premiumActive() ? Icon.CROWN : Icon.LOCK, badgeX, rowY + 6,
                        Theme.ICON_SMALL, premium ? MenuUi.GOLD : Theme.TEXT_DIM);
            }
        }
    }

    private void drawPager(Gfx g, View view, int mouseX, int mouseY) {
        int y = pagerTop();
        int[] pages = pageNumbers(view);
        float total = (pages.length + 2) * (PAGE_BUTTON + PAGE_GAP) - PAGE_GAP;
        float x = left + (WIDTH - total) / 2f;
        drawPageButton(g, x, y, Icon.CHEVRON_LEFT, null, false, view.hasPrev(), mouseX, mouseY);
        x += PAGE_BUTTON + PAGE_GAP;
        for (int page : pages) {
            boolean current = page == view.page();
            drawPageButton(g, x, y, null, page < 0 ? "…" : String.valueOf(page), current, page > 0, mouseX, mouseY);
            x += PAGE_BUTTON + PAGE_GAP;
        }
        drawPageButton(g, x, y, Icon.CHEVRON_RIGHT, null, false, view.hasNext(), mouseX, mouseY);
        if (targetPage > 0 && targetPage != view.page()) {
            String status = "Листаю к странице " + targetPage + "…";
            Draw.text(g, status, left + WIDTH - Draw.width(status, TextStyle.BODY), y + 8, Theme.TEXT_DIM, TextStyle.BODY);
        }
    }

    private void drawPageButton(Gfx g, float x, float y, Icon icon, String label, boolean current, boolean enabled,
                                int mouseX, int mouseY) {
        boolean hovered = enabled && !current && inside(mouseX, mouseY, x, y, PAGE_BUTTON, PAGE_BUTTON);
        int fill = current ? MenuUi.BLOOD : hovered ? Theme.CARD_HOVER : Colors.fade(Theme.CARD, 0.9f);
        int border = current ? MenuUi.BLOOD : hovered ? Theme.BORDER_SELECTED : Theme.BORDER_HOVER;
        int color = current ? Theme.TEXT : enabled ? (hovered ? Theme.TEXT : Theme.TEXT_SOFT) : Theme.TEXT_FAINT;
        Draw.box(g, x, y, PAGE_BUTTON, PAGE_BUTTON, PAGE_BUTTON / 2f, fill, border);
        if (icon != null) {
            Draw.icon(g, icon, x + (PAGE_BUTTON - Theme.ICON_SMALL) / 2f, y + (PAGE_BUTTON - Theme.ICON_SMALL) / 2f,
                    Theme.ICON_SMALL, color);
        } else {
            Draw.text(g, label, x + (PAGE_BUTTON - Draw.width(label)) / 2f, y + (PAGE_BUTTON - 8) / 2f, color);
        }
    }

    /** Номера страниц: первая, пять вокруг текущей, последняя; -1 — многоточие между ними. */
    private static int[] pageNumbers(View view) {
        int total = Math.max(view.totalPages(), view.page());
        if (total <= PAGE_BUTTONS) {
            return IntStream.rangeClosed(1, total).toArray();
        }
        int from = Math.max(2, view.page() - 2);
        int to = Math.min(total - 1, view.page() + 2);
        while (to - from < 4) {
            if (from > 2) {
                from--;
            } else if (to < total - 1) {
                to++;
            } else {
                break;
            }
        }
        List<Integer> pages = new ArrayList<>();
        pages.add(1);
        if (from > 2) {
            pages.add(-1);
        }
        for (int page = from; page <= to; page++) {
            pages.add(page);
        }
        if (to < total - 1) {
            pages.add(-1);
        }
        pages.add(total);
        return pages.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Сервер листает только на одну страницу, поэтому к выбранной идём шагами. Новая страница первое
     * мгновение приходит пустой (без стрелок), поэтому шагаем только когда её содержимое уже пришло.
     */
    private void stepTowardsTarget(View view) {
        AbstractContainerMenu menu = menu();
        if (targetPage <= 0 || menu == null) {
            return;
        }
        if (targetPage == view.page()) {
            targetPage = 0;
            return;
        }
        long now = Motion.now();
        // после клика сервер сразу убирает стрелки в старом окне и лишь потом открывает новое:
        // пока мы в окне, где уже кликнули, просто ждём новое окно
        if (steppedFrom == menu.containerId) {
            if (now - steppedAt > PAGE_STEP_TIMEOUT_MS) {
                targetPage = 0;
            }
            return;
        }
        boolean loaded = view.hasNext() || view.hasPrev() || !view.free().isEmpty();
        if (!loaded) {
            if (steppedAt > 0 && now - steppedAt > PAGE_STEP_TIMEOUT_MS) {
                targetPage = 0;
            }
            return;
        }
        boolean forward = targetPage > view.page();
        if (forward ? !view.hasNext() : !view.hasPrev()) {
            targetPage = 0;
            return;
        }
        steppedFrom = menu.containerId;
        steppedAt = now;
        clickSlot(forward ? BattlePassMenu.NEXT_SLOT : BattlePassMenu.PREV_SLOT, false);
    }

    // ---------- состояние награды ----------

    private static boolean isAvailable(Reward reward, boolean premium, BattlePassProgress progress) {
        return !reward.claimed() && !isLocked(reward, premium, progress);
    }

    private static boolean isLocked(Reward reward, boolean premium, BattlePassProgress progress) {
        if (reward.level() <= 0) {
            return false;
        }
        // Premium качается отдельно с нуля, поэтому у него свой уровень
        int lineLevel = premium ? progress.premiumLevel() : progress.level();
        return reward.level() > lineLevel;
    }

    private Reward rewardAt(View view, double x, double y) {
        for (boolean premium : new boolean[]{false, true}) {
            List<Reward> rewards = premium ? view.premium() : view.free();
            for (int i = 0; i < rewards.size() && i < COLUMNS; i++) {
                if (inside(x, y, cellX(rewards.get(i).column()), rowTop(premium), CELL_WIDTH, ROW_HEIGHT)) {
                    return rewards.get(i);
                }
            }
        }
        return null;
    }

    // ---------- подсказки и ввод ----------

    @Override
    protected void drawTooltips(Gfx g, int canvasX, int canvasY, int screenX, int screenY) {
        View view = view();
        if (view == null) {
            return;
        }
        ItemStack hovered = ItemStack.EMPTY;
        Reward reward = rewardAt(view, canvasX, canvasY);
        if (reward != null) {
            hovered = reward.stack();
        } else if (inside(canvasX, canvasY, left, levelTop(), WIDTH, LEVEL_HEIGHT)) {
            hovered = view.progressItem();
        }
        if (!hovered.isEmpty()) {
            g.itemTooltip(minecraft.font, hovered, screenX, screenY);
        }
    }

    @Override
    protected boolean onCanvasClick(double x, double y) {
        View view = view();
        if (view == null) {
            return false;
        }
        if (inside(x, y, closeX(), top + PAD, MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        if (!view.progress().premiumActive() && inside(x, y, buyX(), buyY(), BUY_WIDTH, MenuUi.BUTTON_HEIGHT)) {
            BattlePassNavigator.openShop();
            return true;
        }
        if (inside(x, y, questX() + 10, allQuestsY(), QUEST_PANEL_WIDTH - 20, MenuUi.BUTTON_HEIGHT)) {
            clickSlot(BattlePassMenu.QUESTS_SLOT, false);
            return true;
        }
        if (clickPager(view, x, y)) {
            return true;
        }
        Reward reward = rewardAt(view, x, y);
        if (reward != null) {
            clickSlot(reward.slot(), true);
            return true;
        }
        return false;
    }

    private boolean clickPager(View view, double x, double y) {
        int top = pagerTop();
        int[] pages = pageNumbers(view);
        float total = (pages.length + 2) * (PAGE_BUTTON + PAGE_GAP) - PAGE_GAP;
        float bx = left + (WIDTH - total) / 2f;
        if (inside(x, y, bx, top, PAGE_BUTTON, PAGE_BUTTON)) {
            goTo(view, view.page() - 1, view.hasPrev());
            return true;
        }
        bx += PAGE_BUTTON + PAGE_GAP;
        for (int page : pages) {
            if (page > 0 && inside(x, y, bx, top, PAGE_BUTTON, PAGE_BUTTON)) {
                goTo(view, page, page != view.page());
                return true;
            }
            bx += PAGE_BUTTON + PAGE_GAP;
        }
        if (inside(x, y, bx, top, PAGE_BUTTON, PAGE_BUTTON)) {
            goTo(view, view.page() + 1, view.hasNext());
            return true;
        }
        return false;
    }

    private void goTo(View view, int page, boolean allowed) {
        if (allowed && page > 0) {
            targetPage = page;
            targetSetAt = Motion.now();
            steppedFrom = -1;
            stepTowardsTarget(view);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        View view = view();
        // над списком заданий колесо листает задания, а не страницы наград
        double canvasX = mouseX / canvasScale();
        double canvasY = mouseY / canvasScale();
        if (inside(canvasX, canvasY, questX(), questListTop(), QUEST_PANEL_WIDTH, questListBottom() - questListTop())) {
            questScroll = Math.max(0, Math.min(questScroll - (float) scrollY * QUEST_ROW_HEIGHT * 0.6f,
                    maxQuestScroll(QuestMenu.lastSeen().size())));
            return true;
        }
        if (view != null && scrollY != 0 && (targetPage <= 0 || targetPage == view.page())) {
            if (scrollY < 0) {
                goTo(view, view.page() + 1, view.hasNext());
            } else {
                goTo(view, view.page() - 1, view.hasPrev());
            }
        }
        return true;
    }
}
