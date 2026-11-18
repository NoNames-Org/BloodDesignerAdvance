package dev.no.name.bdadvance.party.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.no.name.afriends.compat.Gfx;
import dev.no.name.afriends.compat.WidgetBase;
import dev.no.name.afriends.gui.hub.PanelState;
import dev.no.name.afriends.api.Friends;
import dev.no.name.bdadvance.party.NameSuggestions;
import dev.no.name.bdadvance.party.NameSuggestions.Suggestion;
import dev.no.name.bdadvance.party.PartyService;
import dev.no.name.bdadvance.party.PartyState;
import dev.no.name.afriends.gui.Theme;
import dev.no.name.afriends.gui.render.Colors;
import dev.no.name.afriends.gui.render.Draw;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.afriends.gui.render.text.TextStyle;
import dev.no.name.afriends.gui.sound.UiSound;
import dev.no.name.afriends.gui.sound.UiSounds;
import dev.no.name.afriends.gui.widget.ButtonWidget;
import dev.no.name.afriends.gui.widget.TextFieldWidget;
import dev.no.name.afriends.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.regex.Pattern;

public class PartyPanel extends WidgetBase {
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 36;
    private static final int OWN_FACE = 20;
    private static final int FIELD_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 16;
    private static final int GAP = 4;
    private static final int INVITE_HEIGHT = 40;
    private static final int ROW_HEIGHT = 18;
    private static final int FACE = 12;
    private static final int STATUS_HEIGHT = 14;
    private static final int SUGGESTION_HEIGHT = 13;
    private static final int MAX_SUGGESTIONS = 6;
    private static final int NAME_LENGTH = 16;
    private static final float DISABLED_ALPHA = 0.35f;
    private static final Pattern NAME_CHAR = Pattern.compile("[A-Za-z0-9_]");

    private final PartyService party;
    private final Friends friends;
    private final PanelState state;
    private final TextFieldWidget field = new TextFieldWidget(Icon.SEARCH, "Ник игрока").withMaxLength(NAME_LENGTH);
    private final ButtonWidget invite = new ButtonWidget(Icon.PLUS, "Пригласить", this::invite);
    private final ButtonWidget kick = new ButtonWidget(Icon.MINUS, "Кикнуть", this::kick);
    private final ButtonWidget disband;
    private final ButtonWidget leave;
    private final ButtonWidget accept;
    private List<Suggestion> suggestions = List.of();
    private int selected;
    private boolean takeFocus;

    private boolean inviteShown;
    private int inviteTop;
    private int membersTop;

    private final int panelWidth;

    public PartyPanel(int x, int y, int width, int height, PartyService party, Friends friends, PanelState state) {
        super(x, y, width, height, Component.literal("Пати"));
        this.panelWidth = width;
        this.party = party;
        this.friends = friends;
        this.state = state;
        this.disband = new ButtonWidget(Icon.TRASH, "Распустить", party::disband);
        this.leave = new ButtonWidget(Icon.LOG_OUT, "Выйти", party::leave);
        this.accept = new ButtonWidget(Icon.CHECK, "Принять", party::accept);
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
        float open = state.progress();
        if (open <= 0.01f) {
            return;
        }
        Draw.setPixelScale(Minecraft.getInstance().getWindow().getGuiScale());
        layout();
        refreshSuggestions();

        int mx = state.isOpen() && open > 0.99f ? mouseX : -1000;
        int my = mouseY;

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((1 - open) * (panelWidth + 8), 0);
        Draw.pushAlpha(open);
        Draw.shadow(g, getX(), getY() + 2, panelWidth, height, Theme.PANEL_RADIUS, 14, Colors.fade(Theme.SHADOW, 0.7f));
        Draw.gradient(g, getX(), getY(), panelWidth, height, Theme.PANEL_RADIUS,
                Colors.fade(Theme.PANEL_TOP, 0.92f), Colors.fade(Theme.PANEL_BOTTOM, 0.92f));
        Draw.border(g, getX(), getY(), panelWidth, height, Theme.PANEL_RADIUS, Theme.BORDER);

        PartyState partyState = party.state();
        drawHeader(g, partyState);
        if (inviteShown) {
            drawInvite(g, partyState, mx, my);
        }
        field.render(g, mx, my);
        invite.render(g, mx, my);
        drawButton(g, kick, party.canKick(), mx, my);
        drawButton(g, disband, isSelfLeader(partyState), mx, my);
        drawButton(g, leave, partyState.inParty(), mx, my);
        drawMembers(g, partyState, mx, my);
        drawStatus(g, partyState);
        drawSuggestions(g, mx, my);

        Draw.popAlpha();
        pose.popMatrix();
    }

