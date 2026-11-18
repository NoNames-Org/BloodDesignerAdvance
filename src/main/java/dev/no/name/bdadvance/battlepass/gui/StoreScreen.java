package dev.no.name.bdadvance.battlepass.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.no.name.bdadvance.BdAdvance;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.bdadvance.battlepass.BloodBalance;
import dev.no.name.bdadvance.battlepass.StoreItem;
import dev.no.name.bdadvance.battlepass.StoreMenu;
import dev.no.name.bdadvance.battlepass.StoreMenu.Page;
import dev.no.name.bdadvance.battlepass.StoreMenu.View;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.bdadvance.battlepass.gui.MenuUi.ButtonStyle;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.afriends.gui.render.text.TextStyle;
import dev.no.name.afriends.gui.widget.ConfirmBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Свой экран магазина bloodstone вместо серверного: шапка со страницей и балансом, на главной — плитки категорий
 * и карточки MVP/VIP, в разделах — карточки товаров. Сервер покупает сразу по клику, поэтому перед покупкой
 * спрашиваем подтверждение. Не хватает бладиков — кнопка всё равно нажимается (сервер ответит в чат), неактивна
 * она, только если товар сейчас не продаётся (например, нет мута для размута).
 * Экран один на все страницы: сервер открывает новую страницу — экран её подхватывает (follow)
 */
public class StoreScreen extends MenuScreenBase {
    private static final int WIDTH = 460;
    private static final int HEADER = 60;
    private static final int GAP = 8;
    /** Пока товары не пришли с сервера. */
    private static final int LOADING_HEIGHT = 160;
    private static final int LINE = 9;
    private static final int MIN_LINES = 2;
    /** Длиннее (у MVP 16 строк) — обрезаем, полностью — в подсказке при наведении. */
    private static final int MAX_LINES = 7;
    /** Под описанием: разделитель, цена, баланс и кнопка. */
    private static final int PRICE_BLOCK = 14 + 28 + MenuUi.BUTTON_HEIGHT;
    private static final int PAD = 12;
    private static final int TILE_HEIGHT = 56;
    private static final int ICON = 28;
    private static final int BACK_WIDTH = 84;
    private static final Identifier BANNER = Identifier.fromNamespaceAndPath(BdAdvance.MOD_ID,
            "textures/gui/shop_banner.png");

    private final ConfirmBox confirm = new ConfirmBox();
    private Page page;
    // под какую высоту окно расставлено: страница или товары сменились — пересчитываем
    private int laidOutHeight = -1;

    public StoreScreen(Page page) {
        super("Магазин");
        this.page = page;
    }

    // сервер открыл другую страницу магазина, пока наш экран открыт
    public void follow(Page next) {
        page = next;
        confirm.close();
    }

    @Override
    protected int canvasWidth() {
        return WIDTH;
    }

    // высота по содержимому страницы: шапка, плитки категорий, карточки по самому длинному описанию
    @Override
    protected int canvasHeight() {
        View view = view();
        if (view == null || view.categories().isEmpty() && view.products().isEmpty()) {
            return LOADING_HEIGHT;
        }
        int height = HEADER + GAP;
        if (!view.categories().isEmpty()) {
            height += TILE_HEIGHT + (view.products().isEmpty() ? 0 : GAP);
        }
        return height + (view.products().isEmpty() ? 0 : cardHeight(view));
    }

    private View view() {
        AbstractContainerMenu menu = menu();
        return menu == null ? null : StoreMenu.read(page, menu);
    }

    private int contentY() {
        return top + HEADER + GAP;
    }

    private int closeX() {
        return left + WIDTH - PAD - MenuUi.BUTTON_HEIGHT;
    }

    private int backX() {
        return closeX() - 6 - BACK_WIDTH;
    }

    private int buttonY() {
        return top + (HEADER - MenuUi.BUTTON_HEIGHT) / 2;
    }

    private int balanceRight() {
        return page == Page.MAIN ? closeX() - 6 : backX() - 6;
    }

    // карточки товаров: на главной под плитками категорий, в разделах — сразу под шапкой
    private int cardsY(View view) {
        return view.categories().isEmpty() ? contentY() : contentY() + TILE_HEIGHT + GAP;
    }

