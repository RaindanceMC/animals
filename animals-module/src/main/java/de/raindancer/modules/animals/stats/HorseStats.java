package de.raindancer.modules.animals.stats;

import java.util.Locale;

/** A horse's stats in the words a player reads. */
public final class HorseStats {

    /** A horse's movement-speed attribute times this is about its blocks a second, ridden on flat ground. */
    private static final double BLOCKS_PER_SECOND_PER_POINT = 43.17;

    private HorseStats() {
    }

    public static double blocksPerSecond(double movementSpeed) {
        return movementSpeed * BLOCKS_PER_SECOND_PER_POINT;
    }

    /** How high a horse jumps, in blocks, for its jump strength — the curve the game's physics give. */
    public static double jumpBlocks(double strength) {
        double s = strength;
        return -0.1817584952 * s * s * s + 3.689713992 * s * s + 2.128599134 * s - 0.343930367;
    }

    /** The line as plain text: speed (and what it is here when that differs), jump, hearts. */
    public static String plainLine(double movementSpeed, double jumpStrength, double maxHealth, double factorHere) {
        StringBuilder line = new StringBuilder("Speed ").append(one(blocksPerSecond(movementSpeed))).append(" b/s");
        if (Math.abs(factorHere - 1.0) > 1e-6) {
            line.append(" (").append(one(blocksPerSecond(movementSpeed) * factorHere)).append(" here)");
        }
        line.append(" · Jump ").append(one(jumpBlocks(jumpStrength))).append(" blocks");
        line.append(" · Health ").append(hearts(maxHealth)).append(" ❤");
        return line.toString();
    }

    /** Blocks a second, one decimal. */
    public static String speedText(double movementSpeed) {
        return one(blocksPerSecond(movementSpeed));
    }

    public static String jumpText(double strength) {
        return one(jumpBlocks(strength));
    }

    public static String heartsText(double maxHealth) {
        return hearts(maxHealth);
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String hearts(double health) {
        double hearts = Math.round(health) / 2.0;
        return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.format(Locale.ROOT, "%.1f", hearts);
    }
}
