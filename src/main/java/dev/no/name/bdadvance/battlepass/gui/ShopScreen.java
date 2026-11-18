package dev.no.name.bdadvance.battlepass.gui;

import dev.no.name.bdadvance.BdAdvance;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.bdadvance.battlepass.BattlePassNavigator;
import dev.no.name.bdadvance.battlepass.BloodBalance;
import dev.no.name.bdadvance.battlepass.ShopMenu;
import dev.no.name.bdadvance.battlepass.StoreItem;
import dev.no.name.bdadvance.battlepass.ShopMenu.View;
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

/** Свой экран покупки Premium: две карточки товаров с описанием с сервера, ценой, балансом и кнопкой «Купить». */
public class ShopScreen extends MenuScreenBase {
    private static final int WIDTH = 460;
    private static final int GAP = 8;
    private static final int BANNER_HEIGHT = 72;
    private static final int CARD_WIDTH = (WIDTH - GAP) / 2;
    private static final int CARD_HEIGHT = 196;
    private static final int HEIGHT = BANNER_HEIGHT + GAP + CARD_HEIGHT;
    private static final int PAD = 12;
    private static final int BACK_WIDTH = 96;
    private static final int MAX_LINES = 9;
    private static final Identifier BANNER = Identifier.fromNamespaceAndPath(BdAdvance.MOD_ID,
            "textures/gui/shop_banner.png");

    /** Пришли из уровней БП — «назад» ведёт к ним; пришли из магазина — обратно в магазин. */
    private final boolean fromLevels;

    public ShopScreen(boolean fromLevels) {
        super("Покупка Premium");
        this.fromLevels = fromLevels;
    }

