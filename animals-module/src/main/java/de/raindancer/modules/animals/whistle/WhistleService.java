package de.raindancer.modules.animals.whistle;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.items.CustomItem;
import de.raindancer.core.ui.effect.ParticleShape;
import de.raindancer.core.ui.effect.ParticleShows;
import de.raindancer.core.world.safety.Spot;
import de.raindancer.core.world.teleport.TravelShow;
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
 * it to them, from any distance and any world unless the server sets a reach. It lands in front of them, with the
 * caller's own teleport look. A horse whose chunk is not loaded is fetched by loading its neighbourhood for a moment.
 */
public final class WhistleService implements Listener {

    public static final String PLUGIN = "animals";
    public static final String ID = "whistle";
    static final NamespacedKey LAST = new NamespacedKey("rainsanimals", "last_horse");
    /** How long a horse whose chunk had to be loaded is waited for, in ticks. */
    private static final int WAIT_TICKS = 60;
    /** How far in front of the caller the horse lands, and how far around that a safe spot is looked for. */
    private static final double AHEAD = 3;
    private static final int SEARCH = 3;
    /** How long the rings show before the horse comes, and how often they are drawn, in ticks. */
    private static final long RING_TICKS = 30;
    private static final long RING_EVERY = 2;
    private static final double RANGE = 32;

    private final Plugin plugin;
    private final RainsCore core;
    private final Server server;
    private final Messages messages;
    private final Supplier<AnimalsSettings> settings;

    public WhistleService(Plugin plugin, RainsCore core, Messages messages, Supplier<AnimalsSettings> settings) {
        this.plugin = plugin;
        this.core = core;
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
            boolean sameWorld = horse.getWorld().equals(player.getWorld());
            outcome = WhistleRule.decide(player.getUniqueId(), true, horse.getOwnerUniqueId(), sameWorld,
                    !horse.getPassengers().isEmpty() && !horse.getPassengers().contains(player),
                    sameWorld ? horse.getLocation().distance(player.getLocation()) : 0, now.whistleReach());
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
        World world = server.getWorld(last.world());
        if (world == null) {
            messages.send(player, "animals.whistle.lost");
            return;
        }
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
            if (found instanceof AbstractHorse || waited[0] >= WAIT_TICKS) {
                task.cancel();
                if (found instanceof AbstractHorse horse && player.isOnline()) {
                    // The tickets stay until the horse has left, or it would be unloaded under the rings.
                    bring(player, horse, () -> letGo(world, where, cx, cz));
                } else {
                    letGo(world, where, cx, cz);
                    if (player.isOnline()) {
                        Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.lost"));
                    }
                }
            }
        });
    }

    private void letGo(World world, Location where, int cx, int cz) {
        Scheduling.region(plugin, where, () -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.removePluginChunkTicket(cx + dx, cz + dz, plugin);
                }
            }
        });
    }

    private void bring(Player player, AbstractHorse horse) {
        bring(player, horse, () -> { });
    }

    /**
     * Finds a safe spot a few blocks in front of the caller, shows the caller's own teleport look where the horse
     * stands and where it will land, then brings it there, turned to face them. {@code done} runs once it is over.
     */
    private void bring(Player player, AbstractHorse horse, Runnable done) {
        Scheduling.entity(plugin, player, () -> {
            Location at = player.getLocation();
            InFront front = InFront.of(at.getX(), at.getZ(), at.getYaw(), AHEAD);
            Location fallback = at.clone();
            fallback.setYaw(front.yaw());
            fallback.setPitch(0);
            core.safety().findSafe(new Spot(at.getWorld().getName(), (int) Math.floor(front.x()), at.getBlockY(),
                            (int) Math.floor(front.z())), SEARCH)
                    .exceptionally(failed -> Optional.empty())
                    .thenAccept(safe -> {
                        Location landing = safe.map(spot -> new Location(at.getWorld(), spot.x() + 0.5, spot.y(),
                                spot.z() + 0.5, front.yaw(), 0)).orElse(fallback);
                        rings(player.getUniqueId(), horse, landing, () -> arrive(player, horse, landing, done));
                    });
        });
    }

    /** The caller's waiting particles round the horse and on the landing spot, for a moment; then {@code then}. */
    private void rings(UUID caller, AbstractHorse horse, Location landing, Runnable then) {
        TravelShow show = core.travelShow();
        Optional<String> particle = show.waitParticle(caller);
        if (particle.isEmpty()) {
            then.run();
            return;
        }
        ParticleShape shape = show.waitShape(caller);
        Integer colour = show.waitColour(caller).orElse(null);
        Integer colourTo = show.waitColourTo(caller).orElse(null);
        int density = show.waitDensity(caller);
        Integer lifetime = shape.isWings() ? (int) RING_EVERY : null;
        long[] drawnHorse = {0};
        Scheduling.entityTimer(plugin, horse, 1, RING_EVERY, task -> {
            if (drawnHorse[0] * RING_EVERY >= RING_TICKS) {
                task.cancel();
                return;
            }
            Location here = horse.getLocation();
            ParticleShows.at(here, here.getYaw(), particle.get(), colour, colourTo, density, shape, drawnHorse[0]++,
                    RANGE, viewer -> true, lifetime);
        });
        long[] drawnLanding = {0};
        Scheduling.regionTimer(plugin, landing, 1, RING_EVERY, task -> {
            if (drawnLanding[0] * RING_EVERY >= RING_TICKS) {
                task.cancel();
                then.run();
                return;
            }
            ParticleShows.at(landing, landing.getYaw(), particle.get(), colour, colourTo, density, shape,
                    drawnLanding[0]++, RANGE, viewer -> true, lifetime);
        });
    }

    private void arrive(Player player, AbstractHorse horse, Location landing, Runnable done) {
        Scheduling.entity(plugin, horse, () -> {
            if (!horse.isValid()) {
                done.run();
                Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.lost"));
                return;
            }
            Location from = horse.getLocation();
            UUID caller = player.getUniqueId();
            core.travelShow().departed(caller, from.getWorld().getName(), from.getX(), from.getY(), from.getZ());
            horse.teleportAsync(landing).thenAccept(moved -> {
                done.run();
                if (Boolean.TRUE.equals(moved)) {
                    Scheduling.entity(plugin, horse, () -> {
                        core.travelShow().arrived(caller, landing.getWorld().getName(), landing.getX(),
                                landing.getY(), landing.getZ());
                        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_HORSE_AMBIENT,
                                SoundCategory.NEUTRAL, 1f, 1f);
                    });
                    Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.came"));
                } else {
                    Scheduling.entity(plugin, player, () -> messages.send(player, "animals.whistle.lost"));
                }
            });
        });
    }

    /** Unused chunk tickets never outlive the plugin. */
    public void stop() {
        for (World world : server.getWorlds()) {
            Scheduling.global(plugin, () -> world.removePluginChunkTickets(plugin));
        }
    }
}
