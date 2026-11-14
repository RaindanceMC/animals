package de.raindancer.modules.animals;

import de.raindancer.modules.animals.stats.StatsDisplay;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** {@code /horses stats} switches the stats above the hotbar for oneself. */
public final class HorsesCommand implements BasicCommand {

    private final Supplier<AnimalsServices> services;

    public HorsesCommand(Supplier<AnimalsServices> services) {
        this.services = services;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        AnimalsServices live = services.get();
        if (!(source.getSender() instanceof Player player)) {
            live.messages().send(source.getSender(), "animals.only-a-player");
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("stats")) {
            if (!live.settings().get().stats()) {
                live.messages().send(player, "animals.stats.disabled");
                return;
            }
            live.messages().send(player, StatsDisplay.toggle(player) ? "animals.stats.shown" : "animals.stats.hidden");
            return;
        }
        live.messages().send(player, "animals.usage");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return args.length <= 1 ? List.of("stats") : List.of();
    }
}