    // все карточки ряда одной высоты — по самому длинному описанию, но не меньше двух строк и не больше MAX_LINES
    private static int cardHeight(View view) {
        int width = columnWidth(view.products().size()) - PAD * 2;
        int lines = MIN_LINES;
        for (StoreItem product : view.products()) {
            lines = Math.max(lines, Math.min(MAX_LINES + 1, description(product, width).size()));
        }
        return PAD + ICON + 10 + lines * LINE + PRICE_BLOCK + PAD;
    }

    // описание, перенесённое по ширине карточки
    private static List<String> description(StoreItem product, int width) {
        List<String> lines = new ArrayList<>();
        for (String line : product.lines()) {
            lines.addAll(MenuUi.wrap(line, width, 3, TextStyle.SMALL));
        }
        return lines;
    }

    private static int columnWidth(int count) {
        return (WIDTH - GAP * (count - 1)) / Math.max(1, count);
    }

    private int columnX(int index, int count) {
        return left + index * (columnWidth(count) + GAP);
    }

    private int buyY(View view) {
        return cardsY(view) + cardHeight(view) - PAD - MenuUi.BUTTON_HEIGHT;
    }

    @Override
    protected void drawCanvas(Gfx g, int mouseX, int mouseY) {
        View view = view();
        if (view == null) {
            return;
        }
        if (canvasHeight() != laidOutHeight) {
            laidOutHeight = canvasHeight();
            init();
        }
        int hoverX = confirm.isOpen() ? -1000 : mouseX;
        drawHeader(g, hoverX, mouseY);
        List<StoreItem> categories = view.categories();
        for (int i = 0; i < categories.size(); i++) {
            drawCategory(g, categories.get(i), columnX(i, categories.size()), columnWidth(categories.size()), hoverX, mouseY);
        }
        List<StoreItem> products = view.products();
        if (products.isEmpty() && categories.isEmpty()) {
            Draw.textCentered(g, "Загружаю товары…", left + WIDTH / 2f, contentY() + 40, Theme.TEXT_DIM);
        }
        for (int i = 0; i < products.size(); i++) {
            drawProduct(g, view, products.get(i), columnX(i, products.size()), columnWidth(products.size()), hoverX, mouseY);
        }
        if (confirm.isOpen()) {
            Draw.pushOffset(g, left, top);
            confirm.draw(g, WIDTH, laidOutHeight, mouseX - left, mouseY - top);
            Draw.popOffset(g);
        }
    }

    private void drawHeader(Gfx g, int mouseX, int mouseY) {
        MenuUi.pixelBanner(g, BANNER, left, top, WIDTH, HEADER);
        MenuUi.display(g, page.title().toUpperCase(Locale.ROOT), left + PAD + 2, top + PAD, 2f, MenuUi.GOLD);
        Draw.text(g, page.subtitle(), left + PAD + 3, top + HEADER - PAD - 7, Colors.fade(Theme.TEXT, 0.8f));
        if (page != Page.MAIN) {
            boolean backHovered = inside(mouseX, mouseY, backX(), buttonY(), BACK_WIDTH, MenuUi.BUTTON_HEIGHT);
            MenuUi.button(g, backX(), buttonY(), BACK_WIDTH, Icon.ARROW_LEFT, "Назад", ButtonStyle.LIGHT, true, backHovered);
        }
        MenuUi.balance(g, balanceRight(), buttonY(), BloodBalance.current());
        boolean closeHovered = inside(mouseX, mouseY, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT);
        MenuUi.button(g, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, Icon.CLOSE, null, ButtonStyle.OUTLINE, true, closeHovered);
    }