    private void layout() {
        int x = getX() + PADDING;
        int width = panelWidth - PADDING * 2;
        int half = (width - GAP) / 2;
        int y = getY() + HEADER_HEIGHT + 6;
        inviteShown = party.state().incomingInvite(System.currentTimeMillis()) != null;
        if (inviteShown) {
            inviteTop = y;
            accept.setBounds(x + 6, y + INVITE_HEIGHT - BUTTON_HEIGHT - 6, width - 12, BUTTON_HEIGHT);
            y += INVITE_HEIGHT + GAP;
        }
        field.setBounds(x, y, width, FIELD_HEIGHT);
        y += FIELD_HEIGHT + GAP;
        invite.setBounds(x, y, half, BUTTON_HEIGHT);
        kick.setBounds(x + half + GAP, y, half, BUTTON_HEIGHT);
        y += BUTTON_HEIGHT + GAP;
        disband.setBounds(x, y, half, BUTTON_HEIGHT);
        leave.setBounds(x + half + GAP, y, half, BUTTON_HEIGHT);
        membersTop = y + BUTTON_HEIGHT + 10;
    }

    private void drawHeader(Gfx g, PartyState partyState) {
        LocalPlayer self = Minecraft.getInstance().player;
        int faceX = getX() + PADDING;
        int faceY = getY() + (HEADER_HEIGHT - OWN_FACE) / 2;
        if (self != null) {
            g.face(self.getSkin(), faceX, faceY, OWN_FACE);
        }
        float textX = faceX + OWN_FACE + 7;
        Draw.text(g, "Пати", textX, getY() + 10, Theme.TEXT, TextStyle.BODY_MEDIUM);
        String summary = partyState.inParty()
                ? "Лидер: " + partyState.leader() + "  ·  " + partyState.members().size()
                : "Ты не в пати";
        Draw.text(g, Draw.trim(summary, (int) (getX() + panelWidth - PADDING - textX)), textX, getY() + 22,
                Theme.TEXT_DIM, TextStyle.SMALL);
        Draw.rect(g, getX() + PADDING, getY() + HEADER_HEIGHT, panelWidth - PADDING * 2, 1, Theme.BORDER);
    }

    private void drawInvite(Gfx g, PartyState partyState, int mouseX, int mouseY) {
        int x = getX() + PADDING;
        int width = panelWidth - PADDING * 2;
        Draw.box(g, x, inviteTop, width, INVITE_HEIGHT, Theme.RADIUS, Theme.CARD_SELECTED, Theme.BORDER_SELECTED);
        String from = partyState.incomingInvite(System.currentTimeMillis());
        Draw.icon(g, Icon.FLAG, x + 6, inviteTop + 6, Theme.ICON_SMALL, Theme.ACCENT);
        Draw.text(g, Draw.trim(from + " зовёт в пати", width - 24), x + 18, inviteTop + 6, Theme.TEXT);
        accept.render(g, mouseX, mouseY);
    }

    private static void drawButton(Gfx g, ButtonWidget button, boolean enabled, int mouseX, int mouseY) {
        Draw.pushAlpha(enabled ? 1 : DISABLED_ALPHA);
        button.render(g, enabled ? mouseX : -1000, mouseY);
        Draw.popAlpha();
    }

    private void drawMembers(Gfx g, PartyState partyState, int mouseX, int mouseY) {
        int x = getX() + PADDING;
        List<String> members = partyState.members();
        Draw.text(g, "Участники" + (members.isEmpty() ? "" : "  ·  " + members.size()), x, membersTop,
                Theme.TEXT_DIM, TextStyle.SMALL);
        int top = membersTop + 10;
        int bottom = getY() + height - STATUS_HEIGHT - 4;
        if (members.isEmpty()) {
            float centerX = getX() + panelWidth / 2f;
            float centerY = (top + bottom) / 2f;
            Draw.icon(g, Icon.FLAG, centerX - 9, centerY - 24, 18, Theme.TEXT_FAINT);
            Draw.textCentered(g, "Пати пока нет", centerX, centerY, Theme.TEXT_DIM);
            String hint = "Впиши ник и жми «Пригласить»";
            Draw.text(g, hint, centerX - Draw.width(hint, TextStyle.SMALL) / 2f, centerY + 12, Theme.TEXT_FAINT, TextStyle.SMALL);
            return;
        }
        g.enableScissor(getX(), top, getX() + panelWidth, bottom);
        for (int i = 0; i < members.size(); i++) {
            int rowY = top + i * ROW_HEIGHT;
            if (rowY > bottom) {
                break;
            }
            drawMember(g, members.get(i), partyState.isLeader(members.get(i)), rowY, mouseX, mouseY);
        }
        g.disableScissor();
    }

