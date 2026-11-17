package de.raindancer.modules.animals;

import de.raindancer.core.content.items.CustomItem;
import de.raindancer.core.content.items.ItemAbility;
import de.raindancer.core.content.items.ItemRecipes;
import de.raindancer.core.content.items.ItemTrigger;
import de.raindancer.core.content.items.TrimTwins;
import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.modules.api.FlexModule;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.api.ModuleInfo;
import de.raindancer.modules.animals.breeding.BreedingListener;
import de.raindancer.modules.animals.pack.AnimalsPack;
import de.raindancer.modules.animals.road.RoadSpeeds;
import de.raindancer.modules.animals.stats.StatsDisplay;
import de.raindancer.modules.animals.whistle.WhistleService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public final class AnimalsModule implements FlexModule {

    private static final ModuleInfo INFO = ModuleInfo.of("animals", "Animals", "0.4.0")
            .describedAs("Horses faster on roads, trims on horse armour, a horse whistle, better breeding and horse stats")
            .by("Raindancer118");

    /** Every horse armour, each a base of the trim twins. */
    static final Set<Material> HORSE_ARMOURS = Set.of(Material.LEATHER_HORSE_ARMOR, Material.COPPER_HORSE_ARMOR,
            Material.IRON_HORSE_ARMOR, Material.GOLDEN_HORSE_ARMOR, Material.DIAMOND_HORSE_ARMOR,
            Material.NETHERITE_HORSE_ARMOR);

    private static volatile AnimalsServices services;

    @Override
    public ModuleInfo info() {
        return INFO;
    }

    /**
     * Horse and dog armour into the item tags armour enchantments check, so books go on them at an anvil and the
     * enchanting table offers them: protections and Thorns (armour), the boots' ones (feet), Respiration (head),
     * Unbreaking and Mending.
     */
    @Override
    public void bootstrap(io.papermc.paper.plugin.bootstrap.BootstrapContext context) {
        java.util.List<io.papermc.paper.registry.TypedKey<org.bukkit.inventory.ItemType>> armours = new java.util.ArrayList<>();
        for (Material material : HORSE_ARMOURS) {
            armours.add(io.papermc.paper.registry.TypedKey.create(io.papermc.paper.registry.RegistryKey.ITEM,
                    net.kyori.adventure.key.Key.key(material.getKey().toString())));
        }
        armours.add(io.papermc.paper.registry.TypedKey.create(io.papermc.paper.registry.RegistryKey.ITEM,
                net.kyori.adventure.key.Key.key("minecraft:wolf_armor")));
        context.getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.TAGS.postFlatten(io.papermc.paper.registry.RegistryKey.ITEM),
                event -> {
                    var tags = event.registrar();
                    tags.addToTag(io.papermc.paper.registry.keys.tags.ItemTypeTagKeys.ENCHANTABLE_ARMOR, armours);
                    tags.addToTag(io.papermc.paper.registry.keys.tags.ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR, armours);
                    tags.addToTag(io.papermc.paper.registry.keys.tags.ItemTypeTagKeys.ENCHANTABLE_DURABILITY, armours);
                    tags.addToTag(io.papermc.paper.registry.keys.tags.ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR, armours);
                });
    }

    @Override
    public void enable(ModuleContext context) {
        Server server = context.plugin().getServer();
        context.core().messages().defineFrom(AnimalsModule.class.getResourceAsStream("messages.yml"),
                context.chat().brand()::chatPrefix);
        SettingsStore<AnimalsSettings> settings = context.settings(AnimalsSettings.class, AnimalsSettings.DEFAULTS);

        TrimTwins trims = new TrimTwins(server, new NamespacedKey(context.plugin(), "horse").getNamespace(), "horse",
                HORSE_ARMOURS);
        int twins = trims.register();
        context.closeWith(trims::unregister);
        context.listener(trims.followReloads(n -> context.log().info("Datapacks reloaded: {} trim recipes take horse armour.", n)));
        if (twins == 0 || !trims.allPresent()) {
            context.log().warn("No trim recipe for horse armour is in the server's recipes ({} added).", twins);
        }
        TrimTwins dogTrims = new TrimTwins(server, new NamespacedKey(context.plugin(), "wolf").getNamespace(), "wolf",
                Set.of(Material.WOLF_ARMOR));
        int dogTwins = dogTrims.register();
        context.closeWith(dogTrims::unregister);
        context.listener(dogTrims.followReloads(n -> context.log().info("Datapacks reloaded: {} trim recipes take dog armour.", n)));
        AnimalsPack.contribute(context.plugin(), context.core(), context.log());
        context.closeWith(() -> AnimalsPack.withdraw(context.core()));

        RoadSpeeds roads = new RoadSpeeds(context.plugin(), settings::current);
        context.listener(roads);
        var roading = roads.start();
        context.closeWith(roading::cancel);

        de.raindancer.modules.animals.taming.TamedHealth tamed =
                new de.raindancer.modules.animals.taming.TamedHealth(() -> settings.current().tamedHealthPercent());
        context.listener(tamed);
        context.listener(new BreedingListener(settings::current, context.core().messages(), tamed));

        StatsDisplay stats = new StatsDisplay(context.plugin(), context.core(), settings::current, roads);
        var showing = stats.start();
        context.closeWith(showing::cancel);

        WhistleService whistle = new WhistleService(context.plugin(), context.core().messages(), settings::current);
        context.listener(whistle);
        context.closeWith(whistle::stop);
        context.core().items().defineIfAbsent(WhistleService.definition());
        registerWhistle(context, whistle, settings.current());
        settings.onChange(now -> registerWhistle(context, whistle, now));
        CustomItem item = context.core().items().byKey(WhistleService.PLUGIN + ":" + WhistleService.ID)
                .orElse(WhistleService.definition());
        context.core().itemFactory().create(item).flatMap(stack -> ItemRecipes.build(context.plugin(), item, stack))
                .ifPresent(recipe -> {
                    if (server.addRecipe(recipe, true)) {
                        context.closeWith(() -> server.removeRecipe(recipe.getKey(), true));
                    }
                });

        de.raindancer.modules.animals.armour.AnimalArmour armour =
                new de.raindancer.modules.animals.armour.AnimalArmour(context.plugin());
        context.listener(armour);
        var sweeping = armour.start();
        context.closeWith(sweeping::cancel);

        // /preferences (Core 2.103.0) lists the stats switch; /horses stats keeps working.
        try {
            var preference = de.raindancer.modules.animals.stats.StatsPreference.build(settings::current,
                    context.core().messages());
            de.raindancer.core.ui.preference.Preferences.offer(context.plugin(), preference);
            context.closeWith(() -> de.raindancer.core.ui.preference.Preferences.withdraw(preference));
        } catch (LinkageError olderCore) {
            context.log().info("RainsCore is older than 2.103.0: the horse stats switch is not on /preferences.");
        }

        services = new AnimalsServices(context.core().messages(), settings::current);
        context.log().info("Horses are up: {} trim recipes take horse armour, {} dog armour; roads {} %, elsewhere {} %; breeding {}; whistle {}.",
                twins, dogTwins, settings.current().roadPercent(), settings.current().offRoadPercent(),
                settings.current().breeding() ? "better" : "vanilla", settings.current().whistle() ? "on" : "off");
    }

    private static void registerWhistle(ModuleContext context, WhistleService whistle, AnimalsSettings now) {
        context.core().itemAbilities().register(ItemAbility.builder(WhistleService.PLUGIN, WhistleService.ID)
                .on(ItemTrigger.RIGHT_CLICK)
                .describedAs("Call your horse")
                .cooldown(Duration.ofSeconds(now.whistleCooldown()))
                .attempts(use -> {
                    Player player = context.plugin().getServer().getPlayer(use.player());
                    return player != null && whistle.call(player);
                })
                .build());
    }

    @Override
    public List<ModuleCommand> commands() {
        return List.of(ModuleCommand.of("horses", "Switches the horse stats above your hotbar",
                        new HorsesCommand(AnimalsModule::require))
                .aliased("horse")
                .taking("stats: switch the horse stats above your hotbar on or off"));
    }

    @Override
    public void disable() {
        services = null;
    }

    private static AnimalsServices require() {
        AnimalsServices live = services;
        if (live == null) {
            throw new IllegalStateException("the animals module is not running");
        }
        return live;
    }
}
