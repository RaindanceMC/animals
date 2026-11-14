package de.raindancer.modules.animals.stats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HorseStatsTest {

    @Test
    @DisplayName("speed in blocks a second: the best horse about 14.6, the slowest about 4.9")
    void speed() {
        assertThat(HorseStats.blocksPerSecond(0.3375)).isCloseTo(14.57, within(0.05));
        assertThat(HorseStats.blocksPerSecond(0.1125)).isCloseTo(4.86, within(0.05));
    }

    @Test
    @DisplayName("jump height in blocks: strength 0.4 about 1.1, 0.7 about 2.9, 1.0 about 5.3")
    void jump() {
        assertThat(HorseStats.jumpBlocks(0.4)).isCloseTo(1.11, within(0.05));
        assertThat(HorseStats.jumpBlocks(0.7)).isCloseTo(2.89, within(0.05));
        assertThat(HorseStats.jumpBlocks(1.0)).isCloseTo(5.29, within(0.05));
    }

    @Test
    @DisplayName("one line: speed (with the road), jump and hearts, rounded as a player reads them")
    void line() {
        assertThat(HorseStats.plainLine(0.225, 0.7, 22, 1.6))
                .isEqualTo("Speed 9.7 b/s (15.5 here) · Jump 2.9 blocks · Health 11 ❤");
        assertThat(HorseStats.plainLine(0.225, 0.7, 23, 1.0))
                .isEqualTo("Speed 9.7 b/s · Jump 2.9 blocks · Health 11.5 ❤");
    }
}
