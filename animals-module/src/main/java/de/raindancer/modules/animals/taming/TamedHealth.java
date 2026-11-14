package de.raindancer.modules.animals.taming;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;

import java.util.function.IntSupplier;

/**
 * A tamed horse grows stronger, as a wolf does when it becomes a dog: its health is multiplied by a modifier saved with
 * the horse. The base value stays the wild one, so breeding works from it and foals do not compound the bonus.
 * Applied when a horse is tamed, and on mounting for horses tamed before (or born tame).
 */
public final class TamedHealth implements Listener {

    static final NamespacedKey KEY = new NamespacedKey("rainsanimals", "tamed_health");

    private final IntSupplier percent;

    public TamedHealth(IntSupplier percent) {
        this.percent = percent;
    }

    /** The modifier's amount (multiplied base added) for a percentage of the wild health. */
    public static double amount(int percent) {
        return Math.max(0, percent - 100) / 100.0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse && !(horse instanceof Llama)) {
            apply(horse, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player && event.getMount() instanceof AbstractHorse horse
                && !(horse instanceof Llama) && horse.isTamed()) {
            apply(horse, false);
        }
    }

    /** Puts the right modifier on a tamed horse; heals it to the new maximum when it was just tamed. */
    public void apply(AbstractHorse horse, boolean heal) {
        AttributeInstance health = horse.getAttribute(Attribute.MAX_HEALTH);
        if (health == null) {
            return;
        }
        double amount = amount(percent.getAsInt());
        AttributeModifier current = health.getModifier(KEY);
        if (current == null || Math.abs(current.getAmount() - amount) > 1e-9) {
            if (current != null) {
                health.removeModifier(KEY);
            }
            if (amount > 0) {
                health.addModifier(new AttributeModifier(KEY, amount, AttributeModifier.Operation.ADD_SCALAR));
            }
        }
        if (heal) {
            horse.setHealth(health.getValue());
        } else if (horse.getHealth() > health.getValue()) {
            horse.setHealth(health.getValue());
        }
    }
}
