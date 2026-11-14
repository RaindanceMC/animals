package de.raindancer.modules.animals.breeding;

import java.util.Random;

/**
 * What a foal inherits, stat by stat. Vanilla averages its parents with a random third, so a foal hardly ever beats
 * them; here it lands between them, nearer the better one, and now and then a little past both — never past the
 * game's own range.
 */
public final class Inheritance {

    /** A stat's range in the game. */
    public record Range(double min, double max) {

        double clamp(double value) {
            return Math.max(min, Math.min(max, value));
        }
    }

    public static final Range SPEED = new Range(0.1125, 0.3375);
    public static final Range JUMP = new Range(0.4, 1.0);
    public static final Range HEALTH = new Range(15, 30);

    /** How far towards the better parent a foal lands, on average. */
    static final double LEAN = 0.6;
    /** How far either side of that it may land, as a share of the gap between the parents. */
    static final double SPREAD = 0.15;

    private final int mutationPercent;
    private final int mutationMostPercent;

    public Inheritance(int mutationPercent, int mutationMostPercent) {
        this.mutationPercent = Math.max(0, Math.min(100, mutationPercent));
        this.mutationMostPercent = Math.max(0, mutationMostPercent);
    }

    public double child(double parentA, double parentB, Range range, Random random) {
        double better = Math.max(parentA, parentB);
        double worse = Math.min(parentA, parentB);
        if (random.nextInt(100) < mutationPercent) {
            return range.clamp(better * (1 + random.nextDouble() * mutationMostPercent / 100.0));
        }
        double gap = better - worse;
        double lean = LEAN + (random.nextDouble() * 2 - 1) * SPREAD;
        return range.clamp(worse + gap * lean);
    }
}
