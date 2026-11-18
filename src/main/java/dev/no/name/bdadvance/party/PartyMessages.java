package dev.no.name.bdadvance.party;

import dev.no.name.bdadvance.ServerText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyMessages {
    private static final String NAME = "([A-Za-z0-9_]{3,16})";
    // Сервер шлёт тексты на языке клиента, поэтому каждый шаблон знает английский и русский вариант.
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern LEADER = Pattern.compile("^(?:Leader|Лидер): " + NAME + "$", FLAGS);
    private static final Pattern MEMBERS = Pattern.compile("^(?:Members|Участники): \\[(.*)]$", FLAGS);
    private static final Pattern INVITED = Pattern.compile(
            "^" + NAME + " (?:has been invited to the party|был[аи]? приглаш[её]н[аы]? в тусовку)[!.]?$", FLAGS);
    private static final Pattern INCOMING = Pattern.compile(
            "^" + NAME + " (?:invited you to their party|пригласил[аи]? (?:вас|тебя) в (?:сво[юё] )?тусовку)[!.]?$", FLAGS);
    private static final Pattern LEFT = Pattern.compile("^(?:you left from the party|вы покинули тусовку)", FLAGS);
    private static final Pattern DISBANDED = Pattern.compile(
            "^(?:the party was disbanded|тусовка (?:была )?распущена|вы распустили тусовку)", FLAGS);
    private static final Pattern INVITE_SELF = Pattern.compile(
            "^(?:you can't invite yourself|вы не можете пригласить себя)", FLAGS);
    private static final Pattern NOT_IN_PARTY = Pattern.compile(
            "you are not (at|in) a party|вы не (состоите )?в (тусовке|пати|группе)",
            FLAGS);
    private static final Pattern PARTY_WORD = Pattern.compile("\\bpart(y|ies)\\b|тусовк", FLAGS);


    private static final String ACCEPT_PREFIX = "party accept";

    private PartyMessages() {
    }

    public static String acceptCommand(List<String> clickCommands) {
        for (String command : clickCommands) {
            String stripped = command.strip();
            if (stripped.startsWith("/")) {
                stripped = stripped.substring(1);
            }
            if (stripped.toLowerCase(Locale.ROOT).startsWith(ACCEPT_PREFIX)) {
                return stripped;
            }
        }
        return null;
    }

    public static PartyEvent parse(String plain) {
        if (isNotInParty(plain)) {
            return new PartyEvent.NotInParty();
        }
        if (!ServerText.isBlock(plain)) {
            return null;
        }
        List<String> lines = ServerText.lines(plain);
        if (lines.isEmpty()) {
            return null;
        }
        PartyEvent info = parseInfo(lines);
        if (info != null) {
            return info;
        }
        String first = lines.get(0);
        Matcher matcher = INVITED.matcher(first);
        if (matcher.matches()) {
            return new PartyEvent.Invited(matcher.group(1));
        }
        matcher = INCOMING.matcher(first);
        if (matcher.matches()) {
            return new PartyEvent.IncomingInvite(matcher.group(1));
        }
        if (LEFT.matcher(first).find()) {
            return new PartyEvent.Left();
        }
        if (DISBANDED.matcher(first).find()) {
            return new PartyEvent.Disbanded();
        }
        if (INVITE_SELF.matcher(first).find()) {
            return new PartyEvent.InviteSelf();
        }
        String text = String.join(" ", lines);
        return PARTY_WORD.matcher(text).find() ? new PartyEvent.Other(text) : null;
    }

    static boolean isNotInParty(String plain) {
        List<String> lines = ServerText.lines(plain);
        return lines.size() == 1 && NOT_IN_PARTY.matcher(lines.get(0)).find();
    }

    private static PartyEvent parseInfo(List<String> lines) {
        if (lines.size() < 2) {
            return null;
        }
        Matcher leader = LEADER.matcher(lines.get(0));
        Matcher members = MEMBERS.matcher(lines.get(1));
        if (!leader.matches() || !members.matches()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (String name : members.group(1).split(",")) {
            if (!name.isBlank()) {
                names.add(name.strip());
            }
        }
        return new PartyEvent.Info(leader.group(1), names);
    }
}
