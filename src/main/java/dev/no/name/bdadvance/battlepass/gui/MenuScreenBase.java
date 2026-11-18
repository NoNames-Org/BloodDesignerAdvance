package dev.no.name.bdadvance.battlepass.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.no.name.afriends.compat.Compat;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.afriends.compat.ScreenBase;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.afriends.gui.animation.Animated;
import dev.no.name.afriends.gui.animation.Motion;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.blur.MenuBlur;
import dev.no.name.afriends.gui.sound.UiSound;
import dev.no.name.afriends.gui.sound.UiSounds;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.joml.Matrix3x2fStack;

/**
 * Общая основа экранов, которые рисуют серверное меню bloodstone по-своему: серверное меню остаётся
 * открытым, клики уходят в его слоты, Esc закрывает его и на сервере.
 */
abstract class MenuScreenBase extends ScreenBase {
    private static final float SCREEN_SHARE = 0.66f;
    private static final int MARGIN = 12;
    /** Переход между нашими экранами (страница, квесты, магазин) не должен заново проигрывать появление. */
    private static final long REOPEN_WINDOW_MS = 600;
    private static long lastClosedAt;

    private final Animated windowOpen = new Animated(0, Motion.SLOW);
    private float scale = 1;
    private int pixelScale = 1;
    protected int left;
    protected int top;

    protected MenuScreenBase(String title) {
        super(Component.literal(title));
        if (Motion.now() - lastClosedAt < REOPEN_WINDOW_MS) {
            windowOpen.snap(1);
        } else {
            UiSounds.play(UiSound.MENU_OPEN);
        }
        windowOpen.setTarget(1);
    }

    protected abstract int canvasWidth();

    protected abstract int canvasHeight();

    /** Рисует содержимое в координатах холста; мышь уже переведена в них. */
    protected abstract void drawCanvas(Gfx g, int mouseX, int mouseY);

    /** Подсказки поверх всего; {@code screenX/screenY} — настоящие координаты мыши для ванильной подсказки. */
    protected void drawTooltips(Gfx g, int canvasX, int canvasY, int screenX, int screenY) {
    }

    /** Клик в координатах холста. */
    protected abstract boolean onCanvasClick(double x, double y);

    @Override
    protected void init() {
        Window window = minecraft.getWindow();
        int byWidth = Math.round(window.getWidth() * SCREEN_SHARE / canvasWidth());
        int byHeight = window.getHeight() / (canvasHeight() + MARGIN * 2);
        pixelScale = Math.max(1, Math.min(byWidth, byHeight));
        scale = pixelScale / (float) window.getGuiScale();
        int fullWidth = window.getWidth() / pixelScale;
        int fullHeight = window.getHeight() / pixelScale;
        left = (fullWidth - canvasWidth()) / 2;
        top = Math.max(MARGIN, (fullHeight - canvasHeight()) / 2);
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
        if (menu() == null) {
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        Draw.setPixelScale(pixelScale);
        int x = (int) toCanvas(mouseX);
        int y = (int) toCanvas(mouseY);
        float shown = windowOpen.get();
        Draw.pushAlpha(shown);
        Draw.pushOffset(g, 0, (1 - shown) * Motion.RISE);
        drawCanvas(g, x, y);
        Draw.popOffset(g);
        Draw.popAlpha();
        pose.popMatrix();
        drawTooltips(g, x, y, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return menu() != null && onCanvasClick(toCanvas(event.x()), toCanvas(event.y()));
    }

    protected AbstractContainerMenu menu() {
        return minecraft == null || minecraft.player == null ? null : minecraft.player.containerMenu;
    }

    /** Клик по слоту серверного меню; с Shift — как Shift+ЛКМ (у наград это предпросмотр). */
    protected void clickSlot(int slot, boolean allowShift) {
        AbstractContainerMenu menu = menu();
        if (menu == null) {
            return;
        }
        if (allowShift && isShiftDown()) {
            Compat.quickMoveSlot(menu.containerId, slot);
        } else {
            Compat.clickSlot(menu.containerId, slot);
        }
        UiSounds.play(UiSound.CLICK);
    }

    protected static boolean inside(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static boolean isShiftDown() {
        return Compat.isKeyDown(InputConstants.KEY_LSHIFT) || Compat.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    /** Во сколько раз холст меньше экранных координат: экранная координата / это = координата холста. */
    protected float canvasScale() {
        return scale;
    }

    private double toCanvas(double screenValue) {
        return screenValue / scale;
    }

    @Override
    public void onClose() {
        Compat.closeContainer();
    }

    @Override
    public void removed() {
        lastClosedAt = Motion.now();
        MenuBlur.cancel();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
