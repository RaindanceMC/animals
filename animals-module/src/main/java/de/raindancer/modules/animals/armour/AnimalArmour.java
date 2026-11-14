package de.raindancer.modules.animals.armour;

import de.raindancer.core.platform.util.Scheduling;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Enchantable;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Horse armour that works as wolf armour does — it takes the hits instead of the horse, wears out, breaks, and is mended
 * at an anvil with its material — and armour enchantments that do something on horse and dog armour. The protections,
 * Feather Falling and Thorns are the game's own: they work from any armour slot, the animal's included (seen on a real
 * server). What the game does only for boots, helmets and players' items is done here: Depth Strider, Soul Speed,
 * Frost Walker, Respiration, Mending, and Unbreaking on horse armour.
 */
public final class AnimalArmour implements Listener {

    static final NamespacedKey SOUL = new NamespacedKey("rainsanimals", "soul_speed");
    static final NamespacedKey STRIDER = new NamespacedKey("rainsanimals", "depth_strider");
    static final NamespacedKey BREATH = new NamespacedKey("rainsanimals", "respiration");
    private static final int REACH = 48;

    private final Plugin plugin;

    public AnimalArmour(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Every two seconds: armour in players' inventories made whole items, and Depth Strider on animals near them. */
    public ScheduledTask start() {
        return Scheduling.globalTimer(plugin, 40, 40, task -> {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                Scheduling.entity(plugin, online, () -> sweep(online));
            }
        });
    }

