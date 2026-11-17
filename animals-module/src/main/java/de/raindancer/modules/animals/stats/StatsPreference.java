package de.raindancer.modules.animals.stats;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.animals.AnimalsSettings;

import java.util.function.Supplier;

/** The horse stats switch for Core's {@code /preferences}; {@code /horses stats} stays. */
public final class StatsPreference {

    private StatsPreference() {
    }

    public static Preference build(Supplier<AnimalsSettings> settings, Messages messages) {
        return Preference.toggle("animals:horse-stats", "Horses", "Horse stats",
                "Shows your horse's speed, jump and health above the hotbar while you ride or look at it.",
                player -> !StatsDisplay.hidden(player), (player, on) -> {
                    StatsDisplay.set(player, on);
                    messages.send(player, on ? "animals.stats.shown" : "animals.stats.hidden");
                }).shownTo(player -> settings.get().stats());
    }
}