    private void drawMember(Gfx g, String name, boolean leader, int rowY, int mouseX, int mouseY) {
        int rowX = getX() + 4;
        int rowWidth = panelWidth - 8;
        boolean hovered = mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
        if (hovered) {
            Draw.roundedRect(g, rowX, rowY, rowWidth, ROW_HEIGHT, Theme.RADIUS, Theme.CARD_HOVER);
        }
        int faceX = getX() + PADDING;
        int faceY = rowY + (ROW_HEIGHT - FACE) / 2;
        PlayerSkin skin = friends.skin(name);
        if (skin != null) {
            g.face(skin, faceX, faceY, FACE);
        } else {
            Draw.gradient(g, faceX, faceY, FACE, FACE, 2, Theme.TILE_ON, Theme.TILE);
            Draw.icon(g, Icon.USER, faceX + 2, faceY + 2, FACE - 4, Theme.TEXT_DIM);
        }
        float textX = faceX + FACE + 6;
        int textWidth = (int) (getX() + panelWidth - PADDING - textX - (leader ? 14 : 0));
        Draw.text(g, Draw.trim(name, textWidth), textX, rowY + (ROW_HEIGHT - 8) / 2f, Theme.TEXT, TextStyle.BODY_MEDIUM);
        if (leader) {
            Draw.icon(g, Icon.CROWN, getX() + panelWidth - PADDING - Theme.ICON_SMALL, rowY + (ROW_HEIGHT - Theme.ICON_SMALL) / 2f,
                    Theme.ICON_SMALL, Theme.ACCENT);
        }
    }

    private void drawStatus(Gfx g, PartyState partyState) {
        String status = partyState.status();
        if (status.isEmpty()) {
            return;
        }
        int y = getY() + height - STATUS_HEIGHT;
        Draw.rect(g, getX() + PADDING, y - 3, panelWidth - PADDING * 2, 1, Theme.BORDER);
        Draw.text(g, Draw.trim(status, panelWidth - PADDING * 2), getX() + PADDING, y + 2, Theme.TEXT_DIM, TextStyle.SMALL);
    }

    private void drawSuggestions(Gfx g, int mouseX, int mouseY) {
        if (suggestions.isEmpty()) {
            return;
        }
        int x = field.getX();
        int top = suggestionsTop();
        int width = field.getWidth();
        int listHeight = suggestions.size() * SUGGESTION_HEIGHT + 4;
        Draw.shadow(g, x, top + 2, width, listHeight, Theme.RADIUS + 2, 10, Theme.SHADOW);
        Draw.box(g, x, top, width, listHeight, Theme.RADIUS + 2, Theme.POPUP, Theme.BORDER_HOVER);
        for (int i = 0; i < suggestions.size(); i++) {
            Suggestion suggestion = suggestions.get(i);
            int rowY = top + 2 + i * SUGGESTION_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + SUGGESTION_HEIGHT;
            if (hovered || i == selected) {
                Draw.roundedRect(g, x + 2, rowY, width - 4, SUGGESTION_HEIGHT, Theme.RADIUS, Theme.CARD_HOVER);
            }
            PlayerSkin skin = friends.skin(suggestion.name());
            int faceX = x + 5;
            int faceY = rowY + 2;
            if (skin != null) {
                g.face(skin, faceX, faceY, 9);
            } else {
                Draw.icon(g, Icon.USER, faceX, faceY, 9, Theme.TEXT_FAINT);
            }
            Draw.text(g, Draw.trim(suggestion.name(), width - 34), faceX + 13, rowY + 2.5f,
                    i == selected ? Theme.TEXT : Theme.TEXT_SOFT);
            if (suggestion.friend()) {
                Draw.icon(g, Icon.USERS, x + width - 6 - Theme.ICON_SMALL, rowY + (SUGGESTION_HEIGHT - Theme.ICON_SMALL) / 2f,
                        Theme.ICON_SMALL, Theme.TEXT_FAINT);
            }
        }
    }

    private int suggestionsTop() {
        return field.getY() + FIELD_HEIGHT + 2;
    }

