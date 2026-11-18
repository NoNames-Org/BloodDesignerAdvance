package dev.no.name.bdadvance.party;

import java.util.List;

public sealed interface PartyEvent {
    record Info(String leader, List<String> members) implements PartyEvent {
        public Info {
            members = List.copyOf(members);
        }
    }

    record Invited(String name) implements PartyEvent {
    }

    record IncomingInvite(String from) implements PartyEvent {
    }

    record Left() implements PartyEvent {
    }

    record Disbanded() implements PartyEvent {
    }

    record NotInParty() implements PartyEvent {
    }

    record InviteSelf() implements PartyEvent {
    }

    record Other(String text) implements PartyEvent {
    }
}
