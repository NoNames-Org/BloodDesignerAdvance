package dev.no.name.bdadvance.battlepass.gui;

import dev.no.name.afriends.compat.Gfx;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.afriends.gui.render.text.TextStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/** Общие элементы экранов БП, квестов и магазина: баннер, панели, кнопки, полосы, перенос текста. */
final class MenuUi {
    static final int GOLD = 0xFFF2C14E;
    static final int GREEN = 0xFF5BD47A;
    static final int RED = 0xFFFF6B6B;
    static final int BLOOD = 0xFFE51B1B;
    static final int BLOOD_DEEP = 0xFF8F0E1A;
    static final int WINE = 0xFF5A1520;
    static final int BUTTON_HEIGHT = 20;
    private static final int GOLD_TEXT = 0xFF2A1404;

    enum ButtonStyle { PRIMARY, LIGHT, OUTLINE, GOLD }

    private MenuUi() {
    }

    static void panel(Gfx g, float x, float y, float w, float h) {
        Draw.shadow(g, x, y + 3, w, h, Theme.PANEL_RADIUS, 14, Colors.fade(Theme.SHADOW, 0.75f));
        Draw.gradient(g, x, y, w, h, Theme.PANEL_RADIUS, Theme.PANEL_TOP, Theme.PANEL_BOTTOM);
        Draw.border(g, x, y, w, h, Theme.PANEL_RADIUS, Theme.BORDER_HOVER);
    }

    /**
     * Баннер с пиксельной картинкой (текстура вдвое меньше холста, скруглённые углы уже в ней):
     * затемнение слева под текст и снизу, красная рамка.
     */
    static void pixelBanner(Gfx g, Identifier texture, int x, int y, int w, int h) {
        Draw.shadow(g, x, y + 3, w, h, Theme.PANEL_RADIUS, 16, Colors.fade(Theme.SHADOW, 0.8f));
        g.image(texture, x, y, w, h, w / 2, h / 2);
        Draw.gradientHorizontal(g, x, y, w * 0.55f, h, Theme.PANEL_RADIUS, 0xD9080305, 0x00080305);
        Draw.gradient(g, x, y + h * 0.5f, w, h * 0.5f, Theme.PANEL_RADIUS, 0x00080305, 0x99080305);
        Draw.border(g, x, y, w, h, Theme.PANEL_RADIUS, Colors.fade(BLOOD, 0.45f));
    }

    /** Крупный текст шрифтом Unbounded с тёмно-красной тенью. */
    static void display(Gfx g, String text, float x, float y, float scale, int color) {
        headline(g, text, x, y + 1.5f, scale, BLOOD_DEEP, TextStyle.DISPLAY);
        headline(g, text, x, y, scale, color, TextStyle.DISPLAY);
    }

    static float displayWidth(String text, float scale) {
        return headlineWidth(text, scale, TextStyle.DISPLAY);
    }

    /**
     * Крупный заголовок. Шрифт мода растрирован под размер пикселя, поэтому вместо растяжения
     * берём шрифт под увеличенный размер: так буквы остаются чёткими, а не пиксельными.
     */
    static void headline(Gfx g, String text, float x, float y, float scale, int color) {
        headline(g, text, x, y, scale, color, TextStyle.TITLE);
    }

    static void headline(Gfx g, String text, float x, float y, float scale, int color, TextStyle style) {
        int base = Draw.pixelScale();
        int sharp = sharpScale(base, scale);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(sharp / (float) base, sharp / (float) base);
        Draw.setPixelScale(sharp);
        Draw.text(g, text, 0, 0, color, style);
        Draw.setPixelScale(base);
        pose.popMatrix();
    }

    static float headlineWidth(String text, float scale) {
        return headlineWidth(text, scale, TextStyle.TITLE);
    }

    static float headlineWidth(String text, float scale, TextStyle style) {
        int base = Draw.pixelScale();
        return Draw.width(text, style) * sharpScale(base, scale) / (float) base;
    }

    /** Ближайший размер пикселя, под который есть шрифт (1..8). */
    private static int sharpScale(int base, float scale) {
        return Math.max(1, Math.min(TextStyle.MAX_PIXEL_SCALE, Math.round(base * scale)));
    }

