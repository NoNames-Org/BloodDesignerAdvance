package dev.no.name.bdadvance.battlepass;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Чтение серверных меню bloodstone: шрифты и глифы заголовка, ключи перевода, описания предметов. */
public final class MenuText {
    private MenuText() {
    }

    /** Есть ли в тексте кусок, набранный шрифтом с таким id (например bloodlevels:menu_progress). */
    public static boolean hasFont(Component text, String font) {
        boolean[] found = {false};
        text.visit((style, part) -> {
            if (fontOf(style).contains(font)) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    /** Коды символов, набранных этим шрифтом. */
    public static int[] glyphs(Component text, String font) {
        List<Integer> glyphs = new ArrayList<>();
        text.visit((style, part) -> {
            if (fontOf(style).contains(font)) {
                part.codePoints().forEach(glyphs::add);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return glyphs.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Есть ли в тексте ключ перевода с таким значением (не зависит от языка клиента). */
    public static boolean hasKey(Component text, String key) {
        if (text.getContents() instanceof TranslatableContents translatable && key.equals(translatable.getKey())) {
            return true;
        }
        for (Component sibling : text.getSiblings()) {
            if (hasKey(sibling, key)) {
                return true;
            }
        }
        return false;
    }

    /** Есть ли ключ перевода, оканчивающийся на {@code suffix}. */
    public static boolean hasKeySuffix(Component text, String suffix) {
        if (text.getContents() instanceof TranslatableContents translatable && translatable.getKey().endsWith(suffix)) {
            return true;
        }
        for (Component sibling : text.getSiblings()) {
            if (hasKeySuffix(sibling, suffix)) {
                return true;
            }
        }
        return false;
    }

    /** Первый ключ перевода в тексте; пусто, если текст набран без ключей. */
    public static String key(Component text) {
        if (text.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        for (Component sibling : text.getSiblings()) {
            String found = key(sibling);
            if (!found.isEmpty()) {
                return found;
            }
        }
        return "";
    }

    /** Первый аргумент ключа перевода, оканчивающегося на {@code suffix}, например цена в «...price». */
    public static String keyArgument(Component text, String suffix) {
        if (text.getContents() instanceof TranslatableContents translatable && translatable.getKey().endsWith(suffix)
                && translatable.getArgs().length > 0) {
            Object arg = translatable.getArgs()[0];
            return arg instanceof Component component ? component.getString() : String.valueOf(arg);
        }
        for (Component sibling : text.getSiblings()) {
            String found = keyArgument(sibling, suffix);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static List<Component> loreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? List.of() : lore.lines();
    }

    public static List<String> loreStrings(ItemStack stack) {
        return loreLines(stack).stream().map(Component::getString).toList();
    }

    public static String loreJoined(ItemStack stack) {
        return String.join(" ", loreStrings(stack));
    }

    public static String model(ItemStack stack) {
        Object model = stack.get(DataComponents.ITEM_MODEL);
        return model == null ? "" : model.toString();
    }

    public static String name(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    public static ItemStack stack(AbstractContainerMenu menu, int slot) {
        return slot >= 0 && slot < menu.slots.size() ? menu.getSlot(slot).getItem() : ItemStack.EMPTY;
    }

    private static String fontOf(Style style) {
        Object font = style.getFont();
        return font == null ? "" : font.toString();
    }
}
