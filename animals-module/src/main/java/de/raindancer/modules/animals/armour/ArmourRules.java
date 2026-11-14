package de.raindancer.modules.animals.armour;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Animal armour as the game's wolf armour works, and what each armour enchantment does on it — the arithmetic, apart
 * from Bukkit. The numbers follow the game's own for boots and chestplates.
 */
public final class ArmourRules {

    /** Horse armour lasts like the chestplate of its material. */
    private static final Map<Material, Integer> DURABILITY = Map.of(
            Material.LEATHER_HORSE_ARMOR, 80,
            Material.GOLDEN_HORSE_ARMOR, 112,
            Material.COPPER_HORSE_ARMOR, 176,
            Material.IRON_HORSE_ARMOR, 240,
            Material.DIAMOND_HORSE_ARMOR, 528,
            Material.NETHERITE_HORSE_ARMOR, 592);

    private static final Map<Material, Material> REPAIR = Map.of(
            Material.LEATHER_HORSE_ARMOR, Material.LEATHER,
            Material.GOLDEN_HORSE_ARMOR, Material.GOLD_INGOT,
            Material.COPPER_HORSE_ARMOR, Material.COPPER_INGOT,
            Material.IRON_HORSE_ARMOR, Material.IRON_INGOT,
            Material.DIAMOND_HORSE_ARMOR, Material.DIAMOND,
            Material.NETHERITE_HORSE_ARMOR, Material.NETHERITE_INGOT);

    /** What wolf armour lets through (the game's bypasses_wolf_armor), and what nothing stops. */
    private static final Set<DamageCause> THROUGH = EnumSet.of(DamageCause.CRAMMING, DamageCause.DROWNING,
            DamageCause.DRYOUT, DamageCause.FREEZE, DamageCause.SUFFOCATION, DamageCause.MAGIC, DamageCause.POISON,
            DamageCause.WORLD_BORDER, DamageCause.STARVATION, DamageCause.THORNS, DamageCause.WITHER,
            DamageCause.VOID, DamageCause.KILL, DamageCause.SUICIDE);

    private ArmourRules() {
    }

    public static Set<Material> horseArmours() {
        return DURABILITY.keySet();
    }

    /** How many points of wear it takes; 0 for what is no horse armour. */
    public static int durability(Material material) {
        return DURABILITY.getOrDefault(material, 0);
    }

    public static Material repairWith(Material material) {
        return REPAIR.get(material);
    }

    /** Whether the armour takes this hit instead of the animal. */
    public static boolean absorbs(DamageCause cause) {
        return cause != null && !THROUGH.contains(cause);
    }

    /** The chance a point of wear is really taken, with Unbreaking at {@code level} — the game's for armour. */
    public static double wearChance(int level) {
        return level <= 0 ? 1.0 : 0.6 + 0.4 / (level + 1);
    }

    /** Points of wear for a hit of {@code damage}: at least one, each spared by Unbreaking's chance. */
    public static int wear(double damage, int unbreaking, Random random) {
        int points = Math.max(1, (int) Math.ceil(damage - 1e-9));
        if (unbreaking <= 0) {
            return points;
        }
        double chance = wearChance(unbreaking);
        int worn = 0;
        for (int i = 0; i < points; i++) {
            if (random.nextDouble() < chance) {
                worn++;
            }
        }
        return worn;
    }

    /** Depth Strider: the share of land speed kept in water, a third per level up to all of it. */
    public static double waterEfficiency(int level) {
        return Math.min(1.0, Math.max(0, level) / 3.0);
    }

    /** Soul Speed: how much faster on soul sand and soil. */
    public static double soulSpeedBonus(int level) {
        return level <= 0 ? 0 : 0.3 + 0.1 * level;
    }

    /** Frost Walker: the radius of water frozen underfoot; 0 without it. */
    public static int frostRadius(int level) {
        return level <= 0 ? 0 : Math.min(16, 2 + level);
    }

    /** Durability given back for {@code xp} points of experience, never more than is worn. */
    public static int mended(int xp, int worn) {
        return Math.min(worn, Math.max(0, xp) * 2);
    }

    /** Experience used up mending {@code points} of durability. */
    public static int xpUsed(int points) {
        return (points + 1) / 2;
    }

    /** Respiration: extra air, a point per level — the game's oxygen bonus, as on a helmet. */
    public static double oxygenBonus(int level) {
        return Math.max(0, level);
    }
}