    private void drawCategory(Gfx g, StoreItem category, int x, int w, int mouseX, int mouseY) {
        int y = contentY();
        boolean hovered = inside(mouseX, mouseY, x, y, w, TILE_HEIGHT);
        MenuUi.panel(g, x, y, w, TILE_HEIGHT);
        if (hovered) {
            Draw.border(g, x, y, w, TILE_HEIGHT, Theme.PANEL_RADIUS, Colors.fade(StoreIcons.color(category), 0.8f));
        }
        StoreIcons.draw(g, category, x + 9, y + (TILE_HEIGHT - ICON) / 2f, ICON);
        float textX = x + 9 + ICON + 8;
        int textW = (int) (x + w - 16 - textX);
        Draw.text(g, Draw.trim(category.name(), textW, TextStyle.BODY_MEDIUM), textX, y + TILE_HEIGHT / 2f - 9,
                Theme.TEXT, TextStyle.BODY_MEDIUM);
        String line = category.lines().isEmpty() ? "" : category.lines().get(0);
        Draw.text(g, Draw.trim(line, textW, TextStyle.SMALL), textX, y + TILE_HEIGHT / 2f + 2, Theme.TEXT_DIM, TextStyle.SMALL);
        Draw.icon(g, Icon.CHEVRON_RIGHT, x + w - 14, y + (TILE_HEIGHT - Theme.ICON_SMALL) / 2f, Theme.ICON_SMALL,
                hovered ? Theme.TEXT : Theme.TEXT_DIM);
    }

    private void drawProduct(Gfx g, View view, StoreItem product, int x, int w, int mouseX, int mouseY) {
        int y = cardsY(view);
        int h = cardHeight(view);
        int color = StoreIcons.color(product);
        MenuUi.panel(g, x, y, w, h);
        Draw.gradient(g, x, y, w, 48, Theme.PANEL_RADIUS, Colors.fade(color, 0.14f), Colors.fade(color, 0));
        Draw.border(g, x, y, w, h, Theme.PANEL_RADIUS, Colors.fade(color, inside(mouseX, mouseY, x, y, w, h) ? 0.7f : 0.3f));
        StoreIcons.draw(g, product, x + PAD, y + PAD, ICON);
        int innerW = w - PAD * 2;
        int nameX = x + PAD + ICON + 8;
        List<String> name = MenuUi.wrap(product.name(), x + w - PAD - nameX, 2, TextStyle.BODY_MEDIUM);
        float nameY = y + PAD + (name.size() == 1 ? 10 : 4);
        for (String part : name) {
            Draw.text(g, part, nameX, nameY, Theme.TEXT, TextStyle.BODY_MEDIUM);
            nameY += 10;
        }
        drawLines(g, product, x + PAD, y + PAD + ICON + 10, innerW);
        drawPrice(g, view, product, x, w, color, mouseX, mouseY);
    }

    // описание до MAX_LINES строк; длиннее — последней строкой подсказка, полностью — при наведении
    private static void drawLines(Gfx g, StoreItem product, int x, int y, int w) {
        List<String> lines = description(product, w);
        boolean cut = lines.size() > MAX_LINES;
        int shown = cut ? MAX_LINES : lines.size();
        for (int i = 0; i < shown; i++) {
            Draw.text(g, lines.get(i), x, y + i * LINE, Theme.TEXT_SOFT, TextStyle.SMALL);
        }
        if (cut) {
            Draw.text(g, "Ещё " + (lines.size() - MAX_LINES) + " — наведи на карточку", x, y + MAX_LINES * LINE,
                    Theme.TEXT_DIM, TextStyle.SMALL);
        }
    }

    private int priceY(View view) {
        return buyY(view) - 28;
    }

    private void drawPrice(Gfx g, View view, StoreItem product, int x, int w, int color, int mouseX, int mouseY) {
        int priceY = priceY(view);
        Draw.rect(g, x + PAD, priceY - 6, w - PAD * 2, 1, Theme.BORDER_HOVER);
        String price = String.valueOf(product.price());
        MenuUi.display(g, price, x + PAD, priceY, 1.4f, Theme.TEXT);
        float unitX = x + PAD + 4 + MenuUi.displayWidth(price, 1.4f);
        Draw.text(g, "бладиков", unitX, priceY + 4, Theme.TEXT_SOFT, TextStyle.SMALL);
        drawBalance(g, product, x, w, priceY, unitX + Draw.width("бладиков", TextStyle.SMALL));
        int buttonW = w - PAD * 2;
        boolean enabled = product.buyable();
        boolean hovered = enabled && inside(mouseX, mouseY, x + PAD, buyY(view), buttonW, MenuUi.BUTTON_HEIGHT);
        String label = Draw.trim(buttonLabel(product), buttonW - 22, TextStyle.BODY);
        // как в покупке Premium: золотая, если хватает; иначе обычная, но нажимается — ответит сервер
        ButtonStyle style = enabled && product.affordable() ? ButtonStyle.GOLD : ButtonStyle.OUTLINE;
        MenuUi.button(g, x + PAD, buyY(view), buttonW, enabled ? Icon.CROWN : Icon.LOCK, label, style, enabled, hovered);
    }

