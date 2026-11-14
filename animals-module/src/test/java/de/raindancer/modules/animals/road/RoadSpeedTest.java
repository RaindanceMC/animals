package de.raindancer.modules.animals.road;

import de.raindancer.modules.animals.AnimalsSettings;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
}
