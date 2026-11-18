package dev.no.name.bdadvance.battlepass;

/** Уровень и опыт обычной и Premium-линии. Premium считается активным, если ему нужен опыт. */
public record BattlePassProgress(int level, int maxLevel, int xp, int xpNeeded,
                                 int premiumLevel, int premiumMax, int premiumXp, int premiumXpNeeded) {
    public static final BattlePassProgress EMPTY = new BattlePassProgress(0, 0, 0, 0, 0, 0, 0, 0);

    public boolean premiumActive() {
        return premiumXpNeeded > 0 || premiumLevel > 0;
    }

    /** Заполнение полосы опыта 0..1. */
    public float fill() {
        return xpNeeded <= 0 ? 0 : Math.max(0, Math.min(1, xp / (float) xpNeeded));
    }

    public float premiumFill() {
        return premiumXpNeeded <= 0 ? 0 : Math.max(0, Math.min(1, premiumXp / (float) premiumXpNeeded));
    }
}
