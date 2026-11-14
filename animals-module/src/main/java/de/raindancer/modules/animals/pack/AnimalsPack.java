package de.raindancer.modules.animals.pack;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.LogChannel;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** The horse and dog armour trims and the whistle (its look and its sounds), offered to Core's resource pack. */
public final class AnimalsPack {

    public static final String ROOT = "/de/raindancer/modules/animals/pack/";
    public static final List<String> FILES = List.of(
            "assets/minecraft/textures/trims/entity/horse_body/bolt.png",
            "assets/minecraft/textures/trims/entity/horse_body/bolt.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/coast.png",
            "assets/minecraft/textures/trims/entity/horse_body/coast.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/dune.png",
            "assets/minecraft/textures/trims/entity/horse_body/dune.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/eye.png",
            "assets/minecraft/textures/trims/entity/horse_body/eye.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/flow.png",
            "assets/minecraft/textures/trims/entity/horse_body/flow.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/host.png",
            "assets/minecraft/textures/trims/entity/horse_body/host.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/raiser.png",
            "assets/minecraft/textures/trims/entity/horse_body/raiser.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/rib.png",
            "assets/minecraft/textures/trims/entity/horse_body/rib.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/sentry.png",
            "assets/minecraft/textures/trims/entity/horse_body/sentry.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/shaper.png",
            "assets/minecraft/textures/trims/entity/horse_body/shaper.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/silence.png",
            "assets/minecraft/textures/trims/entity/horse_body/silence.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/snout.png",
            "assets/minecraft/textures/trims/entity/horse_body/snout.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/spire.png",
            "assets/minecraft/textures/trims/entity/horse_body/spire.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/tide.png",
            "assets/minecraft/textures/trims/entity/horse_body/tide.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/vex.png",
            "assets/minecraft/textures/trims/entity/horse_body/vex.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/ward.png",
            "assets/minecraft/textures/trims/entity/horse_body/ward.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/wayfinder.png",
            "assets/minecraft/textures/trims/entity/horse_body/wayfinder.png.mcmeta",
            "assets/minecraft/textures/trims/entity/horse_body/wild.png",
            "assets/minecraft/textures/trims/entity/horse_body/wild.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/bolt.png",
            "assets/minecraft/textures/trims/entity/wolf_body/bolt.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/coast.png",
            "assets/minecraft/textures/trims/entity/wolf_body/coast.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/dune.png",
            "assets/minecraft/textures/trims/entity/wolf_body/dune.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/eye.png",
            "assets/minecraft/textures/trims/entity/wolf_body/eye.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/flow.png",
            "assets/minecraft/textures/trims/entity/wolf_body/flow.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/host.png",
            "assets/minecraft/textures/trims/entity/wolf_body/host.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/raiser.png",
            "assets/minecraft/textures/trims/entity/wolf_body/raiser.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/rib.png",
            "assets/minecraft/textures/trims/entity/wolf_body/rib.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/sentry.png",
            "assets/minecraft/textures/trims/entity/wolf_body/sentry.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/shaper.png",
            "assets/minecraft/textures/trims/entity/wolf_body/shaper.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/silence.png",
            "assets/minecraft/textures/trims/entity/wolf_body/silence.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/snout.png",
            "assets/minecraft/textures/trims/entity/wolf_body/snout.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/spire.png",
            "assets/minecraft/textures/trims/entity/wolf_body/spire.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/tide.png",
            "assets/minecraft/textures/trims/entity/wolf_body/tide.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/vex.png",
            "assets/minecraft/textures/trims/entity/wolf_body/vex.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/ward.png",
            "assets/minecraft/textures/trims/entity/wolf_body/ward.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/wayfinder.png",
            "assets/minecraft/textures/trims/entity/wolf_body/wayfinder.png.mcmeta",
            "assets/minecraft/textures/trims/entity/wolf_body/wild.png",
            "assets/minecraft/textures/trims/entity/wolf_body/wild.png.mcmeta",
            "assets/rainsanimals/items/whistle.json",
            "assets/rainsanimals/models/item/whistle.json",
            "assets/rainsanimals/sounds.json",
            "assets/rainsanimals/sounds/whistle1.ogg",
            "assets/rainsanimals/sounds/whistle2.ogg",
            "assets/rainsanimals/sounds/whistle3.ogg",
            "assets/rainsanimals/sounds/whistle4.ogg",
            "assets/rainsanimals/textures/item/whistle.png",
            "CREDITS.txt",
            "licenses/horse-whistle-Apache-2.0.txt",
            "licenses/whistlebound-horse-MIT.txt",
            "pack.mcmeta");
    private static final String OWNER = "Animals";
    private static final String NAME = "trims-and-whistle";

    private AnimalsPack() {
    }

    public static void contribute(Plugin plugin, RainsCore core, LogChannel log) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), AnimalsPack.class, ROOT, FILES, OWNER, NAME,
                    "Trims on horse and dog armour and the horse whistle");
        } catch (IOException failure) {
            log.warn("The animals pack could not be written ({}); trims still go on, but horse armour looks plain "
                    + "and the whistle looks like an iron nugget.", failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(Locale.ROOT) + ":" + NAME);
    }
}
