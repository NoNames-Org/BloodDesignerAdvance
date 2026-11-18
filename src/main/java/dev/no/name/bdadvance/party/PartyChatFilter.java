package dev.no.name.bdadvance.party;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Прячет из чата системные сообщения пати: их показывает панель «Пати». */
public final class PartyChatFilter {
    private final PartyService party;

    public PartyChatFilter(PartyService party) {
        this.party = party;
    }

    public void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || !party.consume(message.getString(), clickCommands(message)));
    }

    private static List<String> clickCommands(Component message) {
        List<String> commands = new ArrayList<>();
        for (Component part : message.toFlatList()) {
            if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                commands.add(run.command());
            }
        }
        return commands;
    }
}
