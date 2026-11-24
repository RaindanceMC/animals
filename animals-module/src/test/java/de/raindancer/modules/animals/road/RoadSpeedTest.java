package de.raindancer.modules.animals.road;

import de.raindancer.modules.animals.AnimalsSettings;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.raindancer.core.world.protection.LandAudience;
import de.raindancer.core.world.protection.LandAction;
import de.raindancer.core.world.protection.LandFlag;
import de.raindancer.core.world.protection.ProtectedArea;
import de.raindancer.modules.animals.stats.HorseStats;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RoadSpeedTest {

    /** What a running server says; the test has none. */
    private static final java.util.function.Predicate<Material> BLOCKS = m -> m != Material.DIAMOND && m != Material.AIR;


    @Test
    @DisplayName("shipped: 160 percent on dirt path, gravel and cobblestone, 110 everywhere else")
    void shipped() {
        AnimalsSettings d = AnimalsSettings.DEFAULTS;
        assertThat(d.roadPercent()).isEqualTo(160);
        assertThat(d.offRoadPercent()).isEqualTo(110);
        assertThat(RoadSpeed.roads(d.roadBlocks(), BLOCKS)).containsExactlyInAnyOrder(Material.DIRT_PATH, Material.GRAVEL, Material.COBBLESTONE);
        assertThat(RoadSpeed.isOff(100, 100)).isTrue();
        assertThat(RoadSpeed.isOff(d.roadPercent(), d.offRoadPercent())).isFalse();
    }

    @Test
    @DisplayName("road blocks are read by name, any case, by comma or space; what is no block is skipped")
    void readsRoadBlocks() {
        assertThat(RoadSpeed.roads(" Dirt_Path, minecraft:gravel  stone_bricks, diamond, nonsense,", BLOCKS))
                .containsExactlyInAnyOrder(Material.DIRT_PATH, Material.GRAVEL, Material.STONE_BRICKS);
        assertThat(RoadSpeed.roads(null, BLOCKS)).isEmpty();
    }

    @Test
    @DisplayName("on a road the road's percent, anywhere else the other one")
    void multiplier() {
        Set<Material> roads = Set.of(Material.DIRT_PATH, Material.GRAVEL);
        assertThat(RoadSpeed.multiplier(Material.DIRT_PATH, roads, 160, 110)).isCloseTo(1.6, within(1e-9));
        assertThat(RoadSpeed.multiplier(Material.GRASS_BLOCK, roads, 160, 110)).isCloseTo(1.1, within(1e-9));
        assertThat(RoadSpeed.multiplier(null, roads, 160, 110)).isCloseTo(1.1, within(1e-9));
    }

    @Test
    @DisplayName("the block looked at is the one the hooves stand on — a dirt path is lower than a full block")
    void standsOn() {
        assertThat(RoadSpeed.standingBlockY(64.0)).isEqualTo(63);
        assertThat(RoadSpeed.standingBlockY(63.9375)).isEqualTo(63);
        assertThat(RoadSpeed.standingBlockY(-0.0)).isEqualTo(-1);
    }

    /** A piece of protected ground as Core hands it out: the server's (nobody owns it) or a player's. */
    private static ProtectedArea area(String name, boolean server) {
        return new ProtectedArea() {
            public String id() { return name; }
            public String name() { return name; }
            public List<UUID> owners() { return server ? List.of() : List.of(UUID.randomUUID()); }
            public Optional<Boolean> flagOverride(LandFlag flag, LandAudience audience) { return Optional.empty(); }
            public LandAudience audienceOf(UUID who) { return LandAudience.VISITOR; }
            public boolean may(UUID who, LandAction action) { return true; }
        };
    }

    @Test
    @DisplayName("shipped: highways are the server claims called highway-…, where the best horse runs 40 blocks a second")
    void highwayShipped() {
        assertThat(AnimalsSettings.DEFAULTS.highwayPrefix()).isEqualTo("highway-");
        assertThat(AnimalsSettings.DEFAULTS.highwayTopBps()).isEqualTo(40);
    }

    @Test
    @DisplayName("on a highway the best possible horse reaches the top speed, and a slower one keeps its share")
    void highwayFactor() {
        double factor = RoadSpeed.highwayFactor(40);
        assertThat(HorseStats.blocksPerSecond(0.3375 * factor)).isCloseTo(40.0, within(1e-6));
        assertThat(HorseStats.blocksPerSecond(0.225 * factor)).isCloseTo(40.0 * 0.225 / 0.3375, within(1e-6));
        assertThat(RoadSpeed.highwayFactor(0)).isZero();
    }

    @Test
    @DisplayName("only a server claim whose name starts with the prefix is a highway, in any case")
    void whatIsAHighway() {
        assertThat(RoadSpeed.isHighway(Optional.of(area("highway-12", true)), "highway-")).isTrue();
        assertThat(RoadSpeed.isHighway(Optional.of(area("Highway-3", true)), "highway-")).isTrue();
        assertThat(RoadSpeed.isHighway(Optional.of(area("Spawn", true)), "highway-")).isFalse();
        assertThat(RoadSpeed.isHighway(Optional.of(area("highway-mine", false)), "highway-"))
                .as("a player cannot make their own highway by naming a claim").isFalse();
        assertThat(RoadSpeed.isHighway(Optional.empty(), "highway-")).isFalse();
        assertThat(RoadSpeed.isHighway(Optional.of(area("highway-1", true)), "")).as("no prefix, no highways").isFalse();
    }

    @Test
    @DisplayName("the highway outranks the road blocks and the off-road speed; everywhere else they stay as they were")
    void highwayOutranksRoads() {
        Set<Material> roads = Set.of(Material.DIRT_PATH);
        AnimalsSettings s = AnimalsSettings.DEFAULTS;
        Optional<ProtectedArea> highway = Optional.of(area("highway-1", true));
        Optional<ProtectedArea> spawn = Optional.of(area("Spawn", true));

        assertThat(RoadSpeed.factor(Material.STONE_BRICKS, highway, roads, s)).isCloseTo(RoadSpeed.highwayFactor(40), within(1e-9));
        assertThat(RoadSpeed.factor(Material.DIRT_PATH, highway, roads, s)).isCloseTo(RoadSpeed.highwayFactor(40), within(1e-9));
        assertThat(RoadSpeed.factor(Material.DIRT_PATH, spawn, roads, s)).isCloseTo(1.6, within(1e-9));
        assertThat(RoadSpeed.factor(Material.GRASS_BLOCK, Optional.empty(), roads, s)).isCloseTo(1.1, within(1e-9));
    }

    @Test
    @DisplayName("a top speed of 0 switches highways off, and roads at 100/100 do not switch highways off")
    void switches() {
        Set<Material> roads = Set.of(Material.DIRT_PATH);
        Optional<ProtectedArea> highway = Optional.of(area("highway-1", true));
        AnimalsSettings off = new AnimalsSettings(160, 110, "dirt_path", "highway-", 0, 200, true, 10, 5, true, 0, 30, true);
        AnimalsSettings vanillaRoads = new AnimalsSettings(100, 100, "dirt_path", "highway-", 40, 200, true, 10, 5, true, 0, 30, true);

        assertThat(RoadSpeed.factor(Material.DIRT_PATH, highway, roads, off)).isCloseTo(1.6, within(1e-9));
        assertThat(RoadSpeed.factor(Material.STONE, highway, roads, vanillaRoads)).isCloseTo(RoadSpeed.highwayFactor(40), within(1e-9));
        assertThat(RoadSpeed.factor(Material.STONE, Optional.empty(), roads, vanillaRoads)).isEqualTo(1.0);
    }
}
