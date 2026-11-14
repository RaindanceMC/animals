package de.raindancer.modules.animals.breeding;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.animals.AnimalsSettings;
import de.raindancer.modules.animals.stats.HorseStats;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** A horse foal's speed, jump and health from {@link Inheritance} instead of vanilla's average. Donkeys and mules keep the game's. */
public final class BreedingListener implements Listener {

    private final Supplier<AnimalsSettings> settings;
    private final Messages messages;
    private final de.raindancer.modules.animals.taming.TamedHealth tamed;

    public BreedingListener(Supplier<AnimalsSettings> settings, Messages messages,
                            de.raindancer.modules.animals.taming.TamedHealth tamed) {
        this.settings = settings;
        this.messages = messages;
        this.tamed = tamed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        AnimalsSettings now = settings.get();
        if (!now.breeding() || !(event.getEntity() instanceof Horse foal)
                || !(event.getMother() instanceof Horse mother) || !(event.getFather() instanceof Horse father)) {
            return;
        }
        Inheritance inheritance = new Inheritance(now.betterChance(), now.betterMost());
        Random random = ThreadLocalRandom.current();
        inherit(foal, mother, father, Attribute.MOVEMENT_SPEED, Inheritance.SPEED, inheritance, random);
        inherit(foal, mother, father, Attribute.JUMP_STRENGTH, Inheritance.JUMP, inheritance, random);
        inherit(foal, mother, father, Attribute.MAX_HEALTH, Inheritance.HEALTH, inheritance, random);
        if (foal.isTamed()) {
            tamed.apply(foal, true);
        } else {
            AttributeInstance health = foal.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) {
                foal.setHealth(health.getValue());
            }
        }
        if (event.getBreeder() instanceof Player breeder) {
            messages.send(breeder, "animals.foal", "line", HorseStats.plainLine(base(foal, Attribute.MOVEMENT_SPEED),
                    base(foal, Attribute.JUMP_STRENGTH), base(foal, Attribute.MAX_HEALTH), 1.0));
        }
    }

    private static void inherit(Horse foal, Horse mother, Horse father, Attribute attribute, Inheritance.Range range,
                                Inheritance inheritance, Random random) {
        AttributeInstance child = foal.getAttribute(attribute);
        if (child == null) {
            return;
        }
        child.setBaseValue(inheritance.child(base(mother, attribute), base(father, attribute), range, random));
    }

    private static double base(Horse horse, Attribute attribute) {
        AttributeInstance instance = horse.getAttribute(attribute);
        return instance == null ? 0 : instance.getBaseValue();
    }
}
