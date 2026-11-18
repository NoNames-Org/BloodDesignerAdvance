package dev.no.name.bdadvance.battlepass;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Баланс бладиков из боковой панели лобби bloodstone. Строку узнаём по ключу перевода
 * bloodstone.scoreboard.label.blood_coins, поэтому язык клиента не важен.
 */
public final class BloodBalance {
    private static final String BALANCE_KEY = "bloodstone.scoreboard.label.blood_coins";

    private BloodBalance() {
    }

    /** Баланс или -1, если панели с балансом сейчас нет (например, не в лобби). */
    public static int current() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return -1;
        }
        Scoreboard board = minecraft.level.getScoreboard();
        Objective sidebar = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return -1;
        }
        for (PlayerScoreEntry entry : board.listPlayerScores(sidebar)) {
            Component line = entry.display() != null ? entry.display()
                    : PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()), Component.literal(entry.owner()));
            if (MenuText.hasKey(line, BALANCE_KEY)) {
                return BattlePassText.lastNumber(line.getString());
            }
        }
        return -1;
    }
}