    private String backLabel() {
        return fromLevels ? "К уровням" : "В магазин";
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
        return menu == null ? null : ShopMenu.read(menu);
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

    private int cardX(int index) {
        return left + index * (CARD_WIDTH + GAP);
    }

    private int cardY() {
        return top + BANNER_HEIGHT + GAP;
    }

    private int buyY() {
        return cardY() + CARD_HEIGHT - PAD - MenuUi.BUTTON_HEIGHT;
    }

    @Override
    protected void drawCanvas(Gfx g, int mouseX, int mouseY) {
        View view = view();
        if (view == null) {
            return;
        }
        drawBanner(g, mouseX, mouseY);
        List<StoreItem> products = view.products();
        if (products.isEmpty()) {
            Draw.textCentered(g, "Загружаю товары…", left + WIDTH / 2f, cardY() + 40, Theme.TEXT_DIM);
        }
        for (int i = 0; i < products.size() && i < 2; i++) {
            drawProduct(g, products.get(i), i == 1, cardX(i), mouseX, mouseY);
        }
    }

    private void drawBanner(Gfx g, int mouseX, int mouseY) {
        MenuUi.pixelBanner(g, BANNER, left, top, WIDTH, BANNER_HEIGHT);
        MenuUi.display(g, "ПРЕМИУМ", left + PAD + 2, top + PAD + 2, 2.4f, MenuUi.GOLD);
        Draw.text(g, "Открой Premium-линию батл пасса", left + PAD + 3, top + 50, Colors.fade(Theme.TEXT, 0.8f));
        boolean backHovered = inside(mouseX, mouseY, backX(), buttonY(), BACK_WIDTH, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, backX(), buttonY(), BACK_WIDTH, Icon.ARROW_LEFT, backLabel(), ButtonStyle.LIGHT, true, backHovered);
        MenuUi.balance(g, backX() - 6, buttonY(), BloodBalance.current());
        boolean closeHovered = inside(mouseX, mouseY, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, Icon.CLOSE, null, ButtonStyle.OUTLINE, true, closeHovered);
    }

    private void drawProduct(Gfx g, StoreItem product, boolean best, int x, int mouseX, int mouseY) {
        int y = cardY();
        MenuUi.panel(g, x, y, CARD_WIDTH, CARD_HEIGHT);
        if (best) {
            Draw.gradient(g, x, y, CARD_WIDTH, CARD_HEIGHT, Theme.PANEL_RADIUS, 0xF2401019, 0xF21E0A0E);
            Draw.border(g, x, y, CARD_WIDTH, CARD_HEIGHT, Theme.PANEL_RADIUS, Colors.fade(MenuUi.GOLD, 0.8f));
            String tag = "ВЫГОДНЕЕ";
            float tagW = Draw.width(tag, TextStyle.BODY) + 12;
            Draw.roundedRect(g, x + CARD_WIDTH - PAD - tagW, y + PAD - 1, tagW, 12, 6, MenuUi.GOLD);
            Draw.text(g, tag, x + CARD_WIDTH - PAD - tagW + 6, y + PAD + 1, 0xFF1A1407, TextStyle.BODY);
        }
        Draw.icon(g, Icon.CROWN, x + PAD, y + PAD + 1, Theme.ICON_SMALL, MenuUi.GOLD);
        int innerW = CARD_WIDTH - PAD * 2;
        Draw.text(g, Draw.trim(product.name(), innerW - 70, TextStyle.BODY_MEDIUM), x + PAD + 13, y + PAD,
                Theme.TEXT, TextStyle.BODY_MEDIUM);
        int lineY = y + PAD + 18;
        int shown = 0;
        for (String line : product.lines()) {
            for (String part : MenuUi.wrap(line, innerW, 2, TextStyle.BODY)) {
                if (shown++ >= MAX_LINES) {
                    break;
                }
                Draw.text(g, part, x + PAD, lineY, Theme.TEXT_SOFT, TextStyle.BODY);
                lineY += 10;
            }
        }
        drawPrice(g, product, best, x, mouseX, mouseY);
    }

    private void drawPrice(Gfx g, StoreItem product, boolean best, int x, int mouseX, int mouseY) {
        int priceY = buyY() - 30;
        Draw.rect(g, x + PAD, priceY - 6, CARD_WIDTH - PAD * 2, 1, Theme.BORDER_HOVER);
        String price = String.valueOf(product.price());
        MenuUi.display(g, price, x + PAD, priceY, 1.6f, Theme.TEXT);
        Draw.text(g, "бладиков", x + PAD + 5 + MenuUi.displayWidth(price, 1.6f), priceY + 5, Theme.TEXT_SOFT);
        String balance = product.affordable()
                ? "Баланс " + product.balance()
                : "Не хватает " + (product.price() - product.balance());
        Draw.text(g, balance, x + CARD_WIDTH - PAD - Draw.width(balance), priceY + 5,
                product.affordable() ? Theme.TEXT_DIM : MenuUi.RED);
        int w = CARD_WIDTH - PAD * 2;
        boolean hovered = inside(mouseX, mouseY, x + PAD, buyY(), w, MenuUi.BUTTON_HEIGHT);
        ButtonStyle style = !product.affordable() ? ButtonStyle.OUTLINE : best ? ButtonStyle.GOLD : ButtonStyle.LIGHT;
        MenuUi.button(g, x + PAD, buyY(), w, Icon.CROWN, "Купить за " + product.price(), style, true, hovered);
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
            if (fromLevels) {
                BattlePassNavigator.openBattlePass();
            } else {
                // серверная кнопка «Назад» раздела возвращает на главную магазина, её подхватит StoreScreen
                clickSlot(ShopMenu.BACK_SLOT, false);
            }
            return true;
        }
        List<StoreItem> products = view.products();
        for (int i = 0; i < products.size() && i < 2; i++) {
            if (inside(x, y, cardX(i) + PAD, buyY(), CARD_WIDTH - PAD * 2, MenuUi.BUTTON_HEIGHT)) {
                clickSlot(products.get(i).slot(), false);
                return true;
            }
        }
        return false;
    }
}
