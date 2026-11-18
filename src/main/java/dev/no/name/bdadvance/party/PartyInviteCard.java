package dev.no.name.bdadvance.party;

import dev.no.name.afriends.api.MessageCard;
import dev.no.name.bdadvance.modes.BloodstoneServer;
import dev.no.name.afriends.compat.Compat;
import dev.no.name.afriends.gui.render.icon.Icon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Приглашение в пати в ЛС: карточка с кнопкой «Вступить» вместо текста. Само сообщение — обычный текст
 * с командой, поэтому старые версии мода и /msg сервера покажут его как есть, и по нему тоже можно вступить
 */
public final class PartyInviteCard implements MessageCard {
    private static final String PREFIX = "[Пати] Зову тебя в пати на Bloodstone: /party accept ";
    private static final Pattern TEXT = Pattern.compile("^" + Pattern.quote(PREFIX) + "([A-Za-z0-9_]{1,16})$");

    private final PartyService party;

    public PartyInviteCard(PartyService party) {
        this.party = party;
    }

    // текст приглашения от игрока leader
    public static String text(String leader) {
        return PREFIX + leader;
    }

    @Override
    public Card card(String partner, String text, boolean fromMe) {
        Matcher matcher = TEXT.matcher(text == null ? "" : text.strip());
        if (!matcher.matches()) {
            return null;
        }
        String leader = matcher.group(1);
        if (fromMe) {
            return new Card(Icon.FLAG, "Приглашение в пати", "Ты позвал " + partner, null, true, () -> { });
        }
        boolean joined = party.state().inParty();
        return new Card(Icon.FLAG, "Приглашение в пати", leader + " зовёт тебя на Bloodstone",
                joined ? "Ты в пати" : "Вступить", joined, () -> join(leader));
    }

    private void join(String leader) {
        if (!BloodstoneServer.isConnected()) {
            Compat.localMessage(Component.literal("[Bloodstone] Зайди на Bloodstone, чтобы вступить в пати к " + leader)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        party.acceptFrom(leader);
    }
}
