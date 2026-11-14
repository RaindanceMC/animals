package de.raindancer.modules.animals.road;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
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

    /** The block the hooves stand on, for feet at height {@code y}. */
    public static int standingBlockY(double y) {
        return (int) Math.floor(y - UNDER_HOOVES);
    }
}
