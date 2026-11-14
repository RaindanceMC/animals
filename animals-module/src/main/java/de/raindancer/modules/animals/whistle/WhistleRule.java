package de.raindancer.modules.animals.whistle;

import java.util.UUID;

/** Whether a whistle brings the horse, decided apart from the world. */
public final class WhistleRule {

    public enum Outcome { CALL, NO_HORSE, NOT_YOURS, OTHER_WORLD, TOO_FAR, RIDDEN }

    private WhistleRule() {
    }

    /**
     * @param distance blocks between the caller and where the horse was last known
     * @param reach    the most blocks it comes from; 0 for any distance in the same world
     */
    public static Outcome decide(UUID caller, boolean hasHorse, UUID owner, boolean sameWorld, boolean riddenByOther,
                                 double distance, int reach) {
        if (!hasHorse) {
            return Outcome.NO_HORSE;
        }
        if (!owns(caller, owner)) {
            return Outcome.NOT_YOURS;
        }
        if (!sameWorld) {
            return Outcome.OTHER_WORLD;
        }
        if (riddenByOther) {
            return Outcome.RIDDEN;
        }
        if (reach > 0 && distance > reach) {
            return Outcome.TOO_FAR;
        }
        return Outcome.CALL;
    }

    public static boolean owns(UUID caller, UUID owner) {
        return caller != null && caller.equals(owner);
    }
}
