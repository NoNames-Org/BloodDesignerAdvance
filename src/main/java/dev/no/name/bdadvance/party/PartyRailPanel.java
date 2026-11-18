package dev.no.name.bdadvance.party;

import dev.no.name.afriends.api.Friends;
import dev.no.name.afriends.api.RailPanel;
import dev.no.name.afriends.api.SidePanel;
import dev.no.name.afriends.compat.WidgetBase;
import dev.no.name.afriends.gui.hub.PanelState;
import dev.no.name.afriends.gui.render.icon.Icon;
import dev.no.name.bdadvance.AdvanceFeatures;
import dev.no.name.bdadvance.AdvanceFeatures.Feature;
import dev.no.name.bdadvance.modes.BloodstoneServer;
import dev.no.name.bdadvance.party.gui.PartyPanel;
import net.minecraft.client.input.KeyEvent;

/** Кнопка «Пати» в панели AFriends: только на Bloodstone, где есть команды /party. */
public final class PartyRailPanel implements RailPanel {
    private final PartyService party;
    private final Friends friends;

    public PartyRailPanel(PartyService party, Friends friends) {
        this.party = party;
        this.friends = friends;
    }

    @Override
    public Icon icon() {
        return Icon.FLAG;
    }

    @Override
    public String label() {
        return "Пати";
    }

    @Override
    public boolean visible() {
        return AdvanceFeatures.enabled(Feature.PARTY) && BloodstoneServer.isConnected();
    }

    @Override
    public String badge() {
        PartyState state = party.state();
        if (state.incomingInvite(System.currentTimeMillis()) != null) {
            return "!";
        }
        int members = state.members().size();
        return members > 1 ? String.valueOf(members) : null;
    }

    @Override
    public SidePanel create(Bounds bounds, PanelState state) {
        PartyPanel panel = new PartyPanel(bounds.x(), bounds.y(), bounds.width(), bounds.height(), party, friends, state);
        return new SidePanel() {
            @Override
            public WidgetBase widget() {
                return panel;
            }

            @Override
            public boolean handleKey(KeyEvent event) {
                return panel.handleKey(event);
            }

            @Override
            public void blur() {
                panel.blur();
            }

            @Override
            public void opened(boolean open) {
                party.setPanelOpen(open);
            }
        };
    }
}
