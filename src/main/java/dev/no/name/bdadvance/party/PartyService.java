package dev.no.name.bdadvance.party;

import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PartyService {
    private static final long INFO_WINDOW_MS = 3000;

    private static final long ACTION_WINDOW_MS = 3000;

    private static final long REFRESH_AFTER_ACTION_MS = 1000;

    private static final long AUTO_REFRESH_MS = 20_000;
    private static final long MIN_REFRESH_GAP_MS = 3000;
    private static final Set<String> NO_PLAYER = Set.of("no player was found", "игрок не обнаружен");

    private static final List<String> KICK_VERBS = List.of("kick", "remove");

    private final PartyState state = new PartyState();
    private long hideInfoUntil;
    private long actionUntil;
    private long refreshAt;
    private long lastRefresh;
    private boolean panelOpen;

    public void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> state.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            state.reset();
            refreshAt = 0;
            panelOpen = false;
        });
    }

    public PartyState state() {
        return state;
    }

    public void setPanelOpen(boolean open) {
        panelOpen = open;
        if (open && now() - lastRefresh > MIN_REFRESH_GAP_MS) {
            refresh();
        }
    }

    public void tick() {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        long now = now();
        if (refreshAt > 0 && now >= refreshAt || panelOpen && now - lastRefresh > AUTO_REFRESH_MS) {
            refresh();
        }
    }

    public void refresh() {
        long now = now();
        refreshAt = 0;
        lastRefresh = now;
        if (send("party info")) {
            hideInfoUntil = now + INFO_WINDOW_MS;
        }
    }

    public void invite(String name) {
        action("party invite " + name);
    }

    public boolean kick(String name) {
        String verb = kickVerb();
        if (verb == null) {
            state.setStatus("На сервере нет команды кика из пати");
            return false;
        }
        action("party " + verb + " " + name);
        return true;
    }

    public boolean canKick() {
        return kickVerb() != null;
    }

    public void disband() {
        action("party disband");
    }

    public void leave() {
        action("party leave");
    }

    public void accept() {
        String command = state.inviteCommand();
        String from = state.incomingInvite(now());
        state.clearInvite();
        if (command == null) {
            command = from != null ? "party accept " + from : "party accept";
        }
        action(command);
    }

    // вступить по приглашению из ЛС: зовущий известен по сообщению
    public void acceptFrom(String leader) {
        state.clearInvite();
        action("party accept " + leader);
    }

    public boolean consume(String plain, List<String> clickCommands) {
        long now = now();
        PartyEvent event = PartyMessages.parse(plain);
        if (event == null) {
            if (now < actionUntil && NO_PLAYER.contains(plain.strip().toLowerCase(Locale.ROOT))) {
                state.setStatus("Игрок не найден");
                return true;
            }
            return false;
        }
        state.apply(event, now);
        if (event instanceof PartyEvent.IncomingInvite) {
            state.setInviteCommand(PartyMessages.acceptCommand(clickCommands));
        }
        boolean answer = event instanceof PartyEvent.Info || event instanceof PartyEvent.NotInParty;
        if (answer && now < hideInfoUntil) {
            hideInfoUntil = 0;
            return true;
        }
        if (!answer) {
            refreshAt = now + REFRESH_AFTER_ACTION_MS;
        }
        // Ответ на кнопку из панели уже виден статусом в панели, дублировать его в чат незачем.
        boolean fromPanel = now < actionUntil && !(event instanceof PartyEvent.IncomingInvite);
        if (fromPanel) {
            actionUntil = 0;
        }
        return fromPanel;
    }

    private void action(String command) {
        if (send(command)) {
            long now = now();
            actionUntil = now + ACTION_WINDOW_MS;
            refreshAt = now + REFRESH_AFTER_ACTION_MS;
        }
    }

    private static String kickVerb() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return null;
        }
        CommandNode<?> party = connection.getCommands().getRoot().getChild("party");
        if (party == null) {
            return null;
        }
        for (String verb : KICK_VERBS) {
            if (party.getChild(verb) != null) {
                return verb;
            }
        }
        return null;
    }

    private static boolean send(String command) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        connection.sendCommand(command);
        return true;
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
