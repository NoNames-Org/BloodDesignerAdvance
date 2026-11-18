package dev.no.name.bdadvance.modes.gui;

import dev.no.name.bdadvance.BdAdvance;
import dev.no.name.bdadvance.modes.ModeRoute;
import dev.no.name.bdadvance.modes.ModeRoute.Submenu;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum GameMode {
    LOBBY("lobby", "Лобби", 0xFF4F6275, null, null, "hub хаб лобби lobby", ModeRoute.command("hub")),
    FFA_SMP("ffa_smp", "FFA SMP", 0xFFB4532A, null, null, "ффа смп", ModeRoute.via(2, Submenu.FFA, 20)),
    FFA_KB("ffa_kb", "FFA КБ", 0xFF2F7A68, null, null, "ффа kb королевская битва", ModeRoute.via(2, Submenu.FFA, 24)),
    CUSTOM_BATTLE_ROYALE("custom_br", "Кастомная королевская битва", 0xFF6B3FB5, "СКОРО", null, "кастом кб custom br", null),
    BATTLE_ROYALE("battle_royale", "Королевская битва", 0xFFB03535, null, null, "кб br", ModeRoute.direct(66)),
    SMP("smp", "СМП", 0xFF3F7D3A, null, null, "smp выживание", ModeRoute.direct(16)),
    MEETUPS("meetups", "Миттапы", 0xFFB08A2E, null, null, "meetup митап", null),
    MEETUP_SMP("meetup_smp", "СМП", 0xFF3F7D3A, null, "meetups", "smp миттап смп", ModeRoute.via(19, Submenu.MEETUPS, 20)),
    MEETUP_UHC("meetup_uhc", "УХК", 0xFFC9662C, null, "meetups", "uhc ухц миттап", ModeRoute.via(19, Submenu.MEETUPS, 24)),
    PRACTICE("practice", "PRACTICE", 0xFF2F5FA8, null, null, "практис тренировка", ModeRoute.via(25, Submenu.PRACTICE, -1)),
    MACE_ROULETTE("mace", "Мейс рулетка", 0xFF5C6370, null, null, "mace булава рулетка", ModeRoute.direct(46));

    public static final int ART_SIZE = 40;

    private final String id;
    private final String title;
    private final int color;
    private final String badge;
    private final String parent;
    private final String aliases;
    private final Identifier art;
    private final ModeRoute route;

    GameMode(String id, String title, int color, String badge, String parent, String aliases, ModeRoute route) {
        this.id = id;
        this.title = title;
        this.color = color;
        this.badge = badge;
        this.parent = parent;
        this.aliases = aliases;
        this.route = route;
        this.art = Identifier.fromNamespaceAndPath(BdAdvance.MOD_ID, "textures/gui/modes/" + id + ".png");
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public int color() {
        return color;
    }

    public String badge() {
        return badge;
    }

    public Identifier art() {
        return art;
    }

    public ModeRoute route() {
        return route;
    }

    public static List<GameMode> topLevel() {
        List<GameMode> result = new ArrayList<>();
        for (GameMode mode : values()) {
            if (mode.parent == null) {
                result.add(mode);
            }
        }
        return result;
    }

    public List<GameMode> children() {
        List<GameMode> result = new ArrayList<>();
        for (GameMode mode : values()) {
            if (id.equals(mode.parent)) {
                result.add(mode);
            }
        }
        return result;
    }

    public boolean isGroup() {
        return !children().isEmpty();
    }

    public boolean matches(String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }
        return title.toLowerCase(Locale.ROOT).contains(needle) || id.contains(needle)
                || aliases.toLowerCase(Locale.ROOT).contains(needle);
    }
}
