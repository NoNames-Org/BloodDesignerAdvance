package dev.no.name.bdadvance.modes.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.no.name.afriends.compat.Compat;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.afriends.compat.ScreenBase;
import dev.no.name.bdadvance.modes.ModeFavorites;
import dev.no.name.bdadvance.modes.ModeLauncher;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.afriends.gui.animation.Animated;
import dev.no.name.afriends.gui.animation.Easing;
import dev.no.name.afriends.gui.animation.Motion;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.blur.MenuBlur;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.afriends.gui.render.text.TextStyle;
import dev.no.name.afriends.gui.sound.UiSound;
import dev.no.name.afriends.gui.sound.UiSounds;
import dev.no.name.afriends.gui.widget.ButtonWidget;
import dev.no.name.afriends.gui.widget.TextFieldWidget;
import dev.no.name.afriends.gui.widget.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModeSelectScreen extends ScreenBase {
    private static final int COLUMNS = 6;
    private static final int CARD_WIDTH = 80;
    private static final int CARD_HEIGHT = 111;
    private static final int GAP = 7;
    private static final int GRID_WIDTH = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GAP;

    private static final float SCREEN_SHARE = 0.54f;
    private static final int TOP = 14;
    private static final int BAR_HEIGHT = 30;
    private static final int CONTROL_HEIGHT = 18;
    private static final int BAR_PAD = 6;
    private static final int SEARCH_WIDTH = 104;
    private static final int FAVORITES_WIDTH = 72;
    private static final int BACK_WIDTH = 86;
    private static final int GRID_TOP = TOP + BAR_HEIGHT + 10;
    private static final int BOTTOM_MARGIN = 12;
    private static final int ART = 64;
    private static final int STAR = 13;
    private static final int SCROLL_STEP = 24;
    private static final float CARD_LIFT = 2;
    private static final int BADGE_COLOR = 0xFFD8314A;
    private static final int FAVORITE_COLOR = 0xFFFFD15C;
    private static final long DOTS_PERIOD = 900;

    private final Screen parent;
    private final ModeFavorites favorites;
    private final ModeLauncher launcher;
    private final TextFieldWidget search = new TextFieldWidget(Icon.SEARCH, "Поиск режима").withMaxLength(32);
    private final ButtonWidget close = new ButtonWidget(Icon.CLOSE, this::onClose);
    private final Animated windowOpen = new Animated(0, Motion.SLOW);
    private final Animated scrollView = new Animated(0, Motion.FAST);
    private final Map<GameMode, Animated> hover = new EnumMap<>(GameMode.class);
    private final Map<GameMode, Animated> press = new EnumMap<>(GameMode.class);

    private GameMode group;
    private boolean favoritesOnly;
    private long viewOpenedAt = Motion.now();
    private int scroll;

    private int pixelScale = 1;
    private float scale = 1;
    private int canvasWidth;
    private int canvasHeight;

    private int left;

    public ModeSelectScreen(Screen parent, ModeFavorites favorites, ModeLauncher launcher) {
        super(Component.literal("Выбор режима"));
        this.parent = parent;
        this.favorites = favorites;
        this.launcher = launcher;
        for (GameMode mode : GameMode.values()) {
            hover.put(mode, new Animated(0, Motion.FAST));
            press.put(mode, new Animated(0, Motion.SLOW));
        }
        windowOpen.setTarget(1);
        UiSounds.play(UiSound.MENU_OPEN);
    }

    @Override
    protected void init() {
        Window window = minecraft.getWindow();

        int byWidth = Math.round(window.getWidth() * SCREEN_SHARE / GRID_WIDTH);
        int byHeight = window.getHeight() / (GRID_TOP + CARD_HEIGHT + BOTTOM_MARGIN + 20);
        pixelScale = Math.max(1, Math.min(byWidth, byHeight));
        scale = pixelScale / (float) window.getGuiScale();
        canvasWidth = window.getWidth() / pixelScale;
        canvasHeight = window.getHeight() / pixelScale;
        left = (canvasWidth - GRID_WIDTH) / 2;
        int barY = TOP + (BAR_HEIGHT - CONTROL_HEIGHT) / 2;
        search.setBounds(left + BAR_PAD, barY, SEARCH_WIDTH, CONTROL_HEIGHT);
        close.setBounds(left + GRID_WIDTH - BAR_PAD - CONTROL_HEIGHT, barY, CONTROL_HEIGHT, CONTROL_HEIGHT);
    }

    @Override
    protected void drawBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
        float shown = windowOpen.get();
        MenuBlur.request(shown);
        g.blurBeforeThisStratum();
        Draw.pushAlpha(shown);
        Draw.backdrop(g, width, height, Theme.DIM, Theme.DIM_VIGNETTE, Theme.DIM_GRAIN);
        Draw.popAlpha();
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        Draw.setPixelScale(pixelScale);
        int canvasX = (int) toCanvas(mouseX);
        int canvasY = (int) toCanvas(mouseY);

        float shown = windowOpen.get();
        Draw.pushAlpha(shown);
        Draw.pushOffset(g, 0, (1 - shown) * Motion.RISE);
        drawBar(g, canvasX, canvasY);
        drawGrid(g, canvasX, canvasY, shown);
        Draw.popOffset(g);
        Draw.popAlpha();
        pose.popMatrix();
    }

    private int favoritesX() {
        return left + GRID_WIDTH - BAR_PAD - CONTROL_HEIGHT - 6 - FAVORITES_WIDTH;
    }

    private int barControlY() {
        return TOP + (BAR_HEIGHT - CONTROL_HEIGHT) / 2;
    }

    private void drawBar(Gfx g, int mouseX, int mouseY) {
        Draw.shadow(g, left, TOP + 2, GRID_WIDTH, BAR_HEIGHT, Theme.PANEL_RADIUS, 12, Colors.fade(Theme.SHADOW, 0.7f));
        Draw.gradient(g, left, TOP, GRID_WIDTH, BAR_HEIGHT, Theme.PANEL_RADIUS, Theme.PANEL_TOP, Theme.PANEL_BOTTOM);
        Draw.border(g, left, TOP, GRID_WIDTH, BAR_HEIGHT, Theme.PANEL_RADIUS, Theme.BORDER_HOVER);
        if (group == null) {
            search.render(g, mouseX, mouseY);
            drawChip(g, favoritesX(), FAVORITES_WIDTH, Icon.STAR, "Избранное", favoritesOnly, mouseX, mouseY);
        } else {
            drawChip(g, left + BAR_PAD, BACK_WIDTH, Icon.ARROW_LEFT, "Все режимы", false, mouseX, mouseY);
            Draw.text(g, group.title(), left + BAR_PAD + BACK_WIDTH + 10, TOP + BAR_HEIGHT / 2f - 5, Theme.TEXT, TextStyle.TITLE);
        }
        close.render(g, mouseX, mouseY);
    }

    private void drawChip(Gfx g, int x, int width, Icon icon, String text, boolean active, int mouseX, int mouseY) {
        int y = barControlY();
        boolean hovered = inside(mouseX, mouseY, x, y, width, CONTROL_HEIGHT);
        int fill = active ? Theme.ACCENT : hovered ? Theme.CARD_HOVER : Theme.CARD;
        int border = active ? Theme.ACCENT : hovered ? Theme.BORDER_HOVER : Theme.BORDER;
        int color = active ? Theme.ACCENT_TEXT : hovered ? Theme.TEXT : Theme.TEXT_SOFT;
        Draw.box(g, x, y, width, CONTROL_HEIGHT, Theme.RADIUS, fill, border);
        int textWidth = Draw.width(text);
        float contentX = x + (width - Theme.ICON_SMALL - 5 - textWidth) / 2f;
        float centerY = y + CONTROL_HEIGHT / 2f;
        Draw.icon(g, icon, contentX, centerY - Theme.ICON_SMALL / 2f, Theme.ICON_SMALL, color);
        Draw.text(g, text, contentX + Theme.ICON_SMALL + 5, centerY - 4, color);
    }

    private List<GameMode> visibleModes() {
        if (group != null) {
            return favorites.order(group.children(), GameMode::id);
        }
        String query = search.getText();
        List<GameMode> result = new ArrayList<>();
        for (GameMode mode : GameMode.topLevel()) {
            boolean favoriteOk = !favoritesOnly || favorites.contains(mode.id()) || hasFavoriteChild(mode);
            if (favoriteOk && (mode.matches(query) || childMatches(mode, query))) {
                result.add(mode);
            }
        }
        return favorites.order(result, GameMode::id);
    }

    private boolean hasFavoriteChild(GameMode mode) {
        return mode.children().stream().anyMatch(child -> favorites.contains(child.id()));
    }

    private static boolean childMatches(GameMode mode, String query) {
        return !query.isBlank() && mode.children().stream().anyMatch(child -> child.matches(query));
    }

    private int gridBottom() {
        return canvasHeight - BOTTOM_MARGIN;
    }

    private int maxScroll(int count) {
        int rows = (count + COLUMNS - 1) / COLUMNS;
        int content = rows * CARD_HEIGHT + Math.max(0, rows - 1) * GAP + 4;
        return Math.max(0, content - (gridBottom() - GRID_TOP));
    }

    private int cardX(int index, int count) {
        int inRow = Math.min(COLUMNS, count - index / COLUMNS * COLUMNS);
        int rowStart = group == null ? left : left + (GRID_WIDTH - inRow * CARD_WIDTH - (inRow - 1) * GAP) / 2;
        return rowStart + index % COLUMNS * (CARD_WIDTH + GAP);
    }

    private int cardY(int index) {
        return GRID_TOP + 2 + index / COLUMNS * (CARD_HEIGHT + GAP) - Math.round(scrollView.get());
    }

    private void drawGrid(Gfx g, int mouseX, int mouseY, float shown) {
        List<GameMode> modes = visibleModes();
        clampScroll(modes.size());
        if (modes.isEmpty()) {
            drawEmpty(g);
            return;
        }
        boolean mouseInGrid = mouseY >= GRID_TOP && mouseY < gridBottom();
        int hoverX = mouseInGrid ? mouseX : -1000;
        g.enableScissor(0, GRID_TOP, canvasWidth, gridBottom());
        for (int i = 0; i < modes.size(); i++) {
            int y = cardY(i);
            if (y + CARD_HEIGHT < GRID_TOP || y > gridBottom()) {
                continue;
            }
            float appear = Motion.appear(viewOpenedAt, i);
            Draw.pushAlpha(appear);
            Draw.pushOffset(g, 0, (1 - appear) * Motion.RISE);
            drawCard(g, modes.get(i), cardX(i, modes.size()), y, hoverX, mouseY, shown > 0.6f && appear > 0.6f);
            Draw.popOffset(g);
            Draw.popAlpha();
        }
        g.disableScissor();
    }

    private void drawCard(Gfx g, GameMode mode, int x, int y, int mouseX, int mouseY, boolean showArt) {
        boolean hovered = inside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
        Animated hoverAnim = hover.get(mode);
        hoverAnim.setTarget(hovered);
        float lift = hoverAnim.get();
        float pressed = press.get(mode).get();
        float top = y - lift * CARD_LIFT + pressed * 1.5f;
        int color = mode.color();

        int topColor = Colors.mix(color, 0xFFFFFFFF, lift * 0.12f + pressed * 0.2f);
        int bottomColor = Colors.mix(color, 0xFF000000, 0.45f);
        Draw.shadow(g, x, top + 3, CARD_WIDTH, CARD_HEIGHT, Theme.RADIUS + 2, 9 + lift * 4, Colors.fade(Theme.SHADOW, 0.6f));
        Draw.gradient(g, x, top, CARD_WIDTH, CARD_HEIGHT, Theme.RADIUS + 1, topColor, bottomColor);
        int border = Colors.mix(Colors.mix(color, 0xFFFFFFFF, 0.3f), 0xFFFFFFFF, lift * 0.5f);
        Draw.border(g, x, top, CARD_WIDTH, CARD_HEIGHT, Theme.RADIUS + 1, border);
        Draw.border(g, x + 2, top + 2, CARD_WIDTH - 4, CARD_HEIGHT - 4, Theme.RADIUS, Colors.fade(border, 0.35f));

        if (showArt) {
            float bob = hovered ? (Easing.pulse(Motion.now(), Theme.IDLE_PERIOD / 2) - 0.5f) * 2 : 0;
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate(x + (CARD_WIDTH - ART) / 2f, top + 8 - lift * 2 + bob);
            g.blit(mode.art(), 0, 0, ART, GameMode.ART_SIZE);
            pose.popMatrix();
        }

        if (drawStatus(g, mode, x, top)) {
            return;
        }
        boolean isGroup = mode.isGroup();
        List<String> lines = wrap(mode.title(), CARD_WIDTH - 10);
        float textY = top + CARD_HEIGHT - (isGroup ? 19 : 10) - lines.size() * 9;
        for (String line : lines) {
            Draw.textCentered(g, line, x + CARD_WIDTH / 2f, textY, Theme.TEXT);
            textY += 9;
        }
        if (isGroup) {
            String hint = mode.children().size() + " режима  ›";
            Draw.text(g, hint, x + (CARD_WIDTH - Draw.width(hint, TextStyle.SMALL)) / 2f, top + CARD_HEIGHT - 12,
                    Colors.fade(Theme.TEXT, 0.6f), TextStyle.SMALL);
        }
        if (mode.badge() != null) {
            float badgeWidth = Draw.width(mode.badge(), TextStyle.SMALL) + 8;
            drawBadge(g, mode.badge(), x + CARD_WIDTH - 5 - badgeWidth, top + 5);
        }
        if (!isGroup) {
            drawStar(g, mode, x, top, hovered, mouseX, mouseY);
        }
    }

    private boolean drawStatus(Gfx g, GameMode mode, int x, float top) {
        if (!mode.id().equals(launcher.modeId())) {
            return false;
        }
        String title;
        String hint = "";
        int color = Theme.TEXT;
        switch (launcher.status()) {
            case GOING_HUB -> title = "В лобби" + dots();
            case OPENING, CHOOSING -> title = "Открываем" + dots();
            case WAITING -> title = "Подключаем" + dots();
            case QUEUED -> {
                title = "В очереди";
                hint = "нажми, чтобы выйти";
            }
            case ERROR -> {
                title = "Не вышло";
                hint = launcher.error();
                color = Theme.DANGER;
            }
            default -> {
                return false;
            }
        }
        Draw.roundedRect(g, x, top, CARD_WIDTH, CARD_HEIGHT, Theme.RADIUS + 1, Colors.fade(0xFF000000, 0.55f));
        List<String> hintLines = hint.isEmpty() ? List.of() : wrap(hint, CARD_WIDTH - 10);
        float y = top + CARD_HEIGHT / 2f - 6 - Math.min(4, hintLines.size()) * 4.5f;
        Draw.textCentered(g, title, x + CARD_WIDTH / 2f, y, color);
        y += 12;
        for (int i = 0; i < hintLines.size() && i < 4; i++) {
            String line = hintLines.get(i);
            Draw.text(g, line, x + (CARD_WIDTH - Draw.width(line, TextStyle.SMALL)) / 2f, y, Theme.TEXT_SOFT, TextStyle.SMALL);
            y += 8;
        }
        return true;
    }

    private static String dots() {
        int count = (int) (Motion.now() % DOTS_PERIOD * 3 / DOTS_PERIOD) + 1;
        return ".".repeat(count);
    }

    private void drawStar(Gfx g, GameMode mode, int x, float top, boolean cardHovered, int mouseX, int mouseY) {
        boolean favorite = favorites.contains(mode.id());
        if (!favorite && !cardHovered) {
            return;
        }
        float starX = x + 5;
        float starY = top + 5;
        boolean hovered = inside(mouseX, mouseY, (int) starX, (int) starY, STAR, STAR);
        Draw.roundedRect(g, starX, starY, STAR, STAR, Theme.RADIUS, Colors.fade(0xFF000000, hovered ? 0.5f : 0.3f));
        int color = favorite ? FAVORITE_COLOR : hovered ? Theme.TEXT : Theme.TEXT_SOFT;
        Draw.icon(g, Icon.STAR, starX + 2.5f, starY + 2.5f, STAR - 5, color, favorite ? 0.4f : 0);
    }

    private static void drawBadge(Gfx g, String text, float x, float y) {
        float width = Draw.width(text, TextStyle.SMALL) + 8;
        Draw.roundedRect(g, x, y, width, 11, 3, BADGE_COLOR);
        Draw.text(g, text, x + 4, y + 2.5f, Theme.TEXT, TextStyle.SMALL);
    }

    private void drawEmpty(Gfx g) {
        float centerX = left + GRID_WIDTH / 2f;
        float centerY = GRID_TOP + CARD_HEIGHT / 2f + 10;
        float drift = (Easing.pulse(Motion.now(), Theme.IDLE_PERIOD) - 0.5f) * 3;
        Draw.icon(g, favoritesOnly ? Icon.STAR : Icon.SEARCH, centerX - 10, centerY - 26 + drift, 20, Theme.TEXT_FAINT);
        String text = favoritesOnly && favorites.all().isEmpty()
                ? "В избранном пусто — отметь режим звёздочкой"
                : "Ничего не нашлось";
        Draw.textCentered(g, text, centerX, centerY, Theme.TEXT_DIM);
    }

    private static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (Draw.width(candidate) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        lines.add(line.toString());
        return lines;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void clampScroll(int count) {
        int max = maxScroll(count);
        if (scroll > max) {
            scroll = max;
            scrollView.setTarget(scroll);
        }
    }

    private void openGroup(GameMode mode) {
        group = mode;
        resetView();
        search.blur();
        UiSounds.play(UiSound.DROPDOWN_OPEN);
    }

    private void closeGroup() {
        group = null;
        resetView();
        UiSounds.play(UiSound.DROPDOWN_CLOSE);
    }

    private void resetView() {
        viewOpenedAt = Motion.now();
        scroll = 0;
        scrollView.snap(0);
    }

    private void startMode(GameMode mode) {
        boolean queuedHere = launcher.status() == ModeLauncher.Status.QUEUED && mode.id().equals(launcher.modeId());
        if (queuedHere) {
            launcher.leaveQueue();
        } else if (mode.route() != null && !launcher.isBusy()) {
            launcher.launch(mode.id(), mode.route());
            // «Лобби» — просто команда /hub: ждать в навигаторе нечего, закрываем его сразу
            if (mode.route().isCommand() && launcher.status() == ModeLauncher.Status.TRANSFERRED) {
                Compat.setScreen(null);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = toCanvas(event.x());
        double y = toCanvas(event.y());
        int button = Widget.LEFT_BUTTON;
        if (close.mouseClicked(x, y, button)) {
            return true;
        }
        if (group != null) {
            if (inside(x, y, left + BAR_PAD, barControlY(), BACK_WIDTH, CONTROL_HEIGHT)) {
                closeGroup();
                return true;
            }
        } else {
            search.mouseClicked(x, y, button);
            if (inside(x, y, favoritesX(), barControlY(), FAVORITES_WIDTH, CONTROL_HEIGHT)) {
                favoritesOnly = !favoritesOnly;
                resetView();
                UiSounds.play(UiSound.CLICK);
                return true;
            }
        }
        return y >= GRID_TOP && y < gridBottom() && clickCard(x, y);
    }

    private boolean clickCard(double x, double y) {
        List<GameMode> modes = visibleModes();
        for (int i = 0; i < modes.size(); i++) {
            GameMode mode = modes.get(i);
            int cardX = cardX(i, modes.size());
            int cardY = cardY(i);
            if (!inside(x, y, cardX, cardY, CARD_WIDTH, CARD_HEIGHT)) {
                continue;
            }
            if (mode.isGroup()) {
                openGroup(mode);
                return true;
            }
            if (inside(x, y, cardX + 5, cardY + 3, STAR, STAR + 2)) {
                favorites.toggle(mode.id());
                UiSounds.play(UiSound.CLICK);
                return true;
            }
            Animated pressed = press.get(mode);
            pressed.snap(1);
            pressed.setTarget(0);
            UiSounds.play(UiSound.CLICK);
            startMode(mode);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll(visibleModes().size());
        scroll = (int) Math.max(0, Math.min(max, scroll - scrollY * SCROLL_STEP));
        scrollView.setTarget(scroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE && group != null) {
            closeGroup();
            return true;
        }
        if (group == null && key != InputConstants.KEY_ESCAPE && search.keyPressed(key)) {
            resetView();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (group == null && search.charTyped(event.codepointAsString())) {
            resetView();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        Compat.setScreen(parent);
    }

    @Override
    public void removed() {
        MenuBlur.cancel();
        Compat.stopTextInput();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private double toCanvas(double screenValue) {
        return screenValue / scale;
    }
}
