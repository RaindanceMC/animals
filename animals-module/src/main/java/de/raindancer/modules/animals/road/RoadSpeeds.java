package de.raindancer.modules.animals.road;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.animals.AnimalsSettings;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Twice a second the horse under every rider gets a speed modifier for the block it stands on. A ridden horse moves on
 * its rider's client, which reads the speed attribute, so the modifier is what the client goes by. Transient — never
 * saved with the horse — and taken off when the rider gets off.
 */
public final class RoadSpeeds implements Listener {

    static final NamespacedKey KEY = new NamespacedKey("rainsanimals", "road_speed");

    private final Plugin plugin;
    private final Supplier<AnimalsSettings> settings;
    private volatile String roadsRead;
    private volatile Set<Material> roads = Set.of();

    public RoadSpeeds(Plugin plugin, Supplier<AnimalsSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public ScheduledTask start() {
        return Scheduling.globalTimer(plugin, 10, 10, task -> {
            AnimalsSettings now = settings.get();
            if (!now.roadBlocks().equals(roadsRead)) {
                roads = RoadSpeed.roads(now.roadBlocks());
                roadsRead = now.roadBlocks();
            }
            for (Player rider : plugin.getServer().getOnlinePlayers()) {
                if (rider.getVehicle() instanceof AbstractHorse) {
                    Scheduling.entity(plugin, rider, () -> apply(rider, now));
                }
            }
        });
    }

    /** The factor the rider's horse goes at right now, for the stats line; 1 when on foot. */
    public double factorFor(AbstractHorse horse) {
        AnimalsSettings now = settings.get();
        if (RoadSpeed.isOff(now.roadPercent(), now.offRoadPercent())) {
            return 1.0;
        }
        Location at = horse.getLocation();
        Material under = horse.getWorld().getBlockAt(at.getBlockX(), RoadSpeed.standingBlockY(at.getY()),
                at.getBlockZ()).getType();
        return RoadSpeed.multiplier(under, roads, now.roadPercent(), now.offRoadPercent());
    }

    private void apply(Player rider, AnimalsSettings now) {
        Entity vehicle = rider.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse) || horse instanceof Llama) {
            return;
        }
        set(horse, factorFor(horse));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player && event.getDismounted() instanceof AbstractHorse horse) {
            set(horse, 1.0);
        }
    }

    static void set(AbstractHorse horse, double factor) {
        AttributeInstance speed = horse.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        double amount = factor - 1.0;
        AttributeModifier current = speed.getModifier(KEY);
        if (current != null && Math.abs(current.getAmount() - amount) < 1e-9) {
            return;
        }
        if (current != null) {
            speed.removeModifier(KEY);
        }
        if (Math.abs(amount) > 1e-9) {
            speed.addTransientModifier(new AttributeModifier(KEY, amount, AttributeModifier.Operation.ADD_SCALAR));
        }
    }
}
