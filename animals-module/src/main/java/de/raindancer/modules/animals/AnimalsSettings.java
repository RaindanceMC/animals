package de.raindancer.modules.animals;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/** What an owner decides about horses and animal armour. */
@Settings(id = "animals", topics = {
        @Topic(path = "animals/roads", title = "Roads", icon = Material.DIRT_PATH),
        @Topic(path = "animals/breeding", title = "Taming & breeding", icon = Material.GOLDEN_CARROT),
        @Topic(path = "animals/whistle", title = "Whistle", icon = Material.IRON_NUGGET),
        @Topic(path = "animals/stats", title = "Stats", icon = Material.SADDLE),
})
public record AnimalsSettings(

        @In("animals/roads") @Title("Speed on a road (percent)") @Range(min = 10, max = 400)
        @Describe("A ridden horse, donkey, mule or camel standing on one of the road blocks below goes this much of its "
                + "own speed, so a faster horse stays faster. 100 is vanilla.")
        @Key("road-percent")
        int roadPercent,

        @In("animals/roads") @Title("Speed everywhere else (percent)") @Range(min = 10, max = 400)
        @Describe("The same off the road. 100 is vanilla.")
        @Key("off-road-percent")
        int offRoadPercent,

        @In("animals/roads") @Title("Road blocks")
        @Describe("Block names, comma separated, that count as road — what the hooves stand on. Dirt path is what a "
                + "shovel makes of grass.")
        @Key("road-blocks")
        String roadBlocks,

        @In("animals/breeding") @Title("Health once tamed (percent)") @Range(min = 100, max = 1000)
        @Describe("A tamed horse, donkey or mule has this much of the health it had wild, as a wolf grows stronger when "
                + "it becomes a dog (a wolf gets 500). 100 is vanilla.")
        @Key("tamed-health-percent")
        int tamedHealthPercent,

        @In("animals/breeding") @Title("Better breeding")
        @Describe("A foal lands between its parents, nearer the better one, stat by stat, instead of vanilla's average "
                + "with a random third that hardly ever beats them. Off: vanilla.")
        @Key("breeding.enabled")
        boolean breeding,

        @In("animals/breeding") @Title("Chance to beat both parents (percent)") @Range(min = 0, max = 100)
        @Describe("How often a stat comes out better than the better parent's. Never past the game's own maximum.")
        @Key("breeding.better-chance")
        int betterChance,

        @In("animals/breeding") @Title("By at most (percent)") @Range(min = 0, max = 50)
        @Describe("How much better than the better parent such a stat may be.")
        @Key("breeding.better-most")
        int betterMost,

        @In("animals/whistle") @Title("Horse whistle")
        @Describe("A whistle, crafted from three iron nuggets and a string, calls the last horse its owner rode to them. "
                + "Off: it does nothing (it can still be crafted).")
        @Key("whistle.enabled")
        boolean whistle,

        @In("animals/whistle") @Title("Reach (blocks)") @Range(min = 0, max = 10_000)
        @Describe("How far away the horse may be to come, in the same world. 0: any distance.")
        @Key("whistle.reach")
        int whistleReach,

        @In("animals/whistle") @Title("Cooldown (seconds)") @Range(min = 0, max = 3600)
        @Describe("How long a whistle rests after it called a horse.")
        @Key("whistle.cooldown-seconds")
        int whistleCooldown,

        @In("animals/stats") @Title("Horse stats above the hotbar")
        @Describe("Riding a horse, or looking at one close by, shows its speed, jump and health. Every player can switch "
                + "it off for themselves with /horses stats.")
        @Key("stats.enabled")
        boolean stats) {

    public static final AnimalsSettings DEFAULTS = new AnimalsSettings(160, 110, "dirt_path, gravel",
            200, true, 10, 5, true, 256, 30, true);
}