    private void refreshSuggestions() {
        if (!field.isFocused()) {
            suggestions = List.of();
            return;
        }
        LocalPlayer self = Minecraft.getInstance().player;
        String selfName = self == null ? null : self.getGameProfile().name();
        // в пати зовём только тех, кто сейчас в игре: офлайн-друзей не подсказываем
        List<String> onlineFriends = friends.names().stream().filter(friends::online).toList();
        suggestions = NameSuggestions.suggest(field.getText(), onlineFriends, friends.nearbyNames(), selfName, MAX_SUGGESTIONS);
        selected = Math.max(0, Math.min(selected, suggestions.size() - 1));
    }

    private int suggestionAt(double mouseX, double mouseY) {
        if (suggestions.isEmpty() || mouseX < field.getX() || mouseX >= field.getX() + field.getWidth()) {
            return -1;
        }
        int index = (int) Math.floor((mouseY - suggestionsTop() - 2) / SUGGESTION_HEIGHT);
        return mouseY >= suggestionsTop() + 2 && index >= 0 && index < suggestions.size() ? index : -1;
    }

    private void applySuggestion(int index) {
        field.setText(suggestions.get(index).name());
        selected = 0;
        UiSounds.play(UiSound.CLICK);
    }

    private void invite() {
        String name = targetName();
        if (name != null) {
            party.invite(name);
            field.setText("");
        }
    }

    private void kick() {
        String name = targetName();
        if (name != null && party.kick(name)) {
            field.setText("");
        }
    }

    private String targetName() {
        String name = field.getText().strip();
        if (name.isEmpty()) {
            party.state().setStatus("Впиши ник игрока");
            return null;
        }
        return name;
    }

    private String memberAt(double mouseY) {
        List<String> members = party.state().members();
        int top = membersTop + 10;
        int index = (int) Math.floor((mouseY - top) / ROW_HEIGHT);
        return mouseY >= top && index >= 0 && index < members.size() ? members.get(index) : null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return state.isOpen() && state.progress() > 0.99f && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = Widget.LEFT_BUTTON;
        int hit = suggestionAt(mouseX, mouseY);
        if (hit >= 0) {
            applySuggestion(hit);
            takeFocus = true;
            return;
        }
        field.mouseClicked(mouseX, mouseY, button);
        takeFocus = field.isFocused();
        if (inviteShown && accept.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        boolean inParty = party.state().inParty();
        if (invite.mouseClicked(mouseX, mouseY, button)
                || party.canKick() && kick.mouseClicked(mouseX, mouseY, button)
                || isSelfLeader(party.state()) && disband.mouseClicked(mouseX, mouseY, button)
                || inParty && leave.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        String member = mouseY >= membersTop + 10 ? memberAt(mouseY) : null;
        if (member != null) {
            field.setText(member);
            field.focus();
            takeFocus = true;
            UiSounds.play(UiSound.SELECT);
        }
    }

    public boolean handleKey(KeyEvent event) {
        if (!field.isFocused() || !state.isOpen()) {
            return false;
        }
        int key = event.key();
        switch (key) {
            case InputConstants.KEY_ESCAPE -> {
                return false;
            }
            case InputConstants.KEY_TAB -> {
                if (!suggestions.isEmpty()) {
                    applySuggestion(selected);
                }
                return true;
            }
            case InputConstants.KEY_DOWN -> {
                selected = suggestions.isEmpty() ? 0 : (selected + 1) % suggestions.size();
                return true;
            }
            case InputConstants.KEY_UP -> {
                selected = suggestions.isEmpty() ? 0 : (selected - 1 + suggestions.size()) % suggestions.size();
                return true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                invite();
                return true;
            }
            default -> {
                field.keyPressed(key);
                selected = 0;
                return true;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return handleKey(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!field.isFocused()) {
            return false;
        }
        String typed = event.codepointAsString();
        if (NAME_CHAR.matcher(typed).matches()) {
            field.charTyped(typed);
            selected = 0;
        }
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            field.blur();
        } else {
            field.refocus();
        }
    }

    @Override
    public boolean wantsTextInput() {
        return field.isFocused();
    }

    public void blur() {
        field.blur();
    }

    public boolean isTyping() {
        return field.isFocused();
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return takeFocus;
    }

    private static boolean isSelfLeader(PartyState partyState) {
        LocalPlayer self = Minecraft.getInstance().player;
        return partyState.inParty() && self != null && partyState.isLeader(self.getGameProfile().name());
    }
}
