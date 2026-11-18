package dev.no.name.bdadvance.battlepass.gui;

import dev.no.name.bdadvance.BdAdvance;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.bdadvance.battlepass.BattlePassNavigator;
import dev.no.name.bdadvance.battlepass.BloodBalance;
import dev.no.name.bdadvance.battlepass.QuestMenu;
import dev.no.name.bdadvance.battlepass.QuestMenu.Entry;
import dev.no.name.bdadvance.battlepass.QuestMenu.Group;
import dev.no.name.bdadvance.battlepass.QuestMenu.View;
import dev.no.name.bdadvance.battlepass.QuestText.Quest;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.bdadvance.battlepass.gui.MenuUi.ButtonStyle;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.afriends.gui.render.text.TextStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Свой экран квестов: группы (ежедневные, еженедельные…) карточками с прогрессом и наградой. */
public class QuestsScreen extends MenuScreenBase {
    private static final int WIDTH = 538;
    private static final int GAP = 8;
    private static final int BANNER_HEIGHT = 72;
    private static final int GROUP_TITLE = 18;
    private static final int COLUMNS = 3;
    private static final int CARD_WIDTH = (WIDTH - GAP * (COLUMNS - 1)) / COLUMNS;
    private static final int CARD_HEIGHT = 92;
    private static final int MAX_GROUPS = 3;
    private static final int HEIGHT = BANNER_HEIGHT + MAX_GROUPS * (GAP + GROUP_TITLE + CARD_HEIGHT);
    private static final int PAD = 12;
    private static final float ICON_SCALE = 2;
    private static final int BACK_WIDTH = 96;
    private static final Identifier BANNER = Identifier.fromNamespaceAndPath(BdAdvance.MOD_ID,
            "textures/gui/quests_banner.png");
    /** Цвет группы по порядку: ежедневные, еженедельные, ежемесячные. */
    private static final int[] GROUP_COLORS = {MenuUi.BLOOD, MenuUi.GOLD, 0xFFA46BFF};

    public QuestsScreen() {
        super("Задания");
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
        return menu == null ? null : QuestMenu.read(menu);
    }

    private int closeX() {
        return left + WIDTH - PAD - MenuUi.BUTTON_HEIGHT;
    }

    private int backX() {
        return closeX() - 6 - BACK_WIDTH;
    }

    private int buttonY() {
        return top + PAD;
    }

    /** Верх группы: группы идут друг за другом сразу под баннером. */
    private int groupTop(View view, int index) {
        return top + BANNER_HEIGHT + GAP + index * (GROUP_TITLE + CARD_HEIGHT + GAP);
    }

    private int cardX(int column) {
        return left + column * (CARD_WIDTH + GAP);
    }

    @Override
    protected void drawCanvas(Gfx g, int mouseX, int mouseY) {
        View view = view();
        if (view == null) {
            return;
        }
        drawBanner(g, view, mouseX, mouseY);
        List<Group> groups = view.groups();
        for (int i = 0; i < groups.size() && i < MAX_GROUPS; i++) {
            drawGroup(g, groups.get(i), groupTop(view, i), GROUP_COLORS[i % GROUP_COLORS.length], mouseX, mouseY);
        }
        g.nextStratum();
        for (int i = 0; i < groups.size() && i < MAX_GROUPS; i++) {
            drawGroupIcons(g, groups.get(i), groupTop(view, i));
        }
    }

    private void drawBanner(Gfx g, View view, int mouseX, int mouseY) {
        MenuUi.pixelBanner(g, BANNER, left, top, WIDTH, BANNER_HEIGHT);
        MenuUi.display(g, "ЗАДАНИЯ", left + PAD + 2, top + PAD + 2, 2.4f, Theme.TEXT);
        String subtitle = view.total() > 0
                ? "Выполнено " + view.done() + " из " + view.total() + "  ·  опыт начисляется сам"
                : "Выполняй задания и получай опыт для батл пасса";
        Draw.text(g, subtitle, left + PAD + 3, top + 50, Colors.fade(Theme.TEXT, 0.8f));
        boolean backHovered = inside(mouseX, mouseY, backX(), buttonY(), BACK_WIDTH, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, backX(), buttonY(), BACK_WIDTH, Icon.ARROW_LEFT, "К уровням", ButtonStyle.LIGHT, true, backHovered);
        MenuUi.balance(g, backX() - 6, buttonY(), BloodBalance.current());
        boolean closeHovered = inside(mouseX, mouseY, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, Icon.CLOSE, null, ButtonStyle.OUTLINE, true, closeHovered);
    }

