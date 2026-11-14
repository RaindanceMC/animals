package de.raindancer.modules.animals.stats;

import de.raindancer.core.RainsCore;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.actionbar.ActionBarPriority;
import de.raindancer.modules.animals.AnimalsSettings;
import de.raindancer.modules.animals.road.RoadSpeeds;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * The horse a player rides, or looks at close by, with its stats above the hotbar — twice a second, through Core's
 * action bars so it gives way to anything more pressing. Each player can switch it off; that is kept on the player.
 */
public final class StatsDisplay {

    static final NamespacedKey HIDDEN = new NamespacedKey("rainsanimals", "stats_hidden");
    private static final String OWNER = "animals";
    private static final int LOOK_REACH = 6;

    private final Plugin plugin;
    private final RainsCore core;
    private final Supplier<AnimalsSettings> settings;
    private final RoadSpeeds roads;

    public StatsDisplay(Plugin plugin, RainsCore core, Supplier<AnimalsSettings> settings, RoadSpeeds roads) {
        this.plugin = plugin;
        this.core = core;
        this.settings = settings;
        this.roads = roads;
    }

    public ScheduledTask start() {
        return Scheduling.globalTimer(plugin, 10, 10, task -> {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                Scheduling.entity(plugin, online, () -> show(online));
            }
        });
    }

    public static boolean hidden(Player player) {
        return player.getPersistentDataContainer().has(HIDDEN, PersistentDataType.BYTE);
    }

    /** Switches it for the player. @return whether it is on now */
    public static boolean toggle(Player player) {
        if (hidden(player)) {
            player.getPersistentDataContainer().remove(HIDDEN);
            return true;
        }
        player.getPersistentDataContainer().set(HIDDEN, PersistentDataType.BYTE, (byte) 1);
        return false;
    }

    private void show(Player player) {
        AbstractHorse horse = settings.get().stats() && !hidden(player) ? horseOf(player) : null;
        if (horse == null) {
            core.actionBars().clear(player.getUniqueId(), OWNER);
            return;
        }
        double here = player.getVehicle() == horse ? roads.factorFor(horse) : 1.0;
        double speed = base(horse, Attribute.MOVEMENT_SPEED);
        Component here_ = Math.abs(here - 1.0) > 1e-6
                ? core.messages().get("animals.stats.here", "value", HorseStats.speedText(speed * here))
                : Component.empty();
        Component line = core.messages().get("animals.stats.line", "speed", HorseStats.speedText(speed),
                "here", here_, "jump", HorseStats.jumpText(base(horse, Attribute.JUMP_STRENGTH)),
                "hearts", HorseStats.heartsText(value(horse, Attribute.MAX_HEALTH)));
        core.actionBars().show(player.getUniqueId(), OWNER, line, Duration.ofSeconds(1), ActionBarPriority.LOW);
    }

    private static AbstractHorse horseOf(Player player) {
        Entity riding = player.getVehicle();
        if (riding instanceof AbstractHorse horse && !(riding instanceof Llama)) {
            return horse;
        }
        Entity looked = player.getTargetEntity(LOOK_REACH);
        return looked instanceof AbstractHorse horse && !(looked instanceof Llama) ? horse : null;
    }

    /** With modifiers: health counts the tamed bonus, which is what the horse really has. */
    private static double value(AbstractHorse horse, Attribute attribute) {
        AttributeInstance instance = horse.getAttribute(attribute);
        return instance == null ? 0 : instance.getValue();
    }

    private static double base(AbstractHorse horse, Attribute attribute) {
        AttributeInstance instance = horse.getAttribute(attribute);
        return instance == null ? 0 : instance.getBaseValue();
    }
}
