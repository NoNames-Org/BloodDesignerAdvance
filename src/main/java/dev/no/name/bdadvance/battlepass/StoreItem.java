package dev.no.name.bdadvance.battlepass;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Товар магазина bloodstone из описания предмета: строки описания, цена (ключ …price), баланс (…balance)
 * и что внизу — «Нажмите, чтобы купить» (…buy) или причина, почему купить нельзя (например «У вас нет мута»).
 * key — ключ перевода названия: по нему экран выбирает иконку
 */
public record StoreItem(int slot, ItemStack stack, String key, String name, List<String> lines, int price, int balance,
                        boolean buyable, String status) {
    public StoreItem {
        lines = List.copyOf(lines);
    }

    public boolean affordable() {
        return balance >= price;
    }

    public static StoreItem read(int slot, ItemStack stack) {
        int price = 0;
        int balance = 0;
        boolean pricedYet = false;
        boolean buyable = false;
        String status = "";
        List<String> lines = new ArrayList<>();
        for (Component line : MenuText.loreLines(stack)) {
            String priceArg = MenuText.keyArgument(line, ".price");
            String balanceArg = MenuText.keyArgument(line, ".balance");
            String text = line.getString().strip();
            if (priceArg != null) {
                price = BattlePassText.firstNumber(priceArg);
                pricedYet = true;
            } else if (balanceArg != null) {
                balance = BattlePassText.firstNumber(balanceArg);
            } else if (MenuText.hasKeySuffix(line, ".buy")) {
                buyable = true;
            } else if (pricedYet) {
                // после цены — только состояние: что мешает купить
                if (!text.isEmpty()) {
                    status = text;
                }
            } else if (!text.isEmpty()) {
                lines.add(text);
            }
        }
        return new StoreItem(slot, stack, MenuText.key(stack.getHoverName()), MenuText.name(stack), lines,
                Math.max(0, price), Math.max(0, balance), buyable, status);
    }
}