    private void drawGroup(Gfx g, Group group, int y, int accent, int mouseX, int mouseY) {
        long done = group.entries().stream().filter(entry -> entry.quest().done()).count();
        Draw.roundedRect(g, left + 2, y + 3, 3, 10, 1.5f, accent);
        Draw.text(g, group.title(), left + 10, y + 4, Theme.TEXT, TextStyle.BODY_MEDIUM);
        String count = done + " / " + group.entries().size();
        Draw.text(g, count, left + 16 + Draw.width(group.title(), TextStyle.BODY_MEDIUM), y + 4, Theme.TEXT_DIM);
        List<Entry> entries = group.entries();
        for (int i = 0; i < entries.size() && i < COLUMNS; i++) {
            drawCard(g, entries.get(i).quest(), cardX(i), y + GROUP_TITLE, accent, mouseX, mouseY);
        }
    }

    private void drawCard(Gfx g, Quest quest, int x, int y, int accent, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
        MenuUi.panel(g, x, y, CARD_WIDTH, CARD_HEIGHT);
        if (quest.done()) {
            Draw.roundedRect(g, x, y, CARD_WIDTH, CARD_HEIGHT, Theme.PANEL_RADIUS, Colors.fade(MenuUi.GREEN, 0.07f));
            Draw.border(g, x, y, CARD_WIDTH, CARD_HEIGHT, Theme.PANEL_RADIUS, Colors.fade(MenuUi.GREEN, 0.6f));
        } else if (hovered) {
            Draw.border(g, x, y, CARD_WIDTH, CARD_HEIGHT, Theme.PANEL_RADIUS, Theme.BORDER_SELECTED);
        }
        int iconBox = 40;
        Draw.roundedRect(g, x + 8, y + 8, iconBox, iconBox, Theme.RADIUS + 2, 0x55000000);
        int textX = x + 8 + iconBox + 8;
        int textW = CARD_WIDTH - (textX - x) - 8;
        Draw.text(g, Draw.trim(quest.title(), textW - (quest.done() ? 12 : 0), TextStyle.BODY_MEDIUM), textX, y + 9,
                quest.done() ? MenuUi.GREEN : Theme.TEXT, TextStyle.BODY_MEDIUM);
        if (quest.done()) {
            Draw.icon(g, Icon.CHECK, x + CARD_WIDTH - 8 - Theme.ICON_SMALL, y + 9, Theme.ICON_SMALL, MenuUi.GREEN);
        }
        int lineY = y + 21;
        for (String line : MenuUi.wrap(quest.description(), textW, 3, TextStyle.BODY)) {
            Draw.text(g, line, textX, lineY, Theme.TEXT_SOFT, TextStyle.BODY);
            lineY += 10;
        }
        int barY = y + CARD_HEIGHT - 26;
        int barColor = quest.done() ? MenuUi.GREEN : accent;
        MenuUi.progressBar(g, x + 8, barY, CARD_WIDTH - 16, 5, quest.fill(), barColor);
        String count = quest.progress() + " / " + quest.goal();
        int rowY = barY + 10;
        Draw.text(g, count, x + 8, rowY, Theme.TEXT_SOFT);
        float rewardW = Draw.width(quest.reward()) + 12;
        float rewardX = x + CARD_WIDTH - 8 - rewardW;
        Draw.roundedRect(g, rewardX, rowY - 2, rewardW, 12, 6, Colors.fade(MenuUi.GOLD, 0.18f));
        Draw.text(g, quest.reward(), rewardX + 6, rowY, MenuUi.GOLD);
    }

    private void drawGroupIcons(Gfx g, Group group, int y) {
        List<Entry> entries = group.entries();
        float size = 16 * ICON_SCALE;
        for (int i = 0; i < entries.size() && i < COLUMNS; i++) {
            MenuUi.item(g, minecraft.font, entries.get(i).stack(), cardX(i) + 8 + (40 - size) / 2f,
                    y + GROUP_TITLE + 8 + (40 - size) / 2f, ICON_SCALE);
        }
    }

    private Entry entryAt(View view, double x, double y) {
        List<Group> groups = view.groups();
        for (int i = 0; i < groups.size() && i < MAX_GROUPS; i++) {
            List<Entry> entries = groups.get(i).entries();
            int cardY = groupTop(view, i) + GROUP_TITLE;
            for (int j = 0; j < entries.size() && j < COLUMNS; j++) {
                if (inside(x, y, cardX(j), cardY, CARD_WIDTH, CARD_HEIGHT)) {
                    return entries.get(j);
                }
            }
        }
        return null;
    }

    @Override
    protected void drawTooltips(Gfx g, int canvasX, int canvasY, int screenX, int screenY) {
        View view = view();
        if (view == null) {
            return;
        }
        ItemStack hovered = ItemStack.EMPTY;
        if (inside(canvasX, canvasY, left, top, WIDTH - BACK_WIDTH - 40, BANNER_HEIGHT)) {
            hovered = view.info();
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
        if (inside(x, y, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        if (inside(x, y, backX(), buttonY(), BACK_WIDTH, MenuUi.BUTTON_HEIGHT)) {
            BattlePassNavigator.openBattlePass();
            return true;
        }
        Entry entry = entryAt(view, x, y);
        if (entry != null) {
            clickSlot(entry.slot(), false);
            return true;
        }
        return false;
    }
}
