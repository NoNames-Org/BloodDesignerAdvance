package dev.no.name.bdadvance.battlepass.gui;

import dev.no.name.afriends.compat.Gfx;
import dev.no.name.bdadvance.battlepass.StoreItem;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.icon.Icon;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Значки товаров и категорий магазина. У кнопок сервера модель пустая (картинку рисует шрифт меню), поэтому
 * значок выбираем по ключу названия; у кейсов модель настоящая — рисуем сам предмет
 */
final class StoreIcons {
    private static final String UI = "bloodstone.blooddonate.store.ui.";
    private static final int PURPLE = 0xFFB070FF;

    private record Mark(String part, Icon icon, int color) {
    }

    private static final List<Mark> MARKS = List.of(
            new Mark("category.crates", Icon.ARCHIVE, MenuUi.GOLD),
            new Mark("category.other", Icon.MORE_HORIZONTAL, 0xFFD6D8DE),
            new Mark("category.battlepass", Icon.STAR, MenuUi.GOLD),
            new Mark("category.ffa", Icon.SWORD, MenuUi.RED),
            new Mark("mvp.", Icon.CROWN, PURPLE),
            new Mark("vip.", Icon.CROWN, MenuUi.GOLD),
            new Mark("unmute.", Icon.MIC, 0xFFD6D8DE),
            new Mark("smp.", Icon.KEY, 0xFFF0E2B6),
            new Mark("customGame.", Icon.TROPHY, MenuUi.GOLD),
            new Mark("coins.", Icon.DIAMOND, MenuUi.GOLD),
            new Mark("booster.", Icon.ARROW_UP, MenuUi.GREEN));

    private StoreIcons() {
    }

    /** Цвет товара: им подсвечены значок и рамка карточки. */
    static int color(StoreItem item) {
        Mark mark = mark(item);
        return mark == null ? MenuUi.GOLD : mark.color();
    }

    /** Значок в квадрате size×size с подложкой. */
    static void draw(Gfx g, StoreItem item, float x, float y, int size) {
        Mark mark = mark(item);
        int color = mark == null ? MenuUi.GOLD : mark.color();
        Draw.roundedRect(g, x, y, size, size, size / 4f, Colors.fade(color, 0.14f));
        Draw.border(g, x, y, size, size, size / 4f, Colors.fade(color, 0.35f));
        if (mark == null) {
            // кейс: его собственная модель (сундук, тыква, гаджет)
            float scale = size * 0.75f / 16f;
            MenuUi.item(g, Minecraft.getInstance().font, item.stack(), x + (size - 16 * scale) / 2f,
                    y + (size - 16 * scale) / 2f, scale);
            return;
        }
        float icon = size * 0.55f;
        Draw.icon(g, mark.icon(), x + (size - icon) / 2f, y + (size - icon) / 2f, icon, color);
    }

    private static Mark mark(StoreItem item) {
        if (!item.key().startsWith(UI)) {
            return null;
        }
        String rest = item.key().substring(UI.length());
        for (Mark mark : MARKS) {
            if (rest.startsWith(mark.part())) {
                return mark;
            }
        }
        return new Mark("", Icon.TAG, MenuUi.GOLD);
    }
}
