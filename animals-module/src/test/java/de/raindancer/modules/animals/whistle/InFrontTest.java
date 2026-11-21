package de.raindancer.modules.animals.whistle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InFrontTest {

    @Test
    @DisplayName("the horse lands a few blocks ahead of where the caller looks, level with them")
    void ahead() {
        // Minecraft's yaw: 0 looks south (+z), 90 west (-x), 180 north (-z), -90 east (+x)
        InFront south = InFront.of(10, 20, 0f, 3);
        assertThat(south.x()).isCloseTo(10, within(1e-9));
        assertThat(south.z()).isCloseTo(23, within(1e-9));
        InFront west = InFront.of(10, 20, 90f, 3);
        assertThat(west.x()).isCloseTo(7, within(1e-9));
        assertThat(west.z()).isCloseTo(20, within(1e-9));
        InFront east = InFront.of(0, 0, -90f, 2);
        assertThat(east.x()).isCloseTo(2, within(1e-9));
    }

    @Test
    @DisplayName("and turns to face them")
    void facing() {
        assertThat(InFront.of(0, 0, 0f, 3).yaw()).isEqualTo(180f);
        assertThat(InFront.of(0, 0, 90f, 3).yaw()).isEqualTo(-90f);
        assertThat(InFront.of(0, 0, -170f, 3).yaw()).isEqualTo(10f);
    }

    @Test
    @DisplayName("a whistle reaches the horse wherever the owner is, unless the server sets a reach")
    void reachesEverywhereByDefault() {
        assertThat(de.raindancer.modules.animals.AnimalsSettings.DEFAULTS.whistleReach()).isZero();
    }
}
