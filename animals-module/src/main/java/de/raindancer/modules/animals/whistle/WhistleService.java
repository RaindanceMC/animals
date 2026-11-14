package de.raindancer.modules.animals.whistle;

import de.raindancer.core.content.items.CustomItem;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.animals.AnimalsSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The horse whistle: remembers on each player the last of their own horses they rode and where they got off, and calls
 * it to them. A horse whose chunk is not loaded is fetched by loading its neighbourhood for a moment.
 */
public final class WhistleService implements Listener {

    public static final String PLUGIN = "animals";
    public static final String ID = "whistle";
    static final NamespacedKey LAST = new NamespacedKey("rainsanimals", "last_horse");
    /** How long a horse whose chunk had to be loaded is waited for, in ticks. */
    private static final int WAIT_TICKS = 60;

    private final Plugin plugin;
    private final Server server;
    private final Messages messages;
    private final Supplier<AnimalsSettings> settings;

    public WhistleService(Plugin plugin, Messages messages, Supplier<AnimalsSettings> settings) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.messages = messages;
        this.settings = settings;
    }

    public static CustomItem definition() {
        return CustomItem.builder(PLUGIN, ID)
                .material(Material.IRON_NUGGET)
                .name("<gold>Horse Whistle")
                .lore(List.of(
                        "<gray>Calls the last of your horses",
                        "<gray>you rode to you.",
                        "",
                        "<white>Right-click <gray>to whistle."))
                .itemModel("rainsanimals:whistle")
                .ability(ID)
                .recipe(List.of("IRON_NUGGET IRON_NUGGET IRON_NUGGET", "- STRING -"))
                .notAnIngredient()
                .build();
    }

    /** The last horse and where it was left: horse, world, block x, block z. */
    record Last(UUID horse, UUID world, int x, int z) {

        String write() {
            return horse + ";" + world + ";" + x + ";" + z;
        }

        static Optional<Last> read(String text) {
            if (text == null) {
                return Optional.empty();
            }
            String[] parts = text.split(";");
            try {
                return Optional.of(new Last(UUID.fromString(parts[0]), UUID.fromString(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
            } catch (RuntimeException broken) {
                return Optional.empty();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player player && event.getMount() instanceof AbstractHorse horse
                && !(horse instanceof Llama) && WhistleRule.owns(player.getUniqueId(), horse.getOwnerUniqueId())) {
            remember(player, horse);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && event.getDismounted() instanceof AbstractHorse horse
                && !(horse instanceof Llama) && WhistleRule.owns(player.getUniqueId(), horse.getOwnerUniqueId())) {
            remember(player, horse);
        }
    }

    private static void remember(Player player, AbstractHorse horse) {
        Location at = horse.getLocation();
        player.getPersistentDataContainer().set(LAST, PersistentDataType.STRING,
                new Last(horse.getUniqueId(), at.getWorld().getUID(), at.getBlockX(), at.getBlockZ()).write());
    }

    /** Whistles for the player's horse. @return whether it was called — the whistle then rests */
    public boolean call(Player player) {
        AnimalsSettings now = settings.get();
        if (!now.whistle()) {
            messages.send(player, "animals.whistle.disabled");
            return false;
        }
        Optional<Last> last = Last.read(player.getPersistentDataContainer().get(LAST, PersistentDataType.STRING));
        Entity loaded = last.map(l -> server.getEntity(l.horse())).orElse(null);
        WhistleRule.Outcome outcome;
        if (loaded instanceof AbstractHorse horse) {
            outcome = WhistleRule.decide(player.getUniqueId(), true, horse.getOwnerUniqueId(),
                    horse.getWorld().equals(player.getWorld()),
                    !horse.getPassengers().isEmpty() && !horse.getPassengers().contains(player),
                    horse.getWorld().equals(player.getWorld()) ? horse.getLocation().distance(player.getLocation()) : 0,
                    now.whistleReach());
        } else {
            outcome = last.map(l -> WhistleRule.decide(player.getUniqueId(), true, player.getUniqueId(),
                            l.world().equals(player.getWorld().getUID()), false,
                            Math.hypot(l.x() - player.getLocation().getX(), l.z() - player.getLocation().getZ()),
                            now.whistleReach()))
                    .orElse(WhistleRule.Outcome.NO_HORSE);
        }
        if (outcome != WhistleRule.Outcome.CALL) {
            messages.send(player, switch (outcome) {
                case NO_HORSE -> "animals.whistle.no-horse";
                case NOT_YOURS -> "animals.whistle.not-yours";
                case OTHER_WORLD -> "animals.whistle.other-world";
                case RIDDEN -> "animals.whistle.ridden";
                default -> "animals.whistle.too-far";
            }, "reach", String.valueOf(now.whistleReach()));
            return false;
        }
        player.getWorld().playSound(player.getLocation(), "rainsanimals:whistle", SoundCategory.PLAYERS, 2f, 1f);
        messages.send(player, "animals.whistle.coming");
        if (loaded instanceof AbstractHorse horse) {
            bring(player, horse);
        } else {
            fetch(player, last.get());
        }
        return true;
    }

    /** Loads the horse's neighbourhood for a moment, waits for it, brings it and lets the chunks go again. */
    private void fetch(Player player, Last last) {
        World world = player.getWorld();
        Location where = new Location(world, last.x(), 64, last.z());
        int cx = last.x() >> 4;
        int cz = last.z() >> 4;
        Scheduling.region(plugin, where, () -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.addPluginChunkTicket(cx + dx, cz + dz, plugin);
                }
            }
        });
        int[] waited = {0};
        Scheduling.globalTimer(plugin, 5, 5, task -> {
            waited[0] += 5;
            Entity found = server.getEntity(last.horse());
            if (found instanceof AbstractHorse horse || waited[0] >= WAIT_TICKS) {
                task.cancel();
                Scheduling.region(plugin, where, () -> {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            world.removePluginChunkTicket(cx + dx, cz + dz, plugin);
                        }
                    }
                });
                if (found instanceof AbstractHorse horse && player.isOnline()) {
                    bring(player, horse);
                } else if (player.isOnline()) {
                    Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.lost"));
                }
            }
        });
    }

    private void bring(Player player, AbstractHorse horse) {
        Scheduling.entity(plugin, player, () -> {
            Location to = player.getLocation();
            Scheduling.entity(plugin, horse, () -> horse.teleportAsync(to).thenAccept(done -> {
                if (Boolean.TRUE.equals(done)) {
                    Scheduling.entity(plugin, horse, () -> horse.getWorld().playSound(horse.getLocation(),
                            Sound.ENTITY_HORSE_AMBIENT, SoundCategory.NEUTRAL, 1f, 1f));
                    Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.came"));
                } else {
                    Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.lost"));
                }
            }));
        });
    }

    /** Unused chunk tickets never outlive the plugin. */
    public void stop() {
        for (World world : server.getWorlds()) {
            Scheduling.global(plugin, () -> world.removePluginChunkTickets(plugin));
        }
    }
}
