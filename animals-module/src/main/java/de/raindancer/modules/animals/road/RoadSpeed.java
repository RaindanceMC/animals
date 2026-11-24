package de.raindancer.modules.animals.road;

import de.raindancer.core.world.protection.ProtectedArea;
import de.raindancer.modules.animals.AnimalsSettings;
import de.raindancer.modules.animals.stats.HorseStats;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The arithmetic of road speed, apart from any entity. */
public final class RoadSpeed {

    /** A dirt path is a sixteenth lower than a full block; looking this far under the hooves finds either. */
    private static final double UNDER_HOOVES = 0.0625;

    private RoadSpeed() {
    }

    public static Set<Material> roads(String list) {
        return roads(list, material -> material.isBlock() && !material.isAir());
    }

    /** {@code isBlock} is the world's answer, passed in: asking the material needs a running server. */
    static Set<Material> roads(String list, java.util.function.Predicate<Material> isBlock) {
        Set<Material> roads = EnumSet.noneOf(Material.class);
        if (list == null) {
            return roads;
        }
        for (String name : list.split("[\\s,;]+")) {
            String plain = name.strip().toUpperCase(Locale.ROOT).replaceFirst("^MINECRAFT:", "");
            if (plain.isEmpty()) {
                continue;
            }
            Material material = Material.matchMaterial(plain);
            if (material != null && isBlock.test(material)) {
                roads.add(material);
            }
        }
        return roads;
    }

    public static boolean isOff(int roadPercent, int offRoadPercent) {
        return roadPercent == 100 && offRoadPercent == 100;
    }

    /** The factor on the mount's own speed while it stands on {@code under}. */
    public static double multiplier(Material under, Set<Material> roads, int roadPercent, int offRoadPercent) {
        return (under != null && roads.contains(under) ? roadPercent : offRoadPercent) / 100.0;
    }

    /** The best movement speed a horse can be born or bred with. */
    static final double BEST_HORSE = 0.3375;

    /** The factor that takes the best horse to {@code topBps} blocks a second; 0 for highways switched off. */
    public static double highwayFactor(int topBps) {
        return topBps <= 0 ? 0.0 : topBps / HorseStats.blocksPerSecond(BEST_HORSE);
    }

    /**
     * Whether this ground is a highway: held by the server (nobody owns it) and named with the prefix. Asked of
     * whatever protects land through Core, so it is the claims plugin's server claims today without this module
     * knowing it.
     */
    public static boolean isHighway(Optional<ProtectedArea> area, String prefix) {
        if (area.isEmpty() || prefix == null || prefix.isBlank()) {
            return false;
        }
        ProtectedArea ground = area.get();
        String name = ground.name();
        return ground.owners().isEmpty() && name != null
                && name.toLowerCase(Locale.ROOT).startsWith(prefix.strip().toLowerCase(Locale.ROOT));
    }

    /** The factor on the mount's own speed here: a highway's first, then the road block under it, then off-road. */
    public static double factor(Material under, Optional<ProtectedArea> area, Set<Material> roads, AnimalsSettings settings) {
        if (settings.highwayTopBps() > 0 && isHighway(area, settings.highwayPrefix())) {
            return highwayFactor(settings.highwayTopBps());
        }
        if (isOff(settings.roadPercent(), settings.offRoadPercent())) {
            return 1.0;
        }
        return multiplier(under, roads, settings.roadPercent(), settings.offRoadPercent());
    }

    /** The block the hooves stand on, for feet at height {@code y}. */
    public static int standingBlockY(double y) {
        return (int) Math.floor(y - UNDER_HOOVES);
    }
}
