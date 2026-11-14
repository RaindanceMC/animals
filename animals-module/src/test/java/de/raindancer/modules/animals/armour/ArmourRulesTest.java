package de.raindancer.modules.animals.armour;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArmourRulesTest {

    @Test
    @DisplayName("horse armour lasts like the chestplate of its material")
    void durability() {
        assertThat(ArmourRules.durability(Material.LEATHER_HORSE_ARMOR)).isEqualTo(80);
        assertThat(ArmourRules.durability(Material.GOLDEN_HORSE_ARMOR)).isEqualTo(112);
        assertThat(ArmourRules.durability(Material.COPPER_HORSE_ARMOR)).isEqualTo(176);
        assertThat(ArmourRules.durability(Material.IRON_HORSE_ARMOR)).isEqualTo(240);
        assertThat(ArmourRules.durability(Material.DIAMOND_HORSE_ARMOR)).isEqualTo(528);
        assertThat(ArmourRules.durability(Material.NETHERITE_HORSE_ARMOR)).isEqualTo(592);
        assertThat(ArmourRules.durability(Material.SADDLE)).isZero();
    }

    @Test
    @DisplayName("and is mended with what it is made of")
    void repairs() {
        assertThat(ArmourRules.repairWith(Material.IRON_HORSE_ARMOR)).isEqualTo(Material.IRON_INGOT);
        assertThat(ArmourRules.repairWith(Material.GOLDEN_HORSE_ARMOR)).isEqualTo(Material.GOLD_INGOT);
        assertThat(ArmourRules.repairWith(Material.LEATHER_HORSE_ARMOR)).isEqualTo(Material.LEATHER);
        assertThat(ArmourRules.repairWith(Material.NETHERITE_HORSE_ARMOR)).isEqualTo(Material.NETHERITE_INGOT);
    }

    @Test
    @DisplayName("like wolf armour it takes every hit except what armour cannot stop")
    void absorbs() {
        assertThat(ArmourRules.absorbs(DamageCause.ENTITY_ATTACK)).isTrue();
        assertThat(ArmourRules.absorbs(DamageCause.FALL)).isTrue();
        assertThat(ArmourRules.absorbs(DamageCause.PROJECTILE)).isTrue();
        assertThat(ArmourRules.absorbs(DamageCause.LAVA)).isTrue();
        for (DamageCause through : new DamageCause[]{DamageCause.DROWNING, DamageCause.STARVATION, DamageCause.SUFFOCATION,
                DamageCause.MAGIC, DamageCause.POISON, DamageCause.WITHER, DamageCause.FREEZE, DamageCause.CRAMMING,
                DamageCause.VOID, DamageCause.WORLD_BORDER, DamageCause.THORNS, DamageCause.DRYOUT, DamageCause.KILL}) {
            assertThat(ArmourRules.absorbs(through)).as(through.name()).isFalse();
        }
    }

    @Test
    @DisplayName("each point of wear: Unbreaking spares it like on armour, 60 % plus 40 % divided by level + 1")
    void unbreaking() {
        assertThat(ArmourRules.wearChance(0)).isEqualTo(1.0);
        assertThat(ArmourRules.wearChance(3)).isCloseTo(0.7, within(1e-9));
        Random random = new Random(3);
        int worn = 0;
        for (int i = 0; i < 10_000; i++) {
            worn += ArmourRules.wear(1, 3, random);
        }
        assertThat(worn / 10_000.0).isCloseTo(0.7, within(0.02));
        assertThat(ArmourRules.wear(0.4, 0, random)).as("any hit costs at least one point").isEqualTo(1);
        assertThat(ArmourRules.wear(5.5, 0, random)).isEqualTo(6);
    }

    @Test
    @DisplayName("Depth Strider: a third of land speed in water per level, all of it at three")
    void depthStrider() {
        assertThat(ArmourRules.waterEfficiency(0)).isZero();
        assertThat(ArmourRules.waterEfficiency(1)).isCloseTo(1 / 3.0, within(1e-9));
        assertThat(ArmourRules.waterEfficiency(3)).isCloseTo(1.0, within(1e-9));
        assertThat(ArmourRules.waterEfficiency(5)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("Soul Speed: 40 % faster on soul sand and soil at level I, 10 % more per level after")
    void soulSpeed() {
        assertThat(ArmourRules.soulSpeedBonus(0)).isZero();
        assertThat(ArmourRules.soulSpeedBonus(1)).isCloseTo(0.4, within(1e-9));
        assertThat(ArmourRules.soulSpeedBonus(3)).isCloseTo(0.6, within(1e-9));
    }

    @Test
    @DisplayName("Frost Walker: freezes water in a radius of 2 + level, as on boots")
    void frostWalker() {
        assertThat(ArmourRules.frostRadius(1)).isEqualTo(3);
        assertThat(ArmourRules.frostRadius(2)).isEqualTo(4);
        assertThat(ArmourRules.frostRadius(0)).isZero();
    }

    @Test
    @DisplayName("Mending: two points of durability per point of experience")
    void mending() {
        assertThat(ArmourRules.mended(5, 100)).isEqualTo(10);
        assertThat(ArmourRules.mended(5, 3)).as("never more than is worn").isEqualTo(3);
        assertThat(ArmourRules.xpUsed(3)).isEqualTo(2);
    }

    @Test
    @DisplayName("Respiration: a point of extra air per level, as on a helmet")
    void respiration() {
        assertThat(ArmourRules.oxygenBonus(0)).isZero();
        assertThat(ArmourRules.oxygenBonus(3)).isEqualTo(3.0);
    }
}
