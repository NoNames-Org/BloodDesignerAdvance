package dev.no.name.bdadvance;

import dev.no.name.afriends.api.AFriendsAddon;
import dev.no.name.afriends.api.AFriendsApi;
import dev.no.name.afriends.api.FriendAction;
import dev.no.name.afriends.api.SettingsCategory;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.bdadvance.AdvanceFeatures.Feature;
import dev.no.name.bdadvance.battlepass.BattlePassNavigator;
import dev.no.name.bdadvance.modes.BloodstoneServer;
import dev.no.name.bdadvance.modes.ModeFavorites;
import dev.no.name.bdadvance.modes.ModeLauncher;
import dev.no.name.bdadvance.modes.gui.PauseMenuHook;
import dev.no.name.bdadvance.party.PartyChatFilter;
import dev.no.name.bdadvance.party.PartyInviteCard;
import dev.no.name.bdadvance.party.PartyRailPanel;
import dev.no.name.bdadvance.party.PartyService;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * BloodDesignerAdvance — аддон AFriends для bloodstone.boo и extra.bloodstone.boo: пати, меню батл пасса и
 * магазина, навигатор режимов. Всё встраивается через API AFriends; без AFriends мод не загружается.
 */
public final class BdAdvanceAddon implements AFriendsAddon {
    private static final String NOTE = "Выключенное меню пропадает сразу, кнопки в чате — при следующем открытии чата.";

    @Override
    public void register(AFriendsApi api) {
        ModeFavorites favorites = new ModeFavorites();
        AdvanceConfig config = new AdvanceConfig(favorites, configDir(BdAdvance.MOD_ID).resolve("config.json"),
                List.of(configDir("afriends").resolve("config.json"), configDir("blooddesigner").resolve("config.json")));
        config.load();
        favorites.setOnChange(config::save);
        AdvanceFeatures.setOnChange(config::save);

        PartyService party = new PartyService();
        party.register();
        new PartyChatFilter(party).register();
        api.addRailPanel(new PartyRailPanel(party, api.friends()));
        api.addMessageCard(new PartyInviteCard(party));
        api.addFriendAction(inviteAction(api, party));

        ModeLauncher launcher = new ModeLauncher();
        launcher.register();
        new PauseMenuHook(favorites, launcher).register();
        BattlePassNavigator.register();

        api.addSettingsCategory(SettingsCategory.toggles("Bloodstone", "Меню BloodDesignerAdvance для bloodstone.boo",
                Icon.FLAG, Arrays.stream(Feature.values())
                        .map(feature -> new SettingsCategory.Toggle(feature.title(), feature.description(),
                                () -> AdvanceFeatures.enabled(feature), () -> AdvanceFeatures.toggle(feature)))
                        .toList(), NOTE));

        ClientTickEvents.END_CLIENT_TICK.register(client -> party.tick());
    }

    // «Позвать в пати» в меню друга и переписки: только на Bloodstone и только тем, кто сейчас в игре.
    // Тому, кто рядом, — командой; другу по сети — ещё и карточкой в ЛС, чтобы он мог вступить с другого сервера
    private static FriendAction inviteAction(AFriendsApi api, PartyService party) {
        return new FriendAction(Icon.FLAG, name -> "Позвать в пати", name -> {
            if (api.friends().nearby(name)) {
                party.invite(name);
            }
            if (api.viaNetwork(name)) {
                api.sendMessage(name, PartyInviteCard.text(Minecraft.getInstance().getUser().getName()));
            }
        }, name -> AdvanceFeatures.enabled(Feature.PARTY) && BloodstoneServer.isConnected() && api.friends().online(name),
                false);
    }

    private static Path configDir(String mod) {
        return FabricLoader.getInstance().getConfigDir().resolve(mod);
    }
}