    // «Баланс N» или красным «Не хватает N»: справа от цены, а в узкой карточке — под ней
    private static void drawBalance(Gfx g, StoreItem product, int x, int w, int priceY, float priceRight) {
        String text = product.affordable() ? "Баланс " + product.balance() : "Не хватает " + (product.price() - product.balance());
        int color = product.affordable() ? Theme.TEXT_DIM : MenuUi.RED;
        float right = x + w - PAD;
        if (priceRight + 8 + Draw.width(text) <= right) {
            Draw.text(g, text, right - Draw.width(text), priceY + 4, color);
        } else {
            Draw.text(g, Draw.trim(text, w - PAD * 2, TextStyle.SMALL), x + PAD, priceY + 15, color, TextStyle.SMALL);
        }
    }

    private static String buttonLabel(StoreItem product) {
        if (!product.buyable()) {
            return product.status().isEmpty() ? "Недоступно" : product.status();
        }
        return "Купить за " + product.price();
    }

    @Override
    protected void drawTooltips(Gfx g, int canvasX, int canvasY, int screenX, int screenY) {
        View view = view();
        if (view == null || confirm.isOpen()) {
            return;
        }
        ItemStack hovered = ItemStack.EMPTY;
        List<StoreItem> products = view.products();
        for (int i = 0; i < products.size(); i++) {
            int x = columnX(i, products.size());
            int w = columnWidth(products.size());
            if (inside(canvasX, canvasY, x, cardsY(view), w, buyY(view) - cardsY(view) - 4)) {
                hovered = products.get(i).stack();
            }
        }
        float balanceLeft = balanceRight() - 60;
        if (inside(canvasX, canvasY, balanceLeft, buttonY(), 60, MenuUi.BUTTON_HEIGHT)) {
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
        if (confirm.isOpen()) {
            confirm.click(x - left, y - top);
            return true;
        }
        if (inside(x, y, closeX(), buttonY(), MenuUi.BUTTON_HEIGHT, MenuUi.BUTTON_HEIGHT)) {
            onClose();
            return true;
        }
        if (page != Page.MAIN && view.backSlot() >= 0 && inside(x, y, backX(), buttonY(), BACK_WIDTH, MenuUi.BUTTON_HEIGHT)) {
            clickSlot(view.backSlot(), false);
            return true;
        }
        List<StoreItem> categories = view.categories();
        for (int i = 0; i < categories.size(); i++) {
            if (inside(x, y, columnX(i, categories.size()), contentY(), columnWidth(categories.size()), TILE_HEIGHT)) {
                clickSlot(categories.get(i).slot(), false);
                return true;
            }
        }
        List<StoreItem> products = view.products();
        for (int i = 0; i < products.size(); i++) {
            StoreItem product = products.get(i);
            int buttonX = columnX(i, products.size()) + PAD;
            if (product.buyable() && inside(x, y, buttonX, buyY(view), columnWidth(products.size()) - PAD * 2, MenuUi.BUTTON_HEIGHT)) {
                if (product.affordable()) {
                    askToBuy(product);
                } else {
                    // бладиков не хватает: покупки не будет, сервер сам скажет сколько и даст ссылку на сайт
                    clickSlot(product.slot(), false);
                }
                return true;
            }
        }
        return false;
    }

    // сервер покупает сразу по клику на товар — сначала спрашиваем
    private void askToBuy(StoreItem product) {
        int slot = product.slot();
        confirm.open(new ConfirmBox.Request("Купить «" + product.name() + "» за " + product.price() + " бладиков?",
                null, false, "Купить", false, ignored -> clickSlot(slot, false)), WIDTH, laidOutHeight);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (confirm.isOpen()) {
            int key = event.key();
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                confirm.confirm();
            } else if (key == InputConstants.KEY_ESCAPE) {
                confirm.close();
            }
            return true;
        }
        return super.keyPressed(event);
    }
}