    private void sweep(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && complete(item)) {
                inventory.setItem(slot, item);
            }
        }
        for (Entity near : player.getNearbyEntities(REACH, REACH, REACH)) {
            if (wears(near)) {
                LivingEntity animal = (LivingEntity) near;
                Scheduling.entity(plugin, animal, () -> strider(animal));
            }
        }
    }

    /**
     * Gives horse armour what makes it armour like wolf armour — durability, its repair material, enchantability — and
     * dog armour enchantability. @return whether anything was added
     */
    public static boolean complete(ItemStack item) {
        Material type = item.getType();
        boolean changed = false;
        int durability = ArmourRules.durability(type);
        if (durability > 0) {
            if (!item.hasData(DataComponentTypes.MAX_DAMAGE)) {
                item.setData(DataComponentTypes.MAX_DAMAGE, durability);
                changed = true;
            }
            if (!item.hasData(DataComponentTypes.DAMAGE)) {
                item.setData(DataComponentTypes.DAMAGE, 0);
                changed = true;
            }
            if (!item.hasData(DataComponentTypes.REPAIRABLE)) {
                TypedKey<ItemType> with = TypedKey.create(RegistryKey.ITEM, Key.key(ArmourRules.repairWith(type).getKey().toString()));
                item.setData(DataComponentTypes.REPAIRABLE, Repairable.repairable(RegistrySet.keySet(RegistryKey.ITEM, with)));
                changed = true;
            }
        }
        if ((durability > 0 || type == Material.WOLF_ARMOR) && !item.hasData(DataComponentTypes.ENCHANTABLE)) {
            item.setData(DataComponentTypes.ENCHANTABLE, Enchantable.enchantable(10));
            changed = true;
        }
        return changed;
    }

    private static boolean wears(Entity entity) {
        return (entity instanceof AbstractHorse && !(entity instanceof Llama)) || entity instanceof Wolf;
    }

    private static ItemStack armourOf(LivingEntity animal) {
        EntityEquipment equipment = animal.getEquipment();
        if (equipment == null) {
            return null;
        }
        ItemStack body = equipment.getItem(EquipmentSlot.BODY);
        return body == null || body.getType().isAir() ? null : body;
    }

    private static int level(ItemStack armour, Enchantment enchantment) {
        return armour == null ? 0 : armour.getEnchantmentLevel(enchantment);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHorseHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || horse instanceof Llama
                || !ArmourRules.absorbs(event.getCause())) {
            return;
        }
        ItemStack armour = armourOf(horse);
        if (armour == null || ArmourRules.durability(armour.getType()) == 0) {
            return;
        }
        double damage = event.getFinalDamage();
        event.setDamage(0);
        complete(armour);
        Random random = ThreadLocalRandom.current();
        int worn = armour.getData(DataComponentTypes.DAMAGE) + ArmourRules.wear(damage, level(armour, Enchantment.UNBREAKING), random);
        Integer most = armour.getData(DataComponentTypes.MAX_DAMAGE);
        if (most != null && worn >= most) {
            horse.getEquipment().setItem(EquipmentSlot.BODY, null);
            horse.getWorld().playSound(horse.getLocation(), Sound.ITEM_WOLF_ARMOR_BREAK, SoundCategory.NEUTRAL, 1f, 1f);
            return;
        }
        armour.setData(DataComponentTypes.DAMAGE, worn);
        horse.getEquipment().setItem(EquipmentSlot.BODY, armour);
        horse.getWorld().playSound(horse.getLocation(), Sound.ITEM_WOLF_ARMOR_DAMAGE, SoundCategory.NEUTRAL, 0.6f, 1f);
    }

    /** Mending: experience picked up while riding mends the horse's armour; else that of one's dogs close by. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onExperience(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) {
            return;
        }
        Player player = event.getPlayer();
        LivingEntity mended = null;
        if (player.getVehicle() instanceof AbstractHorse horse && !(horse instanceof Llama) && needsMending(armourOf(horse))) {
            mended = horse;
        } else {
            for (Entity near : player.getNearbyEntities(16, 8, 16)) {
                if (near instanceof Wolf dog && player.getUniqueId().equals(dog.getOwnerUniqueId())
                        && needsMending(armourOf(dog))) {
                    mended = dog;
                    break;
                }
            }
        }
        if (mended == null) {
            return;
        }
        ItemStack armour = armourOf(mended);
        int worn = armour.getData(DataComponentTypes.DAMAGE);
        int back = ArmourRules.mended(event.getAmount(), worn);
        armour.setData(DataComponentTypes.DAMAGE, worn - back);
        mended.getEquipment().setItem(EquipmentSlot.BODY, armour);
        event.setAmount(event.getAmount() - Math.min(event.getAmount(), ArmourRules.xpUsed(back)));
    }

    private static boolean needsMending(ItemStack armour) {
        return armour != null && level(armour, Enchantment.MENDING) > 0 && armour.hasData(DataComponentTypes.DAMAGE)
                && armour.getData(DataComponentTypes.DAMAGE) > 0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRide(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof AbstractHorse horse && !(horse instanceof Llama)) {
            underfoot(horse);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWalk(EntityMoveEvent event) {
        if (wears(event.getEntity()) && event.hasChangedBlock()) {
            underfoot(event.getEntity());
        }
    }

    /** Soul Speed on soul sand and soil, Frost Walker on water — what the ground under the armour asks. */
    private static void underfoot(LivingEntity animal) {
        ItemStack armour = armourOf(animal);
        int soul = level(armour, Enchantment.SOUL_SPEED);
        Block under = animal.getLocation().getBlock().getRelative(BlockFace.DOWN);
        boolean onSoul = under.getType() == Material.SOUL_SAND || under.getType() == Material.SOUL_SOIL;
        modifier(animal, Attribute.MOVEMENT_SPEED, SOUL, onSoul ? ArmourRules.soulSpeedBonus(soul) : 0);
        int radius = ArmourRules.frostRadius(level(armour, Enchantment.FROST_WALKER));
        if (radius > 0 && animal.isOnGround()) {
            freeze(animal.getLocation(), radius);
        }
    }

    private static void strider(LivingEntity animal) {
        ItemStack armour = armourOf(animal);
        modifier(animal, Attribute.WATER_MOVEMENT_EFFICIENCY, STRIDER,
                ArmourRules.waterEfficiency(level(armour, Enchantment.DEPTH_STRIDER)));
        modifier(animal, Attribute.OXYGEN_BONUS, BREATH, ArmourRules.oxygenBonus(level(armour, Enchantment.RESPIRATION)));
    }

    /** Aqua Affinity comes with the helmet enchantments but would do nothing on an animal: never on its armour. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result != null && isAnimalArmour(result) && result.getEnchantmentLevel(Enchantment.AQUA_AFFINITY) > 0) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(org.bukkit.event.enchantment.EnchantItemEvent event) {
        if (isAnimalArmour(event.getItem())) {
            event.getEnchantsToAdd().remove(Enchantment.AQUA_AFFINITY);
        }
    }

    private static boolean isAnimalArmour(ItemStack item) {
        return ArmourRules.durability(item.getType()) > 0 || item.getType() == Material.WOLF_ARMOR;
    }

    /** As the game's Frost Walker: still water with air above, in a disc under the feet, turns to frosted ice. */
    private static void freeze(Location at, int radius) {
        Block centre = at.getBlock().getRelative(BlockFace.DOWN);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                Block block = centre.getRelative(dx, 0, dz);
                if (block.getType() == Material.WATER && block.getBlockData() instanceof Levelled water
                        && water.getLevel() == 0 && block.getRelative(BlockFace.UP).getType().isAir()) {
                    block.setType(Material.FROSTED_ICE);
                }
            }
        }
    }

    private static void modifier(LivingEntity animal, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance instance = animal.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(key);
        if (current != null && Math.abs(current.getAmount() - amount) < 1e-9) {
            return;
        }
        if (current != null) {
            instance.removeModifier(key);
        }
        if (Math.abs(amount) > 1e-9) {
            AttributeModifier.Operation operation = attribute == Attribute.MOVEMENT_SPEED
                    ? AttributeModifier.Operation.ADD_SCALAR : AttributeModifier.Operation.ADD_NUMBER;
            instance.addTransientModifier(new AttributeModifier(key, amount, operation));
        }
    }
}