    static void button(Gfx g, float x, float y, float w, Icon icon, String label, ButtonStyle style,
                       boolean enabled, boolean hovered) {
        int fill;
        int border;
        int color;
        switch (style) {
            case PRIMARY -> {
                fill = hovered ? Colors.mix(Theme.ACCENT, 0xFFFFFFFF, 0.18f) : Theme.ACCENT;
                border = fill;
                color = Theme.ACCENT_TEXT;
            }
            case LIGHT -> {
                fill = hovered ? 0xFFFFFFFF : 0xFFE9EAEE;
                border = fill;
                color = 0xFF111216;
            }
            case GOLD -> {
                fill = hovered ? Colors.mix(GOLD, 0xFFFFFFFF, 0.2f) : GOLD;
                border = fill;
                color = GOLD_TEXT;
            }
            default -> {
                fill = hovered ? Theme.CARD_HOVER : Colors.fade(Theme.CARD, 0.85f);
                border = hovered ? Theme.BORDER_SELECTED : Theme.BORDER_HOVER;
                color = hovered ? Theme.TEXT : Theme.TEXT_SOFT;
            }
        }
        if (!enabled) {
            fill = Colors.fade(Theme.CARD, 0.7f);
            border = Theme.BORDER;
            color = Theme.TEXT_FAINT;
        }
        Draw.box(g, x, y, w, BUTTON_HEIGHT, Theme.RADIUS, fill, border);
        float centerY = y + BUTTON_HEIGHT / 2f;
        if (label == null || label.isEmpty()) {
            Draw.icon(g, icon, x + (w - Theme.ICON_SMALL) / 2f, centerY - Theme.ICON_SMALL / 2f, Theme.ICON_SMALL, color);
            return;
        }
        float iconSpace = icon == null ? 0 : Theme.ICON_SMALL + 5;
        float contentX = x + (w - iconSpace - Draw.width(label)) / 2f;
        if (icon != null) {
            Draw.icon(g, icon, contentX, centerY - Theme.ICON_SMALL / 2f, Theme.ICON_SMALL, color);
        }
        Draw.text(g, label, contentX + iconSpace, centerY - 4, color);
    }

    /** Плашка баланса бладиков, правым краем в {@code rightX}. Ничего не рисует, если баланс неизвестен. */
    static void balance(Gfx g, float rightX, float y, int balance) {
        if (balance < 0) {
            return;
        }
        String text = String.valueOf(balance);
        float w = Theme.ICON_SMALL + 5 + Draw.width(text, TextStyle.BODY_MEDIUM) + 14;
        float x = rightX - w;
        Draw.box(g, x, y, w, BUTTON_HEIGHT, BUTTON_HEIGHT / 2f, 0x66000000, Colors.fade(BLOOD, 0.6f));
        Draw.icon(g, Icon.GEM, x + 7, y + (BUTTON_HEIGHT - Theme.ICON_SMALL) / 2f, Theme.ICON_SMALL, BLOOD);
        Draw.text(g, text, x + 7 + Theme.ICON_SMALL + 5, y + (BUTTON_HEIGHT - 8) / 2f, Theme.TEXT, TextStyle.BODY_MEDIUM);
    }

    static void progressBar(Gfx g, float x, float y, float w, float h, float fill, int color) {
        Draw.roundedRect(g, x, y, w, h, h / 2f, Colors.fade(0xFF000000, 0.45f));
        if (fill > 0) {
            Draw.roundedRect(g, x, y, Math.max(h, w * Math.min(1, fill)), h, h / 2f, color);
        }
    }

    /** Предмет, увеличенный в {@code scale} раз, левым верхним углом в (x, y). */
    static void item(Gfx g, Font font, ItemStack stack, float x, float y, float scale) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.item(font, stack, 0, 0);
        pose.popMatrix();
    }

    /** Перенос по словам в ширину {@code maxWidth}; не больше {@code maxLines} строк, последняя с «…». */
    static List<String> wrap(String text, int maxWidth, int maxLines, TextStyle style) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (Draw.width(candidate, style) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            lines.add(line.toString());
            line.setLength(0);
            line.append(word);
            if (lines.size() == maxLines) {
                break;
            }
        }
        if (!line.isEmpty() && lines.size() < maxLines) {
            lines.add(line.toString());
        } else if (!line.isEmpty() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, Draw.trim(lines.get(last) + " " + line, maxWidth, style));
        }
        return lines;
    }

    /** Текст, повёрнутый на 90° против часовой стрелки, по центру точки (centerX, centerY). */
    static void verticalText(Gfx g, String text, float centerX, float centerY, int color, TextStyle style) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate((float) (-Math.PI / 2));
        Draw.text(g, text, -Draw.width(text, style) / 2f, -4, color, style);
        pose.popMatrix();
    }
}
