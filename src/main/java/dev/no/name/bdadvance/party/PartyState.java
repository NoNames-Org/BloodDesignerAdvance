package dev.no.name.bdadvance.party;

import java.util.List;

public final class PartyState {
    static final long INVITE_TTL_MS = 60_000;

    private String leader;
    private List<String> members = List.of();
    private String inviteFrom;
    private String inviteCommand;
    private long inviteAt;
    private String status = "";

    public void apply(PartyEvent event, long now) {
        switch (event) {
            case PartyEvent.Info info -> {
                leader = info.leader();
                members = info.members();
            }
            case PartyEvent.Invited invited -> status = invited.name() + " приглашён в пати";
            case PartyEvent.IncomingInvite incoming -> {
                inviteFrom = incoming.from();
                inviteCommand = null;
                inviteAt = now;
                status = incoming.from() + " зовёт тебя в пати";
            }
            case PartyEvent.Left left -> {
                clearParty();
                status = "Ты вышел из пати";
            }
            case PartyEvent.Disbanded disbanded -> {
                clearParty();
                status = "Пати распущена";
            }
            case PartyEvent.NotInParty notInParty -> {
                clearParty();
                status = "Ты не в пати";
            }
            case PartyEvent.InviteSelf inviteSelf -> status = "Нельзя пригласить себя";
            case PartyEvent.Other other -> status = other.text();
        }
    }

    public void setStatus(String text) {
        status = text;
    }

    public void setInviteCommand(String command) {
        inviteCommand = command;
    }

    public String inviteCommand() {
        return inviteCommand;
    }

    public void clearInvite() {
        inviteFrom = null;
        inviteCommand = null;
    }

    public void reset() {
        clearParty();
        clearInvite();
        status = "";
    }

    public boolean inParty() {
        return leader != null;
    }

    public String leader() {
        return leader;
    }

    public List<String> members() {
        return members;
    }

    public boolean isLeader(String name) {
        return leader != null && leader.equalsIgnoreCase(name);
    }

    public String incomingInvite(long now) {
        return inviteFrom != null && now - inviteAt <= INVITE_TTL_MS ? inviteFrom : null;
    }

    public String status() {
        return status;
    }

    private void clearParty() {
        leader = null;
        members = List.of();
    }
}
